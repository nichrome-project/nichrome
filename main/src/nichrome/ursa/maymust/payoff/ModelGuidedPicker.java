package nichrome.ursa.maymust.payoff;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

import nichrome.mln.Atom;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.util.Config;
import nichrome.ursa.maymust.AlarmResolutionDriver;
import nichrome.ursa.maymust.AnalysisWithAnnotations;
import nichrome.ursa.maymust.InferenceRule;
import nichrome.ursa.maymust.Label;
import nichrome.ursa.maymust.QuestionPicker;

public class ModelGuidedPicker implements QuestionPicker {

	protected MarkovLogicNetwork mln;
	private Set<Integer> questions;
	private Set<Integer> reports;

	private AnalysisWithAnnotations analysis;

	private boolean encodeCorre = true;

	private OptimizationSolver solver;

	private Model m;

	static final int TrueVar = 1;
	static final int FalseVar = 2;
	private int nextVar = 3;

	public final static int termRate = 1;

	// if any in value is false, then key is false
	private Map<Integer, Set<Integer>> correlMap;

	/**
	 * How many questions we are allowed to ask
	 */
	private int budget = 1;
	private int lastPickValue = -1;

	// current implementation is PLDI14 like, each tuple t is associated with:
	// R_t and Q_t
	private HashMap<Integer, List<Integer>> atomsToVar = new HashMap<Integer, List<Integer>>();
	private HashMap<Integer, Integer> varToAtom = new HashMap<Integer, Integer>();

	public ModelGuidedPicker(MarkovLogicNetwork mln, OptimizationSolver solver, Model m, boolean encodeCorre,
			Set<Integer> questions, Set<Integer> reports, AnalysisWithAnnotations analysis) {
		if (!AlarmResolutionDriver.client.equals("datarace") && encodeCorre)
			throw new RuntimeException("Cannot encode correlation for client " + AlarmResolutionDriver.client);
		this.encodeCorre = encodeCorre;
		this.mln = mln;
		this.solver = solver;
		this.m = m;

		this.analysis = analysis;
		this.questions = questions;
		this.reports = reports;

		this.loadCorre();
	}

	public void setBudget(int b) {
		this.budget = b;
	}

	@Override
	public List<Integer> pick(AnalysisWithAnnotations a, int lr) {
		assert(a == this.analysis);
		// start binary search
		int up = 0;
		if (lr == 0 || lastPickValue < 0)
			for (int r : this.reports) {
				if (!a.isAtLabeled(r))
					up++;
			}
		else
			// an optimization, when the answers to last round are all true, or
			// all false, the gen rate in this round cannot improve.
			up = lastPickValue;
		int lb = 0;
		boolean ifFirst = true;
		;
		while (true) {
			System.out.println("UB: " + up + ", LB: " + lb);
			List<Integer> res = onlyResolveFalse(a, up, !ifFirst); // hack(a);
																	// //
			ifFirst = false;
			if (res != null) {
				if (up <= termRate)
					return null;
				lastPickValue = up;
				return res;
			} else {
				int mid = (up + lb) / 2;
				res = onlyResolveFalse(a, mid, !ifFirst);
				if (res != null) {
					if (lb == up - 1) {
						if (mid <= termRate)
							return null;
						lastPickValue = mid;
						return res;
					}
					lb = Math.max(mid, (int) this.getLastExpectedRatio());
					continue;
				} else {
					if (up == mid)
						return null;
					up = mid;
					continue;
				}
			}
		}
	}

	private boolean isQuestionTuple(int atomId) {
		return questions.contains(atomId);
	}

	private boolean isReportTuple(int atomId) {
		return reports.contains(atomId);
	}

	private void loadCorre() {
		if (encodeCorre && correlMap == null) {
			correlMap = new HashMap<Integer, Set<Integer>>();
			try {
				Scanner sc = new Scanner(new File(Config.dir_out + File.separator + "correlEE.txt"));
				while (sc.hasNextLine()) {
					String line = sc.nextLine().trim();
					String lTokens[] = line.split(" ");
					int e1 = mln.getAtomID(mln.parseAtom(lTokens[1]));
					int e2 = mln.getAtomID(mln.parseAtom(lTokens[0]));
					Set<Integer> dep = correlMap.get(e1);
					if (dep == null) {
						dep = new HashSet<Integer>();
						correlMap.put(e1, dep);
					}
					dep.add(e2);
				}
				sc.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private Set<Integer> setQ; // cache
	Set<Integer> allRelated;
	private double lastRatioLB = -1;
	private List<Integer> nRs;
	private double lastExpectedRatio = -1;

	public double getLastExpectedRatio() {
		return this.lastExpectedRatio;
	}

	public List<Integer> onlyResolveFalse(AnalysisWithAnnotations a, double ratioLB, boolean warmStart) {
		assert(this.analysis == a);
		if (warmStart) {
			if (lastRatioLB != ratioLB) {
				solver.forget();
				encodeLB(ratioLB);
			}
		} else {
			allRelated = getAllRelatedTuples(a, this.reports, this.correlMap);
			Map<Integer, Set<InferenceRule>> headToRules = a.getHeadToRules();

			Set<Integer> setR = new LinkedHashSet<Integer>();
			setQ = new LinkedHashSet<Integer>();

			solver.newProblem();

			solver.encodeSingle(TrueVar, -1);
			solver.encodeSingle(-FalseVar, -1);

			Set<Integer> varDom = new HashSet<Integer>();
			varDom.add(TrueVar);
			varDom.add(FalseVar);
			for (int at : allRelated) {
				if (this.analysis.isAtLabeled(at))
					continue;
				varDom.addAll(this.getVarsForTuple(at));
			}
			solver.createVarsBatch(varDom);
			int edbNum = 0;
			for (int i : allRelated)
				if (!this.analysis.isAtLabeled(i))
					if (!headToRules.containsKey(i) || headToRules.get(i).isEmpty())
						edbNum++;
			solver.createTmpVarBatch(this.analysis.getNumRules() + edbNum);

			// soft constraints for all Query vars
			// current could query any tuple

			for (Integer at : allRelated) {
				if (this.analysis.isAtLabeled(at)) // it won't propagate
					continue;
				Set<InferenceRule> rules = headToRules.get(at);
				List<Integer> vars = getVarsForTuple(at);
				Integer Rt = vars.get(0);
				Integer Qt = vars.get(1);
				setR.add(Rt);
				setQ.add(Qt);

				// encode the correlation
				Set<Integer> decidingQs = new HashSet<Integer>();
				if (encodeCorre && this.correlMap.containsKey(at)) {
					for (int dat : this.correlMap.get(at)) {
						if (this.analysis.isAtLabeled(dat)) {
							if (this.analysis.getLabel(dat) == Label.FALSE)
								throw new RuntimeException("Correllation on " + atomNiceDisplay(dat));
							else
								continue;
						}
						// here, we use Qts rather than Rts. It works when the
						// correlation rules are on edb.
						// if we use Rts, cycle is a problem.
						decidingQs.add(getVarsForTuple(dat).get(1));
					}
				}

				// Not the head of any grounded rule
				if (rules == null || rules.isEmpty()) {
					List<Integer> ls = new ArrayList<Integer>();
					ls.add(Qt);
					ls.addAll(decidingQs);
					solver.encodeImplication(ls, Rt, -1);
				} else {
					for (InferenceRule r : rules) {
						if (!decidingQs.isEmpty())
							throw new RuntimeException(
									"Haven't handled the case where the correlation rules are on on idbs.");
						List<Integer> ls = new ArrayList<Integer>();
						ls.add(Qt);
						for (int l : r.getCondition()) {
							if (this.analysis.isAtLabeled(Math.abs(l))) {
								Label label = this.analysis.getLabel(Math.abs(l));
								if (label == Label.TRUE && l > 0 || label == Label.FALSE && l < 0) {
									ls.add(TrueVar);
								} else{
									System.out.println("Prelabel false: "+this.atomNiceDisplay(Math.abs(l)));
									ls.add(FalseVar);
								}
							} else {
								if (l < 0) {
									// throw new RuntimeException("negation in
									// body
									// is
									// not expected in maxsat encoding, rule:" +
									// ruleNiceDisplay(r));

									// System.out.println("negation in body is
									// not
									// expected in maxsat encoding, rule:"
									// + ruleNiceDisplay(r));
									// System.out
									// .println("will assume that " +
									// atomNiceDisplay(Math.abs(l)) + " is edb
									// tuple");
									ls.add(TrueVar); // it
														// is
														// always
														// safe
														// to
														// assume
														// l
														// to
														// be
														// true
								}
								List<Integer> RQ = getVarsForTuple(l);
								ls.add(RQ.get(0));
							}
						}

						// R_t1 /\ R_t2 /\ ... /\ Qt -> Rt
						solver.encodeImplication(ls, Rt, -1);
					}
				}
			}

			// encode the constraint that we at most can ask k questions:
			// solver.encodeCardinality(qs, budget);

			// encode the objective: maximize expectation, probability * number
			// of
			// tuples resolved
			// solver.encodeQuadraticObjective(rs, rewards, qs, probs);

			encodeLB(ratioLB);

			nRs = new ArrayList<Integer>();
			for (Integer at : allRelated) {
				if (this.analysis.isAtLabeled(at))
					continue;
				List<Integer> vars = getVarsForTuple(at);
				Integer Rt = vars.get(0);
				if (this.isReportTuple(at)) {
					nRs.add(Rt);
				}
			}

			// at least resolve a report
			solver.encodeCardinality(nRs, nRs.size() - 1);

			List<Integer> ovars = new ArrayList<Integer>();
			List<Double> coefs = new ArrayList<Double>();

			for (int q : setQ) {
				ovars.add(q);
				coefs.add(1.0);
			}

			solver.encodeLinearObjective(ovars, coefs);

		}

		List<Integer> ret = new ArrayList<Integer>();

		Set<Integer> sol = solver.solve();

		this.lastRatioLB = ratioLB;

		if (sol == null)
			return null;

		for (int q : setQ)
			if (!sol.contains(q)) {
				ret.add(this.varToAtom.get(q));
			}

		Set<Integer> resolvedRs = new HashSet<Integer>();

		for (int r : nRs) {
			if (!sol.contains(r))
				resolvedRs.add(this.varToAtom.get(r));
		}

		System.out.println("Question picked: ");
		for (int i : ret)
			System.out.println(this.atomNiceDisplay(i));

		System.out.println("Number of expected resolved tuples: " + resolvedRs.size());

		this.lastExpectedRatio = ((double) resolvedRs.size()) / ((double) ret.size());

		return ret;
	}

	private void encodeLB(double ratioLB) {
		List<Integer> grVars = new ArrayList<Integer>();
		List<Double> grCoefs = new ArrayList<Double>();
		for (Integer at : allRelated) {
			if (this.analysis.isAtLabeled(at))
				continue;
			List<Integer> vars = getVarsForTuple(at);
			Integer Rt = vars.get(0);
			Integer Qt = vars.get(1);
			if (this.isQuestionTuple(at) && m.resolve(at) == Label.FALSE) {
				// take the negative, as Qt = false means that the
				// question
				// is
				// being asked
				grVars.add(-Qt);
				grCoefs.add(-ratioLB);
			} else {
				solver.encodeSingle(Qt, -1);
			}
			if (this.isReportTuple(at)) {
				grVars.add(-Rt);
				grCoefs.add(1.0);
			}
		}

		// encode the constraint that we at most can ask k questions:
		// solver.encodeCardinality(qs, budget);

		// encode the objective: maximize expectation, probability *
		// number
		// of
		// tuples resolved
		// solver.encodeQuadraticObjective(rs, rewards, qs, probs);

		// (-r1)+....+(-rn) - lb((-q1)+...+(-qn)) >= 0
		solver.encodeLinearConsLB(grVars, grCoefs, 0, true);
	}

	public String atomNiceDisplay(int atomId) {
		Atom at = mln.getAtom(atomId);
		return at.toGroundString(mln);
	}

	public String ruleNiceDisplay(InferenceRule r) {

		StringBuilder sb = new StringBuilder();

		for (int x : r.getCondition()) {
			if (x < 0) {
				sb.append("NOT ");
				x = -x;
			}
			sb.append(atomNiceDisplay(x));
			sb.append(" /\\ ");
		}

		sb.append(" => ");
		sb.append(atomNiceDisplay(r.getResult()));
		return sb.toString();
	}

	// Some pruning operation. Be very careful when the correlation rules are
	// involved
	private Set<Integer> getAllRelatedTuples(AnalysisWithAnnotations a, Set<Integer> initSet, Map<Integer, Set<Integer>> correlMap2) {
		Set<Integer> rs = new HashSet<Integer>();
		rs.addAll(a.getAllAtoms());
		if(correlMap2 != null)
			for (Map.Entry<Integer, Set<Integer>> entry : correlMap2.entrySet()) {
				rs.add(entry.getKey());
				rs.addAll(entry.getValue());
			}
		/*
		Queue<Integer> queue = new LinkedList<Integer>();

		Map<Integer, Set<InferenceRule>> headToRules = a.getHeadToRules();

		for (Integer atId : initSet) {
			queue.add(atId);
			rs.add(atId);
		}

		while (queue.size() != 0) {
			Integer currentAt = queue.remove();
			Set<InferenceRule> predRules = headToRules.get(currentAt);

			if (predRules != null)
				for (InferenceRule ir : predRules) {
					Set<Integer> cnds = ir.getCondIds();
					for (Integer pred : cnds) {
						if (rs.add(pred)) {
							queue.add(pred);
						}
					}
				}

			if (this.correlMap != null && this.correlMap.containsKey(currentAt))
				for (int dq : this.correlMap.get(currentAt)) {
					if (rs.add(dq))
						queue.add(dq);
				}
		}
		*/
		return rs;
	}

	private List<Integer> getVarsForTuple(Integer a) {
		a = Math.abs(a);
		List<Integer> vars = atomsToVar.get(a);
		if (vars != null) {
			return vars;
		}

		int Rt = nextVar++;
		int Qt = nextVar++;
		vars = new ArrayList<Integer>();
		vars.add(Rt);
		vars.add(Qt);

		varToAtom.put(Rt, a);
		varToAtom.put(Qt, a);

		atomsToVar.put(a, vars);
		return vars;
	}

	@Override
	public void simplify(AnalysisWithAnnotations a) {
		Set<Integer> userFalseTuples = new HashSet<Integer>();
		for (int at : a.getAllAtoms()) {
			if (this.questions.contains(at) && m.resolve(at) == Label.FALSE) {
				userFalseTuples.add(at);
			}
		}
		a.simplify(userFalseTuples, this.reports);
	}

}

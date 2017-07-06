package nichrome.ursa.maymust;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.Pair;

import nichrome.datalog.DlogRunner;
import nichrome.datalog.utils.ArraySet;
import nichrome.mln.Atom;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.util.Config;
import nichrome.mln.util.UIMan;

/**
 * Core class for label propagation.
 * 
 * @author xin
 *
 */
public class AnalysisWithAnnotations {
	protected Set<InferenceRule> rules = new HashSet<InferenceRule>();
	protected Map<Integer, Label> labelMap;
	private Map<Integer, Set<InferenceRule>> headToRules;
	protected Map<Integer, Set<InferenceRule>> bodyToRules;
	protected MarkovLogicNetwork mln;

	protected Integer numForwardProp = 0;
	protected Integer numBackwardProp = 0;
	protected Integer numBackwardPropTrue = 0;
	protected Integer numBackwardPropFalse = 0;

	private Oracle o;

	/**
	 * The rules should not contain cycle.
	 * 
	 * @param rules
	 */
	public AnalysisWithAnnotations(Set<InferenceRule> rules, MarkovLogicNetwork mln) {
		this.init(rules, mln);
	}
	
	protected void init(Set<InferenceRule> rules, MarkovLogicNetwork mln) {
		this.mln = mln;
		System.out.println("Number of rules: " + rules.size());
		Pair<Map<Integer, Set<InferenceRule>>, Map<Integer, Set<InferenceRule>>> graphs = this.generateGraph(rules);
		this.setHeadToRules(graphs.left);
		this.bodyToRules = graphs.right;
		this.labelMap = new HashMap<Integer, Label>();
		this.rules = rules;
	}
	
	public void simplify(Set<Integer> falseTuples, Set<Integer> reports){
		throw new RuntimeException("Not implemented yet.");
	}

	protected void postProcess() {
		try {
			PrintWriter pw1 = new PrintWriter(
					new File(nichrome.datalog.Config.bddbddbWorkDirName + File.separator + "escE.txt"));
			PrintWriter pw2 = new PrintWriter(
					new File(nichrome.datalog.Config.bddbddbWorkDirName + File.separator + "localE.txt"));
			for (Integer t : this.labelMap.keySet()) {
				Atom at = this.mln.getAtom(t);
				if (at.pred.getName().equals("escE")) {
					if (this.labelMap.get(t) == Label.TRUE)
						pw1.println(at.toGroundString(mln));
					else
						pw2.println(at.toGroundString(mln));
				}
			}
			pw1.flush();
			pw1.close();
			pw2.flush();
			pw2.close();

			UIMan.suppressStdout();
			DlogRunner.run();
			UIMan.recoverStdout();

			Set<Pair<Integer, Label>> toBeLabelled = new HashSet<Pair<Integer, Label>>();
			
			File f1 = new File(nichrome.datalog.Config.workDirName + File.separator + "escEDep.txt");
			Scanner sc = new Scanner(f1);
			while (sc.hasNextLine()) {
				String nextLine = sc.nextLine();
				String atomSegs[] = nextLine.split("<");
				String atomBody = atomSegs[1].replaceAll(">", "");
				String toParse = "escE(" + atomBody + ")";
				Atom at = mln.parseAtom(toParse);
				toBeLabelled.add(new Pair<Integer, Label>(mln.getAtomID(at), Label.TRUE));
			}
			sc.close();

			File f2 = new File(nichrome.datalog.Config.workDirName + File.separator + "localEDep.txt");
			System.out.println("Read from "+f2.getAbsolutePath());
			sc = new Scanner(f2);
			while (sc.hasNextLine()) {
				String nextLine = sc.nextLine();
				String atomSegs[] = nextLine.split("<");
				String atomBody = atomSegs[1].replaceAll(">", "");
				String toParse = "escE(" + atomBody + ")";
				Atom at = mln.parseAtom(toParse);
				System.out.println("New false tuple: "+toParse);
				toBeLabelled.add(new Pair<Integer, Label>(mln.getAtomID(at), Label.FALSE));
			}
			sc.close();

			File f3 = new File(nichrome.datalog.Config.workDirName + File.separator + "escEDep.bdd");
			f3.delete();
			f3 = new File(nichrome.datalog.Config.workDirName + File.separator + "localEDep.bdd");
			f3.delete();
			f3 = new File(nichrome.datalog.Config.workDirName + File.separator + "escE.bdd");
			f3.delete();
			f3 = new File(nichrome.datalog.Config.workDirName + File.separator + "localE.bdd");
			f3.delete();

			this.labelAndPropagate(toBeLabelled);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public void labelAndPropagate(Set<Pair<Integer, Label>> labels) {
		List<Integer> workList = new ArrayList<Integer>();
		for (Pair<Integer, Label> p : labels) {
			this.setLabel(p.left, p.right);
			workList.addAll(this.propagate(p.left, p.right));
		}
		while (!workList.isEmpty()) {
			int workAt = workList.remove(workList.size() - 1);
			workList.addAll(this.propagate(workAt, this.getLabel(workAt)));
		}
	}

	public void setOracle(Oracle o) {
		this.o = o;
	}

	private boolean setLabelHelper(int aid, Label l) {
		Label el = labelMap.get(aid);
		boolean result;
		if (el != null && !el.equals(l))
			throw new ConflictingLabelException();
		if (el == null) {
			labelMap.put(aid, l);
			result = true;
		} else {
			result = false;
		}

		if (o != null) {
			Label EL = o.resolve(aid);
			if (EL != null) {
				if (l != EL) {
					System.out.println("ERROR: Inconsistent labels on tuple " + mln.getAtom(aid).toGroundString(mln));
					System.out.println("Expected: " + EL + ", actual: " + l);
				}
			}
		}

		return result;
	}

	private String getPredicateName(int id) {
		Atom at = this.mln.getAtom(Math.abs(id));
		return at.pred.getName();
	}

	private boolean setLabel(int aid, Label l) {
		boolean result = false;
		if (setLabelHelper(aid, l)) {
			result = true;
		}

		/*
		 * // add special check to add more: CVC <--> ptsVH if(
		 * AlarmResolutionDriver.client != null &&
		 * AlarmResolutionDriver.client.equals("pts") ){ Atom at =
		 * this.mln.getAtom(aid); if (at.pred.getName().equals("CVC")) {
		 * Set<InferenceRule> rules = bodyToRules.get(aid); if (rules != null) {
		 * int cnt = 0;
		 * 
		 * for (InferenceRule ir : rules) { String predName =
		 * getPredicateName(ir.getResult()); if (predName.equals("ptsVH")) {
		 * ++cnt; if (setLabelHelper(ir.getResult(), l)) { result = true; } } }
		 * if (cnt > 1) { throw new RuntimeException(
		 * "CVC get involved in more than one ptsVH derivation"); } }
		 * 
		 * } if (at.pred.getName().equals("ptsVH")) { Set<InferenceRule> rules =
		 * headToRules.get(aid);
		 * 
		 * if (rules != null) { int cnt = 0; for (InferenceRule ir : rules) {
		 * String predName = getPredicateName(ir.getResult()); if
		 * (predName.equals("CVC")) { ++cnt; if (setLabelHelper(ir.getResult(),
		 * l)) { result = true; } } } if (cnt > 1) { throw new RuntimeException(
		 * "ptsVH is derived from more than one CVC"); } } } }
		 */

		return result;
	}

	/**
	 * Propagate the labels from one atom. The label map should be updated with
	 * the given label before calling this method. Its main functions: 1.
	 * Propagate the labels to atoms in the same rules. 2. Return the atoms
	 * labeled
	 * 
	 * @param aid
	 * @param l
	 * @return
	 */
	private Set<Integer> propagate(int aid, Label l) {
		if (l == null)
			throw new RuntimeException(
					"Trying to propagate null label from atom " + mln.getAtom(aid).toGroundString(mln));

		Set<Integer> ret = new HashSet<Integer>();

		// Forward propagation can be handled regardless of the parameter label
		Set<InferenceRule> frs = bodyToRules.get(aid);
		if (frs != null) {
			OUT: for (InferenceRule r : frs) {
				if (this.isAtLabeled(r.getResult()))
					continue;
				// Must (forward): TRUE -> TRUE
				if (r.isMust()) {
					if (this.isCondTrue(r) && this.setLabel(r.getResult(), Label.TRUE))
						ret.add(Math.abs(r.getResult()));
				}
				// May (forward): FALSE -> FALSE
				if (r.isMay()) {
					int resultId = Math.abs(r.getResult());
					if (this.isCondFalse(r) && !this.isAtLabeled(resultId)) {
						Set<InferenceRule> preds = this.getHeadToRules().get(resultId);
						if (preds != null) {
							for (InferenceRule pr : preds) {
								if (!this.isCondFalse(pr))
									continue OUT;
							}
						} else {
							throw new RuntimeException("Preds is null, this should not happen. ");
						}
						if (this.setLabel(resultId, Label.FALSE)) {
							ret.add(resultId);
							this.numForwardProp += 1;
						}
					}
				}
			}
		}

		// Backward propagation
		Set<InferenceRule> brs = getHeadToRules().get(aid);
		if (brs != null) {
			switch (l) {
			case TRUE:
				// May (backward): TRUE <- TRUE
				InferenceRule candMayRule = null;
				for (InferenceRule r : brs) {
					if (r.isMay()) {
						if (this.isCondTrue(r)) {// already there's one disjunct
													// validating the result
							candMayRule = null;
							break;
						}
						if (!this.isCondFalse(r)) {// the condition of r is not
													// determined yet
							if (candMayRule != null) {
								candMayRule = null;
								break;
							}
							candMayRule = r;
						}
					}
				}
				if (candMayRule != null) {
					for (int cl : candMayRule.condition) {
						int clAtId = Math.abs(cl);
						if (cl > 0)
							if (this.setLabel(clAtId, Label.TRUE)) {
								ret.add(clAtId);
								this.numBackwardProp += 1;
								this.numBackwardPropTrue += 1;
							}
						if (cl < 0)
							if (this.setLabel(clAtId, Label.FALSE)) {
								ret.add(clAtId);
								this.numBackwardProp += 1;
								this.numBackwardPropFalse += 1;
							}
					}
				}
				break;
			case FALSE:
				// Must (backward): FALSE <- FALSE
				OUT: for (InferenceRule r : brs) {
					if (r.isMust()) {
						Integer unsignedL = null;
						if (this.isCondTrue(r))
							throw new RuntimeException("Conflicted labels.");
						for (int cl : r.getCondition()) {
							int clAtId = Math.abs(cl);
							if (!this.isAtLabeled(clAtId)) {
								if (unsignedL != null)
									continue OUT;
								unsignedL = cl;
							}
						}
						if (unsignedL != null) {
							int unsId = Math.abs(unsignedL);
							ret.add(unsId);
							// set opposite label
							if (unsignedL > 0) {
								this.setLabel(unsId, Label.FALSE);
							} else
								this.setLabel(unsId, Label.TRUE);
						}
					}
				}

				break;
			}
		}

		Atom at = this.mln.getAtom(aid);

		// System.out.println("Propogate " + at.toGroundString(mln) + ", label:
		// " + l.toString());
		// System.out.println("Until now ForwardProp: " + this.numForwardProp +
		// ", BackwardProp: "+ this.numBackwardProp
		// + ", BackwardPropFalse: " + this.numBackwardPropFalse + ",
		// BackwardPropTrue: " + this.numBackwardPropTrue);

		if (l == Label.FALSE) {
			for (Integer r : ret) {
				System.out.println(at.toGroundString(mln) + " ==> " + (mln.getAtom(r).toGroundString(mln)));
			}
		}

		return ret;
	}

	public void checkIfFullyResolvedFP(Oracle o, Set<Integer> queries) {
		for (int q : queries) {
			if (o.resolve(q) == Label.FALSE && this.getLabel(q) != Label.FALSE) {
				System.out.println("Fail to resolve: " + mln.getAtom(q).toGroundString(mln));
				boolean ifAllBodyFalse = true;
				Set<InferenceRule> workList = new ArraySet<InferenceRule>();
				Set<InferenceRule> printed = new HashSet<InferenceRule>();
				workList.addAll(this.getHeadToRules().get(q));
				while (!workList.isEmpty()) {
					InferenceRule r = workList.iterator().next();
					workList.remove(r);
					System.out.println(r.toVerboseString(mln));
					System.out.print("Body label: ");
					for (int b : r.getCondition()) {
						if (b <= 0)
							continue;
						System.out.print(mln.getAtom(b).toGroundString(mln) + ": " + this.getLabel(b) + ", ");
						if (this.getLabel(b) == null) {
							Set<InferenceRule> hrs = this.getHeadToRules().get(b);
							if (hrs != null)
								for (InferenceRule r1 : hrs)
									if (printed.add(r1))
										workList.add(r1);
						}
						else if(this.getLabel(b) == Label.FALSE)
							System.out.println("WOWOWOWOW");
					}
					System.out.println();
				}
			}
		}
	}

	public void analyze(Oracle o) {
		Set<Integer> falseTuples = new HashSet<Integer>();
		System.out.println("Number of rules: " + rules.size());
		for (InferenceRule r : this.rules) {
			for (int l : r.getCondition()) {
				if (l > 0 && o.resolve(l) == Label.FALSE)
					falseTuples.add(l);
			}
			int result = r.getResult();
			if (o.resolve(result) == Label.FALSE)
				falseTuples.add(result);
		}
		this.analyze(falseTuples);
	}

	public Set<Integer> getAllAtoms() {
		Set<Integer> ret = new HashSet<Integer>();
		for (InferenceRule r : this.rules) {
			for (int l : r.getCondition()) {
				ret.add(Math.abs(l));
			}
			int result = r.getResult();
			ret.add(Math.abs(result));
		}
		return ret;
	}

	public void analyze(Set<Integer> falseTuples) {
		Map<Integer, Label> oldLabelMap = this.labelMap;
		this.labelMap = new HashMap<Integer, Label>();
		Set<Pair<Integer, Label>> labels = new ArraySet<Pair<Integer, Label>>();
		for (int t : falseTuples)
			labels.add(new Pair<Integer, Label>(t, Label.FALSE));
		this.labelAndPropagate(labels);

		String ruleOut = Config.dir_out + File.separator + "gc_verbose.txt";
		try {
			PrintWriter pw = new PrintWriter(new File(ruleOut));
			for (InferenceRule r : this.rules)
				pw.println(r.toVerboseString(mln));
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		String flout = Config.dir_out + File.separator + "false_tuples.txt";
		try {
			PrintWriter pw = new PrintWriter(new File(flout));
			for (int at : this.getAllAtoms())
				if (this.getLabel(at) == Label.FALSE)
					pw.println(mln.getAtom(at).toGroundString(mln));
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		String frOut = Config.dir_out + File.separator + "false_rules.txt";
		try {
			PrintWriter pw = new PrintWriter(new File(frOut));
			for (InferenceRule r : this.rules)
				if (!isCondFalse(r) && this.getLabel(r.getResult()) == Label.FALSE)
					pw.println("False rule: " + r.toVerboseString(mln));
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		this.labelMap = oldLabelMap;
	}

	private boolean isCondFalse(InferenceRule r) {
		for (int l : r.getCondition()) {
			if (l > 0 && this.getLabel(l) == Label.FALSE)
				return true;
			if (l < 0 && this.getLabel(l) == Label.TRUE)
				return true;
		}
		return false;
	}

	private boolean isCondTrue(InferenceRule r) {
		for (int l : r.getCondition()) {
			if (this.getLabel(l) == null)
				return false;
			if (l > 0 && this.getLabel(l) == Label.FALSE)
				return false;
			if (l < 0 && this.getLabel(l) == Label.TRUE)
				return false;
		}
		return true;
	}

	public boolean isAtLabeled(int l) {
		return this.labelMap.containsKey(Math.abs(l));
	}

	public Label getLabel(int l) {
		return this.labelMap.get(Math.abs(l));
	}

	/*
	 * Generate the graph given the ground clauses. The return value is a pair
	 * of maps <headTuple->clauses, bodyTuple->clauses>
	 */
	protected Pair<Map<Integer, Set<InferenceRule>>, Map<Integer, Set<InferenceRule>>> generateGraph(
			Set<InferenceRule> rs) {
		UIMan.verbose(2, "Generating Graph");
		Map<Integer, Set<InferenceRule>> headToC = new HashMap<Integer, Set<InferenceRule>>();
		Map<Integer, Set<InferenceRule>> bodyToC = new HashMap<Integer, Set<InferenceRule>>();
		for (InferenceRule r : rs) {
			int headId = Math.abs(r.getResult());
			for (int atmId : r.getCondIds()) {
				if (atmId != headId) {
					// Body tuple.
					atmId = Math.abs(atmId);
					Set<InferenceRule> succ = bodyToC.get(atmId);
					if (succ == null) {
						succ = new HashSet<InferenceRule>();
						bodyToC.put(atmId, succ);
					}
					succ.add(r);
				}
			}
			Set<InferenceRule> pred = headToC.get(headId);
			if (pred == null) {
				pred = new HashSet<InferenceRule>();
				headToC.put(headId, pred);
			}
			pred.add(r);
		}
		UIMan.verbose(2, "Leave graph generation.");
		return new Pair<Map<Integer, Set<InferenceRule>>, Map<Integer, Set<InferenceRule>>>(headToC, bodyToC);
	}

	public Map<Integer, Set<InferenceRule>> getHeadToRules() {
		return headToRules;
	}

	public void setHeadToRules(Map<Integer, Set<InferenceRule>> headToRules) {
		this.headToRules = headToRules;
	}
	
	public int getNumRules(){
		return rules.size();
	}

}

class ConflictingLabelException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6776360911900487363L;

}

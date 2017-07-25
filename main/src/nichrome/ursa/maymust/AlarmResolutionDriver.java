package nichrome.ursa.maymust;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.Pair;

import nichrome.mln.Atom;
import nichrome.mln.Clause;
import nichrome.mln.GClause;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.Predicate;
import nichrome.mln.db.RDB;
import nichrome.mln.parser.CommandOptions;
import nichrome.mln.util.Config;
import nichrome.mln.util.FileMan;
import nichrome.mln.util.UIMan;
import nichrome.ursa.maymust.InferenceRule.Kind;
import nichrome.ursa.maymust.payoff.ExternalModel;
import nichrome.ursa.maymust.payoff.MIQPSolver;
import nichrome.ursa.maymust.payoff.Model;
import nichrome.ursa.maymust.payoff.ModelGuidedPicker;
import nichrome.ursa.maymust.payoff.OptimizationSolver;
import nichrome.ursa.maymust.payoff.OracleModel;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class AlarmResolutionDriver {
	private MarkovLogicNetwork mln;
	private CommandOptions options;
	protected PrintWriter logOut;
	protected double hardWeight = Config.hard_weight;
	private Set<Integer> trueHardEvidence = new HashSet<Integer>();
	private Set<Integer> falseHardEvidence = new HashSet<Integer>();

	private Set<GClause> groundedClauses;
	private Set<GClause> forwardClauses;
	protected Set<Integer> queries;
	protected Set<Integer> questions;

	private Set<Integer> evidenceTuples = new HashSet<Integer>();
	private Set<Pair<Integer, Label>> preLabelled = new HashSet<Pair<Integer, Label>>();
	public static String client = null;
	
	private String externalModelPath = null;
	
	private int picker;
	private Oracle o;
	private Set<Integer> appQuestions;
	private boolean offPre;

	public void init(CommandOptions options) {
		Clause.mappingFromID2Const = new HashMap<Integer, String>();
		Clause.mappingFromID2Desc = new HashMap<String, String>();
		UIMan.println(">>> Connecting to RDBMS at " + Config.db_url);
		RDB db = RDB.getRDBbyConfig();
		db.resetSchema(Config.db_schema);
		logOut = FileMan.getPrintWriter(options.fout);
		this.options = options;
		mln = new MarkovLogicNetwork();
		mln.setDB(db);
		String[] progFiles = options.fprog.split(",");
		mln.loadPrograms(progFiles);
		String[] eviFiles = options.fevid.split(",");
		mln.loadEvidences(eviFiles);
		mln.materializeTables();
		mln.prepareDB(db);
		client = options.ursaClient;
		// Load all pre labeled tuples
		this.loadPreLabelledTuples(options.ursaPreLabelledTuples);

		// Preparing stage, create all the constraints
		this.loadQueries(options.fquery);
		this.loadQuestions(options.ursaSearchSpace);
		this.loadAppQuestions(options.appQuestionFile);
		this.fillUniverse();

		// this.eliminateCycles(this.groundedClauses);
		forwardClauses = this.groundedClauses;

		// this.eliminateBackEdges(this.groundedClauses);
		this.externalModelPath = options.ursaExtermalModel;
		this.picker = options.picker;
		this.o = null;
		this.offPre = options.turnOffPre;
	}

	// Some pruning operation. Be very careful when the correlation rules are
	// involved
	private Set<Integer> getAllRelatedTuples(AnalysisWithAnnotations a, Set<Integer> initSet) {
		Set<Integer> rs = new HashSet<Integer>();
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
		}

		return rs;
	}
	
	private void printInputStatistics(){	
		int numTrueReports = 0;
		int numFalseReports = 0;
		int numTrueQuestions = 0;
		int numFalseQuestions = 0;
		for(int q : this.questions)
			if(this.getOracle().resolve(q) == Label.FALSE)
				numFalseQuestions++;
			else
				numTrueQuestions++;
		for(int r : this.queries)
			if(this.getOracle().resolve(r) == Label.FALSE)
				numFalseReports++;
			else
				numTrueReports++;
		System.out.println("Statistics of questions, true: "+numTrueQuestions+", false: "+numFalseQuestions);			
		System.out.println("Statistics of reports, true: "+numTrueReports+", false: "+numFalseReports);
		
				// relevant questions
		Set<Integer> relevantTuples = this.getAllRelatedTuples(this.constructAnnotatedAnalysis(), this.queries);
		relevantTuples.retainAll(this.questions);
		int relNumTrueQuestions = 0;
		int relNumFalseQuestions = 0;
		for(int q : relevantTuples)
			if(this.getOracle().resolve(q) == Label.FALSE)
				relNumFalseQuestions ++;
			else
				relNumTrueQuestions ++;
		System.out.println("Statistics of relevant questions, true: " + relNumTrueQuestions + ", false: " + relNumFalseQuestions);
		// relevant app questions
		int relAppTrueQuestions = 0;
		int relAppFalseQuestions = 0;
		for(int q : relevantTuples){
			if(this.appQuestions!=null && !this.appQuestions.contains(q))
				continue;
			if(this.getOracle().resolve(q) == Label.FALSE)
				relAppFalseQuestions ++;
			else
				relAppTrueQuestions ++;
		}
		System.out.println("Statistics of relevant app questions, true: " + relAppTrueQuestions + ", false: " + relAppFalseQuestions);
	}
	
	private void printOutputStatistics(AnalysisWithAnnotations output){
		System.out.println("False reports that fail to to resolve: ");
		for(int r : this.queries)
			if(this.getOracle().resolve(r) == Label.FALSE && !output.isAtLabeled(r))
				System.out.println(mln.getAtom(r).toGroundString(mln));
		
		{// for experiment of introducing random noise
			int overall_queries = this.queries.size();
			//int resolved = output.labelMap.size();
			int false_positive = 0;
			int false_negative = 0;
			int labels = 0;
			
			int labelNull_oracle_true = 0;
			int labelNull_oracle_false = 0;
			for(int r : this.queries){
				Label l = output.getLabel(r);
				if(l == null) {
					//if(this.getOracle().resolve(r) == Label.TRUE) {
					if(this.getOracle().resolve(r) != Label.FALSE) {
								++ labelNull_oracle_true;
					}
					if(this.getOracle().resolve(r) == Label.FALSE) {
						++ labelNull_oracle_false;
					}
					continue;
				}
				
				++labels;
				
				if(this.getOracle().resolve(r) == Label.FALSE && l == Label.TRUE) {
					++false_positive;
				}
				//if(this.getOracle().resolve(r) == Label.TRUE && l == Label.FALSE) {
				if(this.getOracle().resolve(r) != Label.FALSE && l == Label.FALSE) {
					++false_negative;
				}
			}
			
			System.out.println("Overall_queries: " + overall_queries + " labels: " + labels 
					+ " #FP: " + false_positive + " #FN: " + false_negative 
					+ " labelNull_oracle_true: " + labelNull_oracle_true + " labelNull_oracle_false: " + labelNull_oracle_false);
		}
	}
	
	public void run() {	
		this.printInputStatistics();
		AnalysisWithAnnotations anAnalysis = this.constructAnnotatedAnalysis();
		this.releaseMemory();
		Oracle o = this.getOracle();
		QuestionPicker picker = this.getPicker(anAnalysis);
		AlarmResolver resolver = new AlarmResolver(anAnalysis, o, picker, this.queries, mln,this.offPre);
		resolver.prepolulate(preLabelled);
		resolver.resolve();
		System.out.println("AlarmResolver: Num of queries resolved " + resolver.getNumResolvedQueries());
		System.out.println("AlarmResolver: Num of questions " + resolver.getQuestionCount());
		Map<Integer, Label> result = resolver.getLabelMap();
		for (Integer a : result.keySet()) {
			Label l = result.get(a);
			String ls = l == Label.TRUE ? "T" : "F";
			Atom at = mln.getAtom(a);
			logOut.println(at.toGroundString(mln) + " " + ls);
		}
		logOut.flush();
		logOut.close();

		int queryAsked = 0;
		Set<Integer> questionsAsked = resolver.getQuestionsAsked();
		for (Integer atId : questionsAsked) {
			if (this.queries.contains(atId))
				queryAsked++;
		}
		System.out.println("AlarmResolver: Number of queries asked as questions " + queryAsked);
		// TODO print out the result and statistics
		this.printOutputStatistics(anAnalysis);
	}

	private Set<InferenceRule> ruleCache = null;
	
	protected AnalysisWithAnnotations constructAnnotatedAnalysis() {
		if (ruleCache == null) {
			ruleCache = new HashSet<InferenceRule>();
			OUT: for (GClause gc : this.forwardClauses) {
				int result = this.getHead(gc);
				List<Integer> body = new ArrayList<Integer>();
				for (int l : gc.lits) {
					if (Math.abs(l) == result) {
						if (l > 0)
							continue;
						if (l < 0)
							continue OUT; // trivial rule: a /\ b => a
					}
					body.add(0 - l);
				}
				InferenceRule r = new InferenceRule(result, body, this.getRuleKind(gc));
				ruleCache.add(r);
			}
		}
		return new AnalysisWithAnnotationsCycle(ruleCache, mln);
	}

	protected Oracle getOracle() {
		if(o!= null)
			return o;
		ChordOracle co = new ChordOracle();
		co.init(mln, options);
		AnalysisWithAnnotations annoatedAnalyzer = this.constructAnnotatedAnalysis();
		o = new PropagatedOracle(co, annoatedAnalyzer);
		return o;
	}

	protected QuestionPicker getPicker(AnalysisWithAnnotations analysis) {
		// TODO : depending on option, should change the object returned
		// ReverseBFSPicker rbp = new ReverseBFSPicker();
		// rbp.init(mln, options);
		// return rbp;
		
		switch (this.picker) {
		case Config.MODEL_PICKER:
			OptimizationSolver solver = new MIQPSolver();

			// Decide what model to use
			Model m = null;
			if(externalModelPath != null)
				m = new ExternalModel(mln, externalModelPath);
			else{
				UIMan.verbose(0, "No file for model provided, use oracle as the model.");
				m = new OracleModel(this.getOracle());
			}
			boolean encodeCorre = false;
			if (client.equals("datarace")) {
				System.out.println("Encode correlation.");
				encodeCorre = true;
			}

			ModelGuidedPicker picker = new ModelGuidedPicker(mln, solver, m, encodeCorre, questions, queries, analysis);

			picker.setBudget(1);

			return picker;

		case Config.RANDOM_PICKER:
			RandomPicker rp = new RandomPicker();
			rp.init(mln, options);
			return rp;
		
			default:
				throw new RuntimeException("Unknown picker option "+this.picker);
		}

		


	}

	/**
	 *
	 * Release the memory held by the MLN data structures.
	 */
	protected void releaseMemory() {
		this.evidenceTuples = null;
		this.falseHardEvidence = null;
		this.forwardClauses = null;
		this.groundedClauses = null;
		this.trueHardEvidence = null;
		System.gc();
	}

	/**
	 * Return whether a rule is a may rule, must rule, or a precise rule. Now
	 * just treat every rule as a may rule.
	 * 
	 * @param gc
	 * @return
	 */
	protected Kind getRuleKind(GClause gc) {
		return Kind.MAY;
	}

	// Assuming here that negations in the body are only on EDB tuples
	private int getHead(GClause gc) {
		int headId = 0;
		for (int atmId : gc.lits) {
			if (atmId > 0 && !evidenceTuples.contains(Math.abs(atmId))) {
				Atom at = mln.getAtom(Math.abs(atmId));
				if(at.pred.isClosedWorld()) //close-world tuples are usually input tuples
					continue;
				if(headId != 0)
					throw new RuntimeException("Unable to locate the head of clause: "+gc.toConstraintString(mln));
				headId = atmId;
			}
		}
		if (headId == 0)
			throw new RuntimeException("No head found for ground clause: " + gc.toVerboseString(mln));
		return headId;
	}

	// Assuming here that negations in the body are only on EDB tuples
	private int getSomeBody(GClause gc) {
		int headId = 0;
		int bodyId = 0;
		for (int atmId : gc.lits) {
			if (atmId > 0 && !evidenceTuples.contains(Math.abs(atmId))) {
				headId = atmId;
			} else {
				bodyId = Math.abs(atmId);
			}
		}
		if (bodyId == 0)
			throw new RuntimeException("Ground clause is a unit clause: " + gc.toVerboseString(mln));
		return bodyId;
	}

	public static void main(String[] args) {
		// test eliminateBackEdges()

		AlarmResolutionDriver ard = new AlarmResolutionDriver();
		Set<Integer> edb = new HashSet<Integer>();
		Set<GClause> rules = new HashSet<GClause>();

		ard.evidenceTuples = edb;

		{ // test-case 1
			// edb
			edb.add(1);
			edb.add(2);

			int[] a = { -1, -2, 3 };
			int[] b = { -3, 4 };
			int[] c = { -4, 3 };

			rules.add(new GClause(1, a));
			rules.add(new GClause(1, b));
			rules.add(new GClause(1, c));
		}

		{ // test-case 2

			edb.clear();
			edb.add(1);
			edb.add(2);

			int[] a = { -1, 3 };
			int[] d = { -2, 4 };
			int[] b = { -3, 4 };
			int[] c = { -4, 3 };

			rules.clear();
			rules.add(new GClause(1, a));
			rules.add(new GClause(1, d));
			rules.add(new GClause(1, b));
			rules.add(new GClause(1, c));
		}

		{ // test-case 3

			edb.clear();
			edb.add(1);
			edb.add(2);
			edb.add(5);

			int[] a = { -1, 3 };
			int[] d = { -2, 4 };
			int[] b = { -3, 4 };
			int[] c = { -4, -5, 3 };

			rules.clear();
			rules.add(new GClause(1, a));
			rules.add(new GClause(1, d));
			rules.add(new GClause(1, b));
			rules.add(new GClause(1, c));
		}

		{ // test-case 4

			edb.clear();
			edb.add(1);
			edb.add(2);
			edb.add(5);

			int[] a = { -1, 3 };
			int[] d = { -2, 4 };
			int[] b = { -3, 4 };
			int[] c = { -4, 5, 3 };

			rules.clear();
			rules.add(new GClause(1, a));
			rules.add(new GClause(1, d));
			rules.add(new GClause(1, b));
			rules.add(new GClause(1, c));
		}

		System.out.println("Before elimination:");
		for (GClause gc : rules) {
			System.out.println(gc);
		}
		ard.eliminateBackEdges(rules);
		System.out.println("After elimination:");
		for (GClause gc : ard.forwardClauses) {
			System.out.println(gc);
		}
	}

	void eliminateBackEdges(Set<GClause> clauses) {
		forwardClauses = new HashSet<GClause>();
		Pair<Map<Integer, Set<GClause>>, Map<Integer, Set<GClause>>> clauseMaps = generateGraph(clauses);
		Map<Integer, Set<GClause>> headToClausesT = clauseMaps.left;
		// Map<Integer, Set<GClause>> bodyToClausesT = clauseMaps.right;

		Map<Integer, Set<Integer>> dom = new HashMap<Integer, Set<Integer>>();

		for (Integer key : evidenceTuples) {
			Set<Integer> val = new HashSet<Integer>();
			val.add(0);
			val.add(key);
			dom.put(key, val);
		}

		List<Integer> order = new LinkedList<Integer>();
		Set<Integer> in_order = new HashSet<Integer>();

		in_order.addAll(evidenceTuples);

		int n = headToClausesT.size();
		while (order.size() < n) {
			for (Integer key : headToClausesT.keySet()) {
				if (in_order.contains(key)) {
					continue;
				}

				Boolean derived = false;
				for (GClause gc : headToClausesT.get(key)) {
					if (canDerive(gc, in_order)) {
						derived = true;
						break;
					}
				}

				if (derived) {
					order.add(key);
					in_order.add(key);
				}
			}
		}

		// System.out.println("after initialization, dom: " + dom);

		Boolean updated = true;
		int iter = 0;
		while (updated) {
			++iter;
			System.out.println("Back edge elimination Iteration: " + iter);

			updated = false;

			for (Integer key : order) {
				Set<Integer> val = dom.get(key);

				Set<Integer> new_val = null;
				for (GClause clause : headToClausesT.get(key)) {
					Set<Integer> clause_dom = getClauseDominator(clause, dom);
					// System.out.println("clause: " + clause);
					// System.out.println("clause_dom: " + clause_dom);

					if (clause_dom == null) {
						continue;
					}

					if (new_val == null) {
						new_val = clause_dom;
					} else {
						new_val.retainAll(clause_dom);
					}
				}

				// System.out.println("For key: " + key + ", new val: " +
				// new_val);

				if (new_val == null) {
					if (val != null) {
						System.err.println("Error: new_val becomes Top, which should not happen.");
						throw new RuntimeException("Error: new_val becomes Top, which should not happen.");
					}
				} else {
					new_val.add(key); // add itself as a dominator

					if (isChanged(val, new_val)) {
						dom.put(key, new_val);
						updated = true;
					}
				}
			}
		}

		// System.out.println("after propagation, dom: " + dom);

		for (GClause gc : clauses) {
			if (isBackEdge(gc, dom) == false) {
				forwardClauses.add(gc);
			}
		}

		System.out.println("clause.size = " + clauses.size() + ", forwardClauses.size = " + forwardClauses.size());
	}

	private Boolean canDerive(GClause clause, Set<Integer> st) {
		int head = getHead(clause);

		for (int atmId : clause.lits) {
			if (atmId == head)
				continue;
			atmId = Math.abs(atmId);
			if (!st.contains(atmId)) {
				return false;
			}
		}
		return true;
	}

	private Boolean isBackEdge(GClause clause, Map<Integer, Set<Integer>> dom) {
		int head = getHead(clause);

		for (int atmId : clause.lits) {
			if (atmId == head)
				continue;

			atmId = Math.abs(atmId);
			Set<Integer> D = dom.get(atmId);
			if (D == null) {
				System.err.println("Error: there should not exist any top element at this point");
				throw new RuntimeException("Error: there should not exist any top element at this point");
				// continue;
			}

			if (D.contains(head)) {
				return true;
			}
		}

		return false;
	}

	private Boolean isChanged(Set<Integer> V, Set<Integer> new_V) {
		if (V == null || new_V == null) {
			return V != new_V;
		}
		if (V.size() != new_V.size()) {
			return true;
		}

		for (Integer x : V) {
			if (new_V.contains(x) == false) {
				return true;
			}
		}

		return false;
	}

	private Set<Integer> getClauseDominator(GClause clause, Map<Integer, Set<Integer>> dom) {
		Set<Integer> clause_dom = new HashSet<Integer>();
		int head = getHead(clause);
		for (int atmId : clause.lits) {
			if (head == atmId) {
				continue;
			}

			atmId = Math.abs(atmId);
			if (dom.get(atmId) == null) { // we use null to represent the Top
				// System.out.println("atmId: " + atmId + ", related dom: " +
				// dom.get(atmId));
				clause_dom = null;
				break;
			}

			clause_dom.addAll(dom.get(atmId));
		}

		return clause_dom;
	}

	protected void eliminateCycles(Set<GClause> clauses) {
		forwardClauses = new HashSet<GClause>();
		Pair<Map<Integer, Set<GClause>>, Map<Integer, Set<GClause>>> clauseMaps = generateGraph(clauses);
		Map<Integer, Set<GClause>> headToClausesT = clauseMaps.left;
		Map<Integer, Set<GClause>> bodyToClausesT = clauseMaps.right;

		TIntSet ats = this.getAllAtoms(clauses);
		TIntSet srcSet = new TIntHashSet(ats);
		srcSet.removeAll(headToClausesT.keySet());

		Map<Integer, List<GClause>> watch = new HashMap<Integer, List<GClause>>();
		Set<Integer> justified = new HashSet<Integer>();
		Set<Integer> now = new HashSet<Integer>();
		Set<Integer> nxt = new HashSet<Integer>();

		TIntIterator seenIter = srcSet.iterator();
		while (seenIter.hasNext()) {
			int atId = Math.abs(seenIter.next());
			nxt.add(atId);
		}

		for (GClause c : clauses) {
			if (c.lits.length == 1) {
				nxt.add(getHead(c));
				forwardClauses.add(c);
			} else {
				int bodyT = getSomeBody(c);
				List<GClause> cs = watch.get(bodyT);
				if (cs == null) {
					cs = new ArrayList<GClause>();
					watch.put(bodyT, cs);
				}
				cs.add(c);
			}
		}

		while (!nxt.isEmpty()) {
			{
				Set<Integer> tmpSI = now;
				now = nxt;
				nxt = tmpSI;
				nxt.clear();
			}
			justified.addAll(now);
			for (Integer x : now) {
				List<GClause> ws = watch.get(x);
				if (ws == null)
					continue;
				for (GClause w : ws) {
					int y = getHead(w);
					if (justified.contains(y))
						continue; // w isn't a forward clause
					boolean allLitPresent = true;
					int missingLit = 0;
					for (int gcLit : w.lits) {
						gcLit = Math.abs(gcLit);
						if (gcLit == y)
							continue;
						if (!justified.contains(gcLit)) {
							allLitPresent = false;
							missingLit = gcLit;
							break;
						}
					}
					if (!allLitPresent) {
						List<GClause> zs = watch.get(missingLit);
						if (zs == null) {
							zs = new ArrayList<GClause>();
							watch.put(missingLit, zs);
						}
						zs.add(w);
					} else {
						nxt.add(y);
						forwardClauses.add(w);
					}
				}
			}
		}
		forwardClauses = clauses;
	}

	/*
	 * Generate the graph given the ground clauses
	 */
	protected Pair<Map<Integer, Set<GClause>>, Map<Integer, Set<GClause>>> generateGraph(Set<GClause> groundClauses) {
		UIMan.verbose(2, "Generating Graph");
		Map<Integer, Set<GClause>> headToC = new HashMap<Integer, Set<GClause>>();
		Map<Integer, Set<GClause>> bodyToC = new HashMap<Integer, Set<GClause>>();
		for (GClause gc : groundClauses) {
			int headId = getHead(gc);
			for (int atmId : gc.lits) {
				atmId = Math.abs(atmId);
				if (atmId != headId) {
					// Body tuple.
					Set<GClause> succ = bodyToC.get(atmId);
					if (succ == null) {
						succ = new HashSet<GClause>();
						bodyToC.put(atmId, succ);
					}
					succ.add(gc);
				} else {
					Set<GClause> pred = headToC.get(atmId);
					if (pred == null) {
						pred = new HashSet<GClause>();
						headToC.put(atmId, pred);
					}
					pred.add(gc);
				}
			}
		}
		UIMan.verbose(2, "Leave graph generation.");
		return new Pair<Map<Integer, Set<GClause>>, Map<Integer, Set<GClause>>>(headToC, bodyToC);
	}

	/**
	 * Create the set of constraints that we need to solve.
	 */
	protected void fillUniverse() {
		this.groundedClauses = new HashSet<GClause>();
		if (Config.gcLoadFile != null) {
			UIMan.println("Loading grounded clauses from file " + Config.gcLoadFile + ".");
			try {
				FileInputStream in = FileMan.getFileInputStream(Config.gcLoadFile);
				this.loadGroundedConstraints(in);
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if (Config.revLoadFile != null) {
				throw new RuntimeException("No reverse constraints needed in a hard analysis");
			}
		} else {
			throw new RuntimeException("Need to implement constraints grounding without warm start!");
		}
		TIntSet atoms = this.getAllAtoms(this.groundedClauses);
		this.loadEvidenceConstraints(atoms);
	}

	// TODO: Scanner has poor performance, if we observe it to be a bottleneck,
	// let us change it.
	public void loadQueries(String fPath) {
		this.queries = new HashSet<Integer>();
		try {
			Scanner sc = new Scanner(new File(fPath));
			while (sc.hasNextLine()) {
				String nextLine = sc.nextLine();
				if (nextLine.startsWith("//"))
					continue;
				Atom at = mln.parseAtom(nextLine);
				this.queries.add(mln.getAtomID(at));
			}
			sc.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void loadQuestions(String qPath) {
		this.questions = new HashSet<Integer>();
		try {
			Scanner sc = new Scanner(new File(qPath));
			while (sc.hasNextLine()) {
				String nextLine = sc.nextLine();
				if (nextLine.startsWith("//"))
					continue;
				Atom at = mln.parseAtom(nextLine);
				this.questions.add(mln.getAtomID(at));
			}
			sc.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void loadAppQuestions(String qPath) {
		if(qPath == null)
			return;
		this.appQuestions = new HashSet<Integer>();
		try {
			Scanner sc = new Scanner(new File(qPath));
			while (sc.hasNextLine()) {
				String nextLine = sc.nextLine();
				if (nextLine.startsWith("//"))
					continue;
				Atom at = mln.parseAtom(nextLine);
				this.appQuestions.add(mln.getAtomID(at));
			}
			sc.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Load evidences as part of the grounded constraints. Note for closed-world
	 * predicate, the negative evidence is loaded if it is used in other
	 * constraints.
	 * 
	 * @param atoms
	 *            the atoms used in
	 */
	private void loadEvidenceConstraints(TIntSet atoms) {
		for (Predicate p : mln.getAllPred()) {
			for (Atom at : p.getHardEvidences()) {
				int atId = mln.getAtomID(at.base());
				if (at.truth) {
					this.trueHardEvidence.add(atId);
					this.evidenceTuples.add(atId);
					// GClause egc = new GClause(Config.hard_weight, atId);
					// this.groundedClauses.add(egc);
				} else {
					this.falseHardEvidence.add(atId);
					this.evidenceTuples.add(atId);
					// GClause egc = new GClause(Config.hard_weight, -atId);
					// this.groundedClauses.add(egc);
				}
			}

			for (Atom at : p.getSoftEvidences()) {
				throw new RuntimeException("No soft evidence in a hard analysis.");
			}
		}

		TIntIterator iter = atoms.iterator();
		while (iter.hasNext()) {
			int at = iter.next();
			Atom atom = mln.getAtom(at);
			Predicate p = atom.pred;
			if (p.isClosedWorld()) {
				if (!this.trueHardEvidence.contains(at) && !this.falseHardEvidence.contains(atom)) {
					this.evidenceTuples.add(at);
					// GClause egc = new GClause(Config.hard_weight, -at);
					// this.groundedClauses.add(egc);
				}
			}
		}
	}

	/**
	 * Example of accepted format: 1.0E7: NOT reachableCM(0,0), NOT
	 * MobjVarAsgnInst(0,25,24), RobjVarAsgnInst(0,25,24)
	 *
	 * @param in
	 */
	private void loadGroundedConstraints(InputStream in) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("//"))
					continue;
				String[] parts = line.split(": ");
				double weight;
				if (parts[0].equals("infi")) {
					weight = Config.hard_weight;
				} else {
					weight = Double.parseDouble(parts[0]);
				}
				String litStrs[] = parts[1].split(", ");
				int lits[] = new int[litStrs.length];
				for (int i = 0; i < litStrs.length; i++) {
					String litSegs[] = litStrs[i].split(" ");
					boolean isNeg = (litSegs.length > 1);
					String atomStr = litSegs[litSegs.length - 1];
					Atom at = this.mln.parseAtom(atomStr);
					int atId = this.mln.getAtomID(at);
					if (isNeg) {
						atId = 0 - atId;
					}
					lits[i] = atId;
				}
				GClause gc = this.mln.matchGroundedClause(weight, lits);
				if (gc != null) {
					this.groundedClauses.add(gc);
				}
				else
					UIMan.verbose(3,"Warning: fail to match "+line);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Load pre-labeled tuples. Format: <tuple> {T|F}
	 */
	private void loadPreLabelledTuples(String file) {
		if (file == null)
			return;
		try {
			FileInputStream in = FileMan.getFileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("//"))
					continue;
				line = line.trim();
				if (line.equals(""))
					continue;
				String[] tokens = line.split(" ");
				Atom at = this.mln.parseAtom(tokens[0]);
				int atId = this.mln.getAtomID(at);
				Label l = null;
				if (tokens[1].equals("T"))
					l = Label.TRUE;
				else
					l = Label.FALSE;
				preLabelled.add(new Pair<Integer, Label>(atId, l));
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Loading Pre Labelled file failed with " + e.toString());
		}
	}
	
	private TIntSet getAllAtoms(Set<GClause> gcs){
		TIntSet ret = new TIntHashSet();
		for(GClause gc : gcs){
			for(int literal : gc.lits)
				ret.add(Math.abs(literal));
		}
		return ret;
	}
}

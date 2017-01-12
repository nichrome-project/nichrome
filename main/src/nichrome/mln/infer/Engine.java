package nichrome.mln.infer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Pair;

import nichrome.mln.Atom;
import nichrome.mln.Clause;
import nichrome.mln.GClause;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.Predicate;
import nichrome.mln.Clause.ClauseInstance;
import nichrome.mln.db.RDB;
import nichrome.mln.util.Config;
import nichrome.mln.util.Timer;
import nichrome.mln.util.UIMan;

public class Engine {
	private MarkovLogicNetwork mln;
	private RDB db;
	private LazyGrounder grounder;
	private LazySolver solver;
	private double curObj;
	private double finalObj;
	private double finalCost;
	private Set<Integer> optSolution;
	private Set<Integer> optLastSolution;
	private List<Set<Integer>> allSolutions;
	private List<Set<Integer>> allLastSolutions;
	private boolean hasHardClauseViolated;
	private boolean timeout;

	public Engine(MarkovLogicNetwork mln, RDB db) {
		this.mln = mln;
		this.db = db;
		this.grounder = new LazyGrounder(db, mln);
		if (Config.solver.equals(Config.ILP_SOLVER)) {
			this.solver = new LazySolverILP(mln);
		} else if (Config.solver.equals(Config.LBX_SOLVER)){
			this.solver = new LazySolverLBX(mln);
		} else if (Config.solver.equals(Config.MCSLS_SOLVER)) {
			this.solver = new LazySolverMCSls(mln);
		} else if (Config.solver.equals(Config.WALK_SOLVER)) {
			this.solver = new LazySolverWalkSAT(mln);
		} else if (Config.solver.equals(Config.TUFFY_SOLVER)) {
			this.solver = new LazySolverT(mln);
		} else if (Config.solver.equals(Config.TWO_STAGE_SOLVER)) {
			this.solver = new LazySolverTwoStage(mln);
		} else if (Config.solver.equals(Config.LBX_MCS_SOLVER)){
			this.solver = new LazySolverLBXMCS(mln);
		} else if (Config.solver.equals(Config.INCREMENTAL_SOLVER)) {
			this.solver = new LazySolverIncrementalMaxSAT(mln);			
		}
		else {
			System.out.println("Will use default option, cannot find a match for solver config: " + Config.solver);
			this.solver = new LazySolverMaxSAT(mln);
		}
		this.hasHardClauseViolated = true;
		this.allSolutions = new ArrayList<Set<Integer>>();
	}

	public boolean run() {
		if(Config.checkSolutionPath != null){
			return this.run_check();
		}
		
		if (Config.blocking_mode) {
			boolean ret = true;
			List<Set<Integer>> allCurrentSolutions = new ArrayList<Set<Integer>>();
			for (int i = 0; i < Config.num_solver_solutions; ++i) {
				ret = run_internal();
				if (!ret) {
					if (i == 0) {
						return false;
					} else {
						ret = true;
						break;
					}
				}
				this.grounder.blockSolutions(this.allSolutions);
				allCurrentSolutions.addAll(this.allSolutions);
				clearAll();
			}
			this.grounder.clearBlockedSolutions();
			this.allSolutions = allCurrentSolutions;
			this.optSolution = this.allSolutions.get(0);
			this.finalObj = this.grounder.evaluate(this.optSolution);
			UIMan.verbose(1, "Optimum sum of weights on fully grounded rules: "
				+ this.finalObj);
			this.finalCost = this.grounder.evaluateCost(this.optSolution);
			UIMan.verbose(1, "Cost of solution: "
				+ this.finalCost);
			return ret;
		} else {
			return run_internal();
		}
	}
	
	public boolean run_check(){
		try {
			UIMan.verbose(0, "Check the solution loaded from "+Config.checkSolutionPath+".");
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(Config.checkSolutionPath)));
			this.optSolution = new HashSet<Integer>();
			String line;
			while ((line = br.readLine()) != null) {
				line = line.replaceAll("\"", "");
				Atom at = mln.parseAtom(line);
				if(at == null){
					UIMan.verbose(0, "Fail to find the related atom for "+line+".");
				}
				int atId = mln.getAtomID(at);
				optSolution.add(atId);
			}
			br.close();
			this.grounder.ground(optSolution);
			int numViolatedHardClauses = 0;
			double cost = 0.0;
			for(GClause gc : this.grounder.getGroundedClauses())
				if(!gc.isSatisfiedBy(optSolution)){
					if(Config.verbose_level >= 3)
						UIMan.verbose(3, "Rule violated: "+gc.toVerboseString(mln));
					if(gc.isHardClause())
						numViolatedHardClauses ++;
					else
						cost += gc.weight;
				}
			UIMan.verbose(0, "Number of hard clauses violated: "+numViolatedHardClauses);
			UIMan.verbose(0, "Cost: "+cost);
			return true;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}	
	
	public boolean run_internal() {
		timeout = false;
		double lastObj = Config.impossible_obj;
		boolean lastIfVio = true;
		this.curObj = lastObj;
		int numIterations = 0;
		String lazy_grnd = "lazy_grounding";
		Timer.start(lazy_grnd);
		int lastGNum = -1;
		int curGNum = -1;
		do {
			++numIterations;
			UIMan.verbose(1, "Lazy grounding iteration "
				+ numIterations);
			this.allLastSolutions = this.allSolutions;
			this.optLastSolution = this.optSolution;
			this.allSolutions.clear();
			lastIfVio = this.hasHardClauseViolated;
			lastObj = this.curObj;
			lastGNum = curGNum;
			Set<GClause> gcs = this.grounder.getGroundedClauses();
			if (Config.verbose_level >= 3) {
				UIMan.println("Grounded formulas: ");
				for (GClause gc : gcs) {
					UIMan.println(gc.toVerboseString(this.mln));
				}
				UIMan.println();
			}
			if (Config.useCAV) {
				UIMan.verbose(2, "Grounded clauses size (excluding edbs): "
					+ gcs.size());
			} else {
				UIMan.verbose(2, "Grounded clauses size (including edbs): "
					+ gcs.size());
			}
			//List<Pair<Double, Set<Integer>>> result = this.solver.solve(gcs);
			List<Pair<Double, Set<Integer>>> result = this.solver.solveWithTimeout(gcs);
			
			for (Pair<Double, Set<Integer>> solution : result) {
				if (solution != null) {
					this.allSolutions.add(solution.right);
				}
			}
			if (result.get(0) != null) {
				this.optSolution = result.get(0).right;
				if (Config.verbose_level >= 3){
					double currentCost = this.grounder.evaluateCost(this.optSolution);	
					UIMan.println("The cost of current solution on soft clauses: "+ currentCost);
				}
				
				if (Config.verbose_level >= 3) {
					UIMan
						.println("Objective function value on grounded rules: "
							+ result.get(0).left);
					UIMan.println("Variables set to true by MAXSAT: ");
					for (Integer i : this.optSolution) {
						UIMan.println(this.mln.getAtom(i).toGroundString(
							this.mln));
					}
				}
			} else {
				if (Config.verbose_level >= 3) {
					UIMan.println("UNSAT!");
				}
				return false;
			}
				
			//Termination condition 1: Number of iteration runs out of budget.
			if(Config.num_grounding_iterations >0 && numIterations >= Config.num_grounding_iterations){
				this.allLastSolutions = this.allSolutions;
				this.optLastSolution = this.optSolution;
				UIMan.verbose(2, "Number of iterations exceeds max!");
				timeout = true;
				break;
			}
			//Termination condition 2: Times out.
			if(Config.grounding_timeout > 0 && Timer.elapsedSeconds("lazy_grounding") > Config.grounding_timeout){
				this.allLastSolutions = this.allSolutions;
				this.optLastSolution = this.optSolution;
				UIMan.verbose(2, "Lazy grounding time out!");
				timeout = true;
				break;
			}
			//Termination condition 3: ours. a. no hard rules violated for the first iteration. b. two iterations have the
			// same optimum objective function on the grounded clauses. Note the solution to the former iteration is the optimum solution.
			if (!Config.enable_concurrency) {
				this.grounder.ground(this.optSolution);
			} else {
				this.grounder.groundConcurrently(this.optSolution);
			}
			if (!this.grounder.hasHardClauseViolated()) {
				UIMan.verbose(2, "No hard rules violated.\n");
			}
			else{
				UIMan.verbose(2, "Hard rules violated.\n");
				if (Config.verbose_level > 2) {
					if (numIterations >=2)	{
						for (GClause gc : this.getViolatdClauses()){
							if(gc.isHardClause())
								System.out.println(gc.toVerboseString(mln));
						}
					}
				}
			}
			this.curObj = result.get(0).left;
			this.hasHardClauseViolated = this.grounder.hasHardClauseViolated();		
			
			if(!Config.cpiCheck){
				if(!lastIfVio && this.curObj == lastObj){
					UIMan.verbose(2, "Two adjacent obj values are equal!");
					break;
				}
			}else{
				//I checked the JDK source code. The following statement shouldn't be a performance concern.
				curGNum = this.getGroundedClauses().size();
				//Termination condition 4: cpi. no more clauses grounded.
				if(curGNum == lastGNum){
					this.allLastSolutions = this.allSolutions;
					this.optLastSolution = this.optSolution;
					UIMan.verbose(2, "CPI check pass!");
					break;
				}
			}
		} while (true);

		this.allSolutions = this.allLastSolutions;
		this.optSolution = this.optLastSolution;
		this.finalObj = this.grounder.evaluate(this.optSolution);
		this.finalCost = this.grounder.evaluateCost(this.optSolution);
		UIMan.verbose(1, "Sum of weights on fully grounded rules: "
			+ this.finalObj);
		UIMan.verbose(1, "Number of lazy grounding iterations: "
			+ numIterations);
		UIMan.verbose(1, "Cost of the solution: "
			+ this.finalCost);
		return true;
	}

	public double getObjValue() {
		return this.finalObj;
	}
	
	public double getCost() {
		return this.finalCost;
	}

	public List<Set<Integer>> getAllSolutions() {
		return this.allSolutions;
	}
	
	public Set<Integer> getOptSolution() {
		return this.optSolution;
	}

	public Set<GClause> getViolatdClauses() {
		if(!this.timeout)
			return this.grounder.getlastViolatedClauses();
		else{
			this.grounder.ground(this.optSolution);
			return this.grounder.getViolatedClauses();
		}
	}

	public Set<GClause> getGroundedClauses() {
		return this.grounder.getGroundedClauses();
	}

	public void storeGroundedConstraints(OutputStream out) {
		this.grounder.storeGoundedConstraints(out);
	}

	public void loadGroundedConstraints(InputStream in) {
		this.grounder.loadGroundedConstraints(in);
	}
	
	public void fullyGround(){
		this.grounder.fullyGround();
	}
	
	public void loadRevertedConstraints(InputStream in) {
		this.grounder.loadRevertedConstraints(in);
	}

	/**
	 * For each un-grounded clause in the MLN, find the number of grounded
	 * clauses violated and satisfied, in that order, for the current evidence
	 * and training data. This method loads the DB tables with the evidence
	 * and training data.
	 * 
	 * @return the count of <violations, satisfactions> for each clause
	 */
	public HashMap<String, Pair<Double, Double>> countViolations() {
		Set<Integer> trainingData = new HashSet<Integer>();
		for (Predicate p : mln.getAllPred()) {
			for (Atom at : p.getHardEvidences()) {
				int atId = mln.getAtomID(at.base());
				if (at.truth) {
					trainingData.add(atId);
				}
			}
			for (Atom at : p.getHardTrainData()) {
				int atId = mln.getAtomID(at.base());
				if (at.truth) {
					trainingData.add(atId);
				}
			}

		}
		return this.grounder.countViolations(trainingData);
	}

	/**
	 * For each un-grounded clause in the MLN, find the number of grounded
	 * clauses violated and satisfied, in that order, for the current solution.
	 * This method loads that the DB tables with the current solution
	 * (including EDB).
	 * 
	 * @return the count of <violations, satisfactions> for each clause
	 */
	public HashMap<String, Pair<Double, Double>> countViolations(
		Set<Integer> solution) {
		return this.grounder.countViolations(solution);
	}

	public double countTotalGroundings() {
		return this.grounder.countTotalGroundings();
	}
	
	/**
	 * Clear all stored state including the predicate tables in the DB.
	 * 
	 * @return true if operation completed successfully.
	 */
	public boolean clearAll() {
		// this.grounder.resetDB();
		if (this.optSolution != null)
			this.optSolution.clear();
		if (this.optLastSolution != null)
			this.optLastSolution.clear();
		if (this.allSolutions != null)
			this.allSolutions.clear();
		if (this.allLastSolutions != null)
			this.allLastSolutions.clear();
		this.hasHardClauseViolated = true;
		return true;
	}

	public void updateWeights(HashMap<String, Double> currentWeights) {
		for (Clause c : this.mln.getAllNormalizedClauses()) {
			if (c.isTemplate()) {
				for (ClauseInstance ci : c.instances) {
					if (!ci.isHardClause()) {
						ci.weight = currentWeights.get(ci.getStrId());
					}
				}
			} else {
				if (!c.isHardClause()) {
					c.setWeight(currentWeights.get(c.getStrId()));
				}
			}
		}
		for (GClause gc : this.grounder.getGroundedClauses()) {
			if (gc.ci == null && gc.c == null) {
				continue;
			}
			if (gc.isHardClause()) {
				continue;
			}
			String clauseID = "";
			if (gc.ci == null) {
				clauseID += gc.c.getStrId();
			} else {
				clauseID += gc.ci.getStrId();
			}
			gc.weight = currentWeights.get(clauseID);
		}
	}

}

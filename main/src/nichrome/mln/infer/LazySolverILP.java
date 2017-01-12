package nichrome.mln.infer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Pair;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import nichrome.mln.CardinalityConstr;
import nichrome.mln.GClause;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.util.Config;
import nichrome.mln.util.Timer;
import nichrome.mln.util.UIMan;

public class LazySolverILP extends LazySolver {
	// original var id -> new var id
	private Map<Integer, Integer> varMap = null;
	// new var id -> original var id
	private List<Integer> varList = null;

	private double gcWeights[] = null;

	private List<GClause> softClauses = null;
	private List<GClause> hardClauses = null;
	private GRBEnv env;
	private GRBModel model;
	private GRBVar[] vars;

	public LazySolverILP(MarkovLogicNetwork mln) {
		super(mln);
	}
	
	public void createModel(Set<GClause> gcs, Set<CardinalityConstr> carCons){
		try {
			this.calculateStats(gcs);
			env = new GRBEnv("mln_gurobi.log");
			env.set(GRB.DoubleParam.NodefileStart, Config.ilpMemory);
			model = new GRBModel(env);
			// vars for atoms
			vars = model.addVars(this.varList.size(), GRB.BINARY);
			// vars for soft clauses
			GRBVar[] softVars =
					model.addVars(this.softClauses.size(), GRB.BINARY);
			// vars for hard clauses
			GRBVar[] hardVars =
					model.addVars(this.hardClauses.size(), GRB.BINARY);
			model.update();
			// objective function
			GRBLinExpr objExpr = new GRBLinExpr();
			objExpr.addTerms(this.gcWeights, softVars);
			model.setObjective(objExpr, GRB.MAXIMIZE);
			// soft clauses
			for (int i = 0; i < this.softClauses.size(); i++) {
				GClause gc = this.softClauses.get(i);
				GRBVar gcVar = softVars[i];
				GRBLinExpr cons = new GRBLinExpr();
				cons.addTerm(1, gcVar);
				for (int j = 0; j < gc.lits.length; j++) {
					int lit = gc.lits[j];
					boolean pos = lit > 0;
					int lvIdx = this.varMap.get(Math.abs(lit));
					GRBVar litVar = vars[lvIdx];
					if (pos) {
						cons.addTerm(-1, litVar);
					} else {
						cons.addConstant(-1);
						cons.addTerm(1, litVar);
					}
					GRBLinExpr cons1 = new GRBLinExpr();
					cons1.addTerm(1, gcVar);
					if (pos) {
						cons1.addTerm(-1, litVar);
					} else {
						cons1.addConstant(-1);
						cons1.addTerm(1, litVar);
					}
					model.addConstr(cons1, GRB.GREATER_EQUAL, 0, "soft_disj_"
							+ i + "_" + j);
				}
				model.addConstr(cons, GRB.LESS_EQUAL, 0, "soft_disj_" + i
						+ "_all");
			}
			// hard clauses
			for (int i = 0; i < this.hardClauses.size(); i++) {
				GClause gc = this.hardClauses.get(i);
				GRBVar gcVar = hardVars[i];
				GRBLinExpr cons = new GRBLinExpr();
				cons.addTerm(1, gcVar);
				for (int j = 0; j < gc.lits.length; j++) {
					int lit = gc.lits[j];
					boolean pos = lit > 0;
					int lvIdx = this.varMap.get(Math.abs(lit));
					GRBVar litVar = vars[lvIdx];
					if (pos) {
						cons.addTerm(-1, litVar);
					} else {
						cons.addConstant(-1);
						cons.addTerm(1, litVar);
					}
					GRBLinExpr cons1 = new GRBLinExpr();
					cons1.addTerm(1, gcVar);
					if (pos) {
						cons1.addTerm(-1, litVar);
					} else {
						cons1.addConstant(-1);
						cons1.addTerm(1, litVar);
					}
					model.addConstr(cons1, GRB.GREATER_EQUAL, 0, "hard_disj_"
							+ i + "_" + j);
				}
				model.addConstr(cons, GRB.LESS_EQUAL, 0, "hard_disj_" + i
						+ "_all");
				GRBLinExpr cons2 = new GRBLinExpr();
				cons2.addTerm(1, gcVar);
				model
				.addConstr(cons2, GRB.EQUAL, 1, "hard_disj_" + i + "_hard");
			}
			// cardinality
			int counter = 0;
			for(CardinalityConstr cCons : carCons){
				counter++;
				GRBLinExpr cons = new GRBLinExpr();
				for(int j : cCons.getAts()){
					int atIdx = this.varMap.get(j);
					GRBVar atVar = vars[atIdx];
					cons.addTerm(1, atVar);
				}
				char atWhat = (cCons.getKind() == CardinalityConstr.Kind.AT_MOST?GRB.LESS_EQUAL : GRB.GREATER_EQUAL);
				model.addConstr(cons, atWhat, cCons.getK(), "cardinality_csontr_"+counter);
			}
			model.update();
		} catch (GRBException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Try to improve an existing solution. Return null, if no better solution is found.
	 * @param gcs
	 * @param asgn
	 * @return
	 */

	public Pair<Double, Set<Integer>> refine(Set<GClause> gcs, Set<Integer> right, Double left) {
		try {
			this.calculateStats(gcs);
			env = new GRBEnv("mln_gurobi.log");
			env.set(GRB.DoubleParam.NodefileStart, Config.ilpMemory);
			env.set(GRB.IntParam.MIPFocus, 1);
			env.set(GRB.IntParam.SolutionLimit, Config.ilpSolLimit);
			model = new GRBModel(env);
			// vars for atoms
			vars = model.addVars(this.varList.size(), GRB.BINARY);
			// vars for soft clauses
			GRBVar[] softVars =
					model.addVars(this.softClauses.size(), GRB.BINARY);
			// vars for hard clauses
			GRBVar[] hardVars =
					model.addVars(this.hardClauses.size(), GRB.BINARY);
			model.update();

			// soft clauses
			for (int i = 0; i < this.softClauses.size(); i++) {
				GClause gc = this.softClauses.get(i);
				GRBVar gcVar = softVars[i];
				GRBLinExpr cons = new GRBLinExpr();
				cons.addTerm(1, gcVar);
				for (int j = 0; j < gc.lits.length; j++) {
					int lit = gc.lits[j];
					boolean pos = lit > 0;
					int lvIdx = this.varMap.get(Math.abs(lit));
					GRBVar litVar = vars[lvIdx];
					if (pos) {
						cons.addTerm(-1, litVar);
					} else {
						cons.addConstant(-1);
						cons.addTerm(1, litVar);
					}
					GRBLinExpr cons1 = new GRBLinExpr();
					cons1.addTerm(1, gcVar);
					if (pos) {
						cons1.addTerm(-1, litVar);
					} else {
						cons1.addConstant(-1);
						cons1.addTerm(1, litVar);
					}
					model.addConstr(cons1, GRB.GREATER_EQUAL, 0, "soft_disj_"
							+ i + "_" + j);
				}
				model.addConstr(cons, GRB.LESS_EQUAL, 0, "soft_disj_" + i
						+ "_all");
			}
			// hard clauses
			for (int i = 0; i < this.hardClauses.size(); i++) {
				GClause gc = this.hardClauses.get(i);
				GRBVar gcVar = hardVars[i];
				GRBLinExpr cons = new GRBLinExpr();
				cons.addTerm(1, gcVar);
				for (int j = 0; j < gc.lits.length; j++) {
					int lit = gc.lits[j];
					boolean pos = lit > 0;
					int lvIdx = this.varMap.get(Math.abs(lit));
					GRBVar litVar = vars[lvIdx];
					if (pos) {
						cons.addTerm(-1, litVar);
					} else {
						cons.addConstant(-1);
						cons.addTerm(1, litVar);
					}
					GRBLinExpr cons1 = new GRBLinExpr();
					cons1.addTerm(1, gcVar);
					if (pos) {
						cons1.addTerm(-1, litVar);
					} else {
						cons1.addConstant(-1);
						cons1.addTerm(1, litVar);
					}
					model.addConstr(cons1, GRB.GREATER_EQUAL, 0, "hard_disj_"
							+ i + "_" + j);
				}
				model.addConstr(cons, GRB.LESS_EQUAL, 0, "hard_disj_" + i
						+ "_all");
				GRBLinExpr cons2 = new GRBLinExpr();
				cons2.addTerm(1, gcVar);
				model
				.addConstr(cons2, GRB.EQUAL, 1, "hard_disj_" + i + "_hard");
			}

			// objective function
			GRBLinExpr objExpr = new GRBLinExpr();
			objExpr.addTerms(this.gcWeights, softVars);
			model.addConstr(objExpr, GRB.GREATER_EQUAL, left+0.5, "object");
			
			model.setObjective(objExpr);

			model.update();
		} catch (GRBException e) {
			throw new RuntimeException(e);
		}		

		try {

			if(Config.saveILPModelPath != null){
				String fName = Config.saveILPModelPath + File.separator+"refine"+System.currentTimeMillis()+".mps";
				model.write(fName);
			}
			UIMan.verbose(1, "Start the ILP solver:");
			String maxsatTimer = "ILP";
			Timer.start(maxsatTimer);
			model.optimize();
			int retStatus = model.get(GRB.IntAttr.Status);
			if( retStatus == GRB.Status.INFEASIBLE){
				return null;
			}
			if(retStatus != GRB.Status.OPTIMAL && retStatus != GRB.Status.SOLUTION_LIMIT)
				throw new RuntimeException("Something wrong with gurobi optimization!");
			if (Config.verbose_level >= 1) {
				Timer.printElapsed(maxsatTimer);
			}
			double solution[] = model.get(GRB.DoubleAttr.X, vars);
			Set<Integer> trueVars = new HashSet<Integer>();
			for (int i = 0; i < solution.length; i++) {
				if (solution[i] > 0.5) {// Gurobi can return value like
					// 0.99999999
					trueVars.add(this.varList.get(i));
				}
			}
			double obj = model.get(GRB.DoubleAttr.ObjVal);
			Pair<Double,Set<Integer>> ret = new Pair<Double, Set<Integer>>(obj, trueVars);
			return ret;

		} catch (GRBException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private boolean areEqual(double l, double r){
		if(Math.abs(l-r) < 0.01)
			return true;
		return false;
	}
	
	@Override
	public List<Pair<Double, Set<Integer>>> solve(Set<GClause> gcs) {
		return this.solve(gcs, new HashSet<CardinalityConstr>());
	}

	public List<Pair<Double, Set<Integer>>> solve(Set<GClause> gcs, Set<CardinalityConstr> carCons) {
		ArrayList<Pair<Double, Set<Integer>>> ret = new ArrayList<Pair<Double, Set<Integer>>>();
		if (gcs == null || gcs.isEmpty()) {
			ret.add(new Pair<Double, Set<Integer>>(0.0, new HashSet<Integer>()));
			return ret;
		}
		try {
			this.createModel(gcs, carCons);
			if(Config.saveILPModelPath != null){
				String fName = Config.saveILPModelPath + File.separator+"solve"+System.currentTimeMillis()+".mps";
				model.write(fName);
			}
			UIMan.verbose(1, "Start the ILP solver:");
			String maxsatTimer = "ILP";
			Timer.start(maxsatTimer);
			model.optimize();
			if (Config.verbose_level >= 1) {
				Timer.printElapsed(maxsatTimer);
			}
			if (model.get(GRB.IntAttr.Status) != GRB.Status.OPTIMAL) {
				ret.add(null);
				return ret;
			}
			double solution[] = model.get(GRB.DoubleAttr.X, vars);
			Set<Integer> trueVars = new HashSet<Integer>();
			for (int i = 0; i < solution.length; i++) {
				if (solution[i] > 0.5) {// Gurobi can return value like
					// 0.99999999
					trueVars.add(this.varList.get(i));
				}
			}
			double obj = model.get(GRB.DoubleAttr.ObjVal);
			ret.add(new Pair<Double, Set<Integer>>(obj, trueVars));
			return ret;
		} catch (GRBException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void calculateStats(Set<GClause> gcs) {
		this.varMap = new HashMap<Integer, Integer>();
		this.varList = new ArrayList<Integer>();
		this.softClauses = new ArrayList<GClause>();
		this.hardClauses = new ArrayList<GClause>();
		for (GClause gc : gcs) {
			if (gc.isHardClause()) {
				this.hardClauses.add(gc);
			} else {
				this.softClauses.add(gc);
			}
			for (int l : gc.lits) {
				int oid = Math.abs(l);
				if (!this.varMap.containsKey(oid)) {
					this.varList.add(oid);
					this.varMap.put(oid, this.varList.size() - 1);
				}
			}
		}
		this.gcWeights = new double[this.softClauses.size()];
		for (int i = 0; i < this.softClauses.size(); i++) {
			this.gcWeights[i] = this.softClauses.get(i).weight;
		}
	}

}

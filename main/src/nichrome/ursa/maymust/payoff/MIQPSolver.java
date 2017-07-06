package nichrome.ursa.maymust.payoff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBExpr;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import nichrome.datalog.bddbddb.Solver;
import nichrome.mln.util.Timer;

public class MIQPSolver extends Solver implements OptimizationSolver {
	GRBEnv env;
	GRBModel model;
	Map<Integer, GRBVar> atomToVar;
	Map<GRBVar, Integer> varToAtom;
	Map<GRBVar, GRBVar> negVarMap;
	Set<GRBVar> auxiVars;
	Map<GRBVar, String> varNameMap;
//	GRBQuadExpr obj;
	GRBLinExpr obj;
	int consCounter = 0;
	Set<GRBConstr> tempConstrs;

	List<GRBVar> tmpVars;
	int tmpVarIdx = -1; // haven't consumed any

	@Override
	public void newProblem() {
		try {
			env = new GRBEnv("qb.log");
			model = new GRBModel(env);
			atomToVar = new HashMap<Integer, GRBVar>();
			varToAtom = new HashMap<GRBVar, Integer>();
			auxiVars = new HashSet<GRBVar>();
			negVarMap = new HashMap<GRBVar, GRBVar>();
			varNameMap = new HashMap<GRBVar, String>();
//			obj = new GRBQuadExpr();
			obj = new GRBLinExpr();
			consCounter = 0;
			tempConstrs = new HashSet<GRBConstr>();
		} catch (GRBException e) {
			throw new RuntimeException(e);
		}
	}

	public void createVarsBatch(Set<Integer> atoms) {
		for (int at : atoms) {
			assert (at > 0);
			this.getOrCreateVarOrNeg(-at, false);
		}
		try {
			model.update();
			for(Map.Entry<GRBVar, GRBVar> e : negVarMap.entrySet()){
				GRBVar v = e.getKey();
				GRBVar nv = e.getValue();
				GRBLinExpr cons = new GRBLinExpr();
				cons.addTerm(1, v);
				cons.addTerm(1, nv);
				model.addConstr(cons, GRB.EQUAL, 1, this.varNameMap.get(v) + "AssoNeg");
			}
		} catch (GRBException e) {
			throw new RuntimeException(e);
		}
	}

	private GRBVar getOrCreateVar(int aId, boolean ifUpdateModel) {
		assert (aId > 0);
		if (atomToVar.containsKey(aId))
			return atomToVar.get(aId);
		GRBVar v = createBooleanVar("atom" + aId, ifUpdateModel);
		atomToVar.put(aId, v);
		varToAtom.put(v, aId);
		return v;
	}

	private GRBVar getOrCreateVarOrNeg(int l, boolean ifUpdateModel) {
		assert (l != 0);
		if (l > 0)
			return this.getOrCreateVar(l, ifUpdateModel);
		return this.getOrCreateNegVar(this.getOrCreateVar(-l, ifUpdateModel), ifUpdateModel);
	}

	private GRBVar getOrCreateNegVar(GRBVar v, boolean ifUpdateModel) {
		if (negVarMap.containsKey(v))
			return negVarMap.get(v);
		try {
			GRBVar nv = createBooleanVar("Neg" + this.varNameMap.get(v), ifUpdateModel);
			if (ifUpdateModel){
				GRBLinExpr cons = new GRBLinExpr();
				cons.addTerm(1, v);
				cons.addTerm(1, nv);
				model.addConstr(cons, GRB.EQUAL, 1, this.varNameMap.get(v) + "AssoNeg");
			}
			negVarMap.put(v, nv);
			negVarMap.put(nv, v);
			return nv;
		} catch (GRBException e) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	private GRBVar createBooleanVar(String name, boolean ifUpdateModel) {
		try {
			GRBVar ret = model.addVar(0, 1, 0, GRB.BINARY, name);
			varNameMap.put(ret, name);
			if (ifUpdateModel)
				model.update();
			return ret;
		} catch (GRBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void encodeConjunction(List<Integer> lits, double weight) {
		try {
			GRBVar auxi = this.getTmpVar();
			// -auxi + l1+..+ln <= n - 1
			GRBLinExpr lowerExpr = new GRBLinExpr();
			lowerExpr.addTerm(-1, auxi);
			for (int l : lits) {
				GRBLinExpr lexp = new GRBLinExpr();
				// auxi -l <= 0
				lexp.addTerm(1.0, auxi);
				GRBVar lvar = this.getOrCreateVarOrNeg(l, true);
				lexp.addTerm(-1, lvar);
				lowerExpr.addTerm(1, lvar);
				model.addConstr(lexp, GRB.LESS_EQUAL, 0, "conjunc" + (consCounter++));
			}
			model.addConstr(lowerExpr, GRB.LESS_EQUAL, lits.size() - 1, "conjunc" + (consCounter++));
			// auxi == 1
			if (weight < 0)
				model.addConstr(auxi, GRB.EQUAL, 1, "hard_conjunc" + (consCounter++));
			else {
				obj.addTerm(weight, auxi);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void encodeDisjunction(List<Integer> lits, double weight) {
		try {
			GRBVar auxi = this.getTmpVar();
			// auxi - l1-...-ln <= 0
			GRBLinExpr upperExpr = new GRBLinExpr();
			upperExpr.addTerm(1, auxi);
			for (int l : lits) {
				GRBLinExpr lexp = new GRBLinExpr();
				// auxi -l >= 0
				lexp.addTerm(1.0, auxi);
				GRBVar lvar = this.getOrCreateVarOrNeg(l, true);
				lexp.addTerm(-1, lvar);
				upperExpr.addTerm(-1, lvar);
				model.addConstr(lexp, GRB.GREATER_EQUAL, 0, "disjunc" + (consCounter++));
			}
			model.addConstr(upperExpr, GRB.LESS_EQUAL, 0, "disjunc" + (consCounter++));
			// auxi == 1
			if (weight < 0)
				model.addConstr(auxi, GRB.EQUAL, 1, "hard_disjunc" + (consCounter++));
			else {
				obj.addTerm(weight, auxi);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void encodeConjunction(double weight, int... lits) {
		List<Integer> litsList = new ArrayList<Integer>();
		for (int l : lits)
			litsList.add(l);
		this.encodeConjunction(litsList, weight);
	}

	@Override
	public void encodeDisjunction(double weight, int... lits) {
		List<Integer> litsList = new ArrayList<Integer>();
		for (int l : lits)
			litsList.add(l);
		this.encodeDisjunction(litsList, weight);
	}

	@Override
	public void encodeImplication(List<Integer> ls, int r, double weight) {
		// convert into disjunction
		List<Integer> disj = new ArrayList<Integer>();
		for (int l : ls)
			disj.add(-l);
		disj.add(r);
		this.encodeDisjunction(disj, weight);
	}

	@Override
	public void encodeCardinality(List<Integer> lits, int k) {
		GRBLinExpr expr = new GRBLinExpr();
		for (int l : lits){
			GRBVar v = this.getOrCreateVarOrNeg(l, true);
			expr.addTerm(1, v);
		}
		System.out.println("Card among "+lits.size());
		try {
			model.addConstr(expr, GRB.LESS_EQUAL, k, "card" + (consCounter++));
		} catch (GRBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void encodeQuadraticObjective(List<Integer> lits1, List<Double> coef1, List<Integer> lits2,
			List<Double> coef2) {
//		assert (lits1.size() == coef1.size());
//		assert (lits2.size() == coef2.size());
//		int s1 = lits1.size();
//		int s2 = lits2.size();
//		for (int i = 0; i < s1; i++)
//			for (int j = 0; j < s2; j++) {
//				this.obj.addTerm(coef1.get(i) * coef2.get(j), this.getOrCreateVarOrNeg(lits1.get(i), true),
//						this.getOrCreateVarOrNeg(lits2.get(j), true));
//			}
		throw new RuntimeException("Turned off");
	}

	@Override
	public Set<Integer> solve() {
		try {
			{
				GRBEnv env = model.getEnv();
				env.set(GRB.IntParam.Presolve, 1);
				env.set(GRB.IntParam.Threads, 8);
			}
			model.setObjective(this.obj, GRB.MAXIMIZE);
			model.update();
			String ilpTimer = "ilp";
			Timer.start(ilpTimer);
			model.optimize();
			Timer.printElapsed(ilpTimer);
			if (model.get(GRB.IntAttr.Status) != GRB.Status.OPTIMAL) {
				return null;
			}
			Set<Integer> ret = new HashSet<Integer>();
			for (Map.Entry<Integer, GRBVar> e : atomToVar.entrySet()) {
				if (e.getValue().get(GRB.DoubleAttr.X) > 0.5)
					ret.add(e.getKey());
			}
			return ret;
		} catch (GRBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public double getObjective() {
		try {
			return model.get(GRB.DoubleAttr.ObjVal);
		} catch (GRBException e) {
			throw new RuntimeException();
		}
	}

	@Override
	public void createTmpVarBatch(int n) {
		System.out.println("Create " + n + " tmp vars in batch.");
		this.tmpVars = new ArrayList<GRBVar>();
		for (int i = 0; i < n; i++)
			this.tmpVars.add(this.createBooleanVar("aug" + i, false));
		this.tmpVarIdx = 0;
		try {
			model.update();
		} catch (GRBException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private GRBVar getTmpVar() {
		if (tmpVarIdx == this.tmpVars.size())
			return this.createBooleanVar("AugExce", true);
		GRBVar ret = this.tmpVars.get(tmpVarIdx);
		tmpVarIdx++;
		return ret;
	}

	@Override
	public void encodeSingle(int l, double weight) {
		GRBVar v = this.getOrCreateVarOrNeg(l, true);
		if (weight < 0) {
			try {
				model.addConstr(v, GRB.EQUAL, 1, "Single" + l);
			} catch (GRBException e) {
				e.printStackTrace();
				System.exit(1);
			}
		} else
			obj.addTerm(weight, v);
	}

	@Override
	public void encodeLinearObjective(List<Integer> lits, List<Double> coef) {
		for(int i = 0 ; i < lits.size(); i++)
			obj.addTerm(coef.get(i), this.getOrCreateVarOrNeg(lits.get(i),true));
	}

	@Override
	public void encodeLinearConsLB(List<Integer> lits, List<Double> ceofs, double lb, boolean temp) {
		GRBLinExpr expr = this.createLinExpr(lits, ceofs);
		try {
			GRBConstr cons = model.addConstr(expr, GRB.GREATER_EQUAL, lb, "lin"+consCounter++);
			if(temp)
				this.tempConstrs.add(cons);
		} catch (GRBException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void encodeLinearConsUP(List<Integer> lits, List<Double> ceofs, double up) {
		GRBLinExpr expr = this.createLinExpr(lits, ceofs);
		try {
			model.addConstr(expr, GRB.LESS_EQUAL, up, "lin"+consCounter++);
		} catch (GRBException e) {
			e.printStackTrace();
			System.exit(1);
		}	
	}
	
	private GRBLinExpr createLinExpr(List<Integer> lits, List<Double> ceofs){
		GRBLinExpr ret = new GRBLinExpr();
		for(int i = 0 ; i < lits.size() ; i++){
			GRBVar v = this.getOrCreateVarOrNeg(lits.get(i), true);
			ret.addTerm(ceofs.get(i), v);
		}
		return ret;
	}

	@Override
	public void forget() {
		for(GRBConstr cons : tempConstrs)
			try {
				model.remove(cons);
			} catch (GRBException e) {
				e.printStackTrace();
				System.exit(1);
			}
		tempConstrs.clear();
	}

}

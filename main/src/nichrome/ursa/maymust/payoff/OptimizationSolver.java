package nichrome.ursa.maymust.payoff;

import java.util.List;
import java.util.Set;

public interface OptimizationSolver {

	public void newProblem();
	
	//interface for an optimization. ideally, i shouldn't expose it
	public void createVarsBatch(Set<Integer> atoms);
	
	public void createTmpVarBatch(int n);
	
	public void encodeSingle(int l, double weight);

	public void encodeConjunction(List<Integer> lits, double weight);

	public void encodeDisjunction(List<Integer> lits, double weight);

	public void encodeConjunction(double weight, int... lits);

	public void encodeDisjunction(double weight, int... lits);
	
	public void encodeImplication(List<Integer> ls, int r, double weight);
	
	public void encodeCardinality(List<Integer> lits, int k);

	public void encodeQuadraticObjective(List<Integer> lits1, List<Double> coef1, List<Integer> lits2, List<Double> coef2);
	
	public void encodeLinearObjective(List<Integer> lits, List<Double> coef);
	
	public void encodeLinearConsLB(List<Integer> lits, List<Double> ceofs, double lb, boolean temp);
	
	public void encodeLinearConsUP(List<Integer> lits, List<Double> ceofs, double up);
	
	public Set<Integer> solve();
	
	public double getObjective();
	
	public void forget();
}

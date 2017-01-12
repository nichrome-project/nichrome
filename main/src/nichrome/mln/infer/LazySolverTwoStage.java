package nichrome.mln.infer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Pair;

import nichrome.mln.GClause;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.util.ContainerMan;


/**
 * A two-stage approximate MaxSAT solver. First solve the hard constraints, then soft constraints 
 * @author xin
 *
 */
public class LazySolverTwoStage extends LazySolver {
	private SATSolver satSolver = new SATSolver();

	public LazySolverTwoStage(MarkovLogicNetwork mln) {
		super(mln);
	}

	@Override
	public List<Pair<Double, Set<Integer>>> solve(Set<GClause> gcs) {
		List<Pair<Double, Set<Integer>>> ret = new ArrayList<Pair<Double, Set<Integer>>>();
		Set<GClause> hard = new HashSet<GClause>();
		Set<GClause> soft = new HashSet<GClause>();
		for(GClause gc : gcs){
			if(gc.isHardClause())
				hard.add(gc);
			else
				soft.add(gc);
		}
		
		Pair<Double, Set<Integer>> retHard = satSolver.solve(hard).get(0);
		if(retHard == null){
			ret.add(retHard);
			return ret;
		}
		
		Set<Integer> trueVars = new HashSet<Integer>();
		Set<Integer> falseVars = new HashSet<Integer>();
		
		for(GClause gc : hard){
			for(int l : gc.lits){
				falseVars.add(Math.abs(l));
			}
		}
		
		trueVars.addAll(retHard.right);
		falseVars.removeAll(trueVars);
		
		double obj = 0.0;
		
		Set<GClause> transformedSoft = new HashSet<GClause>();
		OUT: for(GClause gc : soft){
			List<Integer> lits = new ArrayList<Integer>();
			for(int l : gc.lits){
				int al = Math.abs(l);
				if(l > 0 && trueVars.contains(al)){
					obj += gc.weight;
					continue OUT;
				}
				if(l < 0 && falseVars.contains(al)){
					obj += gc.weight;
					continue OUT;
				}	
				if(!trueVars.contains(al) && !falseVars.contains(al)){
					lits.add(l);
				}
			}
			if(lits.size() > 0){
				GClause ngc = new GClause(gc.weight, ContainerMan.convertIntegers(lits));
				transformedSoft.add(ngc);
			}
		}
		
		if(transformedSoft.size() > 0){
			Pair<Double, Set<Integer>> retSoft = satSolver.solve(transformedSoft).get(0);	
			obj += retSoft.left;
			trueVars.addAll(retSoft.right);
		}
		ret.add(new Pair<Double, Set<Integer>>(obj,trueVars));
		return ret;
	}

}

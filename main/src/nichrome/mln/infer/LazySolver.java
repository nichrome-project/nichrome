package nichrome.mln.infer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.Pair;

import nichrome.mln.GClause;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.util.Config;

public abstract class LazySolver {
	private MarkovLogicNetwork mln;
	protected List<Process> pList;

	public LazySolver(MarkovLogicNetwork mln) {
		this.mln = mln;
		pList = new ArrayList<Process>();
	}

	/**
	 * Use the MAXSAT solver to solve the grounded rules. Because of the
	 * limitation of MAXSAT solver, all the rules should have positive integers
	 * as weights. The solver might return multiple solutions. The solutions
	 * should be ordered by optimality i.e. the most optimal solution must
	 * appear at the start of the list.
	 *
	 * @param gcs
	 * @return
	 */
	public abstract List<Pair<Double, Set<Integer>>> solve(Set<GClause> gcs);

	public List<Pair<Double, Set<Integer>>> solveWithTimeout(Set<GClause> gcs){
		if(Config.maxSATTimeOut > 0){
			final Set<GClause> fgcs = gcs;
			Callable<List<Pair<Double, Set<Integer>>>> task = new Callable<List<Pair<Double, Set<Integer>>>>() {
			   public List<Pair<Double, Set<Integer>>> call() {
			      return solve(fgcs);
			   }
			};
			Future<List<Pair<Double, Set<Integer>>>> future = Config.executor.submit(task);
			try {
			   return future.get(Config.maxSATTimeOut, TimeUnit.SECONDS); 
			} catch (TimeoutException ex) {
				for(Process p : pList){
					p.destroy();
				}
				throw new RuntimeException("MAXSAT Solver "+this+" time out!");
			} catch (InterruptedException e) {
				for(Process p : pList){
					p.destroy();
				}
				throw new RuntimeException("MAXSAT Solver "+this+" interrupted!");
			} catch (ExecutionException e) {
				for(Process p : pList){
					p.destroy();
				}
				throw new RuntimeException("MAXSAT Solver +"+this+" execution errors!");
			} 
		}else{
			return this.solve(gcs);
		}
	}
	
	public double evaluate(Collection<Integer> solution,
		Collection<GClause> clauses) {
		double ret = 0.0;
		OUT: for (GClause c : clauses) {
			if (c.isHardClause()) {
				continue;
			}
			for (int i : c.lits) {
				if (i > 0) {
					if (solution.contains(i)) {
						ret += c.weight;
						continue OUT;
					}
				} else {
					if (!solution.contains(-i)) {
						ret += c.weight;
						continue OUT;
					}
				}
			}
		}
		return ret;
	}

	public Pair<Double, Set<Integer>> refine(Set<GClause> checkSet,
			Set<Integer> right, Double left) {
		throw new RuntimeException("Method not implemetned yet");
	}

}

package nichrome.mln.infer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Pair;

import nichrome.mln.GClause;
import nichrome.mln.util.Config;

public class SATSolver extends LazySolverMaxSAT {

	public SATSolver() {
		super(null);
	}

	/**
	 * Currently use maxsat solver for sat solving.
	 *
	 * @param gcs
	 * @return
	 */
	public Set<Integer> solveSAT(Set<GClause> gcs) {
		Set<GClause> hardClauses = new HashSet<GClause>();
		for (GClause gc : gcs) {
			GClause ngc = new GClause(Config.hard_weight, gc.lits);
			hardClauses.add(ngc);
		}
		List<Pair<Double, Set<Integer>>> result = super.solve(hardClauses);
		if (result.get(0) == null) {
			//UNSAT
			return null;
		} else {
			return result.get(0).right;
		}
	}
}

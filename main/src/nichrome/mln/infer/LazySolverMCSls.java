package nichrome.mln.infer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.lang3.Pair;

import nichrome.mln.GClause;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.util.Config;
import nichrome.mln.util.Timer;
import nichrome.mln.util.UIMan;

public class LazySolverMCSls extends LazySolver {
	private double inflateRate = 0.5;
	private int inflateLimit;
	private int highestVarId = 0;
	public static int instanceCounter = 0;

	public LazySolverMCSls(MarkovLogicNetwork mln) {
		super(mln);
	}

	@Override
	public List<Pair<Double, Set<Integer>>> solve(Set<GClause> gcs) {
		ArrayList<Pair<Double, Set<Integer>>> ret = new ArrayList<Pair<Double, Set<Integer>>>();
		if (gcs == null || gcs.isEmpty()) {
			ret.add(new Pair<Double, Set<Integer>>(0.0, new HashSet<Integer>()));
			return ret;
		}
		this.inflateLimit = (int) (gcs.size() * this.inflateRate);
		UIMan.verbose(1, "Start the MCSLS solving process:");
		String maxsatTimer = "mcsls";
		Timer.start(maxsatTimer);
		this.setHighestVarId(gcs);
		TreeMap<Double, List<GClause>> weightMap =
			new TreeMap<Double, List<GClause>>();
		Set<Integer> tseitinLits = new HashSet<Integer>();
		int nextTseitinLitId = this.highestVarId + 1;
		for (GClause gc : gcs) {
			List<GClause> wList = weightMap.get(Math.abs(gc.weight));
			if (wList == null) {
				wList = new ArrayList<GClause>();
				weightMap.put(Math.abs(gc.weight), wList);
			}
			if (gc.isPositiveClause()) {
				wList.add(gc);
			} else {
				// Apply Tseitin Transformation
				int newTseitinLitId = nextTseitinLitId++;
				tseitinLits.add(newTseitinLitId);
				List<GClause> hardList = weightMap.get(Config.hard_weight);
				if (hardList == null) {
					hardList = new ArrayList<GClause>();
					weightMap.put(Config.hard_weight, hardList);
				}
				
				int[] nLits1 = new int[gc.lits.length + 1];
				for (int i = 0; i < nLits1.length - 1; ++i) {
					nLits1[i] = gc.lits[i];
				}
				nLits1[nLits1.length - 1] = newTseitinLitId;
				GClause nGc1 = new GClause(Config.hard_weight, nLits1);
				hardList.add(nGc1);

				for (int lit : gc.lits) {
					int[] nLits2 = new int[2];
					nLits2[0] = -newTseitinLitId;
					nLits2[1] = -lit;
					GClause nGc2 = new GClause(Config.hard_weight, nLits2);
					hardList.add(nGc2);
				}
				
				GClause nGc3 = new GClause(-gc.weight, newTseitinLitId);
				wList.add(nGc3);
			}
		}
		List<Map.Entry<Double, List<GClause>>> clauseGroups =
			new ArrayList<Map.Entry<Double, List<GClause>>>(weightMap
				.entrySet());
		int groupSize = clauseGroups.size();
		Map.Entry<Double, List<GClause>> endGroup =
			clauseGroups.get(groupSize - 1);
		List<GClause> hardClauses = null;
		if (endGroup.getKey() == Config.hard_weight) {
			hardClauses = endGroup.getValue();
			clauseGroups.remove(groupSize - 1);
		} else {
			hardClauses = new ArrayList<GClause>();
		}
		List<Pair<Double, List<GClause>>> transClauseGroups =
			new ArrayList<Pair<Double, List<GClause>>>();

		for (Map.Entry<Double, List<GClause>> group : clauseGroups) {
			if (transClauseGroups.size() == 0) {
				transClauseGroups.add(new Pair<Double, List<GClause>>(group
					.getKey(), group.getValue()));
				continue;
			}
			Pair<Double, List<GClause>> lastGroup =
				transClauseGroups.get(transClauseGroups.size() - 1);
			double lgWeight = lastGroup.left * lastGroup.right.size();
			if (lgWeight <= group.getKey()) {
				transClauseGroups.add(new Pair<Double, List<GClause>>(group
					.getKey(), group.getValue()));
				continue;
			}
			int splitNum = (int) (group.getKey() / lastGroup.left);
			// MCSls does not support weights. To compensate it, is to make
			// copies of certain rules. But we also do not want the number of
			// constraints to blow up too much.
			if (splitNum * group.getValue().size() > this.inflateLimit) {
				lastGroup.right.addAll(group.getValue());
			} else {
				for (int i = 0; i < splitNum; i++) {
					for (GClause cgc : group.getValue()) {
						lastGroup.right.add(cgc);
					}
				}
			}
		}
		
		// Not the best way to get multiple solutions since essentially we
		// are just using the multiples MCSes for the last transClauseGroup
		List<List<GClause>> hardClausesVersions = new ArrayList<List<GClause>>();
		for (int i = 0; i < Config.num_solver_solutions; ++i) {
			hardClausesVersions.add(new ArrayList<GClause>(hardClauses));
		}
		for (int i = transClauseGroups.size() - 1; i >= 0; i--) {
			List<GClause> softClauses = null;
			softClauses = transClauseGroups.get(i).right;
			List<Set<GClause>> cgcToRemove =
				this.relaxConstraints(hardClausesVersions.get(0), softClauses);
			for (int j = 0; j < Config.num_solver_solutions; ++j) {
				int indx = (j < cgcToRemove.size()) ? j : cgcToRemove.size() - 1;
				// indx = -1 if the formula is satisfiable and no MCSs returned.
				List<GClause> hardClausesVersionJ = hardClausesVersions.get(j);
				List<GClause> softClausesVersionJ = new ArrayList<GClause>(softClauses);
				if (indx >= 0) softClausesVersionJ.removeAll(cgcToRemove.get(indx));
				hardClausesVersionJ.addAll(softClausesVersionJ);
			}
		}
		for (int i = 0; i < Config.num_solver_solutions; ++i) {
			Set<GClause> finalGcs = new HashSet<GClause>(hardClausesVersions.get(i));
			Set<Integer> solution = (new SATSolver()).solveSAT(finalGcs);
			if (solution == null) {
				ret.add(null);
			} else {
				solution.removeAll(tseitinLits);
				double objValue = super.evaluate(solution, gcs);
				ret.add(new Pair<Double, Set<Integer>>(objValue, solution));
			}
		}

		if (Config.verbose_level >= 1) {
			Timer.printElapsed(maxsatTimer);
		}
		return ret;
	}

	/**
	 * This method use mcsls solver to relax over-constrained problems. It means
	 * if the clauses in the return value are removed from the soft clauses,
	 * there would be no conflict in constraints.
	 *
	 * @param hardClauses
	 * @param softClauses
	 * @return
	 */
	private List<Set<GClause>> relaxConstraints(List<GClause> hardClauses,
		List<GClause> softClauses) {
		File consFile = null;
		if(Config.saveMaxSATInstance)
			consFile = new File(Config.dir_working + File.separator + this.hashCode()+"_"+(instanceCounter++)
				+ "_mcsls.wcnf");
		else
			consFile = new File(Config.dir_working + File.separator + this.hashCode()
				+ "_mcsls.wcnf");
		try {
			PrintWriter pw = new PrintWriter(consFile);
			Set<GClause> clauses = new HashSet<GClause>();
			clauses.addAll(hardClauses);
			clauses.addAll(softClauses);
			long nbvar = this.numOfVars(clauses);
			long nclauses = hardClauses.size() + softClauses.size();
			long top = softClauses.size() + 1;
			pw.println("p wcnf " + nbvar + " " + nclauses + " " + top);
			for (GClause gc : softClauses) {
				pw.print(1);
				for (int l : gc.lits) {
					pw.print(" " + l);
				}
				pw.println(" 0");
			}
			for (GClause gc : hardClauses) {
				pw.print(top);
				for (int l : gc.lits) {
					pw.print(" " + l);
				}
				pw.println(" 0");
			}
			pw.flush();
			pw.close();

			ProcessBuilder pb =
				new ProcessBuilder(Config.mcsls_path, "-T",
					Config.mcsls_timeout + "", "-num", Config.mcsls_numLimit
						+ "", "-alg", Config.mcsls_algo, consFile
						.getAbsolutePath());
			pb.redirectErrorStream(true); // otherwise it might fill the pipe
			// and block
			final Process p = pb.start();
			ResultInterpreter ri =
				new ResultInterpreter(p.getInputStream(), softClauses, true);
			Future<List<Set<GClause>>> futureResult = Config.executor.submit(ri);
			List<Set<GClause>> ret = futureResult.get();
			if (p.waitFor() != 0 && (ret == null || ret.size() == 0)) {
				UIMan.println(ri.getLog());
				throw new RuntimeException(
					"The MAXSAT solver did not terminate normally");
			}
			if (p != null) {
				if (p.getOutputStream() != null) {
					p.getOutputStream().close();
				}
				if (p.getErrorStream() != null) {
					p.getErrorStream().close();
				}
				if (p.getInputStream() != null) {
					p.getInputStream().close();
				}
				p.destroy();
			}
			return ret;
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private int numOfVars(Collection<GClause> gcs) {
		Set<Integer> vars = new HashSet<Integer>();
		for (GClause gc : gcs) {
			for (int i : gc.lits) {
				vars.add(Math.abs(i));
			}
		}
		return vars.size();
	}
	
	private void setHighestVarId(Collection<GClause> gcs) {
		for (GClause gc : gcs) {
			for (int i : gc.lits) {
				if (this.highestVarId < (Math.abs(i))){
					this.highestVarId = Math.abs(i);
				}
			}
		}
	}

	class ResultInterpreter implements Callable<List<Set<GClause>>> {
		private InputStream resultStream;
		private List<GClause> softClauses;
		private boolean realWeight;
		private StringBuffer log;

		public ResultInterpreter(InputStream resultStream,
			List<GClause> softClauses, boolean realWeight) {
			this.resultStream = resultStream;
			this.softClauses = softClauses;
			this.realWeight = realWeight;
		}

		@Override
		public List<Set<GClause>> call() throws Exception {
			BufferedReader in =
				new BufferedReader(new InputStreamReader(this.resultStream));
			List<Set<GClause>> ret = new ArrayList<Set<GClause>>();
			HashMap<Double, List<GClause>> solutions = new HashMap<Double, List<GClause>>();
			boolean timedout = false;
			this.log = new StringBuffer();
			String line;
			while ((line = in.readLine()) != null) {
				this.log.append(line + "\n");
				timedout |= line.startsWith("c mcsls timed out");
				if (!line.startsWith("c MCS: ")) {
					continue;
				}
				Scanner scanLine = new Scanner(line);
				scanLine.next(); // c
				scanLine.next(); // MCS:
				List<GClause> ca = new ArrayList<GClause>();
				while (scanLine.hasNextInt()) {
					int nint = scanLine.nextInt();
					ca.add(this.softClauses.get(nint - 1));
				}
				scanLine.close();
				solutions.put(this.calWeight(ca), ca);
			/*	if (ret == null) {
					ret = ca;
				} else if (this.calWeight(ca) < this.calWeight(ret)) {
					ret = ca;
				}
			*/	
			}
			in.close();
			Double[] sorted_cost = new Double[solutions.size()];
			solutions.keySet().toArray(sorted_cost);
			Arrays.sort(sorted_cost);
			for (int i = 0; i < Config.num_solver_solutions && i < solutions.size(); ++i) {
				ret.add(new HashSet<GClause>(solutions.get(sorted_cost[i])));
			}
			return ret;
		}

		private double calWeight(List<GClause> gcs) {
			if (this.realWeight) {
				Set<GClause> dupRemoved = new HashSet<GClause>(gcs);
				double ret = 0;
				for (GClause gc : dupRemoved) {
					ret += gc.weight;
				}
				return ret;
			} else {
				return gcs.size();
			}
		}

		public String getLog() {
			return this.log.toString();
		}
	}

}

package nichrome.mln.infer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.commons.lang3.Pair;

import nichrome.mln.GClause;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.util.Config;
import nichrome.mln.util.Timer;
import nichrome.mln.util.UIMan;

public class LazySolverWalkSAT extends LazySolver {

	/**
	 * See http://maxsat.ia.udl.cat/requirements/ nbvar: number of variables
	 * nbclauses: number of clauses top: hard constraint weight = sum of soft
	 * weights + 1
	 */
	private long nbvar = 0;
	private long nbclauses = 0;
	private int highestVarId = 0;
	private long top = (long) Config.hard_weight;

	public LazySolverWalkSAT(MarkovLogicNetwork mln) {
		super(mln);
	}

	/*
	 * (non-Javadoc)
	 * @see nichrome.mln.infer.LazyInferer#infer(java.util.Set)
	 */
	@Override
	public List<Pair<Double, Set<Integer>>> solve(Set<GClause> gcs) {
		ArrayList<Pair<Double, Set<Integer>>> ret = new ArrayList<Pair<Double, Set<Integer>>>();
		if (gcs == null || gcs.isEmpty()) {
			ret.add(new Pair<Double, Set<Integer>>(0.0, new HashSet<Integer>()));
			return ret;
		}
		File consFile =
			new File(Config.dir_working + File.separator + this.hashCode()
				+ "_walksat.wcnf");
		try {
			String genInTimer = "Walksat input timer";
			Timer.start(genInTimer);
			PrintWriter pw = new PrintWriter(consFile);
			pw.println("c mln inference: lazy grouding");
			// Calculate the num of rules and variables
			this.calculateStats(gcs);
			pw.println("p wcnf " + this.nbvar + " " + this.nbclauses + " "
				+ this.top);
			double totalWeight = 0.0;
			Set<Integer> tseitinLits = new HashSet<Integer>();
			int nextTseitinLitId = this.highestVarId + 1;
			for (GClause gc : gcs) {
				if (gc.isPositiveClause()) {
					if (gc.isHardClause()) {
						pw.print(this.top);
					} else {
						double w = gc.weight;
						pw.print((int) w);
						totalWeight += w;
					}
					for (int lit : gc.lits) {
						pw.print(" " + lit);
					}
					pw.println(" 0");
				} else {
					//Apply Tseitin transformation
					int newTseitinLitId = nextTseitinLitId++;
					tseitinLits.add(newTseitinLitId);
					pw.print(this.top);
					for (int lit : gc.lits) {
						pw.print(" " + lit);
					}
					pw.print(" " + newTseitinLitId);
					pw.println(" 0");
					
					for (int lit : gc.lits) {
						pw.print(this.top);
						pw.print(" " + -newTseitinLitId);
						pw.print(" " + -lit);
						pw.println(" 0");
					}
					
					if (gc.isHardClause()) {
						pw.print(this.top);
					} else {
						double w = -gc.weight;
						pw.print((int) w);
						totalWeight += w;
					}
					pw.print(" " + newTseitinLitId);
					pw.println(" 0");
				}
			}
			pw.flush();
			pw.close();
			if (Config.verbose_level >= 1) {
				Timer.printElapsed(genInTimer);
			}

			String cmd[] = new String[2];
			cmd[0] = Config.walksat_path;
			cmd[1] = consFile.getAbsolutePath();
			UIMan.verbose(1, "Start the Walksat solver:");
			String maxsatTimer = "Walksat";
			Timer.start(maxsatTimer);
			ProcessBuilder pb = new ProcessBuilder(cmd[0], "-hard", "-cutoff",
				"100000", "-tries", "10","-targetcost", Long.MAX_VALUE+"", "-withcost", "-solcnf");
			pb.redirectErrorStream(true);
			final Process p = pb.start();

			if (p != null) {
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

				BufferedReader inputFile = new BufferedReader(new InputStreamReader(new FileInputStream(consFile.getAbsolutePath())));

				String currInputLine = null;
				while((currInputLine = inputFile.readLine()) != null) {
					bw.write(currInputLine);
					bw.newLine();
				}
				bw.close();
				inputFile.close();
			}

			ResultInterpreter ri =
				new ResultInterpreter(p.getInputStream(), totalWeight);

			Future<Pair<Double, Set<Integer>>> futureResult =
				Config.executor.submit(ri);

			if (p.waitFor() != 0) {
				throw new RuntimeException(
					"The Walksat solver did not terminate normally");
			}

			Pair<Double, Set<Integer>> retP = futureResult.get();
			if (retP != null) {
				retP.right.removeAll(tseitinLits);
			}
			ret.add(retP);

			// if(retP.left != this.evaluate(retP.right, gcs)){
			// throw new
			// RuntimeException("Something wrong with objective function evaluation!");
			// }

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
			if (Config.verbose_level >= 1) {
				Timer.printElapsed(maxsatTimer);
			}
			return ret;
			// return interpreteResult(result);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * @deprecated Return the variables set to true.
	 * @param result
	 * @return
	 */
	@Deprecated
	private Set<Integer> interpretResult(File result) {
		String interOutTimer = "Walksat output timer";
		Timer.start(interOutTimer);
		try {
			Scanner sc = new Scanner(result);
			Set<Integer> ret = new HashSet<Integer>();
			while (sc.hasNext()) {
				String line = sc.nextLine();
				if (line.startsWith("ASSIGNMENT ")) {
					if (line.startsWith("ASSIGNMENT NOT FOUND")) {
						return null;
					}
					if (!line.startsWith("ASSIGNMENT ACHIEVING TARGET")) {
						throw new RuntimeException(
							"Expecting a solution but got " + line);
					}
				}
				if (line.startsWith("v ")) {
					Scanner lineSc = new Scanner(line);
					String c = lineSc.next();
					if (!c.trim().equals("v")) {
						throw new RuntimeException(
							"Expected char of a solution line: " + c);
					}
					while (lineSc.hasNext()) {
						int i = lineSc.nextInt();
						if (i < 0) {
						} else {
							ret.add(i);
						}
					}
				}
			}
			if (Config.verbose_level >= 1) {
				Timer.printElapsed(interOutTimer);
			}
			return ret;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void calculateStats(Set<GClause> gcs) {
		this.nbclauses = gcs.size();
		Set<Integer> atomSet = new HashSet<Integer>();
		this.top = 0;
		for (GClause gc : gcs) {
			if (!gc.isHardClause()) {
				this.top += Math.abs(gc.weight);
			}
			for (int l : gc.lits) {
				if (Math.abs(l) > this.highestVarId) {
					this.highestVarId = Math.abs(l);
				}
				atomSet.add(Math.abs(l));
			}
		}
		this.nbvar = atomSet.size();
		this.top++;
		if (this.top < 0) {
			UIMan.verbose(1,
				"The sum of weights of soft constraints overflows, set it to "
					+ Config.hard_weight);
			this.top = (long) Config.hard_weight;
		}
	}

	class ResultInterpreter implements Callable<Pair<Double, Set<Integer>>> {
		private InputStream resultStream;
		private double totalWeight;

		public ResultInterpreter(InputStream resultStream, double totalWeight) {
			this.resultStream = resultStream;
			this.totalWeight = totalWeight;
		}

		@Override
		public Pair<Double, Set<Integer>> call() throws Exception {
			BufferedReader in =
				new BufferedReader(new InputStreamReader(this.resultStream));
			Set<Integer> sol = new HashSet<Integer>();
			double unsatWeight = 0.0;
			String line;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("ASSIGNMENT ")) {
					if (line.startsWith("ASSIGNMENT NOT FOUND")) {
						return null;
					}
					if (!line.startsWith("ASSIGNMENT ACHIEVING TARGET")) {
						throw new RuntimeException(
							"Expecting a solution but got " + line);
					}
				}
				if (line.startsWith("v ")) {
					String[] lsplits = line.split(" ");
					for (String s : lsplits) {
						s = s.trim();
						if (s.equals("v")) {
							continue;
						}
						int i = Integer.parseInt(s);
						if (i > 0) {
							sol.add(i);
						}
					}
				}
			}
			in.close();
			Pair<Double, Set<Integer>> ret =
				new Pair<Double, Set<Integer>>(this.totalWeight - unsatWeight,
					sol);
			return ret;
		}
	}

}

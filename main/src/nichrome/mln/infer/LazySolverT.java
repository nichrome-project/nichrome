package nichrome.mln.infer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.commons.lang3.Pair;

import nichrome.mln.Atom;
import nichrome.mln.GClause;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.util.Config;
import nichrome.mln.util.Timer;
import nichrome.mln.util.UIMan;

public class LazySolverT extends LazySolver {
	static int counter = 0;
	private MarkovLogicNetwork mln;

	public LazySolverT(MarkovLogicNetwork mln) {
		super(mln);
		this.mln = mln;
	}

	@Override
	public List<Pair<Double, Set<Integer>>> solve(Set<GClause> gcs) {
		ArrayList<Pair<Double, Set<Integer>>> ret = new ArrayList<Pair<Double, Set<Integer>>>();
	//	if (gcs == null || gcs.isEmpty()) {
	//		ret.add(new Pair<Double, Set<Integer>>(0.0, new HashSet<Integer>()));
	//		return ret;
	//	}
		String consFile = Config.tuffyMLN + "_" + counter;
		try {
			String genInTimer = "Tuffy input timer";
			Timer.start(genInTimer);
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(consFile)));
			pw.println("// Hard constraints generated for Tuffy");
			for (GClause gc : gcs) {
				pw.println(gc.toConstraintString(this.mln));
			}
			pw.flush();
			pw.close();
			if (Config.verbose_level >= 1) {
				Timer.printElapsed(genInTimer);
			}

			List<String> command = new ArrayList<String>();
			command.add("/usr/bin/time");
			command.add("-v");
			command.add("-o");
			command.add(Config.tuffyMemstats+"_"+counter);
			
			command.add("java");
			command.add("-Xmx64g");
			command.add("-jar");
			command.add(Config.tuffy_path);
			command.add("-conf");
			command.add(Config.tuffyConf);
			command.add("-i");
			command.add(Config.tuffyI+","+consFile);
			command.add("-e");
			command.add(Config.tuffyEDB);
			String resPath = Config.tuffyRes+"_"+counter;
			command.add("-r");
			command.add(resPath);
			command.add("-queryFile");
			command.add(Config.tuffyQuery);
			command.add("-dribble");
			command.add(Config.tuffyOut+"_"+counter);
			if (Config.tuffyDB != null) {
				command.add("-db");
				command.add(Config.tuffyDB);
			}
			command.add("-keepData");
			command.add("-verbose");
			command.add("2");
			
			counter++;

			String[] cmdAry = command.toArray(new String[command.size()]);
			for (int i = 0; i < cmdAry.length; ++i) {
				System.out.println(cmdAry[i]+",");
			}
			UIMan.verbose(1, "Start Tuffy:");
			String tuffyTimer = "Tuffy";
			Timer.start(tuffyTimer);
			ProcessBuilder pb = new ProcessBuilder(cmdAry);
			pb.redirectErrorStream(true);
			final Process p = pb.start();
			ResultInterpreter ri =
				new ResultInterpreter(p.getInputStream(), resPath, this.mln);

			Future<Pair<Double, Set<Integer>>> futureResult =
				Config.executor.submit(ri);

			if (p.waitFor() != 0) {
				throw new RuntimeException(
					"Tuffy did not terminate normally");
			}

			Pair<Double, Set<Integer>> retP = futureResult.get();
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
				Timer.printElapsed(tuffyTimer);
			}
			return ret;
			// return interpreteResult(result);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	class ResultInterpreter implements Callable<Pair<Double, Set<Integer>>> {
		private String resultPath;
		private MarkovLogicNetwork mln;
		private InputStream resultStream;

		public ResultInterpreter(InputStream resultStream, String resultPath, MarkovLogicNetwork mln) {
			this.resultPath = resultPath;
			this.mln = mln;
			this.resultStream = resultStream;
		}

		@Override
		public Pair<Double, Set<Integer>> call() throws Exception {
			BufferedReader in =
				new BufferedReader(new InputStreamReader(this.resultStream));
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(Config.tuffyOut+"_combo", true)));
			
			String line;
			while ((line = in.readLine()) != null) {
				if (line.contains("Data remains in schema")) {
					String[] splitLine = line.split("'");
					if (splitLine.length == 3) {
						Config.tuffyDB = splitLine[1];
					}
				}
				pw.println(line);
			}
			in.close();
			pw.flush();
			pw.close();
		
			
			BufferedReader br =
				new BufferedReader(new InputStreamReader(new FileInputStream(
					resultPath)));
			Set<Integer> sol = new HashSet<Integer>();
			while ((line = br.readLine()) != null) {
				if (line.trim().equals("")) {
					continue;
				}
				if (line.startsWith("//")) {
					continue;
				}
				if (line.startsWith("!")) {
					br.close();
					throw new RuntimeException("Result produced by the MLN engine should not contain negated tuples");
				} else {
					Atom at = this.mln.parseAtomAndCheck(line);
					if (at != null) {
						int atId = mln.getAtomID(at.base());
						sol.add(atId);
					}
				}
			}
			br.close();
			Pair<Double, Set<Integer>> ret = new Pair<Double, Set<Integer>>(0.0, sol);
			return ret;
		}
	}

}

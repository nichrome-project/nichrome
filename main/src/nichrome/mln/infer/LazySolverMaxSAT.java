package nichrome.mln.infer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Pair;

import nichrome.mln.GClause;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.util.Config;
import nichrome.mln.util.IncMan;
import nichrome.mln.util.Timer;
import nichrome.mln.util.UIMan;

public class LazySolverMaxSAT extends LazySolver {

	private Set<GClause> prevQuery;
	private List<GClause> queryInOrder;
	//private List<Pair<Double, Set<Integer>>> prevResult;
	private static int deltaHardCount = 0;
	private static int deltaSoftCount = 0;

	/**
	 * See http://maxsat.ia.udl.cat/requirements/ nbvar: number of variables
	 * nbclauses: number of clauses top: hard constraint weight = sum of soft
	 * weights + 1
	 */
	private long nbvar = 0;
	private long nbclauses = 0;
	private int highestVarId = 0;
	private long top = (long) Config.hard_weight;
	/**
	 * old var id -> new var id
	 */
	private static Map<Integer,Integer> varIdMap;
	/**
	 * new var id -> old var id
	 */
	private static List<Integer> varList;
	
	public static int instanceCounter = 0;
	private static int iter = 0;

	
	static{
		varIdMap = new HashMap<Integer, Integer>();
		varList = new ArrayList<Integer>();
		varList.add(null);//Skip 0 in id list
	}
	
	public LazySolverMaxSAT(MarkovLogicNetwork mln) {
		super(mln);
		prevQuery = new HashSet<GClause>();
		queryInOrder = new LinkedList<GClause>();
	}
	
	private List<GClause> computeDeltaConstraints(Set<GClause> gcs) {
		deltaHardCount = 0;
		deltaSoftCount = 0;
		
		//if( prevQuery == null || prevQuery.isEmpty()) {
		//	return gcs;
		//}
		
		List<GClause> delta = new LinkedList<GClause>();
		
		for(GClause c : gcs) {
			if ( ! prevQuery.contains(c) ) { // be careful, we may need to override equal method
				delta.add(c);
				queryInOrder.add(c);
				if(c.isHardClause()) {
					++deltaHardCount;
				}
				else{
					++deltaSoftCount;
				}
			}
		}
		
		return delta;
	}


	/*
	 * (non-Javadoc)
	 * @see nichrome.mln.infer.LazyInferer#infer(java.util.Set)
	 */
	@Override
	public List<Pair<Double, Set<Integer>>> solve(Set<GClause> gcs) {
		//String logFile = "/tmp/maxsat/" + this.hashCode()
		//		+ ".wcnf." + (iter++) + ".iter";
		//IncMan.logClauses(gcs, logFile);

		ArrayList<Pair<Double, Set<Integer>>> ret = new ArrayList<Pair<Double, Set<Integer>>>();
		if (gcs == null || gcs.isEmpty()) {
			ret.add(new Pair<Double, Set<Integer>>(0.0, new HashSet<Integer>()));
			return ret;
		}
		
		List<GClause> deltaCons = computeDeltaConstraints(gcs);

		String logFile = null;
		if (Config.saveMaxSATInstance) {
			logFile = Config.dir_working + File.separator
					+ this.hashCode() + "_inc." + iter + ".wcnf";

			IncMan.logClauses(queryInOrder, logFile);
		}
		else {
			logFile = Config.dir_working + File.separator
					+ this.hashCode() + ".wcnf";
		}

		if (Config.verbose_level >= 1) {
			System.out.println("delta size: " + deltaCons.size());
			System.out.println("#Hard: " + deltaHardCount);
			System.out.println("#Soft: " + deltaSoftCount);
		}
		
		if (Config.verbose_level >= 2) {
			if (Config.saveMaxSATInstance) {
				String deltaFile = Config.dir_working + File.separator
						+ this.hashCode() + "_delta.wcnf." + iter + ".iter";

				IncMan.logClauses(deltaCons, deltaFile);
			}

			if (Config.verbose_level >= 3) {
				for (GClause e : deltaCons) {
					System.out.println(e);
				}
			}
		}

		++iter;
		for(GClause gc : deltaCons){
			prevQuery.add(gc);
		}
		
		this.buildVarIdMap(queryInOrder);
		
		//this.buildVarIdMap(gcs);
		File consFile = null;
		consFile = new File(logFile);
		//if(Config.saveMaxSATInstance)
		//	consFile = new File(Config.dir_working + File.separator + this.hashCode()+"."+(instanceCounter++)
		//		+ ".wcnf");
		//else
		//	consFile = new File(Config.dir_working + File.separator + this.hashCode()
		//		+ "_mifu.wcnf");
		
		try {
			String genInTimer = "Maxsat input timer";
			Timer.start(genInTimer);
			PrintWriter pw = new PrintWriter(consFile);
			pw.println("c mln inference: lazy grouding");
			// Calculate the num of rules and variables
			this.calculateStats(gcs);
			pw.println("p wcnf " + this.nbvar + " " + this.nbclauses + " "
				+ this.top);
			UIMan.verbose(1, "Solving a MaxSAT problem of "+this.nbvar+" variables and "+this.nbclauses+" clauses.");
			double totalWeight = 0.0;
			Set<Integer> tseitinLits = new HashSet<Integer>();
			int nextTseitinLitId = this.highestVarId + 1;
			List<GClause> clauses = new ArrayList<GClause>(gcs);
			Collections.sort(clauses,gclauseComparator);
			for (GClause gc : clauses) {
				if (gc.isPositiveClause()) {
					if (gc.isHardClause()) {
						pw.print(this.top);
					} else {
						double w = gc.weight;
						pw.print((int) w);
						totalWeight += w;
					}
					for (int lit : gc.lits) {
						pw.print(" " + this.getNewLit(lit));
					}
					pw.println(" 0");
				} else {
					//Apply Tseitin transformation
					int newTseitinLitId = nextTseitinLitId++;
					tseitinLits.add(newTseitinLitId);
					pw.print(this.top);
					for (int lit : gc.lits) {
						pw.print(" " + this.getNewLit(lit));
					}
					pw.print(" " + newTseitinLitId);
					pw.println(" 0");
					
					for (int lit : gc.lits) {
						pw.print(this.top);
						pw.print(" " + -newTseitinLitId);
						pw.print(" " + -this.getNewLit(lit));
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
			cmd[0] = Config.maxsat_path;
			cmd[1] = consFile.getAbsolutePath();
			UIMan.verbose(2, "Using solver: "+Config.maxsat_path);
			UIMan.verbose(1, "Start the MAXSAT solver:");
			String maxsatTimer = "MAXSAT";
			Timer.start(maxsatTimer);
			ProcessBuilder pb = null;
			if(Config.MEM_TAG == null || Config.MEM_OUT_FOLDER == null)
				pb = new ProcessBuilder(cmd[0], cmd[1]);
			else{
				pb = new ProcessBuilder("/usr/bin/time","-v","-o",Config.MEM_OUT_FOLDER+File.separator+Config.MEM_TAG+"mem_log.txt",cmd[0], cmd[1]);	
			}
			pb.redirectErrorStream(true);
			final Process p = pb.start();
			super.pList.add(p);
			ResultInterpreter ri =
				new ResultInterpreter(p.getInputStream(), totalWeight);

			Future<Pair<Double, Set<Integer>>> futureResult =
				Config.executor.submit(ri);

			int status = p.waitFor();
			if (status != 0 && status != 30) {
				super.pList.remove(p);
				throw new RuntimeException(
					"The MAXSAT solver did not terminate normally, consFile="+ consFile);
			}

			super.pList.remove(p);
			
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
		String interOutTimer = "Maxsat output timer";
		Timer.start(interOutTimer);
		try {
			Scanner sc = new Scanner(result);
			Set<Integer> ret = new HashSet<Integer>();
			while (sc.hasNext()) {
				String line = sc.nextLine();
				if (line.startsWith("s ")) {
					if (line.startsWith("s UNSATISFIABLE")) {
						return null;
					}
					if (!line.startsWith("s OPTIMUM FOUND")) {
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
	public static Comparator<GClause> gclauseComparator = new Comparator<GClause>() {

		public int compare(GClause g1, GClause g2) {
			int a = g1.hashCode();
			int b = g2.hashCode();
			if(a < b) return -1;
			if(a == b) return 0;
			return 1;
			
		   //return g1.hashCode()-g2.hashCode();
	}};
	private void buildVarIdMap(List<GClause> gcs){
		List<GClause> clauses = new ArrayList<GClause>(gcs);
		Collections.sort(clauses,gclauseComparator);
		for(GClause gc : clauses)
			for(int l : gc.lits){
				int at = Math.abs(l);
				if(!varIdMap.containsKey(at)){
					varIdMap.put(at, varList.size());
					varList.add(at);
				}
			}
	}
	
	public int getNewLit(int oldLit){
		if(oldLit > 0){
			return varIdMap.get(oldLit);
		}
		if(oldLit < 0)
			return -varIdMap.get(0-oldLit);
		throw new RuntimeException("Var id cannot be 0.");
	}
	
	public int getOldLit(int newLit){
		if(newLit > 0)
			return varList.get(newLit);
		if(newLit < 0)
			return -varList.get(0-newLit);
		throw new RuntimeException("Var id cannot be 0.");
	}
	
	private void calculateStats(Set<GClause> gcs) {
		this.nbclauses = gcs.size();
//		Set<Integer> atomSet = new HashSet<Integer>();
		this.top = 0;
		for (GClause gc : gcs) {
			if (!gc.isHardClause()) {
				this.top += Math.abs(gc.weight);
			}
//			for (int l : gc.lits) {
//				if (Math.abs(l) > this.highestVarId) {
//					this.highestVarId = Math.abs(l);
//				}
//				atomSet.add(Math.abs(l));
//			}
		}
//		this.nbvar = atomSet.size();
		this.nbvar = this.varList.size()-1;
		this.highestVarId = this.varList.size()-1;
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
				if (line.startsWith("s ")) {
					if (line.startsWith("s UNSATISFIABLE")) {
						return null;
					}
					if (!line.startsWith("s OPTIMUM FOUND")) {
						throw new RuntimeException(
							"Expecting a solution but got " + line);
					}
				}
				if (line.startsWith("o ")) {
					String lsplits[] = line.split(" ");
					unsatWeight = Double.parseDouble(lsplits[1]);
				}
				if (line.startsWith("v ")) {
					String[] lsplits = line.split(" ");
					for (String s : lsplits) {
						s = s.trim();
						if (s.equals("v")) {
							continue;
						}
						int i = Integer.parseInt(s);
						if (i > 0 && i <= highestVarId) {
							sol.add(getOldLit(i));
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

	@Override
	public Pair<Double, Set<Integer>> refine(Set<GClause> checkSet,
			Set<Integer> right, Double left) {
		Pair<Double,Set<Integer>> refined = this.solveWithTimeout(checkSet).get(0);
		if(Math.abs(refined.left-left) < 0.1 )
			return null;
		if(refined.left < left)
			 throw new RuntimeException("The provided objective function is not achievable: "+left);
		return refined;
	}
	
}

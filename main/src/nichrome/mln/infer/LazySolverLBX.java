package nichrome.mln.infer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.commons.lang3.Pair;

import nichrome.mln.GClause;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.util.Config;
import nichrome.mln.util.Timer;
import nichrome.mln.util.UIMan;

public class LazySolverLBX extends LazySolver {


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
	private Map<Integer,Integer> varIdMap;
	/**
	 * new var id -> old var id
	 */
	private List<Integer> varList;
	
	public static int instanceCounter = 0;
	
	public LazySolverLBX(MarkovLogicNetwork mln) {
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
		this.buildVarIdMap(gcs);
		File consFile = null;
		if(Config.saveMaxSATInstance)
			consFile = new File(Config.dir_working + File.separator + this.hashCode()+"_"+(instanceCounter++)
				+ "_mifu.wcnf");
		else
			consFile = new File(Config.dir_working + File.separator + this.hashCode()
				+ "_mifu.wcnf");
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

			UIMan.verbose(2, "Using solver: "+Config.lbx_path);
			UIMan.verbose(1, "Start the LBX solver:");
			String maxsatTimer = "LBX";
			Timer.start(maxsatTimer);
			ProcessBuilder pb = null;
			if(Config.MEM_TAG == null || Config.MEM_OUT_FOLDER == null)
				pb = new ProcessBuilder(Config.lbx_path, "-mxapp", "-wm", "-nw", 
						"-num", Config.lbx_numLimit+"", "-T", Config.lbx_timeout+"",
						consFile.getAbsolutePath());
			else{
				pb = new ProcessBuilder("/usr/bin/time","-v","-o",Config.MEM_OUT_FOLDER+File.separator+Config.MEM_TAG+"mem_log.txt",
						Config.lbx_path, "-mxapp", "-wm", "-nw", 
						"-num", Config.lbx_numLimit+"", "-T", Config.lbx_timeout+"",
						consFile.getAbsolutePath());
			}
			pb.redirectErrorStream(true);
			final Process p = pb.start();
			super.pList.add(p);
			ResultInterpreter ri =
				new ResultInterpreter(p.getInputStream(), totalWeight);

			Future<Pair<Double, Set<Integer>>> futureResult =
				Config.executor.submit(ri);

//			if (p.waitFor() != 0) {
//				super.pList.remove(p);
//				throw new RuntimeException(
//					"The MAXSAT solver did not terminate normally");
//			}

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

			verifySolution(gcs, retP.left, retP.right);
			UIMan.verbose(2, "################Sanity check passed! Remove this check when we're confident with"
					+ "the implementation.\n");

			return ret;
			// return interpreteResult(result);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void buildVarIdMap(Set<GClause> gcs){
		varIdMap = new HashMap<Integer, Integer>();
		varList = new ArrayList<Integer>();
		varList.add(null);//Skip 0 in id list
		for(GClause gc : gcs)
			for(int l : gc.lits){
				int at = Math.abs(l);
				if(!varIdMap.containsKey(at)){
					varIdMap.put(at, varList.size());
					varList.add(at);
				}
			}
	}
	
	public int getNewLit(int oldLit){
		if(oldLit > 0)
			return varIdMap.get(oldLit);
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
			boolean ifTimeout = false;
			Set<Integer> sol = null;
			double cost = -1.0;
			String line;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("c MCS UB:")) {
					String tokens[] = line.split("\\s+");
					double curCost = Double.parseDouble(tokens[tokens.length-1]);
					String modelLine = in.readLine();
					if(cost < 0 || curCost < cost){
						cost = curCost;
						sol = new HashSet<Integer>();
						String asgns[] = modelLine.split("\\s+");
						for(int i = 2 ; i < asgns.length; i++){
							int asgn = Integer.parseInt(asgns[i]);
							if(asgn > 0 && asgn <= highestVarId)
								sol.add(getOldLit(asgn));
						}
					}
				}
				if (line.startsWith("c LBX timed out"))
					ifTimeout = true;
			}
			in.close();
			if(sol != null){
				Pair<Double, Set<Integer>> ret =
						new Pair<Double, Set<Integer>>(this.totalWeight - cost,
								sol);
				return ret;
			}
			if(!ifTimeout){ // Not timeout but no solution, UNSAT
				return null;

			}
			throw new RuntimeException("LBX fails to find a solution within "+Config.lbx_timeout+" secs.");
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
	
	private static void verifySolution(Set<GClause> gcs, double obj, Set<Integer> sol){
		double obj1 = 0.0;
		OUT: for(GClause gc : gcs){
			for(int l : gc.lits){
				int al = Math.abs(l);
				if(l > 0 && sol.contains(al)){
					if(!gc.isHardClause())
						obj1 += gc.weight;
					continue OUT;
				}
				
				if(l < 0 && !sol.contains(al)){
					if(!gc.isHardClause())
						obj1 += gc.weight;
					continue OUT;
				
				}
			}
			if(gc.isHardClause())
				throw new RuntimeException("Hard clauses violated by solution found by LBX!");
		}
		if(obj1 != obj)
			throw new RuntimeException("Objective function does not match!");
	}
}

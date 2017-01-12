package nichrome.mln.infer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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

public class LazySolverIncrementalMaxSAT extends LazySolver {

	final static int MAX_LEN = 1<<20;
	final static byte[] buf = new byte[MAX_LEN];
	
	/**
	 * See http://maxsat.ia.udl.cat/requirements/ nbvar: number of variables
	 * nbclauses: number of clauses top: hard constraint weight = sum of soft
	 * weights + 1
	 */
	private long nbvar = 0;
	private long nbclauses = 0;
	private int highestVarId = 0;
	private long top = (long) Config.hard_weight;
	
	private Set<GClause> prevQuery;
	private List<GClause> queryInOrder;
	private List<Pair<Double, Set<Integer>>> prevResult;
	//private File answerPipeFile;
	//private File queryPipeFile;
	private PrintWriter queryPipe;
	private BufferedReader answerPipe;
	//private BlockedReader answerPipe;
	private double totalWeight = 0.0;
	
	
	private static int iter = 0;
	private static int emptyCt = 0;
	private static int deltaHardCount = 0;
	private static int deltaSoftCount = 0;
	
	//private static boolean working = false;
	
	/**
	 * old var id -> new var id
	 */
	private static Map<Integer,Integer> varIdMap;
	/**
	 * new var id -> old var id
	 */
	private static List<Integer> varList;
	
	//public static int instanceCounter = 0;
	
	static{
		varIdMap = new HashMap<Integer, Integer>();
		varList = new ArrayList<Integer>();
		varList.add(null);//Skip 0 in id list
	}

	class BlockedReader{
		FileInputStream ins;
		BlockedReader( FileInputStream fin ){
			ins = fin;
		}
		public String readLine() {
			
			int i = 0;
			while(true) {
				if (i >= LazySolverIncrementalMaxSAT.MAX_LEN){
					break;
				}
				
				try {
					byte x = (byte) ins.read();
					if(x == '\n') {
						break;
					}
					buf[i ++] = x;
				} catch (IOException e) {
					System.err.println("read error in BlockedReader");
					e.printStackTrace();
				}				
			}

			return new String(buf, 0, i );
		}
	}
	

	public static void createFifoPipe(String fifoPathName) {
	    Process process = null;
	    String[] command = new String[] {"mkfifo", fifoPathName};
	    try {
			process = new ProcessBuilder(command).start();
		    if(process.waitFor() != 0){
		    	System.out.println("Failed to create fifo: " + fifoPathName);
		    	System.exit(-1);
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public static void startSolverThread(String inputPipe, String outputPipe) {
	    final String[] command = new String[] { Config.maxsat_path,
				inputPipe, outputPipe};

		(new Thread() {			
			public void run() {
				try {
					new ProcessBuilder(command).start();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public LazySolverIncrementalMaxSAT(MarkovLogicNetwork mln) {
		super(mln);

		prevQuery = new HashSet<GClause>();
		queryInOrder = new LinkedList<GClause>();

		//String answerPipeName = Config.openWBOAnswerPipe;
		//String queryPipeName = Config.openWBOQueryPipe;
		//if(answerPipeName == null || queryPipeName == null) {
		//	System.err.println("There is no (or incorrect) configuration for: answerPipe(" 
		//			+ answerPipeName+ "), queryPipe( " + queryPipeName + ")");
		//	System.exit(-1);
		//}
		
		try{
			// create named pipe
			
			String answerPipePathName = Config.dir_working + File.separator + "answerPipe";
			createFifoPipe(answerPipePathName);
			//System.out.println("Created name pipe: " + answerPipePathName);
			
			String queryPipePathName = Config.dir_working + File.separator + "queryPipe";
			createFifoPipe(queryPipePathName);
			//System.out.println("Created name pipe: " + queryPipePathName);
			//System.out.flush();
			
			startSolverThread(queryPipePathName,  answerPipePathName);
			
			// the open order really matters, since it may cause deadlock if the order is different
			// from the open order of underlying incremental maxsat solver
			queryPipe =  new PrintWriter( new File(queryPipePathName) );
			//System.out.println("query Pipe is opened.");
			
			//answerPipe = new BlockedReader( new FileInputStream( answerPipeName) );
			answerPipe = new BufferedReader(new FileReader(answerPipePathName));
			//System.out.println("answer Pipe is opened.");
			
			if(Config.verbose_level >= 1){
				System.out.println("Incremental Solver initialization is done.");
			}
		}
		catch(IOException ex) {
			System.err.println("Got an error when open pipe");
			ex.printStackTrace();
			System.exit(-1);
		}
	}
	
	public static Comparator<GClause> gclauseComparator = new Comparator<GClause>() {

		public int compare(GClause g1, GClause g2) {
			
		   return g1.hashCode()-g2.hashCode();
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


	private void calculateStats(List<GClause> gcs) {
		this.nbclauses = gcs.size();
		this.top = 0;
		for (GClause gc : gcs) {
			if (!gc.isHardClause()) {
				this.top += Math.abs(gc.weight);
			}
		}
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

	private void sendConstraintsToSolver(List<GClause> deltaCons) {
		
		// init start
		// queryPipe.println("a");
		
		// print comment (optional)
		queryPipe.println("c mln inference: lazy grouding");
		
		// Calculate the num of rules and variables
		this.calculateStats(deltaCons);
		
		// print header info
		queryPipe.println("p wcnf " + this.nbvar + " " + this.nbclauses + " "
			+ this.top);
		
		//UIMan.verbose(1, "Solving an incremental MaxSAT problem, delta info: "+this.nbvar+" variables and "+this.nbclauses+" clauses.");
		
		Set<Integer> tseitinLits = new HashSet<Integer>();
		int nextTseitinLitId = this.highestVarId + 1;
		List<GClause> clauses = new ArrayList<GClause>(deltaCons);
		Collections.sort(clauses,gclauseComparator);
		for (GClause gc : clauses) {
			if (gc.isPositiveClause()) {
				if (gc.isHardClause()) {
					queryPipe.print(this.top);
				} else {
					double w = gc.weight;
					queryPipe.print((int) w);
					totalWeight += w;
				}
				for (int lit : gc.lits) {
					queryPipe.print(" " + this.getNewLit(lit));
				}
				queryPipe.println(" 0");
			} else {
				//Apply Tseitin transformation
				int newTseitinLitId = nextTseitinLitId++;
				tseitinLits.add(newTseitinLitId);
				queryPipe.print(this.top);
				for (int lit : gc.lits) {
					queryPipe.print(" " + this.getNewLit(lit));
				}
				queryPipe.print(" " + newTseitinLitId);
				queryPipe.println(" 0");
				
				for (int lit : gc.lits) {
					queryPipe.print(this.top);
					queryPipe.print(" " + -newTseitinLitId);
					queryPipe.print(" " + -this.getNewLit(lit));
					queryPipe.println(" 0");
				}
				
				if (gc.isHardClause()) {
					queryPipe.print(this.top);
				} else {
					double w = -gc.weight;
					queryPipe.print((int) w);
					totalWeight += w;
				}
				queryPipe.print(" " + newTseitinLitId);
				queryPipe.println(" 0");
			}
		}
		
		// mark end of this phase
		queryPipe.println("e");
		
		// clear the buffer
		queryPipe.flush();
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
	
	private Pair<Double, Set<Integer>> interpretResult() throws Exception {
			//new BufferedReader(new InputStreamReader(this.resultStream));
		Set<Integer> sol = new HashSet<Integer>();
		double unsatWeight = 0.0;
		String line;
		while ((line = answerPipe.readLine()) != null) {
			if (line.startsWith("e") || line.startsWith("Q") ) {
				break;
			}
			else if (line.startsWith("s ")) {
				if (line.startsWith("s UNSATISFIABLE")) {
					return null;
				}
				if (!line.startsWith("s OPTIMUM FOUND")) {
					throw new RuntimeException(
						"Expecting a solution but got " + line);
				}
			}
			else if (line.startsWith("o ")) {
				String lsplits[] = line.split(" ");
				unsatWeight = Double.parseDouble(lsplits[1]);
			}
			else if (line.startsWith("v ")) {
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
			else if(line.startsWith("c ")) {
				if(Config.verbose_level >= 1){
					// c  Nb (UN)SAT calls:
					// c  CS:  
					System.out.println(line.substring(3));
				}
			}
			else{
				System.out.println("Unknown format in solution: " + line);
			}
		}
		
		if (line == null) {
			throw new RuntimeException("line is null, solver may be killed.");
		}

		Pair<Double, Set<Integer>> ret =
			new Pair<Double, Set<Integer>>(this.totalWeight - unsatWeight,
				sol);
		return ret;
	}

	
	@Override
	public List<Pair<Double, Set<Integer>>> solve(Set<GClause> gcs) {

		
		if (Config.verbose_level >= 2) {
			if (prevQuery != null) {
				System.out.println("previous gcs size: " + prevQuery.size());
				if (prevResult != null) {
					Pair<Double, Set<Integer>> answer = prevResult.get(0);
					System.out.println("previous result, goal: " + answer.left
							+ ", size: " + answer.right.size());
				} else {
					System.out.println("previous result is null ");
				}
			}
			System.out.println("gcs size: " + gcs.size());
			if (Config.verbose_level >= 3) {
				for (GClause e : gcs) {
					System.out.println(e);
				}
			}
		}

		ArrayList<Pair<Double, Set<Integer>>> ret = new ArrayList<Pair<Double, Set<Integer>>>();

		// compute the delta
		// System.out.println("compute delta...");
		List<GClause> deltaCons = computeDeltaConstraints(gcs);

		if (Config.saveMaxSATInstance) {
			String logFile = Config.dir_working + File.separator
					+ this.hashCode() + "_inc." + iter + ".wcnf";

			IncMan.logClauses(queryInOrder, logFile);
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

		// trivial case: deltaCons is empty
		if (deltaCons == null || deltaCons.isEmpty()) {
			++emptyCt;

			if (emptyCt > 5) {
				throw new RuntimeException("Too many times empty delta: emptyCt=" + emptyCt);
			}

			if(prevResult == null) {
				ret.add(new Pair<Double, Set<Integer>>(0.0, new HashSet<Integer>()));
				return ret;
			}
			else{
				return prevResult;
			}
			
		}

		// System.out.println("build VarId Map...");
		this.buildVarIdMap(deltaCons);


		String maxsatTimer = "MAXSAT";
		Timer.start(maxsatTimer);

		// System.out.println("Send delta to incremental MaxSAT solver ...");
		// send delta to incremental maxsat solver
		try {
			String genInTimer = "Maxsat input timer";
			Timer.start(genInTimer);

			sendConstraintsToSolver(deltaCons);
			
			if (Config.verbose_level >= 1) {
				Timer.printElapsed(genInTimer);
			}

		} catch (Exception e) {
			System.err.println("Failed to send constraints to incremental MaxSAT solver!");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
				
		// System.out.println("wait and interpret result ...");
		// wait and interpret result from incremental MaxSAT
		try {
			Pair<Double, Set<Integer>> answer = interpretResult();
			ret.add( answer );

			if (Config.verbose_level >= 1) {
				Timer.printElapsed(maxsatTimer);
			}
			
			if (Config.verbose_level >= 2) {
				System.out.println("goal: " + answer.left + ", size: "
						+ answer.right.size());
				if (Config.verbose_level >= 3) {
					System.out.println("*** answer begin ***");
					for (Integer i : answer.right) {
						System.out.println(i);
					}
					System.out.println("*** answer end ***");
				}
			}

		}catch(Exception e){
			System.err.println("Failed to interpret result");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		
		// System.out.println("incremental maxsat solver finished current iteration.");
		
		// update prevQuery with most recent query
		//prevQuery = gcs;
		for(GClause gc : deltaCons){
			prevQuery.add(gc);
		}
		
		prevResult = ret;
		
		return ret;
	}

}

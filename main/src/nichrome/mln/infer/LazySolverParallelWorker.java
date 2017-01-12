package nichrome.mln.infer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.Pair;

import nichrome.mln.GClause;
import nichrome.mln.util.Config;
import nichrome.mln.util.FileMan;
import nichrome.mln.util.NamedThreadFactory;
import nichrome.mln.util.UIMan;

public class LazySolverParallelWorker{
	private String solverConfig = null;
	private int port;

	public LazySolverParallelWorker(int port, String solverConfig) {
		super();
		this.port = port;
		this.solverConfig = solverConfig;
	}

	private LazySolver createSolver(){
		LazySolver solver = null;
		if (solverConfig.equals(Config.ILP_SOLVER)) {
			solver = new LazySolverILP(null);
		} else if (Config.solver.equals(Config.LBX_SOLVER)){
			solver = new LazySolverLBX(null);
		} else if (Config.solver.equals(Config.MCSLS_SOLVER)) {
			solver = new LazySolverMCSls(null);
		} else if (Config.solver.equals(Config.WALK_SOLVER)) {
			solver = new LazySolverWalkSAT(null);
		} else if (Config.solver.equals(Config.TUFFY_SOLVER)) {
			solver = new LazySolverT(null);
		} else if (Config.solver.equals(Config.TWO_STAGE_SOLVER)) {
			solver = new LazySolverTwoStage(null);
		} else if (Config.solver.equals(Config.LBX_MCS_SOLVER)){
			solver = new LazySolverLBXMCS(null);
		}
		else {
			solver = new LazySolverMaxSAT(null);
		}
		return solver;
	}
	
	public void listen() {
		try {
			UIMan.verbose(0, "MaxSAT worker start listenting at port "+port);
			ServerSocket socket = new ServerSocket(port);
			while(true){
				final Socket client = socket.accept();
				new Thread(new Runnable(){
					@Override
					public void run() {
						try{
							PrintWriter pw = new PrintWriter(client.getOutputStream());
							BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
							int numClauses = Integer.parseInt(reader.readLine().trim());
							Set<GClause> problem = new HashSet<GClause>();
							for(int i = 0 ; i < numClauses; i++){
								String line = reader.readLine().trim();
								String tokens [] = line.split(" ");
								double weight = Double.parseDouble(tokens[0]);
								int numLits = tokens.length - 2;
								if(weight == -1)
									weight = Config.hard_weight;
								int lits[] = new int[numLits];
								for(int j = 1; j < tokens.length - 1; j++){
									lits[j-1] = Integer.parseInt(tokens[j]);
								}
								GClause gc = new GClause(weight,lits);
								problem.add(gc);
							}
							List<Pair<Double, Set<Integer>>> result = createSolver().solve(problem);
							Pair<Double,Set<Integer>> candidate = result.get(0);
							if(candidate == null){
								pw.println(-1);
							}else{
								pw.println(candidate.left);
								pw.println(candidate.right.size());
								for(int asgn : candidate.right)
									pw.println(asgn);
							}
							pw.flush();
							pw.close();
							reader.close();
							client.close();
						
						}catch(Exception e){
							throw new RuntimeException(e);
						}
					}
				}).start();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Parameter format: <port> <solver config> <config file path>
	 * @param args
	 */
	public static void main(String args[]){
		Config.executor =
				Executors.newCachedThreadPool(new NamedThreadFactory(
						"MLN thread pool", true));

		int port = Integer.parseInt(args[0]);
		UIMan.parseConfigFile(args[2]);
		FileMan.ensureExistence(Config.getWorkingDir());
		Runtime.getRuntime().addShutdownHook(new Thread() {// This is a
			@Override
			public void run() {
				System.out.print("Removing temporary dir '"
						+ Config.getWorkingDir() + "'...");
				System.out.println(FileMan
						.removeDirectory(new File(Config
								.getWorkingDir())) ? "OK" : "FAILED");
			}
		});

		
		LazySolverParallelWorker worker = new LazySolverParallelWorker(port, args[1]);
		worker.listen();
	}
	
}

package nichrome.mln.infer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.Pair;

import nichrome.mln.GClause;
import nichrome.mln.util.Config;
import nichrome.mln.util.NamedThreadFactory;

public class LazySolverParallel {
	private Map<Worker,Integer> workLoadMap;
	public final static int MAX_WORK_LOAD = 4;
	
	public LazySolverParallel(){
		workLoadMap = new HashMap<Worker,Integer>();
	}
	
	public void registerWorker(String addr, int port){
		Worker worker = new Worker();
		worker.addr = addr;
		worker.port = port;
		if(!workLoadMap.containsKey(worker)){
			workLoadMap.put(worker, 0);
		}
	}
	
	public int getNumWorkers() {
		return workLoadMap.size();
	}
	
	public Future<Pair<Double,Set<Integer>>> solve(Set<GClause> clauses){
		Map.Entry<Worker, Integer> candidate = null;
		synchronized (workLoadMap) {
			for(Map.Entry<Worker, Integer> entry : workLoadMap.entrySet()){
				if(candidate == null)
					candidate = entry;
				else
					if(entry.getValue() < candidate.getValue()){
						candidate = entry;
					}
			}	
			if(candidate == null)
				throw new RuntimeException("No MaxSAT workers!");
			if(candidate.getValue() >= MAX_WORK_LOAD)
				return null; //reject the current task, as all workers are busy
			workLoadMap.put(candidate.getKey(), candidate.getValue()+1);
		}
		MaxSATTask task = new MaxSATTask(candidate.getKey(), clauses);
		return Config.executor.submit(task);
	}
	
	public static void main(String args[]) throws Exception{
		Config.executor =
			Executors.newCachedThreadPool(new NamedThreadFactory(
				"MLN thread pool", true));
		LazySolverParallel master = new LazySolverParallel();
		master.registerWorker("127.0.0.1", 8888);
		master.registerWorker("127.0.0.1", 9999);
		Set<GClause> problem = new HashSet<GClause>();
		problem.add(new GClause(1, 1, -2, 3));
		problem.add(new GClause(Config.hard_weight, 1));
		problem.add(new GClause(2, 3));
		List<Future<Pair<Double,Set<Integer>>>> results = new ArrayList<Future<Pair<Double,Set<Integer>>>>();
		for(int i = 0 ; i< 100;i++){
			while(true){
				Future<Pair<Double,Set<Integer>>> r = master.solve(problem);
				if(r != null){
					results.add(r);
					break;
				}
				else{
//					System.out.println("All workers busy when submitting Problem "+i);
				}
			}
		}
		for(Future<Pair<Double,Set<Integer>>> r: results ){
			System.out.println(r.get());
		}
	}
	
	class MaxSATTask implements Callable<Pair<Double,Set<Integer>>>{
		private Worker worker;
		private Set<GClause> problem;

		public MaxSATTask(Worker worker, Set<GClause> problem) {
			super();
			this.worker = worker;
			this.problem = problem;
		}

		@Override
		public Pair<Double, Set<Integer>> call() {
			try{
				Socket socket = new Socket(worker.addr,worker.port);
				PrintWriter pw = new PrintWriter(socket.getOutputStream());
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				int numClauses = problem.size();
				pw.println(numClauses);
				for(GClause gc : problem){
					if(gc.isHardClause())
						pw.print(-1);
					else
						pw.print(gc.weight);
					for(int lit : gc.lits)
						pw.print(" "+lit);
					pw.println(" 0");
				}
				pw.flush();
				double obj = Double.parseDouble(reader.readLine().trim());
				if(obj < 0){
					pw.close();
					reader.close();
					socket.close();
					synchronized (workLoadMap) {
						int count = workLoadMap.get(worker);
						workLoadMap.put(worker, count-1);
					}
					return null;
				}
				int numPos = Integer.parseInt(reader.readLine().trim());
				Set<Integer> sol = new HashSet<Integer>();
				for(int i = 0 ; i < numPos; i++)
					sol.add(Integer.parseInt(reader.readLine().trim()));
				pw.close();
				reader.close();
				socket.close();
				synchronized (workLoadMap) {
					int count = workLoadMap.get(worker);
					workLoadMap.put(worker, count-1);
				}
				return new Pair<Double,Set<Integer>>(obj,sol);
			}catch(Exception e){
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

	}
}


class Worker{
	String addr;
	int port;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((addr == null) ? 0 : addr.hashCode());
		result = prime * result + port;
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Worker other = (Worker) obj;
		if (addr == null) {
			if (other.addr != null)
				return false;
		} else if (!addr.equals(other.addr))
			return false;
		if (port != other.port)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Worker [addr=" + addr + ", port=" + port + "]";
	}
	
}
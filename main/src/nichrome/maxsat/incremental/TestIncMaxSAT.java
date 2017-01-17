package nichrome.maxsat.incremental;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Pair;

import nichrome.mln.GClause;
import nichrome.mln.infer.LazySolver;
import nichrome.mln.infer.LazySolverIncrementalMaxSAT;
import nichrome.mln.parser.CommandOptions;
import nichrome.mln.util.Config;
import nichrome.mln.util.Timer;
import nichrome.mln.util.UIMan;


public class TestIncMaxSAT {
	private static LazySolver solver;
	
	public static Set<GClause> parseFile(String fileInput) throws Exception{
		Set<GClause> gcs  = new HashSet<GClause>();
		
		int hardWeight = 0;

		BufferedReader br = new BufferedReader(new FileReader(fileInput));
		String line;
		int ct = 0;
		while ((line = br.readLine()) != null) {
			++ct;
			if (line.startsWith("c") ) {
				continue;
			}
			
			if(line.startsWith("p")) {
				String[] r = line.split(" ");
				hardWeight = Integer.valueOf( r[4] );
				continue;
			}
			
			String[] lsplits = line.split(" ");
			double w = 0;
			int [] res = new int[ lsplits.length-2];
			
			for(int k = 0; k < lsplits.length-1; ++k){
			//for (String s : lsplits) {
				String s = lsplits[k];
				s = s.trim();
				
				int tmp = 0;
				if(k == 0){
					int x = Integer.parseInt(s);
					tmp = x;
					w = (x == hardWeight) ? Config.hard_weight : x;
				}
				else{
					tmp = res[k-1] = Integer.parseInt(s);
				}

				if (tmp == 0){
					System.out.println("check line: " + ct + ", file: " + fileInput);
					System.exit(-1);
				}
			}

			gcs.add(  new GClause(w, res) );

		}
		
		return gcs;
	}
	
	public static void outputResult(List<Pair<Double, Set<Integer>>> result){
		for (Pair<Double, Set<Integer>> solution : result) {
			if (solution != null) {
				System.out.println("w  " + solution.left);
				for(Integer x : solution.right){
					System.out.println("o " + x);
				}
			}
		}
	}
	
	public static void main(String args[]) {
		
		if (args.length != 3) {
			System.out.println("Expected arguments: base_name  answerPipe queryPipe");
			System.exit(0);
		}
		


		String baseName = args[0];
		String answerPipe = args[1];
		String queryPipe = args[2];
		
		System.out.println("baseName: " + baseName );
		System.out.println("answerPipe: " + answerPipe );
		System.out.println("queryPipe: " + queryPipe );

		String [] empty = {};
		CommandOptions options = UIMan.parseCommand(empty);
		
		//solver = new LazySolverIncrementalMaxSAT(null, answerPipe, queryPipe);
		solver = new LazySolverIncrementalMaxSAT(null);
		
		
		for(int i = 0; i< 49; ++i){

			String fileInput = baseName + "." + i + ".iter";
			Set<GClause> gcs = null;
			
			try {
				gcs  = parseFile(fileInput);	
			} catch(Exception e) {
				e.printStackTrace();
			}
			solver.solve(gcs);
			//outputResult( solver.solve(gcs) );
			
		
		}
	}
}

package nichrome.mln.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import nichrome.mln.GClause;

public class IncMan {
	public static void logClauses(List<GClause> gcs, String filePath) {
		String logTimer = "logTimer";
		Timer.start(logTimer);
		
		try {
			PrintWriter pw = new PrintWriter(new File(filePath));
			int nc = gcs.size();
			int nv = 0;
			long hw = (long) Config.hard_weight;
			for(GClause e : gcs){
				for(int x : e.lits){
					if (x < 0) x = -x;	
					if(nv < x) nv = x;
				}				
			}
			
			pw.println("p wcnf " + nv + " " + nc + " "
					+ hw);
			
			for(GClause e : gcs){
				if(e.isHardClause()){
					pw.print(hw);
				}
				else {
					pw.print( (long) e.weight);
				}
				
				for(int x : e.lits){
					pw.print(" " + x);
				}
				
				pw.println(" 0");
			}			
			
			pw.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("Cannot open the log file: " + filePath);
			e.printStackTrace();
		}
		
		Timer.printElapsed(logTimer);
	}

}

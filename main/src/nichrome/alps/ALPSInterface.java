package nichrome.alps;

import java.io.File;

public class ALPSInterface {
	static {
		//System.out.println(System.getProperty("java.library.path"));
		System.loadLibrary("alps");
	}

	protected String templateFile;
	protected String dataFile;
	protected int K;
	protected int M;
	protected boolean enableG;
	protected boolean enableS;


	public static void main(String [] args) {
		ALPSInterface alps = new ALPSInterface();
		
		alps.templateFile = "/Users/xujie/git/ALPS/templates/all.t";
		alps.dataFile = "/Users/xujie/git/ALPS/data/path.d";
		alps.K = 2;
		alps.M = 4;
		
		alps.enableG = true;
		alps.enableS = true;
		
		alps.learn();
	}
	
	
	public void learn() {
		
		// check if template and data file exists
		{
			File templF = new File(templateFile);
			if(!templF.exists()) {
				System.err.println("Error: Cannot find template file: " + templateFile);
				return;
			}
			
			File dataF = new File(dataFile);
			if(!dataF.exists()) {
				System.err.println("Error: Cannot find data file: " + dataFile);
				return;
			}
		}
		
		int mode = 0;
		if(enableS) mode |= 1;
		if(enableG) mode |= 2;
		
		int res = learnRules(templateFile, dataFile, mode, K, M);
		if(res == 0) {
			System.out.println("finish learn with no errors.");
		}
		else{
			System.out.println("something went wrong.");
		}
	}
	
	public native int learnRules(String templateFile, String dataFile, int mode, int K, int M);
}

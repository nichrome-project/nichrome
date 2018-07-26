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
	protected int augmentations;
	protected int bindings;
	protected boolean enableG;
	protected boolean enableS;
  
  protected boolean enableChain1;
	protected boolean enableChain2;
  protected boolean enableChain3;
	protected boolean enableHeadBinding;

  protected boolean enableFilter1;
	protected boolean enableFilter2;

	public static void main(String [] args) {
		ALPSInterface alps = new ALPSInterface();
		
		alps.templateFile = "/Users/xujie/git/ALPS/templates/all.t";
		alps.dataFile = "/Users/xujie/git/ALPS/data/path.d";
		alps.K = 2;
		alps.M = 4;
		alps.augmentations = 0;
		alps.bindings = 0;
		
		alps.enableG = true;
		alps.enableS = true;

		alps.enableChain1 = false;
		alps.enableChain2 = false;
		alps.enableChain3 = false;
		alps.enableHeadBinding = false;
		alps.enableFilter1 = false;
		alps.enableFilter2 = false;
		
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
		if(enableChain1) mode |= 4;
		if(enableChain2) mode |= 8;
		if(enableChain3) mode |= 16;
		if(enableHeadBinding) mode |= 32;
		if(enableFilter1) mode |= 64;
		if(enableFilter2) mode |= 128;
		
		int res = learnRules(templateFile, dataFile, mode, K, M, augmentations, bindings);
		if(res == 0) {
			System.out.println("finish learn with no errors.");
		}
		else{
			System.out.println("something went wrong.");
		}
	}
	
	// A for augmentations, B for bindings
	public native int learnRules(String templateFile, String dataFile, int mode, int K, int M, int A, int B);
}

package nichrome.alps;

public class ALPSInterface {
	static {
		//System.out.println(System.getProperty("java.library.path"));
		System.loadLibrary("alps");
	}

	String templateFile;
	String dataFile;
	int K;
	int M;
	boolean enableG;
	boolean enableS;


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

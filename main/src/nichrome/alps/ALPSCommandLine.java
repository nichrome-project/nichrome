package nichrome.alps;

import org.kohsuke.args4j.Option;

import nichrome.util.AbstractBaseCommand;
import nichrome.util.CommandInterface;

public class ALPSCommandLine extends AbstractBaseCommand implements CommandInterface {

	//@Option(name = "-h", aliases="-help", usage = "Display command options.")
	//public boolean showHelp = false;
	
	@Option(name="-G",  usage="enable general-to-specific search (top-down)")
	private Boolean enableG = true;

	@Option(name="-S",  usage="enable specific-to-general search (bottom-up)")
	private Boolean enableS = true;

	@Option(name="-M", metaVar="<number>", usage="maximum number of overall rules")
	private Integer M = 1<<30;
	
	@Option(name="-K", metaVar="<number>", required=true, usage="maximum number of rules for each output realtion")
    private Integer K = 1;

    @Option(name="-T", metaVar="<path>", required=true, usage="specify the template file")
    private String templateFile;
    
    @Option(name="-D", metaVar="<path>", required=true, usage="specify the data file")
    private String dataFile;
    

	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
		
//		System.out.println("enableG = " + enableG);
//		System.out.println("enableS = " + enableS);
//		
//		System.out.println("M = " + M);
//		System.out.println("K = " + K);
//		System.out.println("T = " + templateFile);
//		System.out.println("D = " + dataFile);
		
		
		ALPSInterface alps = new ALPSInterface();

		alps.enableG = enableG;
		alps.enableS = enableS;
		
		alps.templateFile = templateFile;
		alps.dataFile = dataFile;
		alps.K = K;
		alps.M = M;
		
		
		alps.learn();

		return true;
	}
	
	@Override
	public String getName() {
	    return "SubCommand ALPS";
	 }

}

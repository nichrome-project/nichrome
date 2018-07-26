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
   
	@Option(name="-a", metaVar="<number>", usage="maximum number of augmentations")
	private Integer augmentations = 0;

	@Option(name="-b", metaVar="<number>", usage="maximum number of bindings")
	private Integer bindings = 0;

	@Option(name="-1", usage="enable chain1 template")
	private Boolean chain1 = false;

	@Option(name="-2", usage="enable chain2 template")
	private Boolean chain2 = false;

	@Option(name="-3", usage="enable chain3 template")
	private Boolean chain3 = false;

	@Option(name="-f1", usage="enable filter1 augmentation")
	private Boolean filter1 = false;

	@Option(name="-f2", usage="enable filter2 augmentation")
	private Boolean filter2 = false;

	@Option(name="-hb", usage="allow bindings in head")
	private Boolean headBinding = false;






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

		alps.augmentations = augmentations;
		alps.bindings = bindings;
		alps.enableChain1 = chain1;
		alps.enableChain2 = chain2;
		alps.enableChain3 = chain3;
		alps.enableHeadBinding = headBinding;
		alps.enableFilter1 = filter1;
		alps.enableFilter2 = filter2;

		
		
		alps.learn();

		return true;
	}
	
	@Override
	public String getName() {
	    return "SubCommand ALPS";
	 }

}

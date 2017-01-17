package nichrome.mln.driver;

import java.io.OutputStreamWriter;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.spi.OptionHandler;

import nichrome.mln.util.Config;
import nichrome.util.AbstractBaseCommand;
import nichrome.util.CommandInterface;

public class MLNCommandLine extends AbstractBaseCommand implements CommandInterface {
	public static String[] outsiderOptions = {
			/* Datalog */
			"-datalog",
			
			/* Essential input/output */
			"-i ", "-mln", "-e ", "-evidence", "-o", "-r", "-result",
			
			/*Auxiliary input/output */
			"-t ", "-train", "-outId", "-keepData", "-verbose", "-printVio",
			"-countVioNoDB", "-loadgc", "-storegc", "-loadrev", "-loadfeedback", 
			"-ignoreWarmGCWeight",
			
			/* Misc */
			"-conf", "-help", "-solver", "-lbx", "-mcsls", "-ilp", //"-tuffy", 
			"-useCAV", "-cpiCheck", "-numSolutions", "-numGIter", "-fullyGround",
			"-groundingTimeout", "-saveMaxSATInstances", "-maxsatSolverPath",
			
			/*Mode selection*/
			"-parallel",
			
			/*Learning-related options*/
			"-learnwt", "-lMaxIter",
			};

	public static OptionHandlerFilter showToOutsider = new OptionHandlerFilter() {
        public boolean select(OptionHandler o) {
        	String optionName = o.option.toString();
        	//System.out.println("Process option: " + o.option.toString() );
        	for(String x : outsiderOptions) {
        		if(optionName.startsWith(x)) {
        			return true;
        		}
        	}
        	return false;
        }
    };
	
	/**
	 * Datalog
	 */
	@Option(name = "-datalog",
		usage = "Run in datalog mode.")
	public boolean isDatalogMode = false;
	
	/**
	 * Query-driven inferences related options
	 */

	@Option(name="-queryFile",
			usage="Input query file(s). Separate with linebreak.")
	public String fquery;
	
	@Option(name="-queryDrivenHorn", usage="Use the special query-driven MaxSAT solver for MaxSAT problems consisting of definite horn"
			+ " clauses and goal clauses of Size 1.")
	public boolean isQueryHorn = false;
	
	@Option(name="-initReachability", usage="Choose the method to compute the initial reachability information"
			+ "ont the derivation graph: \n"
			+ Config.REACH_PRECISE_JAVA+": using java to compute precise initial reachability information.\n"
					+ Config.REACH_PRECISE_DLOG+": using datalog to compute precise intial reachability information.\n"
							+ Config.REACH_APPROX_JAVA+": using java to compute approximate intial reachability information.")
	public int initReachability = Config.REACH_PRECISE_JAVA;

	@Option(name="-queryDriven", usage = "Run in query-driven mode.")
	public boolean isQueryDrivenMode = false;
	
	@Option(name="-forwardBias", usage = "Enable the foward heuristics in expanding of query-driven MaxSAT. "
			+ "Only enable it for user-guided analysis.")
	public boolean forwardBias = false;
	
	@Option(name = "-checker",
			usage = "Choose the checker in the query driven checking phase: \n " + "1. "
					+ Config.OPTIMAL_CHECKER+ "(default): use a MaxSAT solver to check current solution"
							+ "and return the optimal solution on the strengthened constraints.\n"
					+ "2. " + Config.FEASIBLE_CHECKER
					+ ": use ilp solver check the current solution. It returns a better feasible solution when current"
					+ "solution is not optimal.\n")
	public String checker = Config.EXACT_SOLVER;
	
	@Option(name = "-upLevel", usage = "Specify the level of unit propagation. -1 means no limit. 0 means no unit propagation. Default: 1")
	public int upLevel = 1;
	
	@Option(name="-queryEager", usage = "Answer the query by solving the whole MaxSAT problem eagerly.")
	public boolean isQueryDrivenEager = false;
	
	@Option(name="-lfpFile", usage = "Provide the file that we need to enforce least fixpoint on.")
	public String lfpFile;
	
	// Later we might separate the dimacs frontend out.
	@Option(name="-maxsat", usage = "Run as a query-driven MaxSAT solver with a dimacs file as the input.")
	public boolean isMaxsat;
	
	@Option(name="-maxsatTimeout", usage="Specify the maximum time allowing MaxSAT to execute.")
	public int maxSATTimeOut = -1;
	
	@Option(name="-queryCompo", usage = "Solve the MaxSAT problem in a compositional way by solving each query "
			+ "in sequence.")
	public boolean isQueryCompo;
	
	@Option(name="-memOutDir", usage = "Specify the folder to store the memory footprints.")
	public String memOutDir = null;
	
	@Option(name="-logPrefix", usage = "Specify the prefix of various log file.")
	public String logPrefix = "";
	
	@Option(name="-outVisDir", usage = "Specify the output directory for storing the files visualizing the query-driven solving process.")
	public String qVisDir = null;
	
	@Option(name="-warmStart", usage = "In the horn case, warm start qmaxsat.")
	public boolean warmStart = false;
	
	@Option(name="-storeReach", usage = "Store the reachability information to a given file.")
	public String storeReach = null;
	
	@Option(name="-loadReach", usage = "Load the reachability information from a given file.")
	public String loadReach = null;
	
	@Option(name="-checkSolutionPath", usage = "Load a solution and use our db to do the check.")
	public String checkSolutionPath = null;
	
	/**
	 * URSA options
	 */
	@Option(name="-convertToMaxSAT", usage = "Convert the MLN problem to a MaxSAT problem. The output (-r) file will be used to store generated "
			+ "MaxSAT instance. File <ouput>_map will be used to store the variable->atom map.")
	public boolean converToMaxSAT;
	
	@Option(name="-pickFeedback", usage = "Iterative feedback with best feedback picked in each iteration.")
	public boolean pickFeedback;
	
	@Option(name="-pickFeedbackIter", usage = "Iterative feedback on intermediate tuples.")
	public boolean pickFeedbackIter;
	
	@Option(name="-pickFeedbackMincut", usage = "Feedback on intermediate tuples using MinCut.")
	public boolean pickFeedbackMincut;
	
	@Option(name="-mincutIterative", usage = "Use MinCut with iterative mode.")
	public boolean mincutIterative;
	
	@Option(name="-mincutIDBSpur", usage = "Use MinCut with IDB tuples as spurious.")
	public boolean mincutIDBSpur;
	
	@Option(name="-alarmResolution", usage = "URSA Alarm resolution")
	public boolean alarmResolution;
	
	@Option(name="-mincutClient", usage = "The client analysis to which MinCut is being applied. Choose between:\n"
		+ "1. datarace\n"
		+ "2. nullderef\n")
	public String mincutClient;
	
	@Option(name="-oracleQueriesFile",
		usage="Oracle queries file(s). Separate with linebreak.")
	public String fOracle;
	
	@Option(name="-oracleTuplesFile",
		usage="Oracle tuple file(s). Separate with linebreak.")
	public String fOracleTuples;
	
	@Option(name="-oracleSpuriousTupleFile",
		usage="Specify the path to the file containing oracle output for potentially false tuples.")
	public String oracleSpurTupleFile;
	
	@Option(name="-baseSpuriousTupleFile", usage ="Specify the path to the file containing potentially false tuples.")
	public String baseSpurTupleFile;
	
	@Option(name = "-pickBudget", usage = "Specify the number of iterations to be performed for picking feedback. Default: 4")
	public int pickBudget = 4;
	
	@Option(name = "-pickStrategy",
		usage = "Choose the strategy to pick the next feedback: \n "
			+ "1. " + Config.RANDOM_ALL_STRAT + "(default): use random strategy to pick.\n"
			+ "2. " + Config.RANDOM_ALT_STRAT + ": use random strategy with alternative modes to pick.\n"
			+ "3. " + Config.RANDOM_ORACLE_STRAT + ": use random strategy with alternative modes but filtered by oracle to pick.\n"
			+ "4. " + Config.MAXCROSS_ALL_STRAT + ": use max crossover strategy to pick.\n"
			+ "5. " + Config.MAXCROSS_ALT_STRAT + ": use max crossover strategy with alternative modes to pick.\n"
			+ "6. " + Config.MAXCROSS_ORACLE_STRAT + ": use max crossover strategy with alternative modes but filtered by oracle to pick.\n")
	public String pickStrategy = Config.RANDOM_ALL_STRAT;
	

	@Option(name = "-feedbackWeight", usage = "Specify weight of the feedback tuples. Default:100")
	public int feedbackWeight = 100;

	@Option(name="-provenanceWeights",
		usage="Run the provenance weights mode to generate provenance weights of intermediate tuples")
	public boolean isProvWeights;

	@Option(name="-provenanceWeightsFile",
		usage="Specify output file for provenance weights of intermediate tuples")
	public String fProvWeights;

	@Option(name = "-numFeedback", usage = "Specify the max number of intermediate to be provided as feedback in each iteration. "
		+ "Default:-1 indicating all possible tuples")
	public int numFeedback = -1;
	
	@Option(name = "-pessimisticRate", usage = "Specify probability with which the pessimistic mode will be chosen. Default:0.5")
	public double pessimisticRate = 0.5;
	
	@Option(name="-isParallel", usage = "Use parallel maxsat solver.")
	public boolean isParallel;
	
	@Option(name="-workerAddr", usage="Specify maxsat worker addresses as follows: <worker1-IP_address>:<worker1-port>##<worker2-IP_address>:<worker2-port>##...")
	public String workerAddr;
	
	@Option(name="-feedbackEncodingSavePath", usage="Specify the folder to save the encoding for feedback selection.")
	public String feedbackEncodingSavePath = null;

	@Option(name="-ursaClient", usage = "The client analysis to which MinCut is being applied. Choose between:\n"
			+ "1. datarace\n"
			+ "2. pts\n")
	public String ursaClient = null;

	@Option(name="-ursaSearchSpace", usage="Tuples that can be asked as questions")
	public String ursaSearchSpace = null;
	
	@Option(name="-ursaPreLabelledTuples", usage="Tuples which are precise and can be labelled at beginning")
	public String ursaPreLabelledTuples = null;
	
	@Option(name="-picker", usage=Config.MODEL_PICKER+". Model guided. "+Config.PLID_PICKER+". Oracle+batch. "+Config.RANDOM_PICKER+". Random.")
	public int picker = Config.MODEL_PICKER;
	
	@Option(name="-ursaExternalModel", usage="Path to external model for ursa. If null, using oracle")
	public String ursaExtermalModel = null;
	
	@Option(name="-appQuestions", usage="Path to file specifying app question tuples.")
	public String appQuestionFile = null;
	
	/**
	 * Essential input/output
	 */
	@Option(name = "-i", aliases = "-mln", required = false,
		usage = "REQUIRED. Input MLN program(s). Separate with comma.")
	public String fprog;

	
	@Option(name = "-e", aliases = "-evidence", required = false,
		usage = "REQUIRED. Input evidence file(s). Separate with comma.")
	public String fevid;

	@Option(name = "-o", aliases = { "-r", "-result" }, required = false,
		usage = "REQUIRED. Output file.")
	public String fout;

	/**
	 * Auxiliary input/output
	 */
	@Option(name = "-t", aliases = "-train", required = false,
		usage = "Input training data file(s). Separate with comma.")
	public String ftrain;

	@Option(name = "-outId",
		usage = "The output folder id, default is the process id.")
	public String outDirId;

	@Option(name = "-keepData",
		usage = "Keep the data in the database upon exiting.")
	public boolean keepData;

	@Option(name = "-verbose", usage = "Verbose level (0-3). Default=0")
	public int verboseLevel = 0;

	@Option(
		name = "-printVio",
		usage = "After finishing inferring, print out grounded clauses which are violated.")
	public boolean printViolation = false;
	
	@Option(name = "-countVioNoDB", usage = "Count number of violations without querying DB")
	public boolean countVioNoDB = false;

	@Option(name = "-loadgc", usage = "Load grounded rules for 'warm start'.")
	public String loadGCf = null;

	@Option(name = "-storegc", usage = "Store grounded rules to a given file.")
	public String storeGCf = null;
	
	@Option(name = "-loadrev", usage = "Load reverted grounded constraints for generalizing feedback.")
	public String loadRev = null;
	
	@Option(name = "-loadfeedback", usage = "Load grounded feedback constraints.")
	public String loadFeedback = null;
	
	@Option(
		name = "-ignoreWarmGCWeight",
		usage = "Ignore the weights associated with grounded rules loaded for 'warm start' Instead use the weight"
			+ "associated with the corresponding template rule.")
	public boolean ignoreWarmGCWeight = false;

	/**
	 * Misc
	 */
	@Option(name = "-conf", required=true,
		usage = "Path of the configuration file. Default='./nichrome.conf'")
	public String pathConf = null;

	@Option(name = "-help", usage = "Display command options.")
	public boolean showHelp = false;

	@Option(name = "-internal", usage = "Display all internal options.")
	public boolean showInternal = false;

	@Option(name = "-solver",
		usage = "Choose the solver to solve the MAXSAT problem: \n " + "1. "
			+ Config.EXACT_SOLVER + "(default): use mifumax solver to solve.\n"
			+ "2. " + Config.ILP_SOLVER
			+ ": use ilp solver to solve. It does not always give an "
			+ "exact answer, but should be very close.\n"
			+ "3. " + Config.LBX_SOLVER
			+ ": use lbx solver to solve. This is an approximate solver.\n"		
			+ "4. " + Config.MCSLS_SOLVER
			+ ": use mcsls solver to solve. This is an approximate solver.\n"
			+ "5. "
			+ Config.WALK_SOLVER
			+ ": use walksat solver to solve. This is an approximate solver.\n"
			+ "6."
			+ Config.TUFFY_SOLVER
			+ ": use tuffy to solve.\n"
			+ "7."+Config.TWO_STAGE_SOLVER
			+ ": use an two-stage approximate solver to solve.\n"
			+ "8."+Config.LBX_MCS_SOLVER
			+ ": use LBX as a minimal correction set solver to solve. This is an approximation solver.")
	public String solver = Config.EXACT_SOLVER;
	@Option(name = "-lbxTimeout",
		usage = "The timeout value for lbx solver.")
	public int lbx_timeout = 60 * 60;
	@Option(name = "-lbxLimit",
		usage = "The max num of minumal correction subsets to return by lbx.")
	public int lbx_numLimit = 10;
	@Option(name = "-mcslsTimeout",
		usage = "The timeout value for mcsls solver.")
	public int mcsls_timeout = 60 * 60;
	@Option(name = "-mcslsLimit",
		usage = "The max num of minumal correction subsets to return by mcsls.")
	public int mcsls_numLimit = 50;
	@Option(name = "-mcslsAlgo", usage = "The algorithm to use for mcsls.")
	public String mcsls_algo = "cld";

	@Option(name = "-saveMaxSATInstances", usage = "Use different file names for the MaxSAT instances solved")
	public boolean saveInstances = false;
	
	@Option(name = "-fullyGround", usage = "Set to ground EDB and feedback initially instead of Lazily")
	public boolean fullyGround = false;
	
	@Option(name = "-softRulesLimit", usage = "Limit the number of soft rules posted to the maxsat solver")
	public int softLimit = -1;

	@Option(name = "-ilpMemory",
		usage = "Set the max memory the ILP solver uses (in GB).")
	public double ilpMemory = 60;
	
	@Option(name = "-ilpSavePath",
			usage = "Set the path to save the ILP models. Default is null (do not save).")
	public String saveILPModelPath = null;	
	
	@Option(name = "-numSolutions",
		usage = "The number of solutions to be generated by the MaxSat solver during learning.")
	public int num_solutions = 1;
	
	@Option(name = "-numGIter",
		usage = "The maximum number of iterations of the lazy grounding algorithm.")
	public int num_grounding_iterations = -1;
	
	@Option(name = "-cpiCheck",
			usage = "Apply the cutting plane inference termination check.")
	public boolean cpiCheck = false;
	
	@Option(name = "-groundingTimeout",
		usage = "The timeout in seconds for the lazy grounding algorithm.")
	public double grounding_timeout = -1;

	@Option(name = "-useCAV", usage = "Use CAV'13 algorithm for inference.")
	public boolean useCAV = false;
	
	@Option(name = "-tuffyMLN", usage = "Path for printing the MLN that tuffy will use as input.")
	public String tuffyMLN = "tuffy_hard.mln";
	
	@Option(name = "-tuffyI", usage = "Path for MLNs used by tuffy.")
	public String tuffyI = "tuffy_hard.mln";
	
	@Option(name = "-tuffyRes", usage = "Path to file where tuffy writes its results.")
	public String tuffyRes = "tuffy.out";
	
	@Option(name = "-tuffyMemstats", usage = "Path for printing the mem stats for tuffy.")
	public String tuffyMemstats = "tuffy_mem.txt";
	
	@Option(name = "-tuffyConf", usage = "Path to tuffy conf file.")
	public String tuffyConf = "tuffy.out";
	
	@Option(name = "-tuffyEDB", usage = "Path of tuffy EDB.")
	public String tuffyEDB = "tuffy.edb";
	
	@Option(name = "-tuffyQuery", usage = "Path to file where tuffy writes its results.")
	public String tuffyQuery = "tuffy.out";
	
	@Option(name = "-tuffyOut", usage = "Path to file where tuffy writes its results.")
	public String tuffyOut = "tuffy.out";
	
	@Option(name = "-maxsatSolverPath", usage = "Path to maxsat solver binary.")
	public String maxsatSolverPath;
	
	/**
	 * Mode selection
	 */

	@Option(name = "-parallel",
		usage = "Whether to fire queries concurrently. Default = false.")
	public boolean enableConcurrency = false;

	/**
	 * Learning-related options
	 */
	@Option(name = "-learnwt",
		usage = "Run Tuffy in discriminative weight learning mode.")
	public boolean isLearningMode = false;

	@Option(name = "-lMaxIter",
		usage = "Max number of iterations for learning. DEFAULT=50")
	public int nLIteration = 50;

	
	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void printUsage(){
		OptionHandlerFilter optionFilter = showToOutsider;
		if(showInternal) {
			optionFilter = OptionHandlerFilter.ALL;
		}
		
		CmdLineParser parser= new CmdLineParser(this);

		OutputStreamWriter osw = new OutputStreamWriter(System.err);
		System.err.println("\n------\nUsage: "+ getName());
		parser.printUsage(osw,null, optionFilter);
	}
	
	@Override
	public String getName() {
	    return "SubCommand MLN";
	 }
}

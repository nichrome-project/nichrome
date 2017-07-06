package nichrome.mln.util;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;

import org.kohsuke.args4j.Option;

/**
 * Container for global configuration parameters.
 */
public class Config {
	
	public final static String GLOBAL_TIMER="total running time";

	public static String openWBOQueryPipe = null;
	public static String openWBOAnswerPipe = null;
	
	public static PrintWriter sampleLog = null;
	public static String samplerWriterPath = null;
	
	public static String outVisDir = null;

	public static String outDirId = null;

	public static String gcLoadFile = null;

	public static String gcStoreFile = null;
	
	public static String revLoadFile = null;
	
	public static String revFdbkFile = null;
	
	public static String storeReachFile = null;
	
	public static String loadReachFile = null;
	
	public static boolean ignoreWarmGCWeight = false;
	
	public static boolean saveMaxSATInstance = false;
	
	public static String checkSolutionPath = null;
	
	public final static int MODEL_PICKER = 0;
	public final static int RANDOM_PICKER = 1;
//	public final static int PLID_PICKER = 2;

	public final static String RANDOM_ALT_STRAT = "random_alt";
	public final static String RANDOM_ORACLE_STRAT = "random_oracle";
	public final static String RANDOM_ALL_STRAT = "random_all";
	public final static String MAXCROSS_ALT_STRAT = "maxcross_alt";
	public final static String MAXCROSS_ORACLE_STRAT = "maxcross_oracle";
	public final static String MAXCROSS_ALL_STRAT = "maxcross_all";
	public static String pickStrategy = Config.RANDOM_ALL_STRAT;
	public static int pickBudget = 4;
	public static int feedbackWeight = 100;
	public static double pessimisticRate = 0.5;
	public static boolean isParallel = false;
	public static int numFeedback = -1;
	
	public final static String EXACT_SOLVER = "exact";
	public final static String ILP_SOLVER = "ilp";
	public final static String MCSLS_SOLVER = "mcsls";
	public final static String LBX_SOLVER = "lbx";
	public final static String WALK_SOLVER = "walksat";
	public final static String TUFFY_SOLVER = "tuffy";
	public final static String TWO_STAGE_SOLVER = "twoStage";
	public final static String LBX_MCS_SOLVER = "lbxmcs";
	public final static String INCREMENTAL_SOLVER = "inc";
	
	
	public static boolean warmStart = false;
	
	public final static String OPTIMAL_CHECKER = "optimal";
	public final static String FEASIBLE_CHECKER = "feasible";
	public static boolean fowardBias = false;
	
	public static String LOG_PREFIX = "";
	public static String MEM_OUT_FOLDER = null;
	public static String MEM_TAG = null;
		
	public final static int REACH_PRECISE_JAVA = 1;
	public final static int REACH_PRECISE_DLOG = 2;
	public final static int REACH_APPROX_JAVA = 3;
	
	public static int REACH_NUM_THREADS = 8;
		
	public static int reachMeth = REACH_PRECISE_JAVA;

	public static String solver = Config.EXACT_SOLVER;
	public static String checker = Config.OPTIMAL_CHECKER;
	public static int mcsls_timeout = 60 * 60;
	public static int mcsls_numLimit = 50;
	public static String mcsls_algo = "cld";
	
	public static int lbx_timeout = 60 * 60;
	public static int lbx_numLimit = 10;
	
	public static int num_solver_solutions = 1;
	public static boolean blocking_mode = false;
	
	public static int num_grounding_iterations = -1;
	public static double grounding_timeout = -1;
	public static boolean cpiCheck = false;
	
	public static boolean useCAV = false;
	public static String tuffyMLN = "tuffy_hard.mln";
	public static String tuffyI = "tuffy.mln";
	public static String tuffyRes = "tuffy.out";
	public static String tuffyMemstats = "tuffy_mem.txt";
	public static String tuffyConf = "tuffy.out";
	public static String tuffyEDB = "tuffy.edb";
	public static String tuffyQuery = "tuffy.out";
	public static String tuffyOut = "tuffy.out";
	public static String tuffyDB = null;

	public static double ilpMemory = 100;
	public static int ilpSolLimit = 1;
	public static String saveILPModelPath = null;

	public static String product_line = "Nichrome";
	public static String product_name = "Nichrome 0.1";
	public static String path_conf = "./nichrome.conf";

	public static boolean snapshot_mode = false;
	public static boolean snapshoting_so_do_not_do_init_flip = false;
	public static int currentSampledNumber = 0;

	public static boolean no_pushdown = false;

	public static boolean using_greenplum = false;

	public static boolean skipUselessComponents = true;

	public static boolean mark_atoms_in_useful_components = false;

	public static boolean activate_soft_evid = true;

	public static boolean count_only_useful_inconsistencies = false;

	public static boolean warmTuffy = false;

	public static boolean log_trace = true;

	public static boolean log_vio_clauses = false;
	
	public static boolean count_vio_no_db = false;

	/**
	 * Runtime
	 */
	public static boolean exiting_mode = false;
	public static boolean learning_mode = true;

	public static String display_marker;

	/**
	 * DB
	 */
	public static String relConstants = "constants";

	public static String db_url = "jdbc:postgresql://localhost:5432/postgres";
	public static String db_username = "tuffer";

	public static String db_password = "tuffer";
	public static String db_schema = "a";

	public static String maxsat_path = "/lib/mifumax-mac";
	public static String lbx_path = "/lib/lbx";
	public static String mcsls_path = "/lib/mcsls";
	public static String walksat_path = "/lib/maxwalksat";
	public static String tuffy_path = "/lib/tuffy.jar";
	public static String mincut_path = "/lib/pseudo_fifo";
	public static boolean enable_concurrency = false;
	
	public static int maxSATTimeOut = -1;

	/**
	 * File System
	 */
	public static String dir_working = "/tmp/tuffy-workspace";
	public static String dir_out = ".";
	public static boolean output_files_in_gzip = false;
	public static String dir_tests = "/tmp/tuffy-tests";
	public static String file_stats = "tuffy_stats.txt";

	/**
	 * System
	 */
	public static boolean disable_partition = false;
	public static int max_threads = 0;
	public static int evidence_file_chunk_size = 1 << 22;
	public static double partition_size_bound = 1L << 32;
	public static double ram_size = 1L << 32;

	public static int max_number_components_per_bucket = Integer.MAX_VALUE;

	public static String evidDBSchema = null;
	public static boolean dbNeedTranslate = false;

	public static boolean reuseTables = false;

	public static boolean sortWhenParitioning = false;
	
	public static boolean fullyGround = false;

	public static ExecutorService executor;

	/**
	 * Inference
	 */

	public static double hard_weight = Long.MAX_VALUE;
	public static double hard_threshold = Long.MAX_VALUE;

	public static enum TUFFY_INFERENCE_TASK {
		MAP, MARGINAL, MLE
	};

	public static int gauss_seidel_infer_rounds = 5;

	public static boolean key_constraint_allows_null_label = false;

	/**
	 * The impossible value for sum of weights. Currently use -1.0 as we do not
	 * support negative weights for now.
	 */
	public static double impossible_obj = -1.0;

	/**
	 * UI
	 */
	public static int verbose_level = 0;
	public static String console_line_header = null;
	public static boolean clause_display_multiline = true;

	public static enum MCSAT_OUTPUT_TUPLE_ORDER {
		PROBABILITY, PRED_ARGS
	};

	public static MCSAT_OUTPUT_TUPLE_ORDER mcsat_output_order =
		MCSAT_OUTPUT_TUPLE_ORDER.PROBABILITY;

	public static double marginal_output_min_prob = 0;
	public static boolean mcsat_output_hidden_atoms = false;
	public static int mcsat_dump_interval = 0;

	public static boolean mcsat_cumulative = false;

	public static boolean enron_exp = true;
	public static boolean silent_on_single_thread = true;

	/**
	 * Helper
	 */
	public static int mcsatDumpPeriodSamples = 20;
	public static int mcsatDumpPeriodSeconds = 0;

	public static boolean output_prolog_format = false;
	public static boolean output_prior_with_marginals = true;
	public static boolean throw_exception_when_dying = false;

	public static boolean keep_db_data = false;

	public static boolean track_clause_provenance = false;
	public static boolean reorder_literals = false;

	public static double timeout = Double.MAX_VALUE;
	public static int num_tries_per_periodic_flush = 0;

	/**
	 * Research
	 */
	public static boolean checkNumCriticalNodes = false;
	public static boolean focus_on_critical_atoms = false;

	// public static int mleTopK = 100;
	public static int mleTopK = -1;
	// very expensive
	public static boolean calcRealMLECost = false;
	public static int innerPara = 1;
	public static int nMLESamples = 10000;

	public static boolean addReporter = true;
	public static boolean debug_mode = false;

	public static int mle_gibbs_mcmc_steps = 10;
	public static boolean mle_use_key_constraint = true;

	public static boolean mle_optimize_small_components = false;
	public static boolean mle_partition_components = false;

	public static boolean mle_use_gibbs_sampling = false;
	public static boolean mle_use_mcsat_sampling = false;
	public static boolean mle_use_serialmix_sampling = false;
	public static boolean mle_use_junction_tree = false;

	public static int mle_serialmix_constant = 100;

	public final static String CONSTANT_ID = "constantID";
	public final static String CONSTANT_VALUE = "constantVALUE";
	public static int MAX_UNIT_PROG = 1;

	public static int getNumThreads() {
		if (Config.max_threads > 0) {
			return Config.max_threads;
		}
		return Runtime.getRuntime().availableProcessors();
	}

	public static String getLoadingDir() {
		String path = Config.dir_working + "/loading";
		FileMan.ensureExistence(path);
		return path;
	}

	public static String getWorkingDir() {
		return Config.dir_working;
	}

	public static int globalCounter = 0;

	public static int getNextGlobalCounter() {
		synchronized (Config.class) {
			Config.globalCounter = Config.globalCounter + 1;
			return Config.globalCounter;
		}
	}

	public static String getProcessID() {
		return ManagementFactory.getRuntimeMXBean().getName();
	}

	public static double logAdd(double logX, double logY) {

		if (logY > logX) {
			double temp = logX;
			logX = logY;
			logY = temp;
		}

		if (logX == Double.NEGATIVE_INFINITY) {
			return logX;
		}

		double negDiff = logY - logX;
		if (negDiff < -200) {
			return logX;
		}

		return logX + java.lang.Math.log(1.0 + java.lang.Math.exp(negDiff));
	}

}

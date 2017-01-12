package nichrome.mln.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.concurrent.Executors;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionHandlerFilter;

import nichrome.mln.parser.CommandOptions;
import nichrome.mln.parser.ConfigLexer;
import nichrome.mln.parser.ConfigParser;

/**
 * Container of user-interface utilities.
 */
public class UIMan {

	protected static boolean silent = false;
	protected static boolean silentErr = false;

	public static boolean isSilent() {
		return UIMan.silent;
	}

	public static boolean isSilentErr() {
		return UIMan.silentErr;
	}

	public synchronized static void setSilentErr(boolean v) {
		UIMan.silentErr = v;
	}

	public synchronized static void setSilent(boolean v) {
		UIMan.silent = v;
	}

	public synchronized static void println(String... strings) {
		if (UIMan.silent) {
			return;
		}
		if (Config.console_line_header != null) {
			System.out.print("@" + Config.console_line_header + " ");
		}
		for (String s : strings) {
			System.out.print(s);
			UIMan.writeToDribbleFile(s);
		}
		System.out.println();
		UIMan.writeToDribbleFile("\n");
	}

	public synchronized static void print(String... strings) {
		if (UIMan.silent) {
			return;
		}
		for (String s : strings) {
			System.out.print(s);
			UIMan.writeToDribbleFile(s);
		}
	}

	public synchronized static void warn(String... strings) {
		if (UIMan.silentErr) {
			return;
		}
		System.err.print("WARNING: ");
		UIMan.writeToDribbleFile("WARNING: ");
		for (String s : strings) {
			System.err.print(s);
		}
		System.err.println();
		UIMan.writeToDribbleFile("\n");
	}

	public synchronized static void error(String... strings) {
		if (UIMan.silentErr) {
			return;
		}
		System.err.print("ERROR: ");
		UIMan.writeToDribbleFile("ERROR: ");
		for (String s : strings) {
			System.err.print(s);
		}
		System.err.println();
		UIMan.writeToDribbleFile("\n");
	}

	private static PrintStream dribbleStream = null;
	public static String dribbleFileName = null;

	public synchronized static void writeToDribbleFile(String str) {
		if (UIMan.dribbleStream != null) {
			UIMan.dribbleStream.print(str);
		}
	}

	public synchronized static void closeDribbleFile() {
		UIMan.dribbleFileName = null;
		if (UIMan.dribbleStream == null) {
			return;
		}
		UIMan.dribbleStream.close();
		UIMan.dribbleStream = null;
	}

	public synchronized static void createDribbleFile(String fileName) {
		UIMan.closeDribbleFile();
		try {
			FileOutputStream outStream = new FileOutputStream(fileName);
			UIMan.dribbleStream = new PrintStream(outStream, false); // No
																		// auto-flush
			// (can slow down
			// code).
			UIMan.dribbleFileName = fileName;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Unable to open file for logging:\n " + fileName
				+ ".\nError message: " + e.getMessage());
		}
	}

	public static String comma(int value) { // Always use separators (e.g.,
		// "100,000").
		return String.format("%,d", value);
	}

	public static String comma(long value) {
		return String.format("%,d", value);
	}

	public static String comma(double value) {
		return String.format("%,.3f", value);
	}

	public static String decimalRound(int digits, double num) {
		return String.format("%." + digits + "f", num);
	}

	public static CommandOptions processOptions(CommandOptions opt) {

		if (opt.pathConf != null) {
			Config.path_conf = opt.pathConf;
		}

		Config.LOG_PREFIX = opt.logPrefix;
		Config.MEM_OUT_FOLDER = opt.memOutDir;
		Config.warmStart = opt.warmStart;
		
		Config.outVisDir = opt.qVisDir;
		Config.outDirId = opt.outDirId;
		Config.gcLoadFile = opt.loadGCf;
		Config.gcStoreFile = opt.storeGCf;
		Config.revLoadFile = opt.loadRev;
		Config.revFdbkFile = opt.loadFeedback;
		Config.ignoreWarmGCWeight = opt.ignoreWarmGCWeight;
		Config.checkSolutionPath = opt.checkSolutionPath;
		
		Config.loadReachFile = opt.loadReach;
		Config.storeReachFile = opt.storeReach;
		
		Config.count_vio_no_db = opt.countVioNoDB;
		
		Config.log_vio_clauses = opt.printViolation;
		Config.solver = opt.solver;
		Config.checker = opt.checker;
		Config.fowardBias = opt.forwardBias;
		Config.MAX_UNIT_PROG = opt.upLevel;
		Config.saveMaxSATInstance = opt.saveInstances;
		Config.mcsls_numLimit = opt.mcsls_numLimit;
		Config.mcsls_timeout = opt.mcsls_timeout;
		Config.lbx_numLimit = opt.lbx_numLimit;
		Config.lbx_timeout = opt.lbx_timeout;
		Config.mcsls_algo = opt.mcsls_algo;
		Config.ilpMemory = opt.ilpMemory;
		Config.saveILPModelPath = opt.saveILPModelPath;
		Config.num_solver_solutions = opt.num_solutions;
		Config.reachMeth = opt.initReachability;
		if (Config.num_solver_solutions > 1 && !Config.solver.equalsIgnoreCase(Config.MCSLS_SOLVER)) {
			Config.blocking_mode = true;
		}
		
		Config.fullyGround = opt.fullyGround;
		
		Config.pickBudget = opt.pickBudget;
		Config.pickStrategy = opt.pickStrategy;
		Config.feedbackWeight = opt.feedbackWeight;
		Config.pessimisticRate = opt.pessimisticRate;
		Config.isParallel = opt.isParallel;
		Config.numFeedback = opt.numFeedback;
		
		Config.num_grounding_iterations = opt.num_grounding_iterations;
		Config.grounding_timeout = opt.grounding_timeout;
		Config.cpiCheck = opt.cpiCheck;
		Config.useCAV = opt.useCAV;
		Config.tuffyMLN = opt.tuffyMLN;
		Config.tuffyI = opt.tuffyI;
		Config.tuffyRes = opt.tuffyRes;
		Config.tuffyMemstats = opt.tuffyMemstats;
		Config.tuffyConf = opt.tuffyConf;
		Config.tuffyEDB = opt.tuffyEDB;
		Config.tuffyQuery = opt.tuffyQuery;
		Config.tuffyOut = opt.tuffyOut;

		Config.maxsat_path = opt.maxsatSolverPath;
		UIMan.parseConfigFile(Config.path_conf);

		Config.using_greenplum = opt.gp;
		Config.innerPara = opt.innerPara;

		Config.snapshot_mode = opt.snapshot;

		Config.output_prolog_format = opt.outputProlog;

		Config.max_threads = opt.maxThreads;
		// Config.use_atom_blocking = opt.block;
		
		Config.maxSATTimeOut = opt.maxSATTimeOut;

		Config.enable_concurrency = opt.enableConcurrency;

		Config.executor =
			Executors.newCachedThreadPool(new NamedThreadFactory(
				"MLN thread pool", true));

		Config.evidDBSchema = opt.evidDBSchema;
		Config.dbNeedTranslate = opt.dbNeedTranslate;
		
		Config.dir_out = FileMan.getParentDir(opt.fout);

		Config.disable_partition = opt.disablePartition;
		Config.output_files_in_gzip = opt.outputGz;
		if (Config.output_files_in_gzip
			&& !opt.fout.toLowerCase().endsWith(".gz")) {
			opt.fout += ".gz";
		}
		Config.mcsat_cumulative = opt.mcsatCumulative;
		Config.mcsatDumpPeriodSeconds = opt.mcsatDumpPeriodSec;
		Config.timeout = opt.timeout;
		Config.mcsat_dump_interval = opt.mcsatDumpInt;
		Config.marginal_output_min_prob = opt.minProb;
		/*
		 * if(opt.timeout > 0){ Config.timeout = opt.timeout; }
		 */
//		Config.dir_out = FileMan.getParentDir(opt.fout);
		Config.file_stats = opt.fout + ".stats";
		// Config.file_stats = Config.dir_out + "/tuffy_stats.txt";

		/*
		 * if(opt.reportingFreq > 0 && opt.marginal == false){
		 * Config.num_tries_per_periodic_flush = opt.reportingFreq; }
		 */
		if(opt.saveInstances)
			Config.keep_db_data = true;
		else
			Config.keep_db_data = opt.keepData;

		Config.console_line_header = opt.consoleLineHeader;

		Config.no_pushdown = opt.noPushDown;

		if (opt.fDribble != null) {
			UIMan.createDribbleFile(opt.fDribble);
		}

		// if(opt.fquery == null && opt.queryAtoms == null &&
		// opt.getClass().equals(CommandOptions.class)){
		// System.err.println("Please specify queries with -q or -queryFiles");
		// return null;
		// }

		Config.verbose_level = opt.verboseLevel;

		// /////SGD & MLE
		Config.mle_gibbs_mcmc_steps = opt.mle_gibbs_thinning;
		Config.mle_use_gibbs_sampling = opt.mle_use_gibbs;
		Config.mle_use_key_constraint = opt.mle_use_key_constraint;
		Config.debug_mode = opt.debug;
		Config.mle_partition_components = opt.mle_part_component;
		Config.mle_use_mcsat_sampling = opt.mle_use_mcsat;
		Config.mle_optimize_small_components = opt.mle_optimize_small_component;

		if (opt.mle_serialmix != -1) {
			Config.mle_use_serialmix_sampling = true;
			Config.mle_serialmix_constant = opt.mle_serialmix;
		}

		Config.mle_use_junction_tree = opt.mle_use_junction_tree;

		if (opt.sampleLog) {
			try {
				Config.sampleLog =
					new PrintWriter(new FileWriter(opt.fout + "_sampleLog.txt"));
				Config.samplerWriterPath = opt.fout + "_sampleLog.txt";
				System.out.println(opt.fout + "_sampleLog.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return opt;
	}

	public static CommandOptions parseCommand(String[] args) {
		CommandOptions opt = new CommandOptions();
		CmdLineParser parser = new CmdLineParser(opt);
		OptionHandlerFilter optionFilter = CommandOptions.showToOutsider;
		
		try {
			parser.parseArgument(args);
			if(opt.showInternal) {
				optionFilter = OptionHandlerFilter.ALL;
			}
			
			if (opt.showHelp) {
				UIMan.println("USAGE:");
				//parser.printUsage(System.out);
				OutputStreamWriter osw = new OutputStreamWriter(System.out);
				parser.printUsage(osw,null, optionFilter);
				
				return null;
			}
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			UIMan.println("USAGE:");
			//parser.printUsage(System.out);
			OutputStreamWriter osw = new OutputStreamWriter(System.out);
			parser.printUsage(osw,null, optionFilter);

			return null;
		}

		return UIMan.processOptions(opt);
	}

	public static boolean parseConfigFile(String fconf) {
		try {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(fconf);
			} catch (Exception e) {
				System.out.println("Failed to open config file.");
				System.err.println(e.getMessage());
				return false;
			}
			ANTLRInputStream input = new ANTLRInputStream(fis);
			ConfigLexer lexer = new ConfigLexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			ConfigParser parser = new ConfigParser(tokens);
			try {
				parser.config();
			} catch (Exception e) {
				System.out.println("Ill-formed config file: " + fconf);
				System.err.println(e.getMessage());
				return false;
			}
			Hashtable<String, String> map = parser.map;
			String value;

			value = map.get("db_url");
			if (value == null) {
				ExceptionMan.die("missing db_url in config file " + fconf);
			} else {
				Config.db_url = value.trim();
			}

			value = map.get("db_username");
			if (value == null) {
				// Config.db_username = "tuffer";
				ExceptionMan.die("missing db_username in config file " + fconf);
			} else {
				Config.db_username = value.trim();
			}

			value = map.get("db_password");
			if (value == null) {
				// Config.db_password = "tuffer";
				ExceptionMan.die("missing db_password in config file " + fconf);
			} else {
				Config.db_password = value.trim();
			}

			value = map.get("openWBOQueryPipe");
			if (value != null) {
				Config.openWBOQueryPipe = value.trim();
			}

			value = map.get("openWBOAnswerPipe");
			if (value != null) {
				Config.openWBOAnswerPipe = value.trim();
			}

			value = map.get("dir_working");
			if (value != null) {
				Config.dir_working = value.trim().replace('\\', '/');
			}

			value = map.get("maxsat");
			if (value != null) {
				Config.maxsat_path = value.trim().replace("\\", "/");
			}

			value = map.get("mcsls");
			if (value != null) {
				Config.mcsls_path = value.trim().replace("\\", "/");
			}

			value = map.get("lbx");
			if (value != null) {
				Config.lbx_path = value.trim().replace("\\", "/");
			}
			
			value = map.get("walksat");
			if (value != null) {
				Config.walksat_path = value.trim().replace("\\", "/");
			}
			
			value = map.get("tuffy");
			if (value != null) {
				Config.tuffy_path = value.trim().replace("\\", "/");
			}
			
			value = map.get("mincut");
			if (value != null) {
				Config.mincut_path = value.trim().replace("\\", "/");
			}

			String pid =
				ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
			String user =
				System.getProperty("user.name").toLowerCase().replaceAll("\\W",
					"_");
			String machine =
				java.net.InetAddress.getLocalHost().getHostName().toLowerCase()
					.replaceAll("\\W", "_");

			String prod = Config.product_line;
			if (Config.outDirId == null) {
				Config.outDirId = pid;
			}

			Config.dir_working +=
				"/" + prod + "_" + machine + "_" + user + "_" + Config.outDirId;

			if (Config.evidDBSchema == null) {// I don't see why the branch
				// statement is needed
				Config.db_schema =
					prod + "_" + machine + "_" + user + "_" + Config.outDirId;
			} else {
				Config.db_schema = Config.evidDBSchema;
			}

			String curDir = System.getProperty("user.dir");

			UIMan.println("Database schema     = " + Config.db_schema);
			UIMan.println("Current directory   = " + curDir);
			UIMan.println("Temporary directory = " + Config.dir_working);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}

	public synchronized static void verbose(int level, String s) {
		if (Config.verbose_level >= level) {
			UIMan.println(s);
		}
	}

	public synchronized static void verboseInline(int level, String s) {
		if (Config.verbose_level >= level) {
			UIMan.print(s);
		}
	}
	
	private static PrintStream originalStream;
	public synchronized static void suppressStdout(){
		originalStream = System.out;
		PrintStream dummyStream    = new PrintStream(new OutputStream(){
		    public void write(int b) {
		        //NO-OP
		    }
		});

		System.setOut(dummyStream);
	}
	
	public synchronized static void recoverStdout(){
		System.setOut(originalStream);
	}
}

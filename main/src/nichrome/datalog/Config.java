package nichrome.datalog;

import java.io.File;
import java.io.IOException;

import nichrome.datalog.utils.Utils;

/**
 * System properties recognized by Chord.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Config {
    private static final String BAD_OPTION = "ERROR: Unknown value '%s' for system property '%s'; expected: %s";

    private Config() { }

    // basic properties about program being analyzed (its main class, classpath, command line args, etc.)

    public final static String workDirName = System.getProperty("chord.work.dir");
    
    // properties dictating what gets computed/printed by Chord

    public final static String runAnalyses = System.getProperty("chord.run.analyses", "");
    public final static String printRels = System.getProperty("chord.print.rels", "");
    public final static boolean saveDomMaps = Utils.buildBoolProperty("chord.save.maps", true);

    // Determines verbosity level of Chord:
    // 0 => silent
    // 1 => print task/process enter/leave/time messages and sizes of computed doms/rels
    //      bddbddb: print sizes of relations output by solver
    // 2 => all other messages in Chord
    //      bddbddb: print bdd node resizing messages, gc messages, and solver stats (e.g. how long each iteration took)
    // 3 => bddbddb: noisy=yes for solver
    // 4 => bddbddb: tracesolve=yes for solver
    // 5 => bddbddb: fulltravesolve=yes for solver
    public final static int verbose = Integer.getInteger("chord.verbose", 1);

    // Chord project properties

    public final static String javaAnalysisPathName = System.getProperty("chord.java.analysis.path");
    public final static String dlogAnalysisPathName = System.getProperty("chord.dlog.analysis.path");

    // properties dictating what is reused across Chord runs

    public final static boolean reuseRels =Utils.buildBoolProperty("chord.reuse.rels", false);

    // properties concerning BDDs

    public final static boolean useBuddy =Utils.buildBoolProperty("chord.use.buddy", false);
    public final static String bddbddbMaxHeap = System.getProperty("chord.bddbddb.max.heap", "1024m");
    public final static String bddCodeFragmentFolder = System.getProperty("chord.bddbddb.codeFragment.out", "");

    // properties specifying names of Chord's output files and directories

    public static String outDirName = System.getProperty("chord.out.dir", workRel2Abs("chord_output"));

    public final static String bddbddbWorkDirName = System.getProperty("chord.bddbddb.work.dir", outRel2Abs("bddbddb"));

    static {
        Utils.mkdirs(outDirName);
        Utils.mkdirs(bddbddbWorkDirName);
    }

    // commonly-used constants

    public final static String mainDirName = System.getProperty("chord.main.dir");

    public static void print() {
        System.out.println("chord.main.dir: " + mainDirName);
        System.out.println("chord.work.dir: " + workDirName);
        System.out.println("chord.run.analyses: " + runAnalyses);
        System.out.println("chord.print.rels: " + printRels);
        System.out.println("chord.save.maps: " + saveDomMaps);
        System.out.println("chord.verbose: " + verbose);
        System.out.println("chord.java.analysis.path: " + javaAnalysisPathName);
        System.out.println("chord.dlog.analysis.path: " + dlogAnalysisPathName);
        System.out.println("chord.reuse.rels: " + reuseRels);
        System.out.println("chord.use.buddy: " + useBuddy);
        System.out.println("chord.bddbddb.max.heap: " + bddbddbMaxHeap);
    }

    public static String outRel2Abs(String fileName) {
        return (fileName == null) ? null : Utils.getAbsolutePath(outDirName, fileName);
    }
    
    public static String workRel2Abs(String fileName) {
        return (fileName == null) ? null : Utils.getAbsolutePath(workDirName, fileName);
    }


    public static void check(String val, String[] legalVals, String key) {
        for (String s : legalVals) {
            if (val.equals(s))
                return;
        }
        String legalValsStr = "[ ";
        for (String s : legalVals)
            legalValsStr += s + " ";
        legalValsStr += "]";
        Messages.fatal(BAD_OPTION, val, key, legalValsStr);
    }
}

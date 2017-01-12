package nichrome.datalog;

import java.io.File;
import java.io.PrintStream;

import nichrome.datalog.utils.Timer;
import nichrome.datalog.utils.Utils;

/**
 * Entry point of Chord after JVM settings are resolved.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DlogRunner {
    public static void run() {
        Timer timer = new Timer("chord");
        timer.init();
        String initTime = timer.getInitTimeStr();
        if (Config.verbose >= 0)
            System.out.println("Chord run initiated at: " + initTime);

        Project project = Project.g();
        ((ClassicProject)project).resetAll();

        String[] analysisNames = Utils.toArray(Config.runAnalyses);
        if (analysisNames.length > 0) {
            project.run(analysisNames);
        }
        String[] relNames = Utils.toArray(Config.printRels);
        if (relNames.length > 0) {
            project.printRels(relNames);
        }

        timer.done();
        String doneTime = timer.getDoneTimeStr();
        if (Config.verbose >= 0) {
            System.out.println("Chord run completed at: " + doneTime);
            System.out.println("Total time: " + timer.getInclusiveTimeStr());
        }
    }
}

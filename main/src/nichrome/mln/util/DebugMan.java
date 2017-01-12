package nichrome.mln.util;

import java.util.Scanner;

/**
 * Container of methods for debugging purposes.
 */
public class DebugMan {

	private static StringBuilder log = new StringBuilder();

	public static void log(String s) {
		DebugMan.log.append(s);
	}

	public static String getLog() {
		return DebugMan.log.toString();
	}

	public static void pause() {
		System.out.println("\nPress enter to continue...");
		Scanner in = new Scanner(System.in);
		in.nextLine();
	}

	public static boolean runningInWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		return os.contains("win");
	}

	private static final Runtime s_runtime = Runtime.getRuntime();

	public static void runGC() throws Exception {
		// It helps to call Runtime.gc()
		// using several method calls:
		for (int r = 0; r < 4; ++r) {
			DebugMan._runGC();
		}
	}

	private static void _runGC() throws Exception {
		long usedMem1 = DebugMan.usedMemoryp(), usedMem2 = Long.MAX_VALUE;
		for (int i = 0; (usedMem1 < usedMem2) && (i < 500); ++i) {
			DebugMan.s_runtime.runFinalization();
			DebugMan.s_runtime.gc();
			Thread.yield();

			usedMem2 = usedMem1;
			usedMem1 = DebugMan.usedMemoryp();
		}
	}

	private static long usedMemoryp() {
		return DebugMan.s_runtime.totalMemory()
			- DebugMan.s_runtime.freeMemory();
	}

	public static long usedMemory() {
		try {
			DebugMan.runGC();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		long mem0 =
			Runtime.getRuntime().totalMemory()
				- Runtime.getRuntime().freeMemory();
		return mem0;
	}

	static private long baseMem = 0;

	public static void checkBaseMem() {
		DebugMan.baseMem = DebugMan.usedMemory();
	}

	public static long getBaseMem() {
		return DebugMan.baseMem;
	}

	static private long peakMem = 0;

	public static void checkPeakMem() {
		long mem = DebugMan.usedMemory();
		if (mem > DebugMan.peakMem) {
			DebugMan.peakMem = mem;
		}
	}

	public static long getPeakMem() {
		return DebugMan.peakMem;
	}

}

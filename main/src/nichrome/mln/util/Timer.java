package nichrome.mln.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

/**
 * Container of time related utilities.
 */
public class Timer {

	private static long birth = Timer.now();
	private static Hashtable<String, Long> points =
		new Hashtable<String, Long>();

	private static long secondInMillis = 1000;
	private static long minuteInMillis = Timer.secondInMillis * 60;

	/**
	 * Resets the global starting point of the Timer.
	 */
	public static void resetClock() {
		Timer.birth = Timer.now();
	}

	public static String getDateTime() {
		DateFormat df = new SimpleDateFormat("H:mm:ss M/d/yy");
		return df.format(new Date());
	}

	/**
	 * Kicks off a timer with the given name.
	 */
	public static void start(String name) {
		Timer.points.put(name, Timer.now());
	}

	/**
	 * Returns the elapsed time of the timer with the given name. The format is
	 * [XX minutes, YYseconds].
	 */
	public static String elapsed(String name) {
		if (!Timer.points.containsKey(name)) {
			return "[UNKNOWN TIMER]";
		} else {
			long diff = Timer.now() - Timer.points.get(name);
			return Timer.readTime(diff);
		}
	}

	/**
	 * Returns the number of elapsed seconds of a given timer; -1 if the timer
	 * is unknown.
	 *
	 * @param name
	 *            the name of the timer
	 */
	public static double elapsedSeconds(String name) {
		if (!Timer.points.containsKey(name)) {
			return -1;
		} else {
			long diff = Timer.now() - Timer.points.get(name);
			return (diff / 1000);
		}
	}

	public static double elapsedMilliSeconds(String name) {
		if (!Timer.points.containsKey(name)) {
			return -1;
		} else {
			long diff = Timer.now() - Timer.points.get(name);
			return diff;
		}
	}

	/**
	 * Returns the number of elapsed seconds since last clock reset.
	 */
	public static double elapsedSeconds() {
		long diff = Timer.now() - Timer.birth;
		return (diff / 1000.0);
	}

	/**
	 * Prints the elapsed time since last clock reset. The format is TIMER: [XX
	 * minutes, YYseconds].
	 */
	public static void printElapsed() {
		UIMan.println("TIMER: " + Timer.elapsed());
	}

	/**
	 * Prints the elapsed time of the timer with the given name. The format is
	 * TIMER name: [XX minutes, YYseconds].
	 */
	public static void printElapsed(String name) {
		UIMan.println("TIMER " + name + ": " + Timer.elapsed(name));
	}

	private static long now() {
		return System.currentTimeMillis();
	}

	/**
	 * Returns a string of elapsed time.
	 */
	public static String elapsed() {
		return Timer.readTime(Timer.now() - Timer.birth);
	}

	private static String readTime(long timeIntervalInMS) {
		long min = timeIntervalInMS / Timer.minuteInMillis;
		long sec = timeIntervalInMS / Timer.secondInMillis % 60;
		long mm = timeIntervalInMS % 1000;
		String t = String.format("[%d min, %d.%03d sec]", min, sec, mm);
		return t;
	}

	public static RunStat runStat = new RunStat();

	public static class RunStat {
		public double groundSec = 0;
		public double inferSec = 0;
		public double inferOps = 0;
		public long effectiveSteps = 0;

		public ArrayList<Double> turns = new ArrayList<Double>();
		public ArrayList<Double> costs = new ArrayList<Double>();

		public void addTurn(double cost) {
			this.turns.add(Timer.elapsedSeconds());
			this.costs.add(cost);
		}

		public double getGroundTime() {
			return this.groundSec;
		}

		public double getFlipRate() {
			return this.inferOps / this.inferSec;
		}

		public void markGroundingDone() {
			this.groundSec = Timer.elapsedSeconds();
		}

		public void markInferDone() {
			this.inferSec = Timer.elapsedSeconds() - this.groundSec;
		}

		public void setInferOps(long ops) {
			this.inferOps = ops;
		}

		public void report() {
			System.out.println("============================");
			boolean report_trace = false;
			if (report_trace) {
				System.out.println("Trace:");
				for (int i = 0; i < this.turns.size(); i++) {
					System.out.println(this.turns.get(i) + "\t"
						+ this.costs.get(i));
				}
			}
			System.out.println("Total run-time = "
				+ UIMan.comma(this.groundSec + this.inferSec) + " sec");
			System.out.println("Ground time = " + UIMan.comma(this.groundSec)
				+ " sec");
			System.out.println("Inference time = " + UIMan.comma(this.inferSec)
				+ " sec");
			// System.out.println("Flip rate = " + getFlipRate());
			System.out.println("Effective #flips in MAP inference = "
				+ UIMan.comma(this.effectiveSteps));
			System.out.println("Base memory = "
				+ UIMan.comma(DebugMan.getBaseMem()) + " bytes");
			System.out.println("Peak memory = "
				+ UIMan.comma(DebugMan.getPeakMem()) + " bytes");
			System.out.println("============================");
		}
	}

}

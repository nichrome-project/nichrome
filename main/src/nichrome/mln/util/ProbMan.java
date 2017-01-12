package nichrome.mln.util;

import java.util.Random;

public class ProbMan {
	private static Random rand = new Random();

	public static boolean testChance(double prob) {
		return ProbMan.rand.nextDouble() < prob;
	}

	public static double nextDouble() {
		return ProbMan.rand.nextDouble();
	}

	public static boolean nextBoolean() {
		return ProbMan.rand.nextBoolean();
	}
}

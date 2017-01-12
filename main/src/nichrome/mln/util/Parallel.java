package nichrome.mln.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class Parallel {
	// private static final int NUM_CORES =
	// Runtime.getRuntime().availableProcessors();

	public static <T> void For(final Iterable<T> elements,
		final Operation<T> operation) {
		try {
			// invokeAll blocks for us until all submitted tasks in the call
			// complete
			Config.executor.invokeAll(Parallel.createCallables(elements,
				operation));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static <T> Collection<Callable<Void>> createCallables(
		final Iterable<T> elements, final Operation<T> operation) {
		List<Callable<Void>> callables = new LinkedList<Callable<Void>>();
		for (final T elem : elements) {
			callables.add(new Callable<Void>() {
				@Override
				public Void call() {
					operation.perform(elem);
					return null;
				}
			});
		}

		return callables;
	}

	public static interface Operation<T> {
		public void perform(T pParameter);
	}

}

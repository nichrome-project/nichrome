package nichrome.mln.util;

/**
 * A Enumerator is used to enumerate exponentially many configurations.
 *
 * @author czhang
 *
 */
public class Enumerator {

	int[] upperBounds;
	int[] currentState;

	/**
	 * Enumerate 2**n worlds.
	 *
	 * @param n
	 */
	public Enumerator(int n) {
		this.upperBounds = new int[n];
		this.currentState = new int[n];
		for (int i = 0; i < n; i++) {
			this.currentState[i] = 0;
			this.upperBounds[i] = 2;
		}
		this.currentState[0] = -1;
	}

	public void clear() {
		for (int i = 0; i < this.currentState.length; i++) {
			this.currentState[i] = 0;
		}
		this.currentState[0] = -1;
	}

	public int[] next() {

		this.currentState[0]++;
		for (int i = 0; i < this.currentState.length; i++) {

			if (this.currentState[i] == this.upperBounds[i]) {
				this.currentState[i] = 0;
				if (i + 1 == this.currentState.length) {
					return null;
				} else {
					this.currentState[i + 1]++;
				}
			} else {
				return this.currentState;
			}

		}
		return null;

	}

}

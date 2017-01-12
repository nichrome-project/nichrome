package nichrome.mln.util;

import java.util.BitSet;

public class BitSetDoublePair implements Comparable {

	public BitSet bitset;
	public Double doub;

	public BitSetDoublePair(BitSet _bitset, Double _double) {
		this.bitset = _bitset;
		this.doub = _double;
	}

	@Override
	public int compareTo(Object o) {
		if (this.doub - ((BitSetDoublePair) o).doub > 0) {
			return 1;
		} else {
			return -1;
		}
	}

	@Override
	public String toString() {
		return this.doub + ": " + this.bitset;
	}

}

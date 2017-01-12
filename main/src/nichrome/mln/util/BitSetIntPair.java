package nichrome.mln.util;

import java.util.BitSet;

public class BitSetIntPair implements Comparable {

	public BitSet bitset;
	public Integer integer;

	public BitSetIntPair(BitSet _bitset, Integer _integer) {
		this.bitset = _bitset;
		this.integer = _integer;
	}

	@Override
	public int compareTo(Object o) {
		return this.integer - ((BitSetIntPair) o).integer;
	}

	@Override
	public String toString() {
		return this.integer + ": " + this.bitset;
	}

}

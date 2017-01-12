package nichrome.mln;

import java.util.ArrayList;
import java.util.Arrays;

import nichrome.mln.util.StringMan;

/**
 * A tuple of constants/variables, represented as a transparent list of
 * integers.
 *
 */
public class Tuple {

	/**
	 * positive element = constant; negative element = variable. variables are
	 * encoded as -1, -2, ...
	 */
	public int[] list = null;

	// ASSUMPTION OF DATA STRUCTURE: the naming of variables
	// are assigned sequentially with increment 1 for each new variable
	// not seen before.

	/**
	 * The degree of freedom, i.e. the number of distinct variables. Actually,
	 * it corresponds to the smallest integer name of variables.
	 */
	public int dimension;

	/**
	 * Constructor of Tuple. Assuming args is already canonicalized
	 *
	 * @param args
	 */
	public Tuple(ArrayList<Integer> args) {
		this.list = new int[args.size()];
		this.dimension = 0;
		for (int i = 0; i < args.size(); i++) {
			this.list[i] = args.get(i);
			if (this.list[i] < this.dimension) {
				this.dimension = this.list[i];
			}
		}
		this.dimension = -this.dimension;
	}

	public Tuple(int[] args) {
		this.list = new int[args.length];
		this.dimension = 0;
		for (int i = 0; i < args.length; i++) {
			this.list[i] = args[i];
			if (this.list[i] < this.dimension) {
				this.dimension = this.list[i];
			}
		}
		this.dimension = -this.dimension;
	}

	/**
	 * Return the i-th element. This value is the variable/constant name of the
	 * i-th element.
	 */
	public int get(int i) {
		return this.list[i];
	}

	// /**
	// * Test if this tuple subsumes the argument.
	// *
	// * @return 1 if subsumes, 0 if equiv, -1 if neither
	// */

	/**
	 * Test if the tuple subsumes the argument tuple. Tuple $a$ subsumes tuple
	 * $b$, if there exists a mapping $\pi$ from variable to variable/constant,
	 * s.t., $\forall i$, $\pi$(a.variable[i]) = b.variable[i].
	 *
	 * @return true if subsumes, false otherwise;
	 */
	public boolean subsumes(Tuple other) {
		int[] l2 = other.list;
		assert (this.list.length == l2.length);
		int[] sub = new int[this.dimension + 1];
		for (int i = 0; i < this.list.length; i++) {
			if (this.list[i] > 0) { // a constant
				if (l2[i] != this.list[i]) {
					return false;
				}
			} else { // a variable
				int target = sub[-this.list[i]];
				if (target == 0) {
					sub[-this.list[i]] = l2[i];
				} else if (target != l2[i]) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.dimension;
		result = prime * result + Arrays.hashCode(this.list);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		Tuple other = (Tuple) obj;
		if (this.dimension != other.dimension) {
			return false;
		}
		if (!Arrays.equals(this.list, other.list)) {
			return false;
		}
		return true;
	}

	public String toCommaList() {
		ArrayList<String> parts = new ArrayList<String>();
		for (int i : this.list) {
			parts.add(i + "");
		}
		return StringMan.commaList(parts);
	}

}

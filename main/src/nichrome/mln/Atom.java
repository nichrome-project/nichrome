package nichrome.mln;

import java.util.ArrayList;

import nichrome.mln.util.StringMan;

/**
 * An atomic formula. It's designed as a light-weight construct, hence all
 * fields are transparent (public).
 */
public class Atom implements Cloneable {

	/**
	 * Enumerated type of Atoms. 1) NONE; 2) EVIDENCE: atom in evidence; 3)
	 * QUERY: atom in query; 4) QUEVID: atom in query as evidence.
	 */
	public static enum AtomType {
		EVIDENCE, QUERY, NONE, QUEVID
	};

	/**
	 * The predicate of this Atom.
	 */

	public Predicate pred;
	/**
	 * The argument list represented as a tuple of integers: constant as
	 * positive number and variable as negative number.
	 */
	public Tuple args = null;

	/**
	 * Truth value of this atom.
	 */
	public Boolean truth = null;

	/**
	 * Probability of "soft evidence".
	 */
	public Double prior = null;

	/**
	 * Type of this atom. Values are enumerated by {@link AtomType}.
	 */
	public AtomType type = AtomType.NONE;

	/**
	 * Map the {@link AtomType} value of this atom into an integer, which is
	 * used internally by the DB.
	 */
	public int club() {
		switch (this.type) {
			case NONE:
				return 0;
			case QUERY:
				return 1;
			case EVIDENCE:
				return 2;
			case QUEVID:
				return 3; // evidence in query
			default:
				return 0;
		}
	}

	/**
	 * Test if this atom is soft evidence.
	 */
	public boolean isSoftEvidence() {
		return this.prior != null;
	}

	/**
	 * Return the number of grounded atoms when grounding this atom. This number
	 * equals to the multiplication of the domain size of each distinct variable
	 * appearing in this atom. That is, we assume cross product is used.
	 */
	public long groundSize() {
		long size = 1;

		// ASSUMPTION OF DATA STRUCTURE: $\forall$ i<j, args.get(i) >
		// args.get(j) if
		// args.get(i), args.get(j) < 0. (i.e., naming new variables
		// sequentially from left to right.
		int lastVar = 0;
		for (int i = 0; i < this.pred.arity(); i++) {
			// if this is a new variable not seen before
			if (this.args.get(i) < lastVar) {
				Type t = this.pred.getTypeAt(i);
				size *= t.size();
				lastVar = this.args.get(i);
			}
		}
		return size;
	}

	public boolean isGrounded() {
		return this.groundSize() == 1 && this.prior == null
			&& this.truth == null;
	}

	private Atom() {
	}

	/**
	 * Create an evidence atom.
	 *
	 * @param p
	 *            the predicate
	 * @param as
	 *            the arguments
	 * @param t
	 *            the truth value
	 */
	public Atom(Predicate p, ArrayList<Integer> as, boolean t) {
		this.pred = p;
		this.args = new Tuple(as);
		this.prior = null;
		this.truth = t;
		this.type = AtomType.EVIDENCE;
	}

	public Atom(Predicate p, ArrayList<Integer> as, double prior) {
		this.pred = p;
		this.args = new Tuple(as);
		this.truth = null;
		// if(prior < 0)
		// throw new
		// RuntimeException("Evidences with negative priors are not supported!");
		this.prior = prior;
		this.type = AtomType.NONE;
	}

	/**
	 * Create an atom of type NONE. The default truth value of unknown is null.
	 *
	 * @param p
	 *            the predicate
	 * @param at
	 *            the arguments in the form of a tuple
	 *
	 * @see tuffy.ground.KBMC#run()
	 */
	public Atom(Predicate p, Tuple at) {
		assert (at.list.length == p.arity());
		this.pred = p;
		this.args = at;
		this.truth = null;
		this.type = AtomType.NONE;
	}

	/**
	 * Returns this atom's human-friendly string representation.
	 */
	@Override
	public String toString() {
		ArrayList<String> as = new ArrayList<String>();
		for (int v : this.args.list) {
			if (v >= 0) {
				as.add("C" + v);
			} else {
				as.add("v" + (-v));
			}
		}
		return this.pred.getName() + StringMan.commaListParen(as);
	}

	public String toGroundString(MarkovLogicNetwork mln) {
		ArrayList<String> as = new ArrayList<String>();
		for (int v : this.args.list) {
			as.add(mln.getSymbol(v));
		}
		return this.pred.getName() + "(" + StringMan.commaListWithoutSpace(as)
			+ ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result =
			prime * result + ((this.args == null) ? 0 : this.args.hashCode());
		result =
			prime * result + ((this.pred == null) ? 0 : this.pred.hashCode());
		result =
			prime * result + ((this.prior == null) ? 0 : this.prior.hashCode());
		result =
			prime * result + ((this.truth == null) ? 0 : this.truth.hashCode());
		result =
			prime * result + ((this.type == null) ? 0 : this.type.hashCode());
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
		Atom other = (Atom) obj;
		if (this.args == null) {
			if (other.args != null) {
				return false;
			}
		} else if (!this.args.equals(other.args)) {
			return false;
		}
		if (this.pred == null) {
			if (other.pred != null) {
				return false;
			}
		} else if (!this.pred.equals(other.pred)) {
			return false;
		}
		if (this.prior == null) {
			if (other.prior != null) {
				return false;
			}
		} else if (!this.prior.equals(other.prior)) {
			return false;
		}
		if (this.truth == null) {
			if (other.truth != null) {
				return false;
			}
		} else if (!this.truth.equals(other.truth)) {
			return false;
		}
		if (this.type != other.type) {
			return false;
		}
		return true;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		Atom ret = new Atom();
		ret.args = this.args;
		ret.pred = this.pred;
		ret.prior = this.prior;
		ret.truth = this.truth;
		ret.type = this.type;
		return ret;
	}

	public Atom base() {
		Atom ret;
		try {
			ret = (Atom) this.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		ret.prior = null;
		ret.truth = null;
		ret.type = AtomType.NONE;
		return ret;
	}
}

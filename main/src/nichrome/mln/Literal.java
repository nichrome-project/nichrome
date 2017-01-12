package nichrome.mln;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

import nichrome.mln.util.StringMan;

/**
 * A literal in first-order logic.
 */
public class Literal implements Cloneable {

	/**
	 * Predicate object associated with this literal.
	 */
	private Predicate pred;

	/**
	 * The index of this literal in its parent clause.
	 */
	private int idx = -1;

	/**
	 * List of terms (variable/constant) contained in this literal.
	 */
	private ArrayList<Term> terms = new ArrayList<Term>();

	/**
	 * The positive/negative value of this literal. Here the positive/negative
	 * refers to that in Horn clause.
	 */
	private boolean sense;

	private boolean coversAllMaterializedTuples = false;

	/**
	 * The tuple format of this literal. Need call {@link Literal#toTuple()} to
	 * make this variable not null. This variable is not automatically
	 * maintained. To obtain the most update to date version, you need to call
	 * {@link Literal#toTuple()}.
	 */
	private Tuple tuple = null;

	/**
	 * The name set of all variables in this literal.
	 */
	private HashSet<String> vars = new HashSet<String>();

	@Override
	@SuppressWarnings("unchecked")
	public Object clone() throws CloneNotSupportedException {

		Literal clone = (Literal) super.clone();

		// make the shallow copy of the object of type Department
		clone.coversAllMaterializedTuples = this.coversAllMaterializedTuples;
		clone.idx = -1;
		clone.pred = this.pred;
		clone.sense = this.sense;
		clone.terms = (ArrayList<Term>) this.terms.clone();
		clone.tuple = this.tuple;
		clone.vars = (HashSet<String>) this.vars.clone();
		return clone;

	}

	/**
	 * Return whether the predicate of this literal is a built-in predicate.
	 */
	public boolean isBuiltIn() {
		return this.pred.isBuiltIn();
	}

	/**
	 * Constructor of Literal.
	 *
	 * @param predicate
	 *            the predicate
	 * @param sense
	 *            true for a positive literal; false for a negative one
	 */
	public Literal(Predicate predicate, boolean sense) {
		this.sense = sense;
		this.pred = predicate;
	}

	/**
	 * Return the set of variable names in this literal.
	 */
	public HashSet<String> getVars() {
		return this.vars;
	}

	/**
	 * Return the predicate of this literal.
	 */
	public Predicate getPred() {
		return this.pred;
	}

	/**
	 * Return the list of terms in this literal.
	 */
	public ArrayList<Term> getTerms() {
		return this.terms;
	}

	/**
	 * Return the assigned index of this literal in its parent clause.
	 */
	public int getIdx() {
		return this.idx;
	}

	/**
	 * Assign an unique (within its parent clause) index to this literal.
	 *
	 * @param i
	 *            the index
	 * @see Clause#addLiteral(Literal)
	 */
	public void setIdx(int i) {
		this.idx = i;
	}

	// /**
	// * Clique of variables.
	// * Used for the purpose of computing MGU.
	// *
	// * @see Literal#mostGeneralUnification(Tuple)
	// */

	/**
	 * Clique of variables. Here by clique, it means a set of variables, plus a
	 * constant.
	 *
	 * @see Literal#mostGeneralUnification(Tuple)
	 */
	private class VarClique {

		/**
		 * The set of variable names in this clique.
		 */
		HashSet<String> vars = new HashSet<String>();

		/**
		 * The constant of this clique.
		 */
		Integer constant = null;

		/**
		 * Add variable to this clique.
		 *
		 * @param v
		 *            the name of added variable.
		 */
		public void addVar(String v) {
			this.vars.add(v);
		}

		/**
		 * Set the constant of this clique. This is allowed when 1) current
		 * constant is null; 2) current constant is equals to the changed
		 * constant.
		 *
		 * @param con
		 *            the added constant.
		 * @return whether the intending setting is allowed and succeeded.
		 */
		public boolean setConstant(int con) {
			if (this.constant != null && this.constant != con) {
				return false;
			}
			this.constant = con;
			return true;
		}

		/**
		 * Merge a new clique with the current one. Here by merge, it requires
		 * the constant of these two cliques should be either null or the same.
		 * This process involves 1) merging the variable set and 2) check the
		 * consistency of constant.
		 *
		 * @param c
		 *            the new clique
		 * @return whether the intending swallow is allowed and succeeded.
		 */
		public boolean swallow(VarClique c) {
			if (this.constant != null && c.constant != null
				&& this.constant != c.constant) {
				return false;
			}
			if (this.constant == null) {
				this.constant = c.constant;
			}
			this.vars.addAll(c.vars);
			return true;
		}
	}

	/**
	 * Compute the most general unification (MGU) of two literals.
	 *
	 * @param atuple
	 *            the literal (in the form of a tuple) to be unified
	 * @return the MGU in the form of a mapping from variables to
	 *         variables/constants
	 */
	public HashMap<String, Term> mostGeneralUnification(Tuple atuple) {
		Hashtable<String, VarClique> cliques =
			new Hashtable<String, VarClique>();
		int[] tuple = atuple.list;
		for (int i = 0; i < this.terms.size(); i++) {
			Term t = this.terms.get(i);
			if (t.isConstant()) {
				int termcon = 0;
				termcon = t.constant();
				// const-const map
				if (tuple[i] > 0 && termcon != tuple[i]) {
					return null;
				}
				// const-var map
				if (tuple[i] < 0) {
					String var = Integer.toString(tuple[i]);
					VarClique clique = cliques.get(var);
					if (clique == null) {
						clique = new VarClique();
						clique.addVar(var);
						cliques.put(var, clique);
					}
					if (!clique.setConstant(termcon)) {
						return null;
					}
				}
			} else {
				VarClique clique1 = cliques.get(t.var());
				if (clique1 == null) {
					clique1 = new VarClique();
					clique1.addVar(t.var());
					cliques.put(t.var(), clique1);
				}
				// var-const map
				if (tuple[i] > 0) {
					if (!clique1.setConstant(tuple[i])) {
						return null;
					}
				} else { // var-var map
					String var = Integer.toString(tuple[i]);
					VarClique clique2 = cliques.get(var);
					if (clique2 == null) {
						clique2 = new VarClique();
						clique2.addVar(var);
						cliques.put(var, clique2);
					}
					if (clique1.swallow(clique2)) {
						for (String v : clique2.vars) {
							cliques.put(v, clique1);
						}
					} else {
						return null;
					}
				}
			}
		}

		HashMap<String, Term> lmap = new HashMap<String, Term>();
		for (String v : this.vars) {
			VarClique clique = cliques.get(v);
			Term t;
			if (clique.constant == null) {
				t = new Term(clique.toString());
			} else {
				t = new Term(clique.constant);
			}
			lmap.put(v, t);
		}
		return lmap;
	}

	/**
	 * Return the human-friendly representation of this literal.
	 */
	@Override
	public String toString() {
		String s = (this.sense ? " " : "!");
		s += this.pred.getName();
		ArrayList<String> a = new ArrayList<String>();
		for (Term t : this.terms) {
			if (t.isVariable()) {
				a.add(t.var());
			} else {
				a.add(t.constantString());
			}
		}
		s += StringMan.commaListParen(a);
		return s;
	}

	/**
	 * Append a new term to this literal.
	 *
	 * @param t
	 *            the term to be appended
	 */
	public void appendTerm(Term t) {
		this.terms.add(t);
		if (t.isVariable()) {
			this.vars.add(t.var());
		}
	}

	/**
	 * Convert this literal into a tuple. This will assign an internal ID for
	 * variables obeying the syntax of class Tuple from Strings.
	 */
	public Tuple toTuple() {
		if (this.tuple != null) {
			return this.tuple;
		}
		ArrayList<Integer> tlist = new ArrayList<Integer>();
		Hashtable<String, Integer> varIDMap = new Hashtable<String, Integer>();
		for (Term t : this.terms) {
			if (t.isConstant()) {
				tlist.add(t.constant());
			} else {
				String v = t.var();
				Integer id = varIDMap.get(v);
				if (id == null) {
					id = -(varIDMap.size() + 1);
					varIDMap.put(v, id);
				}
				tlist.add(id);
			}
		}
		this.tuple = new Tuple(tlist);
		return this.tuple;
	}

	/**
	 * Compare a given literal with this one. By ``same'', it means 1) predicate
	 * is same; 2) sense is same; 3) corresponding constant is same; and 4) the
	 * name of corresponding variable is same. (Do not consider substitution).
	 *
	 * @param lit
	 *            the literal needed to be compared.
	 * @return true if these two literals are the same, false otherwise.
	 */
	public boolean isSameAs(Literal lit) {
		if (this.pred != lit.pred || this.sense != lit.sense) {
			return false;
		}
		for (int i = 0; i < this.terms.size(); i++) {
			Term t1 = this.terms.get(i);
			Term t2 = lit.terms.get(i);
			if (t1.isConstant() != t2.isConstant()) {
				return false;
			}
			if (t1.isConstant()
				&& !t1.constantString().equals(t2.constantString())) {
				return false;
			}
			if (t1.isVariable() && !t1.var().equals(t2.var())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Apply a substitution to this literal.
	 *
	 * @param vmap
	 *            the substitution
	 * @return the new literal
	 */
	public Literal substitute(HashMap<String, Term> vmap) {
		Literal copy = new Literal(this.pred, this.sense);
		copy.coversAllMaterializedTuples = this.coversAllMaterializedTuples;
		for (Term t : this.terms) {
			if (t.isConstant()) {
				copy.appendTerm(t);
			} else {
				Term t2 = vmap.get(t.var());
				if (t2 == null) {
					copy.appendTerm(t);
				} else {
					copy.appendTerm(t2);
				}
			}
		}
		return copy;
	}

	/**
	 * Convert this literal to an atom.
	 *
	 * @param type
	 *            indicates if it's an evidence, a query, etc.
	 */
	public Atom toAtom(Atom.AtomType type) {
		Atom a = new Atom(this.pred, this.toTuple());
		a.type = type;
		return a;
	}

	/**
	 * Flip the sense of this literal.
	 */
	public void flipSense() {
		this.sense = !this.sense;
	}

	/**
	 * Return true if this is a positive literal. Here the positive/negative
	 * refers to that in Horn clause.
	 */
	public boolean getSense() {
		return this.sense;
	}

	/**
	 * Set the sense of this literal.
	 *
	 * @param asense
	 *            true if this is intended to be a positive literal
	 */
	public void setSense(boolean asense) {
		this.sense = asense;
	}

	/**
	 * Set whether we want this literal to cover all materialized tuples
	 * regardless of the sense of this literal.
	 *
	 * @param coversAllMaterializedTuples
	 */
	public void setCoversAllMaterializedTuples(
		boolean coversAllMaterializedTuples) {
		this.coversAllMaterializedTuples = coversAllMaterializedTuples;
	}

	/**
	 * Test whether we want this literal to cover all materialized tuples
	 * regardless of the sense of this literal.
	 */
	public boolean coversAllMaterializedTuples() {
		return this.coversAllMaterializedTuples;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result =
			prime * result + (this.coversAllMaterializedTuples ? 1231 : 1237);
		result = prime * result + this.idx;
		result =
			prime * result + ((this.pred == null) ? 0 : this.pred.hashCode());
		result = prime * result + (this.sense ? 1231 : 1237);
		result =
			prime * result + ((this.terms == null) ? 0 : this.terms.hashCode());
		result =
			prime * result + ((this.tuple == null) ? 0 : this.tuple.hashCode());
		result =
			prime * result + ((this.vars == null) ? 0 : this.vars.hashCode());
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
		Literal other = (Literal) obj;
		if (this.coversAllMaterializedTuples != other.coversAllMaterializedTuples) {
			return false;
		}
		if (this.idx != other.idx) {
			return false;
		}
		if (this.pred == null) {
			if (other.pred != null) {
				return false;
			}
		} else if (!this.pred.equals(other.pred)) {
			return false;
		}
		if (this.sense != other.sense) {
			return false;
		}
		if (this.terms == null) {
			if (other.terms != null) {
				return false;
			}
		} else if (!this.terms.equals(other.terms)) {
			return false;
		}
		if (this.tuple == null) {
			if (other.tuple != null) {
				return false;
			}
		} else if (!this.tuple.equals(other.tuple)) {
			return false;
		}
		if (this.vars == null) {
			if (other.vars != null) {
				return false;
			}
		} else if (!this.vars.equals(other.vars)) {
			return false;
		}
		return true;
	}

	/**
	 * Check if current literal contains any variable
	 *
	 * @return
	 */
	public boolean isConstant() {
		for (Term t : this.terms) {
			if (t.isVariable()) {
				return false;
			}
		}
		return true;
	}

}

package nichrome.mln;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nichrome.mln.Clause.ClauseInstance;
import nichrome.mln.util.Config;
import nichrome.mln.util.ExceptionMan;
import nichrome.mln.util.StringMan;

/**
 * A ground clause.
 *
 */
public class GClause implements Cloneable {

	/**
	 * Weight of this GClause.
	 */
	public double weight = 0;

	/**
	 * List of literals in this grounded clause. This is from $lits$ attribute
	 * in {@link Config#relClauses} table.
	 */
	public int[] lits = null;

	public Clause c;
	public ClauseInstance ci;

	public GClause(Clause c, ClauseInstance ci) {
		this.c = c;
		this.ci = ci;
		if (ci == null) {
			this.weight = c.getWeight();
		} else {
			this.weight = ci.weight;
		}
		this.lits = new int[c.lits.size()];
	}

	public GClause(double weight, int... lits) {
		this.lits = lits;
		this.weight = weight;
	}

	/**
	 * Return whether this clause is a positive clause. Here by positive it
	 * means this clause has a positive weight.
	 */
	public boolean isPositiveClause() {
		return this.weight >= 0;
	}

	/**
	 * Return whether this clause is a hard clause. Here by hard it means this
	 * clause has a weight larger than {@link Config#hard_weight}, which means
	 * this clause must be satisfied in reasoning result.
	 */
	public boolean isHardClause() {
		return Math.abs(this.weight) >= Config.hard_weight;
	}

	/**
	 * Return the cost for violating this GClause. For positive clause, if it is
	 * violated, cost equals to weight. For negative clause, if it is satisfied,
	 * cost equals to -weight. Otherwise, return 0.
	 */
	public double cost() {
		if (this.weight > 0) {
			return this.weight;
		}
		if (this.weight < 0) {
			return -this.weight;
		}
		return 0;
	}

	/**
	 * Returns +/-1 if this GClause contains this atom; 0 if not. If -1, then
	 * the atom in this clause is with negative sense.
	 *
	 * @param atom
	 */
	public int linkType(int atom) {
		for (int l : this.lits) {
			if (l == atom) {
				return 1;
			}
			if (l == -atom) {
				return -1;
			}
		}
		return 0;
	}
	
	/**
	 * Check whether current clause is satisfied by a given solution.
	 * All atoms contained in solutions are set to true; otherwise, false.
	 * @param solution
	 * @return
	 */
	public boolean isSatisfiedBy(Set<Integer> solution){
		for(int l : lits){
			if(l > 0 && solution.contains(l))
				return true;
			if(l < 0 && !solution.contains(-l))
				return true;
		}
		return false;
	}

	/**
	 * Replaces the ID of a particular atom, assuming that no twins exist.
	 *
	 * @param oldID
	 * @param newID
	 * @return 1 if oldID=>newID, -1 if -oldID=>-newID, 0 if no replacement
	 */
	public int replaceAtomID(int oldID, int newID) {
		for (int k = 0; k < this.lits.length; k++) {
			if (this.lits[k] == oldID) {
				this.lits[k] = newID;
				return 1;
			} else if (this.lits[k] == -oldID) {
				this.lits[k] = -newID;
				return -1;
			}
		}
		return 0;
	}

	/**
	 * Initialize GClause from results of SQL. This involves set $cid$ to
	 * {@link GClause#id}, $weight$ {@link GClause#weight}, $lits$ to
	 * {@link GClause#lits}, $fcid$ to {@link GClause#fcid}.
	 *
	 * @param rs
	 *            the ResultSet for SQL. This sql is a sequential scan on table
	 *            {@link Config#relClauses}.
	 *
	 */
	public void parse(ResultSet rs, MarkovLogicNetwork mln) {
		try {
			Map<String, Integer> varMap = new HashMap<String, Integer>();
			for (String var : this.c.vars) {
				varMap.put(var, rs.getInt(var));
			}
			for (int i = 0; i < this.c.metaVars.size(); i++) {
				varMap.put(this.c.metaVars.get(i), this.ci.conList.get(i)
					.constant());
			}
			for (int j = 0; j < this.c.lits.size(); j++) {
				Literal l = this.c.lits.get(j);
				List<Term> terms = l.getTerms();
				ArrayList<Integer> values = new ArrayList<Integer>();
				for (int i = 0; i < terms.size(); i++) {
					values.add(varMap.get(terms.get(i).var()).intValue());
				}
				Atom atom = new Atom(l.getPred(), values, true);
				int atId = mln.getAtomID(atom.base());
				if (l.getSense()) {
					this.lits[j] = atId;
				} else {
					this.lits[j] = -atId;
				}
			}
		} catch (Exception e) {
			ExceptionMan.handle(e);
		}
	}
	
	/**
	 * Check if the clause is violated for the given solution. Violation check
	 * ignores the weight i.e. if weight is negative and isViolated returns
	 * true, then the clause is actually not violated.
	 */
	public boolean isViolated(Set<Integer> solution) {
		boolean isViolated = true;
		for (int lit : lits) {
			if (lit > 0 && solution.contains(lit)) {
				isViolated = false;
				break;
			} else if (lit < 0 && !solution.contains(-lit)) {
				isViolated = false;
				break;
			}
		}
		return isViolated;
	}

	/**
	 * Returns the string form of this GClause, which is,
	 *
	 * { <lit_1>, <lit_2>, ..., <lit_n> } | weight
	 *
	 * where lit_i is the literal ID in {@link GClause#lits}.
	 */
	public String toPGString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (int i = 0; i < this.lits.length; i++) {
			sb.append(this.lits[i]);
			if (i < this.lits.length - 1) {
				sb.append(",");
			}
		}
		sb.append("} | " + this.weight);
		return sb.toString();
	}

	/**
	 * Returns its human-friendly representation.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.weight + ": [");
		for (int l : this.lits) {
			sb.append(l + ",");
		}
		sb.append("]");
		return sb.toString();
	}

	/**
   *
   */
	public String toVerboseString(MarkovLogicNetwork mln) {
		StringBuilder sb = new StringBuilder();
		if (this.isHardClause()) {
			sb.append("infi: ");
		} else {
			sb.append(this.weight + ": ");
		}
		ArrayList<String> parts = new ArrayList<String>();
		for (int l : this.lits) {
			String ls = "";
			if (l < 0) {
				ls = "NOT ";
			}
			ls += mln.getAtom(Math.abs(l)).toGroundString(mln);
			parts.add(ls);
		}
		sb.append(StringMan.commaList(parts));
		// sb.append("]");
		return sb.toString();
	}
	
	// For CAV'13
	public String toConstraintString(MarkovLogicNetwork mln) {
		StringBuilder sb = new StringBuilder();
		if (!this.isHardClause()) {
			sb.append(this.weight + "  ");
		}
		ArrayList<String> parts = new ArrayList<String>();
		for (int l : this.lits) {
			String ls = "";
			if (l < 0) {
				ls = "!";
			}
			ls += mln.getAtom(Math.abs(l)).toGroundString(mln);
			parts.add(ls);
		}
		sb.append(StringMan.orList(parts));
		if(this.isHardClause()) {
			sb.append(".");
		}
		// sb.append("]");
		return sb.toString();
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		GClause ret = new GClause(this.c, this.ci);
		ret.lits = new int[this.lits.length];
		System.arraycopy(this.lits, 0, ret.lits, 0, this.lits.length);
		ret.weight = this.weight;
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.c == null) ? 0 : this.c.hashCode());
		result = prime * result + ((this.ci == null) ? 0 : this.ci.hashCode());
		result = prime * result + Arrays.hashCode(this.lits);
		/*
		 * Cannot use weight to hash since the weight of GClause might be
		 * modified during learning. Consequently, set membership queries for
		 * set of GClauses might be incorrectly answered.
		 */
		// long temp;
		// temp = Double.doubleToLongBits(this.weight);
		// result = prime * result + (int) (temp ^ (temp >>> 32));
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
		GClause other = (GClause) obj;
		if (this.c == null) {
			if (other.c != null) {
				return false;
			}
		} else if (!this.c.equals(other.c)) {
			return false;
		}
		if (this.ci == null) {
			if (other.ci != null) {
				return false;
			}
		} else if (!this.ci.equals(other.ci)) {
			return false;
		}
		if (!Arrays.equals(this.lits, other.lits)) {
			return false;
		}
		if (Double.doubleToLongBits(this.weight) != Double
			.doubleToLongBits(other.weight)) {
			return false;
		}
		return true;
	}

}

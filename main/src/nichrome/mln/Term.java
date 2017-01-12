package nichrome.mln;

import nichrome.mln.util.StringMan;

/**
 * A term in first-order logic; either a variable or a constant.
 */
public class Term {

	/**
	 * Whether this term is a variable.
	 */
	private boolean isVariable;

	/**
	 * The name of this term. $var$ is not null iff. this term is a variable.
	 */
	private String var = null;

	/**
	 * The ID of this term. $constantID$ is not null iff. this term is a
	 * constant.
	 */
	private Integer constantID = null;
	private String constantString = null;

	/**
	 * Constructor of Term (variable version).
	 *
	 * @param var
	 *            name of the variable
	 */
	public Term(String var) {
		this.isVariable = true;
		this.var = var;
	}

	public Term(String s, boolean isConstant) {
		if (isConstant) {
			this.constantString = s;
			this.isVariable = false;
		} else {
			this.var = s;
			this.isVariable = true;
		}
	}

	/**
	 * Constructor a Term (constant version).
	 *
	 * @param cid
	 *            the constant in the form of its integer ID
	 */
	public Term(Integer cid) {
		this.isVariable = false;
		this.constantID = cid;
		this.constantString = cid.toString();
	}

	/**
	 * Return whether this term is a variable.
	 */
	public boolean isVariable() {
		return this.isVariable;
	}

	/**
	 *
	 * @return Whether this term is a constant.
	 */
	public boolean isConstant() {
		return !this.isVariable;
	}

	/**
	 * @return The name of this term, which is null when it is a constant.
	 */
	public String var() {
		return this.var;
	}

	/**
	 *
	 * @return The name of this term, which is null when it is a variable.
	 */
	public int constant() {
		return this.constantID;
	}

	public String constantString() {
		return this.constantString;
	}

	/**
	 * @return This term's human-friendly representation.
	 */
	@Override
	public String toString() {
		if (this.isVariable) {
			return this.var;
		} else {
			return StringMan.quoteJavaString(this.constantString);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result =
			prime * result
				+ ((this.constantID == null) ? 0 : this.constantID.hashCode());
		result =
			prime
				* result
				+ ((this.constantString == null) ? 0 : this.constantString
					.hashCode());
		result = prime * result + (this.isVariable ? 1231 : 1237);
		result =
			prime * result + ((this.var == null) ? 0 : this.var.hashCode());
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
		Term other = (Term) obj;
		if (this.constantID == null) {
			if (other.constantID != null) {
				return false;
			}
		} else if (!this.constantID.equals(other.constantID)) {
			return false;
		}
		if (this.constantString == null) {
			if (other.constantString != null) {
				return false;
			}
		} else if (!this.constantString.equals(other.constantString)) {
			return false;
		}
		if (this.isVariable != other.isVariable) {
			return false;
		}
		if (this.var == null) {
			if (other.var != null) {
				return false;
			}
		} else if (!this.var.equals(other.var)) {
			return false;
		}
		return true;
	}

}

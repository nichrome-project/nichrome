package nichrome.mln.ra;

import java.util.ArrayList;
import java.util.HashSet;

import nichrome.mln.Predicate;

/**
 * STILL IN DEVELOPMENT. An atomic formula with possibly functional arguments.
 *
 */
public class AtomEx {
	private Predicate pred;
	private ArrayList<Expression> args = new ArrayList<Expression>();
	private HashSet<String> vars = new HashSet<String>();

	public ArrayList<Expression> getArguments() {
		return this.args;
	}

	public boolean isBuiltIn() {
		return this.pred.isBuiltIn();
	}

	public AtomEx(Predicate predicate) {
		this.pred = predicate;
	}

	/**
	 * Returns the set of variable names in this literal.
	 */
	public HashSet<String> getVars() {
		return this.vars;
	}

	/**
	 * Returns the predicate of this literal.
	 */
	public Predicate getPred() {
		return this.pred;
	}

	public String toSQL() {
		// cast argument into correct types
		return null;
	}

	/**
	 * Appends a new term.
	 *
	 * @param t
	 *            the term to be appended
	 */
	public void appendTerm(Expression t) {
		this.args.add(t);
		this.vars.addAll(t.getVars());
	}

}

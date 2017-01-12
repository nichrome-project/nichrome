package nichrome.mln.ra;

import nichrome.mln.Literal;

/**
 * A conjunctive query. Used by Datalog and scoping rules.
 */
public class ConjunctiveQuery implements Cloneable {

	public boolean isView;

	public void addConstraint(Expression expression) {
		throw new RuntimeException(
			"Datalog rules and scope rules are not supported!");
	}

	public void addBodyLit(Literal literal) {
		throw new RuntimeException(
			"Datalog rules and scope rules are not supported!");
	}

	public void setHead(Literal literal) {
		throw new RuntimeException(
			"Datalog rules and scope rules are not supported!");
	}

	public void setNewTuplePrior(double pr) {
		throw new RuntimeException(
			"Datalog rules and scope rules are not supported!");
	}

}

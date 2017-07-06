package nichrome.ursa.maymust;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import nichrome.datalog.utils.ArraySet;
import nichrome.mln.MarkovLogicNetwork;

public class InferenceRule {
	public enum Kind {
		MAY, MUST, PRECISE
	}

	protected int result;
	/*
	 * Be very careful about the condition. It is the negation of the signs in
	 * GClause.
	 */
	protected List<Integer> condition;
	protected Kind kind;
	protected Set<Integer> condIds;

	public InferenceRule(int result, List<Integer> condition, Kind kind) {
		super();
		this.result = result;
		this.condition = condition;
		Collections.sort(this.condition);
		this.kind = kind;
	}

	public int getResult() {
		return result;
	}

	public List<Integer> getCondition() {
		return this.condition;
	}

	public Set<Integer> getCondIds() {
		if (condIds != null)
			return condIds;
		condIds = new ArraySet<Integer>();
		for (int l : this.condition) {
			condIds.add(Math.abs(l));
		}
		return condIds;
	}

	public boolean isMay() {
		return this.kind == Kind.MAY;
	}

	public boolean isMust() {
		return this.kind == Kind.MUST;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int size = condition.size();
		for (int i = 0; i < size; i++) {
			sb.append(condition.get(i));
			if (i != size - 1)
				sb.append(" /\\ ");
		}
		sb.append(" => ");
		sb.append(result);
		return sb.toString();
	}

	public String toVerboseString(MarkovLogicNetwork mln) {
		StringBuilder sb = new StringBuilder();

		for (int x : this.getCondition()) {
			if (x < 0) {
				sb.append("NOT ");
				x = -x;
			}
			sb.append(mln.getAtom(x).toGroundString(mln));
			sb.append(" /\\ ");
		}

		sb.append(" => ");
		sb.append(mln.getAtom(this.getResult()).toGroundString(mln));
		return sb.toString();

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((condIds == null) ? 0 : condIds.hashCode());
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + this.result;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InferenceRule other = (InferenceRule) obj;
		if (condIds == null) {
			if (other.condIds != null)
				return false;
		} else if (!condIds.equals(other.condIds))
			return false;
		if (condition == null) {
			if (other.condition != null)
				return false;
		} else if (!condition.equals(other.condition))
			return false;
		if (kind != other.kind)
			return false;
		if (result != other.result)
			return false;
		return true;
	}
	
}

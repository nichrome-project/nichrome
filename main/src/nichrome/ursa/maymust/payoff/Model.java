package nichrome.ursa.maymust.payoff;

import nichrome.ursa.maymust.Label;

public interface Model {
	public double getProbFalse(int qid);
	public Label resolve(int qid);
}

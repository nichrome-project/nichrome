package nichrome.ursa.maymust.payoff;

import nichrome.ursa.maymust.Label;

public class HalfNormalModel implements Model {

	@Override
	public double getProbFalse(int qid) {
		return 0.5;
	}

	@Override
	public Label resolve(int qid) {
		// flip a coin
		if(Math.random() > 0.5)
			return Label.FALSE;
		return Label.TRUE;
	}

}

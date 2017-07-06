package nichrome.ursa.maymust.payoff;

import nichrome.ursa.maymust.Label;
import nichrome.ursa.maymust.Oracle;

public class OracleModel implements Model {
	private Oracle o;
	public OracleModel(Oracle o){
		this.o = o;
	}
	
	@Override
	public double getProbFalse(int qid) {
		if(o.resolve(qid) == Label.FALSE)
			return 1;
		if(o.resolve(qid) == Label.TRUE)
			return 0;
		throw new RuntimeException("Unknown label for "+qid);
	}

	@Override
	public Label resolve(int qid) {
		return o.resolve(qid);
	}

}

package nichrome.ursa.maymust;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Pair;

// This oracle takes another oracle, and propagates the labels as much as possible
public class PropagatedOracle implements Oracle {
	private AnalysisWithAnnotations analyzer;

	public PropagatedOracle(Oracle o, AnalysisWithAnnotations analyzer) {
		this.analyzer = analyzer;
		Set<Pair<Integer, Label>> labels = new HashSet<Pair<Integer, Label>>();
		for (int at : analyzer.getAllAtoms()) {
			if (o.resolve(at) == Label.FALSE)
				labels.add(new Pair<Integer, Label>(at, Label.FALSE));
		}
		analyzer.labelAndPropagate(labels);
		if (AlarmResolutionDriver.client != null && AlarmResolutionDriver.client.equals("datarace"))
			analyzer.postProcess();
	}

	@Override
	public Label resolve(int atId) {
		Label ret = analyzer.getLabel(atId);
		return ret;
	}

}

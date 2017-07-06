package nichrome.ursa.maymust.payoff;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import nichrome.mln.MarkovLogicNetwork;
import nichrome.ursa.maymust.Label;

public class ExternalModel implements Model {
	private Map<Integer, Double> prediction;
	private double threshold = 0;
	private MarkovLogicNetwork mln;

	public ExternalModel(MarkovLogicNetwork mln, String inputPath) {
		this.mln = mln;
		try {
			this.prediction = new HashMap<Integer, Double>();
			Scanner sc = new Scanner(new File(inputPath));
			this.threshold = Double.parseDouble(sc.nextLine());
			while (sc.hasNextLine()) {
				String tokens[] = sc.nextLine().split("\\s+");
				int atId = mln.getAtomID(mln.parseAtom(tokens[0]));
				double prob = Double.parseDouble(tokens[1]);
				prediction.put(atId, prob);
			}
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public double getProbFalse(int qid) {
		if (prediction.containsKey(qid))
			return prediction.get(qid);
		System.out.println("Warning, no label for: "+mln.getAtom(qid).toGroundString(mln));
		return 0.0;
	}

	@Override
	public Label resolve(int qid) {
		double prob = this.getProbFalse(qid);
		if (prob >= threshold)
			return Label.FALSE;
		return Label.TRUE;
	}

}

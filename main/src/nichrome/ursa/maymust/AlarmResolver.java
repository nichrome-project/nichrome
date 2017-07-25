package nichrome.ursa.maymust;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;

import org.apache.commons.lang3.Pair;

import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.util.Config;
import nichrome.mln.util.Timer;

public class AlarmResolver {
	private AnalysisWithAnnotations augAnalyzer;
	private Oracle oracle;
	private QuestionPicker picker;
	private Set<Integer> reports;
	private int questionCount;
	private int loopCount;
	private Map<Integer, Label> qLabels;
	private Set<Integer> questionsAsked;
	private Set<Integer> reportsResolved;
	private MarkovLogicNetwork mln;
	private boolean offPre;

	public AlarmResolver(AnalysisWithAnnotations augAnalyzer, Oracle oracle, QuestionPicker picker,
			Set<Integer> queries, MarkovLogicNetwork mln, boolean offPre) {
		super();
		this.augAnalyzer = augAnalyzer;
		this.oracle = oracle;
		this.picker = picker;
		this.reports = queries;
		this.mln = mln;
		this.offPre = offPre;
	}

	/**
	 * Prepopulate certain labels in the annotated analysis.
	 * 
	 * @param labels
	 */
	public void prepolulate(Set<Pair<Integer, Label>> labels) {
		this.qLabels = null;
		this.augAnalyzer.labelAndPropagate(labels);
	}

	public void resolve() {
		this.questionCount = 0;
		this.loopCount = 0;
		this.qLabels = null;
		this.questionsAsked = new HashSet<Integer>();
		this.reportsResolved = new HashSet<Integer>();
		this.augAnalyzer.setOracle(oracle);
		
		if(!this.offPre)
			this.picker.simplify(augAnalyzer);

		// System.out.println("Enering analysis phase: ");
		// this.augAnalyzer.analyze(oracle);

		int lastRoundResult = 0;
		while (true) {
			System.out.println("INFO: time consumed so far: " + Timer.elapsed(Config.GLOBAL_TIMER));
			System.out.println("AlarmResolver: Iteration " + loopCount);
			List<Integer> questions = this.picker.pick(augAnalyzer, lastRoundResult);
			if (questions == null || questions.isEmpty()) {
				System.out.println("INFO: time consumed when leaving the picking loop: " + Timer.elapsed(Config.GLOBAL_TIMER));
				break;
			}
			System.out.print("Ask questions: ");
			for (int q : questions)
				System.out.print(mln.getAtom(q).toGroundString(mln) + ", ");
			System.out.println();
			questionsAsked.addAll(questions);
			this.loopCount++;
			this.questionCount += questions.size();
			Set<Pair<Integer, Label>> labels = new HashSet<Pair<Integer, Label>>();
			boolean areAllQsFalse = true;
			boolean areAllQsTrue = true;
			for (int q : questions) {
				Label l = this.oracle.resolve(q);
//				if (l == null)
//					throw new RuntimeException("Asking question on a tuple without labels: " + l);
				if(l == null)
					l = Label.TRUE;
				
				{// random inject noise
					Random rand = new Random();
					int x = rand.nextInt(100);
					if(x < 10) {
						Label noisy_l = (l == Label.TRUE) ? Label.FALSE : Label.TRUE;
						System.out.println("Inject noise: l= " + Label.TRUE + " noisy_l= " + noisy_l);
						l = noisy_l;
					}
				}
				
				labels.add(new Pair<Integer, Label>(q, l));
				areAllQsFalse &= (l == Label.FALSE);
				areAllQsTrue &= (l != Label.FALSE);
			}
			if(areAllQsFalse)
				lastRoundResult = 1;
			else if(areAllQsTrue)
				lastRoundResult = -1;
			else
				lastRoundResult = 0;
			this.augAnalyzer.labelAndPropagate(labels);

			// System.out.println("Before correlation.");
			// this.reportStats();
			// Taking care of the dependency between escape tuples
			if (AlarmResolutionDriver.client != null && AlarmResolutionDriver.client.equals("datarace"))
				this.augAnalyzer.postProcess();

			System.out.print("Resolved reports: ");
			for (int q : this.reports)
				if (!this.reportsResolved.contains(q) && this.augAnalyzer.isAtLabeled(q))
					System.out.print(" " + mln.getAtom(q).toGroundString(mln));
			System.out.println();

			this.getQueryLabels();

			System.out.println("After correlation.");
			this.reportStats();

			if (this.getNumResolvedQueries() == this.reports.size()){
				System.out.println("INFO: time consumed when leaving the picking loop: " + Timer.elapsed(Config.GLOBAL_TIMER));
				break;
			}
		}
//		this.augAnalyzer.checkIfFullyResolvedFP(oracle, reports);
	}

	public Map<Integer, Label> getQueryLabels() {
		if (this.qLabels == null) {
			this.qLabels = new HashMap<Integer, Label>();
		}

		for (int q : this.reports) {
			if (this.reportsResolved.contains(q))
				continue;
			Label l = this.augAnalyzer.getLabel(q);
			this.qLabels.put(q, l);
			if (l != null)
				this.reportsResolved.add(q);
		}
		return this.qLabels;
	}

	public int getNumResolvedQueries() {
		int ret = 0;
		int tR = 0;
		int fR = 0;
		this.getQueryLabels(); // trigger the map population
		for (int q : this.reports) {
			Label L = this.qLabels.get(q);
			if (L != null) {
				ret++;
				if (L == Label.FALSE) {
					++fR;
				}
				if (L == Label.TRUE) {
					++tR;
				}
			}
		}
		System.out.println("resolved: " + ret + ", Resolved(true): " + tR + ", Resolved(false): " + fR);
		return ret;
	}

	public Map<Integer, Label> getLabelMap() {
		return this.augAnalyzer.labelMap;
	}

	public int getQuestionCount() {
		return questionCount;
	}

	public Set<Integer> getQuestionsAsked() {
		return this.questionsAsked;
	}

	public int getLoopCount() {
		return loopCount;
	}

	public void reportStats() {
		System.out.println("AlarmResolver: Num of queries resolved " + this.getNumResolvedQueries());
		System.out.println("AlarmResolver: Num of questions " + this.getQuestionCount());
	}
}

package nichrome.ursa.maymust;

import java.util.List;

public interface QuestionPicker {
	/**
	 * Return a list of question to be asked to the oracle.
	 * Returns null if no more questions can be asked.
	 * lastResult: 1. all false(prediction right); 2. some true, some false; 3. all true.
	 * @param a
	 * @return
	 */
	public List<Integer> pick(AnalysisWithAnnotations a, int lastResult);
	
	public void simplify(AnalysisWithAnnotations a);
}

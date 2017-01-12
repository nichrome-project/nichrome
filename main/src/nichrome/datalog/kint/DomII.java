package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramDom;

/**
 * Domain of invoke locations represented as strings of the form: <filename>:<methodname>:<line num>:<col num>
 * 
 */
@Chord(name = "KintI")
public class DomII extends ProgramDom<String> {
 
	@Override
	public void fill() { }
}

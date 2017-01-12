package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramDom;

/**
 * Domain of bug categories represented as strings.
 */
@Chord(name = "KintB")
public class DomBB extends ProgramDom<String> {
 
	@Override
	public void fill() { }
}

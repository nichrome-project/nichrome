package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramDom;

/**
 * Domain of type names represented as strings
 */
@Chord(name = "KintT")
public class DomTT extends ProgramDom<String> {
    
	@Override
	public void fill() { }
}

package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramDom;

/**
 * Domain of Filenames represented as strings
 */
@Chord(name = "KintF")
public class DomFF extends ProgramDom<String> {
    
	@Override
	public void fill() { }
}

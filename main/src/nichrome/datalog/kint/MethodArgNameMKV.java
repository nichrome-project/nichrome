package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,k,v) such that method m has the variable name v as its 'k'th parameter.
 */
@Chord(
    name = "MethodArgNameMKV",
    sign = "KintM0,KintK0,KintV0:KintM0_KintK0_KintV0"
)
public class MethodArgNameMKV extends ProgramRel {
   
	@Override
	public void fill() { }
}


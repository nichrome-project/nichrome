package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,t) such that method m returns a value with type t.
 */
@Chord(
    name = "MethodRetTypeMT",
    sign = "KintM0,KintT0:KintM0_KintT0"
)
public class MethodRetTypeMT extends ProgramRel {
   
	@Override
	public void fill() { }
}


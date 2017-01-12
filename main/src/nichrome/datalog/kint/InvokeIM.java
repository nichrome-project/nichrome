package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramRel;

/**
 * Relation containing each tuple (i,m) such that the invoke location i invokes method m
 */
@Chord(
    name = "InvokeIM",
    sign = "KintI0,KintM0:KintI0_KintM0"
)
public class InvokeIM extends ProgramRel {
   
	@Override
	public void fill() { }
}


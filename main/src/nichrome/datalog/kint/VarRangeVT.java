package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramRel;

/**
 * Relation containing each tuple (v,t) such that the bounds on the value of variable v is defined by the type t.
 * At present, this is not giving any extra information. But, it may change to (v, lb, ub) later where lb and ub are
 * definite integer bounds. 
 */
@Chord(
    name = "VarRangeVT",
    sign = "KintV0,KintT0:KintV0_KintT0"
)
public class VarRangeVT extends ProgramRel {
   
	@Override
	public void fill() { }
}


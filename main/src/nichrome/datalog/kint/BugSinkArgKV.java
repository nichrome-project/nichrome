package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramRel;

/**
 * Relation containing each tuple (k,v) such that v si the arg which is involved in the kth bug.
 */
@Chord(
    name = "BugSinkArgKV",
    sign = "KintK0,KintV0:KintK0_KintV0"
)
public class BugSinkArgKV extends ProgramRel {
   
	@Override
	public void fill() { }
}


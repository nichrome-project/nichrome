package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramRel;

/**
 * Relation containing each tuple (k,m) such that the kth bug has a call to method m on the line 
 * which has the bug.
 */
@Chord(
    name = "BugSinkMethodKM",
    sign = "KintK0,KintM0:KintK0_KintM0"
)
public class BugSinkMethodKM extends ProgramRel {
   
	@Override
	public void fill() { }
}


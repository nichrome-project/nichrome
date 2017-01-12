package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramRel;

/**
 * Relation containing each tuple (k,i,i,i,i) such that it is a sequence of invocations which form a stack trace.
 * Each i is a stack frame. This relation can keep track of stack traces with maximum depth 4.
 */
@Chord(
    name = "BugStackTraceKIIII",
    sign = "KintK0,KintI0,KintI1,KintI2,KintI3:KintK0_KintI0_KintI1_KintI2_KintI3"
)
public class BugStackTraceKIIII extends ProgramRel {
 
	@Override
	public void fill() { }
}


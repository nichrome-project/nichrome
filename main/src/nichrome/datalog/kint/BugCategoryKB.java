package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramRel;

/**
 * Relation containing each tuple (k,b) such that the kth bug is of the category b
 */
@Chord(
    name = "BugCategoryKB",
    consumes = { "KintK", "KintB" },
    sign = "KintK0,KintB0:KintK0_KintB0"
)
public class BugCategoryKB extends ProgramRel {
   
	@Override
	public void fill() { }
}


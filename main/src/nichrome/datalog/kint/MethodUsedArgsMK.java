package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,k) such that method m has k parameters which are of type integer and 
 * which could possibly contribute to integer overflow errors.
 */
@Chord(
    name = "MethodUsedArgsMK",
    sign = "KintM0,KintK0:KintM0_KintK0"
)
public class MethodUsedArgsMK extends ProgramRel {
   
	@Override
	public void fill() { }
}


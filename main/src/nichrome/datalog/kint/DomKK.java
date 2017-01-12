package nichrome.datalog.kint;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramDom;


/**
 * Domain of integers from 0 to chord.domKK.size - 1 in order.
 */

@Chord(name = "KintK")
public class DomKK extends ProgramDom<Integer> {
	public static final int MAXZ = Integer.getInteger("chord.domKK.size", 65535);

    @Override
    public void fill() {
        for (int i = 0; i < MAXZ; i++)
            getOrAdd(new Integer(i));
    }
}

package nichrome.ursa;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramDom;

/**
 * Domain of field access statements.
 */
@Chord(name = "E")
public class DomE extends ProgramDom<Integer> {
 
	@Override
	public void fill() {
		int numElements = 0;
		try {
			Scanner sc = new Scanner(new File(nichrome.datalog.Config.bddbddbWorkDirName+"E.dom"));
			while(sc.hasNextLine()){
				String[] tokens = sc.nextLine().split(" ");
				numElements = Integer.parseInt(tokens[1]);
				break;
			}
			sc.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i = 0; i < numElements; ++i) {
			add(i);
		}
	}
}

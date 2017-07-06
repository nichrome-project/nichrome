package nichrome.ursa;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import nichrome.datalog.Chord;
import nichrome.datalog.analyses.ProgramRel;
import nichrome.mln.Predicate;
import nichrome.mln.util.UIMan;

/**
 * 
 */
@Chord(
    name = "escE",
    sign = "E0"
)
public class RelEscE extends ProgramRel {
   
	@Override
	public void fill() {
		try {
			Scanner sc = new Scanner(new File(nichrome.datalog.Config.bddbddbWorkDirName+"escE.txt"));
			while(sc.hasNextLine()){
				String s = sc.nextLine();
				String atomSegs[] = s.split("\\(");
				String predName = atomSegs[0];
				if (!predName.equalsIgnoreCase("escE")) {
					throw new RuntimeException("Unknown predicate: " + predName);
				}
				String atomBody = atomSegs[1].replaceAll("\\)", "");
				String symbols[] = atomBody.split(",");
				if (symbols.length > 1)
					throw new RuntimeException("Too many elements in tuple: " + s);
					
				String symbol = symbols[0].trim();
				add(Integer.parseInt(symbol));
			}
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}


package nichrome.ursa.maymust;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import nichrome.mln.Atom;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.parser.CommandOptions;
import nichrome.mln.util.FileMan;

public class ChordOracle implements Oracle {

	protected MarkovLogicNetwork mln;
	protected Map<Integer, Label> oracleTuples;

	public void init(MarkovLogicNetwork mln, CommandOptions options) {
		this.mln = mln;
		oracleTuples = new HashMap<Integer, Label>();
		/*
		 * The input oracle tuples must contain all IDB of oracle which are true
		 * The file must contain one tuple per line
		 */
		this.loadOracleTuples(options.fOracleTuples);
	}

	private void loadOracleTuples(String file) {
		try {
			System.out.println("Loading oracle data from " + file);
			FileInputStream in = FileMan.getFileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("//"))
					continue;
				line = line.trim();
				if (line.equals(""))
					continue;
				Label l = Label.TRUE;
				if (line.startsWith("!")) {
					l = Label.FALSE;
					line = line.substring(1);
				}
				Atom at = this.mln.parseAtom(line);
				int atId = this.mln.getAtomID(at);
				oracleTuples.put(atId, l);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Loading Oracle file failed with " + e.toString());
		}
	}

	@Override
	public Label resolve(int atId) {
		return oracleTuples.get(atId);
	}

}

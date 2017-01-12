package nichrome.mln.infer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import nichrome.mln.Atom;
import nichrome.mln.Clause;
import nichrome.mln.GClause;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.db.RDB;
import nichrome.mln.parser.CommandOptions;
import nichrome.mln.util.Config;
import nichrome.mln.util.FileMan;
import nichrome.mln.util.UIMan;

public class Inferer {

	public void infer(CommandOptions options) {
		Clause.mappingFromID2Const = new HashMap<Integer, String>();
		Clause.mappingFromID2Desc = new HashMap<String, String>();
		UIMan.println(">>> Connecting to RDBMS at " + Config.db_url);
		RDB db = RDB.getRDBbyConfig();
		db.resetSchema(Config.db_schema);
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		mln.setDB(db);
		String[] progFiles = options.fprog.split(",");
		mln.loadPrograms(progFiles);
		String[] eviFiles = options.fevid.split(",");
		mln.loadEvidences(eviFiles);
		mln.materializeTables();
		mln.prepareDB(db);

		Engine engine = new Engine(mln, db);
		UIMan.println("Total number of possible groundings: " + engine.countTotalGroundings());
		if (Config.gcLoadFile != null) {
			UIMan.println("Loading grounded clauses from file "
				+ Config.gcLoadFile + ".");
			try {
				FileInputStream in = FileMan.getFileInputStream(Config.gcLoadFile);
				engine.loadGroundedConstraints(in);
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		if (Config.revLoadFile != null) {
			UIMan.println("Loading grounded reverted constrains from file "
				+ Config.revLoadFile + ".");
			try {
				FileInputStream in = FileMan.getFileInputStream(Config.revLoadFile);
				engine.loadRevertedConstraints(in);
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		if (Config.revFdbkFile != null) {
			UIMan.println("Loading grounded feedback constrains from file "
				+ Config.revFdbkFile + ".");
			try {
				FileInputStream in = FileMan.getFileInputStream(Config.revFdbkFile);
				engine.loadRevertedConstraints(in);
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		if(Config.fullyGround){
			UIMan.println("Starting fully ground");
			engine.fullyGround();
			UIMan.println("Done fully grounding");
		}
		PrintWriter pw = FileMan.getPrintWriter(options.fout);

		if (engine.run()) {
			if(options.fquery != null){
				//Highlight the result of the queries
				pw.println("// Query tuples set to TRUE: ");
				Set<Integer> queries = new HashSet<Integer>();
				try {
					Scanner sc = new Scanner(new File(options.fquery));
					while(sc.hasNextLine()){
						String line = sc.nextLine();
						if(line.startsWith("//"))
							continue;
						Atom at = mln.parseAtom(line);
						queries.add(mln.getAtomID(at));
					}
					sc.close();
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
				Set<Integer> solution = engine.getOptSolution();
				for(int q : queries){
					if(solution.contains(q)){
						pw.println(mln.getAtom(q).toGroundString(mln));
					}
				}
				pw.println();
			}
			pw.println("// 0 Optimum value: " + engine.getObjValue());
			pw.println("// 1 The following variables should be set to true: ");
			for (int i : engine.getOptSolution()) {
				Atom at = mln.getAtom(i);
				pw.println(at.toGroundString(mln));
			}
			if (Config.log_vio_clauses) {
				pw.println();
				pw.println("// 2 The following grounded clauses are not satified: ");
				for (GClause gc : engine.getViolatdClauses()) {
					pw.println("// " + gc.toVerboseString(mln));
				}
			}
		} else {
			pw.println("// 0 UNSAT");
		}
		pw.flush();
		pw.close();
		if (Config.gcStoreFile != null) {
			UIMan.println("Storing grounded clauses to file "
				+ Config.gcStoreFile);
			try {
				FileOutputStream gcOut =
					new FileOutputStream(Config.gcStoreFile);
				engine.storeGroundedConstraints(gcOut);
				gcOut.flush();
				gcOut.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

}

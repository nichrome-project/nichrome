package nichrome.mln;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.postgresql.PGConnection;

import nichrome.mln.db.RDB;
import nichrome.mln.util.Config;
import nichrome.mln.util.ExceptionMan;
import nichrome.mln.util.FileMan;
import nichrome.mln.util.StringMan;
import nichrome.mln.util.UIMan;

/**
 * Predicate in First Order Logic.
 */
public class Predicate {

	/**
	 * Map from name to built-in predicates, e.g., same.
	 */
	private static HashMap<String, Predicate> builtInMap =
		new HashMap<String, Predicate>();

	/**
	 * Return true if the argument is the name of a built-in predicate.
	 *
	 * @param s
	 *            name of queried predicate
	 * @return true if s is a built-in predicate.
	 */
	public static boolean isBuiltInPredName(String s) {
		return Predicate.builtInMap.containsKey(s.toLowerCase());
	}

	/**
	 * Return the predicate object with the name as the argument string.
	 *
	 * @param s
	 *            name of queried predicate
	 * @return the predicate object with name s.
	 */
	public static Predicate getBuiltInPredByName(String s) {
		return Predicate.builtInMap.get(s.toLowerCase());
	}

	// logic related fields
	/**
	 * Name of this predicate.
	 */
	private String name;

	/**
	 * Whether this predicate obeys closed-world assumption.
	 */
	private boolean closedWorld = false;

	private boolean hasSoftEvidence = false;

	public boolean isImmutable() {
		return this.closedWorld && !this.hasSoftEvidence;
	}

	/**
	 * List of argument types of this predicate.
	 */
	private ArrayList<Type> types = new ArrayList<Type>();

	/**
	 * TODO: if unsat then {if scope then do scope, else do cross product}
	 */
	private boolean safeRefOnly = true;

	/**
	 * Whether this predicate is a built-in predicate.
	 */
	private boolean isBuiltIn = false;

	// DB related fields
	/**
	 * DB object associated with this predicate.
	 */
	private RDB db = null;

	/**
	 * Name of the table of this predicate in DB.
	 */
	private String relName = null;

	/**
	 * The list of arguments of this predicate. The K-th argument is named
	 * "TypeK" by default, where "Type" if the type name of this argument,
	 * unless explicitly named.
	 */
	private ArrayList<String> args = new ArrayList<String>();

	/**
	 * The assigned ID for this predicate in its parent MLN
	 * {@link Predicate#mln}.
	 */
	private int id = -1;

	/**
	 * The file object used to load evidence to DB
	 */
	protected File loadingFile = null;

	/**
	 * The buffer writer object used to flush evidence to file
	 */
	protected BufferedWriter loadingFileWriter = null;

	// MLN related fields
	/**
	 * The parent MLN containing this predicate.
	 */
	private MarkovLogicNetwork mln;

	/**
	 * Set of clauses referencing this predicate.
	 */
	private HashSet<Clause> iclauses = new HashSet<Clause>();

	/**
	 * Set of queries referencing this predicate.
	 */
	private ArrayList<Atom> queries = new ArrayList<Atom>();

	private Set<Atom> hardEvidences = new HashSet<Atom>();

	private Set<Atom> softEvidences = new HashSet<Atom>();

	private Set<Atom> hardTrainData = new HashSet<Atom>();

	/**
	 * Whether all unknown atoms of this predicate are queries.
	 */
	private boolean isAllQuery = false;

	private PreparedStatement cleanTable = null;

	private PreparedStatement insertRow = null;

	private PreparedStatement deleteRow = null;

	/**
	 * Return the name of relational table containing the ID of active atoms
	 * associated with this predicate.
	 */
	public String getRelAct() {
		return "act_" + this.relName;
	}

	/**
	 * Specify that all atoms of this predicate are queries.
	 */
	public void setAllQuery() {
		if (this.isAllQuery) {
			return;
		}
		this.isAllQuery = true;
		this.queries.clear();
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 1; i <= this.arity(); i++) {
			list.add(-i);
		}
		Tuple t = new Tuple(list);
		Atom a = new Atom(this, t);
		a.type = Atom.AtomType.QUERY;
		this.queries.add(a);
	}

	/**
	 * Specify whether this predicate obeys the closed world assumption.
	 */
	public void setClosedWorld(boolean t) {
		this.closedWorld = t;
	}

	/**
	 * Return the assigned ID of this predicate in its parent MLN.
	 */
	public int getID() {
		return this.id;
	}

	/**
	 * Return argument names of this predicate. The K-th argument is named
	 * "TypeK", where "Type" if the type name of this argument.
	 */
	public ArrayList<String> getArgs() {
		return this.args;
	}

	/**
	 * Assign an ID for this predicate. This predicate ID is used to encode
	 * tuple IDs of this predicate.
	 */
	public boolean setID(int aid) {
		if (this.id == -1) {
			this.id = aid;
			return true;
		}
		return false;
	}

	/**
	 * Return query atoms of this predicate. Used by KBMC.
	 */
	public ArrayList<Atom> getQueryAtoms() {
		return this.queries;
	}

	/**
	 * Return clauses referencing this predicate.
	 */
	public HashSet<Clause> getRelatedClauses() {
		return this.iclauses;
	}

	/**
	 * Register a query atom.
	 *
	 * @param q
	 *            the query atom; could contain variables
	 * @see Predicate#storeQueries()
	 */
	public void addQuery(Atom q) {
		if (this.isAllQuery) {
			return;
		}
		this.queries.add(q);
	}

	public boolean hasEvid = false;

	public boolean hasTrainData = false;

	public boolean scoped = false;

	/**
	 * Store an evidence in the "buffer". There is a buffer (in the form of a
	 * CSV file) for each predicate that holds the DB tuple formats of its
	 * evidence; this buffer will be flushed into the database once all evidence
	 * has been read.
	 *
	 * @param a
	 *            the evidence; following Alchemy, it must be a ground atom
	 * @see Predicate#flushEvidence()
	 */
	public void addEvidence(Atom a) {
		this.hasEvid = true;
		// Register the atom
		this.mln.getAtomID(a.base());
		if (a.isSoftEvidence()) {
			this.setHasSoftEvidence(true);
		}
		this.addEvidenceTuple(a);
	}

	/**
	 * Store training data in the "buffer". There is a buffer (in the form of a
	 * CSV file) for each predicate that holds the DB tuple formats of its
	 * evidence; this buffer will be flushed into the database once all evidence
	 * has been read.
	 *
	 * @param a
	 *            the training data; following Alchemy, it must be a ground atom
	 * @see Predicate#flushEvidence()
	 */
	public void addTrainData(Atom a) {
		this.hasTrainData = true;
		// Register the atom
		this.mln.getAtomID(a.base());
		this.addTrainTuple(a);
	}

	public void appendToWriter(String str) {
		try {
			if (this.loadingFileWriter == null) {
				this.loadingFileWriter =
					new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(this.loadingFile), "UTF8"));
			}
			this.loadingFileWriter.append(str);
		} catch (IOException e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Add evidence tuple related to this predicate. The output of this function
	 * is written to file in format like:
	 *
	 * $tuple_id,$truth_value,$prior,$club_value,$variable_name,......
	 *
	 * @param a
	 *            the atom as evidence. This atom need to be a grounded atom.
	 */
	protected void addEvidenceTuple(Atom a) {
		if (a.isSoftEvidence()) {
			this.softEvidences.add(a);
		} else {
			this.hardEvidences.add(a);
		}
	}

	/**
	 * Add training data tuple related to this predicate. The output of this
	 * function is written to file in format like:
	 *
	 * $tuple_id,$truth_value,$prior,$club_value,$variable_name,......
	 *
	 * @param a
	 *            the atom as training data. This atom need to be a grounded
	 *            atom.
	 */
	protected void addTrainTuple(Atom a) {
		// Training data is never soft.
		this.hardTrainData.add(a);
	}

	/**
	 * Close all file handles.
	 */
	public void closeFiles() {
		try {
			if (this.loadingFileWriter != null) {
				this.loadingFileWriter.close();
				this.loadingFileWriter = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Flush the evidence buffer to the predicate table, using the COPY
	 * statement in PostgreSQL.
	 *
	 * @see Predicate#addEvidence(Atom)
	 */
	public void flushEvidence(boolean... specialMode) {
		try {

			if (this.hasEvid == true && specialMode.length > 0) {
				String sql = "DELETE FROM " + this.getRelName();
				this.db.execute(sql);
			}

			// flush the file
			if (this.loadingFileWriter != null) {
				this.loadingFileWriter.close();
				this.loadingFileWriter = null;
			}
			if (!this.loadingFile.exists()) {
				return;
			}
			// copy into DB
			ArrayList<String> cols = new ArrayList<String>();
			cols.add("truth");
			cols.add("prior");
			cols.add("club");
			cols.addAll(this.args);
			FileInputStream in = new FileInputStream(this.loadingFile);
			PGConnection con = (PGConnection) this.db.getConnection();
			String sql =
				"COPY " + this.getRelName() + StringMan.commaListParen(cols)
					+ " FROM STDIN CSV";
			con.getCopyAPI().copyIn(sql, in);
			in.close();
			// db.analyze(getRelName());
			FileMan.removeFile(this.loadingFile.getAbsolutePath());
		} catch (Exception e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Constructor of Predicate.
	 *
	 * @param mln
	 *            the parent MLN that hosts this predicate
	 * @param aname
	 *            the name; must be unique
	 * @param aClosedWorld
	 *            indicates whether to make the closed-world asssumption
	 */
	public Predicate(MarkovLogicNetwork mln, String aname, boolean aClosedWorld) {
		this.mln = mln;
		this.name = aname;
		this.closedWorld = aClosedWorld;
		this.relName = "pred_" + this.name.toLowerCase();
	}

	/**
	 * Checks if there are any queries associated with this predicate.
	 */
	public boolean hasQuery() {
		return !this.queries.isEmpty();
	}

	public void setDB(RDB adb) {
		this.db = adb;
	}

	public RDB getDB() {
		return this.db;
	}

	/**
	 * Initialize database objects for this predicate.
	 */
	public void prepareDB(RDB adb) {
		this.db = adb;
		UIMan.verbose(1, ">>> Creating predicate table " + this.getRelName());
		this.createTable();
	}

	/**
	 * Create table for storing groundings of this predicate. club: 1 = NONE; 2
	 * = EVIDENCE: atom in evidence; 3 = QUERY: atom in query; 4 = QUEVID: atom
	 * in query as evidence.
	 */
	public void createTable() {
		this.db.dropTable(this.relName);
		this.db.dropView(this.relName);
		this.db.dropSequence(this.relName + "_id_seq");
		String sql = "CREATE TABLE " + this.getRelName() + "(\n";
		ArrayList<String> argDefs = new ArrayList<String>();
		for (int i = 0; i < this.args.size(); i++) {
			String attr = this.args.get(i);
			String ts = " BIGINT";
			Type t = this.getTypeAt(i);
			// String foreignKey =
			// " references "+t.getRelName()+"("+Config.CONSTANT_ID+")";
			String foreignKey = "";
			argDefs.add(attr + ts + foreignKey);
		}
		sql += StringMan.commaList(argDefs) + ")";

		if (Config.using_greenplum) {
			sql = sql + " DISTRIBUTED BY (" + this.args.get(0) + ")";
		}

		this.db.update(sql);

		sql =
			"CREATE UNIQUE INDEX idx_" + this.getRelName() + " ON "
				+ this.getRelName() + " (";

		sql += StringMan.commaList(this.args);

		sql += ")";
		this.db.update(sql);

		// for(String attr : args){
		// sql = "CREATE INDEX idx_"+getRelName()+"_"+attr +
		// " ON "+getRelName()+"("+attr+")";
		// db.update(sql);
		// }
		this.createStatements();
	}

	private void createStatements() {
		this.cleanTable =
			this.db.getPrepareStatement("DELETE FROM " + this.getRelName());

		String insertSql = "INSERT INTO " + this.getRelName() + " VALUES(";
		List<String> parts = new ArrayList<String>();
		for (int i = 0; i < this.args.size(); i++) {
			parts.add("?");
		}
		insertSql += StringMan.commaList(parts);
		insertSql += ")";
		this.insertRow = this.db.getPrepareStatement(insertSql);

		String deleteSql = "DELETE FROM " + this.getRelName() + " WHERE 1=1 ";
		for (String attr : this.args) {
			deleteSql += "AND " + attr + " = ?";
		}

		this.deleteRow = this.db.getPrepareStatement(deleteSql);
	}

	public void insertAtom(Atom a) {
		if (a.pred != this) {
			throw new RuntimeException("Unable to insert " + a + " into "
				+ this);
		}

		try {
			for (int i = 0; i < this.args.size(); i++) {
				this.insertRow.setInt(i + 1, a.args.get(i));
			}
			this.insertRow.execute();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void deleteAtom(Atom a) {
		if (a.pred != this) {
			throw new RuntimeException("Unable to delete " + a + " from "
				+ this);
		}

		try {
			for (int i = 0; i < this.args.size(); i++) {
				this.deleteRow.setInt(i + 1, a.args.get(i));
			}
			this.deleteRow.execute();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void cleanTable() {
		try {
			this.cleanTable.execute();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Set<Atom> reportQuery() {
		Set<Atom> ret = new HashSet<Atom>();
		return ret;
	}

	public void loadTable(List<Atom> atoms) {
		BufferedWriter writer = null;
		File loadingFile =
			new File(Config.getLoadingDir(), "loading_predicate_" + this.name);
		try {
			writer =
				new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
					loadingFile), "UTF8"));
			for (Atom at : atoms) {
				if (at.pred.equals(this)) {
					writer.append(at.args.toCommaList() + "\n");
				}
			}
			writer.flush();
			writer.close();

			FileInputStream in = new FileInputStream(loadingFile);
			PGConnection con = (PGConnection) this.db.getConnection();
			String sql = "COPY " + this.relName + " FROM STDIN (DELIMITER ',')";
			con.getCopyAPI().copyIn(sql, in);
			in.close();

			// domain.clear();
		} catch (Exception e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Return the arity of this predicate.
	 */
	public int arity() {
		return this.args.size();
	}

	/**
	 * Register a clause referencing this predicate
	 *
	 * @param c
	 *            a clause referencing this predicate
	 */
	public void addRelatedClause(Clause c) {
		this.iclauses.add(c);
	}

	/**
	 * Append a new argument without a user-provided name.
	 *
	 * @param t
	 *            the type of the new argument
	 */
	public void appendArgument(Type t) {
		this.types.add(t);
		this.args.add(t.name() + this.types.size());
		this.argNameList.add(null);
	}

	private HashMap<String, Integer> argNameMap =
		new HashMap<String, Integer>();
	private ArrayList<String> argNameList = new ArrayList<String>();

	/**
	 * Append a new argument with a user provided name.
	 *
	 * @param t
	 *            the type of the new argument
	 * @param name
	 *            user-provided name for this argument/attribute
	 */
	public void appendArgument(Type t, String name) {
		this.types.add(t);
		if (name == null) {
			this.args.add(t.name() + this.types.size());
		} else {
			this.args.add(name);
		}
		if (this.argNameMap.containsKey(name)) {
			ExceptionMan.die("duplicate argument name '" + name
				+ "' in predicate '" + this.name + "'");
		} else if (name != null) {
			this.argNameList.add(name);
			this.argNameMap.put(name, this.args.size() - 1);
		}
	}

	/**
	 * Return the position of the given argument name.
	 *
	 * @param aname
	 *            argument name
	 */
	public int getArgPositionByName(String aname) {
		if (!this.argNameMap.containsKey(aname)) {
			return -1;
		}
		return this.argNameMap.get(aname);
	}

	/**
	 * Mark the point when all arguments have been given. Go through the
	 * arguments again to try to give unnamed arguments names.
	 */
	public void sealDefinition() {
		HashSet<Type> tset = new HashSet<Type>();
		HashSet<Type> dset = new HashSet<Type>();
		for (Type t : this.types) {
			if (tset.contains(t)) {
				dset.add(t);
			}
			tset.add(t);
		}
		tset.removeAll(dset);
		for (int i = 0; i < this.argNameList.size(); i++) {
			if (this.argNameList.get(i) == null) {
				Type t = this.types.get(i);
				String tn = t.name();
				if (tset.contains(t) && !this.argNameMap.containsKey(tn)) {
					this.argNameList.set(i, tn);
					this.argNameMap.put(tn, i);
				}
			}
		}
	}

	/**
	 * TODO: look into the implications of FDs
	 *
	 */
	@SuppressWarnings("unused")
	private class FunctionalDependency {
		HashSet<Integer> determinant = null;
		public int dependent = -1;
	}

	/**
	 * Functional dependencies for this predicate.
	 */
	private ArrayList<FunctionalDependency> fds =
		new ArrayList<FunctionalDependency>();

	public void setMLN(MarkovLogicNetwork _mln) {
		this.mln = _mln;
	}

	public MarkovLogicNetwork getMLN() {
		return this.mln;
	}

	/**
	 * Add a functional dependency for the attributes of this predicate
	 *
	 * @param determinant
	 * @param dependent
	 */
	public void addFunctionalDependency(List<String> determinant,
		String dependent) {
		HashSet<Integer> det = new HashSet<Integer>();
		for (String s : determinant) {
			int idx = this.argNameMap.get(s);
			if (idx < 0) {
				ExceptionMan.die("unknown attribute name '" + s
					+ "' for predicate '" + this.name + "' when defining "
					+ "functional dependency.");
			} else {
				det.add(idx);
			}
		}
		int dep = this.argNameMap.get(dependent);
		if (dep < 0) {
			ExceptionMan.die("unknown attribute name '" + dependent
				+ "' for predicate '" + this.name + "' when defining "
				+ "functional dependency.");
		}
		FunctionalDependency fd = new FunctionalDependency();
		fd.dependent = dep;
		fd.determinant = det;
		this.fds.add(fd);
	}

	/**
	 * Return the type of the k-th argument.
	 */
	public Type getTypeAt(int k) {
		return this.types.get(k);
	}

	/**
	 * Check if this predicate makes the closed-world assumption.
	 */
	public boolean isClosedWorld() {
		return this.closedWorld;
	}

	/**
	 * Return the name of this predicate.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the relational table name of this predicate..
	 */
	public String getRelName() {
		return this.relName;
	}

	/**
	 * Set whether all references to this predicate are safe; i.e., all
	 * variables in corresponding positive literals are bound to other literals
	 * in the same clause.
	 *
	 * @param safeRefOnly
	 */
	public void setSafeRefOnly(boolean safeRefOnly) {
		this.safeRefOnly = safeRefOnly;
	}

	public boolean isSafeRefOnly() {
		return this.safeRefOnly;
	}

	public void setHasSoftEvidence(boolean hasSoftEvidence) {
		this.hasSoftEvidence = hasSoftEvidence;
	}

	public boolean hasSoftEvidence() {
		return this.hasSoftEvidence;
	}

	public boolean isBuiltIn() {
		return this.isBuiltIn;
	}

	@Override
	public String toString() {
		String ret = "";

		ret = this.getName();
		ret += "(";
		ret += StringMan.commaList(this.getArgs());
		ret += ")";

		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result =
			prime * result + ((this.name == null) ? 0 : this.name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		Predicate other = (Predicate) obj;
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public void setCompeletelySpecified(boolean b) {
		throw new RuntimeException("Feature not supported.");
	}

	public void addDependentAttrPosition(int i) {
		throw new RuntimeException("Feature not supported.");
	}

	public Set<Atom> getHardEvidences() {
		return this.hardEvidences;
	}

	public Set<Atom> getSoftEvidences() {
		return this.softEvidences;
	}

	public Set<Atom> getHardTrainData() {
		return this.hardTrainData;
	}

}

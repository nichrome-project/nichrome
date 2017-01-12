package nichrome.mln;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import nichrome.mln.Clause.ClauseInstance;
import nichrome.mln.db.RDB;
import nichrome.mln.parser.InputParser;
import nichrome.mln.ra.ConjunctiveQuery;
import nichrome.mln.ra.Expression;
import nichrome.mln.ra.Function;
import nichrome.mln.util.Config;
import nichrome.mln.util.DebugMan;
import nichrome.mln.util.ExceptionMan;
import nichrome.mln.util.FileMan;
import nichrome.mln.util.UIMan;

/**
 * An MLN. Holds the symbol table.
 */
public class MarkovLogicNetwork implements Cloneable {
	private static int idGen = 0;
	private int id = 0;

	/**
	 * The db connection associated with this MLN.
	 */
	private RDB db = null;

	/**
	 * Database tables storing intermediate data.
	 */
	public String relClauses = "clauses";
	public String relAtoms = "atoms";
	public String relTrueAtoms = "true_atoms";
	public String relClausePart = "clause_part";
	public String relAtomPart = "atom_part";
	public String relComponents = "query_components";

	/**
	 * Parser of input.
	 */
	protected InputParser parser;

	public RDB getDB() {
		return this.db;
	}

	/**
	 * List of all predicates appearing in this MLN.
	 */
	private ArrayList<Predicate> listPred = new ArrayList<Predicate>();

	/**
	 * Map from string name to Predicate object.
	 */
	private Hashtable<String, Predicate> nameMapPred =
		new Hashtable<String, Predicate>();

	private Hashtable<String, Function> nameMapFunc =
		new Hashtable<String, Function>();

	/**
	 * Map from string name to Type object.
	 */
	private Hashtable<String, Type> nameMapType = new Hashtable<String, Type>();

	public Type getTypeByName(String tname) {
		return this.nameMapType.get(tname);
	}

	public Collection<Type> getAllTypes() {
		return this.nameMapType.values();
	}

	private HashMap<Clause, Clause> unnormal2normal =
		new HashMap<Clause, Clause>();

	/**
	 * List of normalized clauses.
	 */
	public ArrayList<Clause> listClauses = new ArrayList<Clause>();

	/**
	 * List of unnormalized clauses.
	 */
	public ArrayList<Clause> unnormalizedClauses = new ArrayList<Clause>();

	/**
	 * Map from signature of clauses to Clause object. For the definition of
	 * ``signature'', see {@link Clause#normalize()}.
	 */
	private Hashtable<String, Clause> sigMap = new Hashtable<String, Clause>();

	/**
	 * Map from string name to integer constant ID.
	 */
	private HashMap<String, Integer> mapConstantID =
		new HashMap<String, Integer>();

	private List<String> constants = new ArrayList<String>();

	/**
	 * Map from Atom to Integer constant ID, each atom must be grounded.
	 */
	private Map<Atom, Integer> mapAtomID = new HashMap<Atom, Integer>();

	private List<Atom> atoms = new ArrayList<Atom>();

	private ArrayList<Predicate> clusteringPredicates =
		new ArrayList<Predicate>();

	@Override
	@SuppressWarnings("unchecked")
	public Object clone() throws CloneNotSupportedException {

		MarkovLogicNetwork clone = (MarkovLogicNetwork) super.clone();

		clone.db = this.db;

		clone.parser = this.parser;

		clone.listPred = (ArrayList<Predicate>) this.listPred.clone();

		clone.nameMapPred =
			(Hashtable<String, Predicate>) this.nameMapPred.clone();

		clone.nameMapFunc =
			(Hashtable<String, Function>) this.nameMapFunc.clone();

		clone.nameMapType = (Hashtable<String, Type>) this.nameMapType.clone();

		clone.listClauses = (ArrayList<Clause>) this.listClauses.clone();

		clone.unnormalizedClauses =
			(ArrayList<Clause>) this.unnormalizedClauses.clone();

		clone.sigMap = (Hashtable<String, Clause>) this.sigMap.clone();

		clone.mapConstantID =
			(HashMap<String, Integer>) this.mapConstantID.clone();

		clone.clusteringPredicates =
			(ArrayList<Predicate>) this.clusteringPredicates.clone();

		return clone;

	}

	/**
	 * Returns the RDB used by this MLN.
	 */
	public RDB getRDB() {
		return this.db;
	}

	public int getID() {
		return this.id;
	}

	/**
	 * Constructor of MLN. {@link MarkovLogicNetwork#parser} will be constructed
	 * here.
	 *
	 */
	public MarkovLogicNetwork() {
		this.parser = new InputParser(this);
		this.id = (MarkovLogicNetwork.idGen++);
		String relp = "mln" + this.id + "_";
		this.relAtoms = relp + "atoms";
		this.relClauses = relp + "clauses";
		this.relClausePart = relp + "clause_part";
		this.relAtomPart = relp + "atom_part";
		this.relComponents = relp + "query_components";
		this.constants.add(null);
		this.atoms.add(null);
	}

	/**
	 * Registers a new, unnormalized clause.
	 *
	 * @param c
	 *            the clause to be registered
	 */
	public void registerClause(Clause c) {
		if (c == null) {
			return;
		}
		this.unnormalizedClauses.add(c);
	}

	/**
	 * Get the clause object by integer ID. Accepts negative id, and will
	 * translate it into positive. Does not accept zero id or id larger than the
	 * number of clauses, and will return null.
	 *
	 * @param id
	 *            ID of wanted clause.
	 */
	public Clause getClauseById(int id) {
		if (id < 0) {
			id = -id;
		}
		if (id < 1 || id > this.listClauses.size()) {
			return null;
		}
		return this.listClauses.get(id - 1);
	}

	/**
	 * Normalize all clauses. If the signature of this clause is as the same as
	 * some some existing clauses in {@link MarkovLogicNetwork#listClauses},
	 * then {@link Clause#absorb(Clause)} this new clause. If not absorbed, this
	 * new clause is set an ID sequentially and a name Clause$id. Predicates in
	 * this clause is registered by {@link Predicate#addRelatedClause(Clause)}.
	 *
	 * @see Clause#normalize()
	 * @see Clause#absorb(Clause)
	 */
	public void normalizeClauses() {
		this.listClauses.clear();
		for (Clause c : this.unnormalizedClauses) {

			// applyScopes(c);
			if (c.hasEmbeddedWeight()) {
				this.listClauses.add(c);
				int id = this.listClauses.size();
				c.setId(id);
				c.setName("Clause" + id);
				for (Predicate p : c.getReferencedPredicates()) {
					p.addRelatedClause(c);
				}
				this.unnormal2normal.put(c, c);
				continue;
			}
			Clause tmpc = c;
			c = c.normalize();

			this.unnormal2normal.put(tmpc, c);

			if (c == null) {
				continue;
			}
			c.checkVariableSafety();
			Clause ec = this.sigMap.get(c.getSignature());
			if (ec == null) {
				this.listClauses.add(c);
				this.sigMap.put(c.getSignature(), c);
				int id = this.listClauses.size();
				c.setId(id);
				c.setName("Clause" + id);
				for (Predicate p : c.getReferencedPredicates()) {
					p.addRelatedClause(c);
				}
			} else {
				ec.absorb(c);
			}
		}
		for (Clause c : this.listClauses) {
			UIMan.verbose(2, "\n" + c.toString());
		}
	}

	/**
	 * Finalize the definitions of all clauses, i.e., prepare the database table
	 * used by each clause, including 1) instance table for each clause; 2) SQL
	 * needed to ground this clause. Call this when all clauses have been
	 * parsed.
	 */
	public void finalizeClauseDefinitions(RDB adb) {
		for (Clause c : this.listClauses) {
			c.prepareForDB(adb);
		}
	}

	/**
	 * Return the type of a given name; create if this type does not exist.
	 */
	public Type getOrCreateTypeByName(String name) {
		Type t = this.nameMapType.get(name);
		if (t == null) {
			t = new Type(name);
			this.nameMapType.put(name, t);
		}
		return t;
	}

	/**
	 * Call materialize() for all types. This will put the domain members of
	 * each type into corresponding database tables.
	 *
	 * @see Type#storeConstantList(RDB)
	 */
	private void materializeAllTypes(RDB adb) {
		for (Type t : this.nameMapType.values()) {
			t.storeConstantList(adb);
		}
	}

	/**
	 * Return the set of all predicates.
	 */
	public HashSet<Predicate> getAllPred() {
		return new HashSet<Predicate>(this.listPred);
	}

	public ArrayList<Predicate> getAllPredOrderByName() {
		ArrayList<String> pnames = new ArrayList<String>();
		for (Predicate p : this.listPred) {
			pnames.add(p.getName());
		}
		Collections.sort(pnames);
		ArrayList<Predicate> ps = new ArrayList<Predicate>();
		for (String pn : pnames) {
			ps.add(this.getPredByName(pn));
		}
		return ps;
	}

	/**
	 * Register a new predicate. Here by ``register'' it means 1) set ID for
	 * this predicate sequentially; 2) push it into
	 * {@link MarkovLogicNetwork#listPred} ; 3) building the map from predicate
	 * name to this predicate.
	 */
	public void registerPred(Predicate p) {
		if (this.nameMapPred.containsKey(p.getName())) {
			// ExceptionMan.die("Duplicate predicate definitions - " +
			// p.getName());
			return;
		}
		if (Predicate.isBuiltInPredName(p.getName())) {
			System.err.println("WARNING: user-defined predicate '"
				+ p.getName() + "' will be overridden by the built-in one!");
			return;
		}
		p.setMLN(this);
		p.setID(this.listPred.size());
		this.listPred.add(p);
		this.nameMapPred.put(p.getName(), p);
	}

	/**
	 * Return the predicate of the given name; null if such predicate does not
	 * exist.
	 */
	public Predicate getPredByName(String name) {
		Predicate bip = Predicate.getBuiltInPredByName(name);
		if (bip != null) {
			return bip;
		}
		return this.nameMapPred.get(name);
	}

	/**
	 * Get a function by its name; can be built-in.
	 *
	 * @param name
	 *
	 */
	public Function getFunctionByName(String name) {
		Function f = Function.getBuiltInFunctionByName(name);
		if (f != null) {
			return f;
		}
		return this.nameMapFunc.get(name);
	}

	/**
	 * Return all unnormalized clauses as read from the input file.
	 */
	public ArrayList<Clause> getAllUnnormalizedClauses() {
		return this.unnormalizedClauses;
	}

	/**
	 * Return all normalized clauses.
	 */
	public ArrayList<Clause> getAllNormalizedClauses() {
		return this.listClauses;
	}

	public HashSet<String> additionalHardClauseInstances =
		new HashSet<String>();

	/**
	 * Return assigned ID of a constant symbol. If this symbol is a new one, a
	 * new ID will be assigned to it, and the symbol table will be updated.
	 */
	public Integer getSymbolID(String symbol, Type type) {
		Integer id = this.mapConstantID.get(symbol);
		if (id == null) {
			id = this.mapConstantID.size() + 1;
			this.constants.add(symbol);
			this.mapConstantID.put(symbol, id);
			if (Config.learning_mode) {
				Clause.mappingFromID2Const.put(id, symbol);
			}
		}
		if (type != null) {
			type.addConstant(id);
		}
		return id;
	}

	public String getSymbol(int id) {
		return this.constants.get(id);
	}

	public Atom getAtom(int id) {
		return this.atoms.get(id);
	}

	/**
	 * Parsing this atom won't change the domain of the predicates.
	 *
	 * @param s
	 * @return
	 */
	public Atom parseAtom(String s) {
		String atomSegs[] = s.split("\\(");
		String predName = atomSegs[0];
		Predicate p = this.getPredByName(predName);
		if (p == null) {
			UIMan.verbose(1, "Unknown predicate: " + predName);
		}
		String atomBody = atomSegs[1].replaceAll("\\)", "");
		String symbols[] = atomBody.split(",");
		int symIds[] = new int[symbols.length];
		for (int i = 0; i < symbols.length; i++) {
			String symbol = symbols[i].trim();
			if (!this.mapConstantID.containsKey(symbol)) {
				return null;
			}
			int symbolId = this.mapConstantID.get(symbol);
			Type t = p.getTypeAt(i);
			if (!t.contains(symbolId)) {
				return null;
			}
			symIds[i] = symbolId;
		}
		Tuple tuple = new Tuple(symIds);
		Atom at = new Atom(p, tuple);
		return at;
	}
	
	/**
	 * Parsing the atom. Return null if unknown predicate.
	 * For CAV'13 algo.
	 *
	 * @param s
	 * @return
	 */
	public Atom parseAtomAndCheck(String s) {
		String atomSegs[] = s.split("\\(");
		String predName = atomSegs[0];
		Predicate p = this.getPredByName(predName);
		if (p == null) {
			UIMan.verbose(1, "Unknown predicate: " + predName);
			return null;
		}
		String atomBody = atomSegs[1].replaceAll("\\)", "");
		String symbols[] = atomBody.split(",");
		int symIds[] = new int[symbols.length];
		for (int i = 0; i < symbols.length; i++) {
			String symbol = symbols[i].trim();
			if (!this.mapConstantID.containsKey(symbol)) {
				return null;
			}
			int symbolId = this.mapConstantID.get(symbol);
			Type t = p.getTypeAt(i);
			if (!t.contains(symbolId)) {
				return null;
			}
			symIds[i] = symbolId;
		}
		Tuple tuple = new Tuple(symIds);
		Atom at = new Atom(p, tuple);
		return at;
	}

	public synchronized Integer getAtomID(Atom atom) {
		if (!atom.isGrounded()) {
			throw new RuntimeException("Atom " + atom + " is not grounded.");
		}
		atom = atom.base();
		Integer id = this.mapAtomID.get(atom);
		if (id == null) {
			id = this.mapAtomID.size() + 1;
			this.atoms.add(atom);
			this.mapAtomID.put(atom, id);
		}
		return id;
	}
	
	public int getNumAtoms() {
		return this.mapAtomID.size();
	}

	/**
	 * Store all evidences into the database by flushing the "buffers". These
	 * tuples are pushed into the relational table
	 * {@link Predicate#getRelName()} in the database.
	 */
	public void storeAllEvidence() {
		for (Predicate p : this.getAllPred()) {
			p.flushEvidence();
		}
	}

	/**
	 * Close all file handles used by each predicate in
	 * {@link MarkovLogicNetwork#listPred}.
	 */
	public void closeFiles() {
		for (Predicate p : this.listPred) {
			p.closeFiles();
		}
	}

	/**
	 * Parse multiple MLN program files.
	 *
	 * @param progFiles
	 *            list of MLN program files (in Alchemy format)
	 */
	public void loadPrograms(String[] progFiles) {
		for (String f : progFiles) {
			String g = FileMan.getGZIPVariant(f);
			if (g == null) {
				ExceptionMan.die("non-existent file: " + f);
			} else {
				f = g;
			}
			UIMan.println(">>> Parsing program file: " + f);
			this.parser.parseProgramFile(f);
		}
		this.normalizeClauses();
	}

	public void loadProgramsButNotNormalizeClauses(String[] progFiles) {
		for (String f : progFiles) {
			String g = FileMan.getGZIPVariant(f);
			if (g == null) {
				ExceptionMan.die("non-existent file: " + f);
			} else {
				f = g;
			}
			UIMan.println(">>> Parsing program file: " + f);
			this.parser.parseProgramFile(f);
		}
	}

	/**
	 * Parse multiple MLN evidence files. If file size is larger than 1MB, then
	 * uses a file stream incrementally parse this file. Can also accept .gz
	 * file (see {@link GZIPInputStream#GZIPInputStream(InputStream)}).
	 *
	 * @param evidFiles
	 *            list of MLN evidence files (in Alchemy format)
	 */
	public void loadEvidences(String[] evidFiles) {
		int chunkSize = Config.evidence_file_chunk_size;
		for (String f : evidFiles) {
			String g = FileMan.getGZIPVariant(f);
			if (g == null) {
				ExceptionMan.die("File does not exist: " + f);
			} else {
				f = g;
			}
			UIMan.println(">>> Parsing evidence file: " + f);

			if (FileMan.getFileSize(f) <= chunkSize) {
				this.parser.parseEvidenceFile(f);
			} else {
				try {
					long lineOffset = 0, lastChunkLines = 0;
					BufferedReader reader = FileMan.getBufferedReaderMaybeGZ(f);
					StringBuilder sb = new StringBuilder();
					String line = reader.readLine();
					while (line != null) {
						sb.append(line);
						sb.append("\n");
						lastChunkLines++;
						if (sb.length() >= chunkSize) {
							this.parser.parseEvidenceString(sb.toString(),
								lineOffset);
							sb.delete(0, sb.length());
							sb = new StringBuilder();
							lineOffset += lastChunkLines;
							lastChunkLines = 0;
							UIMan.print(".");
						}
						line = reader.readLine();
					}
					reader.close();
					if (sb.length() > 0) {
						this.parser.parseEvidenceString(sb.toString(),
							lineOffset);
					}
					UIMan.println();
				} catch (Exception e) {
					ExceptionMan.handle(e);
				}
			}
			try {
				DebugMan.runGC();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Parse multiple MLN training data files. If file size is larger than 1MB,
	 * then uses a file stream incrementally parse this file. Can also accept
	 * .gz file (see {@link GZIPInputStream#GZIPInputStream(InputStream)}).
	 *
	 * @param evidFiles
	 *            list of MLN training data files (in Alchemy format)
	 */
	public void loadTrainData(String[] trainFiles) {
		int chunkSize = Config.evidence_file_chunk_size;
		for (String f : trainFiles) {
			String g = FileMan.getGZIPVariant(f);
			if (g == null) {
				ExceptionMan.die("File does not exist: " + f);
			} else {
				f = g;
			}
			UIMan.println(">>> Parsing training data file: " + f);

			if (FileMan.getFileSize(f) <= chunkSize) {
				this.parser.parseTrainFile(f);
			} else {
				try {
					long lineOffset = 0, lastChunkLines = 0;
					BufferedReader reader = FileMan.getBufferedReaderMaybeGZ(f);
					StringBuilder sb = new StringBuilder();
					String line = reader.readLine();
					while (line != null) {
						sb.append(line);
						sb.append("\n");
						lastChunkLines++;
						if (sb.length() >= chunkSize) {
							this.parser.parseTrainString(sb.toString(),
								lineOffset);
							sb.delete(0, sb.length());
							sb = new StringBuilder();
							lineOffset += lastChunkLines;
							lastChunkLines = 0;
							UIMan.print(".");
						}
						line = reader.readLine();
					}
					reader.close();
					if (sb.length() > 0) {
						this.parser.parseTrainString(sb.toString(), lineOffset);
					}
					UIMan.println();
				} catch (Exception e) {
					ExceptionMan.handle(e);
				}
			}
			try {
				DebugMan.runGC();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Parse multiple MLN query files.
	 *
	 * @param queryFiles
	 *            list of MLN query files (in Alchemy format)
	 */
	public void loadQueries(String[] queryFiles) {
		for (String f : queryFiles) {
			String g = FileMan.getGZIPVariant(f);
			if (g == null) {
				ExceptionMan.die("non-existent file: " + f);
			} else {
				f = g;
			}
			UIMan.println(">>> Parsing query file: " + f);
			this.parser.parseQueryFile(f);
		}
	}

	/**
	 * Read in the query atoms provided by the command line.
	 */
	public void parseQueryCommaList(String queryAtoms) {
		this.parser.parseQueryCommaList(queryAtoms);
	}

	/**
	 * Prepare the database for each predicate and clause.
	 *
	 * @see Predicate#prepareDB(RDB)
	 * @see MarkovLogicNetwork#finalizeClauseDefinitions(RDB)
	 */
	public void prepareDB(RDB adb) {
		this.db = adb;
		for (Predicate p : this.listPred) {
			p.prepareDB(adb);
		}
		this.finalizeClauseDefinitions(adb);
	}

	public void setDB(RDB adb) {
		this.db = adb;
	}

	/**
	 * Clean up temporary data in DB and working dir, including 1) drop schema
	 * in PostgreSQL; 2) remove directory.
	 *
	 * @return true on success
	 */
	public boolean cleanUp() {
		this.closeFiles();
		return this.db.dropSchema(Config.db_schema)
			&& FileMan.removeDirectory(new File(Config.getWorkingDir()));
	}

	/**
	 * Stores constants and evidence into database table.
	 *
	 * @see MarkovLogicNetwork#materializeAllTypes(RDB)
	 * @see MarkovLogicNetwork#storeAllEvidence()
	 */
	public void materializeTables() {
		UIMan.verbose(1, ">>> Storing symbol tables...");
		UIMan.verbose(1, "    constants = " + this.mapConstantID.size());
		this.db.createConstantTable(this.mapConstantID, Config.relConstants);
		// mapConstantID = null;
		try {
			DebugMan.runGC();
			DebugMan.runGC();
			DebugMan.runGC();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.materializeAllTypes(this.db);
		for (Predicate p : this.getAllPred()) {
			p.prepareDB(this.db);
		}
		// UIMan.println(">>> Storing evidence...");
		// storeAllEvidence();
	}

	public void registerPostprocRule(ConjunctiveQuery cq) {
		throw new RuntimeException("Post proc rules are not supported!");
	}

	public void registerIntermediateRule(ConjunctiveQuery cq) {
		throw new RuntimeException("Intermediate rules are not supported!");
	}

	public void registerScopingRule(ConjunctiveQuery cq) {
		throw new RuntimeException("Scoping rules are not supported!");
	}

	public void registerDatalogRule(ConjunctiveQuery cq) {
		throw new RuntimeException("Datalog rules are not supported!");
	}

	public GClause matchGroundedClause(double weight, int[] atomIds) {
		OUT: for (Clause c : this.getAllNormalizedClauses()) {
			if (!c.isTemplate() && c.getWeight() != weight && !Config.ignoreWarmGCWeight) {
				continue;
			}
			if (c.lits.size() != atomIds.length) {
				continue;
			}
			Map<String, Integer> varMap = new HashMap<String, Integer>();
			Map<String, String> valueMap = new HashMap<String, String>();
			for (int i = 0; i < atomIds.length; i++) {
				Atom at = this.getAtom(Math.abs(atomIds[i]));
				Literal l = c.lits.get(i);
				if (!at.pred.equals(l.getPred())) {
					continue OUT;
				}
				if ((atomIds[i] > 0) != l.getSense()) {
					continue OUT;
				}
				List<Term> terms = l.getTerms();
				// bind constants in atoms to variable in the clause
				for (int j = 0; j < terms.size(); j++) {
					Term ct = terms.get(j);
					String varName = ct.var();
					int varValue = at.args.get(j);
					Integer oldVarValue = varMap.get(varName);
					if (oldVarValue != null) {
						if (oldVarValue.intValue() != varValue) {
							continue OUT;
						}
					} else {
						varMap.put(varName, varValue);
						valueMap.put(varName, this.getSymbol(varValue));
					}
				}
			}
			// check Expressions
			List<Expression> exps = c.getConstraints();
			try {
				// all expression should evaluate to false
				for (Expression e : exps) {
					if (!e.evaluate(valueMap)) {
						continue OUT;
					}
				}
			} catch (Exception e) {
				UIMan.println("Non floating type expression not supported!");
			}
			if (c.isTemplate()) {
				INNER: for (ClauseInstance ci : c.instances) {
					if (ci.weight != weight && !Config.ignoreWarmGCWeight) {
						continue INNER;
					}
					for (int k = 0; k < ci.conList.size(); k++) {
						String metaVar = c.metaVars.get(k);
						int metaVarValue = varMap.get(metaVar);
						if (metaVarValue != ci.conList.get(k).constant()) {
							continue INNER;
						}
					}
					GClause ret = new GClause(c, ci);
					ret.lits = atomIds;
					return ret;
				}
			} else {
				GClause ret = new GClause(c, null);
				ret.lits = atomIds;
				return ret;
			}
		}
		return null;
	}

	/**
	 * Return a map from ClauseInstance.getStrId() to ClauseInstance .
	 */
	public Map<String, ClauseInstance> getClauseInstanceIDMap() {
		HashMap<String, ClauseInstance> clauseIDMap =
			new HashMap<String, ClauseInstance>();
		for (Clause c : this.getAllNormalizedClauses()) {
			if (c.isTemplate()) {
				for (ClauseInstance ci : c.instances) {
					clauseIDMap.put(ci.getStrId(), ci);
				}
			}
		}
		return clauseIDMap;
	}

	/**
	 * Return a map from Clause.getStrId() to Clause.
	 */
	public Map<String, Clause> getClauseIDMap() {
		HashMap<String, Clause> clauseIDMap = new HashMap<String, Clause>();
		for (Clause c : this.getAllNormalizedClauses()) {
			if (!c.isTemplate()) {
				clauseIDMap.put(c.getStrId(), c);
			}
		}
		return clauseIDMap;
	}

	public HashMap<String, Double> getWeights() {
		HashMap<String, Double> weightMap = new HashMap<String, Double>();
		for (Clause c : this.getAllNormalizedClauses()) {
			if (c.isTemplate()) {
				for (ClauseInstance ci : c.instances) {
					if (ci.isHardClause()) {
						continue;
					}
					weightMap.put(ci.getStrId(), ci.weight);
				}
			} else {
				if (c.isHardClause()) {
					continue;
				}
				weightMap.put(c.getStrId(), c.weight);
			}
		}
		return weightMap;
	}

}

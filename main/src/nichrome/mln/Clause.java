package nichrome.mln;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nichrome.mln.db.RDB;
import nichrome.mln.db.SQLMan;
import nichrome.mln.parser.CommandOptions;
import nichrome.mln.ra.Expression;
import nichrome.mln.util.Config;
import nichrome.mln.util.ExceptionMan;
import nichrome.mln.util.StringMan;
import nichrome.mln.util.UIMan;

/**
 * A first-order logic clause, namely a disjunct of literals. Adapted from the
 * same class in Tuffy.
 *
 * @author xin zhang
 */
public class Clause implements Cloneable {

	@Override
	@SuppressWarnings("unchecked")
	public Clause clone() {
		Clause ret = new Clause();

		ret.bilits = (ArrayList<Literal>) this.bilits.clone();

		ret.constraints = new ArrayList<Expression>();
		for (Expression sub : this.constraints) {
			ret.constraints.add(sub.clone());
		}

		ret.cost = this.cost;
		ret.existentialVars = (ArrayList<String>) this.existentialVars.clone();
		ret.exprWeight = this.exprWeight;
		ret.id = this.id;
		ret.instances = (ArrayList<ClauseInstance>) this.instances.clone();
		for (ClauseInstance ci : ret.instances) {
			ci.parent = ret;
		}
		ret.isTemplate = this.isTemplate;
		ret.lits = (ArrayList<Literal>) this.lits.clone();
		ret.metaTypes = (ArrayList<Type>) this.metaTypes.clone();
		ret.metaVars = (ArrayList<String>) this.metaVars.clone();
		ret.name = this.name;
		ret.predIndex =
			(HashMap<Predicate, ArrayList<Literal>>) this.predIndex.clone();
		ret.reglits = (ArrayList<Literal>) this.reglits.clone();
		ret.relIntanceClauses = this.relIntanceClauses;
		ret.signature = this.signature;
		ret.specText = (ArrayList<String>) this.specText.clone();
		ret.evalSql = this.evalSql;
		ret.checkVioSql = this.checkVioSql;
		ret.uNames = (ArrayList<String>) this.uNames.clone();
		ret.varWeight = this.varWeight;
		ret.weight = this.weight;

		return ret;
	}

	public Literal variableWeights = null;

	public boolean isFixedWeight = true;

	/**
	 * Map from clause ID to its description. This is used in learning part to
	 * dump out the answers. Here by id it means a string like <Clause
	 * ID>.<Instance ID or 0 if not a template>. This variable is materialized
	 * in {@link Infer#setUp(CommandOptions)}.
	 */
	public static HashMap<String, String> mappingFromID2Desc = null;
	// new HashMap<String, String>();

	/**
	 * Map from Constant ID to Constant Name. This map is filled in
	 * {@link MarkovLogicNetwork#getSymbolID(String, Type)}. This variable is
	 * materialized in {@link Infer#setUp(CommandOptions)}.
	 */
	public static HashMap<Integer, String> mappingFromID2Const = null;
	// new HashMap<Integer, String>();

	/**
	 * The set of boolean expressions that must all be TRUE; otherwise the
	 * corresponding grounding is always true, and is useless for inference --
	 * and will be discarded.
	 *
	 * In other words, this is the set of constraints that must be satisfied by
	 * the grounding process.
	 */
	protected ArrayList<Expression> constraints = new ArrayList<Expression>();

	/**
	 * Add a constraint that must hold.
	 *
	 * @param e
	 *            A bool expression that must be TRUE.
	 */
	public void addConstraint(Expression e) {
		this.constraints.add(e);
	}

	public ArrayList<Expression> getConstraints() {
		return this.constraints;
	}

	protected String getConstaintStringAsLits() {
		if (this.constraints.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		ArrayList<String> clines = new ArrayList<String>();
		for (Expression e : this.constraints) {
			clines.add(" v [" + Expression.not(e).toString() + "]");
		}
		sb.append(StringMan.join("\n", clines));
		return sb.toString();
	}

	/**
	 * List of literals in this clause.
	 */
	protected ArrayList<Literal> lits = new ArrayList<Literal>();

	/**
	 * List of regular literals in this clause.
	 */
	protected ArrayList<Literal> reglits = new ArrayList<Literal>(); // regular

	/**
	 * List of built-in literals in this clause.
	 */
	protected ArrayList<Literal> bilits = new ArrayList<Literal>(); // built in

	/**
	 * The index of predicate to set of literals referencing that predicate.
	 */
	protected HashMap<Predicate, ArrayList<Literal>> predIndex =
		new HashMap<Predicate, ArrayList<Literal>>();

	/**
	 * List of variables that are existentially quantified.
	 */
	protected ArrayList<String> existentialVars = new ArrayList<String>();

	/**
	 * Variables corresponding to non-constants in this clause. This list is
	 * only filled after normalize()
	 */
	protected ArrayList<String> vars = new ArrayList<String>();

	/**
	 * Types of vars. This is list is only filled after normalize()
	 */
	protected ArrayList<Type> types = new ArrayList<Type>();

	/**
	 * Variables corresponding to constants in this clause.
	 */
	protected ArrayList<String> metaVars = new ArrayList<String>();

	/**
	 * Types of meta variables.
	 */
	protected ArrayList<Type> metaTypes = new ArrayList<Type>();

	/**
	 * List of instances of this clause. Here by instance, we mean the possible
	 * bindings of meta-variables to constants.
	 */
	public ArrayList<ClauseInstance> instances =
		new ArrayList<ClauseInstance>();

	/**
	 * weight of this clause.
	 */
	protected double weight = 0;

	/**
	 * name of this clause.
	 */
	protected String name = null;

	/**
	 * id of this clause.
	 */
	protected int id = 0;

	/**
	 * user provided names
	 */
	protected ArrayList<String> uNames = new ArrayList<String>();

	/**
	 * Lines in the MLN rule file specifying this clause.
	 */
	protected ArrayList<String> specText = new ArrayList<String>();

	protected String getSpecTextFlat() {
		return StringMan.join("\n", this.specText);
	}

	/**
	 * The database table storing the clause instances.
	 */
	protected String relIntanceClauses = null;

	/**
	 * Indicates whether this clause contains constants.
	 */
	protected boolean isTemplate = false;

	/**
	 * The signature of this clause. Clauses with the same signature have the
	 * same pattern, and thus can be consolidated. See
	 * {@link Clause#normalize()}.
	 */
	protected String signature = null;

	/**
	 * The SQL statement of checking the violated grounded rules.
	 */
	public String checkVioSql = null;
	
	/**
	 * The SQL statement for satisfied grounded rules.
	 */
	public String satSql = null;

	/**
	 * The SQL statement of evaluating the MLN based on current rule.
	 */
	public String evalSql = null;

	/**
	 * Get the number of violated rules. It should be faster than finding
	 * satisfied rules
	 */
	public String revEvalSql = null;

	public PreparedStatement checkVioSt = null;
	
	public PreparedStatement satSt = null;

	public PreparedStatement evalSt = null;

	public PreparedStatement revEvalSt = null;

	/**
	 * The cost ascribed to this clause. For auditing purposes.
	 *
	 * @see tuffy.infer.MRF#auditClauseViolations()
	 */
	public double cost = 0;

	/**
	 * FO variable that is used as clause weights
	 */
	protected String varWeight = null;

	public void setVarWeight(String vw) {
		this.varWeight = vw;
	}

	/**
	 * Get the variable in this clause that is used as clause weights
	 */
	public String getVarWeight() {
		return this.varWeight;
	}

	/**
	 * Check if the weight of this clause comes from a variable in the clause
	 */
	public boolean hasEmbeddedWeight() {
		return (this.varWeight != null);
	}

	/**
	 * Return the weight of this clause. If this clause contains multiple
	 * instances, the returned value only indicates the signum.
	 */
	public double getWeight() {
		return this.weight;
	}

	/**
	 * Return true iff this clause contains constant. Note that the result of
	 * this function is meaningful iff this clause is a normalized clause.
	 */
	public boolean isTemplate() {
		return this.isTemplate;
	}

	/**
	 * Add user provided names to this clause.
	 *
	 * @param nm
	 *            user provided name
	 */
	public void addUserProvidedName(String nm) {
		if (nm != null) {
			this.uNames.add(nm);
		}
	}

	/**
	 * Class of an instance of a clause.
	 */
	public class ClauseInstance {
		/**
		 * The template clause of current instance
		 */
		public Clause parent = null;

		/**
		 * list of constant ID in this clause instance.
		 */
		public ArrayList<Term> conList;

		public boolean isFixedWeight = true;

		/**
		 * weight of this clause instance.
		 */
		public double weight;

		public String checkVioSql = null;
		
		/**
		 * get the satisfied rules
		 */
		public String satSql = null;
		
		/**
		 * get the number of satisfied rules
		 */
		public String evalSql = null;
		/**
		 * get the number of violated rules. It should be faster than finding
		 * satisfied rules
		 */
		public String revEvalSql = null;

		public PreparedStatement checkVioSt = null;
		public PreparedStatement satSt = null;
		public PreparedStatement evalSt = null;
		public PreparedStatement revEvalSt = null;

		/**
		 * Constructor of ClauseInstance.
		 *
		 * @param conList
		 *            list of constant in this clause instance.
		 * @param weight
		 *            weight of this clause instance.
		 */
		public ClauseInstance(Clause p, ArrayList<Term> conList, double weight,
			boolean isFixedWeight) {
			this.parent = p;
			this.conList = conList;
			this.weight = weight;
			this.isFixedWeight = isFixedWeight;
		}

		/**
		 * Check if the weight is positive.
		 */
		public boolean isPositiveClause() {
			return this.weight > 0;
		}
		
		public boolean isHardClause() {
			return this.weight >= Config.hard_weight;
		}

		public int getId() {
			return this.parent.instances.indexOf(this);
		}

		public String getStrId() {
			return this.parent.id + "." + (this.parent.instances.indexOf(this) + 1);
		}

	}

	/**
	 * Return the assigned name of this clause.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Assign a name for this clause.
	 */
	public void setName(String aname) {
		this.name = aname;
		this.relIntanceClauses = this.name + "_instances";
	}

	/**
	 * Return the "signature" of this clause.
	 *
	 * @see Clause#normalize()
	 */
	public String getSignature() {
		return this.signature;
	}

	/**
	 * Return a normalized version of this clause.
	 *
	 * The variables and constants are replaced standardized variable names,
	 * yielding a signature that can be used to identify clauses of the same
	 * pattern. If there are constants in the original clause, the resulting
	 * clause is called a template. Clauses of the same pattern will be
	 * consolidated under the same template.
	 *
	 * For example, clauses "!likes(x, Candy) v has(x, Diabetes)" and
	 * "!likes(x, WeightLifting) v has(x, Muscles)" would be consolidated into
	 * the template "!likes(v1, c1) v has(v1, c2)".
	 *
	 * Zero-weight clauses will be ignored.
	 *
	 * @see MarkovLogicNetwork#registerClause(Clause)
	 */
	public Clause normalize() {
		HashMap<String, Integer> varIndex = new HashMap<String, Integer>();
		HashMap<String, Integer> conIndex = new HashMap<String, Integer>();
		ArrayList<Term> conList = new ArrayList<Term>();
		ArrayList<Type> conTypeList = new ArrayList<Type>();
		ArrayList<Type> varTypeList = new ArrayList<Type>();

		if (this.weight == 0) {
			return null;
		}

		// normalization
		// order into {enlits, eplits, unlits, uplits}.
		if (Config.reorder_literals) {
			ArrayList<Literal> enlits = new ArrayList<Literal>(); // closed, neg
			ArrayList<Literal> eplits = new ArrayList<Literal>(); // closed, pos
			ArrayList<Literal> unlits = new ArrayList<Literal>(); // open, neg
			ArrayList<Literal> uplits = new ArrayList<Literal>(); // open, pos
			for (Literal lit : this.lits) {
				Predicate p = lit.getPred();
				if (p.isClosedWorld()) {
					if (lit.getSense()) {
						eplits.add(lit);
					} else {
						enlits.add(lit);
					}
				} else {
					if (lit.getSense()) {
						uplits.add(lit);
					} else {
						unlits.add(lit);
					}
				}
			}
			enlits.addAll(eplits);
			enlits.addAll(unlits);
			enlits.addAll(uplits);
			this.lits = enlits;
		}

		// TODO: cleaner object cloning
		ArrayList<String> litlist = new ArrayList<String>();
		for (Literal lit : this.lits) {
			StringBuilder sb = new StringBuilder();
			if (!lit.getSense()) {
				sb.append("!");
			} else {
				sb.append(" ");
			}
			sb.append(lit.getPred().getName());
			sb.append("(");
			ArrayList<String> tlist = new ArrayList<String>();
			for (int i = 0; i < lit.getTerms().size(); i++) {
				Term t = lit.getTerms().get(i);
				if (t.isVariable()) {
					Integer vi = varIndex.get(t.var());
					if (vi == null) {
						vi = varIndex.size();
						varIndex.put(t.var(), vi);
						varTypeList.add(lit.getPred().getTypeAt(i));
					}
					tlist.add("v" + vi);
				} else {
					Integer ci = conIndex.get(t.constantString());
					if (ci == null) {
						ci = conIndex.size();
						conIndex.put(t.constantString(), ci);
						conList.add(t);
						conTypeList.add(lit.getPred().getTypeAt(i));
					}
					tlist.add("c" + ci);
				}
			}
			sb.append(StringMan.commaList(tlist));
			sb.append(")");
			litlist.add(sb.toString());
		}

		StringBuilder sigb = new StringBuilder();
		sigb.append(this.weight > 0 ? "sign='+' " : "sign='-' ");
		if (!this.existentialVars.isEmpty()) {
			ArrayList<String> evlist = new ArrayList<String>();
			for (String ev : this.existentialVars) {
				evlist.add("v" + varIndex.get(ev));
			}
			if (Config.clause_display_multiline) {
				sigb.append("\n  ");
			}
			sigb.append("EXIST " + StringMan.join(",", evlist) + " ");
		}
		if (Config.clause_display_multiline) {
			sigb.append("\n   ");
		}
		sigb.append(StringMan.join(
			(Config.clause_display_multiline ? "\n" : "") + " v ", litlist));

		HashMap<String, String> mapVarVar = new HashMap<String, String>();
		for (String v : varIndex.keySet()) {
			mapVarVar.put(v, "v" + varIndex.get(v));
		}
		for (Expression e : this.constraints) {
			// if(e.changeName == true){
			String es = e.renameVariables(mapVarVar);
			if (es != null) {
				ExceptionMan.die("Encountered a dangling variable '" + es
					+ "' in clause\n" + this.getSpecTextFlat());
			}
			// }
		}
		sigb.append("\n");
		sigb.append(this.getConstaintStringAsLits());
		String sig = sigb.toString();

		// generate normalized clause
		Clause c = new Clause();
		c.signature = sig;
		c.uNames = this.uNames;
		c.specText = this.specText;
		c.constraints = this.constraints;
		this.signature = sig;
		for (Literal lit : this.lits) {
			Literal nlit = new Literal(lit.getPred(), lit.getSense());
			for (Term term : lit.getTerms()) {
				Term nterm;
				if (term.isVariable()) {
					nterm = new Term("v" + varIndex.get(term.var()));
				} else {
					nterm = new Term("c" + conIndex.get(term.constantString()));
				}
				nlit.appendTerm(nterm);
			}
			c.addLiteral(nlit);
		}
		for (String ev : this.existentialVars) {
			c.addExistentialVariable("v" + varIndex.get(ev));
		}

		for (int i = 0; i < varTypeList.size(); i++) {
			c.addVariable("v" + i, varTypeList.get(i));
		}

		if (conTypeList.isEmpty()) {
			c.isTemplate = false;
			c.weight = this.weight;

			if (this.isFixedWeight == false) {
				c.isFixedWeight = false;
			}

		} else {
			c.isTemplate = true;
			c.weight = (this.weight > 0 ? 1 : -1);
			for (int i = 0; i < conTypeList.size(); i++) {
				c.addMetaVariable("c" + i, conTypeList.get(i));
			}

			c.instances.add(new ClauseInstance(c, conList, this.weight,
				this.isFixedWeight));
			if (this.isFixedWeight == false) {
				c.isFixedWeight = false;
			}
		}
		return c;
	}

	/**
	 * "Absorb" another clause of the same pattern into this clause. If this
	 * clause is a template, then adding instances into the instance list.
	 * Otherwise, add its weight to current clause.
	 *
	 * @param c
	 *            the clause to be absorbed
	 * @see Clause#normalize()
	 */
	public void absorb(Clause c) {
		if (!this.signature.equals(c.signature)) {
			ExceptionMan
				.die("clauses of different patterns cannot be consolidated!");
		}
		if (this.isTemplate) {
			this.instances.addAll(c.instances);
			for (ClauseInstance ci : c.instances) {
				ci.parent = this;
			}
		} else {
			this.weight += c.weight;
			this.specText.addAll(c.specText);
		}
		this.uNames.addAll(c.uNames);
	}

	/**
	 * Add a meta variable into this clause.
	 *
	 * @param v
	 *            name this this meta variable
	 * @param t
	 *            type of this meta variable
	 * @see Clause#normalize()
	 * @return whether this inserting succeeded.
	 */
	protected boolean addMetaVariable(String v, Type t) {
		if (this.metaVars.contains(v)) {
			return false;
		}
		this.metaVars.add(v);
		this.metaTypes.add(t);
		return true;
	}

	protected boolean addVariable(String v, Type t) {
		if (this.vars.contains(v)) {
			return false;
		}
		this.vars.add(v);
		this.types.add(t);
		return true;
	}

	/**
	 * Existentially quantify a variable.
	 *
	 * @param v
	 *            the variable to be existentially quantified
	 */
	public boolean addExistentialVariable(String v) {
		if (true) {
			throw new RuntimeException(
				"Existential quantifiers are not supported yet.");
		}
		if (this.existentialVars.contains(v)) {
			return false;
		}
		this.existentialVars.add(v);
		return true;
	}

	/**
	 * Construct an empty clause. Initial weight = 0.
	 *
	 */
	public Clause() {
		this.weight = 0;
	}

	/**
	 * Specify this clause as a hard rule. Currently hard rules are treated as
	 * soft rules with a very large weight.
	 *
	 * @see Config#hard_weight
	 */
	public void setHardWeight() {
		this.weight = Config.hard_weight;
	}

	/**
	 * Return whether this clause is a hard rule.
	 */
	public boolean isHardClause() {
		return this.weight >= Config.hard_weight;
	}

	/**
	 * Set the weight of this clause.
	 */
	public void setWeight(double wt) {
		if (wt > Config.hard_weight) {
			UIMan
				.println("WARNING: attempting to set clause with a weight higher than hard weight!");
		}
		this.weight = wt;
	}

	/**
	 * Return the expression of clause weights to be used in SQL. For template
	 * clauses, it's the name of a table attribute; for non-template clauses,
	 * it's a floating number.
	 */
	public String getWeightExp() {
		return this.exprWeight;
	}

	/**
	 * Check if the weight is positive.
	 */
	public boolean isPositiveClause() {
		return this.weight > 0;
	}

	/**
	 * Initialize database objects for this clause.
	 */
	public void prepareForDB(RDB db) {
		this.generateSQL();
		if (!Config.enable_concurrency) {
			if (this.isTemplate()) {
				for (ClauseInstance ci : this.instances) {
					ci.checkVioSt = db.getPrepareStatement(ci.checkVioSql);
					ci.satSt = db.getPrepareStatement(ci.satSql);
					ci.evalSt = db.getPrepareStatement(ci.evalSql);
					ci.revEvalSt = db.getPrepareStatement(ci.revEvalSql);
				}
			} else {
				this.checkVioSt = db.getPrepareStatement(this.checkVioSql);
				this.satSt = db.getPrepareStatement(this.satSql);
				this.evalSt = db.getPrepareStatement(this.evalSql);
				this.revEvalSt = db.getPrepareStatement(this.revEvalSql);
			}
		} else {
			// //Concurrent version, use a standalone connection
			Connection con = db.createConnection();
			if (this.isTemplate()) {
				for (ClauseInstance ci : this.instances) {
					ci.checkVioSt = db.getPrepareStatement(ci.checkVioSql, con);
					ci.satSt = db.getPrepareStatement(ci.satSql, con);
					ci.evalSt = db.getPrepareStatement(ci.evalSql, con);
					ci.revEvalSt = db.getPrepareStatement(ci.revEvalSql, con);
				}
			} else {
				this.checkVioSt = db.getPrepareStatement(this.checkVioSql, con);
				this.satSt = db.getPrepareStatement(this.satSql, con);
				this.evalSt = db.getPrepareStatement(this.evalSql, con);
				this.revEvalSt = db.getPrepareStatement(this.revEvalSql, con);
			}
		}
	}

	/**
	 * Check for unsafe variables in the clause, and mark the corresponding
	 * Predicates. A variable is unsafe if it appears only in a positive
	 * literal; i.e., if it does not appear in the body in the Datalog form.
	 */
	public void checkVariableSafety() {
		HashSet<String> vars = new HashSet<String>();
		HashSet<String> safeVars = new HashSet<String>();
		ArrayList<Literal> posLits = new ArrayList<Literal>();
		for (Literal lit : this.reglits) {
			vars.addAll(lit.getVars());
			if (!lit.getSense()) {
				safeVars.addAll(lit.getVars());
			} else {
				posLits.add(lit);
			}
		}
		for (Literal lit : posLits) {
			for (Term t : lit.getTerms()) {
				if (t.isVariable()) {
					if (!safeVars.contains(t.var())) {
						lit.getPred().setSafeRefOnly(false);
						// TODO: note that order of positive lits is important
						safeVars.add(t.var());
					}
				}
			}
		}
	}

	protected String exprWeight = null;

	public boolean isFullyGrounded() {
		return this.vars.size() == 0;
	}

	/**
	 * Generate the SQL statements for checking violated ground clauses and
	 * evaluating MLN. Ideally this part should be separated from the Clause
	 * structure. However I followed Tuffy's style.
	 */
	public void generateSQL() {
		UIMan.verbose(1, "Generateing SQL queries for:\n" + this);
		this.generateVioSQL();
		this.generateSatSQL();
		this.generateEvalSQL();
		this.generateRevEvalSQL();
	}

	private Map<String, String> genVarPredMap() {
		Map<String, String> ret = new HashMap<String, String>();
		for (Literal lit : this.lits) {
			if (!lit.getSense()) {
				List<Term> terms = lit.getTerms();
				for (int i = 0; i < terms.size(); i++) {
					String varName = terms.get(i).var();
					if (this.metaVars.contains(varName)) {
						continue;
					}
					if (ret.containsKey(varName)) {
						continue;
					} else {
						Predicate p = lit.getPred();
						ret.put(varName, this.renameLiteralTableName(lit) + "."
							+ p.getArgs().get(i));
					}
				}
			}
		}
		for (int i = 0; i < this.vars.size(); i++) {
			String varName = this.vars.get(i);
			if (!ret.containsKey(varName)) {
				ret.put(varName, this.renameTypeTable(varName, this.types
					.get(i))
					+ "." + Config.CONSTANT_ID);
			}
		}
		return ret;
	}

	private List<Literal> getPositiveLits() {
		List<Literal> ret = new ArrayList<Literal>();
		for (Literal lit : this.lits) {
			if (lit.getSense()) {
				ret.add(lit);
			}
		}
		return ret;
	}

	private List<Literal> getNegativeLits() {
		List<Literal> ret = new ArrayList<Literal>();
		for (Literal lit : this.lits) {
			if (!lit.getSense()) {
				ret.add(lit);
			}
		}
		return ret;
	}

	/**
	 * Rename the rel name of literal based on its index in the clause.
	 *
	 * @param l
	 * @return
	 */
	public String renameLiteralTableName(Literal l) {
		return "p_" + this.lits.indexOf(l);
	}

	/**
	 * Rename the rel name of the type based on the correspondent var
	 *
	 * @param var
	 * @param t
	 * @return
	 */
	public String renameTypeTable(String var, Type t) {
		return "d_" + var;
	}

	/**
	 * SQL statement for violated grounded clauses
	 */
	private void generateVioSQL() {
		if (this.isTemplate()) {
			for (ClauseInstance ci : this.instances) {
				ci.checkVioSql = this.generateVioSQL(ci);
				UIMan.verbose(1, "violation sql: " + ci.checkVioSql);
			}
		} else {
			this.checkVioSql = this.generateVioSQL(null);
			UIMan.verbose(1, "violation sql: " + this.checkVioSql);
		}
	}
	
	private String generateVioSQL(ClauseInstance ci) {
		Map<String, String> varPredMap = this.genVarPredMap();
		List<Literal> pLits = this.getPositiveLits();
		List<Literal> nLits = this.getNegativeLits();

		StringBuffer sb = new StringBuffer("");
		// The select part: select all the variable from their domain table
		// (including both constant and variables)
		sb.append("SELECT DISTINCT ");
		// sb.append("SELECT ");
		// vars, use one positive literal containing var for its table
		ArrayList<String> parts = new ArrayList<String>();
		for (int i = 0; i < this.vars.size(); i++) {
			String var = this.vars.get(i);
			String part = varPredMap.get(var) + " as " + var;
			parts.add(part);
		}
		if (parts.size() == 0) {
			parts.add("*");
		}

		sb.append(StringMan.commaList(parts));
		// The from part: domain table+predicate table
		sb.append(" FROM ");
		parts = new ArrayList<String>();
		// look at the negative predicates, as in violation check, we take their
		// negations.
		for (Literal l : nLits) {
			parts.add(l.getPred().getRelName() + " AS "
				+ this.renameLiteralTableName(l));
		}

		for (Map.Entry<String, String> entry : varPredMap.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (value.startsWith("d_")) {
				String[] splits = value.split("\\.");
				String typeTable =
					this.types.get(this.vars.indexOf(key)).getRelName();
				parts.add(typeTable + " AS " + splits[0]);
			}
		}

		if (parts.size() == 0) {
			parts.add(Config.relConstants);
		}
		sb.append(StringMan.commaList(parts));
		// The where part
		sb.append(" WHERE 1=1 ");
		// about negative predicate. Its negation is positive
		parts = new ArrayList<String>();
		for (Literal lit : nLits) {
			ArrayList<String> innerParts = new ArrayList<String>();
			List<Term> terms = lit.getTerms();
			Predicate cp = lit.getPred();
			String litTableName = this.renameLiteralTableName(lit);
			for (int i = 0; i < terms.size(); i++) {
				String lhs;
				lhs = litTableName + "." + cp.getArgs().get(i);
				Term t = terms.get(i);
				String var = t.var();
				String rhs = varPredMap.get(var);
				if (rhs == null) {
					if (!this.metaVars.contains(var)) {
						throw new RuntimeException(
							"Strange, a variable should be already handled: "
								+ var);
					}
					rhs =
						""
							+ ci.conList.get(this.metaVars.indexOf(var))
								.constant();
				}
				if (lhs.equals(rhs)) {
					continue;
				}
				innerParts.add(lhs + " = " + rhs);
			}
			if (innerParts.size() == 0) {
				continue;
			}
			parts.add(SQLMan.andSelCond(innerParts));
		}

		// about positive predicate
		for (Literal lit : pLits) {
			Predicate cp = lit.getPred();
			StringBuffer isb =
				new StringBuffer("NOT EXISTS (SELECT * FROM " + cp.getRelName());
			ArrayList<String> innerParts = new ArrayList<String>();
			List<Term> terms = lit.getTerms();
			for (int i = 0; i < terms.size(); i++) {
				String lhs;
				lhs = cp.getRelName() + "." + cp.getArgs().get(i);
				Term t = terms.get(i);
				String var = t.var();
				String rhs = varPredMap.get(var);
				if (rhs == null) {
					if (!this.metaVars.contains(var)) {
						throw new RuntimeException(
							"Strange, a variable should be already handled: "
								+ var);
					}
					rhs =
						""
							+ ci.conList.get(this.metaVars.indexOf(var))
								.constant();
				}
				if (lhs.equals(rhs)) {
					continue;
				}
				innerParts.add(lhs + " = " + rhs);
			}
			if (innerParts.size() > 0) {
				isb.append(" WHERE " + SQLMan.andSelCond(innerParts));
			}
			isb.append(")");
			parts.add(isb.toString());
		}
		if (parts.size() != 0) {
			sb.append(" AND ");
			sb.append("(");
			sb.append(SQLMan.andSelCond(parts));
			sb.append(")");

		}
		// The expression
		for (Expression cons : this.getConstraints()) {
			Map<String, String> varMap = new HashMap<String, String>();
			for (String var : cons.getVars()) {
				String varPredCol = varPredMap.get(var);
				varMap.put(var, "SELECT string FROM " + Config.relConstants
					+ " WHERE id = " + varPredCol);
			}
			cons.bindVariables(varMap);
			sb.append(" AND " + cons.toSQL());
		}
		// When the clause is already fully grounded
		if (this.vars.size() == 0) {
			sb.append(" LIMIT 1");
		}
		return sb.toString();
	}

	/**
	 * SQL statement for satisfied grounded clauses
	 */
	private void generateSatSQL() {
		if (this.isTemplate()) {
			for (ClauseInstance ci : this.instances) {
				ci.satSql = this.generateSatSQL(ci);
				UIMan.verbose(1, "sat sql: " + ci.satSql);
			}
		} else {
			this.satSql = this.generateSatSQL(null);
			UIMan.verbose(1, "sat sql: " + this.satSql);
		}
	}

	/**
	 * Get the num of all possible grounded rules. 
	 * TODO: I forgot to consider the expressions like a<b in the clause.
	 * It needs to be fixed. Also, groundings that are trivially satisified
	 * by the evidence should be ignored.
	 */
	private String generateSatSQL(ClauseInstance ci) {
		StringBuffer sb = new StringBuffer("");
		// The select part: select all the variable from their domain table
		// (including both constant and variables)
		// sb.append("SELECT DISTINCT ");
		sb.append("SELECT ");
		// vars
		ArrayList<String> parts = new ArrayList<String>();
		for (int i = 0; i < this.vars.size(); i++) {
			String var = this.vars.get(i);
			String part = var + "_table." + Config.CONSTANT_ID + " as " + var;
			parts.add(part);
			// part = var + "_table."+Config.CONSTANT_VALUE+" as "+var+"_v";
			// parts.add(part);
		}
		// constants
		for (int i = 0; i < this.metaVars.size(); i++) {
			String mvar = this.metaVars.get(i);
			String part = mvar + "_table." + Config.CONSTANT_ID + " as " + mvar;
			parts.add(part);
			// part = mvar+"_table."+Config.CONSTANT_VALUE+" as"+mvar+"_v";
			// parts.add(part);
		}
		sb.append(StringMan.commaList(parts));
		// The from part: domain table+predicate table
		sb.append(" FROM ");
		parts = new ArrayList<String>();
		for (int i = 0; i < this.types.size(); i++) {
			parts.add(this.types.get(i).getRelName() + " as "
				+ this.vars.get(i) + "_table");
		}
		for (int i = 0; i < this.metaTypes.size(); i++) {
			parts.add(this.metaTypes.get(i).getRelName() + " as "
				+ this.metaVars.get(i) + "_table");
		}
		sb.append(StringMan.commaList(parts));
		// The where part
		// About predicate
		sb.append(" WHERE 1=1 ");
		parts = new ArrayList<String>();
		for (Literal lit : this.lits) {
			StringBuffer sbLit = new StringBuffer("");
			if (!lit.getSense()) {
				sbLit.append("NOT ");
			}
			sbLit.append("EXISTS ( ");
			Predicate p = lit.getPred();
			sbLit.append("SELECT * FROM " + p.getRelName());
			sbLit.append(" WHERE ");
			ArrayList<String> litParts = new ArrayList<String>();
			List<Term> terms = lit.getTerms();
			for (int i = 0; i < terms.size(); i++) {
				Term t = terms.get(i);
				litParts.add(p.getRelName() + "." + p.getArgs().get(i) + "="
					+ t.var() + "_table.constantID");
			}
			sbLit.append(SQLMan.andSelCond(litParts));
			sbLit.append(")");
			parts.add(sbLit.toString());
		}
		if (parts.size() != 0) {
			sb.append(" AND ");
			sb.append("(");
			sb.append(SQLMan.orSelCond(parts));
			sb.append(")");

		}
		// About constants
		parts = new ArrayList<String>();
		if (ci != null) {
			for (int i = 0; i < this.metaVars.size(); i++) {
				parts.add(this.metaVars.get(i) + "_table." + Config.CONSTANT_ID
					+ "=" + ci.conList.get(i).constant());
			}
		}
		if (parts.size() != 0) {
			sb.append(" AND ");
			sb.append("(");
			sb.append(SQLMan.andSelCond(parts));
			sb.append(")");
		}
		// The expression
		for (Expression cons : this.getConstraints()) {
			Map<String, String> varMap = new HashMap<String, String>();
			for (String var : cons.getVars()) {
				varMap.put(var, var + "_table." + Config.CONSTANT_VALUE);
			}
			cons.bindVariables(varMap);
			sb.append(" OR NOT " + cons.toSQL());
		}
		return sb.toString();
	}
	
	/**
	 * SQL statement for count of satisfied grounded clauses
	 */
	private void generateEvalSQL() {
		if (this.isTemplate()) {
			for (ClauseInstance ci : this.instances) {
				if (ci.satSql == null) {
					this.generateSatSQL();
				}
				ci.evalSql =
					"SELECT COUNT(*) as num FROM (" + ci.satSql
						+ ") as temp_temp_x89";
				UIMan.verbose(1, "evaluation sql: " + ci.evalSql);
			}
		} else {
			if (this.satSql == null) {
				this.generateSatSQL();
			}
			this.evalSql =
				"SELECT COUNT(*) as num FROM (" + this.satSql
					+ ") as temp_temp_x89";
			UIMan.verbose(1, "evaluation sql: " + this.evalSql);
		}
	}

	private void generateRevEvalSQL() {
		if (this.isTemplate()) {
			for (ClauseInstance ci : this.instances) {
				if (ci.checkVioSql == null) {
					this.generateVioSQL();
				}
				ci.revEvalSql =
					"SELECT COUNT(*) as num FROM (" + ci.checkVioSql
						+ ") as temp_temp_x89";
				UIMan.verbose(1, "reverse evaluation sql: " + ci.revEvalSql);
			}
		} else {
			if (this.checkVioSql == null) {
				this.generateVioSQL();
			}
			this.revEvalSql =
				"SELECT COUNT(*) as num FROM (" + this.checkVioSql
					+ ") as temp_temp_x89";
			UIMan.verbose(1, "reverse evaluation sql: " + this.revEvalSql);
		}
	}

	/**
	 * Get the num of all possible grounded rules. 
	 * TODO: I forgot to consider the expressions like a<b in the clause.
	 * It needs to be fixed. Also, groundings that are trivially satisified
	 * by the evidence should be ignored.
	 *
	 * @return
	 */
	public double getFullyGroundSize() {
		double i = 1;
		for (Type t : this.types) {
			i *= t.size();
		}
		return i;
	}

	/**
	 * Return the definition of this clause.
	 */
	@Override
	public String toString() {
		String s = (this.name == null ? "" : this.name);
		if (Config.clause_display_multiline) {
			s += "\n";
		}
		if (!this.uNames.isEmpty()) {
			s += "// " + StringMan.commaList(this.uNames) + "\n";
		}
		String w =
			(this.weight >= Config.hard_weight ? "infty" : "" + this.weight);
		s +=
			(this.isTemplate ? "[#instances=" + this.instances.size() + "]"
				: "[weight=" + w + "]");
		return s
			+ " "
			+ (this.signature == null ? this.getSpecTextFlat() : this.signature);
	}

	/**
	 * Return the definition of clause instance.
	 *
	 * @param ni
	 *            The ID of instance.
	 */
	public String toString(int ni) {
		String s = ""; // = (name == null ? "" : name);

		String tmps =
			(this.signature == null ? this.getSpecTextFlat() : this.signature);
		if (ni >= 0 && !Clause.mappingFromID2Const.isEmpty()) {
			for (int i = 0; i < this.instances.get(ni).conList.size(); i++) {
				tmps =
					tmps.replaceAll("c" + (i), "\""
						+ Clause.mappingFromID2Const.get(
							this.instances.get(ni).conList.get(i).constant())
							.replace("$", "\\$") + "\"");
			}
		}

		tmps = tmps.replaceAll("[\n|\r]", " ");
		tmps = tmps.replaceAll("sign=\'.\'", "");

		return s + " " + tmps;
	}

	public String toStringForFunctionClause(String signature, Double weight) {

		String[] clauses = signature.split("\\.");
		int clauseID = Integer.parseInt(clauses[0]);
		String[] vars = clauses[1].split(",");

		String s = "";

		if (this.hasEmbeddedWeight() == false) {
			return null;
		}

		ArrayList<String> tojoin = new ArrayList<String>();
		HashMap<String, String> var2const = new HashMap<String, String>();

		for (Literal l : this.reglits) {

			boolean isEmbeded = false;
			for (int k = 0; k < l.getTerms().size(); k++) {
				if (l.getTerms().get(k).var().equals(this.getVarWeight())) {
					isEmbeded = true;
				}
			}

			if (isEmbeded == false) {

			} else {

				for (int k = 0; k < l.getTerms().size(); k++) {
					if (vars[k].equals("%f")) {

					} else {
						String tmp =
							Clause.mappingFromID2Const.get(Integer
								.parseInt(vars[k]));
						tmp = tmp.replaceAll("\\$", "\\\\\\$");
						var2const.put(l.getTerms().get(k).var(), tmp);
					}
				}
			}
		}

		for (Literal l : this.reglits) {

			boolean isEmbeded = false;
			for (int k = 0; k < l.getTerms().size(); k++) {
				if (l.getTerms().get(k).var().equals(this.getVarWeight())) {
					isEmbeded = true;
				}
			}

			String str = (l.getSense() ? "" : "!") + l.getPred().getName();
			ArrayList<String> args = new ArrayList<String>();
			for (int k = 0; k < l.getTerms().size(); k++) {
				if (var2const.containsKey(l.getTerms().get(k).var())) {
					args.add("\""
						+ StringMan.escapeJavaString(var2const.get(l.getTerms()
							.get(k).var())) + "\"");
				} else {
					args.add(l.getTerms().get(k).var());
				}
			}

			if (isEmbeded == false) {
				tojoin.add(str + "(" + StringMan.join(", ", args) + ")");
			} else {
				continue;
			}

		}

		s = StringMan.join(" v ", tojoin);

		s = weight + " " + s;

		return s;
	}

	/**
	 * Return the member literals of a particular predicate.
	 */
	public ArrayList<Literal> getLiteralsOfPredicate(Predicate pred) {
		return this.predIndex.get(pred);
	}

	/**
	 * Return the set of predicates referenced by this clause.
	 */
	public Set<Predicate> getReferencedPredicates() {
		return this.predIndex.keySet();
	}

	/**
	 * Return the list of non-built-in literals (i.e., regular literals).
	 */
	public ArrayList<Literal> getRegLiterals() {
		return this.reglits;
	}

	/**
	 * Check if any variable in this clause is existentially quantified.
	 */
	public boolean hasExistentialQuantifiers() {
		return !this.existentialVars.isEmpty();
	}

	/**
	 * Add a literal to this clause.
	 */
	public void addLiteral(Literal lit) {
		if (lit == null) {
			return;
		}
		// update the predicate-literal index
		ArrayList<Literal> plits = this.predIndex.get(lit.getPred());
		if (plits == null) {
			plits = new ArrayList<Literal>();
			this.predIndex.put(lit.getPred(), plits);
		} else {
			for (Literal elit : plits) {
				// duplicate lit, ignore
				if (elit.isSameAs(lit)) {
					return;
				}
			}
		}
		if (lit.isBuiltIn()) {
			this.bilits.add(lit);
			lit.setIdx(1000000 + this.bilits.size());
		} else {
			this.reglits.add(lit);
			lit.setIdx(this.reglits.size());
		}
		this.lits.add(lit);
		plits.add(lit);

	}

	/**
	 * Set clause ID.
	 *
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Get clause ID.
	 */
	public int getId() {
		return this.id;
	}

	public String getStrId() {
		return this.id + ".0";
	}

	public void addSpecText(String s) {
		this.specText.add(s);
	}

	public ArrayList<String> getSpecText() {
		return this.specText;
	}

}

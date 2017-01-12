package nichrome.mln;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;

import org.postgresql.PGConnection;

import nichrome.mln.db.RDB;
import nichrome.mln.util.Config;
import nichrome.mln.util.ExceptionMan;

/**
 * A domain/type of constants; i.e., a subset of constants.
 */
public class Type {

	/**
	 * Built-in types
	 */
	public static Type Generic = new Type("_GENERIC");
	public static Type Float = new Type("_FLOAT");
	public static Type Integer = new Type("_INTEGER");
	public static Type String = new Type("_STRING");
	public static Type Bool = new Type("_BOOL");

	static {
		Type.Float.isNonSymbolicType = true;
		Type.Integer.isNonSymbolicType = true;
		Type.Bool.isNonSymbolicType = true;
		Type.Float.nonSymbolicType = Type.Float;
		Type.Integer.nonSymbolicType = Type.Integer;
		Type.Bool.nonSymbolicType = Type.Bool;
	}

	private boolean isNonSymbolicType = false;

	private Type nonSymbolicType = null;

	/**
	 * See if this type is non-symbolic. "Non-symbolic" means that the value of
	 * this type is directly stored in the predicate table, whereas values of a
	 * "symbolic" (default) type are represented by unique IDs as per the symbol
	 * table.
	 *
	 * @return
	 */
	public boolean isNonSymbolicType() {
		return this.isNonSymbolicType;
	}

	public Type getNonSymbolicType() {
		return this.nonSymbolicType;
	}

	public String getNonSymbolicTypeInSQL() {
		if (this.nonSymbolicType.name.equals("_FLOAT")) {
			return "float";
		} else if (this.nonSymbolicType.name.equals("_STRING")) {
			return "string";
		} else if (this.nonSymbolicType.name.equals("_INTEGER")) {
			return "integer";
		} else if (this.nonSymbolicType.name.equals("_BOOL")) {
			return "boolean";
		} else {
			return null;
		}
	}

	/**
	 * The domain of variable values. The members of a domain are named as
	 * integer.
	 */
	private HashSet<Integer> domain = new HashSet<Integer>();

	/**
	 * Name of this Type.
	 */
	public String name;

	/**
	 * Name of the relational table corresponding to this type. Here it is
	 * type_$name.
	 */
	private String relName;

	public boolean isProbArg = false;

	/**
	 * Constructor of Type.
	 *
	 * @param name
	 *            the name of this new type; it must be unique among all types
	 */
	public Type(String name) {
		this.name = name;
		this.relName = "type_" + name;

		if (name.endsWith("_")) {
			this.isNonSymbolicType = true;
			if (name.toLowerCase().startsWith("float")) {
				this.nonSymbolicType = Type.Float;
			} else if (name.toLowerCase().startsWith("double")) {
				this.nonSymbolicType = Type.Float;
			} else if (name.toLowerCase().startsWith("int")) {
				this.nonSymbolicType = Type.Integer;
			} else {
				this.isNonSymbolicType = false;
			}
		}

		if (name.endsWith("_p_")) {
			this.isProbArg = true;
		}

	}

	/**
	 * Return the name of the DB relational table of this type.
	 */
	public String getRelName() {
		return this.relName;
	}

	/**
	 * Store the list of constants in a DB table.
	 *
	 * @param db
	 */
	public void storeConstantList(RDB db, boolean... onlyNonEmptyDomain) {

		if (onlyNonEmptyDomain.length > 0 && onlyNonEmptyDomain[0] == true
			&& this.domain.size() == 0) {
			return;
		}

		String sql;

		// if(onlyNonEmptyDomain.length == 0){
		db.dropTable(this.relName);
		// String sql = "CREATE TEMPORARY TABLE " + relName +
		// "(constantID INTEGER)\n";
		// if(this.isNonSymbolicType)
		// sql = "CREATE TABLE " + relName +
		// "(constantID bigint, constantVALUE "+this.toBuiltInType()+")";
		// else
		sql =
			"CREATE TABLE " + this.relName + "(" + Config.CONSTANT_ID
				+ " bigint primary key, " + Config.CONSTANT_VALUE + " TEXT)";
		db.update(sql);
		// sql = "CREATE UNIQUE INDEX idx_"+relName
		// +" ON "+relName+"("+Config.CONSTANT_ID+")";
		// db.update(sql);
		// }

		/**
		 * Use the copy api of postgresql db. According to
		 * http://rostislav-matl.
		 * blogspot.com/2011/08/fast-inserts-to-postgresql-with-jdbc.html, it is
		 * much faster.
		 */
		BufferedWriter writer = null;
		File loadingFile =
			new File(Config.getLoadingDir(), "loading_type_" + this.name);
		try {
			writer =
				new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
					loadingFile), "UTF8"));
			for (int v : this.domain) {
				writer.append(v + "\n");
			}
			writer.close();

			FileInputStream in = new FileInputStream(loadingFile);
			PGConnection con = (PGConnection) db.getConnection();
			sql =
				"COPY " + this.relName + " (" + Config.CONSTANT_ID
					+ ") FROM STDIN ";
			con.getCopyAPI().copyIn(sql, in);
			in.close();

			// if(this.isNonSymbolicType)
			// sql = "UPDATE " + relName +
			// " SET constantVALUE = t1.string::"+this.toBuiltInType()+" FROM "
			// + Config.relConstants +
			// " t1 WHERE t1.id = constantID AND constantVALUE IS NULL";
			// else
			sql =
				"UPDATE " + this.relName + " SET " + Config.CONSTANT_VALUE
					+ " = t1.string FROM " + Config.relConstants
					+ " t1 WHERE t1.id = constantID AND constantVALUE IS NULL";
			db.execute(sql);

			// domain.clear();
		} catch (Exception e) {
			ExceptionMan.handle(e);
		}

		// db.analyze(relName);
	}

	/**
	 * Add a constant to this type.
	 *
	 * @param con
	 *            the constant to be added
	 */
	public void addConstant(int con) {
		this.domain.add(con);
	}

	public HashSet<Integer> getDomain() {
		return this.domain;
	}

	/**
	 * Return true if this type contains the constant x
	 */
	public boolean contains(int x) {
		return this.domain.contains(x);
	}

	/**
	 *
	 * Return the number of constants in this type domain.
	 *
	 */
	public int size() {
		// RDB db = RDB.getRDBbyConfig(Config.db_schema);
		// int a = (int) db.countTuples(this.relName);
		// // db.close();
		// return a;
		return this.domain.size();
	}

	public int size(RDB db) {
		int a = (int) db.countTuples(this.relName);
		return a;
	}

	/**
	 * Return the name of this type.
	 */
	public String name() {
		return this.name;
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
		Type other = (Type) obj;
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Type [name=" + this.name + "]";
	}

	/**
	 * Get the database built-in type for current type. Note currently the
	 * implementation is specified to postgresql.
	 *
	 * @return
	 */
	public String toBuiltInType() {
		if (!this.isNonSymbolicType) {
			throw new RuntimeException(this + " is a symbolical type!");
		}
		if (this.nonSymbolicType == Type.Integer) {
			return "bigint";
		}
		if (this.nonSymbolicType == Type.Float) {
			return "real";
		}
		if (this.nonSymbolicType == Type.Bool) {
			return "boolean";
		}
		throw new RuntimeException("No correspondent built-in type: " + this);
	}
}

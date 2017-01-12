package nichrome.mln.ra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nichrome.mln.Type;

/**
 * Bool, numberic, and string functions; user-defined functions.
 *
 * @author Feng Niu
 *
 */

public class Function {
	public static HashMap<String, Function> builtInMap =
		new HashMap<String, Function>();

	public static Function getBuiltInFunctionByName(String name) {
		return Function.builtInMap.get(name);
	}

	// atomic functions
	public static Function ConstantNumber = null;
	public static Function ConstantString = null;
	public static Function VariableBinding = null;

	static {
		Function.ConstantNumber = new Function("_constNum", Type.Float);
		Function.ConstantNumber.isBuiltIn_ = true;
		Function.ConstantNumber.addArgument(Type.Float);
		Function.builtInMap.put("_constNum", Function.ConstantNumber);

		Function.ConstantString = new Function("_constStr", Type.String);
		Function.ConstantString.isBuiltIn_ = true;
		Function.ConstantString.addArgument(Type.String);
		Function.builtInMap.put("_constStr", Function.ConstantString);

		Function.VariableBinding = new Function("_var", Type.Generic);
		Function.VariableBinding.isBuiltIn_ = true;
		Function.VariableBinding.addArgument(Type.Generic);
		Function.builtInMap.put("_var", Function.VariableBinding);
	}

	// boolean functions
	public static Function NOT = null;
	public static Function OR = null;
	public static Function AND = null;

	public static Function Eq = null;
	public static Function Neq = null;
	public static Function LessThan = null;
	public static Function LessThanEq = null;
	public static Function GreaterThan = null;
	public static Function GreaterThanEq = null;

	public static Function StrContains = null;
	public static Function StrStartsWith = null;
	public static Function StrEndsWith = null;

	static {
		Function f = null;

		// logical operators

		f = new Function("NOT", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Bool);
		Function.builtInMap.put("NOT", f);
		Function.builtInMap.put("!", f);
		Function.NOT = f;

		f = new Function("OR", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Bool);
		f.addArgument(Type.Bool);
		Function.builtInMap.put("OR", f);
		f.isOperator_ = true;
		Function.OR = f;

		f = new Function("AND", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Bool);
		f.addArgument(Type.Bool);
		Function.builtInMap.put("AND", f);
		f.isOperator_ = true;
		Function.AND = f;

		// numeric bool functions

		f = new Function("_eq", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Generic);
		f.addArgument(Type.Generic);
		Function.builtInMap.put("_eq", f);
		Function.builtInMap.put("=", f);
		f.isOperator_ = true;
		Function.Eq = f;

		f = new Function("_neq", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Generic);
		f.addArgument(Type.Generic);
		Function.builtInMap.put("_neq", f);
		Function.builtInMap.put("!=", f);
		Function.builtInMap.put("<>", f);
		f.isOperator_ = true;
		Function.Neq = f;

		f = new Function("_less", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		Function.builtInMap.put("_less", f);
		Function.builtInMap.put("<", f);
		f.isOperator_ = true;
		Function.LessThan = f;

		f = new Function("_lessEq", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		Function.builtInMap.put("_lessEq", f);
		Function.builtInMap.put("<=", f);
		f.isOperator_ = true;
		Function.LessThanEq = f;

		f = new Function("_greater", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		Function.builtInMap.put("_greater", f);
		Function.builtInMap.put(">", f);
		f.isOperator_ = true;
		Function.GreaterThan = f;

		f = new Function("_greaterEq", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		Function.builtInMap.put("_greaterEq", f);
		Function.builtInMap.put(">=", f);
		f.isOperator_ = true;
		Function.GreaterThanEq = f;

		// string bool functions

		f = new Function("contains", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		Function.builtInMap.put("contains", f);
		Function.StrContains = f;

		f = new Function("startsWith", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		Function.builtInMap.put("startsWith", f);
		Function.StrStartsWith = f;

		f = new Function("endsWith", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		Function.builtInMap.put("endsWith", f);
		Function.StrEndsWith = f;
	}

	// math functions, unary
	public static Function Sign = null;
	public static Function Abs = null;
	public static Function Exp = null;
	public static Function Ceil = null;
	public static Function Floor = null;
	public static Function Trunc = null;
	public static Function Round = null;
	public static Function Ln = null; // base-e
	public static Function Lg = null; // base-10
	public static Function Sin = null;
	public static Function Cos = null;
	public static Function Tan = null;
	public static Function Sqrt = null;
	public static Function Factorial = null;

	static {
		Function f = null;

		f = new Function("sign", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.Sign = f;

		f = new Function("abs", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.Abs = f;

		f = new Function("exp", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.Exp = f;

		f = new Function("ceil", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.builtInMap.put("ceiling", f);
		Function.Ceil = f;

		f = new Function("floor", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.Floor = f;

		f = new Function("trunc", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.Trunc = f;

		f = new Function("round", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.Round = f;

		f = new Function("ln", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.Ln = f;

		f = new Function("lg", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		f.setPgFunction("log");
		Function.Lg = f;

		f = new Function("sin", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.Sin = f;

		f = new Function("cos", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.Cos = f;

		f = new Function("tan", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.Tan = f;

		f = new Function("sqrt", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.Sqrt = f;

		f = new Function("factorial", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		Function.builtInMap.put(f.name_, f);
		Function.Factorial = f;

	}

	// math functions, binary
	public static Function Add = null;
	public static Function Subtract = null;
	public static Function Multiply = null;
	public static Function Divide = null;
	public static Function Modulo = null;
	public static Function Power = null;
	public static Function Log = null;
	public static Function BitAnd = null;
	public static Function BitOr = null;
	public static Function BitXor = null;
	public static Function BitNeg = null;
	public static Function BitShiftLeft = null;
	public static Function BitShiftRight = null;

	static {
		Function f = null;
		f = new Function("add", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		Function.builtInMap.put("add", f);
		Function.builtInMap.put("+", f);
		f.isOperator_ = true;
		Function.Add = f;

		f = new Function("subtract", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		Function.builtInMap.put("subtract", f);
		Function.builtInMap.put("-", f);
		f.isOperator_ = true;
		Function.Subtract = f;

		f = new Function("multiply", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		Function.builtInMap.put("multiply", f);
		Function.builtInMap.put("*", f);
		f.isOperator_ = true;
		Function.Multiply = f;

		f = new Function("divide", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		Function.builtInMap.put("divide", f);
		Function.builtInMap.put("/", f);
		f.isOperator_ = true;
		Function.Divide = f;

		f = new Function("mod", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		Function.builtInMap.put("mod", f);
		Function.builtInMap.put("%", f);
		f.isOperator_ = true;
		Function.Modulo = f;

		f = new Function("pow", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		Function.builtInMap.put("pow", f);
		f.isOperator_ = false;
		Function.Power = f;

		f = new Function("log", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		Function.builtInMap.put("log", f);
		f.isOperator_ = false;
		Function.Log = f;

		f = new Function("bitand", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		Function.builtInMap.put(f.name_, f);
		Function.builtInMap.put("&", f);
		f.isOperator_ = true;
		Function.BitAnd = f;

		f = new Function("bitor", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		Function.builtInMap.put(f.name_, f);
		Function.builtInMap.put("|", f);
		f.isOperator_ = true;
		Function.BitOr = f;

		f = new Function("bitxor", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		Function.builtInMap.put(f.name_, f);
		Function.builtInMap.put("^", f);
		f.isOperator_ = true;
		Function.BitXor = f;

		f = new Function("bitneg", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		Function.builtInMap.put(f.name_, f);
		Function.builtInMap.put("~", f);
		f.isOperator_ = true;
		Function.BitNeg = f;

		f = new Function("bitShiftLeft", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		Function.builtInMap.put(f.name_, f);
		Function.builtInMap.put("<<", f);
		f.isOperator_ = true;
		Function.BitShiftLeft = f;

		f = new Function("bitShiftRight", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		Function.builtInMap.put(f.name_, f);
		Function.builtInMap.put(">>", f);
		f.isOperator_ = true;
		Function.BitShiftRight = f;

	}

	// string functions, unary
	public static Function Length = null;
	public static Function UpperCase = null;
	public static Function LowerCase = null;
	public static Function Trim = null;
	public static Function InitCap = null;
	public static Function MD5 = null;

	static {
		Function f = null;

		f = new Function("length", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		Function.builtInMap.put("length", f);
		Function.builtInMap.put("strlen", f);
		Function.builtInMap.put("len", f);
		Function.Length = f;

		f = new Function("upper", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		Function.builtInMap.put(f.name_, f);
		Function.UpperCase = f;

		f = new Function("lower", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		Function.builtInMap.put(f.name_, f);
		Function.LowerCase = f;

		f = new Function("trim", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		Function.builtInMap.put(f.name_, f);
		Function.Trim = f;

		f = new Function("initcap", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		Function.builtInMap.put(f.name_, f);
		Function.InitCap = f;

		f = new Function("md5", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		Function.builtInMap.put(f.name_, f);
		Function.MD5 = f;
	}

	// string functions, binary
	public static Function Concat = null;
	public static Function StrPos = null;
	public static Function Repeat = null;

	static {
		Function f = null;

		f = new Function("concat", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		Function.builtInMap.put("concat", f);
		Function.builtInMap.put("||", f);
		Function.Concat = f;

		f = new Function("strpos", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		Function.builtInMap.put(f.name_, f);
		Function.StrPos = f;

		f = new Function("repeat", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.Integer);
		Function.builtInMap.put(f.name_, f);
		Function.Repeat = f;

	}

	// string functions, ternary
	public static Function Substr = null;
	public static Function Replace = null;
	public static Function SplitPart = null;
	public static Function RegexReplace = null;

	static {
		Function f = null;

		f = new Function("substr", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		Function.builtInMap.put(f.name_, f);
		Function.Substr = f;

		f = new Function("replace", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		Function.builtInMap.put(f.name_, f);
		Function.Replace = f;

		f = new Function("split_part", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		f.addArgument(Type.Integer);
		Function.builtInMap.put(f.name_, f);
		Function.SplitPart = f;

		f = new Function("regex_replace", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		f.setPgFunction("regexp_replace");
		Function.builtInMap.put(f.name_, f);
		Function.RegexReplace = f;

	}

	/**
	 * Name of this function.
	 */
	private String name_;
	private String pgfun_ = null;

	private boolean isOperator_ = false;

	public boolean isOperator() {
		return this.isOperator_;
	}

	/**
	 * List of argument types and return type of this function.
	 */
	private ArrayList<Type> argTypes_ = new ArrayList<Type>();
	private Type retType_ = null;

	private boolean isBuiltIn_ = false;

	public boolean isBuiltIn() {
		return this.isBuiltIn_;
	}

	/**
	 * Get the corresponding function name inside PgSQL.
	 *
	 */
	public String getPgFunction() {
		return this.pgfun_;
	}

	/**
	 * Set the corresponding function name inside PgSQL.
	 *
	 */
	public void setPgFunction(String fun) {
		this.pgfun_ = fun;
	}

	public Function(String name, Type retType) {
		this.name_ = name;
		this.retType_ = retType;
		this.pgfun_ = this.name_;
	}

	public void addArgument(Type type) {
		this.argTypes_.add(type);
	}

	public int arity() {
		return this.argTypes_.size();
	}

	public String getName() {
		return this.name_;
	}

	/**
	 * Get return type
	 *
	 */
	public Type getRetType() {
		return this.retType_;
	}

	public List<Type> getArgTypes() {
		return this.argTypes_;
	}
}

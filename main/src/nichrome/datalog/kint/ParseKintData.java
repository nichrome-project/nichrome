package nichrome.datalog.kint;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import nichrome.datalog.Chord;
import nichrome.datalog.ClassicProject;
import nichrome.datalog.analyses.JavaAnalysis;
import nichrome.datalog.analyses.ProgramRel;
import nichrome.datalog.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;


/*
 * chord.parsekintdata.datafile  [default: ""] specifies the input file that should be parsed.   
 * chord.parsekintdata.debug     [default:false] if true, prints the relations in text format (at present)                                      
 */

@Chord(name = "parsekintdata",
consumes = { "KintK" }
)
public class ParseKintData extends JavaAnalysis {
	File dataFile;
	String dataFileName;
	
	TObjectIntHashMap<String> funcTotalArgs = new TObjectIntHashMap<String>();
	TObjectIntHashMap<String> funcUsedArgs = new TObjectIntHashMap<String>();
	HashMap<String, String> funcRetType = new HashMap<String, String>();
	HashMap<String, String> varType = new HashMap<String, String>();
	HashMap<String, String> varRange = new HashMap<String, String>();
	HashMap<String, ArrayList<String[]>> funcArgName = new HashMap<String, ArrayList<String[]>>();
	ArrayList<String> bugSinkArg = new ArrayList<String>();
	ArrayList<String> bugSinkFunc = new ArrayList<String>();
	ArrayList<ArrayList<String>> bugStackTrace = new ArrayList<ArrayList<String>>();
	ArrayList<String> stackFrameBuffer;
	ArrayList<String> bugCategory = new ArrayList<String>();
	ArrayList<String> bugInFunc = new ArrayList<String>();
	String currFunc;
	boolean parsingStack = false;
	boolean sinkFuncPresent = false;
	DomMM domMM;
	DomVV domVV;
	DomFF domFF;
	DomBB domBB;
	DomTT domTT;
	DomII domII;
	boolean debug = false;
	
	public void init() {
		 dataFileName = System.getProperty("chord.parsekintdata.datafile");
		 debug = Utils.buildBoolProperty("chord.parsekintdata.debug", false);
		 
		 if (dataFileName == null || dataFileName.equals("")) {
			 System.out.println("Nothing to parse: empty input file");
			 System.exit(1);
		 }
		 System.out.println("DATAFILE: " + dataFileName);
		 dataFile = new File(dataFileName);
		 domMM = (DomMM) ClassicProject.g().getTrgt("KintM");
		 domVV = (DomVV) ClassicProject.g().getTrgt("KintV");
		 domFF = (DomFF) ClassicProject.g().getTrgt("KintF");
		 domBB = (DomBB) ClassicProject.g().getTrgt("KintB");
		 domTT = (DomTT) ClassicProject.g().getTrgt("KintT");
		 domII = (DomII) ClassicProject.g().getTrgt("KintI");
		 // The 0th element of this domain is the "null" invoke location. This is required to encode 
		 // stack traces of length less than 4 in the relation bugStackTraceKIIII
		 domII.getOrAdd("");
	}
	
	public void run() {
		init();
		parse();
		populateRels();
	}
	
	public void parse() {	
		if (dataFile.exists()) {
			try {
				Scanner sc = new Scanner(dataFile);
				while (sc.hasNext()) {
					String line = sc.nextLine().trim();
					parseLine(line);
				}
				sc.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			if (parsingStack == true) {  // handle the stack entry for the last bug report
				bugStackTrace.add(stackFrameBuffer);
				parsingStack = false;
			}
		}
	}
	
	public void parseLine(String line) {
		if (line.startsWith("---") && parsingStack == true) {
			bugStackTrace.add(stackFrameBuffer);
			parsingStack = false;
		}
		String[] partsSpace = line.split(" ");
		String[] partsColon = partsSpace[0].split(":");
		partsColon[0] = partsColon[0].trim();
		
		if (partsColon[0].equals("bug")) {
			if (parsingStack == true) {
				bugStackTrace.add(stackFrameBuffer);
				parsingStack = false;
			}
			sinkFuncPresent = false;
			parseBug(partsSpace);
		} else if (partsColon[0].equals("status")){
			// ignore
		} else if (partsColon[0].equals("func")){
			parseFunc(partsSpace);
		} else if (partsColon[0].equals("totalargc")){
			parseTotalargc(partsSpace);
		} else if (partsColon[0].equals("usedargc")){
			parseUsedargc(partsSpace);
		} else if (partsColon[0].equals("argv")){
			parseArgv(partsSpace);
		} else if (partsColon[0].equals("argtype")){
			parseArgtype(partsSpace);
		} else if (partsColon[0].equals("argrange")){
			parseArgrange(partsSpace);
		} else if (partsColon[0].equals("rettype")){
			parseRettype(partsSpace);
		} else if (partsColon[0].equals("sinkarg")){
			parseSinkarg(partsSpace);
		} else if (partsColon[0].equals("sinkfunc")){
			parseSinkfunc(partsSpace);
		} else if (partsColon[0].equals("stack")){
			parsingStack = true;
			stackFrameBuffer = new ArrayList<String>();
		} else if (!line.equals("")){
			if (parsingStack)
				parseStackFrame(line);
		}
	}
	
	// Stack frame syntax: <filename>:<methodname>:<lineno>:<column_no>
	private void parseStackFrame(String line) {
		if (line.startsWith("- "))
			line = line.substring(2);
		String[] partsColon = line.split(":");
		for (int i = 0; i < partsColon.length; i++)
			partsColon[i] = partsColon[i].trim();
		stackFrameBuffer.add(line);
		domFF.getOrAdd(partsColon[0]); // filename
		//domMM.getOrAdd(partsColon[0] + ":" + partsColon[1]); // fully qualified method name 
		domII.getOrAdd(line); // a method invocation location
	}

	// syntax sinkfunc: <method_name>
	private void parseSinkfunc(String[] partsSpace) {
		String sinkfunc = "";
		if (partsSpace.length > 1) {
			sinkfunc = partsSpace[1].trim();
			sinkFuncPresent = true;
			domMM.getOrAdd(sinkfunc);
		}
		bugSinkFunc.add(sinkfunc);
	}

	// syntax: sinkarg: <argname>
	private void parseSinkarg(String[] partsSpace) {
		String fqn = "";
		if (partsSpace.length > 1) {
			String sinkarg = partsSpace[1].trim();
			fqn = currFunc + ":" + sinkarg;
			domVV.getOrAdd(fqn);
		}
		bugSinkArg.add(fqn);
	}

	// syntax: rettype: <type>
	private void parseRettype(String[] partsSpace) {
		String rettype = partsSpace[1].trim();
		funcRetType.put(currFunc, rettype);
		domTT.getOrAdd(rettype);
	}

	// syntax: argrange: <param_position>:<int> ...  syntax may change later
	private void parseArgrange(String[] partsSpace) {
		int sz = partsSpace.length;
		for (int i = 1; i < sz; i++) {
			String ar = partsSpace[i].trim();
			String[] partsColon = ar.split(":");
			String argname = getCurrFuncArg(partsColon[0]);
			varRange.put(argname, partsColon[1]);
		}
		
	}

	//syntax: argtype: <param_position>:<param_type> ...
	private void parseArgtype(String[] partsSpace) {
		int sz = partsSpace.length;
		for (int i = 1; i < sz; i++) {
			String at = partsSpace[i].trim();
			String[] partsColon = at.split(":");
			String argname = getCurrFuncArg(partsColon[0]);
			domTT.getOrAdd(partsColon[1]);
			varType.put(argname, partsColon[1]);
		}
	}

	// syntax: argv: <param_position>:<param_name> ...  Note: numbers indicating param positions may not be continuous.
	// This is because, this info is only for used args
	private void parseArgv(String[] partsSpace) {
		int sz = partsSpace.length;
		ArrayList<String[]> argNames = new ArrayList<String[]>();
		for (int i = 1; i < sz; i++) {
			String v = partsSpace[i].trim();
			String[] partsColon = v.split(":");
			String fqn = currFunc + ":" + partsColon[1];
			partsColon[1] = fqn;
			argNames.add(partsColon);
			domVV.getOrAdd(fqn);
		}
		funcArgName.put(currFunc, argNames);
	}

	// syntax: usedargc: <integer>
	private void parseUsedargc(String[] partsSpace) {
		String val = partsSpace[1].trim();
		int intval = Integer.parseInt(val);
		funcUsedArgs.put(currFunc, intval);
	}

	// syntax: totalargc: <integer>
	private void parseTotalargc(String[] partsSpace) {
		String val = partsSpace[1].trim();
		int intval = Integer.parseInt(val);
		funcTotalArgs.put(currFunc, intval);
	}

	// syntax: func: <func_name>
	private void parseFunc(String[] partsSpace) {
		currFunc = partsSpace[1].trim();
		domMM.getOrAdd(currFunc);
		bugInFunc.add(currFunc);
	}

	// syntax: bug: <bug_category>  Bug category could be umul, smul, uadd, sadd, etc 
	public void parseBug(String[] partsSpace) {
		String bcat = partsSpace[1].trim();
		bugCategory.add(bcat);
		domBB.getOrAdd(bcat);
	}
	
	public String getCurrFuncArg(String argnum) {
		String var = "";
		
		ArrayList<String[]> args = funcArgName.get(currFunc);
		for (String[] arg : args) {
			if (arg[0].equals(argnum)) {
				var = arg[1];
			}
		}
		return var;
	}
	
	public void relInit() {
		domMM.save();
		domVV.save();
		domFF.save();
		domBB.save();
		domTT.save();
		domII.save();
	}
	
	public void populateRels() {
		relInit();
		populateMethodTotalArgsMK();
		populateMethodUsedArgsMK();
		populateMethodArgNameMKV();
		populateMethodRetTypeMT();
		populateVarTypeVT();
		populateVarRangeVT();
		populateBugInMethodKM();
		populateBugCategoryKB();
		populateBugSinkArgKV();
		populateBugSinkMethodKM();
		populateBugStackTraceKIIII();
		populateInvokeIM();
	}
	
	public void populateMethodTotalArgsMK() {
		ProgramRel relMethodTotalArgsMK = (ProgramRel) ClassicProject.g().getTrgt("MethodTotalArgsMK");
		relMethodTotalArgsMK.zero();
		for (String m : funcTotalArgs.keySet()) {
			int targs = funcTotalArgs.get(m);
			relMethodTotalArgsMK.add(domMM.indexOf(m), targs);
		}
		if (debug)
			relMethodTotalArgsMK.print();
		else
			relMethodTotalArgsMK.close();
	}
	
	public void populateMethodUsedArgsMK() {
		ProgramRel relMethodUsedArgsMK = (ProgramRel) ClassicProject.g().getTrgt("MethodUsedArgsMK");
		relMethodUsedArgsMK.zero();
		
		for (String m : funcUsedArgs.keySet()) {
			int uargs = funcUsedArgs.get(m);
			relMethodUsedArgsMK.add(domMM.indexOf(m), uargs);
		}
		if (debug)
			relMethodUsedArgsMK.print();
		else
			relMethodUsedArgsMK.close();
	}
	
	public void populateMethodRetTypeMT() {
		ProgramRel relMethodRetTypeMT = (ProgramRel) ClassicProject.g().getTrgt("MethodRetTypeMT");
		relMethodRetTypeMT.zero();
		
		for (String m : funcRetType.keySet()) {
			String rtype = funcRetType.get(m);
			relMethodRetTypeMT.add(domMM.indexOf(m), domTT.indexOf(rtype));
		}
		if (debug)
			relMethodRetTypeMT.print();
		else
			relMethodRetTypeMT.close();
	}
	
	public void populateMethodArgNameMKV() {
		ProgramRel relMethodArgNameMKV = (ProgramRel) ClassicProject.g().getTrgt("MethodArgNameMKV");
		relMethodArgNameMKV.zero();
		
		for (String m : funcArgName.keySet()) {
			ArrayList<String[]> args = funcArgName.get(m);
			for (String[] arg : args) {
				int argnum = Integer.parseInt(arg[0]);
				relMethodArgNameMKV.add(domMM.indexOf(m), argnum, domVV.indexOf(arg[1]));
			}
		}
		if (debug)
			relMethodArgNameMKV.print();
		else
			relMethodArgNameMKV.close();
	}
	
	public void populateVarTypeVT() {
		ProgramRel relVarTypeVT = (ProgramRel) ClassicProject.g().getTrgt("VarTypeVT");
		relVarTypeVT.zero();
	
		for (String var : varType.keySet()) {
			String typ = varType.get(var);
			relVarTypeVT.add(domVV.indexOf(var), domTT.indexOf(typ));
		}
		if (debug)
			relVarTypeVT.print();
		else
			relVarTypeVT.close();
	}
	
	public void populateVarRangeVT() {
		ProgramRel relVarRangeVT = (ProgramRel) ClassicProject.g().getTrgt("VarRangeVT");
		relVarRangeVT.zero();
	
		for (String var : varType.keySet()) {
			String typ = varType.get(var);
			relVarRangeVT.add(domVV.indexOf(var), domTT.indexOf(typ));
		}
		if (debug)
			relVarRangeVT.print();
		else
			relVarRangeVT.close();
	}
	
	public void populateBugInMethodKM() {
		ProgramRel relBugInMethodKM = (ProgramRel) ClassicProject.g().getTrgt("BugInMethodKM");
		relBugInMethodKM.zero();
	
		int ndx = 0;
		for (String m : bugInFunc) {
			relBugInMethodKM.add(ndx, domMM.indexOf(m));
			ndx++;
		}
		if (debug)
			relBugInMethodKM.print();
		else
			relBugInMethodKM.close();
	}
	
	public void populateBugCategoryKB() {
		ProgramRel relBugCategoryKB = (ProgramRel) ClassicProject.g().getTrgt("BugCategoryKB");
		relBugCategoryKB.zero();
	
		int ndx = 0;
		for (String b : bugCategory) {
			relBugCategoryKB.add(ndx, domBB.indexOf(b));
			ndx++;
		}
		if (debug)
			relBugCategoryKB.print();
		else
			relBugCategoryKB.close();
	}
	
	public void populateBugSinkArgKV() {
		ProgramRel relBugSinkArgKV = (ProgramRel) ClassicProject.g().getTrgt("BugSinkArgKV");
		relBugSinkArgKV.zero();
	
		int ndx = 0;
		for (String v : bugSinkArg) {
			if (!v.equals(""))
				relBugSinkArgKV.add(ndx, domVV.indexOf(v));
			ndx++;
		}
		if (debug)
			relBugSinkArgKV.print();
		else
			relBugSinkArgKV.close();
	}
	
	public void populateBugSinkMethodKM() {
		ProgramRel relBugSinkMethodKM = (ProgramRel) ClassicProject.g().getTrgt("BugSinkMethodKM");
		relBugSinkMethodKM.zero();
	
		int ndx = 0;
		for (String m : bugSinkFunc) {
			if (!m.equals(""))
				relBugSinkMethodKM.add(ndx, domMM.indexOf(m));
			ndx++;
		}
		if (debug)
			relBugSinkMethodKM.print();
		else
			relBugSinkMethodKM.close();
	}
	
	public void populateBugStackTraceKIIII() {
		ProgramRel relBugStackTraceKIIII = (ProgramRel) ClassicProject.g().getTrgt("BugStackTraceKIIII");
		relBugStackTraceKIIII.zero();
	
		int ndx = 0;
		for (ArrayList<String> trace : bugStackTrace) {
			int idx0 = 0;
			int idx1 = 0;
			int idx2 = 0;
			int idx3 = 0;
			int sz = trace.size();
			
			idx0 = domII.indexOf(trace.get(0));
			if (sz > 1) idx1 = domII.indexOf(trace.get(1));
			if (sz > 2) idx2 = domII.indexOf(trace.get(2));
			if (sz > 3) idx3 = domII.indexOf(trace.get(3));
			relBugStackTraceKIIII.add(ndx, idx0, idx1, idx2, idx3);
			ndx++;
		}
		if (debug)
			relBugStackTraceKIIII.print();
		else
			relBugStackTraceKIIII.close();
	}
	
	public void populateInvokeIM() {
		
	}
}

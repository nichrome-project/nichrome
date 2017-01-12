package nichrome.datalog.analyses;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import nichrome.datalog.ClassicProject;
import nichrome.datalog.Config;
import nichrome.datalog.ITask;
import nichrome.datalog.bddbddb.Dom;

/**
 * Generic implementation of a program domain (a specialized kind
 * of Java task).
 * <p>
 * A program domain maps each of N values of some related type in the
 * given Java program (e.g., methods, types, statements of a certain
 * kind, etc.) to a unique integer in the range [0..N-1].  The
 * integers are assigned in the order in which the values are added to
 * the domain.
 *
 * @param    <T>    The type of values in the program domain.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ProgramDom<T> extends Dom<T> implements ITask {
    protected Object[] consumes;
    @Override
    public void run() {
        clear();
        init();
        fill();
        save();
    }

    public void init() { }
    public void save() {
        if (Config.verbose >= 1)
            System.out.println("SAVING dom " + name + " size: " + size());
        try {
            super.save(Config.bddbddbWorkDirName, Config.saveDomMaps);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        ClassicProject.g().setTrgtDone(this);
    }
    public void fill() {
    	throw new RuntimeException("Domain '" + getName() +
    		"' must override method fill().");

    }
    /**
     * Provides the XML attributes string of the specified value.
     * Subclasses may override this method if necessary.
     * 
     * @param    val    A value.
     * 
     * @return    The XML attributes string of the specified value.
     *             It is the empty string by default.
     * 
     * @see    #saveToXMLFile()
     */
    public String toXMLAttrsString(T val) {
        return "";
    }
    /**
     * Provides the XML elements string of the specified value.
     * Subclasses may override this method if necessary.
     * 
     * @param    val    A value.
     * 
     * @return    The XML elements string of the specified value.
     *             It is the empty string by default.
     * 
     * @see    #saveToXMLFile()
     */
    public String toXMLElemsString(T val) {
        return "";
    }
    public void saveToXMLFile() {
        String name = getName();
        String tag = name + "list";
        String fileName = tag + ".xml";
        PrintWriter out;
        try {
            File file = new File(Config.outDirName, fileName);
            out = new PrintWriter(new FileWriter(file));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        out.println("<" + tag + ">");
        for (int i = 0; i < size(); i++) {
            T val = get(i);
            out.println("<" + name + " id=\"" + name + i + "\" " +
                toXMLAttrsString(val) + ">");
            out.println(toXMLElemsString(val));
            out.println("</" + name + ">");
        }
        out.println("</" + tag + ">");
        out.close();
    }
    @Override
    public String toString() {
        return name;
    }
}

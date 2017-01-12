package nichrome.datalog.analyses;

import java.util.List;

import nichrome.datalog.ClassicProject;
import nichrome.datalog.Config;
import nichrome.datalog.ITask;
import nichrome.datalog.Messages;
import nichrome.datalog.bddbddb.Rel;

import java.io.File;

/**
 * Generic implementation of a program relation (a specialized kind of Java task).
 * <p>
 * A program relation is a relation over one or more program domains.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ProgramRel extends Rel implements ITask {
    private static final String SKIP_TUPLE =
        "WARN: Skipping a tuple from relation '%s' as element '%s' was not found in domain '%s'.";
    protected Object[] consumes;
    @Override
    public void run() {
        zero();
        init();
        fill();
        save();
    }
    public void init() { }
    public void save() {
        if (Config.verbose >= 1)
            System.out.println("SAVING rel " + name + " size: " + size());
        super.save(Config.bddbddbWorkDirName);
        ClassicProject.g().setTrgtDone(this);
    }
    public void load() {
        super.load(Config.bddbddbWorkDirName);
    }
    public void fill() {
    	throw new RuntimeException("Relation '" + name +
    		"' must override method fill().");
    }
    public void print() {
        super.print(Config.outDirName);
    }
    public String toString() {
        return name;
    }
    public void skip(Object elem, ProgramDom dom) {
        Messages.log(SKIP_TUPLE, getClass().getName(), elem, dom.getClass().getName());
    }
}

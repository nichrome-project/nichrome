package nichrome.util;

import java.io.IOException;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.SubCommand;
import org.kohsuke.args4j.spi.SubCommandHandler;
import org.kohsuke.args4j.spi.SubCommands;

import nichrome.alps.ALPSCommandLine;
import nichrome.maxsat.incremental.MaxSATCommandLine;


public class CommandLineEntry {
	
	// supported engines in Nichrome
	final static String ALPS = "ALPS";
	final static String MaxSAT = "MaxSAT";
	final static String MLN = "MLN";
	
	@Option(name = "-h", aliases="-help", usage = "Display command options.")
	public boolean showHelp = false;

    @Argument(required=true,index=0,metaVar="engine",usage="engines, specify which engine to use",handler=SubCommandHandler.class)
    @SubCommands({
      @SubCommand(name=ALPS,impl=ALPSCommandLine.class),
      @SubCommand(name=MaxSAT,impl=MaxSATCommandLine.class),
    })
    CommandInterface engine;

    
    public void doMain(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            engine.execute();
        } catch( CmdLineException e ) {
            System.err.println(e.getMessage());
            
            if(args.length > 0) {
            	String s = args[0];
            	if(s.equalsIgnoreCase(ALPS)) {
            		new ALPSCommandLine().printUsage();
            	}
            	else if(s.equalsIgnoreCase(MaxSAT)) {
            		CmdLineParser p = new CmdLineParser(new MaxSATCommandLine());
            		p.printUsage(System.err);
            	}
            	else {
            		System.err.println("\n------\nUsage: ");
            		parser.printUsage(System.err);
            	}
            	
            }
        }

    }
    
    public static void main(String[] args) throws IOException{
    	CommandLineEntry m = new CommandLineEntry();
    	m.doMain(args);
    }
}

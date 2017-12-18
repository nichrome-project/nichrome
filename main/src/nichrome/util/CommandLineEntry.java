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
import nichrome.bingo.BINGOCommandLine;
import nichrome.maxsat.incremental.MaxSATCommandLine;
import nichrome.mln.driver.MLNCommandLine;


public class CommandLineEntry {

    // supported engines in Nichrome
    final static String ALPS = "ALPS";
    final static String BINGO = "BINGO";
    final static String MaxSAT = "MaxSAT";
    final static String MLN = "MLN";

    @Option(name = "-h", aliases="-help", usage = "display command options.")
    public boolean showHelp = false;

    @Argument(required=true,index=0,metaVar="engine",usage="specify which engine to use",handler=SubCommandHandler.class)
    @SubCommands({
    @SubCommand(name=ALPS,impl=ALPSCommandLine.class),
    @SubCommand(name=BINGO,impl=BINGOCommandLine.class),
    @SubCommand(name=MaxSAT,impl=MaxSATCommandLine.class),
    @SubCommand(name=MLN,impl=MLNCommandLine.class)
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
                else if(s.equalsIgnoreCase(BINGO)) {
                    new BINGOCommandLine().printUsage();
                }
                else if(s.equalsIgnoreCase(MLN)) {
                    new MLNCommandLine().printUsage();
                }
                else if(s.equalsIgnoreCase(MaxSAT)) {
                    CmdLineParser p = new CmdLineParser(new MaxSATCommandLine());
                    System.err.println("\n------\nUsage: ");
                    p.printUsage(System.err);
                }
                else {
                    System.err.println("\n------\nUsage: ");
                    parser.printUsage(System.err);
                }
            }
            else{
                System.err.println("\n------\nUsage: ");
                parser.printUsage(System.err);
            }
        }

    }

    public static void main(String[] args) throws IOException{
        CommandLineEntry m = new CommandLineEntry();
        m.doMain(args);
    }
}

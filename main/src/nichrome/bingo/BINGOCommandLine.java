package nichrome.bingo;

import org.kohsuke.args4j.Option;

import nichrome.util.AbstractBaseCommand;
import nichrome.util.CommandInterface;

public class BINGOCommandLine extends AbstractBaseCommand implements CommandInterface {
    @Option(name="-F", metaVar="<path>", required=true, usage="specify the factor-graph file")
    private String dataFile;
    
    @Override
    public boolean execute() {
        BINGOInterface bingo = new BINGOInterface();
        bingo.dataFile = dataFile;
        bingo.learn();
        return true;
    }

    @Override
    public String getName() {
        return "SubCommand BINGO";
    }
}

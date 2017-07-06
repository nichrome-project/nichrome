package nichrome.mln.driver;

import java.io.OutputStreamWriter;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionHandlerFilter;

import nichrome.datalog.DlogRunner;
import nichrome.mln.infer.Inferer;
import nichrome.mln.learn.Learner;
import nichrome.mln.parser.CommandOptions;
import nichrome.mln.util.Config;
import nichrome.mln.util.Timer;
import nichrome.mln.util.UIMan;
import nichrome.ursa.maymust.AlarmResolutionDriver;
import nichrome.util.CommandInterface;

public class MLNCommandLine extends CommandOptions implements CommandInterface {
	
	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
		
		Timer.start(Config.GLOBAL_TIMER);
		UIMan.println("*** Welcome to " + Config.product_name + "!");

		UIMan.processOptions(this);
		
		if (isDatalogMode) {
			DlogRunner.run();
		} else if (isLearningMode) {
			Learner learner = new Learner();
			learner.learn(this);
		} else if (this.alarmResolution){
			AlarmResolutionDriver ard = new AlarmResolutionDriver();
			ard.init(this);
			ard.run();
		}
		else {
			Inferer inferer = new Inferer();
			inferer.infer(this);
		}

		UIMan.println("*** MLN Engine exited at " + Timer.getDateTime()
			+ " after running for " + Timer.elapsed(Config.GLOBAL_TIMER));
		UIMan.closeDribbleFile();

		return true;
	}

	@Override
	public void printUsage(){
		OptionHandlerFilter optionFilter = showToOutsider;
		if(showInternal) {
			optionFilter = OptionHandlerFilter.ALL;
		}
		
		CmdLineParser parser= new CmdLineParser(this);

		OutputStreamWriter osw = new OutputStreamWriter(System.err);
		System.err.println("\n------\nUsage: "+ getName());
		parser.printUsage(osw,null, optionFilter);
	}
	
	@Override
	public String getName() {
	    return "SubCommand MLN";
	 }
}

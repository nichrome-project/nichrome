package nichrome.mln.driver;

import nichrome.datalog.DlogRunner;
import nichrome.mln.infer.Inferer;
import nichrome.mln.learn.Learner;
import nichrome.mln.parser.CommandOptions;
import nichrome.mln.util.Config;
import nichrome.mln.util.Timer;
import nichrome.mln.util.UIMan;
import nichrome.ursa.maymust.AlarmResolutionDriver;

public class Main {

	public static void main(String[] args) {
		Timer.start(Config.GLOBAL_TIMER);
		CommandOptions options = UIMan.parseCommand(args);
		UIMan.println("*** Welcome to " + Config.product_name + "!");
		if (options == null) {
			return;
		}

		if (options.isDatalogMode) {
			DlogRunner.run();
		} else if (options.isLearningMode) {
			Learner learner = new Learner();
			learner.learn(options);
		} else if(options.alarmResolution){
			AlarmResolutionDriver ard = new AlarmResolutionDriver();
			ard.init(options);
			ard.run();
		} 
		else {
			Inferer inferer = new Inferer();
			inferer.infer(options);
		}

		UIMan.println("*** MLN Engine exited at " + Timer.getDateTime()
			+ " after running for " + Timer.elapsed(Config.GLOBAL_TIMER));
		UIMan.closeDribbleFile();
	}

}

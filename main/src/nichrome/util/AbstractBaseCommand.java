package nichrome.util;

import java.lang.reflect.Field;

import org.kohsuke.args4j.Option;

public class AbstractBaseCommand {
	
	public String getName() {
	    return "BaseCommand";
	  }

	  public void printUsage() {
	    System.out.println("\n------\nUsage: " + this.getName());

	    for (Field f : this.getClass().getDeclaredFields()) {
	      if (f.isAnnotationPresent(Option.class)) {
	        Option option = f.getAnnotation(Option.class);

	        
	        System.out.println(String.format("  %-5s %-10s: %-50s (required=%s)", option.name(),  option.metaVar(),
	            option.usage(), option.required()));
	      }
	    }
	  }
}

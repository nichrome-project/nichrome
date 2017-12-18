package nichrome.bingo;

import java.io.File;

public class BINGOInterface {
    protected String executable;
    protected String dataFile;

    public static void main(String [] args) {
        BINGOInterface bingo = new BINGOInterface();
        bingo.learn();
    }

    public void learn() {
        executable = System.getenv("NICHROME_HOME") + "/main/lib/bingo";
        File f = new File(executable);
        if(!f.exists()) {
            System.err.println("Error: Cannot find " + executable);
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder().command(executable, dataFile).inheritIO();
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

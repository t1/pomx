package com.github.t1.pomx;

import com.beust.jcommander.*;

public class Main {
    @Parameter(names = { "-h", "--help" }, help = true, description = "Show usage and exit")
    boolean help;

    public static void main(String... args) {
        Main main = new Main();
        JCommander commander = new JCommander(main);
        commander.setProgramName(Main.class.getName());
        commander.parse(args);
        if (main.help)
            commander.usage();
        else
            main.run();
    }

    private void run() {
        System.out.println("run");
    }
}

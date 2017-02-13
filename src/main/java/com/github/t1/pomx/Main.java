package com.github.t1.pomx;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.*;

import static java.util.Arrays.*;

@RequiredArgsConstructor
public class Main {
    public static void main(String... args) {
        new Main(new ArrayList<>(asList(args))).run();
    }

    private final List<String> args;

    void run() {
        args.add(0, "mvn");
        runMaven(args);
    }

    void runMaven(List<String> args) {
        try {
            ProcessBuilder builder = new ProcessBuilder(this.args);
            Process process = builder.inheritIO().start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

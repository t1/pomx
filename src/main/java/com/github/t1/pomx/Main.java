package com.github.t1.pomx;

import lombok.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.*;

import static java.util.Arrays.*;

@RequiredArgsConstructor
public class Main {
    private static final Path POMX = Paths.get("pomx.xml");
    private static final Path POM = Paths.get("pom.xml");

    public static void main(String... args) {
        new Main(new ArrayList<>(asList(args))).run();
    }

    private final List<String> args;

    void run() {
        args.add(0, "mvn");
        ProjectObjectModel pom = ProjectObjectModel.readFrom(POMX);
        pom.writeTo(POM);
        runMaven(args);
    }

    @SneakyThrows({ IOException.class, InterruptedException.class })
    void runMaven(List<String> args) {
        ProcessBuilder builder = new ProcessBuilder(this.args);
        Process process = builder.inheritIO().start();
        process.waitFor();
    }
}

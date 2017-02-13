package com.github.t1.pomx;

import org.junit.Test;

import java.util.ArrayList;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;

public class MainTest {
    @Test
    public void shouldRunMaven() throws Exception {
        Main.main("-version");
    }

    @Test
    public void shouldPassParamsToMaven() throws Exception {
        AtomicReference<List<String>> passed = new AtomicReference<>();
        Main main = new Main(new ArrayList<>(asList("x", "y"))) {
            @Override void runMaven(List<String> args) {
                passed.set(args);
            }
        };

        main.run();

        assertThat(passed.get()).containsExactly("mvn", "x", "y");
    }
}

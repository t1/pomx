package com.github.t1.pomx;

import com.github.t1.testtools.SystemOutCaptorRule;
import org.junit.*;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;

public class MainTest {
    @Rule public SystemOutCaptorRule system = new SystemOutCaptorRule();

    @Test
    public void shouldShowHelp() {
        Main.main("--help");

        assertThat(system.out()).isEqualTo(""
                + "Usage: com.github.t1.pomx.Main [options]\n"
                + "  Options:\n"
                + "    -h, --help\n"
                + "      Show usage and exit\n"
                + "      Default: false");
    }

    @Test
    public void shouldRun() throws Exception {
        Main.main("");

        assertThat(system.out()).isEqualTo("run");
    }

    @Test
    public void shouldLoadRealPom() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.readFrom(Paths.get("pom.xml"));

        assertThat(pom).isNotNull();
    }
}

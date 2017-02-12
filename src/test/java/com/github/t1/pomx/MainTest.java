package com.github.t1.pomx;

import com.github.t1.testtools.SystemOutCaptorRule;
import org.junit.*;

import static org.assertj.core.api.Assertions.*;

public class MainTest {
    @Rule public SystemOutCaptorRule systemOut = new SystemOutCaptorRule();

    @Test
    public void shouldShowHelp() {
        Main.main("--help");

        assertThat(systemOut.out()).isEqualTo(""
                + "Usage: com.github.t1.pomx.Main [options]\n"
                + "  Options:\n"
                + "    -h, --help\n"
                + "      Show usage and exit\n"
                + "      Default: false");
    }
}

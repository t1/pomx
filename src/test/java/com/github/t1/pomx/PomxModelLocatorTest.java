package com.github.t1.pomx;

import org.codehaus.plexus.logging.Logger;
import org.junit.*;

import java.io.File;
import java.nio.file.*;

import static java.nio.charset.StandardCharsets.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PomxModelLocatorTest {
    private final PomxModelLocator locator = new PomxModelLocator();

    private Path tmp;

    @Before public void setUp() throws Exception {
        tmp = Files.createTempDirectory("pom-locator");
        locator.log = mock(Logger.class);
    }

    @After public void tearDown() throws Exception { Files.delete(tmp); }

    @Test
    public void shouldNotLocateMissingPomx() throws Exception {
        File found = locator.locatePom(tmp.toFile());

        assertThat(found).isEqualTo(tmp.resolve("pom.xml").toFile());
        verifyNoMoreInteractions(locator.log);
    }

    @Test
    public void shouldLocateExistingPomx() throws Exception {
        Path pomx = tmp.resolve("pomx.xml");
        File pom = null;
        try {
            Files.write(pomx, ("<project><jar>foo:bar:1.0</jar></project>").getBytes(UTF_8));

            pom = locator.locatePom(tmp.toFile());

            assertThat(pom).isEqualTo(tmp.resolve("pom.xml").toFile());
            verify(locator.log).info("convert " + tmp + "/pomx.xml to " + tmp + "/pom.xml");
        } finally {
            Files.delete(pomx);
            if (pom != null)
                Files.delete(pom.toPath());
        }
    }
}

package com.github.t1.pomx;

import org.apache.maven.model.locator.ModelLocator;
import org.codehaus.plexus.component.annotations.*;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.nio.file.*;

@Component(role = ModelLocator.class)
public class PomxModelLocator implements ModelLocator {
    @Requirement
    Logger log;

    @Override public File locatePom(File dir) {
        Path pom = dir.toPath().resolve("pom.xml");
        Path pomx = dir.toPath().resolve("pomx.xml");
        if (Files.exists(pomx)) {
            log.info("convert " + pomx + " to " + pom);
            ProjectObjectModel.readFrom(pomx).writeTo(pom);
        }
        return new File(dir, "pom.xml");
    }
}

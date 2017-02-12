package com.github.t1.pomx;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class ProjectTest {
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    private static final String NAMESPACE =
            "xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                    + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"";

    @Test
    public void shouldAddModelVersion() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project " + NAMESPACE + ">\n"
                + "</project>\n");

        assertThat(pom.asString()).isEqualTo(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "</project>\n");
    }

    @Test
    public void shouldExpandGAV() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <war>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</war>\n"
                + "</project>\n");

        assertThat(pom.asString()).isEqualTo(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <groupId>dummy-group</groupId>\n"
                + "    <artifactId>dummy-artifact</artifactId>\n"
                + "    <version>1.2.3-SNAPSHOT</version>\n"
                + "    <packaging>war</packaging>\n"
                + "</project>\n");
    }

    @Test
    public void shouldExpandGACV() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <war>dummy-group:dummy-artifact:mac-os:1.2.3-SNAPSHOT</war>\n"
                + "</project>\n");

        assertThat(pom.asString()).isEqualTo(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <groupId>dummy-group</groupId>\n"
                + "    <artifactId>dummy-artifact</artifactId>\n"
                + "    <version>1.2.3-SNAPSHOT</version>\n"
                + "    <classifier>mac-os</classifier>\n"
                + "    <packaging>war</packaging>\n"
                + "</project>\n");
    }
}

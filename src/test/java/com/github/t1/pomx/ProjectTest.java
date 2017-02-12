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
    public void shouldExpandJarGAV() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
                + "</project>\n");

        assertThat(pom.asString()).isEqualTo(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <groupId>dummy-group</groupId>\n"
                + "    <artifactId>dummy-artifact</artifactId>\n"
                + "    <version>1.2.3-SNAPSHOT</version>\n"
                + "    <packaging>jar</packaging>\n"
                + "</project>\n");
    }

    @Test
    public void shouldExpandWarGAV() throws Exception {
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
    public void shouldFailToExpandMultipleGAV() throws Exception {
        Throwable throwable = catchThrowable(() -> ProjectObjectModel
                .from(XML
                        + "<project " + NAMESPACE + ">\n"
                        + "    <war>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</war>\n"
                        + "    <war>dummy-group2:dummy-artifact2:1.2.3-SNAPSHOT</war>\n"
                        + "</project>\n")
                .asString());

        assertThat(throwable).hasMessageContaining("multiple packagings found");
    }

    @Test
    public void shouldFailToExpandGavWithOneItem() throws Exception {
        Throwable throwable = catchThrowable(() -> ProjectObjectModel
                .from(XML
                        + "<project " + NAMESPACE + ">\n"
                        + "    <war>dummy-group</war>\n"
                        + "</project>\n")
                .asString());

        assertThat(throwable).hasMessageContaining("too few elements 1 in GAV expression: 'dummy-group'");
    }

    @Test
    public void shouldFailToExpandGavWithTwoItems() throws Exception {
        Throwable throwable = catchThrowable(() -> ProjectObjectModel
                .from(XML
                        + "<project " + NAMESPACE + ">\n"
                        + "    <war>dummy-group:dummy-artifact</war>\n"
                        + "</project>\n")
                .asString());

        assertThat(throwable).hasMessageContaining(
                "too few elements 2 in GAV expression: 'dummy-group:dummy-artifact'");
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

    @Test
    public void shouldFailToExpandGavWithFiveItems() throws Exception {
        Throwable throwable = catchThrowable(() -> ProjectObjectModel
                .from(XML
                        + "<project " + NAMESPACE + ">\n"
                        + "    <war>dummy-group:dummy-artifact:mac-os:1.2.3-SNAPSHOT:too-much</war>\n"
                        + "</project>\n")
                .asString());

        assertThat(throwable).hasMessageContaining(
                "too many elements 5 in GAV expression: 'dummy-group:dummy-artifact:mac-os:1.2.3-SNAPSHOT:too-much'");
    }


    @Test
    public void shouldExpandDepencencyManagement() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <dependencyManagement>\n"
                + "        <dependencies>\n"
                + "            <pom>org.jboss.arquillian:arquillian-bom:1.1.11.Final</pom>\n"
                + "        </dependencies>\n"
                + "    </dependencyManagement>\n"
                + "</project>\n");


        assertThat(pom.asString()).isEqualTo(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <dependencyManagement>\n"
                + "        <dependencies>\n"
                + "            <dependency>\n"
                + "            <groupId>org.jboss.arquillian</groupId>\n"
                + "            <artifactId>arquillian-bom</artifactId>\n"
                + "            <version>1.1.11.Final</version>\n"
                + "            <scope>import</scope>\n"
                + "            <type>pom</type>\n"
                + "        </dependency>\n"
                + "        </dependencies>\n"
                + "    </dependencyManagement>\n"
                + "</project>\n");
    }
}

package com.github.t1.pomx;

import org.junit.Test;

import java.io.File;
import java.nio.file.*;

import static org.assertj.core.api.Assertions.*;

public class ProjectObjectModelTest {
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    private static final String NAMESPACE = ""
            + "xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"";
    private static final String WARNING = warning("nil:--");

    private static String warning(Object source) {
        return ""
                + "    <!-- WARNING: Do Not Modify This File! -->\n"
                + "    <!-- Generated from " + source + " -->\n";
    }


    @Test
    public void shouldLeaveRealPomMoreOrLessAsIs() throws Exception {
        Path path = Paths.get("pom.xml");
        ProjectObjectModel pom = ProjectObjectModel.readFrom(path);

        String xml = pom.asString().replace(warning("pom.xml"), "");

        assertThat(xml).isEqualTo(contentOf(new File("pom.xml")).replace("\" ?>", "\"?>"));
    }

    @Test
    public void shouldAddModelVersion() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project " + NAMESPACE + ">\n"
                + "</project>\n");

        assertThat(pom.asString()).isEqualTo(XML
                + "<project " + NAMESPACE + ">\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "</project>\n");
    }

    @Test
    public void shouldNotAddModelVersionIfAlreadyThere() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "</project>\n");

        assertThat(pom.asString()).isEqualTo(XML
                + "<project " + NAMESPACE + ">\n" + WARNING
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
                + "<project " + NAMESPACE + ">\n" + WARNING
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
                + "<project " + NAMESPACE + ">\n" + WARNING
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
                + "<project " + NAMESPACE + ">\n" + WARNING
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
    public void shouldExpandBuildPlugin() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <build>\n"
                + "        <plugins>\n"
                + "            <plugin id=\"org.apache.maven.plugins:maven-jar-plugin:2.4\">\n"
                + "                <configuration>\n"
                + "                    <archive>\n"
                + "                        <addMavenDescriptor>false</addMavenDescriptor>\n"
                + "                    </archive>\n"
                + "                </configuration>\n"
                + "            </plugin>\n"
                + "        </plugins>\n"
                + "    </build>\n"
                + "</project>\n");

        assertThat(pom.asString()).isEqualTo(XML
                + "<project " + NAMESPACE + ">\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <build>\n"
                + "        <plugins>\n"
                + "            <plugin>\n"
                + "                <groupId>org.apache.maven.plugins</groupId>\n"
                + "            <artifactId>maven-jar-plugin</artifactId>\n"
                + "            <version>2.4</version>\n"
                + "            <configuration>\n"
                + "                    <archive>\n"
                + "                        <addMavenDescriptor>false</addMavenDescriptor>\n"
                + "                    </archive>\n"
                + "                </configuration>\n"
                + "            </plugin>\n"
                + "        </plugins>\n"
                + "    </build>\n"
                + "</project>\n");
    }


    @Test
    public void shouldExpandDependencyManagement() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <dependencyManagement>\n"
                + "        <dependencies>\n"
                + "            <pom>org.jboss.arquillian:arquillian-bom:1.1.11.Final</pom>\n"
                + "        </dependencies>\n"
                + "    </dependencyManagement>\n"
                + "</project>\n");

        assertThat(pom.asString()).isEqualTo(XML
                + "<project " + NAMESPACE + ">\n" + WARNING
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


    @Test
    public void shouldExpandTestDependency() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <dependencies>\n"
                + "        <test>\n"
                + "            <jar>junit:junit:4.12</jar>\n"
                + "        </test>\n"
                + "    </dependencies>\n"
                + "</project>\n");

        assertThat(pom.asString()).isEqualTo(XML
                + "<project " + NAMESPACE + ">\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <dependencies>\n"
                + "        <dependency>\n"
                + "        <groupId>junit</groupId>\n"
                + "        <artifactId>junit</artifactId>\n"
                + "        <version>4.12</version>\n"
                + "        <scope>test</scope>\n"
                + "    </dependency>\n"
                + "    </dependencies>\n"
                + "</project>\n");
    }

    @Test
    public void shouldExpandTwoTestDependencies() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <dependencies>\n"
                + "        <test>\n"
                + "            <jar>junit:junit:4.12</jar>\n"
                + "            <jar>org.assertj:assertj-core:3.6.1</jar>\n"
                + "        </test>\n"
                + "    </dependencies>\n"
                + "</project>\n");

        assertThat(pom.asString()).isEqualTo(XML
                + "<project " + NAMESPACE + ">\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <dependencies>\n"
                + "        <dependency>\n"
                + "        <groupId>junit</groupId>\n"
                + "        <artifactId>junit</artifactId>\n"
                + "        <version>4.12</version>\n"
                + "        <scope>test</scope>\n"
                + "    </dependency>\n"
                + "    <dependency>\n"
                + "        <groupId>org.assertj</groupId>\n"
                + "        <artifactId>assertj-core</artifactId>\n"
                + "        <version>3.6.1</version>\n"
                + "        <scope>test</scope>\n"
                + "    </dependency>\n"
                + "    </dependencies>\n"
                + "</project>\n");
    }

    @Test
    public void shouldExpandTwoScopeDependencies() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project " + NAMESPACE + ">\n"
                + "    <dependencies>\n"
                + "        <provided>\n"
                + "            <jar>org.projectlombok:lombok:1.16.12</jar>\n"
                + "        </provided>\n"
                + "        <test>\n"
                + "            <jar>org.assertj:assertj-core:3.6.1</jar>\n"
                + "        </test>\n"
                + "    </dependencies>\n"
                + "</project>\n");

        assertThat(pom.asString()).isEqualTo(XML
                + "<project " + NAMESPACE + ">\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <dependencies>\n"
                + "        <dependency>\n"
                + "        <groupId>org.projectlombok</groupId>\n"
                + "        <artifactId>lombok</artifactId>\n"
                + "        <version>1.16.12</version>\n"
                + "        <scope>provided</scope>\n"
                + "    </dependency>\n"
                + "        <dependency>\n"
                + "        <groupId>org.assertj</groupId>\n"
                + "        <artifactId>assertj-core</artifactId>\n"
                + "        <version>3.6.1</version>\n"
                + "        <scope>test</scope>\n"
                + "    </dependency>\n"
                + "    </dependencies>\n"
                + "</project>\n");
    }
}

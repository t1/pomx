package com.github.t1.pomx;

import com.github.t1.xml.Xml;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.t1.pomx.PomxModelLocator.REPOSITORY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.contentOf;

class ProjectObjectModelTest {
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String JDK8_NS = ""
        + "xmlns=\"http://maven.apache.org/POM/4.0.0\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"";
    private static final String JDK9_NS = ""
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"";
    private static final String NS = (isJdk9() ? JDK9_NS : JDK8_NS);
    private static final String WARNING = warning(isJdk9() ? "nil:- -" : "nil:--");
    private static final String HEAD = XML + "\n<project " + NS + ">\n" + WARNING;

    private static String warning(Object source) {
        return ""
            + "    <!-- WARNING: Do Not Modify This File! -->\n"
            + "    <!-- Generated from " + source + " -->\n";
    }

    private static final String DUMMY_GAV = ""
        + "    <groupId>dummy-group</groupId>\n"
        + "    <artifactId>dummy-artifact</artifactId>\n"
        + "    <version>1.2.3-SNAPSHOT</version>\n";

    private static final Path TEST_REPO = Paths.get("src/test/resources/repository");

    private static boolean isJdk9() {
        return Double.parseDouble(System.getProperty("java.specification.version")) >= 9.0;
    }

    private Path resolve(GAV gav, String type) { return TEST_REPO.resolve(gav.asPath(type)); }


    @Test void shouldLeaveRealPomMoreOrLessAsIs() {
        Path path = Paths.get("pom.xml");
        ProjectObjectModel pom = ProjectObjectModel.readFrom(path, this::resolve);

        String xml = pom.asString();

        assertThat(xml
            .replace(warning("pom.xml"), "")
            .replace("<project " + NS + ">", "<project\n" +
                "        xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "        xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">")
        ).isEqualTo(contentOf(new File("pom.xml")));
    }

    @Test void shouldWritePom() throws Exception {
        String folder = "src/test/java/com/github/t1/pomx/";
        ProjectObjectModel pom = ProjectObjectModel.readFrom(Paths.get(folder + "input-pomx.xml"),
            (gav, type) -> REPOSITORY.resolve(gav.asPath(type)));
        Path target = Paths.get("target/test-pom.xml");
        try {
            pom.writeTo(target);

            assertThat(contentOf(target.toFile()))
                .isEqualTo(contentOf(new File(folder + "expected-pom.xml"))
                    .replace(JDK8_NS, NS));
        } finally {
            Files.deleteIfExists(target);
        }
    }

    @Test void shouldConvertNamespaceVersionAndAddModelVersion() {
        ProjectObjectModel pom = ProjectObjectModel.from(Xml.fromString(XML
            + "<project xmlns=\"urn:xsd:maven:pomx:5.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"urn:xsd:maven:pomx:5.0.0 "
            + "https://raw.githubusercontent.com/t1/pomx/master/src/main/resources/schemas/pomx-5.0.0.xsd\">\n"
            + "</project>\n"), this::resolve);

        assertThat(pom.asString()).isEqualTo((XML
            + "\n<project " + NS + ">\n"
            + WARNING)
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "</project>\n");
    }

    @Test void shouldNotAddModelVersionIfAlreadyThere() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "</project>\n");
    }

    @Test void shouldExpandJarGAV() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>\n"
            + "</project>\n");
    }

    @Test void shouldExpandPomGAV() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <pom>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</pom>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>pom</packaging>\n"
            + "</project>\n");
    }

    @Test void shouldExpandWarGAV() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <war>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</war>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>war</packaging>\n"
            + "</project>\n");
    }

    @Test void shouldFailToExpandMultipleGAV() {
        Throwable throwable = catchThrowable(() -> ProjectObjectModel
            .from(XML
                + "<project>\n"
                + "    <war>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</war>\n"
                + "    <war>dummy-group2:dummy-artifact2:1.2.3-SNAPSHOT</war>\n"
                + "</project>\n", this::resolve)
            .asString());

        assertThat(throwable).hasMessageContaining("multiple packagings found");
    }

    @Test void shouldFailToExpandGavWithOneItem() {
        Throwable throwable = catchThrowable(() -> ProjectObjectModel
            .from(XML
                + "<project>\n"
                + "    <war>dummy-group</war>\n"
                + "</project>\n", this::resolve)
            .asString());

        assertThat(throwable).hasMessageContaining("too few elements 1 in GAV expression: 'dummy-group'");
    }

    @Test void shouldExpandGavWithTwoItems() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <war>dummy-group:dummy-artifact</war>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <groupId>dummy-group</groupId>\n"
            + "    <artifactId>dummy-artifact</artifactId>\n"
            + "    <packaging>war</packaging>\n"
            + "</project>\n");
    }

    @Test void shouldExpandGACV() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <war>dummy-group:dummy-artifact:mac-os:1.2.3-SNAPSHOT</war>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <groupId>dummy-group</groupId>\n"
            + "    <artifactId>dummy-artifact</artifactId>\n"
            + "    <version>1.2.3-SNAPSHOT</version>\n"
            + "    <classifier>mac-os</classifier>\n"
            + "    <packaging>war</packaging>\n"
            + "</project>\n");
    }

    @Test void shouldFailToExpandGavWithFiveItems() {
        Throwable throwable = catchThrowable(() -> ProjectObjectModel
            .from(XML
                + "<project>\n"
                + "    <war>dummy-group:dummy-artifact:mac-os:1.2.3-SNAPSHOT:too-much</war>\n"
                + "</project>\n", this::resolve)
            .asString());

        assertThat(throwable).hasMessageContaining(
            "too many elements 5 in GAV expression: 'dummy-group:dummy-artifact:mac-os:1.2.3-SNAPSHOT:too-much'");
    }


    @Test void shouldExpandBuildPlugin() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
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
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <build>\n"
            + "        <plugins>\n"
            + "            <plugin>\n"
            + "                <groupId>org.apache.maven.plugins</groupId>\n"
            + "                <artifactId>maven-jar-plugin</artifactId>\n"
            + "                <version>2.4</version>\n"
            + "                <configuration>\n"
            + "                    <archive>\n"
            + "                        <addMavenDescriptor>false</addMavenDescriptor>\n"
            + "                    </archive>\n"
            + "                </configuration>\n"
            + "            </plugin>\n"
            + "        </plugins>\n"
            + "    </build>\n"
            + "</project>\n");
    }

    @Test void shouldAddPluginDependencies() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <build>\n"
            + "        <plugins>\n"
            + "            <plugin id=\"org.apache.maven.plugins:maven-surefire-plugin:2.22.1\">\n" +
            "                <dependencies>\n" +
            "                    <jar>org.junit.platform:junit-platform-surefire-provider:1.2.0</jar>\n" +
            "                    <jar>org.junit.jupiter:junit-jupiter-engine:${junit.version}</jar>\n" +
            "                </dependencies>\n" +
            "            </plugin>\n"
            + "        </plugins>\n"
            + "    </build>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <build>\n"
            + "        <plugins>\n"
            + "            <plugin>\n"
            + "                <groupId>org.apache.maven.plugins</groupId>\n"
            + "                <artifactId>maven-surefire-plugin</artifactId>\n"
            + "                <version>2.22.1</version>\n"
            + "                <dependencies>\n"
            + "                    <dependency>\n"
            + "                        <groupId>org.junit.platform</groupId>\n"
            + "                        <artifactId>junit-platform-surefire-provider</artifactId>\n"
            + "                        <version>1.2.0</version>\n"
            + "                    </dependency>\n"
            + "                    <dependency>\n"
            + "                        <groupId>org.junit.jupiter</groupId>\n"
            + "                        <artifactId>junit-jupiter-engine</artifactId>\n"
            + "                        <version>${junit.version}</version>\n"
            + "                    </dependency>\n"
            + "                </dependencies>\n"
            + "            </plugin>\n"
            + "        </plugins>\n"
            + "    </build>\n"
            + "</project>\n");
    }

    @Test void shouldExpandDependencyManagement() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <dependencyManagement>\n"
            + "        <pom>org.jboss.arquillian:arquillian-bom:1.1.11.Final</pom>\n"
            + "    </dependencyManagement>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <dependencyManagement>\n"
            + "        <dependencies>\n"
            + "            <dependency>\n"
            + "                <groupId>org.jboss.arquillian</groupId>\n"
            + "                <artifactId>arquillian-bom</artifactId>\n"
            + "                <version>1.1.11.Final</version>\n"
            + "                <scope>import</scope>\n"
            + "                <type>pom</type>\n"
            + "            </dependency>\n"
            + "        </dependencies>\n"
            + "    </dependencyManagement>\n"
            + "</project>\n");
    }


    @Test void shouldExpandTestDependency() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <dependencies>\n"
            + "        <test>\n"
            + "            <jar>junit:junit:4.12</jar>\n"
            + "        </test>\n"
            + "    </dependencies>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <dependencies>\n"
            + "        <dependency>\n"
            + "            <groupId>junit</groupId>\n"
            + "            <artifactId>junit</artifactId>\n"
            + "            <version>4.12</version>\n"
            + "            <scope>test</scope>\n"
            + "        </dependency>\n"
            + "    </dependencies>\n"
            + "</project>\n");
    }

    @Test void shouldExpandTwoTestDependencies() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <dependencies>\n"
            + "        <test>\n"
            + "            <jar>junit:junit:4.12</jar>\n"
            + "            <jar>org.assertj:assertj-core:3.6.1</jar>\n"
            + "        </test>\n"
            + "    </dependencies>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <dependencies>\n"
            + "        <dependency>\n"
            + "            <groupId>junit</groupId>\n"
            + "            <artifactId>junit</artifactId>\n"
            + "            <version>4.12</version>\n"
            + "            <scope>test</scope>\n"
            + "        </dependency>\n"
            + "        <dependency>\n"
            + "            <groupId>org.assertj</groupId>\n"
            + "            <artifactId>assertj-core</artifactId>\n"
            + "            <version>3.6.1</version>\n"
            + "            <scope>test</scope>\n"
            + "        </dependency>\n"
            + "    </dependencies>\n"
            + "</project>\n");
    }

    @Test void shouldExpandTwoScopeDependencies() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <dependencies>\n"
            + "        <provided>\n"
            + "            <jar>org.projectlombok:lombok:1.16.12</jar>\n"
            + "        </provided>\n"
            + "        <test>\n"
            + "            <jar>org.assertj:assertj-core:3.6.1</jar>\n"
            + "        </test>\n"
            + "    </dependencies>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <dependencies>\n"
            + "        <dependency>\n"
            + "            <groupId>org.projectlombok</groupId>\n"
            + "            <artifactId>lombok</artifactId>\n"
            + "            <version>1.16.12</version>\n"
            + "            <scope>provided</scope>\n"
            + "        </dependency>\n"
            + "        <dependency>\n"
            + "            <groupId>org.assertj</groupId>\n"
            + "            <artifactId>assertj-core</artifactId>\n"
            + "            <version>3.6.1</version>\n"
            + "            <scope>test</scope>\n"
            + "        </dependency>\n"
            + "    </dependencies>\n"
            + "</project>\n");
    }

    @Test void shouldExpandPomDependency() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <dependencies>\n"
            + "        <provided>\n"
            + "            <pom>org.eclipse.microprofile:microprofile:2.2</pom>\n"
            + "        </provided>\n"
            + "    </dependencies>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <dependencies>\n"
            + "        <dependency>\n"
            + "            <groupId>org.eclipse.microprofile</groupId>\n"
            + "            <artifactId>microprofile</artifactId>\n"
            + "            <version>2.2</version>\n"
            + "            <type>pom</type>\n"
            + "            <scope>provided</scope>\n"
            + "        </dependency>\n"
            + "    </dependencies>\n"
            + "</project>\n");
    }

    @Test void shouldExpandExternalProfile() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:dummy-profile:1.0</profile>"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:dummy-profile</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.dummy-profile.version>1.0</dummy-group.dummy-profile.version>\n"
            + "            </properties>\n"
            + "            <build>\n"
            + "                <finalName>xxx</finalName>\n"
            + "            </build>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }

    @Test void shouldCopyOneLicenseFromExternalProfile() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:profile-with-license:1.0</profile>"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>\n"
            + "    <licenses>\n"
            + "        <license>\n"
            + "            <name>Apache License 2.0</name>\n"
            + "            <url>https://www.apache.org/licenses/LICENSE-2.0.html</url>\n"
            + "            <distribution>repo</distribution>\n"
            + "        </license>\n"
            + "    </licenses>\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:profile-with-license</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.profile-with-license.version>1.0</dummy-group.profile-with-license.version>\n"
            + "            </properties>\n"
            + "            <build>\n"
            + "                <finalName>xxx</finalName>\n"
            + "            </build>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }

    @Test void shouldAddLicenseFromExternalProfile() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:profile-with-license:1.0</profile>"
            + "    <licenses>\n"
            + "        <license>\n"
            + "            <name>MIT</name>\n"
            + "            <distribution>repo</distribution>\n"
            + "        </license>\n"
            + "    </licenses>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>"
            + "    <licenses>\n"
            + "        <license>\n"
            + "            <name>MIT</name>\n"
            + "            <distribution>repo</distribution>\n"
            + "        </license>\n"
            + "        <license>\n"
            + "            <name>Apache License 2.0</name>\n"
            + "            <url>https://www.apache.org/licenses/LICENSE-2.0.html</url>\n"
            + "            <distribution>repo</distribution>\n"
            + "        </license>\n"
            + "    </licenses>\n"
            + "\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:profile-with-license</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.profile-with-license.version>1.0</dummy-group.profile-with-license.version>\n"
            + "            </properties>\n"
            + "            <build>\n"
            + "                <finalName>xxx</finalName>\n"
            + "            </build>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }

    @Test void shouldCopyTwoLicensesFromExternalProfile() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:profile-with-two-licenses:1.0</profile>"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>\n"
            + "    <licenses>\n"
            + "        <license>\n"
            + "            <name>MIT</name>\n"
            + "            <distribution>repo</distribution>\n"
            + "        </license>\n"
            + "        <license>\n"
            + "            <name>Apache License 2.0</name>\n"
            + "            <url>https://www.apache.org/licenses/LICENSE-2.0.html</url>\n"
            + "            <distribution>repo</distribution>\n"
            + "        </license>\n"
            + "    </licenses>\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:profile-with-two-licenses</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.profile-with-two-licenses.version>1.0</dummy-group.profile-with-two-licenses.version>\n"
            + "            </properties>\n"
            + "            <build>\n"
            + "                <finalName>xxx</finalName>\n"
            + "            </build>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }

    @Test void shouldCopyOneRepositoryFromExternalProfile() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:profile-with-repository:1.0</profile>"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>\n"
            + "    <repositories>\n"
            + "        <repository>\n"
            + "            <id>central</id>\n"
            + "            <name>bintray</name>\n"
            + "            <url>https://jcenter.bintray.com</url>\n"
            + "        </repository>\n"
            + "    </repositories>\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:profile-with-repository</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.profile-with-repository.version>1.0</dummy-group.profile-with-repository.version>\n"
            + "            </properties>\n"
            + "            <build>\n"
            + "                <finalName>xxx</finalName>\n"
            + "            </build>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }

    @Test void shouldAddRepositoryFromExternalProfile() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:profile-with-repository:1.0</profile>"
            + "    <repositories>\n"
            + "        <repository>\n"
            + "            <id>other</id>\n"
            + "            <name>other</name>\n"
            + "        </repository>\n"
            + "    </repositories>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>"
            + "    <repositories>\n"
            + "        <repository>\n"
            + "            <id>other</id>\n"
            + "            <name>other</name>\n"
            + "        </repository>\n"
            + "        <repository>\n"
            + "            <id>central</id>\n"
            + "            <name>bintray</name>\n"
            + "            <url>https://jcenter.bintray.com</url>\n"
            + "        </repository>\n"
            + "    </repositories>\n"
            + "\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:profile-with-repository</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.profile-with-repository.version>1.0</dummy-group.profile-with-repository.version>\n"
            + "            </properties>\n"
            + "            <build>\n"
            + "                <finalName>xxx</finalName>\n"
            + "            </build>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }

    @Test void shouldCopyTwoRepositoriesFromExternalProfile() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:profile-with-two-repositories:1.0</profile>"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>\n"
            + "    <repositories>\n"
            + "        <repository>\n"
            + "            <id>other</id>\n"
            + "            <name>other</name>\n"
            + "        </repository>\n"
            + "        <repository>\n"
            + "            <id>central</id>\n"
            + "            <name>bintray</name>\n"
            + "            <url>https://jcenter.bintray.com</url>\n"
            + "        </repository>\n"
            + "    </repositories>\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:profile-with-two-repositories</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.profile-with-two-repositories.version>1.0</dummy-group.profile-with-two-repositories.version>\n"
            + "            </properties>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }

    @Test void shouldCopyDistributionManagementFromExternalProfile() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:profile-with-distributionManagement:1.0</profile>"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>\n"
            + "    <distributionManagement>\n"
            + "        <repository>\n"
            + "            <id>central</id>\n"
            + "            <name>bintray</name>\n"
            + "            <url>https://jcenter.bintray.com</url>\n"
            + "        </repository>\n"
            + "    </distributionManagement>\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:profile-with-distributionManagement</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.profile-with-distributionManagement.version>1.0</dummy-group.profile-with-distributionManagement.version>\n"
            + "            </properties>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }

    @Test void shouldMergeDistributionManagementFromExternalProfile() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:profile-with-distributionManagement:1.0</profile>"
            + "    <distributionManagement>\n"
            + "        <downloadUrl>http://some.where</downloadUrl>\n"
            + "    </distributionManagement>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>"
            + "    <distributionManagement>\n"
            + "        <downloadUrl>http://some.where</downloadUrl>\n"
            + "        <repository>\n"
            + "            <id>central</id>\n"
            + "            <name>bintray</name>\n"
            + "            <url>https://jcenter.bintray.com</url>\n"
            + "        </repository>\n"
            + "    </distributionManagement>\n"
            + "\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:profile-with-distributionManagement</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.profile-with-distributionManagement.version>1.0</dummy-group.profile-with-distributionManagement.version>\n"
            + "            </properties>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }

    @Test void shouldCopyScmFromExternalProfile() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:profile-with-scm:1.0</profile>"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>\n"
            + "    <scm>\n"
            + "        <developerConnection>scm:git:https://github.com/t1/${project.artifactId}</developerConnection>\n"
            + "        <tag>HEAD</tag>\n"
            + "    </scm>\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:profile-with-scm</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.profile-with-scm.version>1.0</dummy-group.profile-with-scm.version>\n"
            + "            </properties>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }

    @Test void shouldMergeScmFromExternalProfile() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:profile-with-scm:1.0</profile>"
            + "    <scm>\n"
            + "        <url>http://some.where</url>\n"
            + "    </scm>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>"
            + "    <scm>\n"
            + "        <url>http://some.where</url>\n"
            + "        <developerConnection>scm:git:https://github.com/t1/${project.artifactId}</developerConnection>\n"
            + "        <tag>HEAD</tag>\n"
            + "    </scm>\n"
            + "\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:profile-with-scm</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.profile-with-scm.version>1.0</dummy-group.profile-with-scm.version>\n"
            + "            </properties>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }

    @Test void shouldAddNestedExternalProfile() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:profile-with-nested-profile:1.0</profile>"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>"
            + "\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:profile-with-nested-profile</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.profile-with-nested-profile.version>1.0</dummy-group.profile-with-nested-profile.version>\n"
            + "            </properties>\n"
            + "        </profile>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:dummy-profile</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.dummy-profile.version>1.0</dummy-group.dummy-profile.version>\n"
            + "            </properties>\n"
            + "            <build>\n"
            + "                <finalName>xxx</finalName>\n"
            + "            </build>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }

    @Test void shouldAddNestedDependencyInPlugin() {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
            + "<project>\n"
            + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
            + "    <profile>dummy-group:profile-with-nested-dependency-in-plugin:1.0</profile>\n"
            + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(HEAD
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + DUMMY_GAV
            + "    <packaging>jar</packaging>\n"
            + "\n"
            + "    <profiles>\n"
            + "        <profile>\n"
            + "            <id>dummy-group:profile-with-nested-dependency-in-plugin</id>\n"
            + "            <activation>\n"
            + "                <property>\n"
            + "                    <name>user.dir</name>\n"
            + "                </property>\n"
            + "            </activation>\n"
            + "            <properties>\n"
            + "                <dummy-group.profile-with-nested-dependency-in-plugin.version>1.0</dummy-group.profile-with-nested-dependency-in-plugin.version>\n"
            + "            </properties>\n"
            + "            <build>\n"
            + "                <plugins>\n"
            + "            <plugin>\n"
            + "                <groupId>org.apache.maven.plugins</groupId>\n"
            + "                <artifactId>maven-enforcer-plugin</artifactId>\n"
            + "                <version>3.0.0-M2</version>\n"
            + "                <dependencies>\n"
            + "                    <dependency>\n"
            + "                        <groupId>org.sonatype.ossindex.maven</groupId>\n"
            + "                        <artifactId>ossindex-maven-enforcer-rules</artifactId>\n"
            + "                        <version>1.0.0</version>\n"
            + "                    </dependency>\n"
            + "                </dependencies>\n"
            + "                <executions>\n"
            + "                    <execution>\n"
            + "                        <phase>validate</phase>\n"
            + "                        <goals>\n"
            + "                            <goal>enforce</goal>\n"
            + "                        </goals>\n"
            + "                        <configuration>\n"
            + "                            <rules>\n"
            + "                                <banVunerableDependencies implementation=\"org.sonatype.ossindex.maven.enforcer.BanVulnerableDependencies\"/>\n"
            + "                            </rules>\n"
            + "                        </configuration>\n"
            + "                    </execution>\n"
            + "                </executions>\n"
            + "            </plugin>\n"
            + "        </plugins>\n"
            + "            </build>\n"
            + "            <dependencies>\n"
            + "                <dependency>\n"
            + "            <groupId>org.junit.jupiter</groupId>\n"
            + "            <artifactId>junit-jupiter-api</artifactId>\n"
            + "            <version>5.2.0</version>\n"
            + "            <scope>test</scope>\n"
            + "        </dependency>\n"
            + "            </dependencies>\n"
            + "        </profile>\n"
            + "    </profiles>\n"
            + "</project>\n");
    }
}

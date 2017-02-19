package com.github.t1.pomx;

import com.github.t1.xml.Xml;
import org.junit.Test;

import java.io.File;
import java.nio.file.*;

import static com.github.t1.pomx.PomxModelLocator.*;
import static org.assertj.core.api.Assertions.*;

public class ProjectObjectModelTest {
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    private static final String WARNING = warning("nil:--");
    private static final String DUMMY_GAV = ""
            + "    <groupId>dummy-group</groupId>\n"
            + "    <artifactId>dummy-artifact</artifactId>\n"
            + "    <version>1.2.3-SNAPSHOT</version>\n";
    private static final Path TEST_REPO = Paths.get("src/test/resources/repository");

    private static String warning(Object source) {
        return ""
                + "    <!-- WARNING: Do Not Modify This File! -->\n"
                + "    <!-- Generated from " + source + " -->\n";
    }

    private Path resolve(GAV gav, String type) { return TEST_REPO.resolve(gav.asPath(type)); }


    @Test
    public void shouldLeaveRealPomMoreOrLessAsIs() throws Exception {
        Path path = Paths.get("pom.xml");
        ProjectObjectModel pom = ProjectObjectModel.readFrom(path, this::resolve);

        String xml = pom.asString();

        assertThat(xml).isEqualTo(contentOf(new File("pom.xml"))
                .replace("\" ?>", "\"?>")
                .replace(warning("pomx.xml"), warning("pom.xml") + warning("pomx.xml")));
    }

    @Test
    public void shouldWritePom() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.readFrom(Paths.get("pomx.xml"),
                (gav, type) -> REPOSITORY.resolve(gav.asPath(type)));
        Path target = Paths.get("target/test-pom.xml");
        try {
            pom.writeTo(target);

            assertThat(contentOf(target.toFile())).isEqualTo(contentOf(new File("pom.xml")));
        } finally {
            Files.deleteIfExists(target);
        }
    }

    @Test
    public void shouldConvertNamespaceVersionAndAddModelVersion() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(Xml.fromString(XML
                + "<project xmlns=\"urn:xsd:maven:pomx:5.0.0\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"urn:xsd:maven:pomx:5.0.0 "
                + "https://raw.githubusercontent.com/t1/pomx/master/src/main/resources/schemas/pomx-5.0.0.xsd\">\n"
                + "</project>\n"), this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "</project>\n");
    }

    @Test
    public void shouldNotAddModelVersionIfAlreadyThere() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "</project>\n");
    }

    @Test
    public void shouldExpandJarGAV() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + DUMMY_GAV
                + "    <packaging>jar</packaging>\n"
                + "</project>\n");
    }

    @Test
    public void shouldExpandPomGAV() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <pom>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</pom>\n"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + DUMMY_GAV
                + "    <packaging>pom</packaging>\n"
                + "</project>\n");
    }

    @Test
    public void shouldExpandWarGAV() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <war>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</war>\n"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + DUMMY_GAV
                + "    <packaging>war</packaging>\n"
                + "</project>\n");
    }

    @Test
    public void shouldFailToExpandMultipleGAV() throws Exception {
        Throwable throwable = catchThrowable(() -> ProjectObjectModel
                .from(XML
                        + "<project>\n"
                        + "    <war>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</war>\n"
                        + "    <war>dummy-group2:dummy-artifact2:1.2.3-SNAPSHOT</war>\n"
                        + "</project>\n", this::resolve)
                .asString());

        assertThat(throwable).hasMessageContaining("multiple packagings found");
    }

    @Test
    public void shouldFailToExpandGavWithOneItem() throws Exception {
        Throwable throwable = catchThrowable(() -> ProjectObjectModel
                .from(XML
                        + "<project>\n"
                        + "    <war>dummy-group</war>\n"
                        + "</project>\n", this::resolve)
                .asString());

        assertThat(throwable).hasMessageContaining("too few elements 1 in GAV expression: 'dummy-group'");
    }

    @Test
    public void shouldExpandGavWithTwoItems() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <war>dummy-group:dummy-artifact</war>\n"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <groupId>dummy-group</groupId>\n"
                + "    <artifactId>dummy-artifact</artifactId>\n"
                + "    <packaging>war</packaging>\n"
                + "</project>\n");
    }

    @Test
    public void shouldExpandGACV() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <war>dummy-group:dummy-artifact:mac-os:1.2.3-SNAPSHOT</war>\n"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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
                        + "<project>\n"
                        + "    <war>dummy-group:dummy-artifact:mac-os:1.2.3-SNAPSHOT:too-much</war>\n"
                        + "</project>\n", this::resolve)
                .asString());

        assertThat(throwable).hasMessageContaining(
                "too many elements 5 in GAV expression: 'dummy-group:dummy-artifact:mac-os:1.2.3-SNAPSHOT:too-much'");
    }


    @Test
    public void shouldExpandBuildPlugin() throws Exception {
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

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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


    @Test
    public void shouldExpandDependencyManagement() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <dependencyManagement>\n"
                + "        <pom>org.jboss.arquillian:arquillian-bom:1.1.11.Final</pom>\n"
                + "    </dependencyManagement>\n"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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


    @Test
    public void shouldExpandTestDependency() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <dependencies>\n"
                + "        <test>\n"
                + "            <jar>junit:junit:4.12</jar>\n"
                + "        </test>\n"
                + "    </dependencies>\n"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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

    @Test
    public void shouldExpandTwoTestDependencies() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <dependencies>\n"
                + "        <test>\n"
                + "            <jar>junit:junit:4.12</jar>\n"
                + "            <jar>org.assertj:assertj-core:3.6.1</jar>\n"
                + "        </test>\n"
                + "    </dependencies>\n"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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

    @Test
    public void shouldExpandTwoScopeDependencies() throws Exception {
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

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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

    @Test
    public void shouldExpandExternalProfile() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
                + "    <profile>dummy-group:dummy-profile:1.0</profile>"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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
                + "            <build>\n"
                + "        <finalName>xxx</finalName>\n"
                + "    </build>\n"
                + "            <properties>\n"
                + "                <dummy-group.dummy-profile.version>1.0</dummy-group.dummy-profile.version>\n"
                + "            </properties>\n"
                + "        </profile>\n"
                + "    </profiles>\n"
                + "</project>\n");
    }

    @Test
    public void shouldCopyOneLicenseFromExternalProfile() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
                + "    <profile>dummy-group:profile-with-license:1.0</profile>"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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
                + "            <build>\n"
                + "        <finalName>xxx</finalName>\n"
                + "    </build>\n"
                + "            <properties>\n"
                + "                <dummy-group.profile-with-license.version>1.0</dummy-group.profile-with-license.version>\n"
                + "            </properties>\n"
                + "        </profile>\n"
                + "    </profiles>\n"
                + "</project>\n");
    }

    @Test
    public void shouldAddLicenseFromExternalProfile() throws Exception {
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

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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
                + "            <build>\n"
                + "        <finalName>xxx</finalName>\n"
                + "    </build>\n"
                + "            <properties>\n"
                + "                <dummy-group.profile-with-license.version>1.0</dummy-group.profile-with-license.version>\n"
                + "            </properties>\n"
                + "        </profile>\n"
                + "    </profiles>\n"
                + "</project>\n");
    }

    @Test
    public void shouldCopyTwoLicensesFromExternalProfile() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
                + "    <profile>dummy-group:profile-with-two-licenses:1.0</profile>"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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
                + "            <build>\n"
                + "        <finalName>xxx</finalName>\n"
                + "    </build>\n"
                + "            <properties>\n"
                + "                <dummy-group.profile-with-two-licenses.version>1.0</dummy-group.profile-with-two-licenses.version>\n"
                + "            </properties>\n"
                + "        </profile>\n"
                + "    </profiles>\n"
                + "</project>\n");
    }

    @Test
    public void shouldCopyOneRepositoryFromExternalProfile() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
                + "    <profile>dummy-group:profile-with-repository:1.0</profile>"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + DUMMY_GAV
                + "    <packaging>jar</packaging>\n"
                + "    <repositories>\n"
                + "        <repository>\n"
                + "            <id>central</id>\n"
                + "            <name>bintray</name>\n"
                + "            <url>http://jcenter.bintray.com</url>\n"
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
                + "            <build>\n"
                + "        <finalName>xxx</finalName>\n"
                + "    </build>\n"
                + "            <properties>\n"
                + "                <dummy-group.profile-with-repository.version>1.0</dummy-group.profile-with-repository.version>\n"
                + "            </properties>\n"
                + "        </profile>\n"
                + "    </profiles>\n"
                + "</project>\n");
    }

    @Test
    public void shouldAddRepositoryFromExternalProfile() throws Exception {
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

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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
                + "            <url>http://jcenter.bintray.com</url>\n"
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
                + "            <build>\n"
                + "        <finalName>xxx</finalName>\n"
                + "    </build>\n"
                + "            <properties>\n"
                + "                <dummy-group.profile-with-repository.version>1.0</dummy-group.profile-with-repository.version>\n"
                + "            </properties>\n"
                + "        </profile>\n"
                + "    </profiles>\n"
                + "</project>\n");
    }

    @Test
    public void shouldCopyTwoRepositoriesFromExternalProfile() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
                + "    <profile>dummy-group:profile-with-two-repositories:1.0</profile>"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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
                + "            <url>http://jcenter.bintray.com</url>\n"
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

    @Test
    public void shouldCopyDistributionManagementFromExternalProfile() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
                + "    <profile>dummy-group:profile-with-distributionManagement:1.0</profile>"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + DUMMY_GAV
                + "    <packaging>jar</packaging>\n"
                + "    <distributionManagement>\n"
                + "        <repository>\n"
                + "            <id>central</id>\n"
                + "            <name>bintray</name>\n"
                + "            <url>http://jcenter.bintray.com</url>\n"
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

    @Test
    public void shouldMergeDistributionManagementFromExternalProfile() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
                + "    <profile>dummy-group:profile-with-distributionManagement:1.0</profile>"
                + "    <distributionManagement>\n"
                + "        <downloadUrl>http://some.where</downloadUrl>\n"
                + "    </distributionManagement>\n"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + DUMMY_GAV
                + "    <packaging>jar</packaging>"
                + "    <distributionManagement>\n"
                + "        <downloadUrl>http://some.where</downloadUrl>\n"
                + "        <repository>\n"
                + "            <id>central</id>\n"
                + "            <name>bintray</name>\n"
                + "            <url>http://jcenter.bintray.com</url>\n"
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

    @Test
    public void shouldCopyScmFromExternalProfile() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
                + "    <profile>dummy-group:profile-with-scm:1.0</profile>"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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

    @Test
    public void shouldMergeScmFromExternalProfile() throws Exception {
        ProjectObjectModel pom = ProjectObjectModel.from(XML
                + "<project>\n"
                + "    <jar>dummy-group:dummy-artifact:1.2.3-SNAPSHOT</jar>\n"
                + "    <profile>dummy-group:profile-with-scm:1.0</profile>"
                + "    <scm>\n"
                + "        <url>http://some.where</url>\n"
                + "    </scm>\n"
                + "</project>\n", this::resolve);

        assertThat(pom.asString()).isEqualTo(XML
                + "<project>\n" + WARNING
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
}

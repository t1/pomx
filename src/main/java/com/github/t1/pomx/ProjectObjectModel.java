package com.github.t1.pomx;

import com.github.t1.xml.Xml;
import com.github.t1.xml.XmlElement;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.github.t1.xml.XmlElement.atBegin;
import static com.github.t1.xml.XmlElement.before;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

class ProjectObjectModel {
    private static final List<String> PACKAGINGS = asList("war", "jar", "pom");

    /** also in README! */
    private static final List<String> PROFILE_NO_COPY_ELEMENTS =
        asList("modelVersion", "groupId", "artifactId", "version", "packaging", "name", "description");

    /** also in README! */
    private static final List<String> PROFILE_COPY_TO_PROJECT_ELEMENTS =
        asList("licenses", "developers", "repositories", "distributionManagement", "scm", "profiles");

    private static final List<String> SCOPES = asList("provided", "compile", "runtime", "system", "test");

    private ProjectObjectModel(Resolver resolver, Xml in) {
        this.resolver = resolver;
        this.in = in;
    }


    static ProjectObjectModel from(String xml, Resolver resolver) { return from(Xml.fromString(xml), resolver); }

    static ProjectObjectModel readFrom(Path path, Resolver resolver) { return from(Xml.load(path.toUri()), resolver); }

    static ProjectObjectModel from(Xml xml, Resolver resolver) { return new ProjectObjectModel(resolver, xml); }


    private final Resolver resolver;
    private final Xml in;
    private Xml out;


    String asString() { return converted().toXmlString(); }

    private Xml converted() {
        if (out == null) {
            out = Xml.fromString(in.toXmlString());
            expand();
        }
        return out;
    }

    private void expand() {
        convertNamespace();
        expandModelVersion();
        writeGeneratedWarning();
        expandGav();
        expandBuildPlugins();
        expandDependencyManagement();
        expandDependencies();
        expandExternalProfiles();
    }

    private void convertNamespace() {
        out.setAttribute("xmlns", "http://maven.apache.org/POM/4.0.0");
        out.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        out.setAttribute("xsi:schemaLocation", "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd");
    }

    private void writeGeneratedWarning() {
        URI uri = in.uri();
        Object source = uri.getScheme().equals("file")
            ? Paths.get(System.getProperty("user.dir")).relativize(Paths.get(uri))
            : uri;
        out.addComment("Generated from " + source, atBegin());
        out.addComment("WARNING: Do Not Modify This File!", atBegin());
    }

    private void expandModelVersion() {
        Optional<XmlElement> optional = out.getOptionalElement("modelVersion");
        if (!optional.isPresent())
            out.addElement("modelVersion", atBegin()).addText("4.0.0");
    }

    private void expandBuildPlugins() {
        out.getOptionalElement("build/plugins")
            .ifPresent(plugins -> plugins
                .elements().stream()
                .filter(element -> element.getName().equals("plugin"))
                .filter(XmlElement::hasId)
                .forEach(plugin -> {
                    GAV gav = GAV.split(plugin.getAttribute("id"));
                    if (gav.getVersion() != null)
                        plugin.addElement("version", atBegin()).addText(gav.getVersion());
                    plugin.addElement("artifactId", atBegin()).addText(gav.getArtifactId());
                    plugin.addElement("groupId", atBegin()).addText(gav.getGroupId());
                    plugin.removeAttribute("id");
                    plugin.getOptionalElement("dependencies")
                        .ifPresent(dependencies ->
                            plugin.find("dependencies/*")
                                .forEach(dependency -> {
                                    GAV gav2 = GAV.split(dependency.getText());
                                    XmlElement element = dependencies.addElement("dependency");
                                    element.addElement("groupId").addText(gav2.getGroupId());
                                    element.addElement("artifactId").addText(gav2.getArtifactId());
                                    if (gav2.getVersion() != null)
                                        element.addElement("version").addText(gav2.getVersion());
                                    dependency.remove();
                                }));
                }));
    }

    private void expandGav() {
        List<XmlElement> packagings = out.find("/project/*["
            + PACKAGINGS.stream()
            .map(packaging -> "local-name()='" + packaging + "'")
            .collect(joining(" or ")) + "]");
        if (packagings.isEmpty())
            return;
        if (packagings.size() > 1)
            throw new RuntimeException("multiple packagings found");
        XmlElement packaging = packagings.get(0);
        GAV gav = GAV.split(packaging.getText());
        XmlElement project = packaging.getParent();
        project.addElement("groupId", before(packaging)).addText(gav.getGroupId());
        project.addElement("artifactId", before(packaging)).addText(gav.getArtifactId());
        if (gav.getVersion() != null)
            project.addElement("version", before(packaging)).addText(gav.getVersion());
        if (gav.getClassifier() != null)
            project.addElement("classifier", before(packaging)).addText(gav.getClassifier());
        project.addElement("packaging", before(packaging)).addText(packaging.getName());
        packaging.remove();
    }

    private void expandDependencyManagement() {
        out.getOptionalElement("dependencyManagement")
            .ifPresent(management -> management
                .find("pom")
                .forEach(dependency -> {
                    GAV gav = GAV.split(dependency.getText());
                    XmlElement element = management.getOrCreateElement("dependencies").addElement("dependency");
                    element.addElement("groupId").addText(gav.getGroupId());
                    element.addElement("artifactId").addText(gav.getArtifactId());
                    if (gav.getVersion() != null)
                        element.addElement("version").addText(gav.getVersion());
                    element.addElement("scope").addText("import");
                    element.addElement("type").addText(dependency.getName());
                    dependency.remove();
                }));
    }

    private void expandDependencies() {
        out.getOptionalElement("dependencies")
            .ifPresent(dependencies ->
                out.find("/project/dependencies/*")
                    .stream()
                    .filter(scope -> SCOPES.contains(scope.getName()))
                    .forEach(scope -> {
                        scope.find("jar|pom")
                            .forEach(dependency -> expandDependency(dependencies, scope, dependency));
                        scope.remove();
                    }));
    }

    private void expandDependency(XmlElement dependencies, XmlElement scope, XmlElement dependency) {
        GAV gav = GAV.split(dependency.getText());
        XmlElement element = dependencies.addElement("dependency", before(scope));
        element.addElement("groupId").addText(gav.getGroupId());
        element.addElement("artifactId").addText(gav.getArtifactId());
        if (gav.getVersion() != null)
            element.addElement("version").addText(gav.getVersion());
        if (dependency.getName().equals("pom"))
            element.addElement("type").addText("pom");
        element.addElement("scope").addText(scope.getName());
    }

    private void expandExternalProfiles() {
        if (!out.find("profile").isEmpty())
            out.nl();
        out.find("profile")
            .forEach(source -> {
                GAV gav = GAV.split(source.getText());
                source.remove();

                XmlElement target = out.getOrCreateElement("profiles").addElement("profile");
                target.addElement("id").addText(gav.getGroupId() + ":" + gav.getArtifactId());
                // user.dir is always set, so this activation always triggers
                // this is better than activeByDefault, which deactivates when any other profile is activated
                target.addElement("activation").addElement("property").addElement("name").addText("user.dir");

                String propertyName = gav.getGroupId() + "." + gav.getArtifactId() + ".version";
                target.getOrCreateElement("properties").addElement(propertyName, atBegin()).addText(gav.getVersion());

                List<XmlElement> elements = readFrom(resolver.resolve(gav, "xml"), resolver).converted().elements();
                elements.stream()
                    .filter(element -> !PROFILE_NO_COPY_ELEMENTS.contains(element.getName()))
                    .forEach(element -> move(element, target));
            });
    }

    private void move(XmlElement element, XmlElement target) {
        XmlElement parent = PROFILE_COPY_TO_PROJECT_ELEMENTS.contains(element.getName())
            ? out.getOrCreateElement(element.getName(), before("profiles"))
            : target.getOrCreateElement(element.getName());
        for (XmlElement sub : element.elements())
            parent.addNode(sub);
    }


    void writeTo(Path path) {
        try {
            Files.write(path, asString().getBytes(UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

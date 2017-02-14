package com.github.t1.pomx;

import com.github.t1.xml.*;
import com.github.t1.xml.XmlElement;
import lombok.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import static com.github.t1.xml.XmlElement.*;
import static java.nio.charset.StandardCharsets.*;
import static java.util.Arrays.*;

@RequiredArgsConstructor
class ProjectObjectModel {
    private static final List<String> SCOPES = asList("provided", "compile", "runtime", "system", "test");

    static ProjectObjectModel from(String xml) { return new ProjectObjectModel(Xml.fromString(xml)); }

    static ProjectObjectModel readFrom(Path path) { return new ProjectObjectModel(Xml.load(path.toUri())); }

    private final Xml in;
    private Xml out;

    String asString() {
        return converted().toXmlString();
    }

    private Xml converted() {
        if (out == null) {
            out = Xml.fromString(in.toXmlString());
            expand();
        }
        return out;
    }

    private void expand() {
        writeGeneratedWarning();
        expandModelVersion();
        expandGav();
        expandBuildPlugins();
        expandDependencyManagement();
        expandDependencies();
    }

    private void writeGeneratedWarning() {
        out.addComment("WARNING: Do Not Modify This File!", atBegin());
        URI uri = in.uri();
        Object source = uri.getScheme().equals("file")
                ? Paths.get(System.getProperty("user.dir")).relativize(Paths.get(uri))
                : uri;
        out.addComment("Generated from " + source, atBegin());
    }

    private void expandModelVersion() {
        Optional<XmlElement> optional = out.getOptionalElement("modelVersion");
        if (!optional.isPresent())
            out.addElement("modelVersion", atBegin()).addText("4.0.0");
    }

    private void expandBuildPlugins() {
        out.find("/project/build/plugins")
           .forEach(plugins -> plugins
                   .find("plugin")
                   .forEach(plugin -> {
                       if (plugin.hasAttribute("id")) {
                           GAV gav = GAV.split(plugin.getAttribute("id"));
                           plugin.addElement("version", atBegin()).addText(gav.getVersion());
                           plugin.addElement("artifactId", atBegin()).addText(gav.getArtifactId());
                           plugin.addElement("groupId", atBegin()).addText(gav.getGroupId());
                           plugin.removeAttribute("id");
                       }
                   }));
    }

    private void expandGav() {
        List<XmlElement> packagings = out.find("/project/*[local-name()='war' or local-name()='jar']");
        if (packagings.isEmpty())
            return;
        if (packagings.size() > 1)
            throw new RuntimeException("multiple packagings found");
        XmlElement packaging = packagings.get(0);
        GAV gav = GAV.split(packaging.getText());
        XmlElement project = packaging.getParent();
        project.addElement("groupId", before(packaging)).addText(gav.getGroupId());
        project.addElement("artifactId", before(packaging)).addText(gav.getArtifactId());
        project.addElement("version", before(packaging)).addText(gav.getVersion());
        if (gav.getClassifier() != null)
            project.addElement("classifier", before(packaging)).addText(gav.getClassifier());
        project.addElement("packaging", before(packaging)).addText(packaging.getName());
        packaging.remove();
    }

    private void expandDependencyManagement() {
        out.find("/project/dependencyManagement/dependencies")
           .forEach(dependencies -> dependencies
                   .find("pom")
                   .forEach(dependency -> {
                       GAV gav = GAV.split(dependency.getText());
                       XmlElement element = dependencies.addElement("dependency", before(dependency));
                       element.addElement("groupId").addText(gav.getGroupId());
                       element.addElement("artifactId").addText(gav.getArtifactId());
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
                          scope.find("jar")
                               .forEach(dependency -> {
                                   GAV gav = GAV.split(dependency.getText());
                                   XmlElement element = dependencies.addElement("dependency", before(scope));
                                   element.addElement("groupId").addText(gav.getGroupId());
                                   element.addElement("artifactId").addText(gav.getArtifactId());
                                   element.addElement("version").addText(gav.getVersion());
                                   element.addElement("scope").addText(scope.getName());
                               });
                          scope.remove();
                      }));
    }

    @SneakyThrows(IOException.class)
    void writeTo(Path path) {
        Files.write(path, asString().getBytes(UTF_8));
    }
}

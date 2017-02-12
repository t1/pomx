package com.github.t1.pomx;

import com.github.t1.xml.*;
import com.github.t1.xml.XmlElement;
import lombok.*;

import java.nio.file.Path;
import java.util.List;

import static com.github.t1.xml.XmlElement.*;
import static java.util.stream.Collectors.*;

@RequiredArgsConstructor
class ProjectObjectModel {
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
            expandModelVersion();
            expandGav();
        }
        return out;
    }

    private void expandModelVersion() {
        out.addElement("modelVersion", atBegin()).addText("4.0.0");
    }

    private void expandGav() {
        List<XmlElement> packagings = out.elements().stream()
                                         .filter(element -> element.getName().equals("war"))
                                         .collect(toList());
        if (packagings.isEmpty())
            return;
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

    @Value
    private static class GAV {
        String groupId, artifactId, classifier, version;

        private static GAV split(String expression) {
            String[] split = expression.split(":", 10);
            switch (split.length) {
            case 0:
            case 1:
            case 2:
                throw new IllegalArgumentException(
                        "too few elements " + split.length + " in GAV expression: '" + expression + "'");
            case 3:
                return new GAV(split[0], split[1], null, split[2]);
            case 4:
                return new GAV(split[0], split[1], split[2], split[3]);
            default:
                throw new IllegalArgumentException(
                        "too many elements " + split.length + " in GAV expression: '" + expression + "'");
            }
        }
    }
}

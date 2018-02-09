package com.github.t1.pomx;

import java.nio.file.*;
import java.util.Objects;

class GAV {
    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String version;

    GAV(String groupId, String artifactId, String classifier, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.version = version;
    }

    static GAV split(String expression) {
        String[] split = expression.split(":", 10);
        switch (split.length) {
        case 0:
        case 1:
            throw new IllegalArgumentException(
                    "too few elements " + split.length + " in GAV expression: '" + expression + "'");
        case 2:
            return new GAV(split[0], split[1], null, null);
        case 3:
            return new GAV(split[0], split[1], null, split[2]);
        case 4:
            return new GAV(split[0], split[1], split[2], split[3]);
        default:
            throw new IllegalArgumentException(
                    "too many elements " + split.length + " in GAV expression: '" + expression + "'");
        }
    }

    Path asPath(String type) {
        return Paths.get(groupId.replace('.', '/'))
                    .resolve(artifactId)
                    .resolve(version)
                    .resolve(artifactId + "-" + version + "." + type);
    }

    public String getGroupId() { return groupId; }

    public String getArtifactId() { return artifactId; }

    public String getClassifier() { return classifier; }

    public String getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GAV gav = (GAV) o;
        return Objects.equals(groupId, gav.groupId) &&
                Objects.equals(artifactId, gav.artifactId) &&
                Objects.equals(classifier, gav.classifier) &&
                Objects.equals(version, gav.version);
    }

    @Override
    public int hashCode() { return Objects.hash(groupId, artifactId, classifier, version); }
}

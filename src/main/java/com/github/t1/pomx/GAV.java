package com.github.t1.pomx;

import lombok.Value;

import java.nio.file.*;

@Value
class GAV {
    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String version;

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
}

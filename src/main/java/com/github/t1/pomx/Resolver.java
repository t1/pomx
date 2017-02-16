package com.github.t1.pomx;

import java.nio.file.*;

class Resolver {
    static Path REPOSITORY = Paths.get(System.getProperty("user.home")).resolve(".m2/repository");

    Path resolve(GAV gav, String type) {
        return REPOSITORY.resolve(gav.asPath(type));
    }
}

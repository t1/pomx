package com.github.t1.pomx;

import java.nio.file.Path;

interface Resolver {
    Path resolve(GAV gav, String type);
}

package org.okapi.metrics.paths;

import java.nio.file.Path;
import java.util.function.Function;

public interface PersistedSetWalPathSupplier extends Function<Integer, Path> {}

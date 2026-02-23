package org.okapi.web.investigation.ctx.finders;

import java.util.List;

public interface TracesPathFinder {
    List<TracePath> findTracePaths(String dependencyName);
}

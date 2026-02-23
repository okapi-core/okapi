package org.okapi.web.investigation.ctx.finders;

import java.util.List;

public interface LogPathFinder {
    List<LogPath> findRelatedLogs(String dependencyName);
}

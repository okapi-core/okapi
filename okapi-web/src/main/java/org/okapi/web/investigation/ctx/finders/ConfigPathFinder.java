package org.okapi.web.investigation.ctx.finders;

import java.util.List;

public interface ConfigPathFinder {
    List<ConfigPath> findRelatedConfigs(String dependencyName);
}

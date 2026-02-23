package org.okapi.data.dao;

import java.util.List;
import java.util.Optional;
import org.okapi.data.dto.DashboardVersion;

public interface DashboardVersionDao {
  void save(DashboardVersion version);

  Optional<DashboardVersion> get(String orgId, String dashboardId, String versionId);

  List<DashboardVersion> list(String orgId, String dashboardId);
}

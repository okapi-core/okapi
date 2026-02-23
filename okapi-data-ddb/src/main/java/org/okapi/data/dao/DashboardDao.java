package org.okapi.data.dao;

import java.util.List;
import java.util.Optional;
import org.okapi.data.dto.DashboardDdb;
import org.okapi.data.exceptions.ResourceNotFoundException;

public interface DashboardDao {
  Optional<DashboardDdb> get(String orgId, String id);

  DashboardDdb save(DashboardDdb dto);

  void delete(String id) throws ResourceNotFoundException;

  List<DashboardDdb> getAll(String orgId);
}

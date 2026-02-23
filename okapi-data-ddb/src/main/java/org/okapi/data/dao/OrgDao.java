package org.okapi.data.dao;

import java.util.Optional;
import org.okapi.data.dto.OrgDtoDdb;

public interface OrgDao {
  Optional<OrgDtoDdb> findById(String orgId);

  void save(OrgDtoDdb orgDto);
}

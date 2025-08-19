package org.okapi.data.dao;

import org.okapi.data.dto.OrgDto;
import java.util.Optional;

public interface OrgDao {
  Optional<OrgDto> findById(String orgId);

  void save(OrgDto orgDto);
}

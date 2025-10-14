package org.okapi.metrics.cas.dao;

import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import org.okapi.metrics.cas.dto.TypeHints;

@Dao
public interface TypeHintsDao {

  @Insert
  void insert(TypeHints hint);

  @Select(customWhereClause = "tenant_id = :tenantId AND local_path = :localPath")
  TypeHints get(String tenantId, String localPath);
}

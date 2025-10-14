package org.okapi.traces.cas.dao;

import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.DaoKeyspace;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;

@Mapper
public interface TracesMapper {

  @DaoFactory
  TracesDao tracesDao(@DaoKeyspace String keyspace);
}

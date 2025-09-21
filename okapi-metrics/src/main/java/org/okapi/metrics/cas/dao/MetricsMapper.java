package org.okapi.metrics.cas.dao;

import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.DaoKeyspace;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;

@Mapper
public interface MetricsMapper {

  @DaoFactory
  SketchesDao sketchesDao(@DaoKeyspace String keysapce);

  @DaoFactory
  SearchHintDao searchHintDao(@DaoKeyspace String keyspace);

  @DaoFactory
  TypeHintsDao typeHintsDao(@DaoKeyspace String keyspace);
}

package org.okapi.traces.cas.dao;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import org.okapi.traces.cas.dto.*;

@Dao
public interface TracesDao {

  // Inserts
  @Insert
  void saveSpan(CasSpan span);

  @Insert
  void saveSpanByTime(CasSpanByTime row);

  @Insert
  void saveSpanByDuration(CasSpanByDuration row);

  @Insert
  void saveTraceByTime(TraceByTime row);

  @Insert
  void saveSpanById(CasSpanById row);

  // Selects
  @Select(customWhereClause = "tenant_id = :tenantId AND trace_id = :traceId")
  PagingIterable<CasSpan> getSpansByTrace(String tenantId, String traceId);

  @Select(customWhereClause = "tenant_id = :tenantId AND span_id = :spanId")
  CasSpanById getSpanById(String tenantId, String spanId);

  @Select(customWhereClause = "tenant_id = :tenantId AND bucket_second = :bucketSecond")
  PagingIterable<CasSpanByTime> getSpansByTime(String tenantId, long bucketSecond);

  @Select(
      customWhereClause =
          "tenant_id = :tenantId AND bucket_second = :bucketSecond AND status_code = :statusCode")
  PagingIterable<CasSpanByTime> getSpansByTimeAndStatus(
      String tenantId, long bucketSecond, String statusCode);

  @Select(customWhereClause = "tenant_id = :tenantId AND bucket_second = :bucketSecond")
  PagingIterable<CasSpanByDuration> getSpansByDuration(String tenantId, long bucketSecond);

  @Select(customWhereClause = "tenant_id = :tenantId AND bucket_second = :bucketSecond")
  PagingIterable<TraceByTime> getTracesByTime(String tenantId, long bucketSecond);
}


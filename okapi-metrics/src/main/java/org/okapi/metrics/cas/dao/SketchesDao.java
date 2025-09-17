package org.okapi.metrics.cas.dao;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import org.okapi.metrics.cas.dto.*;

@Dao
public interface SketchesDao {
  /** point fetch */
  @Select(
      customWhereClause =
          "tenant_id = :tenantId AND local_path = :localPath AND second_block = :second")
  GaugeSketchSecondly getSecondlySketch(String tenantId, String localPath, long second);

  @Select(
      customWhereClause =
          "tenant_id = :tenantId AND local_path = :localPath AND minute_block = :minute")
  GaugeSketchMinutely getMinutelySketch(String tenantId, String localPath, long minute);

  @Select(
      customWhereClause = "tenant_id = :tenantId AND local_path = :localPath AND hr_block = :hr")
  GaugeSketchHourly getHourlyBlock(String tenantId, String localPath, long hr);

  /** range scan */
  @Select(
      customWhereClause =
          "tenant_id = :tenantId AND local_path = :localPath AND second_block >= :start AND second_block <= :end")
  PagingIterable<GaugeSketchSecondly> scanSecondly(
      String tenantId, String localPath, long start, long end);

  @Select(
      customWhereClause =
          "tenant_id = :tenantId AND local_path = :localPath AND minute_block >= :start AND minute_block <= :end")
  PagingIterable<GaugeSketchMinutely> scanMinutely(
      String tenantId, String localPath, long start, long end);

  @Select(
      customWhereClause =
          "tenant_id = :tenantId AND local_path = :localPath AND hr_block >= :start AND hr_block <= :end")
  PagingIterable<GaugeSketchHourly> scanHourly(
      String tenantId, String localPath, long start, long end);

  @Select(
      customWhereClause =
          "tenant_id = :tenantId AND local_path = :localPath AND start_second >= :start AND start_second <= :end")
  PagingIterable<CounterSketch> scanCountSketches(
      String tenantId, String localPath, long start, long end);

  @Select(
      customWhereClause =
          "tenant_id = :tenantId AND local_path = :localPath AND start_second >= :start AND start_second <= :end")
  PagingIterable<HistoSketch> scanHistoSketches(
      String tenantId, String localPath, long start, long end);

  /** inserts */
  @Insert
  void saveSecondlySketch(GaugeSketchSecondly secondly);

  @Insert
  void saveMinutelySketch(GaugeSketchMinutely minutely);

  @Insert
  void saveHourlySketch(GaugeSketchHourly hourly);

  @Insert
  void saveCountSketch(CounterSketch sketch);

  @Insert
  void saveHistoSketch(HistoSketch histoSketch);
}

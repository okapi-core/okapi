package org.okapi.metrics.singletons;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import java.util.function.Supplier;
import org.okapi.metrics.Merger;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.fdb.FdbTsReader;
import org.okapi.metrics.fdb.FdbWriter;
import org.okapi.metrics.stats.*;
import org.okapi.rest.metrics.SubmitMetricsRequestInternal;

public class FdbSingletonFactory extends AbstractSingletonFactory {

  public FdbSingletonFactory() {
    super();
  }

  public Database getDatabase() {
    return makeSingleton(
        Database.class,
        () -> {
          System.load("/usr/local/lib/libfdb_c.dylib");
          var fdb = FDB.selectAPIVersion(720);
          return fdb.open();
        });
  }

  public StatisticsRestorer<UpdatableStatistics> unmarshaller() {
    return new WritableRestorer();
  }

  public SharedMessageBox<SubmitMetricsRequestInternal> messageBox(Node node) {
    return makeSingleton(
        node,
        SharedMessageBox.class,
        () -> {
          return new SharedMessageBox<SubmitMetricsRequestInternal>(100);
        });
  }

  public Supplier<UpdatableStatistics> updatableStatisticsSupplier() {
    return new KllStatSupplier();
  }

  public Merger<UpdatableStatistics> updatableStatisticsMerger() {
    return new RolledupMergerStrategy();
  }

  public FdbWriter fdbWriter(Node node) {
    return makeSingleton(
        node,
        FdbWriter.class,
        () ->
            new FdbWriter(
                messageBox(node),
                updatableStatisticsMerger(),
                getDatabase(),
                node,
                updatableStatisticsSupplier(),
                unmarshaller()));
  }

  public FdbTsReader fdbTsReader(Node node) {
    return makeSingleton(
        node,
        FdbTsReader.class,
        () -> new FdbTsReader(getDatabase(), updatableStatisticsMerger(), unmarshaller()));
  }
}

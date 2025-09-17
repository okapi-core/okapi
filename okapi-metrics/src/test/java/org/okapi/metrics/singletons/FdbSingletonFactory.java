package org.okapi.metrics.singletons;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import org.okapi.metrics.Merger;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.fdb.*;
import org.okapi.metrics.fdb.FdbMetricsWriter;
import org.okapi.metrics.stats.*;

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

  public SharedMessageBox<FdbTx> messageBox(Node node) {
    return makeSingleton(
        node,
        SharedMessageBox.class,
        () -> {
          return new SharedMessageBox<FdbTx>(100);
        });
  }

  public Merger<UpdatableStatistics> updatableStatisticsMerger() {
    return new RolledupMergerStrategy();
  }

  public FdbWriter fdbWriter(Node node) {
    return makeSingleton(
        node, FdbWriter.class, () -> new FdbWriter(getDatabase(), messageBox(node)));
  }

  public FdbMetricsWriter fdbMetricsWriter(Node node) {
    return makeSingleton(
        node,
        FdbMetricsWriter.class,
        () -> new FdbMetricsWriter(node.id(), messageBox(node), new KllStatSupplier()));
  }

  public FdbTsReader fdbTsReader(Node node) {
    return makeSingleton(
        node,
        FdbTsReader.class,
        () -> new FdbTsReader(getDatabase(), updatableStatisticsMerger(), unmarshaller()));
  }

  public FdbTsSearcher fdbTsSearcher(Node node) {
    return makeSingleton(node, FdbTsSearcher.class, () -> new FdbTsSearcher(getDatabase()));
  }

  public FdbTsClientFactory fdbTsClientFactory(Node node) {
    return makeSingleton(
        node, FdbTsClientFactory.class, () -> new FdbTsClientFactory(fdbTsReader(node)));
  }

  public FdbSeriesDiscoveryFactory fdbSeriesDiscoveryFactory(Node node) {
    return makeSingleton(
        node,
        FdbSeriesDiscoveryFactory.class,
        () -> new FdbSeriesDiscoveryFactory(fdbTsSearcher(node)));
  }
}

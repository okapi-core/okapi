package org.okapi.metrics.query;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.okapi.metrics.query.SampleMetricsPages.*;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.okapi.byterange.LengthPrefixedBlockSeekIterator;
import org.okapi.byterange.RangeIterationException;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.primitives.GaugeBlock;
import org.okapi.pages.AbstractTimeBlockMetadata;
import org.okapi.pages.BlockSeekIterator;
import org.okapi.pages.Codec;
import org.okapi.primitives.ChecksumedSerializable;

public class BlockIteratorTests {

  @Test
  void testSkipsIfNoMdMatch() throws RangeIterationException {
    var seekIterator = mock(BlockSeekIterator.class);
    var checkSumBlock = mock(ChecksumedSerializable.class);
    var mockCodec = mock(Codec.class);
    var mockMetadata = mock(AbstractTimeBlockMetadata.class);
    var iterator =
        new BlockIterator<ChecksumedSerializable, Object>(
            "blockId",
            seekIterator,
            metadata -> false,
            (metadata) -> true,
            () -> checkSumBlock,
            mockCodec);

    when(seekIterator.hasNextPage()).thenReturn(true, false);
    when(seekIterator.readMetadata()).thenReturn(new byte[] {1, 2, 3});
  }

  @Test
  void testIterationOnGaugeBlocks()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var page = getGaugePage();
    var supplier = getRangeSupplier(page);
    var iterator = new LengthPrefixedBlockSeekIterator(supplier);
    var localPath = MetricPaths.localPath(METRIC_NAME, METRIC_TAGS);
    var blockIterator =
        new BlockIterator<>(
            localPath,
            iterator,
            offsetAndLen -> true,
            metadata -> metadata.maybeContainsPath(METRIC_NAME, METRIC_TAGS),
            () -> new GaugeBlock(),
            new MetricsPageCodec());
    var block1 = blockIterator.next();
    Assertions.assertTrue(block1.isPresent());
    Assertions.assertTrue(block1.get().getStat(1L, RES_TYPE.SECONDLY, new double[] {}).isPresent());
    Assertions.assertTrue(block1.get().getStat(2L, RES_TYPE.SECONDLY, new double[] {}).isPresent());
    Assertions.assertTrue(block1.get().getStat(4L, RES_TYPE.SECONDLY, new double[] {}).isPresent());

    Assertions.assertTrue(blockIterator.hasMore());
    var block2 = blockIterator.next();
    Assertions.assertTrue(block2.isPresent());
    Assertions.assertFalse(blockIterator.hasMore());
  }

  @Test
  void testPageSkipping()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var page = getPagesWithMetricsApart();
    var supplier = getRangeSupplier(page);
    var iterator = new LengthPrefixedBlockSeekIterator(supplier);
    var localPath = MetricPaths.localPath(METRIC_NAME, METRIC_TAGS);
    var blockIterator =
        new BlockIterator<>(
            localPath,
            iterator,
            offsetAndLen -> true,
            metadata -> metadata.maybeContainsPath(METRIC_NAME, METRIC_TAGS),
            () -> new GaugeBlock(),
            new MetricsPageCodec());
    var block1 = blockIterator.next();
    Assertions.assertTrue(block1.isPresent());
    Assertions.assertTrue(block1.get().getStat(1L, RES_TYPE.SECONDLY, new double[] {}).isPresent());
    Assertions.assertTrue(block1.get().getStat(2L, RES_TYPE.SECONDLY, new double[] {}).isPresent());
    Assertions.assertTrue(block1.get().getStat(4L, RES_TYPE.SECONDLY, new double[] {}).isPresent());
    var block2 = blockIterator.next();
    Assertions.assertTrue(block2.isPresent());
    Assertions.assertFalse(blockIterator.hasMore());
  }

  @Test
  public void testSinglePageOfMetrics()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var page = getPagesWithDifferentMetrics();
    var supplier = getRangeSupplier(page);
    var iterator = new LengthPrefixedBlockSeekIterator(supplier);
    var localPath = MetricPaths.localPath(METRIC_NAME, METRIC_TAGS);
    var blockIterator =
        new BlockIterator<>(
            localPath,
            iterator,
            offsetAndLen -> true,
            metadata -> metadata.maybeContainsPath(METRIC_NAME, METRIC_TAGS),
            () -> new GaugeBlock(),
            new MetricsPageCodec());
    var block1 = blockIterator.next();
    Assertions.assertTrue(block1.isPresent());
    Assertions.assertTrue(block1.get().getStat(1L, RES_TYPE.SECONDLY, new double[] {}).isPresent());
    Assertions.assertTrue(block1.get().getStat(2L, RES_TYPE.SECONDLY, new double[] {}).isPresent());
    Assertions.assertTrue(block1.get().getStat(4L, RES_TYPE.SECONDLY, new double[] {}).isPresent());
    var block2 = blockIterator.next();
    Assertions.assertTrue(block2.isEmpty());
    Assertions.assertFalse(blockIterator.hasMore());
  }
}

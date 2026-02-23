package org.okapi.traces.ch;

import com.google.gson.Gson;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import org.okapi.metrics.ch.ChWalResources;
import org.okapi.metrics.ch.ChWriter;
import org.okapi.wal.frame.WalEntry;
import org.okapi.wal.io.WalReader;
import org.okapi.wal.manager.WalManager;

@RequiredArgsConstructor
public class ChTracesWalConsumer {
  private final WalReader walReader;
  private final WalManager walManager;
  private final int batchSize;
  private final ChWriter chWriter;
  private final OtelTracesToChRowsConverter converter;
  private final Gson gson = new Gson();

  public ChTracesWalConsumer(
      ChWalResources walResources,
      int batchSize,
      ChWriter chWriter,
      OtelTracesToChRowsConverter converter) {
    this.walReader = walResources.getReader();
    this.walManager = walResources.getManager();
    this.batchSize = batchSize;
    this.chWriter = chWriter;
    this.converter = converter;
  }

  public void consumeRecords() throws IOException, InterruptedException, ExecutionException {
    var batch = walReader.readBatchAndAdvance(batchSize);
    List<ChSpansTableRow> rows = new ArrayList<>();
    List<ChSpansIngestedAttribsRow> attribRows = new ArrayList<>();
    for (var entry : batch) {
      var req = ExportTraceServiceRequest.parseFrom(entry.getPayload());
      rows.addAll(converter.toRows(req));
      attribRows.addAll(converter.toAttributeRows(req));
    }
    if (rows.isEmpty()) return;
    var jsonRows = rows.stream().map(gson::toJson).toList();
    var insertRes = chWriter.writeRows(ChTracesConstants.TBL_SPANS_V1, jsonRows);
    insertRes.get();
    if (!attribRows.isEmpty()) {
      var jsonAttribRows = attribRows.stream().map(gson::toJson).toList();
      var attribInsertRes =
          chWriter.writeRows(ChTracesConstants.TBL_SPANS_INGESTED_ATTRIBS, jsonAttribRows);
      attribInsertRes.get();
    }

    var maxLsn = WalEntry.getMaxLsn(batch);
    walManager.commitLsn(maxLsn);
  }
}

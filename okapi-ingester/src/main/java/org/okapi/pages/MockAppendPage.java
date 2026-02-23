package org.okapi.pages;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.okapi.wal.lsn.Lsn;

public class MockAppendPage
    implements AppendOnlyPage<MockPageInput, MockPageSnapshot, MockPageMetadata, MockPageBody> {

  MockPageMetadata mockPageMetadata;
  MockPageBody mockPageBody;
  long rangeMs;
  long maxBytes;
  long totalBytes;

  @AllArgsConstructor
  @Getter
  @Builder
  public static class MockPageContent {
    List<MockPageInput> inputs;
    long rangeMs;
    long maxBytes;
    long totalBytes;
  }

  public MockAppendPage(long rangeMs, long maxBytes) {
    this.mockPageBody = new MockPageBody();
    this.mockPageMetadata = new MockPageMetadata();
    this.rangeMs = rangeMs;
    this.maxBytes = maxBytes;
    this.totalBytes = 0;
  }

  public MockAppendPage(MockPageMetadata metadata, MockPageBody body) {
    this.mockPageBody = body;
    this.mockPageMetadata = metadata;
  }

  public MockPageContent pageContent() {
    return MockPageContent.builder()
        .inputs(mockPageBody.getInputs())
        .rangeMs(rangeMs)
        .maxBytes(maxBytes)
        .totalBytes(totalBytes)
        .build();
  }

  @Override
  public void append(MockPageInput obj) {
    mockPageBody.append(obj);
    totalBytes += obj.getSize();
    mockPageMetadata.updateTsStart(obj.tsMillis);
    mockPageMetadata.updateTsEnd(obj.tsMillis);
  }

  @Override
  public Optional<InclusiveRange> range() {
    return Optional.of(
        new InclusiveRange(mockPageMetadata.getTsStart(), mockPageMetadata.getTsEnd()));
  }

  @Override
  public boolean isFull() {
    return (mockPageMetadata.getTsStart() - mockPageMetadata.getTsEnd()) >= rangeMs
        || totalBytes >= maxBytes;
  }

  @Override
  public boolean isEmpty() {
    return totalBytes == 0;
  }

  @Override
  public MockPageSnapshot snapshot() {
    return new MockPageSnapshot(
        Collections.unmodifiableList(mockPageBody.getInputs()),
        mockPageMetadata.getTsStart(),
        mockPageMetadata.getTsEnd());
  }

  @Override
  public MockPageMetadata getMetadata() {
    return mockPageMetadata;
  }

  @Override
  public MockPageBody getPageBody() {
    return mockPageBody;
  }

  @Override
  public Lsn getMaxLsn() {
    return Lsn.fromNumber(this.mockPageMetadata.getMaxLsn());
  }

  @Override
  public void updateLsn(Lsn lsn) {
    this.mockPageMetadata.setMaxLsn(lsn.getNumber());
  }
}

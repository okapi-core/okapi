package org.okapi.io;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import lombok.Getter;

import java.util.Objects;
import java.util.Optional;
import java.util.zip.CRC32;

public class OkapiBufferDecoder implements CheckedBufferDecoder {

  DECODER_STATE state;
  Integer expectedCrc;
  Integer computedCrc;
  byte[] buffer;
  int n = -1;
  int bufOffset = -1;
  @Getter int offset;

  public OkapiBufferDecoder() {
    this.state = DECODER_STATE.CREATED;
  }

  public void setExpectedCrc() {
    if (state != DECODER_STATE.INIT) {
      return;
    }
    var crc =
        Ints.fromBytes(
            buffer[bufOffset + n - 4],
            buffer[bufOffset + n - 3],
            buffer[bufOffset + n - 2],
            buffer[bufOffset + n - 1]);
    this.expectedCrc = crc;
  }

  public void setComputedCrc() {
    if (state != DECODER_STATE.INIT) {
      return;
    }
    var crc = new CRC32();
    crc.update(buffer, bufOffset, n - 4);
    this.computedCrc = (int) crc.getValue();
  }

  @Override
  public DECODER_STATE getState() {
    return state;
  }

  @Override
  public boolean isCrcMatch() {
    setExpectedCrc();
    setComputedCrc();
    if (Objects.equals(expectedCrc, computedCrc)) {
      this.state = DECODER_STATE.VALIDATED;
      return true;
    } else {
      this.state = DECODER_STATE.INVALID_BUFFER;
      return false;
    }
  }

  @Override
  public Optional<Integer> getExpectedCrc() {
    return Optional.ofNullable(expectedCrc);
  }

  @Override
  public Optional<Integer> getComputedCrc() {
    return Optional.ofNullable(computedCrc);
  }

  @Override
  public void setBuffer(byte[] bytes, int off, int len) {
    this.buffer = bytes;
    this.n = len;
    this.bufOffset = off;
    this.offset = bufOffset;
    this.state = DECODER_STATE.INIT;
  }

  @Override
  public int nextInt() throws NotEnoughBytesException {
    checkBytesLeft(4);
    var value =
        Ints.fromBytes(buffer[offset], buffer[offset + 1], buffer[offset + 2], buffer[offset + 3]);
    offset += 4;
    updateStateIfFullyRead();
    return value;
  }

  @Override
  public long nextLong() throws NotEnoughBytesException {
    checkBytesLeft(8);
    var value =
        Longs.fromBytes(
            buffer[offset],
            buffer[offset + 1],
            buffer[offset + 2],
            buffer[offset + 3],
            buffer[offset + 4],
            buffer[offset + 5],
            buffer[offset + 6],
            buffer[offset + 7]);
    offset += 8;
    updateStateIfFullyRead();
    return value;
  }

  @Override
  public byte[] nextBytesLenPrefix() throws NotEnoughBytesException {
    checkBytesLeft(4);
    var toRead = nextInt();
    checkBytesLeft(toRead);
    var copied = new byte[toRead];
    System.arraycopy(buffer, offset, copied, 0, toRead);
    offset += toRead;
    updateStateIfFullyRead();
    return copied;
  }

  @Override
  public byte[] nextBytesNoLenPrefix(int n) throws NotEnoughBytesException {
    checkBytesLeft(n);
    var copied = new byte[n];
    System.arraycopy(buffer, offset, copied, 0, n);
    offset += n;
    updateStateIfFullyRead();
    return copied;
  }

  private void updateStateIfFullyRead() {
    if (offset == n) {
      this.state = DECODER_STATE.BUFFER_FULLY_READ;
    }
  }

  private void checkBytesLeft(int expected) throws NotEnoughBytesException {
    var bytesLeft = n - (offset - bufOffset);
    if (bytesLeft < expected) {
      throw new NotEnoughBytesException();
    }
  }
}

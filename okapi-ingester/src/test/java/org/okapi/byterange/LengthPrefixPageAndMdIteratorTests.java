package org.okapi.byterange;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.okapi.ds.ArraySlices;
import org.okapi.io.OkapiIo;
import org.okapi.s3.ByteArrayByteRangeSupplier;

public class LengthPrefixPageAndMdIteratorTests {

  static byte[] getDataBlock(byte[] md, byte[] docs) throws IOException {
    var baos = new ByteArrayOutputStream();
    baos.writeBytes(getLenBlock(md.length, docs.length));
    baos.writeBytes(md);
    baos.writeBytes(docs);
    return baos.toByteArray();
  }

  static byte[] getLenBlock(int mdLen, int docBlkLen) throws IOException {
    var baos = new ByteArrayOutputStream();
    OkapiIo.write(baos, (byte) 'V');
    OkapiIo.write(baos, (byte) '0');
    OkapiIo.write(baos, (byte) '0');
    OkapiIo.write(baos, (byte) '1');
    OkapiIo.writeInt(baos, mdLen);
    OkapiIo.writeInt(baos, docBlkLen);
    return baos.toByteArray();
  }

  static byte[] getLenPrefixMd(byte[] md, int docBlockLen) throws IOException {
    var baos = new ByteArrayOutputStream();
    OkapiIo.writeBytesWithoutLenPrefix(baos, getLenBlock(md.length, docBlockLen));
    OkapiIo.writeBytesWithoutLenPrefix(baos, md);
    return baos.toByteArray();
  }

  @Test
  void testWellBehavedStream() throws RangeIterationException, IOException {
    var md = new byte[] {0x0, 0x1};
    var page = new byte[] {0x2, 0x3, 0x4};
    var mockSupplier = new ByteArrayByteRangeSupplier(getDataBlock(md, page));
    var iterator = new LengthPrefixPageAndMdIterator(mockSupplier);
    var mdFromIter = iterator.readMetadata();
    assertArrayEquals(getLenPrefixMd(md, 3), mdFromIter);
    var pageFromIter = iterator.readPageBody();
    assertArrayEquals(page, pageFromIter);
  }

  @Test
  void testForward() throws IOException, RangeIterationException {
    var md1 = new byte[] {0x0, 0x1};
    var page1 = new byte[] {0x2, 0x3, 0x4};
    var md2 = new byte[] {0x5, 0x6};
    var page2 = new byte[] {0x7, 0x8, 0x9};

    var block1 = getDataBlock(md1, page1);
    var block2 = getDataBlock(md2, page2);
    var block = ArraySlices.concat(block1, block2);
    var mockSupplier = new ByteArrayByteRangeSupplier(block);
    var iterator = new LengthPrefixPageAndMdIterator(mockSupplier);
    var mdFromIter = iterator.readMetadata();
    assertArrayEquals(getLenPrefixMd(md1, 3), mdFromIter);
    var pageFromIter = iterator.readPageBody();
    assertArrayEquals(page1, pageFromIter);
    iterator.forward();
    var mdFromIter2 = iterator.readMetadata();
    assertArrayEquals(getLenPrefixMd(md2, 3), mdFromIter2);
    var pageFromIter2 = iterator.readPageBody();
    assertArrayEquals(page2, pageFromIter2);
  }
}

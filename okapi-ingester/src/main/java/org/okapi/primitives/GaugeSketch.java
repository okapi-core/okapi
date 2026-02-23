package org.okapi.primitives;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.datasketches.kll.KllFloatsSketch;
import org.apache.datasketches.memory.Memory;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.stats.KllStatSupplier;

public class GaugeSketch implements RawSerializable {
  private float mean;
  private float count;
  private float sumOfDeviationsSquared;
  private KllFloatsSketch floatsQuantiles;

  public GaugeSketch() {
    this.floatsQuantiles = KllStatSupplier.kllSketch();
  }

  public ReadOnlySketch getWithQuantiles() {
    var serialized = floatsQuantiles.toByteArray();
    return new ReadOnlySketch(mean, count, sumOfDeviationsSquared, serialized);
  }

  @Override
  public void fromByteArray(byte[] bytes, int off, int len)
      throws StreamReadingException, IOException {
    var is = new ByteArrayInputStream(bytes, off, len);
    // deserialize mean
    mean = OkapiIo.readFloat(is);
    // deserialize count
    count = OkapiIo.readFloat(is);
    // deserialize sumOfDeviationsSquared
    sumOfDeviationsSquared = OkapiIo.readFloat(is);
    // deserialize floatsQuantiles
    var quantilesLength = OkapiIo.readInt(is);
    var quantilesBytes = new byte[quantilesLength];
    var readBytes = is.read(quantilesBytes);
    if (readBytes != quantilesLength) {
      throw new IOException(
          "Could not read enough bytes for floatsQuantiles. Expected: "
              + quantilesLength
              + ", but got: "
              + readBytes);
    }
    floatsQuantiles = KllFloatsSketch.heapify(Memory.wrap(quantilesBytes));
  }

  @Override
  public int byteSize() {
    return 4 // mean;
        + 4 // count
        + 4 // sumOfDeviationsSquared
        + 4 // length of floatsQuantiles
        + floatsQuantiles.getSerializedSizeBytes(); // floatsQuantiles bytes
  }

  @Override
  public byte[] toByteArray() throws IOException {
    var os = new java.io.ByteArrayOutputStream();
    // serialize mean
    OkapiIo.writeFloat(os, mean);
    // serialize count
    OkapiIo.writeFloat(os, count);
    // serialize sumOfDeviationsSquared
    OkapiIo.writeFloat(os, sumOfDeviationsSquared);
    // serialize floatsQuantiles
    var quantilesBytes = floatsQuantiles.toByteArray();
    OkapiIo.writeInt(os, quantilesBytes.length);
    os.write(quantilesBytes);
    return os.toByteArray();
  }

  public void update(float value) {
    floatsQuantiles.update(value);
    count += 1;
    // do a Welford's method update for variance
    var oldMean = mean;
    mean = mean + (value - mean) / count;
    sumOfDeviationsSquared =
        sumOfDeviationsSquared
            + ((value - oldMean) * (value - mean) - sumOfDeviationsSquared) / count;
  }
}

package org.okapi.primitives;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.okapi.io.StreamReadingException;

public class GaugeSketchTests {

  @Test
  void testSerialization_single() throws IOException, StreamReadingException {
    var sketch = new GaugeSketch();
    sketch.update(10.5f);
    var readonly = sketch.getWithQuantiles();
    assert readonly.getQuantile(0.5) == 10.5f;
    var bytes = sketch.toByteArray();
    var deserializedSketch = new GaugeSketch();
    deserializedSketch.fromByteArray(bytes, 0, bytes.length);
    assertSketchEquals(sketch, deserializedSketch);
  }

  @Test
  void testSerialization_none() throws IOException, StreamReadingException {
    var sketch = new GaugeSketch();
    var bytes = sketch.toByteArray();
    var deserializedSketch = new GaugeSketch();
    deserializedSketch.fromByteArray(bytes, 0, bytes.length);
    var readonlyRef = sketch.getWithQuantiles();
    var readonlyDeserialized = deserializedSketch.getWithQuantiles();
    assertEquals(readonlyRef.getMean(), readonlyDeserialized.getMean());
    assertEquals(readonlyRef.getCount(), readonlyDeserialized.getCount());
    assertEquals(readonlyRef.getSumOfDeviationsSquared(), readonlyDeserialized.getSumOfDeviationsSquared());
  }

  @Test
  void testSerialization_multiple() throws IOException, StreamReadingException {
    var sketch = new GaugeSketch();
    sketch.update(10.5f);
    sketch.update(20.5f);
    sketch.update(30.0f);
    var bytes = sketch.toByteArray();
    var deserializedSketch = new GaugeSketch();
    deserializedSketch.fromByteArray(bytes, 0, bytes.length);
    assertSketchEquals(sketch, deserializedSketch);
  }

  @Test
  void testSerialization_large() throws IOException, StreamReadingException {
    var sketch = new GaugeSketch();
    for (int i = 0; i < 10000; i++) {
      sketch.update((float) Math.random() * 1000);
    }
    var bytes = sketch.toByteArray();
    var deserializedSketch = new GaugeSketch();
    deserializedSketch.fromByteArray(bytes, 0, bytes.length);
    assertSketchEquals(sketch, deserializedSketch);
  }

  @Test
  void testSeveralBlocks() {
    var gaugeSketch = new GaugeSketch();
    var samples =
        new float[] {
          0.30914885f,
          0.26453018f,
          0.36938375f,
          0.21202108f,
          0.27185744f,
          0.22443558f,
          0.2127764f,
          0.34225437f,
          0.3126131f,
          0.13575509f,
          0.18033963f,
          0.24253982f,
          0.10030336f,
          0.21237656f
        };
    var sum = 0f;
    for(var s: samples){
      sum += s;
      gaugeSketch.update(s);
    }
    var mean = gaugeSketch.getWithQuantiles().getMean();
    var expected = sum / samples.length;
    assertEquals(expected, mean);
  }

  public void assertSketchEquals(GaugeSketch expected, GaugeSketch actual) {
    var readonly = expected.getWithQuantiles();
    var readonlyActual = actual.getWithQuantiles();
    assertEquals(readonly.getMean(), readonlyActual.getMean());
    assertEquals(readonly.getCount(), readonlyActual.getCount());
    assertEquals(readonly.getSumOfDeviationsSquared(), readonlyActual.getSumOfDeviationsSquared());
    assertEquals(readonly.getQuantile(0.0), readonlyActual.getQuantile(0.0));
    assertEquals(readonly.getQuantile(0.5), readonlyActual.getQuantile(0.5));
    assertEquals(readonly.getQuantile(1.0), readonlyActual.getQuantile(1.0));
  }

}

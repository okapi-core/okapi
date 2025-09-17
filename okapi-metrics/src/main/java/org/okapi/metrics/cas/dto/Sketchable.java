package org.okapi.metrics.cas.dto;

import java.nio.ByteBuffer;

public interface Sketchable {
    ByteBuffer getSketch();
}

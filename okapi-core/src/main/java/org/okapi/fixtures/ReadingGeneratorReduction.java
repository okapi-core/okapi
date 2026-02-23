package org.okapi.fixtures;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ReadingGeneratorReduction {
  ReadingGenerator generator;
  List<Long> timestamp;
  List<Float> values;
}

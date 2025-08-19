package org.okapi.fixtures;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ReadingGeneratorReduction {
    ReadingGenerator generator;
    List<Long> timestamp;
    List<Float> values;
}

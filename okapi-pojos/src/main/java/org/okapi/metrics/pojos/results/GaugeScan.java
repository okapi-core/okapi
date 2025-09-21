package org.okapi.metrics.pojos.results;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Builder
public class GaugeScan {

    @Getter
    public final String universalPath;

    @Getter
    public final List<Long> timestamps;

    @Getter
    public final List<Float> values;

}

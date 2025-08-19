package org.okapi.metrics.pojos;

import lombok.Getter;

import java.util.Optional;

public enum RES_TYPE {
    SECONDLY("1s"),
    MINUTELY("1m"),
    HOURLY("1h"),
    DAILY("1d");

    @Getter
    private String resolution;

    RES_TYPE(String resolution) {
        this.resolution = resolution;
    }

    public static Optional<RES_TYPE> parse(String v) {
        if(v == null || v.isEmpty()){
            return Optional.empty();
        }
        for (RES_TYPE resType : RES_TYPE.values()) {
            if (resType.getResolution().equalsIgnoreCase(v)) {
                return Optional.of(resType);
            }
        }
        return Optional.empty();
    }
}

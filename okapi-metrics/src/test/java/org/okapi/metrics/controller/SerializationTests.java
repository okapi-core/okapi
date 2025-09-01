package org.okapi.metrics.controller;

import com.google.gson.Gson;
import org.okapi.rest.metrics.SubmitMetricsRequestInternal;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class SerializationTests {

    @Test
    public void testSer(){
        var str = "{\"metric_type\": \"test_metric\", \"tags\": {\"test-key\": \"test-value\"}, \"vals\": [42.0, 43.0], \"times\": [1752467820932, 1752467821932]}";
        var gson = new Gson();
        var parsed = gson.fromJson(str, SubmitMetricsRequestInternal.class);
        System.out.println(parsed.getMetricName());
        System.out.println(parsed.getTags());
        System.out.println(Arrays.toString(parsed.getTs()));
        System.out.println(Arrays.toString(parsed.getValues()));

    }
}

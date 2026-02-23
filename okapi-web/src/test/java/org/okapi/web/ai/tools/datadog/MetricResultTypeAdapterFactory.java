/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools.datadog;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import org.okapi.web.ai.tools.results.GaugeResult;
import org.okapi.web.ai.tools.results.HistogramResult;
import org.okapi.web.ai.tools.results.MetricResult;

/**
 * Simple polymorphic (de)serializer for MetricResult based on @type discriminator in test fixtures.
 */
public class MetricResultTypeAdapterFactory implements TypeAdapterFactory {
  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    // Bind only the MetricResult base type; allow subclasses to use their own adapters.
    if (!MetricResult.class.equals(type.getRawType())) {
      return null;
    }
    return (TypeAdapter<T>) new MetricResultAdapter(gson);
  }

  private static class MetricResultAdapter extends TypeAdapter<MetricResult> {
    private final Gson gson;

    MetricResultAdapter(Gson gson) {
      this.gson = gson;
    }

    @Override
    public MetricResult read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
      JsonElement json = com.google.gson.internal.Streams.parse(in);
      JsonObject obj = json.getAsJsonObject();
      String discriminator = obj.get("@type").getAsString();
      if ("gauge".equalsIgnoreCase(discriminator)) {
        return gson.fromJson(json, GaugeResult.class);
      } else if ("histogram".equalsIgnoreCase(discriminator)) {
        return gson.fromJson(json, HistogramResult.class);
      }
      throw new JsonParseException("Unknown metric result type: " + discriminator);
    }

    @Override
    public void write(com.google.gson.stream.JsonWriter out, MetricResult src)
        throws java.io.IOException {
      if (src instanceof GaugeResult gr) {
        JsonObject obj = gson.toJsonTree(gr).getAsJsonObject();
        obj.addProperty("@type", "gauge");
        com.google.gson.internal.Streams.write(obj, out);
      } else if (src instanceof HistogramResult hr) {
        JsonObject obj = gson.toJsonTree(hr).getAsJsonObject();
        obj.addProperty("@type", "histogram");
        com.google.gson.internal.Streams.write(obj, out);
      } else {
        throw new JsonParseException("Unknown MetricResult subclass: " + src.getClass());
      }
    }
  }
}

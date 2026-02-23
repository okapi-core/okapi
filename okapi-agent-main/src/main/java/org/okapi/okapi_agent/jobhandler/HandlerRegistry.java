package org.okapi.okapi_agent.jobhandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.okapi.okapi_agent.connection.HttpConnection;
import org.okapi.okapi_agent.query.HttpQueryDeserializer;

@Slf4j
@Singleton
public class HandlerRegistry {

  Map<String, JobHandler> handlers;

  @Inject
  public HandlerRegistry(HandlerCfg cfg) {
    var fp = cfg.fileName();
    this.handlers = new HashMap<>();
    readFromFile(fp);
  }

  public List<String> getRegisteredSourceIds() {
    return handlers.keySet().stream().toList();
  }

  protected void readFromFile(Path filePath) {
    var yamlParser = new org.yaml.snakeyaml.Yaml();
    try (var reader = java.nio.file.Files.newBufferedReader(filePath)) {
      var cfgBlocks = yamlParser.loadAll(reader);
      for (var cfg : cfgBlocks) {
        if (cfg instanceof List<?> cfgList) {
          for (var kv : cfgList) {
            if (kv instanceof Map<?, ?> map) {
              var mapStrObj = asMap(map);
              var type = readString(mapStrObj, "type");
              Optional<JobHandler> handlerOpt =
                  switch (type) {
                    case "http" -> makeHttpHandler(mapStrObj, new OkHttpClient());
                    default -> {
                      throw new IllegalArgumentException("Unknown handler type: " + type);
                    }
                  };
              handlerOpt.ifPresent(
                  handler -> {
                    if (handlers.containsKey(handler.getSourceId())) {
                      throw new IllegalArgumentException(
                          "Duplicate handler for sourceId: " + handler.getSourceId());
                    }
                    handlers.put(handlerOpt.get().getSourceId(), handler);
                    log.info("Registered handler for sourceId: " + handler.getSourceId());
                  });
            } else {
              throw new IllegalArgumentException("Invalid handler configuration");
            }
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Object> asMap(Map<?, ?> input) {
    return input.entrySet().stream()
        .filter(e -> e.getKey() instanceof String k)
        .collect(java.util.stream.Collectors.toMap(e -> (String) e.getKey(), Map.Entry::getValue));
  }

  public Optional<JobHandler> makeHttpHandler(Map<String, Object> cfg, OkHttpClient httpClient) {
    var sourceId = readString(cfg, "sourceId");
    var host = readString(cfg, "host");
    var deserializer = HttpQueryDeserializer.create();
    var connection = HttpConnection.of(httpClient, cfg);
    var handler =
        new HttpHandler(new HttpHandler.SourceIdAndHost(sourceId, host), deserializer, connection);
    return Optional.of(handler);
  }

  public String readString(Map<String, Object> cfg, String key) {
    return switch (cfg.get(key)) {
      case String s -> s;
      case null -> throw new IllegalArgumentException(key + " is required");
      default -> throw new IllegalArgumentException(key + " must be a string");
    };
  }

  public Optional<JobHandler> get(String sourceId) {
    return Optional.ofNullable(handlers.get(sourceId));
  }
}

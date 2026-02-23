package org.okapi.spring.configs.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "okapi.nodes")
@Component
@Validated
public class NodesCfg {
  Path nodeIdFile;
}

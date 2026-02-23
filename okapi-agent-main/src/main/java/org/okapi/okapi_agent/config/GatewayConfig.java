package org.okapi.okapi_agent.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "okapi.gateway")
@Component
@Validated
@AllArgsConstructor
public class GatewayConfig {
    @NotNull
    @NotBlank
    String endpoint;

    @NotNull
    @NotBlank
    String gatewayToken;
}

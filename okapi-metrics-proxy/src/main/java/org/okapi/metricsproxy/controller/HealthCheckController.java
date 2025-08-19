package org.okapi.metricsproxy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
    
    @GetMapping(path = "/internal/healthcheck")
    public String healthCheck() {
        return "ok";
    }
}

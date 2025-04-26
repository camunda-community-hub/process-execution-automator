package org.camunda.automator.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("pea")

public class PingController {

    @Value("${app.version}")
    private String appVersion;

    @GetMapping("/api/ping")
    public Map<String, Object> ping() {
        return Map.of("version", appVersion);
    }
}

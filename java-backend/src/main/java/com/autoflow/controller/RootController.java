package com.autoflow.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "AutoFlow Java API",
                "status", "ok",
                "health", "/api/health",
                "dashboardStats", "/api/dashboard/stats",
                "executions", "/api/executions",
                "customers", "/api/customers"
        );
    }
}

package com.autoflow.controller;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
@RestController
@RequestMapping("/api")
public class HealthController {
  @GetMapping("/health") public Map<String,Object> health(){return Map.of("status","ok","service","autoflow-java-api","time",Instant.now().toString());}
  @GetMapping("/dashboard") public Map<String,Object> dashboard(){return Map.of("workflowRuns",1248,"successRate",99.4,"activeCustomers",486,"hoursAutomated",214);}
}

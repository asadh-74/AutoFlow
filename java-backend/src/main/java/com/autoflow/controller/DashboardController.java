package com.autoflow.controller;

import com.autoflow.model.Execution;
import com.autoflow.repository.CustomerRepository;
import com.autoflow.repository.ExecutionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final ExecutionRepository executionRepository;
  private final CustomerRepository customerRepository;

  public DashboardController(ExecutionRepository executionRepository, CustomerRepository customerRepository) {
    this.executionRepository = executionRepository;
    this.customerRepository = customerRepository;
  }

  @GetMapping("/stats")
  public Map<String,Object> stats() {
    List<Execution> recent = executionRepository.findTop50ByOrderByStartedAtDesc();
    long totalRuns = executionRepository.count();
    long successRuns = recent.stream().filter(e -> "success".equalsIgnoreCase(e.getStatus())).count();
    long failedRuns = recent.stream().filter(e -> "failed".equalsIgnoreCase(e.getStatus())).count();
    double successRate = recent.isEmpty() ? 0 : ((double) successRuns / recent.size()) * 100.0;
    double averageDuration = recent.stream().filter(e -> e.getDurationMs() != null).mapToLong(Execution::getDurationMs).average().orElse(0);
    long customers = customerRepository.count();

    Map<String,Object> response = new LinkedHashMap<>();
    response.put("totalRuns", totalRuns);
    response.put("successRuns", successRuns);
    response.put("failedRuns", failedRuns);
    response.put("successRate", Math.round(successRate * 10.0) / 10.0);
    response.put("averageDurationMs", Math.round(averageDuration));
    response.put("customers", customers);
    return response;
  }
}

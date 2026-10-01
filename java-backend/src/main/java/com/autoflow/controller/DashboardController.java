package com.autoflow.controller;

import com.autoflow.model.Execution;
import com.autoflow.repository.CustomerRepository;
import com.autoflow.repository.ExecutionRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

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
    double averageDuration = recent.stream()
        .filter(e -> e.getDurationMs() != null)
        .mapToLong(Execution::getDurationMs)
        .average()
        .orElse(0);
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

  @GetMapping("/analytics")
  public Map<String,Object> analytics() {
    List<Execution> recent = executionRepository.findTop50ByOrderByStartedAtDesc();
    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    List<String> labels = new ArrayList<>();
    List<Long> activity = new ArrayList<>();
    for (int i = 6; i >= 0; i--) {
      LocalDate day = today.minusDays(i);
      labels.add(day.getDayOfWeek().name().substring(0, 3));
      long count = recent.stream()
          .filter(e -> e.getStartedAt() != null)
          .filter(e -> e.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(day))
          .count();
      activity.add(count);
    }

    Map<String, Long> workflowCounts = recent.stream()
        .filter(e -> e.getWorkflow() != null)
        .collect(Collectors.groupingBy(Execution::getWorkflow, LinkedHashMap::new, Collectors.counting()));

    List<Map<String,Object>> workflowImpact = workflowCounts.entrySet().stream()
        .sorted((a,b) -> Long.compare(b.getValue(), a.getValue()))
        .limit(5)
        .map(e -> {
          Map<String,Object> item = new LinkedHashMap<>();
          item.put("workflow", e.getKey());
          item.put("runs", e.getValue());
          return item;
        })
        .toList();

    Map<String,Object> response = new LinkedHashMap<>();
    response.put("labels", labels);
    response.put("activity", activity);
    response.put("workflowImpact", workflowImpact);
    return response;
  }
}

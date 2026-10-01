package com.autoflow.controller;
import com.autoflow.model.Execution;
import com.autoflow.repository.ExecutionRepository;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/executions")
public class ExecutionController {
  private final ExecutionRepository repo;
  public ExecutionController(ExecutionRepository repo){this.repo=repo;}
  @GetMapping public List<Map<String,Object>> all(){
    return repo.findTop50ByOrderByStartedAtDesc().stream().map(e->{
      Map<String,Object> m=new LinkedHashMap<>();
      m.put("id",e.getRunId());m.put("workflow",e.getWorkflow());m.put("status",e.getStatus());
      m.put("duration",e.getDurationMs()+"ms");m.put("started",e.getStartedAt().toString());return m;
    }).toList();
  }
}

package com.autoflow.controller;
import com.autoflow.model.Execution;
import com.autoflow.repository.ExecutionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.util.*;

@RestController @RequestMapping("/api/automations")
public class AutomationController {
  private final RestClient rest=RestClient.create();
  private final ExecutionRepository executions;
  @Value("${autoflow.n8n.webhook-url:}") private String webhookUrl;
  @Value("${autoflow.n8n.webhook-secret:}") private String webhookSecret;
  public AutomationController(ExecutionRepository executions){this.executions=executions;}

  public record TriggerRequest(String event,String workflow,Map<String,Object> payload){}

  @PostMapping("/trigger")
  public ResponseEntity<Map<String,Object>> trigger(@RequestBody TriggerRequest req){
    if(webhookUrl==null || webhookUrl.isBlank())
      return ResponseEntity.status(503).body(Map.of("ok",false,"message","N8N_WEBHOOK_URL is not configured"));

    String runId="RUN-"+System.currentTimeMillis();
    long start=System.nanoTime();
    try{
      RestClient.RequestBodySpec spec=rest.post().uri(webhookUrl).contentType(MediaType.APPLICATION_JSON);
      if(webhookSecret!=null && !webhookSecret.isBlank()) spec.header("X-AutoFlow-Secret",webhookSecret);
      Map<String,Object> event=new LinkedHashMap<>();
      event.put("runId",runId); event.put("event",req.event()); event.put("workflow",req.workflow());
      event.put("payload",req.payload()==null?Map.of():req.payload()); event.put("source","autoflow-java-api");
      spec.body(event).retrieve().toBodilessEntity();
      long ms=Duration.ofNanos(System.nanoTime()-start).toMillis();
      executions.save(new Execution(runId,req.workflow(),"success",ms));
      return ResponseEntity.ok(Map.of("ok",true,"runId",runId,"status","sent-to-n8n","durationMs",ms));
    }catch(Exception ex){
      long ms=Duration.ofNanos(System.nanoTime()-start).toMillis();
      executions.save(new Execution(runId,req.workflow(),"failed",ms));
      return ResponseEntity.status(502).body(Map.of("ok",false,"runId",runId,"message","n8n request failed"));
    }
  }
}

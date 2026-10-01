package com.autoflow.controller;

import com.autoflow.model.Execution;
import com.autoflow.repository.ExecutionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/automations")
public class AutomationController {
  private final RestClient rest;
  private final ExecutionRepository executions;

  @Value("${autoflow.n8n.webhook-url:}") private String webhookUrl;
  @Value("${autoflow.n8n.lead-webhook-url:}") private String leadWebhookUrl;
  @Value("${autoflow.n8n.webhook-secret:}") private String webhookSecret;

  public AutomationController(ExecutionRepository executions) {
    this.executions = executions;
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(10));
    this.rest = RestClient.builder().requestFactory(factory).build();
  }

  public record TriggerRequest(String event,String workflow,Map<String,Object> payload){}

  @GetMapping("/status")
  public Map<String,Object> status(){
    return Map.of(
      "customerOnboardingConfigured", webhookUrl != null && !webhookUrl.isBlank(),
      "leadQualificationConfigured", leadWebhookUrl != null && !leadWebhookUrl.isBlank()
    );
  }

  @PostMapping("/trigger")
  public ResponseEntity<Map<String,Object>> trigger(@RequestBody TriggerRequest req){
    String targetUrl = webhookUrl;
    if(req.workflow()!=null && req.workflow().equalsIgnoreCase("Lead qualification")
        && leadWebhookUrl!=null && !leadWebhookUrl.isBlank()){
      targetUrl = leadWebhookUrl;
    }

    if(targetUrl==null || targetUrl.isBlank())
      return ResponseEntity.status(503).body(Map.of("ok",false,"message","n8n production webhook is not configured"));

    String runId="RUN-"+System.currentTimeMillis();
    long start=System.nanoTime();

    try{
      Map<String,Object> event=new LinkedHashMap<>();
      event.put("runId",runId);
      event.put("event",req.event());
      event.put("workflow",req.workflow());
      event.put("payload",req.payload()==null?Map.of():req.payload());
      event.put("source","autoflow-java-api");

      RestClient.RequestBodySpec request=rest.post().uri(targetUrl).contentType(MediaType.APPLICATION_JSON);
      if(webhookSecret!=null && !webhookSecret.isBlank()) request.header("X-AutoFlow-Secret",webhookSecret);

      String n8nResponse=request.body(event).retrieve().body(String.class);
      long ms=Duration.ofNanos(System.nanoTime()-start).toMillis();

      executions.save(new Execution(runId,req.workflow(),"success",ms));
      return ResponseEntity.ok(Map.of(
        "ok",true,
        "runId",runId,
        "status","sent-to-n8n",
        "durationMs",ms,
        "workflow",req.workflow()==null?"":req.workflow(),
        "n8nResponse",n8nResponse==null?"":n8nResponse
      ));
    }catch(Exception ex){
      long ms=Duration.ofNanos(System.nanoTime()-start).toMillis();
      executions.save(new Execution(runId,req.workflow(),"failed",ms));
      return ResponseEntity.status(502).body(Map.of(
        "ok",false,
        "runId",runId,
        "message","n8n request failed: "+ex.getMessage()
      ));
    }
  }
}

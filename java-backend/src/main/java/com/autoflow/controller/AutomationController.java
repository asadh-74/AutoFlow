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
    if(req.workflow()==null || req.workflow().isBlank())
      return ResponseEntity.badRequest().body(Map.of("ok",false,"message","Choose a supported workflow"));
    if(req.workflow().equals("Demo customer onboarding") || req.workflow().equals("Demo lead qualification")) {
      String runId="DEMO-"+java.util.UUID.randomUUID();
      long started=System.nanoTime();
      Map<String,Object> payload=req.payload()==null?Map.of():req.payload();
      Map<String,Object> result=new LinkedHashMap<>();
      result.put("mode","local-demo");
      result.put("notificationsSent",false);
      Map<String,String> customer=new LinkedHashMap<>();
      for(String key:java.util.List.of("name","email","company")){
        String value=String.valueOf(payload.getOrDefault(key,"Demo customer"));
        customer.put(key,value.substring(0,Math.min(value.length(),200)));
      }
      result.put("customer",customer);
      if(req.workflow().equals("Demo lead qualification")) {
        Object raw=payload.getOrDefault("intent","low");
        int score="high".equalsIgnoreCase(String.valueOf(raw))?85:35;
        result.put("score",score);
        result.put("decision",score>=70?"qualified":"needs-review");
      } else {
        result.put("steps",java.util.List.of("Validate demo payload","Prepare onboarding checklist","Record execution"));
        result.put("checklist",java.util.List.of("Review requirements","Prepare workspace","Schedule kickoff"));
      }
      long ms=Duration.ofNanos(System.nanoTime()-started).toMillis();
      Execution execution=new Execution(runId,req.workflow(),"success",ms);
      try{execution.setOutputJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result));}
      catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException("Could not serialize workflow output",ex);}
      executions.save(execution);
      return ResponseEntity.ok(Map.of("ok",true,"runId",runId,"status","completed-local-demo","result",result));
    }
    if(!java.util.Set.of("Customer onboarding","Lead qualification").contains(req.workflow()))
      return ResponseEntity.badRequest().body(Map.of("ok",false,"message","Unsupported workflow"));
    String targetUrl = req.workflow().equals("Lead qualification") ? leadWebhookUrl : webhookUrl;
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
        "message","n8n request failed. Check the private service logs and webhook configuration."
      ));
    }
  }
}

package com.autoflow.controller;
import com.autoflow.repository.ExecutionRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Map;

class AutomationControllerTest {
 @Test void localDemoIsRecordedWithoutExternalWebhook() {
  ExecutionRepository repo=mock(ExecutionRepository.class);
  var response=new AutomationController(repo).trigger(new AutomationController.TriggerRequest("manual_trigger","Demo customer onboarding",Map.of()));
  assertEquals(200,response.getStatusCode().value());
  assertEquals("completed-local-demo",response.getBody().get("status"));
  assertEquals(false,((Map<?,?>)response.getBody().get("result")).get("notificationsSent"));
  verify(repo).save(any());
 }
 @Test void leadDemoReturnsQualification() {
  ExecutionRepository repo=mock(ExecutionRepository.class);
  var response=new AutomationController(repo).trigger(new AutomationController.TriggerRequest("manual_trigger","Demo lead qualification",Map.of("intent","high")));
  assertEquals("qualified",((Map<?,?>)response.getBody().get("result")).get("decision"));
 }
 @Test void unknownWorkflowRejected() {
  ExecutionRepository repo=mock(ExecutionRepository.class);
  var response=new AutomationController(repo).trigger(new AutomationController.TriggerRequest("manual_trigger","Unconfigured task",Map.of()));
  assertEquals(400,response.getStatusCode().value());verifyNoInteractions(repo);
 }
}

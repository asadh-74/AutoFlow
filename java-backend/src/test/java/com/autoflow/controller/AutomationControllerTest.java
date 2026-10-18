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
  var captured=org.mockito.ArgumentCaptor.forClass(com.autoflow.model.Execution.class);
  verify(repo).save(captured.capture());
  assertTrue(captured.getValue().getOutputJson().contains("checklist"));
 }
 @Test void leadDemoReturnsQualification() {
  ExecutionRepository repo=mock(ExecutionRepository.class);
  var response=new AutomationController(repo).trigger(new AutomationController.TriggerRequest("manual_trigger","Demo lead qualification",Map.of("intent","high")));
  assertEquals("qualified",((Map<?,?>)response.getBody().get("result")).get("decision"));
 }
 @Test void unconfiguredLeadDoesNotUseCustomerWebhook() throws Exception {
  ExecutionRepository repo=mock(ExecutionRepository.class);
  var controller=new AutomationController(repo);
  var field=AutomationController.class.getDeclaredField("webhookUrl");field.setAccessible(true);field.set(controller,"https://example.invalid/customer");
  var response=controller.trigger(new AutomationController.TriggerRequest("manual_trigger","Lead qualification",Map.of()));
  assertEquals(503,response.getStatusCode().value());verifyNoInteractions(repo);
 }
 @Test void unknownWorkflowRejected() {
  ExecutionRepository repo=mock(ExecutionRepository.class);
  var response=new AutomationController(repo).trigger(new AutomationController.TriggerRequest("manual_trigger","Unconfigured task",Map.of()));
  assertEquals(400,response.getStatusCode().value());verifyNoInteractions(repo);
 }
}

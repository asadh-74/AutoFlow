package com.autoflow.controller;
import com.autoflow.model.*;import com.autoflow.repository.*;
import org.junit.jupiter.api.Test;import org.springframework.transaction.PlatformTransactionManager;import org.springframework.transaction.support.SimpleTransactionStatus;import org.springframework.web.server.ResponseStatusException;
import java.util.*;import static org.mockito.Mockito.*;import static org.mockito.ArgumentMatchers.*;import static org.junit.jupiter.api.Assertions.*;
class LeadControllerTest {
 @Test void createsContactAndNotificationOnceThenReplays(){
  CustomerRepository customers=mock(CustomerRepository.class);LeadCaptureRepository captures=mock(LeadCaptureRepository.class);ExecutionRepository executions=mock(ExecutionRepository.class);PlatformTransactionManager manager=mock(PlatformTransactionManager.class);
  when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());when(customers.findFirstByEmailIgnoreCase(any())).thenReturn(Optional.empty());
  when(customers.saveAndFlush(any())).thenAnswer(a->{Customer c=a.getArgument(0);c.setId(42L);return c;});
  Map<String,LeadCapture> stored=new HashMap<>();when(captures.findById(any())).thenAnswer(a->Optional.ofNullable(stored.get(a.getArgument(0))));when(captures.save(any())).thenAnswer(a->{LeadCapture l=a.getArgument(0);stored.put(l.submissionId,l);return l;});
  LeadController ctl=new LeadController(customers,captures,executions,manager);var lead=new LeadController.Lead("Jordan","JORDAN@example.com","Northstar","CRM automation","test-submission-0001");
  assertEquals(false,ctl.capture(lead).get("replayed"));assertEquals(true,ctl.capture(lead).get("replayed"));verify(customers,times(1)).saveAndFlush(any());verify(executions,times(1)).save(any());assertEquals("unread",stored.get(lead.submissionId()).notificationStatus);
  assertThrows(ResponseStatusException.class,()->ctl.capture(new LeadController.Lead("Changed",lead.email(),lead.company(),lead.message(),lead.submissionId())));
  ctl.acknowledge(lead.submissionId());assertEquals("acknowledged",stored.get(lead.submissionId()).notificationStatus);
 }
 @Test void sameEmailUpdatesExistingCustomer(){
  var c=mock(CustomerRepository.class);var l=mock(LeadCaptureRepository.class);var e=mock(ExecutionRepository.class);var m=mock(PlatformTransactionManager.class);when(m.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
  Customer existing=new Customer();existing.setId(7L);existing.setStatus("Active");when(c.findFirstByEmailIgnoreCase("jordan@example.com")).thenReturn(Optional.of(existing));when(l.findById(any())).thenReturn(Optional.empty());
  var ctl=new LeadController(c,l,e,m);var out=ctl.capture(new LeadController.Lead("Jordan Avery","jordan@example.com","New Company","Follow up","test-submission-0002"));
  assertEquals("updated-existing-contact",((LeadCapture)out.get("capture")).outcome);assertEquals("Active",existing.getStatus());assertEquals("New Company",existing.getCompany());
  assertThrows(ResponseStatusException.class,()->ctl.capture(new LeadController.Lead("Real","real@private.org","Company","Hello","test-submission-0003")));
 }
}

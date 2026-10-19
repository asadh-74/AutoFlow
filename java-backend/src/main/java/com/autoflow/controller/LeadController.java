package com.autoflow.controller;
import com.autoflow.model.*;
import com.autoflow.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@RestController @RequestMapping("/api/leads")
public class LeadController {
 private final CustomerRepository customers; private final LeadCaptureRepository captures; private final ExecutionRepository executions; private final TransactionTemplate tx;
 public LeadController(CustomerRepository c,LeadCaptureRepository l,ExecutionRepository e,PlatformTransactionManager m){customers=c;captures=l;executions=e;tx=new TransactionTemplate(m);}
 public record Lead(@NotBlank @Size(max=100) String name,@NotBlank @Email @Size(max=200) String email,@NotNull @Size(max=150) String company,@NotBlank @Size(max=2000) String message,@Pattern(regexp="[a-zA-Z0-9-]{16,64}") @NotBlank String submissionId){}
 @GetMapping public List<LeadCapture> history(){return captures.findTop50ByOrderByCreatedAtDesc();}
 @PostMapping public synchronized Map<String,Object> capture(@Valid @RequestBody Lead in){return tx.execute(status->{
  String email=in.email().trim().toLowerCase(Locale.ROOT);
  if(!email.endsWith("@example.com"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a fictional example.com email in this public demo");
  String fingerprint;try{fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((in.name().trim()+"\n"+email+"\n"+in.company().trim()+"\n"+in.message().trim()).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}
  var prior=captures.findById(in.submissionId());if(prior.isPresent()){
   if(!prior.get().fingerprint.equals(fingerprint))throw new ResponseStatusException(HttpStatus.CONFLICT,"Submission ID already used for different data. Start a new enquiry.");
   return result(prior.get(),true);
  }
  if(captures.count()>=1000)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Demo capacity reached");
  var existing=customers.findFirstByEmailIgnoreCase(email);Customer customer=existing.orElseGet(Customer::new);
  customer.setName(in.name().trim());customer.setEmail(email);customer.setCompany(in.company().trim());if(existing.isEmpty())customer.setStatus("Lead");customers.saveAndFlush(customer);
  LeadCapture lead=new LeadCapture();lead.submissionId=in.submissionId();lead.fingerprint=fingerprint;lead.customerId=customer.getId();lead.message=in.message().trim();lead.outcome=existing.isPresent()?"updated-existing-contact":"created-contact";captures.save(lead);
  Execution execution=new Execution("LEAD-"+in.submissionId(),"Lead-to-CRM capture","success",0L);
  execution.setOutputJson("{\"customerId\":"+customer.getId()+",\"outcome\":\""+lead.outcome+"\",\"notification\":\"internal-inbox\"}");executions.save(execution);return result(lead,false);
 });}
 private Map<String,Object> result(LeadCapture l,boolean replay){return Map.of("capture",l,"replayed",replay,"crm","AutoFlow PostgreSQL CRM","notification","Internal staff inbox; no external messages sent","steps",List.of("Validated enquiry","Checked normalized email",l.outcome,"Stored staff notification","Recorded execution"));}
 @PostMapping("/{id}/acknowledge") public synchronized LeadCapture acknowledge(@PathVariable String id){var l=captures.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));l.notificationStatus="acknowledged";return captures.save(l);}
}

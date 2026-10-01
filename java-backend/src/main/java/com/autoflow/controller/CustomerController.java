package com.autoflow.controller;
import com.autoflow.model.Customer;
import com.autoflow.repository.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/customers")
public class CustomerController {
  private final CustomerRepository repo;
  public CustomerController(CustomerRepository repo){this.repo=repo;}
  @GetMapping public List<Customer> all(){return repo.findAll();}
  @PostMapping public ResponseEntity<Customer> create(@Valid @RequestBody Customer c){return ResponseEntity.status(201).body(repo.save(c));}
  @PutMapping("/{id}") public Customer update(@PathVariable Long id,@RequestBody Customer in){
    Customer c=repo.findById(id).orElseThrow();
    if(in.getName()!=null)c.setName(in.getName()); if(in.getEmail()!=null)c.setEmail(in.getEmail());
    if(in.getCompany()!=null)c.setCompany(in.getCompany()); if(in.getStatus()!=null)c.setStatus(in.getStatus());
    return repo.save(c);
  }
  @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id){repo.deleteById(id);return ResponseEntity.noContent().build();}
}

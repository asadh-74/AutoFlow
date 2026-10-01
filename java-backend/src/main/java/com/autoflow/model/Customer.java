package com.autoflow.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
@Entity @Table(name="customers")
public class Customer {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @NotBlank private String name;
  @Email @Column(unique=true) private String email;
  private String company;
  private String status="Active";
  private Instant createdAt=Instant.now();
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getName(){return name;} public void setName(String name){this.name=name;}
  public String getEmail(){return email;} public void setEmail(String email){this.email=email;}
  public String getCompany(){return company;} public void setCompany(String company){this.company=company;}
  public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
  public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
}

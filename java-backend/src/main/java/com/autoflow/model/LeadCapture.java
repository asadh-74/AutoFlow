package com.autoflow.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="lead_captures")
public class LeadCapture {
 @Id public String submissionId;
 @Column(nullable=false,length=64) public String fingerprint;
 public Long customerId;
 public String outcome;
 public String notificationStatus="unread";
 @Column(length=2000) public String message;
 public Instant createdAt=Instant.now();
 public LeadCapture(){}
}

package com.autoflow.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="workflow_executions")
public class Execution {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private String runId; private String workflow; private String status; private Long durationMs;
  private Instant startedAt=Instant.now();
  public Execution(){}
  public Execution(String runId,String workflow,String status,Long durationMs){this.runId=runId;this.workflow=workflow;this.status=status;this.durationMs=durationMs;}
  public Long getId(){return id;} public String getRunId(){return runId;} public void setRunId(String v){runId=v;}
  public String getWorkflow(){return workflow;} public void setWorkflow(String v){workflow=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;}
  public Long getDurationMs(){return durationMs;} public void setDurationMs(Long v){durationMs=v;}
  public Instant getStartedAt(){return startedAt;} public void setStartedAt(Instant v){startedAt=v;}
}

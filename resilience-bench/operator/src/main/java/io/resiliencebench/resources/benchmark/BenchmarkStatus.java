package io.resiliencebench.resources.benchmark;

import io.fabric8.crd.generator.annotation.PrinterColumn;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.resiliencebench.resources.Phase;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

public class BenchmarkStatus {

  @PrinterColumn(name = "Phase", priority = 0)
  private String phase;
  
  @PrinterColumn(name = "Total Scenarios", priority = 1)
  private int totalScenarios;
  
  @PrinterColumn(name = "Running", priority = 2)
  private int runningScenarios;
  
  @PrinterColumn(name = "Completed", priority = 3)
  private int completedScenarios;

  private String executionId;
  private String lastReconcileTime;
  private String startTime;
  private String completionTime;
  private String message;
  private Long observedGeneration;

  public BenchmarkStatus() {
  }

  public BenchmarkStatus(int totalScenarios) {
    this.totalScenarios = totalScenarios;
    this.phase = Phase.PENDING;
    this.runningScenarios = 0;
    this.completedScenarios = 0;
    this.executionId = generateExecutionId();
    this.lastReconcileTime = getCurrentTimestamp();
    this.startTime = getCurrentTimestamp();
  }

  public BenchmarkStatus(String phase, int totalScenarios, int runningScenarios, int completedScenarios, 
                        String executionId, Long observedGeneration) {
    this.phase = phase;
    this.totalScenarios = totalScenarios;
    this.runningScenarios = runningScenarios;
    this.completedScenarios = completedScenarios;
    this.executionId = executionId;
    this.observedGeneration = observedGeneration;
    this.lastReconcileTime = getCurrentTimestamp();
  }

  // Getters and setters
  public String getPhase() {
    return phase;
  }

  public void setPhase(String phase) {
    this.phase = phase;
  }

  public int getTotalScenarios() {
    return totalScenarios;
  }

  public void setTotalScenarios(int totalScenarios) {
    this.totalScenarios = totalScenarios;
  }

  public int getRunningScenarios() {
    return runningScenarios;
  }

  public void setRunningScenarios(int runningScenarios) {
    this.runningScenarios = runningScenarios;
  }

  public int getCompletedScenarios() {
    return completedScenarios;
  }

  public void setCompletedScenarios(int completedScenarios) {
    this.completedScenarios = completedScenarios;
  }

  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public String getLastReconcileTime() {
    return lastReconcileTime;
  }

  public void setLastReconcileTime(String lastReconcileTime) {
    this.lastReconcileTime = lastReconcileTime;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public String getCompletionTime() {
    return completionTime;
  }

  public void setCompletionTime(String completionTime) {
    this.completionTime = completionTime;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Long getObservedGeneration() {
    return observedGeneration;
  }

  public void setObservedGeneration(Long observedGeneration) {
    this.observedGeneration = observedGeneration;
  }

  // Utility methods
  @JsonIgnore
  public boolean isRunning() {
    return Phase.RUNNING.equals(phase);
  }

  @JsonIgnore
  public boolean isCompleted() {
    return Phase.COMPLETED.equals(phase) || Phase.FAILED.equals(phase);
  }

  @JsonIgnore
  public boolean needsReconciliation(Long currentGeneration) {
    return observedGeneration == null || !Objects.equals(observedGeneration, currentGeneration);
  }

  public void updateReconcileTime() {
    this.lastReconcileTime = getCurrentTimestamp();
  }

  public void markAsCompleted() {
    this.phase = Phase.COMPLETED;
    this.completionTime = getCurrentTimestamp();
  }

  public void markAsFailed(String errorMessage) {
    this.phase = Phase.FAILED;
    this.completionTime = getCurrentTimestamp();
    this.message = errorMessage;
  }

  public void updateProgress(int running, int completed) {
    this.runningScenarios = running;
    this.completedScenarios = completed;
    
    if (completed == totalScenarios) {
      markAsCompleted();
    } else if (running > 0 || completed > 0) {
      this.phase = Phase.RUNNING;
    }
  }

  private static String getCurrentTimestamp() {
    return LocalDateTime.now().atZone(ZoneId.of("UTC")).toString();
  }

  private static String generateExecutionId() {
    return "exec-" + System.currentTimeMillis();
  }
}

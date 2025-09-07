package io.resiliencebench.resources.queue;

import io.fabric8.crd.generator.annotation.PrinterColumn;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.resiliencebench.resources.Phase;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

public class ExecutionQueueStatus {

    @PrinterColumn(name = "Phase", priority = 0)
    private String phase;
    
    @PrinterColumn(name = "Total Scenarios", priority = 1)
    private int totalScenarios;
    
    @PrinterColumn(name = "Running", priority = 2)
    private int runningScenarios;
    
    @PrinterColumn(name = "Completed", priority = 3)
    private int completedScenarios;

    @PrinterColumn(name = "Pending", priority = 4)
    private int pendingScenarios;

    private String executionId;
    private String lastReconcileTime;
    private String startTime;
    private String completionTime;
    private String message;
    private Long observedGeneration;

    public ExecutionQueueStatus() {
    }

    public ExecutionQueueStatus(int totalScenarios, String executionId) {
        this.totalScenarios = totalScenarios;
        this.phase = Phase.PENDING;
        this.runningScenarios = 0;
        this.completedScenarios = 0;
        this.pendingScenarios = totalScenarios;
        this.executionId = executionId;
        this.lastReconcileTime = getCurrentTimestamp();
        this.startTime = getCurrentTimestamp();
    }

    public ExecutionQueueStatus(String phase, int totalScenarios, int runningScenarios, int completedScenarios, 
                              int pendingScenarios, String executionId, Long observedGeneration) {
        this.phase = phase;
        this.totalScenarios = totalScenarios;
        this.runningScenarios = runningScenarios;
        this.completedScenarios = completedScenarios;
        this.pendingScenarios = pendingScenarios;
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

    public int getPendingScenarios() {
        return pendingScenarios;
    }

    public void setPendingScenarios(int pendingScenarios) {
        this.pendingScenarios = pendingScenarios;
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

    public void updateProgress(int running, int completed, int pending) {
        this.runningScenarios = running;
        this.completedScenarios = completed;
        this.pendingScenarios = pending;
        
        if (completed == totalScenarios) {
            markAsCompleted();
        } else if (running > 0 || completed > 0) {
            this.phase = Phase.RUNNING;
        }
    }

    @JsonIgnore
    public int getRunning() {
        return runningScenarios;
    }

    @JsonIgnore
    public int getPending() {
        return pendingScenarios;
    }

    private static String getCurrentTimestamp() {
        return LocalDateTime.now().atZone(ZoneId.of("UTC")).toString();
    }
}

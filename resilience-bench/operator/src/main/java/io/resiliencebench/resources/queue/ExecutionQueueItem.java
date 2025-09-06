package io.resiliencebench.resources.queue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.resiliencebench.resources.Phase;

public class ExecutionQueueItem {

  @JsonPropertyDescription("The name of the scenario it belongs to.")
  @JsonProperty(required = true)
  private String scenario;
  @JsonPropertyDescription("The status of the execution. Can be 'pending', 'running' or 'finished'. Automatically managed.")
  @JsonProperty(required = true)
  private String phase;
  @JsonPropertyDescription("The path of the file with the item's results. Automatically created.")
  private String resultFile;

  public ExecutionQueueItem(String scenario, String resultFile) {
    this.scenario = scenario;
    this.resultFile = resultFile;
    this.phase = Phase.PENDING;
  }

  public ExecutionQueueItem() {
  }

  public String getScenario() {
    return scenario;
  }

  public void setStatus(String status) {
    this.phase = status;
  }

  public String getStatus() {
    return phase;
  }

  public String getResultFile() {
    return resultFile;
  }

  @JsonIgnore
  public boolean isPending() {
    return phase.equals(Phase.PENDING);
  }

  @JsonIgnore
  public boolean isRunning() {
    return phase.equals(Phase.RUNNING);
  }

  @JsonIgnore
  public boolean isFinished() {
    return phase.equals(Phase.COMPLETED);
  }

  @JsonIgnore
  public void markAsCompleted() {
    this.setStatus(Phase.COMPLETED);
  }

  @JsonIgnore
  public void markAsRunning() {
    this.setStatus(Phase.RUNNING);
  }
}

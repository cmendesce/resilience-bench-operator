package io.resiliencebench.resources.benchmark;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.generator.annotation.Default;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkSpec {

  @JsonPropertyDescription("The workload name to be used for the benchmark")
  private String workload;

  @JsonPropertyDescription("The name of the execution queue to be used for running the benchmark scenarios")
  private String queueName;

  @JsonPropertyDescription("Whether the ExecutionQueue should be automatically created if it does not exist")
  @Default(value = "true")
  private Boolean autoCreateQueue;

  @JsonPropertyDescription("The set of scenarios templates to be processed and then generated as scenarios")
  private List<ScenarioTemplate> scenarios = new ArrayList<>();

  public BenchmarkSpec() {
  }

  public BenchmarkSpec(String workload, List<ScenarioTemplate> scenarioTemplates) {
    this();
    this.workload = workload;
    this.scenarios = scenarioTemplates;
  }

  public String getWorkload() {
    return workload;
  }

  public Boolean isAutoCreateQueue() {
    return autoCreateQueue;
  }

  public String getQueueName() {
    return queueName;
  }

  public List<ScenarioTemplate> getScenarios() {
    return scenarios;
  }
}

package io.resiliencebench.execution;

import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.scenario.Scenario;

public interface ScenarioExecutor {
  void execute(Scenario scenario, ExecutionQueue executionQueue);
}

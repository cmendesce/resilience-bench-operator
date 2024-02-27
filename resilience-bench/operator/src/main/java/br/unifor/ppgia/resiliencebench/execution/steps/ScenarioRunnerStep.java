package br.unifor.ppgia.resiliencebench.execution.steps;

import br.unifor.ppgia.resiliencebench.resources.scenario.Scenario;
import io.fabric8.kubernetes.client.KubernetesClient;

public abstract class ScenarioRunnerStep {

  public ScenarioRunnerStep(KubernetesClient kubernetesClient) {
  }

  public abstract void run(Scenario scenario);
}
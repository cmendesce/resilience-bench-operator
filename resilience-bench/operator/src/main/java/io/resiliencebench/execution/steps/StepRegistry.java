package io.resiliencebench.execution.steps;

import io.resiliencebench.execution.steps.istio.IstioCircuitBreakerStep;
import io.resiliencebench.execution.steps.istio.IstioFaultStep;
import io.resiliencebench.execution.steps.istio.IstioRetryStep;
import io.resiliencebench.execution.steps.istio.IstioTimeoutStep;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.List.of;

@Component
public class StepRegistry {

  private final List<ExecutorStep> preparationSteps;
  private final List<ExecutorStep> postExecutionSteps;

  public StepRegistry(UpdateStatusQueueStep updateStatusQueueStep,
                      ResultFileStep resultFileStep,
                      IstioCircuitBreakerStep istioCircuitBreakerStep,
                      IstioRetryStep istioRetryStep,
                      IstioTimeoutStep istioTimeoutStep,
                      IstioFaultStep istioFaultStep,
                      EnvironmentStep environmentStep,
                      EnvironmentPostStep environmentPostStep,
                      ApplicationReadinessStep applicationReadinessStep,
                      ScenarioFaultEnvVarStep scenarioFaultEnvVarStep,
                      ScenarioFaultAPIStep scenarioFaultAPIStep) {

    preparationSteps = of(
            updateStatusQueueStep,
            istioRetryStep,
            istioCircuitBreakerStep,
            istioTimeoutStep,
            istioFaultStep,
            environmentStep,
            applicationReadinessStep,
            scenarioFaultEnvVarStep,
            applicationReadinessStep);
    postExecutionSteps = of(
            updateStatusQueueStep,
            resultFileStep,
            environmentPostStep);
  }

  public List<ExecutorStep> getPostExecutionSteps() {
    return postExecutionSteps;
  }

  public List<ExecutorStep> getPreparationSteps() {
    return preparationSteps;
  }
}

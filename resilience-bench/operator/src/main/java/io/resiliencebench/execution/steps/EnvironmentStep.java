package io.resiliencebench.execution.steps;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.resources.service.ResilientService;
import io.resiliencebench.support.CustomResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.resiliencebench.support.Annotations.CONTAINER;

@Service
public class EnvironmentStep extends AbstractEnvironmentStep {

  private final static Logger logger = LoggerFactory.getLogger(EnvironmentStep.class);

  public EnvironmentStep(KubernetesClient kubernetesClient,
      CustomResourceRepository<ResilientService> resilientServiceRepository) {
    super(kubernetesClient, resilientServiceRepository);
  }

  @Override
  protected boolean isApplicable(Scenario scenario) {
    return scenario
        .getSpec()
        .getConnectors()
        .stream()
        .anyMatch(connector -> connector.getDestination().getEnvs() != null ||
            connector.getSource().getEnvs() != null);
  }

  public void applyServiceEnvironment(Scenario scenario, io.resiliencebench.resources.scenario.Service service) {
    var env = service.getEnvs();
    if (env == null) {
      return;
    }
    var resilientService = resilientServiceRepository.get(scenario.getMetadata().getNamespace(), service.getName());
    var containerName = resilientService.getMetadata().getAnnotations().get(CONTAINER);

    var deployment = kubernetesClient()
        .apps()
        .deployments()
        .inNamespace(scenario.getMetadata().getNamespace())
        .withLabelSelector(resilientService.getSpec().getSelector())
        .list()
        .getItems()
        .stream()
        .findFirst();

    if (deployment.isPresent()) {
      var deploymentVars = getActualEnv(deployment.get(), containerName);
      saveActualEnv(deploymentVars, resilientService);

      for (var variable : deploymentVars) {
        var newValue = env.get(variable.getName());
        if (newValue != null) {
          logger.info("Container {}. EnvVar {}={}", containerName, variable.getName(), newValue);
          variable.setValue(newValue.toString());
        }
      }
      updateDeployment(deployment.get());
      restartPods(deployment.get());
    } else {
      logger.warn("Deployment not found for ResilientService {}", service.getName());
    }
  }

  @Override
  protected Deployment internalExecute(Scenario scenario, ExecutionQueue queue) {
    for (var connector : scenario.getSpec().getConnectors()) {
      applyServiceEnvironment(scenario, connector.getSource());
      applyServiceEnvironment(scenario, connector.getDestination());
    }
    return null;
  }

  private void saveActualEnv(List<EnvVar> deploymentVars, ResilientService resilientService) {
    resilientService.getSpec().setEnvs(deploymentVars);
    resilientServiceRepository.update(resilientService);
  }
}

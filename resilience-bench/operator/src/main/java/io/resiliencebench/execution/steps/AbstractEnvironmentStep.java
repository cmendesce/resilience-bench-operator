package io.resiliencebench.execution.steps;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.resources.service.ResilientService;
import io.resiliencebench.support.CustomResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

abstract class AbstractEnvironmentStep extends ExecutorStep {

  private final static Logger logger = LoggerFactory.getLogger(AbstractEnvironmentStep.class);
  protected final CustomResourceRepository<ResilientService> resilientServiceRepository;

  public AbstractEnvironmentStep(KubernetesClient kubernetesClient,
                                 CustomResourceRepository<ResilientService> resilientServiceRepository) {
    super(kubernetesClient);
    this.resilientServiceRepository = resilientServiceRepository;
  }

  protected Deployment getDeployment(Scenario scenario, ResilientService resilientService) {
    return kubernetesClient()
            .apps()
            .deployments()
            .inNamespace(scenario.getMetadata().getNamespace())
            .withLabelSelector(resilientService.getSpec().getSelector())
            .list()
            .getItems()
            .stream()
            .findFirst()
            .orElse(null);
  }

  private void internalUpdateVariablesDeployment(Deployment targetDeployment, String containerName, List<EnvVar> envVars) {
    var deployment = kubernetesClient()
            .apps()
            .deployments()
            .inNamespace(targetDeployment.getMetadata().getNamespace())
            .withName(targetDeployment.getMetadata().getName())
            .get();

    var container = deployment
            .getSpec()
            .getTemplate()
            .getSpec()
            .getContainers()
            .stream()
            .filter(c -> c.getName().equals(containerName))
            .findFirst();

    if (container.isPresent()) {
      container.get().setEnv(envVars);
      kubernetesClient().apps().deployments().inNamespace(targetDeployment.getMetadata().getNamespace()).resource(deployment).update();
    }
  }

  protected void updateVariablesDeployment(Deployment targetDeployment, String containerName, List<EnvVar> envVars) {
    Retry.of("updateVariablesDeployment", RetryConfig.custom().maxAttempts(3).build())
            .executeRunnable(() -> internalUpdateVariablesDeployment(targetDeployment, containerName, envVars));
  }

  protected void waitUntilReady(Deployment targetDeployment) {
    logger.info("Restarting deployment: {}", targetDeployment.getMetadata().getName());
    getPods(targetDeployment).waitUntilCondition(this::waitUntilCondition, 2, TimeUnit.MINUTES);
    logger.info("Deployment restarted: {}", targetDeployment.getMetadata().getName());
  }

  protected List<EnvVar> getActualContainerEnv(Deployment targetDeployment, String containerName) {
    return targetDeployment.getSpec().getTemplate().getSpec().getContainers().stream()
            .filter(c -> c.getName().equals(containerName))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Container not found: " + containerName))
            .getEnv();
  }

  public FilterWatchListDeletable<Pod, PodList, PodResource> getPods(Deployment targetDeployment) {
    return kubernetesClient().pods()
            .inNamespace(targetDeployment.getMetadata().getNamespace())
            .withLabel("app", targetDeployment.getMetadata().getName());
  }

  /**
   * Test if the pod is ready, if yes return true, otherwise return false
   */
  public boolean waitUntilCondition(Pod pod) {
    var isMarkedForDeletion = pod.getMetadata().getDeletionTimestamp() != null;
    if (isMarkedForDeletion) {
      logger.info("Pod marked for deletion {}", pod.getMetadata().getName());
      return false;
    }
    var isReady = pod.getStatus()
            .getConditions()
            .stream()
            .anyMatch(condition -> "Ready".equals(condition.getType()) && "True".equals(condition.getStatus()));
    if (isReady) {
      logger.info("Pod ready {}", pod.getMetadata().getName());
    }

    return isReady;
  }
}
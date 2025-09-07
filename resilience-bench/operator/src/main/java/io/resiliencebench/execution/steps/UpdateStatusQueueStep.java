package io.resiliencebench.execution.steps;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.support.CustomResourceRepository;
import io.resiliencebench.execution.ExecutionQueueStatusUpdater;
import org.springframework.stereotype.Service;

import static java.time.Duration.ofSeconds;

@Service
public class UpdateStatusQueueStep extends ExecutorStep {

  private final CustomResourceRepository<ExecutionQueue> executionRepository;
  private final ExecutionQueueStatusUpdater statusUpdater;

  private final RetryConfig retryConfig;

  public UpdateStatusQueueStep(KubernetesClient kubernetesClient, 
                              CustomResourceRepository<ExecutionQueue> executionRepository,
                              ExecutionQueueStatusUpdater statusUpdater) {
    super(kubernetesClient);
    this.executionRepository = executionRepository;
    this.statusUpdater = statusUpdater;
    this.retryConfig = RetryConfig
            .custom()
            .retryExceptions(KubernetesClientException.class)
            .waitDuration(ofSeconds(1))
            .maxAttempts(3)
            .build();
  }

  @Override
  protected boolean isApplicable(Scenario scenario) {
    return true;
  }

  private void updateQueueItem(String queueName, String scenarioName, String namespace) {
    var queue = executionRepository.get(namespace, queueName);
    var queueItem = queue.getItem(scenarioName);
    if (queueItem.isRunning()) {
      queueItem.markAsCompleted();
    } else if (queueItem.isPending()) {
      queueItem.markAsRunning();
    }

    queue.getMetadata().setNamespace(namespace);
    executionRepository.update(queue);
  }

  @Override
  public void internalExecute(Scenario scenario, ExecutionQueue executionQueue) {
    var queueName = executionQueue.getMetadata().getName();
    var scenarioName = scenario.getMetadata().getName();
    var namespace = scenario.getMetadata().getNamespace();
    Retry.of("updateQueueItem", retryConfig)
            .executeRunnable(() -> updateStatus(queueName, scenarioName, namespace));
  }

  private void updateStatus(String queueName, String scenarioName, String namespace) {
    updateQueueItem(queueName, scenarioName, namespace);
    var queue = executionRepository.get(namespace, queueName);
    var queueItem = queue.getItem(scenarioName);
    if (queueItem.isPending()) {
        statusUpdater.markScenarioAsStarted(namespace, queueName, scenarioName);
    } else if (queueItem.isFinished()) {
        statusUpdater.markScenarioAsCompleted(namespace, queueName, scenarioName);
    }
  }
}

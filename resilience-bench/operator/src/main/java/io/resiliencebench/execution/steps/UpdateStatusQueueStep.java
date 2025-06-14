package io.resiliencebench.execution.steps;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.queue.ExecutionQueueItem;
import io.resiliencebench.resources.queue.ExecutionQueueStatus;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.support.CustomResourceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static io.resiliencebench.resources.queue.ExecutionQueueItem.Status.*;
import static java.time.Duration.ofSeconds;
import static java.time.ZoneId.*;
import static java.util.stream.Collectors.*;

@Service
public class UpdateStatusQueueStep extends ExecutorStep {

  private final CustomResourceRepository<ExecutionQueue> executionRepository;

  private final RetryConfig retryConfig;

  public UpdateStatusQueueStep(KubernetesClient kubernetesClient, CustomResourceRepository<ExecutionQueue> executionRepository) {
    super(kubernetesClient);
    this.executionRepository = executionRepository;
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
    var now = LocalDateTime.now().atZone(of("UTC")).toString();
    if (queueItem.isRunning()) {
      queueItem.setStatus(FINISHED);
      queueItem.setFinishedAt(now);
    } else if (queueItem.isPending()) {
      queueItem.setStatus(RUNNING);
      queueItem.setStartedAt(now);
    }

    queue.getMetadata().setNamespace(namespace);
    executionRepository.update(queue);
    queue.setStatus(creteStatus(queue));
    executionRepository.updateStatus(queue);
  }

  private static ExecutionQueueStatus creteStatus(ExecutionQueue queue) {
    var statusCounts = queue.getSpec().getItems().stream().collect(groupingBy(ExecutionQueueItem::getStatus, counting()));
    return new ExecutionQueueStatus(
            statusCounts.getOrDefault(RUNNING, 0L),
            statusCounts.getOrDefault(PENDING, 0L),
            statusCounts.getOrDefault(FINISHED, 0L)
    );
  }

  @Override
  public void internalExecute(Scenario scenario, ExecutionQueue executionQueue) {
    Retry.of("updateQueueItem", retryConfig)
            .executeRunnable(() ->
                    updateQueueItem(executionQueue.getMetadata().getName(), scenario.getMetadata().getName(), scenario.getMetadata().getNamespace())
            );

  }
}

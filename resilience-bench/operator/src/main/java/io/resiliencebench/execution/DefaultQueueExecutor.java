
package io.resiliencebench.execution;

import static java.lang.String.format;

import io.resiliencebench.resources.queue.ExecutionQueueItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.support.CustomResourceRepository;

@Service
public class DefaultQueueExecutor implements QueueExecutor {

  private final static Logger logger = LoggerFactory.getLogger(DefaultQueueExecutor.class);

  private final CustomResourceRepository<Scenario> scenarioRepository;
  private final CustomResourceRepository<ExecutionQueue> executionRepository;

  private final ScenarioExecutor scenarioExecutor;

  public DefaultQueueExecutor(
          CustomResourceRepository<Scenario> scenarioRepository,
          CustomResourceRepository<ExecutionQueue> executionRepository,
          ScenarioExecutor scenarioExecutor) {
    this.scenarioRepository = scenarioRepository;
    this.executionRepository = executionRepository;
    this.scenarioExecutor = scenarioExecutor;
  }

  @Override
  public void execute(ExecutionQueue queue) {
    var queueToExecute = executionRepository.find(queue.getMetadata())
            .orElseThrow(() -> new RuntimeException("Queue not found " + queue.getMetadata().getName()));

    if (queueToExecute.isRunning()) {
      logger.info("Queue has item running: {}", queueToExecute.getMetadata().getName());
      return;
    }

    var nextItem = queueToExecute.getNextPendingItem();

    if (nextItem.isPresent() && nextItem.get().isPending()) {
      executeScenario(nextItem.get(), queueToExecute);
    } else {
      logger.info("No item available for queue: {}", queueToExecute.getMetadata().getName());
      if (queueToExecute.isDone()) {
        logger.info("All items finished for: {}", queueToExecute.getMetadata().getName());
      }
    }
  }

  private void executeScenario(ExecutionQueueItem item, ExecutionQueue executionQueue) {
    var scenarioName = item.getScenario();
    var namespace = executionQueue.getMetadata().getNamespace();
    var scenario = scenarioRepository.find(namespace, scenarioName);
    if (scenario.isPresent()) {
      logger.info("Running scenario: {}", scenarioName);
      scenarioExecutor.execute(scenario.get(), executionQueue, () -> execute(executionQueue));
    } else {
      throw new RuntimeException(format("Scenario not found: %s.%s", namespace, scenarioName));
    }
  }
}

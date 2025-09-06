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

  private final ScenarioExecutor scenarioExecutor;

  public DefaultQueueExecutor(CustomResourceRepository<Scenario> scenarioRepository, ScenarioExecutor scenarioExecutor) {
    this.scenarioRepository = scenarioRepository;
    this.scenarioExecutor = scenarioExecutor;
  }

  @Override
  public void execute(ExecutionQueue queue) {
    var nextItem = queue.getNextPendingItem();

    if (nextItem.isPresent() && nextItem.get().isPending()) {
      internalExecute(nextItem.get(), queue);
    } else {
      logger.info("No pending items available for queue: {}", queue.getMetadata().getName());
      if (queue.isDone()) {
        logger.info("All items finished for: {}", queue.getMetadata().getName());
      }
    }
  }

  private void internalExecute(ExecutionQueueItem item, ExecutionQueue executionQueue) {
    var scenarioName = item.getScenario();
    var namespace = executionQueue.getMetadata().getNamespace();
    var scenario = scenarioRepository.find(namespace, scenarioName);
    if (scenario.isPresent()) {
      logger.info("Running scenario: {}", scenarioName);
      scenarioExecutor.execute(scenario.get(), executionQueue);
    } else {
      throw new RuntimeException(format("Scenario not found: %s.%s", namespace, scenarioName));
    }
  }
}

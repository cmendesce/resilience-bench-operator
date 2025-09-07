package io.resiliencebench.execution;

import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.queue.ExecutionQueueItem;
import io.resiliencebench.support.CustomResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExecutionQueueStatusUpdater {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionQueueStatusUpdater.class);

    private final CustomResourceRepository<ExecutionQueue> queueRepository;

    public ExecutionQueueStatusUpdater(CustomResourceRepository<ExecutionQueue> queueRepository) {
        this.queueRepository = queueRepository;
    }

    /**
     * Updates the execution queue status based on the current queue items state
     */
    public void updateQueueProgress(String namespace, String queueName) {
        try {
            var queueOpt = queueRepository.find(namespace, queueName);

            if (queueOpt.isEmpty()) {
                logger.warn("Execution queue not found: {}/{}", namespace, queueName);
                return;
            }

            var queue = queueOpt.get();
            updateQueueStatus(queue);
            queueRepository.updateStatus(queue);
            
            logger.debug("Updated queue {} progress: {} running, {} completed, {} pending, {} total", 
                        queueName, queue.getStatus().getRunning(), queue.getStatus().getCompletedScenarios(), 
                        queue.getStatus().getPending(), queue.getStatus().getTotalScenarios());

        } catch (Exception e) {
            logger.error("Error updating execution queue status for: " + namespace + "/" + queueName, e);
        }
    }

    /**
     * Marks a specific scenario as started in the execution queue
     */
    public void markScenarioAsStarted(String namespace, String queueName, String scenarioName) {
        try {
            logger.info("Marking scenario {} as started for queue {}/{}", scenarioName, namespace, queueName);
            var queueOpt = queueRepository.find(namespace, queueName);
            
            if (queueOpt.isEmpty()) {
                logger.warn("Execution queue not found: {}/{}", namespace, queueName);
                return;
            }

            var queue = queueOpt.get();
            var queueItem = queue.getItem(scenarioName);
            
            if (queueItem != null && queueItem.isPending()) {
                queueItem.markAsRunning();
                updateQueueStatus(queue);
                queueRepository.update(queue);
            }
            
        } catch (Exception e) {
            logger.error("Error marking scenario as started: " + scenarioName, e);
        }
    }

    /**
     * Marks a specific scenario as completed in the execution queue
     */
    public void markScenarioAsCompleted(String namespace, String queueName, String scenarioName) {
        try {
            logger.info("Marking scenario {} as completed for queue {}/{}", scenarioName, namespace, queueName);
            var queueOpt = queueRepository.find(namespace, queueName);
            
            if (queueOpt.isEmpty()) {
                logger.warn("Execution queue not found: {}/{}", namespace, queueName);
                return;
            }

            var queue = queueOpt.get();
            var queueItem = queue.getItem(scenarioName);
            
            if (queueItem != null && queueItem.isRunning()) {
                queueItem.markAsCompleted();
                updateQueueStatus(queue);
                queueRepository.update(queue);
            }
            
        } catch (Exception e) {
            logger.error("Error marking scenario as completed: " + scenarioName, e);
        }
    }

    /**
     * Updates the execution queue status based on current items state
     */
    private void updateQueueStatus(ExecutionQueue queue) {
        queue.updateStatusFromItems();
        
        // Set observed generation if available
        if (queue.getMetadata() != null && queue.getMetadata().getGeneration() != null) {
            queue.getStatus().setObservedGeneration(queue.getMetadata().getGeneration());
        }
    }
}

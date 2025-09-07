package io.resiliencebench;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.resiliencebench.execution.QueueExecutor;
import io.resiliencebench.execution.ExecutionQueueStatusUpdater;
import io.resiliencebench.resources.benchmark.Benchmark;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.support.CustomResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerConfiguration
public class ExecutionQueueController implements Reconciler<ExecutionQueue> {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionQueueController.class);

    private final QueueExecutor queueExecutor;
    private final ExecutionQueueStatusUpdater statusUpdater;

    public ExecutionQueueController(QueueExecutor queueExecutor,
                                   ExecutionQueueStatusUpdater statusUpdater) {
        this.queueExecutor = queueExecutor;
        this.statusUpdater = statusUpdater;
    }

    @Override
    public UpdateControl<ExecutionQueue> reconcile(ExecutionQueue queue, Context<ExecutionQueue> context) {
        logger.info("Reconciling ExecutionQueue: {}", queue.getMetadata().getName());

        // Initialize status if needed
        if (queue.getStatus() == null) {
            queue.updateStatusFromItems();
        }
        
        // Update status based on current items state
        statusUpdater.updateQueueProgress(queue.getMetadata().getNamespace(), queue.getMetadata().getName());
        
        if (queue.isDone()) {
            logger.info("ExecutionQueue {} is already completed", queue.getMetadata().getName());
            return UpdateControl.noUpdate();
        }

        // Only execute if there are pending items and no running items
        if (queue.getStatus() != null && 
            queue.getStatus().getRunning() == 0 && 
            queue.getStatus().getPending() > 0) {
            logger.info("Starting execution of next item in queue: {}", queue.getMetadata().getName());
            queueExecutor.execute(queue);
        } else {
            logger.debug("Queue {} is already being processed or has no pending items", queue.getMetadata().getName());
        }

        return UpdateControl.updateStatus(queue);
    }
} 
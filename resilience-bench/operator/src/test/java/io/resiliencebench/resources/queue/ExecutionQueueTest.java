package io.resiliencebench.resources.queue;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.resiliencebench.resources.Phase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionQueueTest {

    private ExecutionQueueSpec spec;
    private ExecutionQueue queue;

    @BeforeEach
    void setUp() {
        var items = List.of(
            new ExecutionQueueItem("scenario-1", "results/scenario-1.json"),
            new ExecutionQueueItem("scenario-2", "results/scenario-2.json"),
            new ExecutionQueueItem("scenario-3", "results/scenario-3.json")
        );
        
        spec = new ExecutionQueueSpec("results/results.json", items, "test-benchmark");
    }

    @Test
    void shouldInitializeStatusWhenCreatingQueueWithConstructor() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .addToLabels("execution-id", "exec-123456")
                .build();

        queue = new ExecutionQueue(spec, meta);

        assertNotNull(queue.getStatus());
        assertEquals(Phase.PENDING, queue.getStatus().getPhase());
        assertEquals(3, queue.getStatus().getTotalScenarios());
        assertEquals(0, queue.getStatus().getRunningScenarios());
        assertEquals(0, queue.getStatus().getCompletedScenarios());
        assertEquals(3, queue.getStatus().getPendingScenarios());
        assertEquals("exec-123456", queue.getStatus().getExecutionId());
        assertNotNull(queue.getStatus().getLastReconcileTime());
        assertNotNull(queue.getStatus().getStartTime());
    }

    @Test
    void shouldInitializeStatusWithGeneratedExecutionIdWhenNoLabelExists() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(spec, meta);

        assertNotNull(queue.getStatus());
        assertNotNull(queue.getStatus().getExecutionId());
        assertTrue(queue.getStatus().getExecutionId().startsWith("exec-"));
    }

    @Test
    void shouldNotInitializeStatusWhenSpecIsNull() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(null, meta);

        assertNull(queue.getStatus());
    }

    @Test
    void shouldNotInitializeStatusWhenItemsIsNull() {
        var emptySpec = new ExecutionQueueSpec("results.json", null, "test-benchmark");
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(emptySpec, meta);

        assertNull(queue.getStatus());
    }

    @Test
    void shouldUpdateStatusFromItemsWithMixedStates() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .addToLabels("execution-id", "exec-789")
                .build();

        // Create items with different states
        var pendingItem = new ExecutionQueueItem("scenario-1", "results/scenario-1.json");
        var runningItem = new ExecutionQueueItem("scenario-2", "results/scenario-2.json");
        runningItem.markAsRunning();
        var completedItem = new ExecutionQueueItem("scenario-3", "results/scenario-3.json");
        completedItem.markAsCompleted();

        var mixedSpec = new ExecutionQueueSpec("results.json", 
                                             List.of(pendingItem, runningItem, completedItem), 
                                             "test-benchmark");
        
        queue = new ExecutionQueue(mixedSpec, meta);
        
        // Manually change some item states to test updateStatusFromItems
        queue.getSpec().getItems().get(0).markAsRunning(); // scenario-1 now running
        
        queue.updateStatusFromItems();

        assertEquals(2, queue.getStatus().getRunningScenarios());
        assertEquals(1, queue.getStatus().getCompletedScenarios());
        assertEquals(0, queue.getStatus().getPendingScenarios());
        assertEquals(Phase.RUNNING, queue.getStatus().getPhase());
    }

    @Test
    void shouldUpdateStatusFromItemsWhenAllCompleted() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(spec, meta);
        
        // Mark all items as completed
        queue.getSpec().getItems().forEach(ExecutionQueueItem::markAsCompleted);
        
        queue.updateStatusFromItems();

        assertEquals(0, queue.getStatus().getRunningScenarios());
        assertEquals(3, queue.getStatus().getCompletedScenarios());
        assertEquals(0, queue.getStatus().getPendingScenarios());
        assertEquals(Phase.COMPLETED, queue.getStatus().getPhase());
        assertNotNull(queue.getStatus().getCompletionTime());
    }

    @Test
    void shouldUpdateStatusFromItemsWhenAllPending() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(spec, meta);
        
        // All items are pending by default
        queue.updateStatusFromItems();

        assertEquals(0, queue.getStatus().getRunningScenarios());
        assertEquals(0, queue.getStatus().getCompletedScenarios());
        assertEquals(3, queue.getStatus().getPendingScenarios());
        assertEquals(Phase.PENDING, queue.getStatus().getPhase());
    }

    @Test
    void shouldInitializeStatusIfNullWhenUpdatingFromItems() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(spec, meta);
        queue.setStatus(null); // Manually set status to null
        
        queue.updateStatusFromItems();

        assertNotNull(queue.getStatus());
        assertEquals(Phase.PENDING, queue.getStatus().getPhase());
        assertEquals(3, queue.getStatus().getTotalScenarios());
    }

    @Test
    void shouldNotUpdateStatusFromItemsWhenSpecIsNull() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(null, meta);
        var initialStatus = queue.getStatus();
        
        queue.updateStatusFromItems();

        assertEquals(initialStatus, queue.getStatus()); // Should remain unchanged
    }

    @Test
    void shouldNotUpdateStatusFromItemsWhenItemsIsNull() {
        var emptySpec = new ExecutionQueueSpec("results.json", null, "test-benchmark");
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(emptySpec, meta);
        var initialStatus = queue.getStatus();
        
        queue.updateStatusFromItems();

        assertEquals(initialStatus, queue.getStatus()); // Should remain unchanged
    }

    @Test
    void shouldUpdateReconcileTimeWhenUpdatingStatusFromItems() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(spec, meta);
        var initialReconcileTime = queue.getStatus().getLastReconcileTime();
        
        // Sleep a bit to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        queue.updateStatusFromItems();

        assertNotEquals(initialReconcileTime, queue.getStatus().getLastReconcileTime());
    }

    @Test
    void shouldGetItemByName() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(spec, meta);
        
        var item = queue.getItem("scenario-2");
        
        assertNotNull(item);
        assertEquals("scenario-2", item.getScenario());
    }

    @Test
    void shouldReturnNullForNonExistentItem() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(spec, meta);
        
        var item = queue.getItem("non-existent-scenario");
        
        assertNull(item);
    }

    @Test
    void shouldGetNextPendingItem() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(spec, meta);
        
        var nextPendingItem = queue.getNextPendingItem();
        
        assertTrue(nextPendingItem.isPresent());
        assertEquals("scenario-1", nextPendingItem.get().getScenario());
        assertTrue(nextPendingItem.get().isPending());
    }

    @Test
    void shouldReturnEmptyWhenNoPendingItems() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(spec, meta);
        
        // Mark all items as completed
        queue.getSpec().getItems().forEach(ExecutionQueueItem::markAsCompleted);
        
        var nextPendingItem = queue.getNextPendingItem();
        
        assertFalse(nextPendingItem.isPresent());
    }

    @Test
    void shouldReturnTrueForIsDoneWhenAllItemsFinished() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(spec, meta);
        
        // Mark all items as completed
        queue.getSpec().getItems().forEach(ExecutionQueueItem::markAsCompleted);
        
        assertTrue(queue.isDone());
    }

    @Test
    void shouldReturnFalseForIsDoneWhenSomeItemsNotFinished() {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();

        queue = new ExecutionQueue(spec, meta);
        
        // Mark only first item as completed
        queue.getSpec().getItems().get(0).markAsCompleted();
        
        assertFalse(queue.isDone());
    }
}

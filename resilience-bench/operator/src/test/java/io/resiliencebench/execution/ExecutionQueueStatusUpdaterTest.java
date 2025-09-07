package io.resiliencebench.execution;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.resiliencebench.resources.Phase;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.queue.ExecutionQueueItem;
import io.resiliencebench.resources.queue.ExecutionQueueSpec;
import io.resiliencebench.resources.queue.ExecutionQueueStatus;
import io.resiliencebench.support.CustomResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionQueueStatusUpdaterTest {

    @Mock
    private CustomResourceRepository<ExecutionQueue> queueRepository;

    @Captor
    private ArgumentCaptor<ExecutionQueue> queueCaptor;

    private ExecutionQueueStatusUpdater statusUpdater;
    private ExecutionQueue testQueue;
    private ExecutionQueueItem pendingItem;
    private ExecutionQueueItem runningItem;
    private ExecutionQueueItem completedItem;

    @BeforeEach
    void setUp() {
        statusUpdater = new ExecutionQueueStatusUpdater(queueRepository);
        
        // Create test items
        pendingItem = new ExecutionQueueItem("scenario-1", "results/scenario-1.json");
        runningItem = new ExecutionQueueItem("scenario-2", "results/scenario-2.json");
        runningItem.markAsRunning();
        completedItem = new ExecutionQueueItem("scenario-3", "results/scenario-3.json");
        completedItem.markAsCompleted();

        // Create test queue
        var spec = new ExecutionQueueSpec("results/results.json", 
                                         List.of(pendingItem, runningItem, completedItem), 
                                         "test-benchmark");
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .withGeneration(1L)
                .build();
        
        testQueue = new ExecutionQueue(spec, meta);
        testQueue.setStatus(new ExecutionQueueStatus(3, "exec-123"));
    }

    @Test
    void shouldUpdateQueueProgressSuccessfully() {
        var namespace = "test-namespace";
        var queueName = "test-queue";
        
        when(queueRepository.find(namespace, queueName)).thenReturn(Optional.of(testQueue));
        
        statusUpdater.updateQueueProgress(namespace, queueName);
        
        verify(queueRepository).updateStatus(queueCaptor.capture());
        var updatedQueue = queueCaptor.getValue();
        
        assertEquals(1, updatedQueue.getStatus().getRunningScenarios());
        assertEquals(1, updatedQueue.getStatus().getCompletedScenarios());
        assertEquals(1, updatedQueue.getStatus().getPendingScenarios());
        assertEquals(Phase.RUNNING, updatedQueue.getStatus().getPhase());
        assertEquals(1L, updatedQueue.getStatus().getObservedGeneration());
    }

    @Test
    void shouldHandleQueueNotFound() {
        var namespace = "test-namespace";
        var queueName = "non-existent-queue";
        
        when(queueRepository.find(namespace, queueName)).thenReturn(Optional.empty());
        
        assertDoesNotThrow(() -> statusUpdater.updateQueueProgress(namespace, queueName));
        
        verify(queueRepository, never()).updateStatus(any());
    }

    @Test
    void shouldHandleExceptionDuringUpdate() {
        var namespace = "test-namespace";
        var queueName = "test-queue";
        
        when(queueRepository.find(namespace, queueName)).thenThrow(new RuntimeException("Database error"));
        
        assertDoesNotThrow(() -> statusUpdater.updateQueueProgress(namespace, queueName));
        
        verify(queueRepository, never()).updateStatus(any());
    }

    @Test
    void shouldMarkScenarioAsStarted() {
        var namespace = "test-namespace";
        var queueName = "test-queue";
        var scenarioName = "scenario-1";
        
        when(queueRepository.find(namespace, queueName)).thenReturn(Optional.of(testQueue));
        
        statusUpdater.markScenarioAsStarted(namespace, queueName, scenarioName);
        
        verify(queueRepository).update(queueCaptor.capture());
        var updatedQueue = queueCaptor.getValue();
        var item = updatedQueue.getItem(scenarioName);
        
        assertTrue(item.isRunning());
        assertFalse(item.isPending());
    }

    @Test
    void shouldNotMarkAlreadyRunningScenarioAsStarted() {
        var namespace = "test-namespace";
        var queueName = "test-queue";
        var scenarioName = "scenario-2"; // Already running
        
        when(queueRepository.find(namespace, queueName)).thenReturn(Optional.of(testQueue));
        
        statusUpdater.markScenarioAsStarted(namespace, queueName, scenarioName);
        
        verify(queueRepository, never()).update(any());
    }

    @Test
    void shouldHandleScenarioNotFoundForStarted() {
        var namespace = "test-namespace";
        var queueName = "test-queue";
        var scenarioName = "non-existent-scenario";
        
        when(queueRepository.find(namespace, queueName)).thenReturn(Optional.of(testQueue));
        
        assertDoesNotThrow(() -> statusUpdater.markScenarioAsStarted(namespace, queueName, scenarioName));
        
        verify(queueRepository, never()).update(any());
    }

    @Test
    void shouldMarkScenarioAsCompleted() {
        var namespace = "test-namespace";
        var queueName = "test-queue";
        var scenarioName = "scenario-2"; // Currently running
        
        when(queueRepository.find(namespace, queueName)).thenReturn(Optional.of(testQueue));
        
        statusUpdater.markScenarioAsCompleted(namespace, queueName, scenarioName);
        
        verify(queueRepository).update(queueCaptor.capture());
        var updatedQueue = queueCaptor.getValue();
        var item = updatedQueue.getItem(scenarioName);
        
        assertTrue(item.isFinished());
        assertFalse(item.isRunning());
    }

    @Test
    void shouldNotMarkNonRunningScenarioAsCompleted() {
        var namespace = "test-namespace";
        var queueName = "test-queue";
        var scenarioName = "scenario-1"; // Still pending
        
        when(queueRepository.find(namespace, queueName)).thenReturn(Optional.of(testQueue));
        
        statusUpdater.markScenarioAsCompleted(namespace, queueName, scenarioName);
        
        verify(queueRepository, never()).update(any());
    }

    @Test
    void shouldHandleQueueNotFoundForScenarioOperations() {
        var namespace = "test-namespace";
        var queueName = "non-existent-queue";
        var scenarioName = "scenario-1";
        
        when(queueRepository.find(namespace, queueName)).thenReturn(Optional.empty());
        
        assertDoesNotThrow(() -> statusUpdater.markScenarioAsStarted(namespace, queueName, scenarioName));
        assertDoesNotThrow(() -> statusUpdater.markScenarioAsCompleted(namespace, queueName, scenarioName));
        
        verify(queueRepository, never()).update(any());
    }

    @Test
    void shouldHandleExceptionDuringScenarioOperations() {
        var namespace = "test-namespace";
        var queueName = "test-queue";
        var scenarioName = "scenario-1";
        
        when(queueRepository.find(namespace, queueName)).thenThrow(new RuntimeException("Database error"));
        
        assertDoesNotThrow(() -> statusUpdater.markScenarioAsStarted(namespace, queueName, scenarioName));
        assertDoesNotThrow(() -> statusUpdater.markScenarioAsCompleted(namespace, queueName, scenarioName));
        
        verify(queueRepository, never()).update(any());
    }

    @Test
    void shouldUpdateQueueStatusFromItemsWhenAllCompleted() {
        // Create a queue with all completed items
        var allCompletedItems = List.of(
            createCompletedItem("scenario-1"),
            createCompletedItem("scenario-2"),
            createCompletedItem("scenario-3")
        );
        
        var spec = new ExecutionQueueSpec("results.json", allCompletedItems, "test-benchmark");
        var meta = new ObjectMetaBuilder()
                .withName("completed-queue")
                .withNamespace("test-namespace")
                .withGeneration(1L)
                .build();
        
        var completedQueue = new ExecutionQueue(spec, meta);
        completedQueue.setStatus(new ExecutionQueueStatus(3, "exec-456"));
        
        when(queueRepository.find("test-namespace", "completed-queue")).thenReturn(Optional.of(completedQueue));
        
        statusUpdater.updateQueueProgress("test-namespace", "completed-queue");
        
        verify(queueRepository).updateStatus(queueCaptor.capture());
        var updatedQueue = queueCaptor.getValue();
        
        assertEquals(0, updatedQueue.getStatus().getRunningScenarios());
        assertEquals(3, updatedQueue.getStatus().getCompletedScenarios());
        assertEquals(0, updatedQueue.getStatus().getPendingScenarios());
        assertEquals(Phase.COMPLETED, updatedQueue.getStatus().getPhase());
    }

    private ExecutionQueueItem createCompletedItem(String scenarioName) {
        var item = new ExecutionQueueItem(scenarioName, "results/" + scenarioName + ".json");
        item.markAsCompleted();
        return item;
    }
}

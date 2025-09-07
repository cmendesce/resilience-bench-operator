package io.resiliencebench.resources.queue;

import io.resiliencebench.resources.Phase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionQueueStatusTest {

    private ExecutionQueueStatus status;

    @BeforeEach
    void setUp() {
        status = new ExecutionQueueStatus();
    }

    @Test
    void shouldCreateStatusWithDefaultConstructor() {
        assertNotNull(status);
        assertNull(status.getPhase());
        assertEquals(0, status.getTotalScenarios());
        assertEquals(0, status.getRunningScenarios());
        assertEquals(0, status.getCompletedScenarios());
        assertEquals(0, status.getPendingScenarios());
    }

    @Test
    void shouldCreateStatusWithTotalScenariosAndExecutionId() {
        var executionId = "exec-123456";
        var totalScenarios = 5;
        
        status = new ExecutionQueueStatus(totalScenarios, executionId);
        
        assertEquals(Phase.PENDING, status.getPhase());
        assertEquals(totalScenarios, status.getTotalScenarios());
        assertEquals(0, status.getRunningScenarios());
        assertEquals(0, status.getCompletedScenarios());
        assertEquals(totalScenarios, status.getPendingScenarios());
        assertEquals(executionId, status.getExecutionId());
        assertNotNull(status.getLastReconcileTime());
        assertNotNull(status.getStartTime());
    }

    @Test
    void shouldCreateStatusWithAllParameters() {
        var phase = Phase.RUNNING;
        var totalScenarios = 10;
        var runningScenarios = 2;
        var completedScenarios = 3;
        var pendingScenarios = 5;
        var executionId = "exec-789";
        var observedGeneration = 1L;
        
        status = new ExecutionQueueStatus(phase, totalScenarios, runningScenarios, 
                                        completedScenarios, pendingScenarios, executionId, observedGeneration);
        
        assertEquals(phase, status.getPhase());
        assertEquals(totalScenarios, status.getTotalScenarios());
        assertEquals(runningScenarios, status.getRunningScenarios());
        assertEquals(completedScenarios, status.getCompletedScenarios());
        assertEquals(pendingScenarios, status.getPendingScenarios());
        assertEquals(executionId, status.getExecutionId());
        assertEquals(observedGeneration, status.getObservedGeneration());
        assertNotNull(status.getLastReconcileTime());
    }

    @Test
    void shouldUpdateProgress() {
        status = new ExecutionQueueStatus(5, "exec-123");
        
        status.updateProgress(2, 1, 2);
        
        assertEquals(2, status.getRunningScenarios());
        assertEquals(1, status.getCompletedScenarios());
        assertEquals(2, status.getPendingScenarios());
        assertEquals(Phase.RUNNING, status.getPhase());
    }

    @Test
    void shouldMarkAsCompletedWhenAllScenariosFinished() {
        var totalScenarios = 3;
        status = new ExecutionQueueStatus(totalScenarios, "exec-123");
        
        status.updateProgress(0, totalScenarios, 0);
        
        assertEquals(Phase.COMPLETED, status.getPhase());
        assertNotNull(status.getCompletionTime());
    }

    @Test
    void shouldMarkAsRunningWhenHasRunningScenariosOrCompleted() {
        status = new ExecutionQueueStatus(5, "exec-123");
        
        // Test with running scenarios
        status.updateProgress(1, 0, 4);
        assertEquals(Phase.RUNNING, status.getPhase());
        
        // Reset to pending
        status.setPhase(Phase.PENDING);
        
        // Test with completed scenarios
        status.updateProgress(0, 1, 4);
        assertEquals(Phase.RUNNING, status.getPhase());
    }

    @Test
    void shouldMarkAsCompleted() {
        status.markAsCompleted();
        
        assertEquals(Phase.COMPLETED, status.getPhase());
        assertNotNull(status.getCompletionTime());
    }

    @Test
    void shouldMarkAsFailed() {
        var errorMessage = "Test error";
        
        status.markAsFailed(errorMessage);
        
        assertEquals(Phase.FAILED, status.getPhase());
        assertEquals(errorMessage, status.getMessage());
        assertNotNull(status.getCompletionTime());
    }

    @Test
    void shouldReturnTrueForIsRunning() {
        status.setPhase(Phase.RUNNING);
        assertTrue(status.isRunning());
        
        status.setPhase(Phase.PENDING);
        assertFalse(status.isRunning());
    }

    @Test
    void shouldReturnTrueForIsCompleted() {
        status.setPhase(Phase.COMPLETED);
        assertTrue(status.isCompleted());
        
        status.setPhase(Phase.FAILED);
        assertTrue(status.isCompleted());
        
        status.setPhase(Phase.RUNNING);
        assertFalse(status.isCompleted());
    }

    @Test
    void shouldNeedReconciliationWhenObservedGenerationIsNull() {
        var currentGeneration = 1L;
        assertTrue(status.needsReconciliation(currentGeneration));
    }

    @Test
    void shouldNeedReconciliationWhenObservedGenerationDiffers() {
        var currentGeneration = 2L;
        status.setObservedGeneration(1L);
        
        assertTrue(status.needsReconciliation(currentGeneration));
    }

    @Test
    void shouldNotNeedReconciliationWhenObservedGenerationMatches() {
        var currentGeneration = 1L;
        status.setObservedGeneration(currentGeneration);
        
        assertFalse(status.needsReconciliation(currentGeneration));
    }

    @Test
    void shouldUpdateReconcileTime() {
        var initialTime = status.getLastReconcileTime();
        
        // Sleep a bit to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        status.updateReconcileTime();
        
        assertNotEquals(initialTime, status.getLastReconcileTime());
        assertNotNull(status.getLastReconcileTime());
    }

    @Test
    void shouldReturnConvenienceMethodsForRunningAndPending() {
        status.setRunningScenarios(3);
        status.setPendingScenarios(2);
        
        assertEquals(3, status.getRunning());
        assertEquals(2, status.getPending());
    }
}

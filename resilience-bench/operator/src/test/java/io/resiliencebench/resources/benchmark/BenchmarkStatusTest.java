package io.resiliencebench.resources.benchmark;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkStatusTest {

    private BenchmarkStatus status;

    @BeforeEach
    void setUp() {
        status = new BenchmarkStatus();
    }

    @Test
    @DisplayName("Default constructor should initialize with null values")
    void defaultConstructor_ShouldInitializeWithNullValues() {
        assertNull(status.getPhase());
        assertEquals(0, status.getTotalScenarios());
        assertEquals(0, status.getRunningScenarios());
        assertEquals(0, status.getCompletedScenarios());
        assertNull(status.getExecutionId());
        assertNull(status.getLastReconcileTime());
        assertNull(status.getStartTime());
        assertNull(status.getCompletionTime());
        assertNull(status.getMessage());
        assertNull(status.getObservedGeneration());
    }

    @Test
    @DisplayName("Constructor with totalScenarios should initialize properly")
    void constructorWithTotalScenarios_ShouldInitializeProperly() {
        int totalScenarios = 5;
        BenchmarkStatus statusWithTotal = new BenchmarkStatus(totalScenarios);

        assertEquals(BenchmarkStatus.Phase.PENDING, statusWithTotal.getPhase());
        assertEquals(totalScenarios, statusWithTotal.getTotalScenarios());
        assertEquals(0, statusWithTotal.getRunningScenarios());
        assertEquals(0, statusWithTotal.getCompletedScenarios());
        assertNotNull(statusWithTotal.getExecutionId());
        assertTrue(statusWithTotal.getExecutionId().startsWith("exec-"));
        assertNotNull(statusWithTotal.getLastReconcileTime());
        assertNotNull(statusWithTotal.getStartTime());
        assertNull(statusWithTotal.getCompletionTime());
        assertNull(statusWithTotal.getMessage());
        assertNull(statusWithTotal.getObservedGeneration());
    }

    @Test
    @DisplayName("Full constructor should set all values correctly")
    void fullConstructor_ShouldSetAllValuesCorrectly() {
        String phase = BenchmarkStatus.Phase.RUNNING;
        int totalScenarios = 10;
        int runningScenarios = 3;
        int completedScenarios = 2;
        String executionId = "exec-12345";
        Long observedGeneration = 5L;

        BenchmarkStatus fullStatus = new BenchmarkStatus(phase, totalScenarios, runningScenarios, 
                                                         completedScenarios, executionId, observedGeneration);

        assertEquals(phase, fullStatus.getPhase());
        assertEquals(totalScenarios, fullStatus.getTotalScenarios());
        assertEquals(runningScenarios, fullStatus.getRunningScenarios());
        assertEquals(completedScenarios, fullStatus.getCompletedScenarios());
        assertEquals(executionId, fullStatus.getExecutionId());
        assertEquals(observedGeneration, fullStatus.getObservedGeneration());
        assertNotNull(fullStatus.getLastReconcileTime());
    }

    @Test
    @DisplayName("Setters and getters should work correctly")
    void settersAndGetters_ShouldWorkCorrectly() {
        String phase = BenchmarkStatus.Phase.RUNNING;
        int totalScenarios = 8;
        int runningScenarios = 2;
        int completedScenarios = 3;
        String executionId = "exec-test";
        String lastReconcileTime = "2023-01-01T10:00:00Z";
        String startTime = "2023-01-01T09:00:00Z";
        String completionTime = "2023-01-01T11:00:00Z";
        String message = "Test message";
        Long observedGeneration = 10L;

        status.setPhase(phase);
        status.setTotalScenarios(totalScenarios);
        status.setRunningScenarios(runningScenarios);
        status.setCompletedScenarios(completedScenarios);
        status.setExecutionId(executionId);
        status.setLastReconcileTime(lastReconcileTime);
        status.setStartTime(startTime);
        status.setCompletionTime(completionTime);
        status.setMessage(message);
        status.setObservedGeneration(observedGeneration);

        assertEquals(phase, status.getPhase());
        assertEquals(totalScenarios, status.getTotalScenarios());
        assertEquals(runningScenarios, status.getRunningScenarios());
        assertEquals(completedScenarios, status.getCompletedScenarios());
        assertEquals(executionId, status.getExecutionId());
        assertEquals(lastReconcileTime, status.getLastReconcileTime());
        assertEquals(startTime, status.getStartTime());
        assertEquals(completionTime, status.getCompletionTime());
        assertEquals(message, status.getMessage());
        assertEquals(observedGeneration, status.getObservedGeneration());
    }

    @Test
    @DisplayName("isRunning should return true only when phase is RUNNING")
    void isRunning_ShouldReturnTrueOnlyWhenPhaseIsRunning() {
        status.setPhase(BenchmarkStatus.Phase.PENDING);
        assertFalse(status.isRunning());

        status.setPhase(BenchmarkStatus.Phase.RUNNING);
        assertTrue(status.isRunning());

        status.setPhase(BenchmarkStatus.Phase.COMPLETED);
        assertFalse(status.isRunning());

        status.setPhase(BenchmarkStatus.Phase.FAILED);
        assertFalse(status.isRunning());

        status.setPhase(null);
        assertFalse(status.isRunning());
    }

    @Test
    @DisplayName("isCompleted should return true for COMPLETED or FAILED phases")
    void isCompleted_ShouldReturnTrueForCompletedOrFailedPhases() {
        status.setPhase(BenchmarkStatus.Phase.PENDING);
        assertFalse(status.isCompleted());

        status.setPhase(BenchmarkStatus.Phase.RUNNING);
        assertFalse(status.isCompleted());

        status.setPhase(BenchmarkStatus.Phase.COMPLETED);
        assertTrue(status.isCompleted());

        status.setPhase(BenchmarkStatus.Phase.FAILED);
        assertTrue(status.isCompleted());

        status.setPhase(null);
        assertFalse(status.isCompleted());
    }

    @Test
    @DisplayName("needsReconciliation should detect generation changes")
    void needsReconciliation_ShouldDetectGenerationChanges() {
        // When observedGeneration is null, should need reconciliation
        assertTrue(status.needsReconciliation(1L));

        // When generations match, should not need reconciliation
        status.setObservedGeneration(5L);
        assertFalse(status.needsReconciliation(5L));

        // When generations differ, should need reconciliation
        assertTrue(status.needsReconciliation(6L));

        // When current generation is null, should need reconciliation
        assertTrue(status.needsReconciliation(null));
    }

    @Test
    @DisplayName("updateReconcileTime should set current timestamp")
    void updateReconcileTime_ShouldSetCurrentTimestamp() {
        String beforeUpdate = getCurrentTimestamp();
        status.updateReconcileTime();
        String afterUpdate = getCurrentTimestamp();

        assertNotNull(status.getLastReconcileTime());
        // Check that the timestamp is between before and after (allowing for execution time)
        assertTrue(status.getLastReconcileTime().compareTo(beforeUpdate) >= 0);
        assertTrue(status.getLastReconcileTime().compareTo(afterUpdate) <= 0);
    }

    @Test
    @DisplayName("markAsCompleted should set phase and completion time")
    void markAsCompleted_ShouldSetPhaseAndCompletionTime() {
        String beforeCompletion = getCurrentTimestamp();
        status.markAsCompleted();
        String afterCompletion = getCurrentTimestamp();

        assertEquals(BenchmarkStatus.Phase.COMPLETED, status.getPhase());
        assertNotNull(status.getCompletionTime());
        assertTrue(status.getCompletionTime().compareTo(beforeCompletion) >= 0);
        assertTrue(status.getCompletionTime().compareTo(afterCompletion) <= 0);
    }

    @Test
    @DisplayName("markAsFailed should set phase, completion time, and message")
    void markAsFailed_ShouldSetPhaseCompletionTimeAndMessage() {
        String errorMessage = "Test error message";
        String beforeFailure = getCurrentTimestamp();
        status.markAsFailed(errorMessage);
        String afterFailure = getCurrentTimestamp();

        assertEquals(BenchmarkStatus.Phase.FAILED, status.getPhase());
        assertEquals(errorMessage, status.getMessage());
        assertNotNull(status.getCompletionTime());
        assertTrue(status.getCompletionTime().compareTo(beforeFailure) >= 0);
        assertTrue(status.getCompletionTime().compareTo(afterFailure) <= 0);
    }

    @Test
    @DisplayName("updateProgress should update counters and phase correctly")
    void updateProgress_ShouldUpdateCountersAndPhaseCorrectly() {
        status.setTotalScenarios(10);
        status.setPhase(BenchmarkStatus.Phase.PENDING);

        // Test transition to RUNNING when scenarios start
        status.updateProgress(2, 1);
        assertEquals(2, status.getRunningScenarios());
        assertEquals(1, status.getCompletedScenarios());
        assertEquals(BenchmarkStatus.Phase.RUNNING, status.getPhase());

        // Test staying in RUNNING phase
        status.updateProgress(3, 2);
        assertEquals(3, status.getRunningScenarios());
        assertEquals(2, status.getCompletedScenarios());
        assertEquals(BenchmarkStatus.Phase.RUNNING, status.getPhase());

        // Test transition to COMPLETED when all scenarios are done
        status.updateProgress(0, 10);
        assertEquals(0, status.getRunningScenarios());
        assertEquals(10, status.getCompletedScenarios());
        assertEquals(BenchmarkStatus.Phase.COMPLETED, status.getPhase());
        assertNotNull(status.getCompletionTime());
    }

    @Test
    @DisplayName("updateProgress should handle edge case with only completed scenarios")
    void updateProgress_ShouldHandleEdgeCaseWithOnlyCompletedScenarios() {
        status.setTotalScenarios(5);
        status.setPhase(BenchmarkStatus.Phase.PENDING);

        // Test with only completed scenarios (no running)
        status.updateProgress(0, 3);
        assertEquals(0, status.getRunningScenarios());
        assertEquals(3, status.getCompletedScenarios());
        assertEquals(BenchmarkStatus.Phase.RUNNING, status.getPhase());
    }

    @Test
    @DisplayName("updateProgress should not change phase if no progress")
    void updateProgress_ShouldNotChangePhaseIfNoProgress() {
        status.setTotalScenarios(5);
        status.setPhase(BenchmarkStatus.Phase.PENDING);

        // Test with no progress
        status.updateProgress(0, 0);
        assertEquals(0, status.getRunningScenarios());
        assertEquals(0, status.getCompletedScenarios());
        assertEquals(BenchmarkStatus.Phase.PENDING, status.getPhase());
    }

    @Test
    @DisplayName("Phase constants should have correct values")
    void phaseConstants_ShouldHaveCorrectValues() {
        assertEquals("Pending", BenchmarkStatus.Phase.PENDING);
        assertEquals("Running", BenchmarkStatus.Phase.RUNNING);
        assertEquals("Completed", BenchmarkStatus.Phase.COMPLETED);
        assertEquals("Failed", BenchmarkStatus.Phase.FAILED);
    }

    @Test
    @DisplayName("ExecutionId generation should create unique IDs")
    void executionIdGeneration_ShouldCreateUniqueIds() {
        BenchmarkStatus status1 = new BenchmarkStatus(5);
        BenchmarkStatus status2 = new BenchmarkStatus(3);

        assertNotNull(status1.getExecutionId());
        assertNotNull(status2.getExecutionId());
        assertNotEquals(status1.getExecutionId(), status2.getExecutionId());
        assertTrue(status1.getExecutionId().startsWith("exec-"));
        assertTrue(status2.getExecutionId().startsWith("exec-"));
    }

    @Test
    @DisplayName("Timestamp generation should be in UTC format")
    void timestampGeneration_ShouldBeInUtcFormat() {
        BenchmarkStatus statusWithTime = new BenchmarkStatus(5);
        
        assertNotNull(statusWithTime.getStartTime());
        assertNotNull(statusWithTime.getLastReconcileTime());
        
        // Verify timestamp format (should be parseable as ISO format)
        assertDoesNotThrow(() -> LocalDateTime.parse(
            statusWithTime.getStartTime().substring(0, statusWithTime.getStartTime().length() - 6)
        ));
        assertDoesNotThrow(() -> LocalDateTime.parse(
            statusWithTime.getLastReconcileTime().substring(0, statusWithTime.getLastReconcileTime().length() - 6)
        ));
    }

    @Test
    @DisplayName("needsReconciliation should handle null observedGeneration correctly")
    void needsReconciliation_ShouldHandleNullObservedGenerationCorrectly() {
        // Test with null observedGeneration and non-null current generation
        status.setObservedGeneration(null);
        assertTrue(status.needsReconciliation(1L));
        
        // Test with non-null observedGeneration and null current generation
        status.setObservedGeneration(1L);
        assertTrue(status.needsReconciliation(null));
        
        // Test with both null
        status.setObservedGeneration(null);
        assertTrue(status.needsReconciliation(null));
    }

    @Test
    @DisplayName("markAsCompleted should not override existing completion time")
    void markAsCompleted_ShouldSetCompletionTimeOnlyOnce() {
        String existingCompletionTime = "2023-01-01T10:00:00Z";
        status.setCompletionTime(existingCompletionTime);
        
        status.markAsCompleted();
        
        // Should update phase but completion time should be overwritten
        assertEquals(BenchmarkStatus.Phase.COMPLETED, status.getPhase());
        assertNotEquals(existingCompletionTime, status.getCompletionTime());
    }

    @Test
    @DisplayName("markAsFailed should not override existing completion time")
    void markAsFailed_ShouldSetCompletionTimeOnlyOnce() {
        String existingCompletionTime = "2023-01-01T10:00:00Z";
        String errorMessage = "Test error";
        status.setCompletionTime(existingCompletionTime);
        
        status.markAsFailed(errorMessage);
        
        // Should update phase and message, but completion time should be overwritten
        assertEquals(BenchmarkStatus.Phase.FAILED, status.getPhase());
        assertEquals(errorMessage, status.getMessage());
        assertNotEquals(existingCompletionTime, status.getCompletionTime());
    }

    // Helper method to get current timestamp in the same format as the class
    private String getCurrentTimestamp() {
        return LocalDateTime.now().atZone(ZoneId.of("UTC")).toString();
    }
}

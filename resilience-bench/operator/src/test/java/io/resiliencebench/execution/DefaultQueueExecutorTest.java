package io.resiliencebench.execution;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.queue.ExecutionQueueItem;
import io.resiliencebench.resources.queue.ExecutionQueueSpec;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.support.CustomResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultQueueExecutorTest {

    @Mock
    private CustomResourceRepository<Scenario> scenarioRepository;

    @Mock
    private ScenarioExecutor scenarioExecutor;

    private DefaultQueueExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new DefaultQueueExecutor(scenarioRepository, scenarioExecutor);
    }

    @Test
    void shouldExecuteNextPendingItem() {
        // Given
        var scenario = new Scenario();
        scenario.setMetadata(new ObjectMetaBuilder().withName("test-scenario").build());
        
        var item = new ExecutionQueueItem("test-scenario", "result.json");
        var queue = createQueueWithItems(item);
        
        when(scenarioRepository.find(eq("test-namespace"), eq("test-scenario")))
                .thenReturn(Optional.of(scenario));

        // When
        executor.execute(queue);

        // Then
        verify(scenarioExecutor).execute(eq(scenario), eq(queue));
    }

    private ExecutionQueue createQueueWithItems(ExecutionQueueItem... items) {
        var meta = new ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();
        var spec = new ExecutionQueueSpec("results.json", List.of(items), "test-benchmark");
        return new ExecutionQueue(spec, meta);
    }
} 
package io.resiliencebench.execution;

import io.fabric8.kubernetes.api.model.batch.v1.JobStatusBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.BatchAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.V1BatchAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.resiliencebench.execution.steps.StepRegistry;
import io.resiliencebench.execution.steps.k6.K6JobFactory;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.queue.ExecutionQueueSpec;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.resources.scenario.ScenarioSpec;
import io.resiliencebench.resources.scenario.ScenarioWorkload;
import io.resiliencebench.resources.workload.Workload;
import io.resiliencebench.resources.workload.WorkloadSpec;
import io.resiliencebench.support.CustomResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static io.resiliencebench.support.Annotations.SCENARIO;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultScenarioExecutorTest {

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private BatchAPIGroupDSL batchApiGroup;

    @Mock
    private V1BatchAPIGroupDSL v1ApiGroup;

    @Mock
    private MixedOperation<Job, JobList, ScalableResource<Job>> jobsOperation;

    @Mock
    private ScalableResource<Job> jobResource;

    @Mock
    private StepRegistry stepRegistry;

    @Mock
    private K6JobFactory k6JobFactory;

    @Mock
    private CustomResourceRepository<Scenario> scenarioRepository;

    @Mock
    private CustomResourceRepository<ExecutionQueue> executionRepository;

    @Mock
    private CustomResourceRepository<Workload> workloadRepository;

    @Captor
    private ArgumentCaptor<Watcher<Job>> jobWatcherCaptor;

    private DefaultScenarioExecutor executor;

    @BeforeEach
    void setUp() {
        when(kubernetesClient.batch()).thenReturn(batchApiGroup);
        when(batchApiGroup.v1()).thenReturn(v1ApiGroup);
        when(v1ApiGroup.jobs()).thenReturn(jobsOperation);
        when(jobsOperation.inNamespace(anyString())).thenReturn(jobsOperation);
        when(jobsOperation.withName(anyString())).thenReturn(jobResource);
        when(jobsOperation.resource(any(Job.class))).thenReturn(jobResource);

        executor = new DefaultScenarioExecutor(
                kubernetesClient,
                stepRegistry,
                k6JobFactory,
                scenarioRepository,
                executionRepository,
                workloadRepository
        );
    }

    @Test
    void shouldExecuteScenario() {
        // Given
        var scenario = createScenario();
        var queue = createQueue();
        var workload = createWorkload();
        var job = createJob();

        when(workloadRepository.find(eq("test-namespace"), eq("test-workload")))
                .thenReturn(of(workload));
        when(k6JobFactory.create(eq(scenario), eq(workload), any()))
                .thenReturn(job);

        // When
        executor.execute(scenario, queue);

        // Then
        verify(jobsOperation, times(2)).resource(job);
        verify(jobResource).create();
        verify(jobResource).watch(any(Watcher.class));
    }

    @Test
    void shouldExecutePreparationAndPostExecutionSteps() {
        // Given
        var scenario = createScenario();
        var queue = createQueue();
        var workload = createWorkload();
        var job = createJob();

        when(workloadRepository.find(eq("test-namespace"), eq("test-workload")))
                .thenReturn(of(workload));
        when(scenarioRepository.get(eq("test-namespace"), eq(scenario.getMetadata().getName())))
                .thenReturn(scenario);
        when(k6JobFactory.create(eq(scenario), eq(workload), any()))
                .thenReturn(job);

        // When
        executor.execute(scenario, queue);

        Job finishedJob = createJob();
        finishedJob.setStatus(new JobStatusBuilder().withCompletionTime("2024-01-01T00:00:00Z").build());
        verify(jobResource).watch(jobWatcherCaptor.capture());
        Watcher<Job> watcher = jobWatcherCaptor.getValue();
        watcher.eventReceived(Watcher.Action.MODIFIED, finishedJob);

        // Then
        verify(stepRegistry).getPreparationSteps();
        verify(stepRegistry).getPostExecutionSteps();
    }

    private Scenario createScenario() {
        var scenario = new Scenario();
        scenario.setMetadata(new io.fabric8.kubernetes.api.model.ObjectMetaBuilder()
                .withName("test-scenario")
                .withNamespace("test-namespace")
                .build());
        scenario.setSpec(new ScenarioSpec(
                "test-scenario",
                new ScenarioWorkload("test-workload", 1),
                null,
                null
        ));
        return scenario;
    }

    private ExecutionQueue createQueue() {
        var meta = new io.fabric8.kubernetes.api.model.ObjectMetaBuilder()
                .withName("test-queue")
                .withNamespace("test-namespace")
                .build();
        var spec = new ExecutionQueueSpec("results.json", List.of(), "test-benchmark");
        return new ExecutionQueue(spec, meta);
    }

    private Workload createWorkload() {
        var workload = new Workload();
        workload.setMetadata(new io.fabric8.kubernetes.api.model.ObjectMetaBuilder()
                .withName("test-workload")
                .withNamespace("test-namespace")
                .build());
        var spec = new WorkloadSpec();
        workload.setSpec(spec);
        return workload;
    }

    private Job createJob() {
        return new JobBuilder()
                .withNewMetadata()
                .withName("test-job")
                .withNamespace("test-namespace")
                .addToAnnotations(SCENARIO, "test-scenario")
                .endMetadata()
                .build();
    }
} 
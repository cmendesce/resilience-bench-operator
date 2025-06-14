package io.resiliencebench;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.resiliencebench.resources.ExecutionQueueFactory;
import io.resiliencebench.resources.ScenarioFactory;
import io.resiliencebench.resources.benchmark.Benchmark;
import io.resiliencebench.resources.benchmark.BenchmarkStatus;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.resources.workload.Workload;
import io.resiliencebench.support.CustomResourceRepository;

@ControllerConfiguration
public class BenchmarkController implements Reconciler<Benchmark> {

    private static final Logger logger = LoggerFactory.getLogger(BenchmarkController.class);

    private final CustomResourceRepository<Scenario> scenarioRepository;
    private final CustomResourceRepository<Workload> workloadRepository;
    private final CustomResourceRepository<ExecutionQueue> queueRepository;

    public BenchmarkController(
            CustomResourceRepository<Scenario> scenarioRepository,
            CustomResourceRepository<Workload> workloadRepository,
            CustomResourceRepository<ExecutionQueue> queueRepository) {
        this.scenarioRepository = scenarioRepository;
        this.workloadRepository = workloadRepository;
        this.queueRepository = queueRepository;
    }

    @Override
    public UpdateControl<Benchmark> reconcile(Benchmark benchmark, Context<Benchmark> context) {
        logger.info("Reconciling Benchmark: {}", benchmark.getMetadata().getName());

        var workload = workloadRepository.find(benchmark.getMetadata().getNamespace(), benchmark.getSpec().getWorkload());
        if (workload.isEmpty()) {
            logger.error("Workload not found: {}", benchmark.getSpec().getWorkload());
            return UpdateControl.noUpdate();
        }

        var scenariosList = createScenarios(benchmark, workload.get());
        if (scenariosList.isEmpty()) {
            logger.error("No scenarios generated for benchmark {}", benchmark.getMetadata().getName());
            return UpdateControl.noUpdate();
        }

        createExecutionQueue(benchmark, scenariosList);

        logger.info("Benchmark reconciled {}. {} scenarios created",
                benchmark.getMetadata().getName(),
                scenariosList.size()
        );
        benchmark.setStatus(new BenchmarkStatus(scenariosList.size()));
        return UpdateControl.updateStatus(benchmark);
    }

    private List<Scenario> createScenarios(Benchmark benchmark, Workload workload) {
        scenarioRepository.deleteAll(benchmark.getMetadata().getNamespace());
        var scenariosList = ScenarioFactory.create(benchmark, workload);
        scenariosList.forEach(scenarioRepository::create);
        return scenariosList;
    }

    private ExecutionQueue createExecutionQueue(Benchmark benchmark, List<Scenario> scenariosList) {
        queueRepository.deleteAll(benchmark.getMetadata().getNamespace());
        var queueCreated = ExecutionQueueFactory.create(benchmark, scenariosList);
        return queueRepository.create(queueCreated);
    }
}

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
    var benchmarkName = benchmark.getMetadata().getName();
    var namespace = benchmark.getMetadata().getNamespace();

    logger.info("Reconciling benchmark: {} in namespace: {}", benchmarkName, namespace);

    try {
      var currentStatus = benchmark.getStatus();
      var currentGeneration = benchmark.getMetadata().getGeneration();

      if (currentStatus != null &&
          currentStatus.isCompleted() &&
          !currentStatus.needsReconciliation(currentGeneration)) {
        logger.info("Benchmark {} is already completed and no spec changes detected, skipping reconciliation", benchmarkName);
        currentStatus.updateReconcileTime();
        return UpdateControl.updateStatus(benchmark);
      }

      var workload = workloadRepository.find(namespace, benchmark.getSpec().getWorkload());
      if (workload.isEmpty()) {
        logger.error("Workload not found: {}", benchmark.getSpec().getWorkload());
        return updateStatusWithError(benchmark, "Workload not found: " + benchmark.getSpec().getWorkload());
      }

      var scenariosList = createScenarios(benchmark, workload.get());
      if (scenariosList.isEmpty()) {
        logger.error("No scenarios generated for benchmark {}", benchmarkName);
        return updateStatusWithError(benchmark, "No scenarios generated");
      }

      createQueue(benchmark, scenariosList);

      var status = createOrUpdateStatus(benchmark, benchmarkName);
      benchmark.setStatus(status);

      logger.info("Benchmark reconciled {}. {} scenarios created", benchmarkName, scenariosList.size());

      return UpdateControl.updateStatus(benchmark);

    } catch (Exception e) {
      logger.error("Error during reconciliation of benchmark: " + benchmarkName, e);
      return updateStatusWithError(benchmark, "Reconciliation error: " + e.getMessage());
    }
  }

  private UpdateControl<Benchmark> updateStatusWithError(Benchmark benchmark, String errorMessage) {
    var status = benchmark.getStatus();
    if (status == null) {
      status = new BenchmarkStatus(benchmark.getMetadata().getName());
      benchmark.setStatus(status);
    }
    status.markAsFailed(errorMessage);
    status.setObservedGeneration(benchmark.getMetadata().getGeneration());
    return UpdateControl.updateStatus(benchmark);
  }

  private BenchmarkStatus createOrUpdateStatus(Benchmark benchmark, String queueName) {
    var currentStatus = benchmark.getStatus();
    var currentGeneration = benchmark.getMetadata().getGeneration();

    if (currentStatus == null || currentStatus.needsReconciliation(currentGeneration)) {
      var newStatus = new BenchmarkStatus(queueName);
      newStatus.setObservedGeneration(currentGeneration);
      logger.info("Created new status for benchmark {} with execution queue {}",
                 benchmark.getMetadata().getName(), queueName);
      return newStatus;
    } else {
      currentStatus.updateReconcileTime();
      currentStatus.setObservedGeneration(currentGeneration);
      currentStatus.setExecutionQueueName(queueName);
      logger.info("Updated existing status for benchmark {}", benchmark.getMetadata().getName());
      return currentStatus;
    }
  }

  private List<Scenario> createScenarios(Benchmark benchmark, Workload workload) {
    var namespace = benchmark.getMetadata().getNamespace();
    scenarioRepository.deleteAll(namespace);
    var scenariosList = ScenarioFactory.create(benchmark, workload);
    scenariosList.forEach(scenarioRepository::create);
    logger.info("Created {} scenarios for benchmark: {}", scenariosList.size(), benchmark.getMetadata().getName());
    return scenariosList;
  }

  private void createQueue(Benchmark benchmark, List<Scenario> scenariosList) {
    var namespace = benchmark.getMetadata().getNamespace();
    queueRepository.deleteAll(namespace);
    var queueCreated = ExecutionQueueFactory.create(benchmark, scenariosList);
    queueRepository.create(queueCreated);
  }
}

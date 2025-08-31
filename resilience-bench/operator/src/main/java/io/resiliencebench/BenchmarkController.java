package io.resiliencebench;

import java.util.List;

import io.resiliencebench.execution.QueueExecutor;
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

  private final QueueExecutor queueExecutor;

  public BenchmarkController(QueueExecutor queueExecutor,
                             CustomResourceRepository<Scenario> scenarioRepository,
                             CustomResourceRepository<Workload> workloadRepository,
                             CustomResourceRepository<ExecutionQueue> queueRepository) {
    this.queueExecutor = queueExecutor;
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
      // Check if this is a periodic reconcile on an already completed benchmark
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

      var executionQueue = prepareToRunScenarios(benchmark, scenariosList);

      // Create or update status with proper tracking
      var status = createOrUpdateStatus(benchmark, scenariosList.size());
      benchmark.setStatus(status);

      logger.info("Benchmark reconciled {}. {} scenarios created", benchmarkName, scenariosList.size());
      
      queueExecutor.execute(executionQueue);
      return UpdateControl.updateStatus(benchmark);
      
    } catch (Exception e) {
      logger.error("Error during reconciliation of benchmark: " + benchmarkName, e);
      return updateStatusWithError(benchmark, "Reconciliation error: " + e.getMessage());
    }
  }

  private UpdateControl<Benchmark> updateStatusWithError(Benchmark benchmark, String errorMessage) {
    var status = benchmark.getStatus();
    if (status == null) {
      status = new BenchmarkStatus(0);
      benchmark.setStatus(status);
    }
    status.markAsFailed(errorMessage);
    status.setObservedGeneration(benchmark.getMetadata().getGeneration());
    return UpdateControl.updateStatus(benchmark);
  }

  private BenchmarkStatus createOrUpdateStatus(Benchmark benchmark, int totalScenarios) {
    var currentStatus = benchmark.getStatus();
    var currentGeneration = benchmark.getMetadata().getGeneration();
    
    if (currentStatus == null || currentStatus.needsReconciliation(currentGeneration)) {
      // Create new status for new benchmark or when spec has changed
      var newStatus = new BenchmarkStatus(totalScenarios);
      newStatus.setObservedGeneration(currentGeneration);
      logger.info("Created new status for benchmark {} with {} total scenarios", 
                 benchmark.getMetadata().getName(), totalScenarios);
      return newStatus;
    } else {
      // Update existing status
      currentStatus.updateReconcileTime();
      currentStatus.setObservedGeneration(currentGeneration);
      logger.info("Updated existing status for benchmark {}", benchmark.getMetadata().getName());
      return currentStatus;
    }
  }

  private List<Scenario> createScenarios(Benchmark benchmark, Workload workload) {
    var namespace = benchmark.getMetadata().getNamespace();
    
    // Clean up existing scenarios for this benchmark to avoid conflicts
    scenarioRepository.deleteAll(namespace);

    var scenariosList = ScenarioFactory.create(benchmark, workload);
    scenariosList.forEach(scenarioRepository::create);
    
    logger.info("Created {} scenarios for benchmark: {}", scenariosList.size(), benchmark.getMetadata().getName());
    return scenariosList;
  }

  private ExecutionQueue prepareToRunScenarios(Benchmark benchmark, List<Scenario> scenariosList) {
    var namespace = benchmark.getMetadata().getNamespace();
    
    // Ensure scenarios are created
    scenarioRepository.deleteAll(namespace);
    scenariosList.forEach(scenarioRepository::create);

    // Clean up existing queues and create new one
    queueRepository.deleteAll(namespace);
    var queueCreated = ExecutionQueueFactory.create(benchmark, scenariosList);
    
    // Add execution ID to the queue for tracking
    if (benchmark.getStatus() != null && benchmark.getStatus().getExecutionId() != null) {
      if (queueCreated.getMetadata().getLabels() == null) {
        queueCreated.getMetadata().setLabels(new java.util.HashMap<>());
      }
      queueCreated.getMetadata().getLabels().put("execution-id", benchmark.getStatus().getExecutionId());
    }
    
    return queueRepository.create(queueCreated);
  }
}

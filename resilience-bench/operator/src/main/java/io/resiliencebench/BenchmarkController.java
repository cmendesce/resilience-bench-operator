package io.resiliencebench;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.resiliencebench.execution.ScenarioExecutor;
import io.resiliencebench.resources.ExecutionQueueFactory;
import io.resiliencebench.resources.ScenarioFactory;
import io.resiliencebench.resources.benchmark.Benchmark;
import io.resiliencebench.resources.benchmark.BenchmarkStatus;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.scenario.Scenario;
import io.resiliencebench.resources.workload.Workload;
import io.resiliencebench.support.CustomResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ControllerConfiguration
public class BenchmarkController implements Reconciler<Benchmark> {

  private static final Logger logger = LoggerFactory.getLogger(BenchmarkController.class);

  private final CustomResourceRepository<Scenario> scenarioRepository;

  private final CustomResourceRepository<Workload> workloadRepository;
  private final CustomResourceRepository<ExecutionQueue> queueRepository;

  private final ScenarioExecutor scenarioExecutor;

  public BenchmarkController(ScenarioExecutor scenarioExecutor,
                             CustomResourceRepository<Scenario> scenarioRepository,
                             CustomResourceRepository<Workload> workloadRepository,
                             CustomResourceRepository<ExecutionQueue> queueRepository) {
    this.scenarioExecutor = scenarioExecutor;
    this.scenarioRepository = scenarioRepository;
    this.workloadRepository = workloadRepository;
    this.queueRepository = queueRepository;
  }

  @Override
  public UpdateControl<Benchmark> reconcile(Benchmark benchmark, Context<Benchmark> context) {
    var workload = workloadRepository.find(benchmark.getMetadata().getNamespace(), benchmark.getSpec().getWorkload());
    if (workload.isEmpty()) {
      logger.error("Workload not found: {}", benchmark.getSpec().getWorkload());
      return UpdateControl.noUpdate();
    }

    var scenariosList = ScenarioFactory.create(benchmark, workload.get());
    if (scenariosList.isEmpty()) {
      logger.error("No scenarios generated for benchmark {}", benchmark.getMetadata().getName());
      return UpdateControl.noUpdate();
    }

    queueRepository.deleteAll(benchmark.getMetadata().getNamespace());
    scenarioRepository.deleteAll(benchmark.getMetadata().getNamespace());

    var executionQueue = getOrCreateQueue(benchmark, scenariosList);
    scenariosList.forEach(this::createOrUpdateScenario);

    scenarioExecutor.run(executionQueue);
    logger.info("Benchmark reconciled {}. {} scenarios created",
            benchmark.getMetadata().getName(),
            scenariosList.size()
    );
    benchmark.setStatus(new BenchmarkStatus(scenariosList.size()));
    return UpdateControl.updateStatus(benchmark);
  }

  private ExecutionQueue getOrCreateQueue(Benchmark benchmark, List<Scenario> scenariosList) {
    var queue = queueRepository.find(benchmark.getMetadata().getNamespace(), benchmark.getMetadata().getName());
    if (queue.isPresent()) {
      logger.debug("ExecutionQueue already exists: {}", benchmark.getMetadata().getName());
      return queue.get();
    } else {
      var queueCreated = ExecutionQueueFactory.create(benchmark, scenariosList);
      queueRepository.create(queueCreated);
      return queueCreated;
    }
  }

  private void createOrUpdateScenario(Scenario scenario) {
    var foundScenario = scenarioRepository.find(scenario.getMetadata());
    if (foundScenario.isEmpty()) {
      scenarioRepository.create(scenario);
    } else {
      logger.debug("Scenario already exists: {}", scenario.getMetadata().getName());
    }
  }
}

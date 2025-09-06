package io.resiliencebench.execution;

import io.resiliencebench.resources.benchmark.Benchmark;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.queue.ExecutionQueueItem;
import io.resiliencebench.support.CustomResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkStatusUpdater {

    private static final Logger logger = LoggerFactory.getLogger(BenchmarkStatusUpdater.class);

    private final CustomResourceRepository<Benchmark> benchmarkRepository;
    private final CustomResourceRepository<ExecutionQueue> queueRepository;

    public BenchmarkStatusUpdater(CustomResourceRepository<Benchmark> benchmarkRepository,
                                 CustomResourceRepository<ExecutionQueue> queueRepository) {
        this.benchmarkRepository = benchmarkRepository;
        this.queueRepository = queueRepository;
    }

    /**
     * Updates the benchmark status based on the current execution queue state
     */
    public void updateBenchmarkProgress(String namespace, String benchmarkName) {
        try {
            var benchmarkOpt = benchmarkRepository.find(namespace, benchmarkName);
            var queueOpt = queueRepository.find(namespace, benchmarkName);

            if (benchmarkOpt.isEmpty()) {
                logger.warn("Benchmark not found: {}/{}", namespace, benchmarkName);
                return;
            }

            if (queueOpt.isEmpty()) {
                logger.warn("Execution queue not found: {}/{}", namespace, benchmarkName);
                return;
            }

            var benchmark = benchmarkOpt.get();
            var queue = queueOpt.get();
            var status = benchmark.getStatus();

            if (status == null) {
                logger.warn("Benchmark status is null for: {}/{}", namespace, benchmarkName);
                return;
            }

            var progress = calculateProgress(queue);
            
            status.updateProgress(progress.running(), progress.completed());
            status.updateReconcileTime();
            benchmarkRepository.updateStatus(benchmark);
            
            logger.debug("Updated benchmark {} progress: {} running, {} completed, {} total", 
                        benchmarkName, progress.running(), progress.completed(), status.getTotalScenarios());

        } catch (Exception e) {
            logger.error("Error updating benchmark status for: " + namespace + "/" + benchmarkName, e);
        }
    }

    /**
     * Marks a specific scenario as started in the benchmark status
     */
    public void markScenarioAsStarted(String namespace, String benchmarkName, String scenarioName) {
        try {
            logger.info("Marking scenario {} as started for benchmark {}/{}", scenarioName, namespace, benchmarkName);
            updateBenchmarkProgress(namespace, benchmarkName);
        } catch (Exception e) {
            logger.error("Error marking scenario as started: " + scenarioName, e);
        }
    }

    /**
     * Marks a specific scenario as completed in the benchmark status
     */
    public void markScenarioAsCompleted(String namespace, String benchmarkName, String scenarioName) {
        try {
            logger.info("Marking scenario {} as completed for benchmark {}/{}", scenarioName, namespace, benchmarkName);
            updateBenchmarkProgress(namespace, benchmarkName);
        } catch (Exception e) {
            logger.error("Error marking scenario as completed: " + scenarioName, e);
        }
    }

    /**
     * Calculates the current progress from the execution queue
     */
    private ProgressInfo calculateProgress(ExecutionQueue queue) {
        var items = queue.getSpec().getItems();
        var running = (int) items.stream().filter(ExecutionQueueItem::isRunning).count();
        var completed = (int) items.stream().filter(ExecutionQueueItem::isFinished).count();
        return new ProgressInfo(running, completed);
    }

    private record ProgressInfo(int running, int completed) {}
}

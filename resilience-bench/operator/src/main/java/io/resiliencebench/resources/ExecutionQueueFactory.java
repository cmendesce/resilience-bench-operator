package io.resiliencebench.resources;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.resiliencebench.resources.benchmark.Benchmark;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.queue.ExecutionQueueSpec;
import io.resiliencebench.resources.queue.ExecutionQueueItem;
import io.resiliencebench.resources.scenario.Scenario;
import static io.resiliencebench.support.Annotations.OWNED_BY;

public class ExecutionQueueFactory {

  public ExecutionQueueFactory() {
    throw new IllegalStateException("Utility class");
  }

  public static ExecutionQueue create(Benchmark benchmark, List<Scenario> scenarios) {
    var meta = new ObjectMetaBuilder()
            .withNamespace(benchmark.getMetadata().getNamespace())
            .addToAnnotations(OWNED_BY, benchmark.getMetadata().getNamespace())
            .addToLabels("execution-id", benchmark.getStatus().getExecutionId())
            .withName(benchmark.getMetadata().getName())
            .build();

    var now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
    var itemResultsFile = Paths.get(now, "%s.json").toString();

    var items = scenarios.stream().map(s -> new ExecutionQueueItem(
            s.getMetadata().getName(), itemResultsFile.formatted(s.getMetadata().getName()))
    ).toList();
    var spec = new ExecutionQueueSpec(
            Paths.get(now,  "results.json").toString(),
            items,
            benchmark.getMetadata().getNamespace()
    );

    var queue = new ExecutionQueue(spec, meta);
    return queue;
  }
}

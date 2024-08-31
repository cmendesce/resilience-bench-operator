package io.resiliencebench.resources;

import java.util.List;
import java.util.UUID;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.resiliencebench.resources.benchmark.Benchmark;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.queue.ExecutionQueueSpec;
import io.resiliencebench.resources.queue.Item;
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
            .withName(benchmark.getMetadata().getName())
            .build();

    var items = scenarios.stream().map(s -> new Item(s.getMetadata().getName()));
    var spec = new ExecutionQueueSpec(
            "/results/%s.json".formatted(UUID.randomUUID().toString()),
            items.toList(),
            benchmark.getMetadata().getNamespace()
    );
    return new ExecutionQueue(spec, meta);
  }
}

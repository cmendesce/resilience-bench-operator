package io.resiliencebench.resources.queue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.*;

import java.util.Optional;

@Group("resiliencebench.io")
@Version("v1beta1")
@ShortNames("eq")
@Plural("queues")
@Kind("Queue")
public class ExecutionQueue extends CustomResource<ExecutionQueueSpec, Void> implements Namespaced {
  ExecutionQueue() { }

  public ExecutionQueue(ExecutionQueueSpec spec, ObjectMeta meta) {
    this.spec = spec;
    this.setMetadata(meta);
  }

  @JsonIgnore
  public ExecutionQueueItem getItem(String name) {
    return getSpec().getItems().stream().filter(item -> item.getScenario().equals(name)).findFirst().orElse(null);
  }

  @JsonIgnore
  public Optional<ExecutionQueueItem> getNextPendingItem() {
    return getSpec().getItems().stream().filter(ExecutionQueueItem::isPending).findFirst();
  }

  @JsonIgnore
  public boolean isDone() {
    return getSpec().getItems().stream().allMatch(ExecutionQueueItem::isFinished);
  }
}

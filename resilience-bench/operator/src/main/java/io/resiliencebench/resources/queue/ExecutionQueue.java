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
public class ExecutionQueue extends CustomResource<ExecutionQueueSpec, ExecutionQueueStatus> implements Namespaced {
  ExecutionQueue() { }

  public ExecutionQueue(ExecutionQueueSpec spec, ObjectMeta meta) {
    this.spec = spec;
    this.setMetadata(meta);
    initializeStatus();
  }

  private void initializeStatus() {
    if (this.status == null && this.spec != null && this.spec.getItems() != null) {
      var executionId =
              this.getMetadata().getLabels() != null && !this.getMetadata().getLabels().isEmpty()
          ? this.getMetadata().getLabels().get("execution-id") 
          : "exec-" + System.currentTimeMillis();
      this.status = new ExecutionQueueStatus(this.spec.getItems().size(), executionId);
    }
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

  @JsonIgnore
  public void updateStatusFromItems() {
    if (this.status == null) {
      initializeStatus();
    }
    
    if (this.status != null && this.spec != null && this.spec.getItems() != null) {
      var items = this.spec.getItems();
      var running = (int) items.stream().filter(ExecutionQueueItem::isRunning).count();
      var completed = (int) items.stream().filter(ExecutionQueueItem::isFinished).count();
      var pending = (int) items.stream().filter(ExecutionQueueItem::isPending).count();
      
      this.status.updateProgress(running, completed, pending);
      this.status.updateReconcileTime();
    }
  }
}

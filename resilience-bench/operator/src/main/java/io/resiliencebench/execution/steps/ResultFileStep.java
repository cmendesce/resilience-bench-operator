package io.resiliencebench.execution.steps;

import io.resiliencebench.execution.io.FileProvider;
import io.resiliencebench.execution.io.FileProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.resiliencebench.resources.queue.ExecutionQueue;
import io.resiliencebench.resources.scenario.Scenario;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Service
public class ResultFileStep extends ExecutorStep {

  private final static Logger logger = LoggerFactory.getLogger(ResultFileStep.class);

  private final FileProvider fileProvider;

  public ResultFileStep(KubernetesClient kubernetesClient, FileProviderFactory fileProviderFactory) {
    super(kubernetesClient);
    this.fileProvider = fileProviderFactory.create();
  }

  @Override
  protected boolean isApplicable(Scenario scenario) {
    return true;
  }

  @Override
  protected void internalExecute(Scenario scenario, ExecutionQueue executionQueue) {
    var executionQueueItem = executionQueue.getItem(scenario.getMetadata().getName());
    var currentResults = fileProvider.getFileAsString(executionQueueItem.getResultFile());
    if (currentResults.isPresent()) {
      var currentResultsJson = new JsonObject(currentResults.get());
      currentResultsJson.put("scenario", scenario.getMetadata().getName());

      if (scenario.getSpec().getFault() != null) {
        for (var json : scenario.getSpec().getFault().toJson()) {
          currentResultsJson.put(json.getKey(), json.getValue());
        }
      }
      currentResultsJson.put("workload_name", scenario.getSpec().getWorkload().getWorkloadName());
      currentResultsJson.put("workload_users", scenario.getSpec().getWorkload().getUsers());

      currentResultsJson.put("connectors", scenario.getSpec().toConnectorsInJson());
      var resultsJson = getJsonResults(executionQueue);
      resultsJson.getJsonArray("results").add(currentResultsJson);
      fileProvider.writeToFile(executionQueue.getSpec().getResultFile(), resultsJson.encode(), "application/json");
    } else {
      logger.warn("No results found for {}", executionQueueItem.getResultFile());
    }
  }

  public JsonObject getJsonResults(ExecutionQueue executionQueue) {
    var results = fileProvider.getFileAsString(executionQueue.getSpec().getResultFile());
    JsonObject resultsJson;
    if (results.isPresent()) {
      resultsJson = new JsonObject(results.get());
    } else {
      resultsJson = new JsonObject();
      resultsJson.put("results", new JsonArray());
    }
    return resultsJson;
  }
}

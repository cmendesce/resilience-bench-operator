package io.resiliencebench.resources.scenario;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static io.vertx.core.json.JsonObject.mapFrom;

public class ScenarioSpec {

  @JsonPropertyDescription("The workload to be used in the scenario")
  private ScenarioWorkload workload;

  @JsonPropertyDescription("The name of the scenario")
  private String scenario;

  @JsonPropertyDescription("The set of connectors to be configured while running the scenario")
  private List<Connector> connectors = new ArrayList<>();

  private ScenarioFault fault;

  public ScenarioSpec() {
  }

  public ScenarioSpec(String scenario, ScenarioWorkload workload, List<Connector> connectors) {
    this.workload = workload;
    this.scenario = scenario;
    this.connectors = connectors;
  }

  public ScenarioSpec(String scenario, ScenarioWorkload workload, List<Connector> connectors, ScenarioFault fault) {
    this(scenario, workload, connectors);
    this.fault = fault;
  }

  public ScenarioWorkload getWorkload() {
    return workload;
  }

  public String getScenario() {
    return scenario;
  }

  public List<Connector> getConnectors() {
    return connectors;
  }

  public ScenarioFault getFault() {
    return fault;
  }

  public JsonArray toConnectorsInJson() {
    JsonArray json = new JsonArray();
    for (var connector : connectors) {
      var connectorJson = new JsonObject();

      connectorJson.put("name", connector.getName());
      connectorJson.put("source", connector.getSource().getName());
      if (connector.getSource().getEnvs() != null) {
        for (var key : connector.getSource().getEnvs().keySet()) {
          connectorJson.put("source" + "_env_" + key, connector.getSource().getEnvs().get(key));
        }
      }

      connectorJson.put("destination", connector.getDestination().getName());
      if (connector.getSource().getEnvs() != null) {
        for (var key : connector.getSource().getEnvs().keySet()) {
          connectorJson.put("destination" + "_env_" + key, connector.getSource().getEnvs().get(key));
        }
      }

      if (connector.getFault() != null) {
        for (var item : connector.getFault().toJson()) {
          connectorJson.put(item.getKey(), item.getValue());
        }
      }
      if (connector.getIstio() != null) {
        for (var item : connector.getIstio().toJson()) {
          connectorJson.put(item.getKey(), item.getValue());
        }
      }
      json.add(connectorJson);
    }
    return json;
  }
}

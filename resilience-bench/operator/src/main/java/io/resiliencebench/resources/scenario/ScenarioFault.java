package io.resiliencebench.resources.scenario;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ScenarioFault {
  private String provider;

  private int percentage;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<String> services = new ArrayList<>();

  public ScenarioFault() {
  }

  public ScenarioFault(String provider, int percentage, List<String> services) {
    this.provider = provider;
    this.percentage = percentage;
    this.services = services;
  }

  public String getProvider() {
    return provider;
  }

  public int getPercentage() {
    return percentage;
  }

  public List<String> getServices() {
    return services;
  }

  public String toJson() {
    var object = new JsonObject();
    object.put("provider", provider);
    object.put("percentage", percentage);
    object.put("services", services);
    return object.encode();
  }
}

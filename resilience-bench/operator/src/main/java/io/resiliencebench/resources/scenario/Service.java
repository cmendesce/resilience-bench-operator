package io.resiliencebench.resources.scenario;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

import static io.resiliencebench.support.Maps.toJsonMap;

public class Service {

  private String name;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, JsonNode> envs;

  public Service() {
  }

  public Service(String name) {
    this.name = name;
  }

  public Service(String name, Map<String, Object> envs) {
    this(name);
    this.envs = toJsonMap(envs);
  }

  public String getName() {
    return name;
  }

  public Map<String, JsonNode> getEnvs() {
    return envs;
  }
}

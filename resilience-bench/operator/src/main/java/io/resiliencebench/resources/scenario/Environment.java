package io.resiliencebench.resources.scenario;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.vertx.core.json.JsonObject;

import java.util.Map;

import static io.resiliencebench.support.Maps.toJsonMap;
import static io.resiliencebench.support.Maps.toObjectMap;

public class Environment {

  private String applyTo;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, JsonNode> envs;

  public Environment() {
  }

  public Environment(String applyTo, Map<String, Object> envs) {
    this.applyTo = applyTo;
    this.envs = toJsonMap(envs);
  }

  /**
   * Returns a copy of the given expanded envs
   */
  public Map<String, Object> getEnvs() {
    return toObjectMap(envs);
  }

  public String getApplyTo() {
    return applyTo;
  }

  public JsonObject toJson() {
    return JsonObject.mapFrom(this);
  }
}

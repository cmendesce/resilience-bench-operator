package io.resiliencebench.resources.scenario;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.vertx.core.json.JsonObject;

import java.util.Map;

import static io.resiliencebench.support.Maps.toJsonMap;
import static io.resiliencebench.support.Maps.toObjectMap;

public class IstioPattern {

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, JsonNode> retry;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, JsonNode> timeout;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, JsonNode> circuitBreaker;

  public IstioPattern() {
  }

  public IstioPattern(Map<String, Object> retry, Map<String, Object> timeout, Map<String, Object> circuitBreaker) {
    this.retry = toJsonMap(retry);
    this.timeout = toJsonMap(timeout);
    this.circuitBreaker = toJsonMap(circuitBreaker);
  }

  public Map<String, Object> getRetry() {
    return toObjectMap(retry);
  }

  public Map<String, Object> getTimeout() {
    return toObjectMap(timeout);
  }

  public Map<String, Object> getCircuitBreaker() {
    return toObjectMap(circuitBreaker);
  }

  public JsonObject toJson() {
    var jsonTimeout = timeout == null ? JsonObject.of() : JsonObject.mapFrom(timeout);
    var jsonCircuitBreaker = circuitBreaker == null ? JsonObject.of() : JsonObject.mapFrom(circuitBreaker);
    var jsonRetry = retry == null ? JsonObject.of() : JsonObject.mapFrom(retry);

    return JsonObject.of(
            "retry", jsonRetry,
            "timeout", jsonTimeout,
            "circuitBreaker", jsonCircuitBreaker
    );
  }
}

package io.resiliencebench.resources.scenario;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IstioPatternTest {

  @Test
  void shouldTransformToJsonWhenRetryIsNull() {
    IstioPattern istioPattern = new IstioPattern();
    assertNotNull(istioPattern.toJson());
  }

  @Test
  void shouldTransformToJsonWhenRetryIsNotNull() {
    IstioPattern istioPattern = new IstioPattern(Map.of("attempts", 1, "perTryTimeout", 1000), null, null);
    var actual = istioPattern.toJson();
    assertEquals(1, actual.getJsonObject("retry").getInteger("attempts").intValue());
    assertEquals(1000, actual.getJsonObject("retry").getInteger("perTryTimeout").intValue());
  }

  @Test
  void shouldTransformToJsonWhenTimeoutIsNull() {
    IstioPattern istioPattern = new IstioPattern();
    assertNotNull(istioPattern.toJson());
  }

  @Test
  void shouldTransformToJsonWhenTimeoutIsNotNull() {
    IstioPattern istioPattern = new IstioPattern(null, Map.of("timeout", 1000), null);
    var actual = istioPattern.toJson();
    assertEquals(1000, actual.getJsonObject("timeout").getInteger("timeout").intValue());
  }

  @Test
  void shouldTransformToJsonWhenCircuitBreakerIsNull() {
    IstioPattern istioPattern = new IstioPattern();
    assertNotNull(istioPattern.toJson());
  }

  @Test
  void shouldTransformToJsonWhenCircuitBreakerIsNotNull() {
    IstioPattern istioPattern = new IstioPattern(null, null, Map.of("maxConnections", 1, "httpMaxPendingRequests", 1));
    var actual = istioPattern.toJson();
    assertEquals(1, actual.getJsonObject("circuitBreaker").getInteger("maxConnections").intValue());
    assertEquals(1, actual.getJsonObject("circuitBreaker").getInteger("httpMaxPendingRequests").intValue());
  }
}
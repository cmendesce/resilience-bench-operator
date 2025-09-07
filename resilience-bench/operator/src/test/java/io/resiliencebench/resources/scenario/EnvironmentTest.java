package io.resiliencebench.resources.scenario;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvironmentTest {

  @Test
  void toJson() {
    var environment = new Environment("client", Map.of("key", 1, "key2", 2));
    var json = environment.toJson();
    assertEquals("client", json.getString("applyTo"));
    assertEquals(1, json.getJsonObject("envs").getInteger("key"));
    assertEquals(2, json.getJsonObject("envs").getInteger("key2"));
  }
}
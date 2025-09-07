package io.resiliencebench.resources.scenario;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnvironmentEnvsTest {

  @Test
  void testPatternConfigWithArrayUsingConstructor() {
    Map<String, Object> patternConfig = new HashMap<>();
    String key = "testArray";
    List<Integer> value = List.of(1, 2, 3);
    patternConfig.put(key, value);

    var source = new Environment("source-service", patternConfig);

    var array = (List) source.getEnvs().get(key);
    Assertions.assertNotNull(array);
    assertEquals(3, array.size());
    assertEquals(1, array.get(0));
  }

  @Test
  void testPatternConfigWithNullUsingConstructor() {
    Map<String, Object> patternConfig = new HashMap<>();
    String key = "testNull";
    patternConfig.put(key, null);
    var source = new Environment("source-service", patternConfig);
    assertNull(source.getEnvs().get(key));
  }

  @ParameterizedTest(name = "pattern parameter w/ string: {0}")
  @ValueSource(strings = {"test", "123", "true", "false", "null", "test-123", "test_123", "tést", "test.123", "test-123.test_123.test.123"})
  void testWithString(String value) {
    Map<String, Object> patternConfig = new HashMap<>();
    String key = "test";
    patternConfig.put(key, value);
    var source = new Environment("source-service", patternConfig);
    assertEquals(value, source.getEnvs().get(key));
  }

  @ParameterizedTest(name = "pattern parameter w/ int: {0}")
  @ValueSource(ints = {0, 1, 100, 1000, -1, Integer.MAX_VALUE, Integer.MIN_VALUE})
  void testWithInteger(int value) {
    Map<String, Object> patternConfig = new HashMap<>();
    String key = "test";
    patternConfig.put(key, value);
    var source = new Environment("source-service", patternConfig);
    assertEquals(value, source.getEnvs().get(key));
  }

  @ParameterizedTest(name = "pattern parameter w/ long: {0}")
  @ValueSource(longs = {0L, 1L, 100L, 1000L, -1L, Long.MAX_VALUE, Long.MIN_VALUE})
  void testWithInteger(long value) {
    Map<String, Object> patternConfig = new HashMap<>();
    String key = "test";
    patternConfig.put(key, value);
    var source = new Environment("source-service", patternConfig);
    assertEquals(value, source.getEnvs().get(key));
  }

  @ParameterizedTest(name = "pattern parameter w/ boolean: {0}")
  @ValueSource(booleans = {true, false})
  void testWithBoolean(boolean value) {
    Map<String, Object> patternConfig = new HashMap<>();
    String key = "test";
    patternConfig.put(key, value);
    var source = new Environment("source-service", patternConfig);
    assertEquals(value, source.getEnvs().get(key));
  }

  @ParameterizedTest(name = "pattern parameter w/ boolean: {0}")
  @ValueSource(doubles = {0, 0.1, 1.234, 100.0, 1000.0, -1.0, Double.MAX_VALUE, Double.MIN_VALUE})
  void testWithDouble(double value) {
    Map<String, Object> patternConfig = new HashMap<>();
    String key = "test";
    patternConfig.put(key, value);
    var source = new Environment("source-service", patternConfig);
    assertEquals(value, source.getEnvs().get(key));
  }
}
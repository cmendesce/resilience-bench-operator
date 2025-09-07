package io.resiliencebench.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Maps {

  private static final ObjectMapper mapper = new ObjectMapper();

  public static Map<String, JsonNode> toJsonMap(Map<String, Object> mapObject) {
    if (mapObject == null) {
      return Map.of();
    }
    return mapObject.entrySet().stream()
            .collect(LinkedHashMap::new,
                    (map, entry) -> map.put(entry.getKey(), mapper.valueToTree(entry.getValue())),
                    LinkedHashMap::putAll);
  }

  public static Map<String, Object> toObjectMap(Map<String, JsonNode> jsonMap) {
    return jsonMap.entrySet().stream()
            .collect(LinkedHashMap::new,
                    (map, entry) -> map.put(entry.getKey(), Maps.toObject(entry.getValue())),
                    LinkedHashMap::putAll);
  }

  public static Object toObject(JsonNode jsonNode) {
    if (jsonNode == null) {
      return null;
    }
    if (jsonNode.isArray()) {
      List<Object> list = new ArrayList<>();
      jsonNode.elements().forEachRemaining(element -> list.add(toObject(element)));
      return list;
    } else if (jsonNode.isTextual()) {
      return jsonNode.textValue();
    } else if (jsonNode.isBoolean()) {
      return jsonNode.booleanValue();
    } else if (jsonNode.isNumber()) {
      if (jsonNode.isDouble() || jsonNode.isFloatingPointNumber()) {
        return jsonNode.doubleValue();
      } else if (jsonNode.isInt()) {
        return jsonNode.intValue();
      } else {
        return jsonNode.longValue();
      }
    } else if (jsonNode.isNull()) {
      return null;
    }
    return jsonNode;
  }
}

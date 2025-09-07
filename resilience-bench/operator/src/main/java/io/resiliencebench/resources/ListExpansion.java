package io.resiliencebench.resources;

import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.resiliencebench.support.JsonNodeObjects.toObject;

final class ListExpansion {
    public ListExpansion() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Map<String, Object>> expandConfigTemplate(NameValueProperties patternConfigs) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        resultList.add(new HashMap<>());
        for (var config : patternConfigs) {
            var key = config.getName();
            var value = config.getValue();
            if (value.isArray()) {
                var arrayNode = (ArrayNode) value;
                resultList = multiplyList(resultList, key, arrayNode);
            } else {
              resultList.forEach(map -> map.put(key, toObject(value)));
            }
        }
        return resultList;
    }

    private static List<Map<String, Object>> multiplyList(List<Map<String, Object>> currentList, String key, ArrayNode valueArray) {
        List<Map<String, Object>> newList = new ArrayList<>();
        for (var existingMap : currentList) {
            for (var arrayItem : valueArray) {
                Map<String, Object> newMap = new HashMap<>(existingMap);
                newMap.put(key, toObject(arrayItem));
                newList.add(newMap);
            }
        }
        return newList;
    }
}
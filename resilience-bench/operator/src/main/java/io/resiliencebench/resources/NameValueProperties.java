package io.resiliencebench.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;

import static io.resiliencebench.support.JsonNodeObjects.toObject;
import static java.util.stream.Collectors.joining;

public class NameValueProperties extends ArrayList<NameValueProperties.Attribute> {
  public NameValueProperties() {
  }

  public NameValueProperties(Attribute... params) {
    this.addAll(Arrays.asList(params));
  }

  @Override
  public String toString() {
    var serialized = this.stream()
            .map(Attribute::toString)
            .collect(joining("."));
    if (serialized.isEmpty()) {
      serialized = "none";
    }
    return serialized;
  }

  public static class Attribute {
    @JsonIgnore
    private static final ObjectMapper mapper = new ObjectMapper();

    private String name;
    private JsonNode value;

    public Attribute() {
    }

    public Attribute(String name, JsonNode value) {
      this.name = name;
      this.value = value;
    }

    public Attribute(String name, Object value) {
      this(name, mapper.valueToTree(value));
    }

    public String getName() {
      return name;
    }

    public JsonNode getValue() {
      return value;
    }

    @JsonIgnore
    public Object getValueAsObject() {
      return toObject(value);
    }

    @Override
    public String toString() {
      return getName() + "-" + getValueAsObject();
    }
  }
}

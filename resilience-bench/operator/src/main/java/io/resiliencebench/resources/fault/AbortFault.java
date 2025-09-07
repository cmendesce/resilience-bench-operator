package io.resiliencebench.resources.fault;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.generator.annotation.Max;
import io.fabric8.generator.annotation.Min;

public record AbortFault(
        @Min(100) @Max(599) @JsonPropertyDescription("The HTTP status code to be returned") int httpStatus) {
  @Override
  public String toString() {
    return "abort-" + httpStatus;
  }
}
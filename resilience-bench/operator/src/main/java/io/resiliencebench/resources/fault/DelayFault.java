package io.resiliencebench.resources.fault;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.generator.annotation.Min;

public record DelayFault(@Min(1) @JsonPropertyDescription("Delay duration in milliseconds") int duration) {

  public DelayFault {
    if (duration < 0) {
      throw new IllegalArgumentException("Duration must be non-negative");
    }
  }

  @Override
  public String toString() {
    return "delay-" + duration + "ms";
  }
}

package io.resiliencebench.resources.fault;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FaultTemplateTest {

  @Test
  public void testToStringAbort() {
    var faultTemplate = new FaultTemplate(10, new AbortFault(500)) {
    };
    assertEquals("abort-500-10p", faultTemplate.toString());
  }

  @Test
  public void testToStringDelay() {
    var faultTemplate = new FaultTemplate(10, new DelayFault(100)) {
    };
    assertEquals("delay-100ms-10p", faultTemplate.toString());
  }
}
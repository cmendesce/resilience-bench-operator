package io.resiliencebench.resources.fault;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DelayFaultTest {

    @Test
    void shouldCreateDelayFaultWithValidDuration() {
        assertEquals("delay-100ms", new DelayFault(100).toString());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 100, 1000, 5000})
    void shouldCreateDelayFaultWithDifferentDurations(int duration) {
        var fault = new DelayFault(duration);
        assertEquals("delay-" + duration + "ms", fault.toString());
    }

    @Test
    void shouldRejectNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () -> new DelayFault(-1));
    }
}
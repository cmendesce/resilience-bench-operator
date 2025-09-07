package io.resiliencebench.resources.fault;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbortFaultTest {

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 500, 502, 503})
    void shouldCreateAbortFaultWithDifferentStatusCodes(int statusCode) {
        var fault = new AbortFault(statusCode);
        assertEquals("abort-" + statusCode, fault.toString());
    }

    @Test
    void shouldHandleInvalidStatusCodes() {
        assertThrows(IllegalArgumentException.class, () -> new AbortFault(0));
        assertThrows(IllegalArgumentException.class, () -> new AbortFault(-1));
    }
}
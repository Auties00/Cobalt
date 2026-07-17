package com.github.auties00.cobalt.exception;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudUnsupportedVersionException;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CloudUnsupportedVersionException")
class CloudUnsupportedVersionExceptionTest {
    @Test
    @DisplayName("Carries the operation and the required/configured versions")
    void carriesContext() {
        var exception = new WhatsAppCloudUnsupportedVersionException(
                "startCall", CloudApiVersion.V22_0, CloudApiVersion.V19_0);
        assertEquals("startCall", exception.operation());
        assertEquals(CloudApiVersion.V22_0, exception.requiredVersion());
        assertEquals(CloudApiVersion.V19_0, exception.configuredVersion());
        assertTrue(exception.getMessage().contains("startCall"));
        assertTrue(exception.getMessage().contains("v22.0"));
        assertTrue(exception.getMessage().contains("v19.0"));
    }

    @Test
    @DisplayName("Discards as a non-fatal Cloud exception in the sealed hierarchy")
    void nonFatalCloudException() {
        var exception = new WhatsAppCloudUnsupportedVersionException(
                "deleteMessageTemplates", CloudApiVersion.V25_0, CloudApiVersion.V24_0);
        assertEquals(WhatsAppLinkedClientErrorResult.DISCARD, exception.toErrorResult());
        assertInstanceOf(WhatsAppCloudException.class, exception);
        assertInstanceOf(WhatsAppException.class, exception);
    }
}

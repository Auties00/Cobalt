package com.github.auties00.cobalt.client.linked;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.auties00.cobalt.client.WhatsAppClient;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LazyPasskeyAuthenticatorTest {

    /** Hosts without the native passkey library (e.g. a container without libfido2) must still build clients. */
    @Test
    void defaultVerificationHandlersDoNotLoadTheNativePasskeyBackend() {
        assertDoesNotThrow(() -> LinkedWhatsAppClientVerificationHandler.Web.QrCode.toTerminal());
        assertDoesNotThrow(() -> WhatsAppClient.builder().linkedApi().webClient());
    }

    @Test
    void backendIsCreatedOnceOnFirstCeremony() {
        var created = new AtomicInteger();
        var lazy = new LazyPasskeyAuthenticator(() -> {
            created.incrementAndGet();
            return request -> {
                throw new IllegalStateException("no credential");
            };
        });
        assertEquals(0, created.get());

        assertThrows(IllegalStateException.class, () -> lazy.assertCredential(null));
        assertThrows(IllegalStateException.class, () -> lazy.assertCredential(null));
        assertEquals(1, created.get());
    }
}

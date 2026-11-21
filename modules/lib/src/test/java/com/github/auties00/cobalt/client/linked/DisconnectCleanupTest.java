package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.client.WhatsAppClientDisconnectReason;
import com.github.auties00.cobalt.exception.linked.WhatsAppSessionException;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class DisconnectCleanupTest {
    @Test
    void disconnectBeforeFirstSyncCompletes() throws Exception {
        var client = LinkedWhatsAppClient.builder()
                .webClient(LinkedWhatsAppStoreFactory.temporary())
                .createConnection()
                .unregistered(LinkedWhatsAppClientVerificationHandler.Web.QrCode.toTerminal());

        var disconnectedReason =
                new AtomicReference<WhatsAppClientDisconnectReason>();

        client.addDisconnectedListener((api, reason) ->
                disconnectedReason.set(reason));

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            client.disconnect();
        });

        assertEquals(
                WhatsAppClientDisconnectReason.DISCONNECTED,
                disconnectedReason.get()
        );
    }

    @Test
    void logoutBeforeFirstSyncCompletes() throws Exception {
        var client = LinkedWhatsAppClient.builder()
                .webClient(LinkedWhatsAppStoreFactory.temporary())
                .createConnection()
                .unregistered(LinkedWhatsAppClientVerificationHandler.Web.QrCode.toTerminal());

        var disconnectedReason =
                new AtomicReference<WhatsAppClientDisconnectReason>();

        client.addDisconnectedListener((api, reason) ->
                disconnectedReason.set(reason));

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            client.handleFailure(
                    new WhatsAppSessionException.LoggedOut(
                            "Server reported logout, reason=401"
                    )
            );
        });

        assertEquals(
                WhatsAppClientDisconnectReason.LOGGED_OUT,
                disconnectedReason.get()
        );
    }
}
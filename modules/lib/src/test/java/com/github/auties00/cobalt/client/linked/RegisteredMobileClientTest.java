package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisteredMobileClientTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("constructs a registered mobile client without a Web verification handler")
    void registered(boolean business) throws Exception {
        var options = LinkedWhatsAppClient.builder().mobileClient(LinkedWhatsAppStoreFactory.temporary()).createConnection();
        var store = options.store;
        store.accountStore().setPhoneNumber(12025550123L);
        store.accountStore().setDevice(LinkedWhatsAppClientDevice.android(business));
        store.accountStore().setClientVersion(ClientAppVersion.of("2.26.41.78"));
        store.accountStore().setJid(Jid.of(12025550123L));
        store.accountStore().setRegistered(true);

        var client = assertDoesNotThrow(() -> options.registered().orElseThrow());
        try {
            assertSame(store, client.store());
            assertTrue(client.store().accountStore().registered());
        } finally {
            // The client never connects; disconnect would wait for the AB-prop sync that only connect starts.
            var field = client.getClass().getDeclaredField("connectivityMonitor");
            field.setAccessible(true);
            ((AutoCloseable) field.get(client)).close();
        }
    }
}

package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDeviceWebBuilder;
import com.github.auties00.cobalt.exception.WhatsAppException;
import com.github.auties00.cobalt.ProxyServer;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration coverage of {@link WhatsAppSocketClient} against the live WhatsApp servers across
 * every supported transport (TCP, WebSocket), handshake shape (web, mobile) and proxy flavour
 * (HTTP plain, HTTPS, SOCKS4, SOCKS5). Each scenario opens a real connection and asserts the
 * Noise XX handshake derived both read and write keys; reconnect scenarios run two handshakes to
 * confirm no state leaks across sessions.
 *
 * <p>Harness design: each proxy-flavour test stands up a local {@link ProxyServer} torn down in
 * {@link #tearDown()}; every scenario uses a fresh {@link LinkedWhatsAppStore} from
 * {@link LinkedWhatsAppStoreFactory#temporary()}; the {@link CapturingListener} latches on the first
 * stanza or close event so a scenario can verify the handshake landed without blocking on the full
 * connect/auth flow. HTTPS-proxy tests use {@link #trustAllSslContextFactory()} because the local
 * proxy presents a self-signed certificate.
 */
@Timeout(60)
class WhatsAppSocketTest {
    private WhatsAppSocketClient client;

    private ProxyServer proxyServer;

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception _) {
            }
            client = null;
        }
        if (proxyServer != null) {
            proxyServer.close();
            proxyServer = null;
        }
    }

    @Test
    void testWebReconnect() throws Exception {
        var store1 = createWebStore();
        client = WhatsAppSocketClient.newCipheredSocketClient(store1);
        assertHandshakeSucceeds();
        client.disconnect();
        client = null;

        var store2 = createWebStore();
        client = WhatsAppSocketClient.newCipheredSocketClient(store2);
        assertHandshakeSucceeds();
    }

    @Test
    void testWebNoProxy() throws Exception {
        var store = createWebStore();
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertInstanceOf(WhatsAppSocketClient.WebSocket.class, client);
        assertHandshakeSucceeds();
    }

    @Test
    void testWebHttpProxy() throws Exception {
        proxyServer = ProxyServer.http();
        var store = createWebStore();
        store.connectionStore().setProxy(proxyServer.toProxy());
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertHandshakeSucceeds();
    }

    @Test
    void testWebHttpsProxy() throws Exception {
        proxyServer = ProxyServer.https();
        var store = createWebStore();
        store.connectionStore().setProxy(proxyServer.toProxy());
        client = WhatsAppSocketClient.newCipheredSocketClient(store, trustAllSslContextFactory());
        assertHandshakeSucceeds();
    }

    @Test
    void testMobileNoProxy() throws Exception {
        var store = createMobileStore();
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertInstanceOf(WhatsAppSocketClient.Tcp.class, client);
        assertHandshakeSucceeds();
    }

    @Test
    void testMobileHttpProxy() throws Exception {
        proxyServer = ProxyServer.http();
        var store = createMobileStore();
        store.connectionStore().setProxy(proxyServer.toProxy());
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertHandshakeSucceeds();
    }

    @Test
    void testMobileHttpsProxy() throws Exception {
        proxyServer = ProxyServer.https();
        var store = createMobileStore();
        store.connectionStore().setProxy(proxyServer.toProxy());
        client = WhatsAppSocketClient.newCipheredSocketClient(store, trustAllSslContextFactory());
        assertHandshakeSucceeds();
    }

    @Test
    void testWebSocks4Proxy() throws Exception {
        proxyServer = ProxyServer.socks4();
        var store = createWebStore();
        store.connectionStore().setProxy(proxyServer.toProxy());
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertHandshakeSucceeds();
    }

    @Test
    void testWebSocks5Proxy() throws Exception {
        proxyServer = ProxyServer.socks5();
        var store = createWebStore();
        store.connectionStore().setProxy(proxyServer.toProxy());
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertHandshakeSucceeds();
    }

    @Test
    void testDesktopReconnect() throws Exception {
        var store1 = createDesktopStore();
        client = WhatsAppSocketClient.newCipheredSocketClient(store1);
        assertHandshakeSucceeds();
        client.disconnect();
        client = null;

        var store2 = createDesktopStore();
        client = WhatsAppSocketClient.newCipheredSocketClient(store2);
        assertHandshakeSucceeds();
    }

    @Test
    void testDesktopNoProxy() throws Exception {
        var store = createDesktopStore();
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertInstanceOf(WhatsAppSocketClient.Tcp.class, client);
        assertHandshakeSucceeds();
    }

    @Test
    void testDesktopHttpProxy() throws Exception {
        proxyServer = ProxyServer.http();
        var store = createDesktopStore();
        store.connectionStore().setProxy(proxyServer.toProxy());
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertHandshakeSucceeds();
    }

    @Test
    void testDesktopHttpsProxy() throws Exception {
        proxyServer = ProxyServer.https();
        var store = createDesktopStore();
        store.connectionStore().setProxy(proxyServer.toProxy());
        client = WhatsAppSocketClient.newCipheredSocketClient(store, trustAllSslContextFactory());
        assertHandshakeSucceeds();
    }

    @Test
    void testDesktopSocks4Proxy() throws Exception {
        proxyServer = ProxyServer.socks4();
        var store = createDesktopStore();
        store.connectionStore().setProxy(proxyServer.toProxy());
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertHandshakeSucceeds();
    }

    @Test
    void testDesktopSocks5Proxy() throws Exception {
        proxyServer = ProxyServer.socks5();
        var store = createDesktopStore();
        store.connectionStore().setProxy(proxyServer.toProxy());
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertHandshakeSucceeds();
    }

    @Test
    void testMobileSocks4Proxy() throws Exception {
        proxyServer = ProxyServer.socks4();
        var store = createMobileStore();
        store.connectionStore().setProxy(proxyServer.toProxy());
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertHandshakeSucceeds();
    }

    @Test
    void testMobileSocks5Proxy() throws Exception {
        proxyServer = ProxyServer.socks5();
        var store = createMobileStore();
        store.connectionStore().setProxy(proxyServer.toProxy());
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertHandshakeSucceeds();
    }

    @Test
    void testDisconnectCleansUp() throws Exception {
        var store = createMobileStore();
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        var listener = new CapturingListener();
        client.connect(listener);
        assertTrue(client.isConnected());
        client.disconnect();
        assertFalse(client.isConnected());
        listener.await();
    }

    @Test
    void testConnectRejectsNullListener() {
        var store = createMobileStore();
        client = WhatsAppSocketClient.newCipheredSocketClient(store);
        assertThrows(NullPointerException.class, () -> client.connect(null));
    }

    // Trust-all factory for the locally-signed HTTPS proxy; test-only, removes all certificate
    // validation and would expose production traffic to MITM if used outside fixtures.
    private static WhatsAppSslContextFactory trustAllSslContextFactory() {
        return new WhatsAppSslContextFactory() {
            @Override
            public SSLContext sslContext() {
                try {
                    var ctx = SSLContext.getInstance("TLS");
                    ctx.init(null, new TrustManager[]{
                            new X509TrustManager() {
                                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                                }

                                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                                }

                                public X509Certificate[] getAcceptedIssuers() {
                                    return new X509Certificate[0];
                                }
                            }
                    }, null);
                    return ctx;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public SSLParameters sslParameters() {
                var params = new SSLParameters();
                params.setApplicationProtocols(new String[]{"http/1.1"});
                return params;
            }
        };
    }

    private void assertHandshakeSucceeds() throws Exception {
        var listener = new CapturingListener();
        client.connect(listener);
        assertTrue(client.isConnected(), "Should be connected after handshake");
        assertNotNull(client.writeKey(), "Write key should be derived");
        assertNotNull(client.readKey(), "Read key should be derived");
        listener.await();
    }

    private static LinkedWhatsAppStore createMobileStore() {
        try {
            return LinkedWhatsAppStoreFactory.temporary()
                    .create(LinkedWhatsAppClientType.MOBILE, 15551234567L);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static LinkedWhatsAppStore createWebStore() {
        try {
            var store = LinkedWhatsAppStoreFactory.temporary()
                    .create(LinkedWhatsAppClientType.WEB, UUID.randomUUID());
            store.accountStore().setDevice(new LinkedWhatsAppClientDeviceWebBuilder()
                    .model("Surface Pro 4")
                    .manufacturer("Microsoft")
                    .platform(ClientPlatformType.WEB)
                    .clientType(LinkedWhatsAppClientType.WEB)
                    .build());
            return store;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static LinkedWhatsAppStore createDesktopStore() {
        try {
            var store = LinkedWhatsAppStoreFactory.temporary()
                    .create(LinkedWhatsAppClientType.WEB, UUID.randomUUID());
            store.accountStore().setDevice(new LinkedWhatsAppClientDeviceWebBuilder()
                    .model("MacBook Pro")
                    .manufacturer("Apple")
                    .platform(ClientPlatformType.MACOS)
                    .osDeviceAppVersion(ClientAppVersion.of("14.5"))
                    .clientType(LinkedWhatsAppClientType.WEB)
                    .build());
            return store;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Records every callback; the latch counts down on the first inbound stanza or orderly close so
    // await() can park until the first event, then surfaces any captured errors as test failures.
    private static class CapturingListener implements WhatsAppSocketListener {
        private final List<Stanza> stanzas;

        private final List<WhatsAppException> errors;

        private final CountDownLatch latch;

        public CapturingListener() {
            this.stanzas = new CopyOnWriteArrayList<>();
            this.errors = new CopyOnWriteArrayList<>();
            this.latch = new CountDownLatch(1);
        }

        @Override
        public void onNode(Stanza stanza) {
            stanzas.add(stanza);
            latch.countDown();
        }

        @Override
        public void onError(WhatsAppException exception) {
            errors.add(exception);
        }

        @Override
        public void onClose() {
            latch.countDown();
        }

        public void await() throws InterruptedException {
            if(!latch.await(30, TimeUnit.SECONDS)) {
                fail("Timed out");
            }

            if(!errors.isEmpty()) {
                fail("Unexpected errors: " + errors);
            }
        }
    }
}

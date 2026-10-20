package com.github.auties00.cobalt.registration;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.client.linked.info.WhatsAppMobileClientInfo;
import com.github.auties00.cobalt.registration.MobileClientRegistration;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.parallel.Isolated;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Exercises the encrypted exist request with synthetic app metadata and an in-memory peer. */
@Isolated("Temporarily replaces app metadata caches and the registration peer key")
class MobileExistTokenTest {
    @ParameterizedTest
    @EnumSource(value = ClientPlatformType.class, names = {"ANDROID", "ANDROID_BUSINESS", "IOS", "IOS_BUSINESS"})
    @DisplayName("includes an APK token in Android exist requests and leaves iOS token-free")
    void existToken(ClientPlatformType platform) throws Exception {
        var store = LinkedWhatsAppStoreFactory.temporary().create(LinkedWhatsAppClientType.MOBILE, 12025550123L);
        var android = platform == ClientPlatformType.ANDROID || platform == ClientPlatformType.ANDROID_BUSINESS;
        var business = platform == ClientPlatformType.ANDROID_BUSINESS || platform == ClientPlatformType.IOS_BUSINESS;
        store.accountStore().setDevice(android ? LinkedWhatsAppClientDevice.android(business) : LinkedWhatsAppClientDevice.ios(business));
        var infoClass = Class.forName("com.github.auties00.cobalt.client.linked.info.WhatsApp" + (android ? "Android" : "Ios") + "ClientInfo");
        var cache = infoClass.getDeclaredField((business ? "business" : "personal") + (android ? "ApkInfo" : "IpaInfo"));
        cache.setAccessible(true);
        var originalInfo = cache.get(null);
        var version = ClientAppVersion.of("2.26.41.78");
        Object fixture;
        if (android) {
            var constructor = infoClass.getDeclaredConstructor(ClientAppVersion.class, int.class, byte[].class, SecretKeySpec.class, byte[][].class, boolean.class);
            constructor.setAccessible(true);
            fixture = constructor.newInstance(version, 1, new byte[16], new SecretKeySpec(new byte[20], "HmacSHA1"), new byte[][] {new byte[] {1, 2, 3}}, business);
        } else {
            var constructor = infoClass.getDeclaredConstructor(ClientAppVersion.class, boolean.class);
            constructor.setAccessible(true);
            fixture = constructor.newInstance(version, business);
        }
        cache.set(null, fixture);
        try {
            var expected = URLDecoder.decode(WhatsAppMobileClientInfo.of(platform)
                    .computeRegistrationToken(2025550123L), StandardCharsets.UTF_8);
            var peer = SignalIdentityKeyPair.random();
            var keyField = MobileClientRegistration.class.getDeclaredField("REGISTRATION_PUBLIC_KEY");
            keyField.setAccessible(true);
            var serverKey = (byte[]) keyField.get(null);
            var original = serverKey.clone();
            System.arraycopy(peer.publicKey().toEncodedPoint(), 0, serverKey, 0, serverKey.length);
            try (var registration = MobileClientRegistration.newRegistration(store,
                    LinkedWhatsAppClientVerificationHandler.Mobile.sms(() -> { throw new AssertionError("No OTP expected"); }),
                    null, null)) {
                var client = new PeerClient(peer, android ? expected : null);
                var httpField = MobileClientRegistration.class.getDeclaredField("httpClient");
                httpField.setAccessible(true);
                ((HttpClient) httpField.get(registration)).close();
                httpField.set(registration, client);
                var exist = MobileClientRegistration.class.getDeclaredMethod("assertRegistrationKeys");
                exist.setAccessible(true);
                try { exist.invoke(registration); }
                catch (java.lang.reflect.InvocationTargetException failure) {
                    if (failure.getCause() instanceof AssertionError assertion) throw assertion;
                    throw failure;
                }
                assertEquals(1, client.existCount);
            } finally { System.arraycopy(original, 0, serverKey, 0, serverKey.length); }
        } finally { cache.set(null, originalInfo); }
    }

    private static final class PeerClient extends HttpClient {
        private final SignalIdentityKeyPair peer;
        private final String expected;
        private int existCount;
        PeerClient(SignalIdentityKeyPair peer, String expected) { this.peer = peer; this.expected = expected; }
        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }
        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }
        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }
        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }
        @Override
        public SSLContext sslContext() {
            throw new UnsupportedOperationException();
        }
        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }
        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }
        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }
        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }
        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h) {
            throw new AssertionError("No async send expected");
        }
        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h, HttpResponse.PushPromiseHandler<T> p) {
            throw new AssertionError("No async send expected");
        }
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            String response = "{\"status\":\"ok\"}";
            if (request.uri().getPath().equals("/v2/exist")) {
                existCount++;
                var bytes = new CompletableFuture<byte[]>();
                var output = new java.io.ByteArrayOutputStream();
                var subscriber = new Flow.Subscriber<ByteBuffer>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscription.request(Long.MAX_VALUE);
                    }
                    public void onNext(ByteBuffer buffer) {
                        var chunk = new byte[buffer.remaining()];
                        buffer.get(chunk);
                        output.writeBytes(chunk);
                    }
                    @Override
                    public void onError(Throwable failure) {
                        bytes.completeExceptionally(failure);
                    }
                    @Override
                    public void onComplete() {
                        bytes.complete(output.toByteArray());
                    }
                };
                request.bodyPublisher().orElseThrow().subscribe(subscriber);
                try {
                    var envelope = new String(bytes.join(), StandardCharsets.UTF_8);
                    var encoded = Base64.getUrlDecoder().decode(envelope.substring(4));
                    var shared = Curve25519.sharedKey(peer.privateKey().toEncodedPoint(), Arrays.copyOf(encoded, 32));
                    var cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(shared, "AES"), new GCMParameterSpec(128, new byte[12]));
                    var form = new String(cipher.doFinal(Arrays.copyOfRange(encoded, 32, encoded.length)), StandardCharsets.UTF_8);
                    var fields = new HashMap<String, String>();
                    for (var pair : form.split("&")) {
                        var parts = pair.split("=", 2);
                        fields.put(parts[0], URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                    }
                    assertEquals(expected, fields.get("token"), "Unexpected registration token in exist request");
                } catch (AssertionError failure) { throw failure; }
                catch (Exception failure) { throw new AssertionError("Cannot decode test-peer request", failure); }
                response = "{\"status\":\"fail\",\"reason\":\"incorrect\"}";
            } else if (!request.uri().getPath().equals("/v2/client_log")) {
                throw new AssertionError("Unexpected endpoint");
            }
            var body = (T) response.getBytes(StandardCharsets.UTF_8);
            return new HttpResponse<>() {
                @Override
                public int statusCode() {
                    return 200;
                }
                @Override
                public HttpRequest request() {
                    return request;
                }
                @Override
                public Optional<HttpResponse<T>> previousResponse() {
                    return Optional.empty();
                }
                @Override
                public HttpHeaders headers() {
                    return HttpHeaders.of(Map.of(), (a,b) -> true);
                }
                @Override
                public T body() {
                    return body;
                }
                @Override
                public Optional<SSLSession> sslSession() {
                    return Optional.empty();
                }
                @Override
                public URI uri() {
                    return request.uri();
                }
                @Override
                public Version version() {
                    return Version.HTTP_1_1;
                }
            };
        }
    }
}

package it.auties.whatsapp.registration.http;

import it.auties.whatsapp.util.Exceptions;
import it.auties.whatsapp.util.ProxyAuthenticator;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class HttpClient {
    private static final String[] IOS_CIPHER_SUITE = {
            // "TLS_GREASE_IS_THE_WORD_1A",
            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_RSA_WITH_AES_256_CBC_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_RSA_WITH_3DES_EDE_CBC_SHA"
    };
    private static final String[] ANDROID_CIPHER_SUITE = {
            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_256_CBC_SHA"
    };
    private static final String[] ALLOWED_NAMED_GROUPS = {
            "x25519",
            "secp256r1",
            "secp384r1",
            "secp521r1",
            "x448",
            "ffdhe2048",
            "ffdhe3072",
            "ffdhe4096",
            "ffdhe6144",
            "ffdhe8192",

    };

    static {
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
        ProxyAuthenticator.allowAll();
        Authenticator.setDefault(ProxyAuthenticator.globalAuthenticator());
    }

    private final SecureRandom random;
    private final SSLSocketFactory sslFactory;
    public HttpClient(boolean ios) {
        this.random = new SecureRandom();
        this.sslFactory = createSocketFactory(ios);
    }

    public static String toFormParams(Map<String, ?> values) {
        return values.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    public static Map<String, String> parseFormParams(String params) {
        return Arrays.stream(params.split("&"))
                .map(entry -> entry.split("=", 2))
                .collect(Collectors.toUnmodifiableMap(entry -> entry[0], entry -> entry[1]));
    }

    public CompletableFuture<byte[]> get(URI uri, boolean useCustomSslFactory, Proxy proxy) {
        return sendRequest("GET", uri, useCustomSslFactory, proxy, null, null);
    }

    public CompletableFuture<String> get(URI uri, boolean useCustomSslFactory, Map<String, ?> headers) {
        return sendRequest("GET", uri, useCustomSslFactory, null, headers, null)
                .thenApplyAsync(String::new);
    }

    public CompletableFuture<String> get(URI uri, Proxy proxy, boolean useCustomSslFactory, Map<String, ?> headers) {
        return sendRequest("GET", uri, useCustomSslFactory, proxy, headers, null)
                .thenApplyAsync(String::new);
    }

    public CompletableFuture<byte[]> post(URI uri, boolean useCustomSslFactory, Map<String, ?> headers) {
        return sendRequest("POST", uri, useCustomSslFactory, null, headers, null);
    }

    public CompletableFuture<byte[]> post(URI uri, Proxy proxy, boolean useCustomSslFactory, Map<String, ?> headers) {
        return sendRequest("POST", uri, useCustomSslFactory, proxy, headers, null);
    }

    public CompletableFuture<byte[]> post(URI uri, boolean useCustomSslFactory, Map<String, ?> headers, byte[] body) {
        return sendRequest("POST", uri, useCustomSslFactory, null, headers, body);
    }

    public CompletableFuture<byte[]> post(URI uri, Proxy proxy, boolean useCustomSslFactory, Map<String, ?> headers, byte[] body) {
        return sendRequest("POST", uri, useCustomSslFactory, proxy, headers, body);
    }

    private CompletableFuture<byte[]> sendRequest(String method, URI uri, boolean useCustomSslFactory, Proxy proxy, Map<String, ?> headers, byte[] body) {
        var future = new CompletableFuture<byte[]>();
        var cause = Exceptions.current("Request failed");
        Thread.startVirtualThread(() -> {
            HttpURLConnection connection = null;
            var disconnected = false;
            try {
                var url = uri.toURL();
                connection = (HttpURLConnection) createConnection(proxy, url);
                connection.setRequestMethod(method);
                if(headers != null) {
                    for (var entry : headers.entrySet()) {
                        connection.setRequestProperty(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
                // connection.setRequestProperty("Connection", "close");
                if(connection instanceof HttpsURLConnection httpsConnection && useCustomSslFactory) {
                    httpsConnection.setSSLSocketFactory(sslFactory);
                }
                // connection.setInstanceFollowRedirects(true);
                if(body != null) {
                    connection.setDoOutput(true);
                    try(var outputStream = connection.getOutputStream()) {
                        outputStream.write(body);
                        outputStream.flush();
                    }
                }
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    future.completeExceptionally(new IllegalStateException("Invalid status code: " + connection.getResponseCode()));
                    return;
                }

                byte[] result;
                try (var inputStream = connection.getInputStream()) {
                    result = inputStream.readAllBytes();
                    connection.disconnect();
                    disconnected = true;
                }

                future.complete(result);
            } catch (Throwable exception) {
                if(connection != null && !disconnected) {
                    connection.disconnect();
                }

                cause.addSuppressed(exception);
                future.completeExceptionally(cause);
            }
        });
        return future;
    }

    private SSLSocketFactory createSocketFactory(boolean ios) {
        try {
            var protocol = "TLSv1." + (random.nextBoolean() ? 3 : 2);
            System.out.println("TLS version: " + protocol);
            var sslContext = SSLContext.getInstance(protocol);
            sslContext.init(null, null, new SecureRandom());
            var sslParameters = sslContext.getDefaultSSLParameters();
            var suite = ios ? IOS_CIPHER_SUITE : ANDROID_CIPHER_SUITE;
            var randomSuite = Arrays.copyOf(suite, ThreadLocalRandom.current().nextInt(3, suite.length));
            System.out.println("TLS Suite: " + Arrays.toString(randomSuite));
            sslParameters.setCipherSuites(randomSuite);
            var packetSize = sslParameters.getMaximumPacketSize();
            sslParameters.setMaximumPacketSize(random.nextInt(packetSize / 3, (int) (packetSize * 1.5)));
            System.out.println("TLS Packet size: " + sslParameters.getMaximumPacketSize());
            var allowedNamedGroups = Arrays.copyOf(ALLOWED_NAMED_GROUPS, ThreadLocalRandom.current().nextInt(3, ALLOWED_NAMED_GROUPS.length));
            System.out.println("TLS named groups: " + Arrays.toString(allowedNamedGroups));
            sslParameters.setNamedGroups(allowedNamedGroups);
            var followOrder = random.nextBoolean();
            System.out.println("TLS follow order " + followOrder);
            sslParameters.setUseCipherSuitesOrder(followOrder);
            return new ProxySSLFactory(sslContext.getSocketFactory(), sslParameters);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    private URLConnection createConnection(Proxy proxy, URL url) throws IOException {
        return proxy == null ? url.openConnection() : url.openConnection(proxy);
    }
}

package it.auties.whatsapp.registration.http;

import it.auties.whatsapp.util.Exceptions;
import it.auties.whatsapp.util.SocketWithProxy;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class HttpClient {
    public static final int MAX_TRIES = 3;

    static {
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
    }

    private final SecureRandom random;
    private final AtomicReference<SSLSocketFactory> sslFactory;

    public HttpClient() {
        this.random = new SecureRandom();
        this.sslFactory = new AtomicReference<>();
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

    public CompletableFuture<byte[]> get(URI uri, boolean useCustomSslFactory, URI proxy) {
        return sendRequest("GET", uri, useCustomSslFactory, proxy, null, null);
    }

    public CompletableFuture<String> get(URI uri, boolean useCustomSslFactory, Map<String, ?> headers) {
        return sendRequest("GET", uri, useCustomSslFactory, null, headers, null)
                .thenApplyAsync(String::new);
    }

    public CompletableFuture<String> get(URI uri, URI proxy, boolean useCustomSslFactory, Map<String, ?> headers) {
        return sendRequest("GET", uri, useCustomSslFactory, proxy, headers, null)
                .thenApplyAsync(String::new);
    }

    public CompletableFuture<byte[]> post(URI uri, boolean useCustomSslFactory, Map<String, ?> headers) {
        return sendRequest("POST", uri, useCustomSslFactory, null, headers, null);
    }

    public CompletableFuture<byte[]> post(URI uri, URI proxy, boolean useCustomSslFactory, Map<String, ?> headers) {
        return sendRequest("POST", uri, useCustomSslFactory, proxy, headers, null);
    }

    public CompletableFuture<byte[]> post(URI uri, boolean useCustomSslFactory, Map<String, ?> headers, byte[] body) {
        return sendRequest("POST", uri, useCustomSslFactory, null, headers, body);
    }

    public CompletableFuture<byte[]> post(URI uri, URI proxy, boolean useCustomSslFactory, Map<String, ?> headers, byte[] body) {
        return sendRequest("POST", uri, useCustomSslFactory, proxy, headers, body);
    }

    private CompletableFuture<byte[]> sendRequest(String method, URI uri, boolean useCustomSslFactory, URI proxy, Map<String, ?> headers, byte[] body) {
        var future = new CompletableFuture<byte[]>();
        var cause = Exceptions.current("%s %s failed (%s tries)".formatted(method, uri, MAX_TRIES));
        Thread.startVirtualThread(() -> {
                for (var i = 0; i < MAX_TRIES; i++) {
                    HttpURLConnection connection = null;
                    var disconnected = false;
                    try {
                        var url = uri.toURL();
                        connection = createConnection(proxy, url);
                        connection.setRequestMethod(method);
                        if (headers != null) {
                            for (var entry : headers.entrySet()) {
                                connection.setRequestProperty(entry.getKey(), String.valueOf(entry.getValue()));
                            }
                        }
                        if (connection instanceof HttpsURLConnection httpsConnection && useCustomSslFactory) {
                            httpsConnection.setSSLSocketFactory(sslFactory.updateAndGet(value -> Objects.requireNonNullElseGet(value, this::createSocketFactory)));
                        }
                        if (body != null) {
                            connection.setDoOutput(true);
                            try (var outputStream = connection.getOutputStream()) {
                                outputStream.write(body);
                                outputStream.flush();
                            }
                        }
                        connection.setInstanceFollowRedirects(true);
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
                        break;
                    } catch (Throwable exception) {
                        if (exception instanceof SSLHandshakeException) {
                            sslFactory.set(createSocketFactory());
                        }

                        if (connection != null && !disconnected) {
                            connection.disconnect();
                        }

                        cause.addSuppressed(exception);
                        future.completeExceptionally(cause);
                    }
                }
        });
        return future;
    }

    private SSLSocketFactory createSocketFactory() {
        try {
            var sslContext = SSLContext.getInstance("TLSv1." + (random.nextBoolean() ? 3 : 2));
            sslContext.init(null, null, new SecureRandom());
            var sslParameters = sslContext.getDefaultSSLParameters();
            var supportedCiphers = Arrays.stream(sslContext.getDefaultSSLParameters().getCipherSuites())
                    .filter(entry -> random.nextBoolean())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), result -> {
                        Collections.shuffle(result, random);
                        return result;
                    }))
                    .toArray(String[]::new);
            sslParameters.setCipherSuites(supportedCiphers);
            var supportedNamedGroups = Arrays.stream(sslContext.getDefaultSSLParameters().getNamedGroups())
                    .filter(entry -> random.nextBoolean())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), result -> {
                        Collections.shuffle(result, random);
                        return result;
                    }))
                    .toArray(String[]::new);
            sslParameters.setNamedGroups(supportedNamedGroups);
            var packetSize = sslParameters.getMaximumPacketSize();
            sslParameters.setMaximumPacketSize(random.nextInt(packetSize / 2, packetSize * 2));
            sslParameters.setUseCipherSuitesOrder(random.nextBoolean());
            return new ProxySSLFactory(sslContext.getSocketFactory(), sslParameters);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    private HttpURLConnection createConnection(URI proxy, URL url) throws IOException {
        if (proxy == null) {
            return (HttpURLConnection) url.openConnection();
        }

        var connection = (HttpURLConnection) url.openConnection(SocketWithProxy.toProxy(proxy));
        connection.setAuthenticator(SocketWithProxy.toAuthenticator(proxy));
        return connection;
    }
}

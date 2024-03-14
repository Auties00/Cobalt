package it.auties.whatsapp.registration.http;

import it.auties.whatsapp.util.ProxyAuthenticator;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class HttpClient {
    static {
        ProxyAuthenticator.allowAll();
        Authenticator.setDefault(ProxyAuthenticator.globalAuthenticator());
    }

    public static String toFormParams(Map<String, ?> values) {
        return values.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private volatile ProxySSLFactory factoryWithParams;

    public CompletableFuture<String> get(URI uri, Map<String, ?> headers) {
        return sendRequest("GET", uri, null, headers, null)
                .thenApplyAsync(String::new);
    }

    public CompletableFuture<String> get(URI uri, Proxy proxy, Map<String, ?> headers) {
        return sendRequest("GET", uri, proxy, headers, null)
                .thenApplyAsync(String::new);
    }

    public CompletableFuture<byte[]> post(URI uri, Map<String, ?> headers) {
        return sendRequest("POST", uri, null, headers, null);
    }

    public CompletableFuture<byte[]> post(URI uri, Proxy proxy, Map<String, ?> headers) {
        return sendRequest("POST", uri, proxy, headers, null);
    }

    public CompletableFuture<byte[]> post(URI uri, Map<String, ?> headers, byte[] body) {
        return sendRequest("POST", uri, null, headers, body);
    }

    public CompletableFuture<byte[]> post(URI uri, Proxy proxy, Map<String, ?> headers, byte[] body) {
        return sendRequest("POST", uri, proxy, headers, body);
    }

    private CompletableFuture<byte[]> sendRequest(String method, URI uri, Proxy proxy, Map<String, ?> headers, byte[] body) {
        var future = new CompletableFuture<byte[]>();
        Thread.startVirtualThread(() -> {
            try {
                var url = uri.toURL();
                var connection = (HttpsURLConnection) createConnection(proxy, url);
                connection.setRequestMethod(method);
                headers.forEach((key, value) -> connection.setRequestProperty(key, String.valueOf(value)));
                connection.setSSLSocketFactory(getOrCreateParams());
                connection.setInstanceFollowRedirects(true);
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

                try (var inputStream = connection.getInputStream()) {
                    future.complete(inputStream.readAllBytes());
                }
                connection.disconnect();
            } catch (Throwable exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    private SSLSocketFactory getOrCreateParams() {
        try {
            if(factoryWithParams != null) {
                return factoryWithParams;
            }

            var sslContext = SSLContext.getInstance("TLSv1." + (ThreadLocalRandom.current().nextBoolean() ? 2 : 3));
            sslContext.init(null, null, new SecureRandom());
            var sslParameters = sslContext.getDefaultSSLParameters();
            var supportedCiphers = Arrays.stream(sslParameters.getCipherSuites())
                    .filter(entry -> ThreadLocalRandom.current().nextBoolean())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), result -> {
                        Collections.shuffle(result);
                        return result;
                    }))
                    .toArray(String[]::new);
            sslParameters.setCipherSuites(supportedCiphers);
            return factoryWithParams = new ProxySSLFactory(sslContext.getSocketFactory(), sslParameters);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    private URLConnection createConnection(Proxy proxy, URL url) throws IOException {
        return proxy == null ? url.openConnection() : url.openConnection(proxy);
    }
}

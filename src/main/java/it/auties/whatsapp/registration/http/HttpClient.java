package it.auties.whatsapp.registration.http;

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
import java.util.stream.Collectors;

public class HttpClient {
    private static final String[] IOS_CIPHER_SUITE = {
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

    public static Map<String, String> parseFormParams(String params) {
        return Arrays.stream(params.split("&"))
                .map(entry -> entry.split("=", 2))
                .collect(Collectors.toUnmodifiableMap(entry -> entry[0], entry -> entry[1]));
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

            var sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, null, new SecureRandom());
            var sslParameters = sslContext.getDefaultSSLParameters();
            sslParameters.setCipherSuites(IOS_CIPHER_SUITE);
            sslParameters.setUseCipherSuitesOrder(true);
            return factoryWithParams = new ProxySSLFactory(sslContext.getSocketFactory(), sslParameters);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    private URLConnection createConnection(Proxy proxy, URL url) throws IOException {
        return proxy == null ? url.openConnection() : url.openConnection(proxy);
    }
}

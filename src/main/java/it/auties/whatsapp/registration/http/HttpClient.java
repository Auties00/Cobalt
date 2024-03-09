package it.auties.whatsapp.registration.http;

import it.auties.whatsapp.util.ProxyAuthenticator;

import javax.net.ssl.*;
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
    }

    private final ProxyAuthenticator authenticator;
    private volatile ProxySSLFactory factoryWithParams;
    public HttpClient() {
        this.authenticator = ProxyAuthenticator.globalAuthenticator();
    }

    public CompletableFuture<String> get(URI uri, Proxy proxy, Map<String, ?> headers) {
        return sendRequest("GET", uri, proxy, headers);
    }

    public CompletableFuture<String> post(URI uri, Proxy proxy, Map<String, ?> headers) {
        return sendRequest("POST", uri, proxy, headers);
    }

    private CompletableFuture<String> sendRequest(String method, URI uri, Proxy proxy, Map<String, ?> headers) {
        var future = new CompletableFuture<String>();
        Thread.startVirtualThread(() -> {
            try {
                var url = uri.toURL();
                var connection = (HttpsURLConnection) createConnection(proxy, url);
                connection.setRequestMethod(method);
                headers.forEach((key, value) -> connection.setRequestProperty(key, String.valueOf(value)));
                connection.setAuthenticator(authenticator);
                connection.setSSLSocketFactory(getOrCreateParams());
                connection.setInstanceFollowRedirects(true);
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("Invalid status code: " + connection.getResponseCode());
                }

                try (var inputStream = connection.getInputStream()) {
                    future.complete(new String(inputStream.readAllBytes()));
                }
                connection.disconnect();
            } catch (IOException exception) {
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

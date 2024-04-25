package it.auties.whatsapp.net;

import it.auties.whatsapp.util.Proxies;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.http2.HttpVersionPolicy;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HttpClient implements AutoCloseable {
    final CloseableHttpClient httpClient;
    public HttpClient() {
        this(null);
    }

    public HttpClient(URI proxy) {
        var connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultTlsConfig(createSocketFactory());
        this.httpClient = HttpClients.custom()
                .setProxy(proxy == null ? null : HttpHost.create(proxy))
                .setConnectionManager(connectionManager)
                .setDefaultCredentialsProvider(proxy == null ? null : createCredentialsProvider(proxy))
                .setConnectionReuseStrategy((request, response, context) -> false)
                .disableCookieManagement()
                .build();
    }

    private BasicCredentialsProvider createCredentialsProvider(URI proxy) {
        var credentials = Proxies.parseUserInfo(proxy.getUserInfo());
        if(credentials == null) {
            return null;
        }

        var provider = new BasicCredentialsProvider();
        var authScope = new AuthScope(null, -1);
        var apacheCredentials = new UsernamePasswordCredentials(credentials.username(), credentials.password().toCharArray());
        provider.setCredentials(authScope, apacheCredentials);
        return provider;
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

    public CompletableFuture<byte[]> getRaw(URI uri) {
        return sendRequest("GET", uri, null, null);
    }

    public CompletableFuture<byte[]> getRaw(URI uri, Map<String, ?> headers) {
        return sendRequest("GET", uri, headers, null);
    }

    public CompletableFuture<String> getString(URI uri) {
        return sendRequest("GET", uri, null, null)
                .thenApplyAsync(String::new);
    }

    public CompletableFuture<String> getString(URI uri, Map<String, ?> headers) {
        return sendRequest("GET", uri, headers, null)
                .thenApplyAsync(String::new);
    }

    public CompletableFuture<byte[]> postRaw(URI uri, Map<String, ?> headers) {
        return sendRequest("POST", uri, headers, null);
    }

    public CompletableFuture<byte[]> postRaw(URI uri, Map<String, ?> headers, byte[] body) {
        return sendRequest("POST", uri, headers, body);
    }

    private CompletableFuture<byte[]> sendRequest(String method, URI uri, Map<String, ?> headers, byte[] body) {
        return CompletableFuture.supplyAsync(() -> sendRequestImpl(method, uri, headers, body, false), Thread::startVirtualThread);
    }

    private byte[] sendRequestImpl(String method, URI uri, Map<String, ?> headers, byte[] body, boolean isRetry) {
        try {
            var request = new BasicClassicHttpRequest(method, uri);
            if(headers != null) {
                headers.forEach(request::setHeader);
            }

            if(body != null) {
                var contentType = Objects.requireNonNull(headers == null ? null : headers.get("Content-Type"), "Missing Content-Type header");
                request.setEntity(HttpEntities.create(body, ContentType.parse(String.valueOf(contentType))));
            }

            return httpClient.execute(request, data -> data.getEntity().getContent().readAllBytes());
        }catch (Throwable throwable) {
            if(!isRetry) {
                return sendRequestImpl(method, uri, headers, body, true);
            }

            throw new RuntimeException("%s request to %s failed".formatted(method, uri), throwable);
        }
    }

    private TlsConfig createSocketFactory() {
        try {
            var random = new SecureRandom();
            var tlsVersion = random.nextBoolean() ? TLS.V_1_3 : TLS.V_1_2;
            var sslContext = SSLContext.getInstance("TLSv1." + tlsVersion.getVersion().getMinor());
            sslContext.init(null, null, new SecureRandom());
            var supportedCiphers = Arrays.stream(sslContext.getDefaultSSLParameters().getCipherSuites())
                    .filter(entry -> random.nextBoolean())
                    .collect(Collectors.collectingAndThen(Collectors.toList(), result -> {
                        Collections.shuffle(result, random);
                        return result;
                    }))
                    .toArray(String[]::new);
            return new TlsConfig.Builder()
                    .setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_1)
                    .setSupportedCipherSuites(supportedCiphers)
                    .setSupportedProtocols(tlsVersion)
                    .build();
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

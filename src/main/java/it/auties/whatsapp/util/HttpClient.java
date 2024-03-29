package it.auties.whatsapp.util;

import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.protocol.HttpContext;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.Authenticator;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

// We use Apache's HTTP client under the hood as it's been tested to have the highest throughput
public class HttpClient implements AutoCloseable {
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

    static {
        ProxyAuthenticator.allowAll();
        Authenticator.setDefault(ProxyAuthenticator.globalAuthenticator());
    }

    private final CloseableHttpClient delegate;
    public HttpClient(Proxy proxy, PlatformType platform) {
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(createSSLConnectionSocketFactory(proxy, platform))
                .build();
        this.delegate = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    private static SSLConnectionSocketFactory createSSLConnectionSocketFactory(Proxy proxy, PlatformType platform) {
        try {
            var sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, null, new SecureRandom());
            var sslParameters = sslContext.getDefaultSSLParameters();
            switch (platform) {
                case ANDROID, ANDROID_BUSINESS -> sslParameters.setCipherSuites(ANDROID_CIPHER_SUITE);
                case IOS, IOS_BUSINESS -> sslParameters.setCipherSuites(IOS_CIPHER_SUITE);
                default -> {}
            }
            sslParameters.setUseCipherSuitesOrder(true);
            return new SSLConnectionSocketFactory(sslContext, HttpsSupport.getDefaultHostnameVerifier()) {
                @Override
                public Socket createSocket(HttpContext context) {
                    return new Socket(proxy);
                }

                @Override
                public Socket createSocket(Proxy ignored, HttpContext context) {
                    return new Socket(proxy);
                }
            };
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException(exception);
        }
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

    public CompletableFuture<String> get(URI uri, Map<String, ?> headers) {
        return sendRequest(new HttpGet(uri), headers, null)
                .thenApplyAsync(String::new);
    }

    public CompletableFuture<byte[]> get(URI uri) {
        return sendRequest(new HttpGet(uri), null, null);
    }

    public CompletableFuture<byte[]> post(URI uri, Map<String, ?> headers) {
        return sendRequest(new HttpPost(uri), headers, null);
    }

    public CompletableFuture<byte[]> post(URI uri, Map<String, ?> headers, String body) {
        return sendRequest(new HttpPost(uri), headers, HttpEntities.create(body));
    }

    public CompletableFuture<byte[]> post(URI uri, Map<String, ?> headers, byte[] body) {
        var contentType = headers.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("content-type"))
                .findFirst()
                .map(entry -> String.valueOf(entry.getValue()))
                .orElseThrow(() -> new NoSuchElementException("Missing Content-Type header"));
        var entity = HttpEntities.create(body, ContentType.create(contentType));
        return sendRequest(new HttpPost(uri), headers, entity);
    }

    private CompletableFuture<byte[]> sendRequest(HttpUriRequestBase request, Map<String, ?> headers, HttpEntity body) {
        var future = new CompletableFuture<byte[]>();
        Thread.startVirtualThread(() -> {
            try {
                if(headers != null) {
                    headers.forEach(request::setHeader);
                }

                if(body != null) {
                    request.setEntity(body);
                }
                
                var response = delegate.execute(request, new AbstractHttpClientResponseHandler<byte[]>() {
                    @Override
                    public byte[] handleEntity(HttpEntity httpEntity) throws IOException {
                        return httpEntity.getContent().readAllBytes();
                    }
                });
                future.complete(response);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }
}

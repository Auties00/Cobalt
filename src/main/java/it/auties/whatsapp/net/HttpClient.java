package it.auties.whatsapp.net;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HttpClient implements AutoCloseable {
    static {
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
    }

    private static final int MAX_TRIES = 5;
    private static final String PROXY_KEY = "proxy";
    private static final String SSL_PARAMS_KEY = "ssl.params";

    final CloseableHttpClient httpClient;
    final Platform platform;
    final URI proxy;
    public HttpClient(Platform platform) {
        this(platform, null);
    }

    public HttpClient(Platform platform, URI proxy) {
        this.platform = platform;
        this.proxy = proxy;
        var socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register(URIScheme.HTTP.id, new HttpConnectionFactory())
                .register(URIScheme.HTTPS.id, new HttpsConnectionFactory(platform))
                .build();
        var connectionManager = new PoolingHttpClientConnectionManager(
               socketFactoryRegistry,
                PoolConcurrencyPolicy.STRICT,
                PoolReusePolicy.LIFO,
                TimeValue.NEG_ONE_MILLISECOND,
                null,
                proxy == null ? null : DummyDnsResolver.instance(),
                null
        );
        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setConnectionReuseStrategy((request, response, context) -> false)
                .disableCookieManagement()
                .build();
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
        return sendRequest("GET", uri, null, null, true);
    }

    public CompletableFuture<byte[]> getRaw(URI uri, Map<String, ?> headers) {
        return sendRequest("GET", uri, headers, null, true);
    }

    public CompletableFuture<String> getString(URI uri) {
        return sendRequest("GET", uri, null, null, true)
                .thenApplyAsync(String::new);
    }

    public CompletableFuture<String> getString(URI uri, Map<String, ?> headers) {
        return sendRequest("GET", uri, headers, null, true)
                .thenApplyAsync(String::new);
    }

    public CompletableFuture<byte[]> postRaw(URI uri, Map<String, ?> headers, byte[] body) {
        return sendRequest("POST", uri, headers, body, true);
    }

    public CompletableFuture<byte[]> postRawWithoutSslParams(URI uri, Map<String, ?> headers, byte[] body) {
        return sendRequest("POST", uri, headers, body, false);
    }

    private CompletableFuture<byte[]> sendRequest(String method, URI uri, Map<String, ?> headers, byte[] body, boolean useSslParams) {
        return CompletableFuture.supplyAsync(() -> sendRequestImpl(method, uri, headers, body, useSslParams), Thread::startVirtualThread);
    }

    private byte[] sendRequestImpl(String method, URI uri, Map<String, ?> headers, byte[] body, boolean useSslParams) {
        Throwable lastError = null;
        for(var i = 0; i < MAX_TRIES; i++) {
            try {
                var request = new BasicClassicHttpRequest(method, uri);
                if(headers != null) {
                    headers.forEach(request::setHeader);
                }

                if(body != null) {
                    var contentType = Objects.requireNonNull(headers == null ? null : headers.get("Content-Type"), "Missing Content-Type header");
                    request.setEntity(HttpEntities.create(body, ContentType.parse(String.valueOf(contentType))));
                }

                var context = HttpCoreContext.create();
                context.setAttribute(SSL_PARAMS_KEY, useSslParams);
                context.setAttribute(PROXY_KEY, proxy);
                return httpClient.execute(request, context, data -> data.getEntity().getContent().readAllBytes());
            }catch (Throwable throwable) {
                lastError = throwable;
            }
        }

        throw new RuntimeException("%s request to %s failed(%s)".formatted(method, uri, lastError.getMessage()), lastError);
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class HttpConnectionFactory extends PlainConnectionSocketFactory {
        @Override
        public Socket createSocket(final HttpContext context) throws IOException {
            return createSocket(null, context);
        }

        @Override
        public Socket createSocket(Proxy proxy, HttpContext context) throws IOException {
            var derivedProxy = (URI) context.getAttribute(PROXY_KEY);
            return SocketClient.newPlainClient(derivedProxy);
        }

        @Override
        public Socket connectSocket(TimeValue connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
            return super.connectSocket(
                    connectTimeout,
                    socket,
                    host,
                    context.getAttribute(PROXY_KEY) == null ? remoteAddress : InetSocketAddress.createUnresolved(host.getHostName(), host.getPort()),
                    localAddress,
                    context
            );
        }

        @Override
        public Socket connectSocket(Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, Timeout connectTimeout, Object attachment, HttpContext context) throws IOException {
            return super.connectSocket(
                    socket,
                    host,
                    context.getAttribute(PROXY_KEY) == null ? remoteAddress : InetSocketAddress.createUnresolved(host.getHostName(), host.getPort()),
                    localAddress,
                    connectTimeout,
                    attachment,
                    context
            );
        }
    }

    private static class HttpsConnectionFactory implements LayeredConnectionSocketFactory {
        private static final String[] IOS_CIPHERS = {
                "TLS_AES_128_GCM_SHA256",
                "TLS_CHACHA20_POLY1305_SHA256",
                "TLS_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_RSA_WITH_AES_128_CBC_SHA",
                "TLS_RSA_WITH_AES_256_CBC_SHA",
                "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"
        };
        private static final String[] ANDROID_CIPHERS = {
                "TLS_AES_128_GCM_SHA256"
                //,"use default"
        };

        private final SSLContext sslContext;
        private final SSLParameters sslParameters;
        private HttpsConnectionFactory(Platform platform) {
            try {
                switch (platform) {
                    case IOS -> {
                        var sslContext = SSLContext.getInstance("TLSv1.3");
                        sslContext.init(null, null, new SecureRandom());
                        this.sslParameters = sslContext.getDefaultSSLParameters();
                        sslParameters.setCipherSuites(IOS_CIPHERS);
                        sslParameters.setUseCipherSuitesOrder(true);
                        this.sslContext = sslContext;
                    }
                    case ANDROID -> {
                        var sslContext = SSLContext.getInstance("TLSv1.3");
                        sslContext.init(null, null, new SecureRandom());
                        this.sslParameters = sslContext.getDefaultSSLParameters();
                        sslParameters.setCipherSuites(ANDROID_CIPHERS);
                        this.sslContext = sslContext;
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + platform);
                }
            }catch (GeneralSecurityException exception) {
                throw new RuntimeException(exception);
            }
        }

        @Override
        public Socket createSocket(final HttpContext context) throws IOException {
            return createSocket(null, context);
        }

        @Override
        public Socket createSocket(Proxy proxy, HttpContext context) throws IOException {
            var derivedProxy = (URI) context.getAttribute(PROXY_KEY);
            return SocketClient.newPlainClient(derivedProxy);
        }

        @Override
        public Socket connectSocket(TimeValue connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
            socket.connect(
                    context.getAttribute(PROXY_KEY) == null ? remoteAddress : InetSocketAddress.createUnresolved(host.getHostName(), host.getPort()),
                    connectTimeout.toMillisecondsIntBound()
            );
            return createLayeredSocket(socket, host.getHostName(), host.getPort(), context);
        }

        @Override
        public Socket connectSocket(Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, Timeout connectTimeout, Object attachment, HttpContext context) throws IOException {
            socket.connect(
                    context.getAttribute(PROXY_KEY) == null ? remoteAddress : InetSocketAddress.createUnresolved(host.getHostName(), host.getPort()),
                    connectTimeout.toMillisecondsIntBound()
            );
            return createLayeredSocket(socket, host.getHostName(), host.getPort(), context);
        }

        @Override
        public Socket createLayeredSocket(Socket socket, String target, int port, HttpContext context) {
            return createLayeredSocket(socket, target, port, null, context);
        }

        @Override
        public Socket createLayeredSocket(Socket socket, String target, int port, Object attachment, HttpContext context) {
            var asyncSocket = (SocketClient) socket;
            var useSslParams = (boolean) context.getAttribute(SSL_PARAMS_KEY);
            var sslEngine = sslContext.createSSLEngine(target, port);
            if(useSslParams) {
                sslEngine.setSSLParameters(sslParameters);
            }
            sslEngine.setUseClientMode(true);
            asyncSocket.upgradeToSsl(sslEngine)
                    .join(); // Await async handshake
            return asyncSocket;
        }
    }

    private static class DummyDnsResolver implements DnsResolver {
        private static final DummyDnsResolver INSTANCE = new DummyDnsResolver();
        private static DummyDnsResolver instance() {
            return INSTANCE;
        }

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            return new InetAddress[] {
                    InetAddress.getByAddress(new byte[] { 1, 1, 1, 1 })
            };
        }

        @Override
        public String resolveCanonicalHostname(String host) {
            return null;
        }
    }

    public enum Platform {
        IOS,
        ANDROID
    }
}

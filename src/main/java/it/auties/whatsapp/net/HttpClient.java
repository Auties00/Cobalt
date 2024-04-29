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
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.*;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HttpClient implements AutoCloseable {
    static {
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
    }

    private static final String PROXY_KEY = "proxy";
    private static final String SSL_PARAMS_KEY = "ssl.params";

    final CloseableHttpClient httpClient;
    final HttpsConnectionFactory httpsConnectionFactory;
    final URI proxy;
    public HttpClient() {
        this(null);
    }

    public HttpClient(URI proxy) {
        this.httpsConnectionFactory = new HttpsConnectionFactory();
        var socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new HttpConnectionFactory())
                .register("https", httpsConnectionFactory)
                .build();
        var connectionManager = new PoolingHttpClientConnectionManager(
                socketFactoryRegistry,
                PoolConcurrencyPolicy.STRICT,
                PoolReusePolicy.LIFO,
                TimeValue.NEG_ONE_MILLISECOND,
                null,
                new DummyDnsResolver(),
                null
        );
        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setConnectionReuseStrategy((request, response, context) -> false)
                .disableCookieManagement()
                .build();
        this.proxy = proxy;
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
        return CompletableFuture.supplyAsync(() -> sendRequestImpl(method, uri, headers, body, useSslParams, false), Thread::startVirtualThread);
    }

    private byte[] sendRequestImpl(String method, URI uri, Map<String, ?> headers, byte[] body, boolean useSslParams, boolean isRetry) {
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
            if(!isRetry) {
                if(throwable instanceof SSLHandshakeException && useSslParams) {
                    httpsConnectionFactory.rotateSSL();
                }

                return sendRequestImpl(method, uri, headers, body, useSslParams, true);
            }

            throw new RuntimeException("%s request to %s failed(%s)".formatted(method, uri, throwable.getMessage()), throwable);
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

    private static class HttpConnectionFactory extends PlainConnectionSocketFactory {
        @Override
        public Socket createSocket(final HttpContext context) throws IOException {
            return createSocket(null, context);
        }

        @Override
        public Socket createSocket(Proxy proxy, HttpContext context) throws IOException {
            var derivedProxy = (URI) context.getAttribute(PROXY_KEY);
            return it.auties.whatsapp.net.Socket.of(derivedProxy);
        }

        @Override
        public Socket connectSocket(TimeValue connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
            var unresolvedRemote = InetSocketAddress.createUnresolved(host.getHostName(), remoteAddress.getPort());
            return super.connectSocket(connectTimeout, socket, host, unresolvedRemote, localAddress, context);
        }

        @Override
        public Socket connectSocket(Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, Timeout connectTimeout, Object attachment, HttpContext context) throws IOException {
            var unresolvedRemote = InetSocketAddress.createUnresolved(host.getHostName(), remoteAddress.getPort());
            return super.connectSocket(socket, host, unresolvedRemote, localAddress, connectTimeout, attachment, context);
        }
    }

    private static class HttpsConnectionFactory implements LayeredConnectionSocketFactory {
        private SSLContext sslContext;
        private SSLParameters sslParameters;
        private HttpsConnectionFactory() {
            rotateSSL();
        }

        private void rotateSSL() {
            try {
                var random = new SecureRandom();
                var sslContext = SSLContext.getInstance("TLSv1.3");
                sslContext.init(null, null, new SecureRandom());
                this.sslParameters = sslContext.getDefaultSSLParameters();
                sslParameters.setCipherSuites(Arrays.stream(sslContext.getDefaultSSLParameters().getCipherSuites())
                        .filter(entry -> random.nextBoolean())
                        .collect(Collectors.collectingAndThen(Collectors.toList(), result -> { Collections.shuffle(result, random); return result; }))
                        .toArray(String[]::new));
                sslParameters.setUseCipherSuitesOrder(true);
                sslParameters.setNamedGroups(Arrays.stream(sslContext.getDefaultSSLParameters().getNamedGroups())
                        .filter(entry -> random.nextBoolean())
                        .collect(Collectors.collectingAndThen(Collectors.toList(), result -> { Collections.shuffle(result, random); return result; }))
                        .toArray(String[]::new));
                var packetSize = sslParameters.getMaximumPacketSize();
                sslParameters.setMaximumPacketSize(random.nextInt(packetSize / 5, packetSize * 5));
                this.sslContext = sslContext;
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
            return it.auties.whatsapp.net.Socket.of(derivedProxy);
        }

        @Override
        public Socket connectSocket(TimeValue connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
            socket.connect(InetSocketAddress.createUnresolved(host.getHostName(), remoteAddress.getPort()), connectTimeout.toMillisecondsIntBound());
            return createLayeredSocket(socket, host.getHostName(), host.getPort(), context);
        }

        @Override
        public Socket connectSocket(Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, Timeout connectTimeout, Object attachment, HttpContext context) throws IOException {
            socket.connect(InetSocketAddress.createUnresolved(host.getHostName(), remoteAddress.getPort()), connectTimeout.toMillisecondsIntBound());
            return createLayeredSocket(socket, host.getHostName(), host.getPort(), context);
        }

        @Override
        public Socket createLayeredSocket(Socket socket, String target, int port, HttpContext context) throws IOException {
            return createLayeredSocket(socket, target, port, null, context);
        }

        @Override
        public Socket createLayeredSocket(Socket socket, String target, int port, Object attachment, HttpContext context) throws IOException {
            var sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(
                    socket,
                    target,
                    port,
                    true
            );
            var useSslParams = (boolean) context.getAttribute(SSL_PARAMS_KEY);
            if(useSslParams) {
                sslSocket.setSSLParameters(sslParameters);
            }
            sslSocket.setReuseAddress(false);
            sslSocket.setKeepAlive(true);
            sslSocket.startHandshake();
            return sslSocket;
        }
    }

    private static class DummyDnsResolver implements DnsResolver {
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
}

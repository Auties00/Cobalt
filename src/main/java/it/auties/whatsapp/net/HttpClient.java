package it.auties.whatsapp.net;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class HttpClient implements AutoCloseable {
    private static final int REQUEST_TIMEOUT = 300;
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
    public static final byte[] EMPTY_BUFFER = new byte[0];
    private static final byte[] HTTP_MESSAGE_END_BYTES = "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);

    private final Platform platform;
    private final URI proxy;
    private final ConcurrentMap<String, SocketClient> aliveSockets;
    public HttpClient(Platform platform) {
        this(platform, null);
    }

    public HttpClient(Platform platform, boolean allowKeepAlive) {
        this(platform, null, allowKeepAlive);
    }

    public HttpClient(Platform platform, URI proxy) {
        this(platform, proxy, true);
    }

    public HttpClient(Platform platform, URI proxy, boolean allowKeepAlive) {
        this.proxy = proxy;
        this.platform = platform;
        this.aliveSockets = allowKeepAlive ? new ConcurrentHashMap<>() : null;
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

    public CompletableFuture<byte[]> postRaw(URI uri, Map<String, ?> headers, byte[] body) {
        return sendRequest("POST", uri, headers, body);
    }

    public CompletableFuture<String> postString(URI uri, Map<String, ?> headers, byte[] body) {
        return sendRequest("POST", uri, headers, body)
                .thenApplyAsync(String::new);
    }

    private CompletableFuture<byte[]> sendRequest(String method, URI uri, Map<String, ?> headers, byte[] body) {
        var builder = createRequestPayload(method, uri, headers, body);
        return getLockableSocketClient(uri)
                .thenComposeAsync(socket -> sendRequest(method, uri, headers, body, socket, builder.toString()));
    }

    private CompletableFuture<byte[]> sendRequest(String method, URI uri, Map<String, ?> headers, byte[] body, SocketClient socket, String payload) {
        return socket.writeAsync(StandardCharsets.ISO_8859_1.encode(payload))
                .thenComposeAsync(ignored -> decodeResponse(uri, socket))
                .thenComposeAsync(result -> handleResponse(method, headers, body, result))
                .orTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .exceptionallyComposeAsync(error -> handleRequestError(uri, error, socket));
    }

    private CompletableFuture<byte[]> handleRequestError(URI uri, Throwable error, SocketClient socket) {
        closeSocketSilently(uri, socket);
        return CompletableFuture.failedFuture(error);
    }

    private CompletableFuture<HttpDecoder.Result> decodeResponse(URI uri, SocketClient socket) {
        var decoder = new HttpDecoder(socket.socketTransport);
        return decoder.readResponse(uri);
    }

    private CompletionStage<byte[]> handleResponse(String method, Map<String, ?> headers, byte[] body, HttpDecoder.Result result) {
        return switch (result) {
            case HttpDecoder.Result.Response response -> {
                if (!String.valueOf(response.statusCode()).startsWith("20")) {
                    throw new IllegalArgumentException("Unexpected response status code: " + response.statusCode());
                }

                yield CompletableFuture.completedFuture(response.data());
            }
            case HttpDecoder.Result.Redirect redirect -> sendRequest(method, redirect.to(), headers, body);
        };
    }

    private void closeSocketSilently(URI uri, SocketClient socket) {
        try {
            socket.close();
            if(aliveSockets != null) {
                aliveSockets.remove(uri.getHost() + ":" + uri.getPort(), socket);
            }
        } catch (Throwable ignored) {

        }
    }

    private StringBuilder createRequestPayload(String method, URI uri, Map<String, ?> headers, byte[] body) {
        var builder = new StringBuilder();
        builder.append(method)
                .append(" ")
                .append(uri.getPath())
                .append(uri.getQuery() == null || uri.getQuery().isEmpty() ? "" : "?" + uri.getQuery())
                .append(" HTTP/1.1\r\n");
        builder.append("Host: ")
                .append(uri.getHost())
                .append(uri.getPort() == -1 ? "" : ":" + uri.getPort())
                .append("\r\n");
        if(platform == Platform.DEFAULT && (headers == null || headers.keySet().stream().noneMatch(entry -> entry.equalsIgnoreCase("User-Agent")))) {
            builder.append("User-Agent: Java/%s\r\n".formatted(Runtime.version().feature()));
        }

        if(headers != null) {
            headers.forEach((headerKey, headerValue) -> builder.append(headerKey.trim())
                    .append(": ")
                    .append(headerValue)
                    .append("\r\n"));
        }
        if (body != null) {
            builder.append("Content-Length: ")
                    .append(body.length)
                    .append("\r\n");
        }
        builder.append("\r\n");
        if (body != null) {
            builder.append(new String(body, StandardCharsets.ISO_8859_1))
                    .append("\r\n");
        }
        return builder;
    }

    private InetSocketAddress toSocketAddress(URI uri) {
        var hostname = uri.getHost();
        var port = uri.getPort() != -1 ? uri.getPort() : switch (uri.getScheme().toLowerCase()) {
            case "https" -> 443;
            case "http" -> 80;
            default -> throw new IllegalStateException("Unexpected scheme: " + uri.getScheme().toLowerCase());
        };
        return proxy == null ? new InetSocketAddress(hostname, port) : InetSocketAddress.createUnresolved(hostname, port);
    }

    private CompletableFuture<SocketClient> getLockableSocketClient(URI uri) {
        try {
            var aliveSocket = aliveSockets == null ? null : aliveSockets.get(uri.getHost() + ":" + uri.getPort());
            if(aliveSocket != null && !aliveSocket.isClosed()) {
                return CompletableFuture.completedFuture(aliveSocket);
            }

            var freshSocket = switch (uri.getScheme().toLowerCase()) {
                case "http" -> {
                    var result = SocketClient.newPlainClient(proxy);
                    result.setKeepAlive(true);
                    if(aliveSockets != null) {
                        aliveSockets.put(uri.getHost() + ":" + uri.getPort(), result);
                    }
                    yield result;
                }
                case "https" -> {
                    var sslEngine = platform.sslContext()
                            .createSSLEngine(uri.getHost(), uri.getPort() == 1 ? 443 : uri.getPort());
                    sslEngine.setUseClientMode(true);
                    platform.sslParameters()
                            .ifPresent(sslEngine::setSSLParameters);
                    var result = SocketClient.newSecureClient(sslEngine, proxy);
                    result.setKeepAlive(true);
                    if(aliveSockets != null) {
                        aliveSockets.put(uri.getHost() + ":" + uri.getPort(), result);
                    }
                    yield result;
                }
                default -> throw new IllegalStateException("Unexpected scheme: " + uri.getScheme().toLowerCase());
            };
            return freshSocket.connectAsync(toSocketAddress(uri))
                    .thenApply(ignored -> freshSocket);
        }catch (Throwable exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public enum Platform {
        DEFAULT,
        IOS,
        ANDROID;

        public SSLContext sslContext() {
            return DEFAULT_SSL_CONTEXT;
        }

        private Optional<SSLParameters> sslParameters() {
            return switch (this) {
                case DEFAULT -> Optional.empty();
                case IOS -> {
                    var sslParameters = sslContext().getDefaultSSLParameters();
                    sslParameters.setCipherSuites(IOS_CIPHERS);
                    sslParameters.setUseCipherSuitesOrder(true);
                    yield Optional.of(sslParameters);
                }
                case ANDROID -> {
                    var sslParameters = sslContext().getDefaultSSLParameters();
                    sslParameters.setCipherSuites(ANDROID_CIPHERS);
                    sslParameters.setUseCipherSuitesOrder(true);
                    yield Optional.of(sslParameters);
                }
            };
        }

        private static final SSLContext DEFAULT_SSL_CONTEXT;
        static {
            try {
                var sslContext = SSLContext.getInstance("TLSv1.3");
                sslContext.init(null, null, new SecureRandom());
                DEFAULT_SSL_CONTEXT = sslContext;
            }catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    @Override
    public void close() {
        if(aliveSockets == null) {
            return;
        }

        for(var socket : aliveSockets.values()) {
            try {
                socket.close();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        aliveSockets.clear();
    }
}

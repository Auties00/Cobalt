package it.auties.whatsapp.net;

import it.auties.whatsapp.util.Exceptions;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
    private static final byte[] EMPTY_BUFFER = new byte[0];

    private final Platform platform;
    private final URI proxy;
    private final ConcurrentMap<String, SocketClient> aliveSockets;
    public HttpClient(Platform platform) {
        this(platform, null);
    }

    public HttpClient(Platform platform, URI proxy) {
        this.proxy = proxy;
        this.platform = platform;
        this.aliveSockets = new ConcurrentHashMap<>();
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
        var socket = getLockableSocketClient(uri, useSslParams);
        var builder = createRequestPayload(method, uri, headers, body);
        return (socket.isConnected() ? CompletableFuture.completedFuture(null) : socket.connectAsync(toSocketAddress(uri)))
                .thenComposeAsync(ignored -> socket.writeAsync(builder.toString().getBytes()))
                .thenComposeAsync(ignored -> socket.readAsync(readReceiveBufferSize(socket)))
                .thenComposeAsync(responseBuffer -> parseResponsePayload(method, uri, headers, body, useSslParams, responseBuffer, socket))
                .orTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .exceptionallyComposeAsync(error -> {
                    closeSocketSilently(uri, socket);
                    return CompletableFuture.failedFuture(error);
                });
    }

    private void closeSocketSilently(URI uri, SocketClient socket) {
        try {
            socket.close();
            aliveSockets.remove(uri.getHost() + ":" + uri.getPort(), socket);
        } catch (Throwable ignored) {

        }
    }

    private CompletableFuture<byte[]> parseResponsePayload(String method, URI uri, Map<String, ?> headers, byte[] body, boolean useSslParameters, ByteBuffer responseBuffer, SocketClient socket) {
        var response = StandardCharsets.UTF_8
                .decode(responseBuffer)
                .toString();

        var responseStatusLineEnd = response.indexOf("\n");
        var redirected = isRedirect(response, responseStatusLineEnd);
        var contentLength = -1;
        var keepAlive = false;
        String location = null;
        var lastHeaderLineIndex = responseStatusLineEnd;
        var currentHeaderLineIndex = responseStatusLineEnd;
        while ((currentHeaderLineIndex = response.indexOf("\n", lastHeaderLineIndex + 1)) != -1) {
            var responseLine = response.substring(lastHeaderLineIndex + 1, currentHeaderLineIndex).trim();
            lastHeaderLineIndex = currentHeaderLineIndex;
            if(responseLine.isEmpty()) {
                break;
            }

            var responseLineParts = responseLine.split(": ", 2);
            if(responseLineParts.length != 2) {
                throw new IllegalArgumentException("Malformed response header: " + responseLine);
            }

            var headerKey = responseLineParts[0];
            var headerValue = responseLineParts[1];
            switch (headerKey.toLowerCase()) {
                case "content-length" -> {
                    try {
                        contentLength = Integer.parseUnsignedInt(responseLineParts[1]);
                    }catch (NumberFormatException exception) {
                        throw new IllegalArgumentException("Malformed Content-Length header: " + responseLine);
                    }
                }
                case "connection" -> keepAlive = headerValue.equalsIgnoreCase("keep-alive");
                case "location" -> location = headerValue;
            }
        }

        if(redirected) {
            Objects.requireNonNull(location, "Missing location for redirect status code");
            return sendRequest(method, URI.create(location), headers, body, useSslParameters);
        }

        if(contentLength == -1) {
            return CompletableFuture.completedFuture(EMPTY_BUFFER);
        }

        var partialBody = response.length() <= currentHeaderLineIndex + 1 ? new byte[0] : response.substring(currentHeaderLineIndex + 1).getBytes();
        if(partialBody.length > contentLength) {
            throw new IllegalArgumentException("Actual content length is bigger than reported in the response(expected: %s, got: %s)".formatted(contentLength, partialBody.length));
        }

        if(partialBody.length == contentLength) {
            if(!keepAlive) {
                closeSocketSilently(uri, socket);
            }

            return CompletableFuture.completedFuture(partialBody);
        }

        var killSocket = !keepAlive;
        var remainingLength = contentLength - partialBody.length;
        return socket.readFullyAsync(remainingLength)
                .thenApplyAsync(additionalBody -> concatMessage(additionalBody, partialBody, remainingLength))
                .whenCompleteAsync((result, error) -> {
                    if(killSocket) {
                        closeSocketSilently(uri, socket);
                    }

                    if(error != null) {
                        Exceptions.rethrow(error);
                    }
                });
    }

    private byte[] concatMessage(ByteBuffer additionalBody, byte[] partialBody, int remainingLength) {
        var completeResult = new byte[partialBody.length + additionalBody.remaining()];
        System.arraycopy(partialBody, 0, completeResult, 0, partialBody.length);
        additionalBody.get(completeResult, partialBody.length, remainingLength);
        return completeResult;
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
        if(headers != null) {
            headers.forEach((headerKey, headerValue) -> {
                builder.append(headerKey)
                        .append(": ")
                        .append(headerValue)
                        .append("\r\n");
            });
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

    private boolean isRedirect(String response, int responseStatusLineEnd) {
        var responseStatusLine = responseStatusLineEnd == -1 ? response : response.substring(0, responseStatusLineEnd);
        var responseStatusParts = responseStatusLine.split(" ");
        if (responseStatusParts.length < 2) {
            throw new IllegalArgumentException("Unexpected response status code: " + response);
        }

        var statusCode = responseStatusParts[1];
        if(statusCode.equals("302")) {
            return true;
        }

        if(!statusCode.startsWith("20")) {
            throw new IllegalArgumentException("Unexpected response status code: " + statusCode);
        }

        return false;
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

    private int readReceiveBufferSize(SocketClient client) {
        try {
            return client.getOption(StandardSocketOptions.SO_RCVBUF);
        }catch (IOException exception) {
            return 8192;
        }
    }

    private SocketClient getLockableSocketClient(URI uri, boolean useSslParams) {
        try {
            var aliveSocket = aliveSockets.get(uri.getHost() + ":" + uri.getPort());
            if(aliveSocket != null) {
                return aliveSocket;
            }

            return switch (uri.getScheme().toLowerCase()) {
                case "http" -> {
                    var result = SocketClient.newPlainClient(proxy);
                    result.setKeepAlive(true);
                    aliveSockets.put(uri.getHost() + ":" + uri.getPort(), result);
                    yield result;
                }
                case "https" -> {
                    var sslEngine = platform.sslData()
                            .context()
                            .createSSLEngine(uri.getHost(), uri.getPort() == 1 ? 443 : uri.getPort());
                    sslEngine.setUseClientMode(true);
                    if(useSslParams) {
                        sslEngine.setSSLParameters(platform.sslData().parameters());
                    }
                    var result = SocketClient.newSecureClient(sslEngine, proxy);
                    result.setKeepAlive(true);
                    aliveSockets.put(uri.getHost() + ":" + uri.getPort(), result);
                    yield result;
                }
                default -> throw new IllegalStateException("Unexpected scheme: " + uri.getScheme().toLowerCase());
            };
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public enum Platform {
        IOS(SSLData.of(true)),
        ANDROID(SSLData.of(false));

        private final SSLData sslData;
        Platform(SSLData sslData) {
            this.sslData = sslData;
        }

        private SSLData sslData() {
            return sslData;
        }
    }

    private record SSLData(SSLContext context, SSLParameters parameters) {
        public static SSLData of(boolean ios) {
            try {
                if(ios) {
                    var sslContext = SSLContext.getInstance("TLSv1.3");
                    sslContext.init(null, null, new SecureRandom());
                    var sslParameters = sslContext.getDefaultSSLParameters();
                    sslParameters.setCipherSuites(IOS_CIPHERS);
                    sslParameters.setUseCipherSuitesOrder(true);
                    return new SSLData(sslContext, sslParameters);
                }else {
                    var sslContext = SSLContext.getInstance("TLSv1.3");
                    sslContext.init(null, null, new SecureRandom());
                    var sslParameters = sslContext.getDefaultSSLParameters();
                    sslParameters.setCipherSuites(ANDROID_CIPHERS);
                    return new SSLData(sslContext, sslParameters);
                }
            }catch (GeneralSecurityException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    @Override
    public void close() {
        aliveSockets.forEach((key, socket) -> {
            try {
                socket.close();
            } catch (Throwable ignored) {

            }
        });
    }
}

package it.auties.whatsapp.net;

import it.auties.whatsapp.util.Bytes;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

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
        var socket = getLockableSocketClient(uri);
        var builder = createRequestPayload(method, uri, headers, body);
        System.out.println(builder);
        return (socket.isConnected() ? CompletableFuture.completedFuture(null) : socket.connectAsync(toSocketAddress(uri)))
                .thenComposeAsync(ignored -> socket.writeAsync(StandardCharsets.ISO_8859_1.encode(builder.toString())))
                .thenComposeAsync(ignored -> readResponse(method, uri, headers, body, socket))
                .orTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .exceptionallyComposeAsync(error -> {
                    closeSocketSilently(uri, socket);
                    return CompletableFuture.failedFuture(error);
                });
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

    private void closeSocketSilently(URI uri, PartialResponseInfo info, SocketClient socket) {
        if (aliveSockets == null || info.closeConnection()) {
            closeSocketSilently(uri, socket);
        }
    }

    private CompletableFuture<byte[]> readResponse(String method, URI uri, Map<String, ?> headers, byte[] body, SocketClient socket) {
        var info = new PartialResponseInfo();
        return readPartialResponse(method, uri, headers, body, info, socket);
    }

    private CompletableFuture<byte[]> readPartialResponse(String method, URI uri, Map<String, ?> headers, byte[] body, PartialResponseInfo info, SocketClient socket) {
        return socket.readAsync(readReceiveBufferSize(socket)).thenComposeAsync(responseBuffer -> {
            info.updateSource(responseBuffer, true);
            return handlePartialResponse(method, uri, headers, body, info, socket);
        });
    }

    private CompletableFuture<byte[]> handlePartialResponse(String method, URI uri, Map<String, ?> headers, byte[] body, PartialResponseInfo info, SocketClient socket) {
        var partial = handleStatusCodeAndHeaders(info);
        if (partial) {
            return readPartialResponse(method, uri, headers, body, info, socket);
        }

        if (info.isRedirect()) {
            var location = URI.create(Objects.requireNonNull(info.location(), "Missing location for redirect status code"));
            return sendRequest(method, location.isAbsolute() ? location : uri.resolve(location), headers, body);
        }

        if (!String.valueOf(info.statusCode()).startsWith("20")) {
            throw new IllegalArgumentException("Unexpected response status code: " + info.statusCode());
        }

        if(info.contentLength() == 0) {
            return CompletableFuture.completedFuture(EMPTY_BUFFER);
        }

        var result = info.contentLength() == -1 ? readChunkedResponse(info, socket) : readFullResponse(uri, info, socket);
        return result.thenApplyAsync(response -> decodeResponse(info, response));
    }

    private byte[] decodeResponse(PartialResponseInfo info, byte[] response) {
        if(info.contentEncoding().isEmpty()) {
            return response;
        }

        for (var contentEncoding : info.contentEncoding()) {
            response = decodeResponse(response, contentEncoding);
        }

        return response;
    }

    private byte[] decodeResponse(byte[] response, String contentEncoding) {
        return switch (contentEncoding.toLowerCase()) {
            case "gzip" -> {
                try (var input = new GZIPInputStream(new ByteArrayInputStream(response))) {
                    yield input.readAllBytes();
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot decode gzip encoded response", exception);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported content encoding: " + contentEncoding);
        };
    }

    private CompletableFuture<byte[]> readFullResponse(URI uri, PartialResponseInfo info, SocketClient socket) {
        var partialBody = info.readBody(info.contentLength());
        if(partialBody != null) {
            closeSocketSilently(uri, info, socket);
            return CompletableFuture.completedFuture(partialBody);
        }

        return socket.readFullyAsync(info.contentLength() - info.remaining())
                .thenApplyAsync(additionalBody -> concatFullResponse(uri, info, socket, additionalBody))
                .exceptionallyComposeAsync(error -> handleFullResponseError(uri, info, socket, error));
    }

    private byte[] concatFullResponse(URI uri, PartialResponseInfo info, SocketClient socket, ByteBuffer additionalBody) {
        closeSocketSilently(uri, info, socket);
        var remaining = info.remaining();
        var result = new byte[remaining + additionalBody.remaining()];
        info.readBody(result);
        additionalBody.get(result, remaining, additionalBody.remaining());
        return result;
    }

    private CompletableFuture<byte[]> handleFullResponseError(URI uri, PartialResponseInfo info, SocketClient socket, Throwable error) {
        closeSocketSilently(uri, info, socket);
        return CompletableFuture.failedFuture(error);
    }

    private boolean handleStatusCodeAndHeaders(PartialResponseInfo info) {
        while (info.hasNext()) {
            var responseLine = info.readHeaderLine();
            info.setLastHeaderLineIndex(info.currentHeaderLineIndex());
            if(info.statusCode() == -1) {
                if(!responseLine.startsWith("HTTP")) {
                    continue;
                }

                info.setStatusCode(parseStatusCode(responseLine));
                continue;
            }

            if (responseLine.isEmpty()) {
                info.finish();
                break;
            }

            var responseLineParts = responseLine.split(":", 2);
            var headerKey = responseLineParts[0];
            var headerValue = responseLineParts.length == 2 ? responseLineParts[1].trim() : "";
            switch (headerKey.toLowerCase()) {
                case "content-length" -> {
                    try {
                        info.setContentLength(Integer.parseUnsignedInt(headerValue));
                    } catch (NumberFormatException exception) {
                        throw new IllegalArgumentException("Malformed Content-Length header: " + responseLine);
                    }
                }
                case "connection" -> info.setCloseConnection(headerValue.equalsIgnoreCase("close"));
                case "location" -> info.setLocation(headerValue);
                case "transfer-encoding" -> info.transferEncoding().addAll(Arrays.stream(headerValue.split(",")).map(TransferEncoding::of).toList());
                case "content-encoding" -> info.contentEncoding().addAll(Arrays.stream(headerValue.split(",")).map(String::trim).toList());
            }
        }
        return info.isPartial();
    }

    private int parseStatusCode(String responseLine) {
        var responseStatusParts = responseLine.split(" ");
        if (responseStatusParts.length < 2) {
            throw new IllegalArgumentException("Unexpected response status code: " + responseLine);
        }

        var statusCode = responseStatusParts[1];
        try {
            return Integer.parseUnsignedInt(statusCode);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Malformed status code: " + responseLine);
        }
    }

    private CompletableFuture<byte[]> readChunkedResponse(PartialResponseInfo info, SocketClient socket) {
        var chunkedLength = info.readChunkedBodyLength();
        if(chunkedLength.truncated()) {
            return socket.readAsync(readReceiveBufferSize(socket)).thenComposeAsync(responseBuffer -> {
                info.updateSource(responseBuffer, false);
                return readChunkedResponse(info, socket);
            });
        }

        if(chunkedLength.value() == -1 || !info.isPartial()) {
            return CompletableFuture.completedFuture(EMPTY_BUFFER);
        }

        return readChunkContent(info, socket, chunkedLength.value()).thenComposeAsync(currentChunk -> {
            if(!info.isPartial()) {
                return CompletableFuture.completedFuture(currentChunk);
            }

            return readChunkedResponse(info, socket)
                    .thenApplyAsync(nextChunk -> Bytes.concat(currentChunk, nextChunk));
        });
    }

    private CompletableFuture<byte[]> readChunkContent(PartialResponseInfo info, SocketClient socket, int chunkedLength) {
        if(info.remaining() >= chunkedLength + 2) {
            var result = info.readBody(chunkedLength);
            info.checkChunkTrailing();
            return CompletableFuture.completedFuture(result);
        }

        return socket.readFullyAsync(chunkedLength - info.remaining() + 2).thenComposeAsync(responseBuffer -> {
            info.updateSource(responseBuffer, false);
            return readChunkContent(info, socket, chunkedLength);
        });
    }

    private static final class PartialResponseInfo {
        private String headers;
        private ByteBuffer body;
        private int statusCode;
        private int contentLength;
        private final List<String> contentEncoding;
        private boolean closeConnection;
        private String location;
        private int lastHeaderLineIndex;
        private int currentHeaderLineIndex;
        private boolean partial;
        private final List<TransferEncoding> transferEncoding;

        private PartialResponseInfo() {
            this.headers = null;
            this.body = null;
            this.statusCode = -1;
            this.contentLength = -1;
            this.closeConnection = false;
            this.location = null;
            this.lastHeaderLineIndex = -1;
            this.currentHeaderLineIndex = -1;
            this.partial = true;
            this.transferEncoding = new ArrayList<>();
            this.contentEncoding = new ArrayList<>();
        }

        public boolean isPartial() {
            return partial;
        }

        private boolean isRedirect() {
            return statusCode == 302;
        }

        public int statusCode() {
            return statusCode;
        }

        public void setStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
        }

        private int contentLength() {
            return contentLength;
        }

        public List<String> contentEncoding() {
            return contentEncoding;
        }

        private boolean closeConnection() {
            return closeConnection;
        }

        private String location() {
            return location;
        }

        public List<TransferEncoding> transferEncoding() {
            return transferEncoding;
        }

        private int currentHeaderLineIndex() {
            return currentHeaderLineIndex;
        }

        private void setContentLength(int contentLength) {
            this.contentLength = contentLength;
        }

        private void setCloseConnection(boolean closeConnection) {
            this.closeConnection = closeConnection;
        }

        private void setLastHeaderLineIndex(int lastHeaderLineIndex) {
            this.lastHeaderLineIndex = lastHeaderLineIndex;
        }

        private void setLocation(String location) {
            this.location = location;
        }

        private void finish() {
            this.partial = false;
        }

        private boolean hasNext() {
            return headers != null && (currentHeaderLineIndex = headers.indexOf("\n", lastHeaderLineIndex + 1)) != -1;
        }

        private void updateSource(ByteBuffer response, boolean headers) {
            if(headers) {
                var divider = getMessageContentDivider(response);
                var oldLimit = response.limit();
                if(divider != -1) {
                    response.limit(divider);
                }
                var content = StandardCharsets.ISO_8859_1.decode(response).toString();
                if(this.headers == null) {
                    this.headers = content;
                }else {
                    this.headers = this.headers + content;
                }
                response.limit(oldLimit);
            }

            if(body != null && body.hasRemaining()) {
                var result = new byte[body.remaining() + response.remaining()];
                var i = 0;
                while (body.hasRemaining()) {
                    result[i++] = body.get();
                }
                while (response.hasRemaining()) {
                    result[i++] = response.get();
                }
                this.body = ByteBuffer.wrap(result);
            }else {
                this.body = response;
            }
        }

        private int getMessageContentDivider(ByteBuffer partialResult) {
            var index = -1;
            for (int i = 0; i < partialResult.remaining() - HTTP_MESSAGE_END_BYTES.length; i++) {
                if(partialResult.get(i) == HTTP_MESSAGE_END_BYTES[0]
                        && partialResult.get(i + 1) == HTTP_MESSAGE_END_BYTES[1]
                        && partialResult.get(i + 2) == HTTP_MESSAGE_END_BYTES[2]
                        && partialResult.get(i + 3) == HTTP_MESSAGE_END_BYTES[3]) {
                    index = i + HTTP_MESSAGE_END_BYTES.length;
                    break;
                }
            }
            return index;
        }

        private String readHeaderLine() {
            return headers.substring(lastHeaderLineIndex + 1, currentHeaderLineIndex).trim();
        }

        private byte[] readBody(int length) {
            if (body.remaining() < length) {
                return null;
            }

            var result = new byte[length];
            body.get(result);
            return result;
        }

        private void readBody(byte[] destination) {
            if(!body.hasRemaining()) {
                return;
            }

            body.get(destination, 0, body.remaining());
        }

        public ChunkedResult readChunkedBodyLength() {
            var position = body.position();
            var chunkSizeDigitsCount = 0;
            while (position + chunkSizeDigitsCount + 1 >= body.limit()
                    || body.get(position + chunkSizeDigitsCount) != '\r'
                    || body.get(position + chunkSizeDigitsCount + 1) != '\n') {
                if(position + chunkSizeDigitsCount + 1 >= body.limit()) {
                    return new ChunkedResult(-1, transferEncoding.contains(TransferEncoding.CHUNKED));
                }

                chunkSizeDigitsCount++;
            }

            var chunkSize = 0;
            for (var i = 1; i <= chunkSizeDigitsCount; i++) {
                chunkSize += (int) (Character.getNumericValue(body.get()) * Math.pow(16, chunkSizeDigitsCount - i));
            }

            checkChunkTrailing();

            this.partial = chunkSize != 0;
            return new ChunkedResult(chunkSize, false);
        }

        private void checkChunkTrailing() {
            if(body.get() != '\r' || body.get() != '\n') {
                throw new IllegalArgumentException("Truncated chunked message size");
            }
        }

        private int remaining() {
            return body.remaining();
        }
    }

    private record ChunkedResult(int value, boolean truncated) {

    }

    private enum TransferEncoding {
        CHUNKED,
        COMPRESS,
        GZIP,
        DEFLATE,
        UNKNOWN;

        private static final Map<String, TransferEncoding> CASES = Map.of(
                "chunked", CHUNKED,
                "compress", COMPRESS,
                "gzip", GZIP,
                "deflate", DEFLATE
        );

        private static TransferEncoding of(String value) {
            return CASES.getOrDefault(value.toLowerCase().trim(), UNKNOWN);
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

    private int readReceiveBufferSize(SocketClient client) {
        try {
            return client.getOption(StandardSocketOptions.SO_RCVBUF);
        }catch (IOException exception) {
            return 8192;
        }
    }

    private SocketClient getLockableSocketClient(URI uri) {
        try {
            var aliveSocket = aliveSockets == null ? null : aliveSockets.get(uri.getHost() + ":" + uri.getPort());
            if(aliveSocket != null) {
                return aliveSocket;
            }

            return switch (uri.getScheme().toLowerCase()) {
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
        }catch (IOException exception) {
            throw new UncheckedIOException(exception);
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

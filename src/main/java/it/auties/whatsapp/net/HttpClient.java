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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HttpClient {
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

    private final Platform platform;
    private final URI proxy;

    public HttpClient(Platform platform) {
        this(platform, null);
    }

    public HttpClient(Platform platform, URI proxy) {
        this.proxy = proxy;
        this.platform = platform;
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
        var socket = getSocketClient(uri);
        var builder = createRequestPayload(method, uri, headers, body);
        return socket.connectAsync(toSocketAddress(uri))
                .thenComposeAsync(ignored -> socket.writeAsync(builder.toString().getBytes()))
                .thenComposeAsync(ignored -> socket.readAsync(readReceiveBufferSize(socket)))
                .thenComposeAsync(responseBuffer -> parseResponsePayload(responseBuffer, socket))
                .orTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .whenCompleteAsync((result, error) -> {
                    closeSocketSilently(socket);
                    if(error != null) {
                        Exceptions.rethrow(error);
                    }
                });
    }

    private void closeSocketSilently(SocketClient socket) {
        try {
            socket.close();
        } catch (IOException ignored) {

        }
    }

    private CompletableFuture<byte[]> parseResponsePayload(ByteBuffer responseBuffer, SocketClient socket) {
        var response = StandardCharsets.UTF_8
                .decode(responseBuffer)
                .toString();

        var responseStatusLineEnd = response.indexOf("\n");
        checkStatusCode(response, responseStatusLineEnd);

        var contentLength = -1;
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

            if(!responseLineParts[0].equalsIgnoreCase("Content-Length")) {
                continue;
            }

            try {
                contentLength = Integer.parseUnsignedInt(responseLineParts[1]);
            }catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Malformed Content-Length header: " + responseLine);
            }
        }

        if(contentLength == -1) {
            return CompletableFuture.completedFuture(new byte[0]);
        }

        var partialBody = response.length() <= currentHeaderLineIndex + 1 ? new byte[0] : response.substring(currentHeaderLineIndex + 1).getBytes();
        if(partialBody.length > contentLength) {
            throw new IllegalArgumentException("Actual content length is bigger than reported in the response(expected: %s, got: %s)".formatted(contentLength, partialBody.length));
        }

        if(partialBody.length == contentLength) {
            return CompletableFuture.completedFuture(partialBody);
        }

        var remainingLength = contentLength - partialBody.length;
        return socket.readFullyAsync(remainingLength).thenApplyAsync(additionalBody -> {
            var completeResult = new byte[partialBody.length + additionalBody.remaining()];
            System.arraycopy(partialBody, 0, completeResult, 0, partialBody.length);
            additionalBody.get(completeResult, partialBody.length, remainingLength);
            return completeResult;
        });
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
            builder.append(new String(body))
                    .append("\r\n");
        }
        return builder;
    }

    private void checkStatusCode(String response, int responseStatusLineEnd) {
        var responseStatusLine = responseStatusLineEnd == -1 ? response : response.substring(0, responseStatusLineEnd);
        var responseStatusParts = responseStatusLine.split(" ");
        if (responseStatusParts.length < 2) {
            throw new IllegalArgumentException("Unexpected response status code: " + response);
        }

        var statusCode = responseStatusParts[1];
        if(!statusCode.startsWith("20")) {
            throw new IllegalArgumentException("Unexpected response status code: " + statusCode);
        }
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

    private SocketClient getSocketClient(URI uri) {
        try {
            return switch (uri.getScheme().toLowerCase()) {
                case "http" -> SocketClient.newPlainClient(proxy);
                case "https" -> {
                    var sslEngine = platform.sslData()
                            .context()
                            .createSSLEngine(uri.getHost(), uri.getPort() == 1 ? 443 : uri.getPort());
                    sslEngine.setUseClientMode(true);
                    sslEngine.setSSLParameters(platform.sslData().parameters());
                    yield SocketClient.newSecureClient(sslEngine, proxy);
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
}

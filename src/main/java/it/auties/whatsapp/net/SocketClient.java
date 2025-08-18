package it.auties.whatsapp.net;

import it.auties.whatsapp.util.Proxies;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class SocketClient implements AutoCloseable {
    private static final int DEFAULT_CONNECTION_TIMEOUT = 300;
    static {
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
    }

    public static SocketClient newPlainClient(URI proxy) throws IOException {
        var channel = AsynchronousSocketChannel.open();
        var layerSupport = new SocketTransport.Plain(channel);
        var proxySupport = SocketConnection.of(channel, layerSupport, proxy);
        return new SocketClient(channel, proxySupport, layerSupport);
    }

    public static SocketClient newSecureClient(SSLEngine sslEngine, URI proxy) throws IOException {
        var channel = AsynchronousSocketChannel.open();
        var layerSupport = new SocketTransport.Secure(channel, sslEngine);
        var proxySupport = SocketConnection.of(channel, layerSupport, proxy);
        return new SocketClient(channel, proxySupport, layerSupport);
    }

    final AsynchronousSocketChannel channel;
    final SocketConnection socketConnection;
    SocketTransport socketTransport;
    private SocketClient(AsynchronousSocketChannel channel, SocketConnection socketConnection, SocketTransport socketTransport) {
        this.channel = channel;
        this.socketConnection = socketConnection;
        this.socketTransport = socketTransport;
    }

    public CompletableFuture<Void> upgradeToSsl(SSLEngine sslEngine) {
        if(!isConnected()) {
            throw new IllegalArgumentException("The socket is not connected");
        }

        if(socketTransport.isSecure()) {
            throw new IllegalStateException("This socket is already using a secure connection");
        }

        this.socketTransport = new SocketTransport.Secure(channel, sslEngine);
        return socketTransport.handshake(); // Upgrading a websocket is not supported, so path is always null
    }

    public CompletableFuture<Void> connectAsync(InetSocketAddress address) {
        return connectAsync(address, DEFAULT_CONNECTION_TIMEOUT);
    }

    public CompletableFuture<Void> connectAsync(InetSocketAddress address, int timeout) {
        return socketConnection.connectAsync(address, timeout)
                .thenComposeAsync(ignored -> socketTransport.handshake())
                .exceptionallyComposeAsync(this::closeSocketOnError);
    }

    private CompletableFuture<Void> closeSocketOnError(Throwable error) {
        try {
            close();
        }catch (Throwable ignored) {

        }

        return CompletableFuture.failedFuture(error);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    public boolean isConnected() {
        try {
            return channel.getRemoteAddress() != null;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isClosed() {
        return !channel.isOpen();
    }

    public int getReceiveBufferSize() {
        return socketTransport.getReceiveBufferSize();
    }

    public int getSendBufferSize() {
        return socketTransport.getSendBufferSize();
    }

    public SocketAddress getRemoteSocketAddress() {
        return socketConnection.address()
                .orElse(null);
    }

    public void setKeepAlive(boolean on) throws SocketException {
        if(!channel.supportedOptions().contains(StandardSocketOptions.SO_KEEPALIVE)) {
            return;
        }

        try {
            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, on);
        } catch (IOException e) {
            throw new SocketException(e);
        }
    }

    public boolean getKeepAlive() {
        try {
            return channel.getOption(StandardSocketOptions.SO_KEEPALIVE);
        } catch (IOException ignored) {
            return false;
        }
    }

    public CompletableFuture<Void> writeAsync(byte[] data) {
        return writeAsync(data, 0, data.length);
    }

    public CompletableFuture<Void> writeAsync(byte[] data, int offset, int length) {
        return writeAsync(ByteBuffer.wrap(data, offset, length));
    }

    public CompletableFuture<Void> writeAsync(ByteBuffer buffer) {
        var future = new Response.Future<Void>();
        return socketTransport.write(buffer, future);
    }

    public void readFullyAsync(int length, Response.Callback<ByteBuffer> callback) {
        if (length < 0) {
            throw new IllegalArgumentException("Cannot read %s bytes from socket".formatted(length));
        }

        var buffer = ByteBuffer.allocate(length);
        socketTransport.readFully(buffer, callback);
    }

    public CompletableFuture<ByteBuffer> readFullyAsync(int length) {
        if (length < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Cannot read %s bytes from socket".formatted(length)));
        }

        var buffer = ByteBuffer.allocate(length);
        var future = new Response.Future<ByteBuffer>();
        socketTransport.readFully(buffer, future);
        return future;
    }

    public CompletableFuture<ByteBuffer> readAsync(int length) {
        var future = new Response.Future<ByteBuffer>();
        return readAsyncBuffer(length, future);
    }

    public void readAsync(int length, Response.Callback<ByteBuffer> callback) {
        readAsyncBuffer(length, callback);
    }

    private <R extends Response<ByteBuffer>> R readAsyncBuffer(int length, R result) {
        if (length < 0) {
            result.completeExceptionally(new IllegalArgumentException("Cannot read %s bytes from socket".formatted(length)));
            return result;
        }

        var buffer = ByteBuffer.allocate(length);
        readAsync(buffer, (bytesRead, error) -> {
            if(error != null) {
                result.completeExceptionally(error);
                return;
            }

            result.complete(buffer);
        });
        return result;
    }

    public CompletableFuture<Integer> readAsync(ByteBuffer buffer) {
        var future = new Response.Future<Integer>();
        return socketTransport.read(buffer, true, future);
    }

    public void readAsync(ByteBuffer buffer, Response.Callback<Integer> callback) {
        socketTransport.read(buffer, true, callback);
    }

    public Optional<String> applicationProtocol() {
        return socketTransport.getApplicationProtocol();
    }

    static sealed abstract class SocketTransport {
        private static final int DEFAULT_BUFFER_SIZE = 8192;

        final AsynchronousSocketChannel channel;
        private SocketTransport(AsynchronousSocketChannel channel) {
            this.channel = channel;
        }

        abstract CompletableFuture<Void> handshake();

        abstract Optional<String> getApplicationProtocol();

        abstract boolean isSecure();

        abstract <R extends Response<Void>> R write(ByteBuffer buffer, R result);

        abstract <R extends Response<Integer>> R read(ByteBuffer buffer, boolean lastRead, R result);

        public int getReceiveBufferSize() {
            try {
                return channel.getOption(StandardSocketOptions.SO_RCVBUF);
            } catch (Throwable e) {
                return DEFAULT_BUFFER_SIZE;
            }
        }

        public int getSendBufferSize() {
            try {
                return channel.getOption(StandardSocketOptions.SO_SNDBUF);
            } catch (Throwable e) {
                return DEFAULT_BUFFER_SIZE;
            }
        }

        <R extends Response<Integer>> R readPlain(ByteBuffer buffer, boolean lastRead, R result) {
            channel.read(buffer, null, new CompletionHandler<>() {
                @Override
                public void completed(Integer bytesRead, Object attachment) {
                    if(bytesRead == -1) {
                        result.completeExceptionally(new EOFException());
                        return;
                    }

                    if(lastRead) {
                        buffer.flip();
                    }

                    result.complete(bytesRead);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    result.completeExceptionally(exc);
                }
            });
            return result;
        }

        <R extends Response<Void>> R writePlain(ByteBuffer buffer, R result) {
            channel.write(buffer, null, new CompletionHandler<>() {
                @Override
                public void completed(Integer bytesWritten, Object attachment) {
                    if(bytesWritten == -1) {
                        result.completeExceptionally(new SocketException());
                        return;
                    }

                    if(buffer.hasRemaining()) {
                        writePlain(buffer, result);
                        return;
                    }

                    result.complete(null);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    result.completeExceptionally(exc);
                }
            });
            return result;
        }

        public void readFully(ByteBuffer buffer, Response<ByteBuffer> result) {
            read(buffer, false, (Response.Callback<Integer>) (readResult, error) -> {
                if (error != null) {
                    result.completeExceptionally(error);
                    return;
                }

                if(buffer.hasRemaining()) {
                    readFully(buffer, result);
                    return;
                }

                buffer.flip();
                result.complete(buffer);
            });
        }

        private static final class Plain extends SocketTransport {
            private Plain(AsynchronousSocketChannel channel) {
                super(channel);
            }

            @Override
            boolean isSecure() {
                return false;
            }

            @Override
            Optional<String> getApplicationProtocol() {
                return Optional.empty();
            }

            @Override
            <R extends Response<Integer>> R read(ByteBuffer buffer, boolean lastRead, R result) {
                return readPlain(buffer, lastRead, result);
            }

            @Override
            <R extends Response<Void>> R write(ByteBuffer buffer, R result) {
                return writePlain(buffer, result);
            }

            @Override
            CompletableFuture<Void> handshake() {
                return CompletableFuture.completedFuture(null);
            }
        }

        private static final class Secure extends SocketTransport {
            private final AtomicBoolean sslHandshakeCompleted;
            private final Object sslHandshakeLock;
            private final SSLEngine sslEngine;
            private final ByteBuffer sslReadBuffer;
            private final ByteBuffer sslWriteBuffer;
            private final ByteBuffer sslOutputBuffer;
            private Response.Future<Void> sslHandshake;
            private Secure(AsynchronousSocketChannel channel, SSLEngine sslEngine) {
                super(channel);
                this.sslHandshakeCompleted = new AtomicBoolean();
                this.sslHandshakeLock = new Object();
                sslHandshakeCompleted.set(sslEngine == null);
                this.sslEngine = sslEngine;
                var bufferSize = sslEngine.getSession().getPacketBufferSize();
                this.sslReadBuffer = ByteBuffer.allocate(bufferSize);
                this.sslWriteBuffer = ByteBuffer.allocate(bufferSize);
                this.sslOutputBuffer = ByteBuffer.allocate(bufferSize);
            }

            @Override
            boolean isSecure() {
                return true;
            }

            @Override
            Optional<String> getApplicationProtocol() {
                return Optional.ofNullable(sslEngine.getApplicationProtocol());
            }

            @Override
            CompletableFuture<Void> handshake() {
                try {
                    if(sslEngine == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    if(sslHandshakeCompleted.get()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    if(sslHandshake != null) {
                        return sslHandshake;
                    }

                    synchronized (sslHandshakeLock) {
                        if(sslHandshake != null) {
                            return sslHandshake;
                        }

                        this.sslHandshake = new Response.Future<>();
                        sslEngine.beginHandshake();
                        sslReadBuffer.position(sslReadBuffer.limit());
                        handleSslHandshakeStatus(null);
                        return sslHandshake;
                    }
                } catch (Throwable throwable) {
                    return CompletableFuture.failedFuture(throwable);
                }
            }

            private void handleSslHandshakeStatus(Status status){
                switch (sslEngine.getHandshakeStatus()) {
                    case NEED_WRAP -> doSslHandshakeWrap();
                    case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> doSslHandshakeUnwrap(status == Status.BUFFER_UNDERFLOW);
                    case NEED_TASK -> doSslHandshakeTasks();
                    case FINISHED -> finishSslHandshake();
                    case NOT_HANDSHAKING -> sslHandshake.completeExceptionally(new IOException("Cannot complete handshake"));
                }
            }

            private void finishSslHandshake() {
                sslHandshakeCompleted.set(true);
                sslOutputBuffer.clear();
                sslHandshake.complete(null);
            }

            private void doSslHandshakeTasks() {
                Runnable runnable;
                while ((runnable = sslEngine.getDelegatedTask()) != null) {
                    runnable.run();
                }

                handleSslHandshakeStatus(null);
            }

            private void doSslHandshakeUnwrap(boolean forceRead) {
                sslReadBuffer.compact();
                if (!forceRead && sslReadBuffer.position() != 0) {
                    sslReadBuffer.flip();
                    doSSlHandshakeUnwrapOperation();
                    return;
                }

                readPlain(sslReadBuffer, true, (Response.Callback<Integer>) (ignored, error) -> {
                    if(error != null) {
                        sslHandshake.completeExceptionally(error);
                        return;
                    }

                    doSSlHandshakeUnwrapOperation();
                });
            }

            private void doSSlHandshakeUnwrapOperation() {
                try {
                    var result = sslEngine.unwrap(sslReadBuffer, sslOutputBuffer);
                    if(isHandshakeFinished(result, false)) {
                        finishSslHandshake();
                    }else {
                        handleSslHandshakeStatus(result.getStatus());
                    }
                }catch(Throwable throwable) {
                    sslHandshake.completeExceptionally(throwable);
                }
            }

            private void doSslHandshakeWrap() {
                try {
                    sslWriteBuffer.clear();
                    var result = sslEngine.wrap(sslOutputBuffer, sslWriteBuffer);
                    var isHandshakeFinished = isHandshakeFinished(result, true);
                    sslWriteBuffer.flip();
                    writePlain(sslWriteBuffer, (Response.Callback<Void>) (ignored, error) -> {
                        if(error != null) {
                            sslHandshake.completeExceptionally(error);
                            return;
                        }

                        if(isHandshakeFinished) {
                            finishSslHandshake();
                        }else {
                            handleSslHandshakeStatus(null);
                        }
                    });
                }catch (Throwable throwable) {
                    sslHandshake.completeExceptionally(throwable);
                }
            }

            private boolean isHandshakeFinished(SSLEngineResult result, boolean wrap) {
                var sslEngineStatus = result.getStatus();
                if (sslEngineStatus != Status.OK && (wrap || sslEngineStatus != Status.BUFFER_UNDERFLOW)) {
                    throw new IllegalStateException("SSL handshake operation failed with status: " + sslEngineStatus);
                }

                if (wrap && result.bytesConsumed() != 0) {
                    throw new IllegalStateException("SSL handshake operation failed with status: no bytes consumed");
                }

                if (!wrap && result.bytesProduced() != 0) {
                    throw new IllegalStateException("SSL handshake operation failed with status: no bytes produced");
                }

                var sslHandshakeStatus = result.getHandshakeStatus();
                return sslHandshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED;
            }

            @Override
            <R extends Response<Integer>> R read(ByteBuffer buffer, boolean lastRead, R result) {
                try {
                    if(!sslHandshakeCompleted.get()) {
                        return readPlain(buffer, lastRead, result);
                    }

                    var bytesCopied = readFromBufferedOutput(buffer, lastRead);
                    if(bytesCopied != 0) {
                        result.complete(bytesCopied);
                    }else if (sslReadBuffer.hasRemaining()) {
                        decodeSslBuffer(buffer, lastRead, result);
                    }else {
                        fillSslBuffer(buffer, lastRead, result);
                    }

                    return result;
                }catch (Throwable throwable) {
                    result.completeExceptionally(throwable);
                    return result;
                }
            }

            private <R extends Response<Integer>> void fillSslBuffer(ByteBuffer buffer, boolean lastRead, R result) {
                sslReadBuffer.compact();
                readPlain(sslReadBuffer, true, (Response.Callback<Integer>) (ignored, error) -> {
                    if (error != null) {
                        result.completeExceptionally(error);
                        return;
                    }

                    decodeSslBuffer(buffer, lastRead, result);
                });
            }

            private void decodeSslBuffer(ByteBuffer buffer, boolean lastRead, Response<Integer> result) {
                try {
                    var unwrapResult = sslEngine.unwrap(sslReadBuffer, sslOutputBuffer);
                    switch (unwrapResult.getStatus()) {
                        case OK -> {
                            if (unwrapResult.bytesProduced() == 0) {
                                sslOutputBuffer.mark();
                                read(buffer, lastRead , result);
                            } else {
                                var bytesCopied = readFromBufferedOutput(buffer, lastRead);
                                result.complete(bytesCopied);
                            }
                        }
                        case BUFFER_UNDERFLOW -> fillSslBuffer(buffer, lastRead, result);
                        case BUFFER_OVERFLOW -> result.completeExceptionally(new IllegalStateException("SSL output buffer overflow"));
                        case CLOSED -> result.completeExceptionally(new EOFException());
                    }
                }catch (Throwable throwable) {
                    result.completeExceptionally(throwable);
                }
            }

            private int readFromBufferedOutput(ByteBuffer buffer, boolean lastRead) {
                var writePosition = sslOutputBuffer.position();
                if(writePosition == 0) {
                    return 0;
                }

                var bytesRead = 0;
                var writeLimit = sslOutputBuffer.limit();
                sslOutputBuffer.limit(writePosition);
                try {
                    sslOutputBuffer.reset(); // Go back to last read position
                }catch (InvalidMarkException exception) {
                    sslOutputBuffer.flip(); // This can happen if unwrapResult.bytesProduced() != 0 on the first call
                }
                while (buffer.hasRemaining() && sslOutputBuffer.hasRemaining()) {
                    buffer.put(sslOutputBuffer.get());
                    bytesRead++;
                }

                if(!sslOutputBuffer.hasRemaining()) {
                    sslOutputBuffer.clear();
                    sslOutputBuffer.mark();
                }else {
                    sslOutputBuffer.limit(writeLimit);
                    sslOutputBuffer.mark();
                    sslOutputBuffer.position(writePosition);
                }

                if(lastRead) {
                    buffer.flip();
                }

                return bytesRead;
            }

            @Override
            <R extends Response<Void>> R write(ByteBuffer buffer, R result) {
                if(!sslHandshakeCompleted.get()) {
                    return writePlain(buffer, result);
                }

                writeSecure(buffer, result);
                return result;
            }

            private <R extends Response<Void>> void writeSecure(ByteBuffer buffer, R result) {
                if(!buffer.hasRemaining()) {
                    result.complete(null);
                    return;
                }

                try {
                    sslWriteBuffer.clear();
                    var wrapResult = sslEngine.wrap(buffer, sslWriteBuffer);
                    var status = wrapResult.getStatus();
                    if (status != Status.OK && status != Status.BUFFER_OVERFLOW) {
                        throw new IllegalStateException("SSL wrap failed with status: " + status);
                    }

                    sslWriteBuffer.flip();
                    writePlain(sslWriteBuffer, (Response.Callback<Void>) (ignored, error) -> {
                        if(error != null) {
                            result.completeExceptionally(error);
                            return;
                        }

                        writeSecure(buffer, result);
                    });
                }catch (SSLException exception) {
                    result.completeExceptionally(exception);
                }
            }
        }
    }

    private sealed static abstract class SocketConnection {
        private static final Semaphore CONNECTION_SEMAPHORE = new Semaphore(50, true);
        final AsynchronousSocketChannel channel;
        final SocketTransport socketTransport;
        final URI proxy;
        InetSocketAddress address;
        private SocketConnection(AsynchronousSocketChannel channel, SocketTransport socketTransport, URI proxy) {
            this.channel = channel;
            this.socketTransport = socketTransport;
            this.proxy = proxy;
        }

        private static SocketConnection of(AsynchronousSocketChannel channel, SocketTransport socketTransport, URI proxy) {
            return switch (Proxies.toProxy(proxy).type()) {
                case DIRECT -> new NoProxy(channel);
                case HTTP -> new HttpProxy(channel, socketTransport, proxy);
                case SOCKS -> new SocksProxy(channel, socketTransport, proxy);
            };
        }

        CompletableFuture<Void> connectAsync(InetSocketAddress address, int timeout) {
            return CompletableFuture.runAsync(() -> connectSync(address), Thread::startVirtualThread)
                    .orTimeout(timeout > 0 ? timeout : DEFAULT_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        }

        private void connectSync(InetSocketAddress address) {
            try {
                CONNECTION_SEMAPHORE.acquire();
                var start = System.currentTimeMillis();
                var future = channel.connect(address);
                future.get();
                this.address = address;
            }catch (Throwable throwable) {
                throw new RuntimeException("Cannot connect to " + address, throwable);
            }finally {
                CONNECTION_SEMAPHORE.release();
            }
        }

        public Optional<InetSocketAddress> address() {
            return Optional.ofNullable(address);
        }

        private static final class NoProxy extends SocketConnection {
            private NoProxy(AsynchronousSocketChannel channel) {
                super(channel, null, null);
            }

            @Override
            public CompletableFuture<Void> connectAsync(InetSocketAddress address, int timeout) {
                return super.connectAsync(address, timeout);
            }
        }

        private static final class HttpProxy extends SocketConnection {
            private static final int DEFAULT_RCV_BUF = 8192;
            private static final int OK_STATUS_CODE = 200;

            private HttpProxy(AsynchronousSocketChannel channel, SocketTransport socketTransport, URI proxy) {
                super(channel, socketTransport, proxy);
            }

            @Override
            public CompletableFuture<Void> connectAsync(InetSocketAddress address, int timeout) {
                return super.connectAsync(new InetSocketAddress(proxy.getHost(), proxy.getPort()), timeout)
                        .thenComposeAsync(openResult -> sendAuthentication(address))
                        .thenComposeAsync(connectionResult -> readAuthenticationResponse())
                        .thenComposeAsync(this::handleAuthentication);
            }

            private CompletableFuture<Void> handleAuthentication(String response) {
                var responseParts = response.split(" ");
                if(responseParts.length < 2) {
                    return CompletableFuture.failedFuture(new SocketException("HTTP : Cannot connect to proxy, malformed response: " + response));
                }

                var statusCodePart = responseParts[1];
                try {
                    var statusCode = statusCodePart == null ? -1 : Integer.parseUnsignedInt(statusCodePart);
                    if(statusCode != OK_STATUS_CODE) {
                        return CompletableFuture.failedFuture(new SocketException("HTTP : Cannot connect to proxy, status code " + statusCode));
                    }

                    return CompletableFuture.completedFuture(null);
                }catch (Throwable throwable) {
                    return CompletableFuture.failedFuture(new SocketException("HTTP : Cannot connect to proxy: " + response));
                }
            }

            private CompletableFuture<String> readAuthenticationResponse() {
                var decoder = new HttpDecoder(socketTransport);
                var future = new CompletableFuture<String>();
                var buffer = ByteBuffer.allocate(readReceiveBufferSize());
                socketTransport.read(buffer, true, (Response.Callback<Integer>) (result, error) -> {
                    if (error != null) {
                        future.completeExceptionally(new SocketException("HTTP : Cannot read authentication response", error));
                        return;
                    }

                    var data = new byte[result];
                    buffer.get(data);
                    future.complete(new String(data));
                });
                return future;
            }

            private int readReceiveBufferSize() {
                try {
                    return channel.getOption(StandardSocketOptions.SO_RCVBUF);
                }catch (IOException exception) {
                    return DEFAULT_RCV_BUF;
                }
            }

            private CompletableFuture<Void> sendAuthentication(InetSocketAddress endpoint) {
                var builder = new StringBuilder();
                builder.append("CONNECT ")
                        .append(endpoint.getHostName())
                        .append(":")
                        .append(endpoint.getPort())
                        .append(" HTTP/1.1\r\n");
                builder.append("Host: ")
                        .append(endpoint.getHostName())
                        .append(":")
                        .append(endpoint.getPort())
                        .append("\r\n");
                var authInfo = proxy.getUserInfo();
                if (authInfo != null) {
                    builder.append("Proxy-Authorization: Basic ")
                            .append(Base64.getEncoder().encodeToString(authInfo.getBytes()))
                            .append("\r\n");
                }
                builder.append("\r\n");
                var result = new Response.Future<Void>();
                socketTransport.write(ByteBuffer.wrap(builder.toString().getBytes()), result);
                return result;
            }
        }

        private static final class SocksProxy extends SocketConnection {
            private static final byte VERSION_5 = 5;

            private static final int NO_AUTH = 0;
            private static final int USER_PASSW = 2;
            private static final int NO_METHODS = -1;

            private static final int CONNECT = 1;

            private static final int IPV4 = 1;
            private static final int DOMAIN_NAME = 3;
            private static final int IPV6 = 4;

            private static final int REQUEST_OK = 0;
            private static final int GENERAL_FAILURE = 1;
            private static final int NOT_ALLOWED = 2;
            private static final int NET_UNREACHABLE = 3;
            private static final int HOST_UNREACHABLE = 4;
            private static final int CONN_REFUSED = 5;
            private static final int TTL_EXPIRED = 6;
            private static final int CMD_NOT_SUPPORTED = 7;
            private static final int ADDR_TYPE_NOT_SUP = 8;

            private SocksProxy(AsynchronousSocketChannel channel, SocketTransport socketTransport, URI proxy) {
                super(channel, socketTransport, proxy);
            }


            @Override
            public CompletableFuture<Void> connectAsync(InetSocketAddress address, int timeout) {
                return super.connectAsync(new InetSocketAddress(proxy.getHost(), proxy.getPort()), timeout)
                        .thenComposeAsync(openResult -> sendAuthenticationRequest())
                        .thenComposeAsync(response -> sendAuthenticationData(address, response));
            }

            private CompletableFuture<ByteBuffer> sendAuthenticationRequest() {
                var connectionPayload = new ByteArrayOutputStream();
                connectionPayload.write(VERSION_5);
                connectionPayload.write(2);
                connectionPayload.write(NO_AUTH);
                connectionPayload.write(USER_PASSW);
                var result = new Response.Future<Void>();
                socketTransport.write(ByteBuffer.wrap(connectionPayload.toByteArray()), result);
                return result.thenComposeAsync(connectionResult -> readServerResponse(2, "Cannot read authentication request response"));
            }

            private CompletionStage<Void> sendAuthenticationData(InetSocketAddress address, ByteBuffer response) {
                var socksVersion = response.get();
                if (socksVersion != VERSION_5) {
                    return CompletableFuture.failedFuture(new SocketException("SOCKS : Invalid version"));
                }

                var method = response.get();
                if (method == NO_METHODS) {
                    return CompletableFuture.failedFuture(new SocketException("SOCKS : No acceptable methods"));
                }

                if (method == NO_AUTH) {
                    return sendConnectionData(address, null);
                }

                if (method != USER_PASSW) {
                    return CompletableFuture.failedFuture(new SocketException("SOCKS : authentication failed"));
                }

                var userInfo = Proxies.parseUserInfo(proxy.getUserInfo());
                if (userInfo == null) {
                    return CompletableFuture.failedFuture(new SocketException("SOCKS : missing user info"));
                }

                var outputStream = new ByteArrayOutputStream();
                outputStream.write(1);
                outputStream.write(userInfo.username().length());
                outputStream.writeBytes(userInfo.username().getBytes(StandardCharsets.ISO_8859_1));
                if (userInfo.password() != null) {
                    outputStream.write(userInfo.password().length());
                    outputStream.writeBytes(userInfo.password().getBytes(StandardCharsets.ISO_8859_1));
                } else {
                    outputStream.write(0);
                }
                var result = new Response.Future<Void>();
                socketTransport.write(ByteBuffer.wrap(outputStream.toByteArray()), result);
                return result.thenComposeAsync(connectionResult -> readServerResponse(2, "Cannot read authentication data response"))
                        .thenComposeAsync(connectionResponse -> sendConnectionData(address, connectionResponse));
            }

            private CompletableFuture<Void> sendConnectionData(InetSocketAddress address, ByteBuffer connectionResponse) {
                if(connectionResponse != null && connectionResponse.get(1) != 0) {
                    return CompletableFuture.failedFuture(new SocketException("SOCKS : authentication failed"));
                }

                var outputStream = new ByteArrayOutputStream();
                outputStream.write(VERSION_5);
                outputStream.write(CONNECT);
                outputStream.write(0);
                outputStream.write(DOMAIN_NAME);
                outputStream.write(address.getHostName().length());
                outputStream.writeBytes(address.getHostName().getBytes(StandardCharsets.ISO_8859_1));
                outputStream.write((address.getPort() >> 8) & 0xff);
                outputStream.write((address.getPort()) & 0xff);
                var result = new Response.Future<Void>();
                socketTransport.write(ByteBuffer.wrap(outputStream.toByteArray()), result);
                return result.thenComposeAsync(authenticationResult -> readServerResponse(4, "Cannot read connection data response"))
                        .thenComposeAsync(this::onConnected);
            }

            private CompletableFuture<Void> onConnected(ByteBuffer authenticationResponse) {
                if(authenticationResponse.limit() < 2) {
                    return CompletableFuture.failedFuture(new SocketException("SOCKS malformed response"));
                }

                return switch (authenticationResponse.get(1)) {
                    case REQUEST_OK -> onConnected(authenticationResponse.get(3));
                    case GENERAL_FAILURE -> CompletableFuture.failedFuture(new SocketException("SOCKS server general failure"));
                    case NOT_ALLOWED -> CompletableFuture.failedFuture(new SocketException("SOCKS: Connection not allowed by ruleset"));
                    case NET_UNREACHABLE -> CompletableFuture.failedFuture(new SocketException("SOCKS: Network unreachable"));
                    case HOST_UNREACHABLE -> CompletableFuture.failedFuture(new SocketException("SOCKS: Host unreachable"));
                    case CONN_REFUSED -> CompletableFuture.failedFuture(new SocketException("SOCKS: Connection refused"));
                    case TTL_EXPIRED -> CompletableFuture.failedFuture(new SocketException("SOCKS: TTL expired"));
                    case CMD_NOT_SUPPORTED -> CompletableFuture.failedFuture(new SocketException("SOCKS: Command not supported"));
                    case ADDR_TYPE_NOT_SUP -> CompletableFuture.failedFuture(new SocketException("SOCKS: address type not supported"));
                    default -> CompletableFuture.failedFuture(new SocketException("SOCKS: unhandled error"));
                };
            }

            private CompletableFuture<Void> onConnected(byte authenticationType) {
                return switch (authenticationType) {
                    case IPV4 -> readServerResponse(4, "Cannot read IPV4 address")
                            .thenComposeAsync(ipResult -> readServerResponse(2, "Cannot read IPV4 port"))
                            .thenRun(() -> {});
                    case IPV6 -> readServerResponse(16, "Cannot read IPV6 address")
                            .thenComposeAsync(ipResult -> readServerResponse(2, "Cannot read IPV6 port"))
                            .thenRun(() -> {});
                    case DOMAIN_NAME -> readServerResponse(1, "Cannot read domain name")
                            .thenComposeAsync(domainLengthBuffer -> readServerResponse(Byte.toUnsignedInt(domainLengthBuffer.get()), "Cannot read domain hostname"))
                            .thenComposeAsync(ipResult -> readServerResponse(2, "Cannot read domain port"))
                            .thenRun(() -> {});
                    default -> CompletableFuture.failedFuture(new SocketException("Reply from SOCKS server contains wrong code"));
                };
            }

            private CompletableFuture<ByteBuffer> readServerResponse(int length, String errorMessage) {
                var buffer = ByteBuffer.allocate(length);
                var result = new Response.Future<ByteBuffer>();
                socketTransport.readFully(buffer, result);
                return result.exceptionallyCompose(error -> CompletableFuture.failedFuture(new SocketException(errorMessage, error)));
            }
        }
    }
}

package it.auties.whatsapp.net;

import it.auties.whatsapp.util.Proxies;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public sealed abstract class Socket extends java.net.Socket {
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30;

    public static Socket newPlainClient(URI proxy) throws IOException {
        return switch (Proxies.toProxy(proxy).type()) {
            case DIRECT -> new Direct(null);
            case HTTP -> new Http(null, proxy);
            case SOCKS -> new Socks5(null, proxy);
        };
    }

    public static Socket newSSLClient(SSLEngine context, URI proxy) throws IOException {
        return switch (Proxies.toProxy(proxy).type()) {
            case DIRECT -> new Direct(context);
            case HTTP -> new Http(context, proxy);
            case SOCKS -> new Socks5(context, proxy);
        };
    }

    final AsynchronousSocketChannel delegate;
    final URI proxy;
    final SSLEngine sslEngine;
    final ByteBuffer sslReadBuffer;
    final ByteBuffer sslWriteBuffer;
    final ByteBuffer sslOutputBuffer;
    final InputStream inputStream;
    final OutputStream outputStream;
    final AtomicBoolean sslHandshakeCompled;
    int sslOutputBufferReadPosition;
    Socket(SSLEngine sslEngine, URI proxy) throws IOException {
        this.delegate = AsynchronousSocketChannel.open();
        delegate.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        this.proxy = proxy;
        this.sslEngine = sslEngine;
        this.inputStream = new BlockingSocketInputStream();
        this.outputStream = new BlockingSocketOutputStream();
        this.sslHandshakeCompled = new AtomicBoolean(sslEngine == null);
        if(sslEngine != null) {
            var bufferSize = sslEngine.getSession().getPacketBufferSize();
            this.sslReadBuffer = ByteBuffer.allocate(bufferSize);
            this.sslWriteBuffer = ByteBuffer.allocate(bufferSize);
            this.sslOutputBuffer = ByteBuffer.allocate(bufferSize);
        }else {
            this.sslReadBuffer = null;
            this.sslWriteBuffer = null;
            this.sslOutputBuffer = null;
        }
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, DEFAULT_CONNECTION_TIMEOUT);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if(!(endpoint instanceof InetSocketAddress inetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }

        var future = connectAsync(inetSocketAddress, timeout);
        future.join();
    }

    public CompletableFuture<Void> connectAsync(InetSocketAddress address) {
        return connectAsync(address, DEFAULT_CONNECTION_TIMEOUT);
    }

    public CompletableFuture<Void> connectAsync(InetSocketAddress address, int timeout) {
        var future = new CompletableFuture<Void>();
        delegate.connect(address, null, new CompletionHandler<>() {
            @Override
            public void completed(Void result, Object attachment) {
                future.complete(null);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                future.completeExceptionally(exc);
            }
        });
        return future.orTimeout(timeout, TimeUnit.SECONDS);
    }

    private CompletableFuture<Void> beginSslHandshake() {
        try {
            if(sslEngine == null) {
                return CompletableFuture.completedFuture(null);
            }

            if(sslHandshakeCompled.get()) {
                return CompletableFuture.completedFuture(null);
            }

            var future = new CompletableFuture<Void>();
            sslEngine.beginHandshake();
            sslReadBuffer.position(sslReadBuffer.limit());
            handleSslHandshakeStatus(null, future);
            return future;
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private void handleSslHandshakeStatus(Status status, CompletableFuture<Void> future){
        switch (sslEngine.getHandshakeStatus()) {
            case NEED_WRAP -> doSslHandshakeWrap(future);
            case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> doSslHandshakeUnwrap(future, status == Status.BUFFER_UNDERFLOW);
            case NEED_TASK -> doSslHandshakeTasks(future);
            case FINISHED -> finishSslHandshake(future);
            case NOT_HANDSHAKING -> future.completeExceptionally(new IOException("Cannot complete handshake"));
        }
    }

    private void finishSslHandshake(CompletableFuture<Void> future) {
        future.complete(null);
        sslHandshakeCompled.set(true);
    }

    private void doSslHandshakeTasks(CompletableFuture<Void> future) {
        Runnable runnable;
        while ((runnable = sslEngine.getDelegatedTask()) != null) {
            runnable.run();
        }

        handleSslHandshakeStatus(null, future);
    }

    private void doSslHandshakeUnwrap(CompletableFuture<Void> future, boolean forceRead) {
        sslReadBuffer.compact();
        if (!forceRead && sslReadBuffer.position() != 0) {
            doSSlHandshakeUnwrapOperation(future);
            return;
        }

        delegate.read(sslReadBuffer, null, new CompletionHandler<>() {
            @Override
            public void completed(Integer result, Object attachment) {
                doSSlHandshakeUnwrapOperation(future);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                future.completeExceptionally(exc);
            }
        });
    }

    private void doSSlHandshakeUnwrapOperation(CompletableFuture<Void> future) {
        try {
            sslReadBuffer.flip();
            var result = sslEngine.unwrap(sslReadBuffer, sslOutputBuffer);
            if(isHandshakeFinished(result, false)) {
                finishSslHandshake(future);
            }else {
                handleSslHandshakeStatus(result.getStatus(), future);
            }
        }catch(Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }

    private void doSslHandshakeWrap(CompletableFuture<Void> future) {
        try {
            sslWriteBuffer.clear();
            var result = sslEngine.wrap(sslOutputBuffer, sslWriteBuffer);
            var isHandshakeFinished = isHandshakeFinished(result, true);
            sslWriteBuffer.flip();
            delegate.write(sslWriteBuffer, null, new CompletionHandler<>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    if(isHandshakeFinished) {
                        finishSslHandshake(future);
                    }else {
                        handleSslHandshakeStatus(null, future);
                    }
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    future.completeExceptionally(exc);
                }
            });
        }catch (Throwable throwable) {
            future.completeExceptionally(throwable);
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
    public InputStream getInputStream() throws IOException {
        if(!isConnected()) {
            throw new IOException("Connection is closed");
        }

        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if(!isConnected()) {
            throw new IOException("Connection is closed");
        }

        return outputStream;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean isBound() {
        return delegate.isOpen();
    }

    @Override
    public boolean isConnected() {
        return delegate.isOpen();
    }

    @Override
    public boolean isOutputShutdown() {
        return !delegate.isOpen();
    }

    @Override
    public boolean isInputShutdown() {
        return !delegate.isOpen();
    }

    @Override
    public boolean isClosed() {
        return !delegate.isOpen();
    }

    @Override
    public int getReceiveBufferSize() {
        try {
            return delegate.getOption(StandardSocketOptions.SO_RCVBUF);
        } catch (Throwable e) {
            return 0;
        }
    }

    @Override
    public int getSendBufferSize() {
        try {
            return delegate.getOption(StandardSocketOptions.SO_SNDBUF);
        } catch (Throwable e) {
            return 0;
        }
    }

    @Override
    public void bind(SocketAddress endpoint) throws IOException {
        throw new UnsupportedOperationException("Client socket");
    }

    @Override
    public int getPort() {
        try {
            if(delegate.getRemoteAddress() instanceof InetSocketAddress inetSocketAddress) {
                return inetSocketAddress.getPort();
            }

            return -1;
        } catch (Throwable e) {
            return -1;
        }
    }

    @Override
    public int getLocalPort() {
        return -1;
    }

    @Override
    public InetAddress getInetAddress() {
        try {
            if(delegate.getRemoteAddress() instanceof InetSocketAddress inetSocketAddress) {
                return inetSocketAddress.getAddress();
            }

            return null;
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public InetAddress getLocalAddress() {
        return null;
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        try {
            return delegate.getRemoteAddress();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return null;
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        if(!supportedOptions().contains(StandardSocketOptions.TCP_NODELAY)) {
            return;
        }

        try {
            delegate.setOption(StandardSocketOptions.TCP_NODELAY, on);
        } catch (IOException e) {
            throw new SocketException(e);
        }
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        try {
            return getOption(StandardSocketOptions.TCP_NODELAY);
        } catch (IOException e) {
            throw new SocketException(e);
        }
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        if(!supportedOptions().contains(StandardSocketOptions.SO_LINGER)) {
            return;
        }

        try {
            if(on) {
                delegate.setOption(StandardSocketOptions.SO_LINGER, linger);
            }else {
                delegate.setOption(StandardSocketOptions.SO_LINGER, -1);
            }
        }catch (IOException e) {
            throw new SocketException(e);
        }
    }

    @Override
    public int getSoLinger() {
        try {
            return getOption(StandardSocketOptions.SO_LINGER);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Override
    public void sendUrgentData(int data) throws SocketException {
        try {
            var future = writeAsync(new byte[]{(byte) data});
            future.join();
        }catch (Throwable throwable) {
            throw new SocketException(throwable);
        }
    }

    @Override
    public void setOOBInline(boolean on) {

    }

    @Override
    public boolean getOOBInline() {
        return false;
    }

    @Override
    public void setSoTimeout(int timeout) {

    }

    @Override
    public int getSoTimeout() {
        return 0;
    }

    @Override
    public void setSendBufferSize(int size) throws SocketException {
        if(!supportedOptions().contains(StandardSocketOptions.SO_SNDBUF)) {
            return;
        }

        try {
            delegate.setOption(StandardSocketOptions.SO_SNDBUF, size);
        } catch (IOException e) {
            throw new SocketException(e);
        }
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        if(!supportedOptions().contains(StandardSocketOptions.SO_RCVBUF)) {
            return;
        }

        try {
            delegate.setOption(StandardSocketOptions.SO_RCVBUF, size);
        } catch (IOException e) {
            throw new SocketException(e);
        }
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        if(!supportedOptions().contains(StandardSocketOptions.SO_KEEPALIVE)) {
            return;
        }

        try {
            delegate.setOption(StandardSocketOptions.SO_KEEPALIVE, on);
        } catch (IOException e) {
            throw new SocketException(e);
        }
    }

    @Override
    public boolean getKeepAlive() {
        try {
            return getOption(StandardSocketOptions.SO_KEEPALIVE);
        } catch (IOException ignored) {
            return false;
        }
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        if(!supportedOptions().contains(StandardSocketOptions.IP_TOS)) {
            return;
        }

        try {
            delegate.setOption(StandardSocketOptions.IP_TOS, tc);
        } catch (IOException e) {
            throw new SocketException(e);
        }
    }

    @Override
    public int getTrafficClass() throws SocketException {
        try {
            return getOption(StandardSocketOptions.IP_TOS);
        } catch (IOException e) {
            throw new SocketException(e);
        }
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        if(!supportedOptions().contains(StandardSocketOptions.SO_REUSEADDR)) {
            return;
        }

        try {
            delegate.setOption(StandardSocketOptions.SO_REUSEADDR, on);
        } catch (IOException e) {
            throw new SocketException(e);
        }
    }

    @Override
    public boolean getReuseAddress() {
        try {
            return getOption(StandardSocketOptions.SO_REUSEADDR);
        } catch (IOException ignored) {
            return false;
        }
    }

    @Override
    public void shutdownInput() throws IOException {
        delegate.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        delegate.shutdownOutput();
    }

    @Override
    public <T> java.net.Socket setOption(SocketOption<T> name, T value) throws IOException {
        delegate.setOption(name, value);
        return this;
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return delegate.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return delegate.supportedOptions();
    }

    public CompletableFuture<Void> writeAsync(byte[] data) {
        return writeAsync(data, 0, data.length);
    }

    @SuppressWarnings("SameParameterValue")
    private CompletableFuture<Void> writeAsync(byte[] data, int offset, int length) {
        return writeAsync(ByteBuffer.wrap(data, offset, length));
    }

    private CompletableFuture<Void> writeAsync(ByteBuffer buffer) {
        var future = new CompletableFuture<Void>();
        try {
            if(sslEngine == null || !sslHandshakeCompled.get()) {
                doPlainWrite(buffer, future);
            }else {
                doSecureWrite(buffer, future);
            }
        }catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
        return future;
    }

    private void doPlainWrite(ByteBuffer buffer, CompletableFuture<Void> future) {
        delegate.write(buffer, null, new CompletionHandler<>() {
            @Override
            public void completed(Integer result, Object attachment) {
                future.complete(null);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                future.completeExceptionally(exc);
            }
        });
    }

    private void doSecureWrite(ByteBuffer buffer, CompletableFuture<Void> future) throws SSLException {
        sslWriteBuffer.clear();
        var result = sslEngine.wrap(buffer, sslWriteBuffer);
        var status = result.getStatus();
        if (status != Status.OK && status != Status.BUFFER_OVERFLOW) {
            throw new IllegalStateException("SSL wrap failed with status: " + status);
        }

        sslWriteBuffer.flip();
        doPlainWrite(sslWriteBuffer, future);
    }

    public void readFullyAsync(int length, Consumer<ByteBuffer> callback, Consumer<Throwable> errorHandler) {
        if (length < 0) {
            throw new IllegalArgumentException("Cannot read %s bytes from socket".formatted(length));
        }

        var buffer = ByteBuffer.allocate(length);
        readFullyAsync(buffer, null, callback, errorHandler);
    }

    public CompletableFuture<ByteBuffer> readFullyAsync(int length) {
        if (length < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Cannot read %s bytes from socket".formatted(length)));
        }

        var buffer = ByteBuffer.allocate(length);
        var future = new CompletableFuture<ByteBuffer>();
        readFullyAsync(buffer, future, null, null);
        return future;
    }

    private void readFullyAsync(ByteBuffer buffer, CompletableFuture<ByteBuffer> future, Consumer<ByteBuffer> callback, Consumer<Throwable> errorHandler) {
        try {
            readAsync(
                    buffer,
                    false,
                    null,
                    (result) -> {
                        if(result == -1) {
                            if(errorHandler != null) {
                                errorHandler.accept(new EOFException());
                            }else {
                                future.completeExceptionally(new EOFException());
                            }
                            return;
                        }
                        
                        if (buffer.hasRemaining()) {
                            readFullyAsync(buffer, future, callback, errorHandler);
                            return;
                        }

                        buffer.flip();
                        if (callback != null) {
                            callback.accept(buffer);
                        } else {
                            future.complete(buffer);
                        }
                    },
                    errorHandler
            );
        }catch (Throwable throwable) {
            if(future != null) {
                future.completeExceptionally(throwable);
            }else {
                errorHandler.accept(throwable);
            }
        }
    }

    public CompletableFuture<ByteBuffer> readAsync(int length) {
        if (length < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Cannot read %s bytes from socket".formatted(length)));
        }

        var buffer = ByteBuffer.allocate(length);
        return readAsync(buffer)
                .thenApply(read -> buffer);
    }

    public CompletableFuture<Integer> readAsync(ByteBuffer buffer) {
        var future = new CompletableFuture<Integer>();
        try {
            readAsync(buffer, true, future, null, null);
        }catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
        return future;
    }

    private void readAsync(ByteBuffer buffer, boolean lastRead, CompletableFuture<Integer> future, IntConsumer callback, Consumer<Throwable> errorHandler) {
        if(sslEngine == null || !sslHandshakeCompled.get()) {
            doPlainRead(buffer, lastRead, future, callback, errorHandler);
        }else {
            doSecureRead(buffer, lastRead, 0, !sslReadBuffer.hasRemaining(), future, callback, errorHandler);
        }
    }

    private void doSecureRead(ByteBuffer buffer, boolean lastRead, int bytesRead, boolean forceRead, CompletableFuture<Integer> future, IntConsumer callback, Consumer<Throwable> errorHandler) {
        var oldLimit = sslOutputBuffer.limit();
        var oldPosition = sslOutputBuffer.position();
        sslOutputBuffer.position(sslOutputBufferReadPosition);
        sslOutputBuffer.limit(oldPosition);
        while (buffer.hasRemaining() && sslOutputBuffer.hasRemaining()) {
            buffer.put(sslOutputBuffer.get());
            bytesRead++;
            sslOutputBufferReadPosition++;
        }
        if(sslOutputBufferReadPosition >= oldLimit) {
            sslOutputBuffer.position(0);
            sslOutputBuffer.limit(sslOutputBuffer.capacity());
            this.sslOutputBufferReadPosition = 0;
        }else {
            sslOutputBuffer.limit(oldLimit);
            sslOutputBuffer.position(oldPosition);
        }

        if(!buffer.hasRemaining()) {
            if(callback != null) {
                callback.accept(bytesRead);
            }else {
                future.complete(bytesRead);
            }
            return;
        }

        sslReadBuffer.compact();
        if (!forceRead) {
            doSecureRead(buffer, lastRead, bytesRead, future, callback, errorHandler);
            return;
        }

        var finalBytesRead = bytesRead;
        delegate.read(sslReadBuffer, null, new CompletionHandler<>() {
            @Override
            public void completed(Integer result, Object attachment) {
                if (result != -1) {
                    sslReadBuffer.flip();
                    doSecureRead(buffer, lastRead, finalBytesRead, future, callback, errorHandler);
                    return;
                }

                if(future != null) {
                    future.completeExceptionally(new EOFException());
                }else {
                    errorHandler.accept(new EOFException());
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                if(future != null) {
                    future.completeExceptionally(exc);
                }else {
                    errorHandler.accept(exc);
                }
            }
        });
    }

    private void doSecureRead(ByteBuffer buffer, boolean lastRead, int read, CompletableFuture<Integer> future, IntConsumer callback, Consumer<Throwable> errorHandler) {
        try {
            if (!sslReadBuffer.hasRemaining()) {
                doSecureRead(buffer, lastRead, read, true, future, callback, errorHandler);
                return;
            }

            var result = sslEngine.unwrap(sslReadBuffer, sslOutputBuffer);
            if (result.getStatus() != Status.OK && result.getStatus() != Status.BUFFER_UNDERFLOW) {
                var error = new IllegalStateException("SSL read operation failed with status: " + result.getStatus());
                if(future != null) {
                    future.completeExceptionally(error);
                }else {
                    errorHandler.accept(error);
                }
                return;
            }

            doSecureRead(buffer, lastRead, read, result.bytesProduced() == 0, future, callback, errorHandler);
        }catch (Throwable throwable) {
            if(future != null) {
                future.completeExceptionally(throwable);
            }else {
                errorHandler.accept(throwable);
            }
        }
    }

    private void doPlainRead(ByteBuffer buffer, boolean lastRead, CompletableFuture<Integer> future, IntConsumer callback, Consumer<Throwable> errorHandler) {
        delegate.read(buffer, null, new CompletionHandler<>() {
            @Override
            public void completed(Integer result, Object attachment) {
                if(lastRead) {
                    buffer.flip();
                }

                if(future != null) {
                    future.complete(result);
                }else {
                    callback.accept(result);
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                if(future != null) {
                    future.completeExceptionally(exc);
                }else {
                    errorHandler.accept(exc);
                }
            }
        });
    }

    private class BlockingSocketInputStream extends InputStream {
        @Override
        public int read() throws EOFException {
            var data = new byte[1];
            var result = read(data);
            if(result == -1) {
                throw new EOFException();
            }

            return Byte.toUnsignedInt(data[0]);
        }

        @Override
        public int read(byte[] b) {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if(len == 0) {
                return 0;
            }

            return readAsync(ByteBuffer.wrap(b, off, len))
                    .join();
        }
    }

    private class BlockingSocketOutputStream extends OutputStream {
        @Override
        public void write(int b) {
            write(new byte[]{(byte) b});
        }

        @Override
        public void write(byte[] b) {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            writeAsync(b, off, len).join();
        }
    }

    private static final class Direct extends Socket {
        Direct(SSLEngine engine) throws IOException {
            super(engine, null);
        }

        @Override
        public CompletableFuture<Void> connectAsync(InetSocketAddress address, int timeout) {
            return super.connectAsync(address, timeout)
                    .thenComposeAsync(proxyResult -> super.beginSslHandshake());
        }
    }

    private static final class Http extends Socket {
        private static final int DEFAULT_RCV_BUF = 1024;
        private static final int OK_STATUS_CODE = 200;

        Http(SSLEngine engine, URI proxy) throws IOException {
            super(engine, proxy);
        }

        @Override
        public CompletableFuture<Void> connectAsync(InetSocketAddress address, int timeout) {
            return super.connectAsync(new InetSocketAddress(proxy.getHost(), proxy.getPort()), timeout)
                    .thenComposeAsync(openResult -> sendAuthentication(address))
                    .thenComposeAsync(connectionResult -> readAuthenticationResponse())
                    .thenComposeAsync(this::handleAuthentication)
                    .thenComposeAsync(proxyResult -> super.beginSslHandshake())
                    .orTimeout(timeout, TimeUnit.SECONDS);
        }

        private CompletableFuture<Void> handleAuthentication(String response) {
            var responseParts = response.split(" ");
            var statusCodePart = responseParts.length < 2 ? null : responseParts[1];
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
            return readAsync(readReceiveBufferSize())
                    .thenApplyAsync(this::readResponse);
        }

        private String readResponse(ByteBuffer response) {
            var data = new byte[response.limit()];
            response.get(data);
            return new String(data);
        }

        private Integer readReceiveBufferSize() {
            try {
                return delegate.getOption(StandardSocketOptions.SO_RCVBUF);
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
            var data = builder.toString().getBytes();
            var future = new CompletableFuture<Void>();
            delegate.write(ByteBuffer.wrap(data), null, new CompletionHandler<>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    future.complete(null);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    future.completeExceptionally(exc);
                }
            });
            return future;
        }
    }

    private static final class Socks5 extends Socket {
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

        private Socks5(SSLEngine engine, URI proxy) throws IOException {
            super(engine, proxy);
        }

        @Override
        public CompletableFuture<Void> connectAsync(InetSocketAddress address, int timeout) {
            return super.connectAsync(new InetSocketAddress(proxy.getHost(), proxy.getPort()), timeout)
                    .thenComposeAsync(openResult -> writeAsync(getAuthenticationPayload()))
                    .thenComposeAsync(connectionResult -> readFullyAsync(2))
                    .thenComposeAsync(response -> onConnected(address, response))
                    .thenComposeAsync(proxyResult -> super.beginSslHandshake());
        }

        private byte[] getAuthenticationPayload() {
            var connectionPayload = new ByteArrayOutputStream();
            connectionPayload.write(VERSION_5);
            connectionPayload.write(2);
            connectionPayload.write(NO_AUTH);
            connectionPayload.write(USER_PASSW);
            return connectionPayload.toByteArray();
        }

        private CompletionStage<Void> onConnected(InetSocketAddress address, ByteBuffer response) {
            var socksVersion = response.get();
            if (socksVersion != VERSION_5) {
                return CompletableFuture.failedFuture(new SocketException("SOCKS : Invalid version"));
            }

            var method = response.get();
            if (method == NO_METHODS) {
                return CompletableFuture.failedFuture(new SocketException("SOCKS : No acceptable methods"));
            }

            if (method == NO_AUTH) {
                return onAuthentication(address, null);
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
            return writeAsync(outputStream.toByteArray())
                    .thenComposeAsync(connectionResult -> readFullyAsync(2))
                    .thenComposeAsync(connectionResponse -> onAuthentication(address, connectionResponse));
        }

        private CompletableFuture<Void> onAuthentication(InetSocketAddress address, ByteBuffer connectionResponse) {
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
            return writeAsync(outputStream.toByteArray())
                    .thenComposeAsync(authenticationResult -> readFullyAsync(4))
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
                case IPV4 -> CompletableFuture.allOf(readOrThrow(4, "Cannot read IPV4 address"), readOrThrow(2, "Cannot read IPV4 port"));
                case DOMAIN_NAME -> readFullyAsync(1)
                        .exceptionallyCompose(ignored -> CompletableFuture.failedFuture(new SocketException("Cannot read domain name")))
                        .thenComposeAsync(domainLengthBuffer -> {
                            var domainLength = Byte.toUnsignedInt(domainLengthBuffer.get());
                            return CompletableFuture.allOf(readOrThrow(domainLength, "Cannot read domain"), readOrThrow(2, "Cannot read domain port"));
                        })
                        .exceptionallyCompose(ignored -> CompletableFuture.failedFuture(new SocketException("Cannot read domain")));
                case IPV6 -> CompletableFuture.allOf(readOrThrow(16, "Cannot read IPV6 address"), readOrThrow(2, "Cannot read IPV6 port"));
                default -> CompletableFuture.failedFuture(new SocketException("Reply from SOCKS server contains wrong code"));
            };
        }

        private CompletableFuture<?> readOrThrow(int length, String message) {
            return readFullyAsync(length)
                    .exceptionallyCompose(error -> CompletableFuture.failedFuture(new SocketException(message)));
        }
    }
}

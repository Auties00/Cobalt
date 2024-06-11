package it.auties.whatsapp.net;

import it.auties.whatsapp.util.Proxies;

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

public sealed abstract class AsyncSocket extends Socket {
    public static AsyncSocket of(URI proxy) throws IOException {
        return switch (Proxies.toProxy(proxy).type()) {
            case DIRECT -> new Direct();
            case HTTP -> new Http(proxy);
            case SOCKS -> new Socks5(proxy);
        };
    }

    final AsynchronousSocketChannel delegate;
    final URI proxy;
    AsyncSocket(URI proxy) throws IOException {
        this.delegate = AsynchronousSocketChannel.open();
        delegate.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        this.proxy = proxy;
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, -1);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if(!(endpoint instanceof InetSocketAddress inetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }

        var future = connectAsync(inetSocketAddress);
        future.join();
    }

    public CompletableFuture<Void> connectAsync(InetSocketAddress address) {
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
        return future;
    }

    @Override
    public void close() {
        try {
            delegate.close();
        }catch (Throwable ignored) {
            // Ignore
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if(!isConnected()) {
            throw new IOException("Connection is closed");
        }

        return new InputStream() {
            @Override
            public int read() throws IOException {
                try {
                    var future = readAsync(1);
                    var result = future.join();
                    if(result.limit() < 1) {
                        return -1;
                    }

                    return Byte.toUnsignedInt(result.get());
                }catch (Throwable throwable) {
                    throw new IOException(throwable.getMessage());
                }
            }

            @Override
            public int read(byte[] b) {
                return read(b, 0, b.length);
            }

            @Override
            public int read(byte[] b, int off, int len) {
                var future = new CompletableFuture<Integer>();
                try {
                    var buffer = ByteBuffer.wrap(b, off, len);
                    delegate.read(buffer, null, new CompletionHandler<>() {
                        @Override
                        public void completed(Integer result, Object attachment) {
                            buffer.flip();
                            future.complete(result);
                        }

                        @Override
                        public void failed(Throwable exc, Object attachment) {
                            future.completeExceptionally(exc);
                        }
                    });
                }catch (Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
                return future.join();
            }
        };
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if(!isConnected()) {
            throw new IOException("Connection is closed");
        }

        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                write(new byte[]{(byte) b});
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                try {
                    var future = sendAsync(b, off, len);
                    future.join();
                }catch (Throwable throwable) {
                    throw new IOException(throwable.getMessage());
                }
            }
        };
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
    public void setTcpNoDelay(boolean on) {
        try {
            delegate.setOption(StandardSocketOptions.TCP_NODELAY, on);
        } catch (Throwable ignored) {
            // Not supported
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
    public void setSoLinger(boolean on, int linger) {
        try {
            if(on) {
                delegate.setOption(StandardSocketOptions.SO_LINGER, linger);
            }else {
                delegate.setOption(StandardSocketOptions.SO_LINGER, -1);
            }
        } catch (Throwable ignored) {
            // Not supported
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
    public void sendUrgentData(int data) throws IOException {
        try {
            var future = sendAsync(new byte[]{(byte) data});
            future.join();
        }catch (Throwable throwable) {
            throw new IOException(throwable.getMessage());
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
    public void setSendBufferSize(int size) {
        try {
            delegate.setOption(StandardSocketOptions.SO_SNDBUF, size);
        } catch (Throwable ignored) {
            // Not supported
        }
    }

    @Override
    public void setReceiveBufferSize(int size) {
        try {
            delegate.setOption(StandardSocketOptions.SO_RCVBUF, size);
        } catch (Throwable ignored) {
            // Not supported
        }
    }

    @Override
    public void setKeepAlive(boolean on) {
        try {
            delegate.setOption(StandardSocketOptions.SO_KEEPALIVE, on);
        } catch (Throwable ignored) {
            // Not supported
        }
    }

    @Override
    public boolean getKeepAlive() {
        try {
            return getOption(StandardSocketOptions.SO_KEEPALIVE);
        } catch (Throwable ignored) {
           return false;
        }
    }

    @Override
    public void setTrafficClass(int tc) {
        try {
            delegate.setOption(StandardSocketOptions.IP_TOS, tc);
        } catch (Throwable ignored) {
            // Not supported
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
    public void setReuseAddress(boolean on) {
        try {
            delegate.setOption(StandardSocketOptions.SO_REUSEADDR, on);
        } catch (Throwable ignored) {
            // Not supported
        }
    }

    @Override
    public boolean getReuseAddress() {
        try {
            return getOption(StandardSocketOptions.SO_REUSEADDR);
        } catch (Throwable ignored) {
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
    public <T> Socket setOption(SocketOption<T> name, T value) throws IOException {
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

    public CompletableFuture<Void> sendAsync(byte[] data) {
        return sendAsync(data, 0, data.length);
    }

    private CompletableFuture<Void> sendAsync(byte[] data, int offset, int length) {
        var future = new CompletableFuture<Void>();
        try {
            delegate.write(ByteBuffer.wrap(data, offset, length), null, new CompletionHandler<>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    future.complete(null);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    future.completeExceptionally(exc);
                }
            });
        }catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
        return future;
    }

    public CompletableFuture<ByteBuffer> readAsync(int length) {
        if (length < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Cannot read %s bytes from socket".formatted(length)));
        }

        var future = new CompletableFuture<ByteBuffer>();
        try {
            var buffer = ByteBuffer.allocate(length);
            delegate.read(buffer, null, new CompletionHandler<>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    buffer.flip();
                    future.complete(buffer);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    future.completeExceptionally(exc);
                }
            });
        }catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
        return future;
    }

    public CompletableFuture<ByteBuffer> readFullyAsync(int length) {
        if (length < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Cannot read %s bytes from socket".formatted(length)));
        }

        var buffer = ByteBuffer.allocate(length);
        return readFullyAsync(buffer);
    }

    public CompletableFuture<ByteBuffer> readFullyAsync(ByteBuffer buffer) {
        var future = new CompletableFuture<ByteBuffer>();
        readFully(buffer, future);
        return future;
    }

    private void readFully(ByteBuffer buffer, CompletableFuture<ByteBuffer> future) {
        try {
            delegate.read(buffer, null, new CompletionHandler<>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    if(result == -1 || !buffer.hasRemaining()) {
                        buffer.flip();
                        future.complete(buffer);
                        return;
                    }

                    readFully(buffer, future);
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

    public AsynchronousSocketChannel channel() {
        return delegate;
    }

    private static final class Direct extends AsyncSocket {
        Direct() throws IOException {
            super(null);
        }
    }

    private static final class Http extends AsyncSocket {
        private static final int DEFAULT_RCV_BUF = 1024;
        private static final int OK_STATUS_CODE = 200;

        Http(URI proxy) throws IOException {
            super(proxy);
        }

        @Override
        public CompletableFuture<Void> connectAsync(InetSocketAddress address) {
            return super.connectAsync(new InetSocketAddress(proxy.getHost(), proxy.getPort()))
                    .thenComposeAsync(openResult -> sendHandshake(address))
                    .thenComposeAsync(connectionResult -> readHandshakeResponse())
                    .thenComposeAsync(this::handleHandshake);
        }

        private CompletableFuture<Void> handleHandshake(String response) {
            var responseParts = response.split(" ");
            var statusCodePart = responseParts[1];
            try {
                var statusCode = Integer.parseUnsignedInt(statusCodePart);
                if(statusCode != OK_STATUS_CODE) {
                    return CompletableFuture.failedFuture(new SocketException("HTTP : Cannot connect to proxy, status code " + statusCode));
                }

                return CompletableFuture.completedFuture(null);
            }catch (Throwable throwable) {
                return CompletableFuture.failedFuture(new SocketException("HTTP : Cannot connect to proxy: " + response));
            }
        }

        private CompletableFuture<String> readHandshakeResponse() {
            return readFullyAsync(readReceiveBufferSize())
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

        private CompletableFuture<Void> sendHandshake(InetSocketAddress endpoint) {
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
            return sendAsync(data);
        }
    }

    private static final class Socks5 extends AsyncSocket {
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

        private Socks5(URI proxy) throws IOException {
            super(proxy);
        }

        @Override
        public CompletableFuture<Void> connectAsync(InetSocketAddress address) {
            return super.connectAsync(new InetSocketAddress(proxy.getHost(), proxy.getPort()))
                    .thenComposeAsync(openResult -> sendAsync(getHandshakePayload()))
                    .thenComposeAsync(connectionResult -> readFullyAsync(2))
                    .thenComposeAsync(response -> onConnected(address, response));
        }

        private byte[] getHandshakePayload() {
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
            return sendAsync(outputStream.toByteArray())
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
            return sendAsync(outputStream.toByteArray())
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

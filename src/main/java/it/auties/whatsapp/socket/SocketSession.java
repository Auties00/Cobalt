package it.auties.whatsapp.socket;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

abstract sealed class SocketSession {
    private static final String HOST_NAME = "g.whatsapp.net";
    private static final int PORT = 443;
    private static final int MAX_MESSAGE_LENGTH = 1048576;
    private static final int DEFAULT_RCV_BUF = 8192;

    SocketChannel channel;

    static SocketSession of(URI proxy) {
        if(proxy == null) {
            return new DirectSession();
        }

        var scheme = proxy.getScheme();
        Objects.requireNonNull(scheme, "Malformed proxy: scheme cannot be null");
        return switch (scheme.toLowerCase()) {
            case "http", "https" -> new ProxiedHttpSession(proxy);
            case "socks5", "socks5h" -> new ProxiedSocksSession(proxy);
            default -> throw new IllegalArgumentException("Malformed proxy: unknown scheme " + scheme);
        };
    }

    void connect(Consumer<ByteBuffer> onMessage) {
        if (isConnected()) {
            return;
        }
        var endpoint = new InetSocketAddress(HOST_NAME, PORT); // Don't resolve this statically
        connect(endpoint, onMessage);
    }

    private void connect(InetSocketAddress endpoint, Consumer<ByteBuffer> onMessage) {
        try {
            this.channel = SocketChannel.open();
            channel.configureBlocking(false);
            var context = new ConnectionContext(onMessage);
            if (channel.connect(endpoint)) {
                CentralSelector.INSTANCE.register(channel, SelectionKey.OP_READ, context);
            } else {
                CentralSelector.INSTANCE.register(channel, SelectionKey.OP_CONNECT, context);
                synchronized (context.connectionLock) {
                    context.connectionLock.wait();
                }
            }
        }catch (Throwable exception) {
            throw new RuntimeException("Cannot connect to socket", exception);
        }
    }


    void disconnect() {
        if(!isConnected()) {
            return;
        }

        CentralSelector.INSTANCE.unregister(channel);

        try {
            channel.close();
        }catch (IOException ignored) {

        }finally {
            channel = null;
        }
    }

    boolean isConnected() {
        return channel != null && channel.isConnected();
    }

    void sendBinary(byte[] bytes) {
        if (!isConnected()) {
            throw new IllegalStateException("Socket is not connected");
        }

        CentralSelector.INSTANCE
                .addWrite(channel, ByteBuffer.wrap(bytes));
    }

    private static final class DirectSession extends SocketSession {

    }

    private static final class ProxiedHttpSession extends SocketSession {
        private static final char CARRIAGE_RETURN = '\r';
        private static final char LINE_FEED = '\n';
        private static final char SPACE = ' ';

        private final URI proxy;

        public ProxiedHttpSession(URI proxy) {
            this.proxy = proxy;
        }

        @Override
        void connect(Consumer<ByteBuffer> onMessage) {
            var host = proxy.getHost();
            var port = proxy.getPort();
            if(port == -1) {
                port = switch (proxy.getScheme().toLowerCase()) {
                    case "http" -> 80;
                    case "https" -> 443;
                    default -> throw new InternalError();
                };
            }
            super.connect(new InetSocketAddress(host, port), onMessage);
            authenticate(host, port);
        }

        private void authenticate(String host, int port) {
            try {
                sendAuthenticationRequest(host, port);
                handleAuthenticationResoinse();
            }catch (IOException exception) {
                throw new UncheckedIOException("Failed to authenticate with proxy", exception);
            }
        }

        private void sendAuthenticationRequest(String host, int port) throws IOException {
            var builder = new StringBuilder();
            builder.append("CONNECT ")
                    .append(host)
                    .append(":")
                    .append(port)
                    .append(" HTTP/1.1\r\n");
            builder.append("Host: ")
                    .append(host)
                    .append(":")
                    .append(port)
                    .append("\r\n");
            var authInfo = proxy.getUserInfo();
            if (authInfo != null) {
                builder.append("Proxy-Authorization: Basic ")
                        .append(Base64.getEncoder().encodeToString(authInfo.getBytes()))
                        .append("\r\n");
            }
            builder.append("\r\n");
            channel.write(ByteBuffer.wrap(builder.toString().getBytes()));
        }

        // Optimized method that just tries to confirm we got a 200 status code
        // Skips everything else
        private void handleAuthenticationResoinse() throws IOException {
            // Allocate the stuff we need
            var buffer = allocateReadBuffer();

            // Skip junk before the actual HTTP response starts
            // Not always necessary
            do {
                var read = channel.read(buffer.position(0));
                if(read == -1) {
                    throw unexpectedEndOfStream();
                }
            } while (!skipJunk(buffer));

            // Make sure we have at least enough data to parse the HTTP version and status code
            var start = buffer.position();
            while (buffer.position() - start < 12) {
                var read = channel.read(buffer);
                if(read == -1) {
                    throw unexpectedEndOfStream();
                }
            }

            // Make the buffer readable
            buffer.limit(buffer.position());
            buffer.position(start);

            // Parse the HTTP/ part
            if(buffer.get() != 'H'
                    || buffer.get() != 'T'
                    || buffer.get() != 'T'
                    || buffer.get() != 'P'
                    || buffer.get() != '/') {
                throw invalidResponse(buffer, start);
            }

            // Parse the HTTP version, we don't care about receiving a specific version back
            var major = buffer.get() - '0';
            if(major < 0 || major > 9) {
                throw invalidResponse(buffer, start);
            }
            var versionSeparator = buffer.get();
            if(versionSeparator == '.'){
                var minor = buffer.get() - '0';
                if(minor < 0 || minor > 9) {
                    throw invalidResponse(buffer, start);
                }
                versionSeparator = buffer.get();
            }
            if(versionSeparator != SPACE) {
                throw invalidResponse(buffer, start);
            }

            // Make sure we are getting a 200 status code
            var statusCodeFirstDigit = buffer.get() - '0';
            if(statusCodeFirstDigit != 2) {
                throw invalidResponse(buffer, start);
            }
            var statusCodeSecondDigit = buffer.get() - '0';
            if(statusCodeSecondDigit != 0) {
                throw invalidResponse(buffer, start);
            }
            var statusCodeThirdDigit = buffer.get() - '0';
            if(statusCodeThirdDigit != 0) {
                throw invalidResponse(buffer, start);
            }

            // Read the payload until the HTTP response ends (\r\n\r\n)
            // If the stream end, throw an error
            byte current;
            while (isConnected()) {
                if(buffer.remaining() < 4) {
                    buffer.limit(buffer.capacity());
                    buffer.position(0);
                    while (buffer.remaining() < 4) {
                        var read = channel.read(buffer);
                        if(read == -1) {
                            throw unexpectedEndOfStream();
                        }
                    }
                    buffer.flip();
                }

                current = buffer.get();
                if (current != CARRIAGE_RETURN) {
                    continue;
                }

                current = buffer.get();
                if(current != LINE_FEED) {
                    continue;
                }

                current = buffer.get();
                if (current != CARRIAGE_RETURN) {
                    continue;
                }

                current = buffer.get();
                if(current != LINE_FEED) {
                    continue;
                }

                return;
            }
            throw unexpectedEndOfStream();
        }

        private boolean skipJunk(ByteBuffer buffer) {
            while (buffer.hasRemaining()) {
                var current = buffer.get();
                if (current != SPACE && current != CARRIAGE_RETURN && current != LINE_FEED) {
                    buffer.position(buffer.position() - 1);
                    return true;
                }
            }
            return false;
        }

        private ByteBuffer allocateReadBuffer() {
            try {
                return ByteBuffer.allocate(channel.getOption(StandardSocketOptions.SO_RCVBUF));
            }catch (IOException exception) {
                return ByteBuffer.allocate(DEFAULT_RCV_BUF);
            }
        }

        private static IOException unexpectedEndOfStream() {
            return new IOException("Unexpected end of stream");
        }

        private static IOException invalidResponse(ByteBuffer buffer, int offset) {
            return new IOException("Invalid HTTP response: " + new String(buffer.array(), offset, buffer.limit() - offset));
        }
    }

    private static final class ProxiedSocksSession extends SocketSession {
        private static final byte SOCKS_VERSION_5 = 0x05;
        private static final byte METHOD_NO_AUTH = 0x00;
        private static final byte METHOD_USER_PASS = 0x02;
        private static final byte CMD_CONNECT = 0x01;
        private static final byte ADDR_TYPE_DOMAIN = 0x03;
        private static final byte AUTH_VERSION_1 = 0x01;
        private static final byte AUTH_SUCCESS = 0x00;
        private static final byte REPLY_SUCCESS = 0x00;
        private static final int IPV4_REPLY = 0x01;
        private static final int DOMAIN_REPLY = 0x03;
        private static final int IPV6_REPLY = 0x04;

        private final URI proxy;

        public ProxiedSocksSession(URI proxy) {
            this.proxy = proxy;
        }

        @Override
        void connect(Consumer<ByteBuffer> onMessage) {
            var proxyHost = proxy.getHost();
            var proxyPort = proxy.getPort() == -1 ? 1080 : proxy.getPort();
            super.connect(new InetSocketAddress(proxyHost, proxyPort), onMessage);
            authenticate(proxyHost, proxyPort);
        }

        private void authenticate(String proxyHost, int proxyPort) {
            try {
                sendAuthenticationRequest(proxyHost, proxyPort);
                handleAuthenticationResponse();
            }catch (IOException exception) {
                throw new UncheckedIOException("Failed to authenticate with proxy", exception);
            }
        }

        private void sendAuthenticationRequest(String proxyHost, int proxyPort) throws IOException {
            var greeting = ByteBuffer.wrap(new byte[]{
                    SOCKS_VERSION_5,
                    2,
                    METHOD_NO_AUTH,
                    METHOD_USER_PASS
            });
            channel.write(greeting);

            var serverChoice = ByteBuffer.allocate(2);
            readFully(serverChoice);
            var version = serverChoice.get();
            if (version != SOCKS_VERSION_5) {
                throw new IOException("Unsupported socks version: " + Byte.toUnsignedInt(version));
            }

            var chosenMethod = serverChoice.get();
            if (chosenMethod == METHOD_USER_PASS) {
                var userInfo = proxy.getUserInfo();
                if (userInfo == null) {
                    throw new IOException("Missing credentials for authentication: please provide them in the proxy URI");
                }
                var credentials = userInfo.split(":", 2);
                var username = credentials[0];
                var password = (credentials.length > 1) ? credentials[1] : "";

                var userBytes = username.getBytes(StandardCharsets.ISO_8859_1);
                var passBytes = password.getBytes(StandardCharsets.ISO_8859_1);

                var authRequest = ByteBuffer.allocate(3 + userBytes.length + passBytes.length);
                authRequest.put(AUTH_VERSION_1);
                authRequest.put((byte) userBytes.length);
                authRequest.put(userBytes);
                authRequest.put((byte) passBytes.length);
                authRequest.put(passBytes);
                authRequest.flip();
                channel.write(authRequest);

                var authResponse = ByteBuffer.allocate(2);
                readFully(authResponse);
                authResponse.get(); // Skip version
                if (authResponse.get() != AUTH_SUCCESS) {
                    throw new IOException("SOCKS proxy authentication failed.");
                }
            } else if (chosenMethod != METHOD_NO_AUTH) {
                throw new IOException("Proxy selected an unsupported authentication method: " + chosenMethod);
            }
            var destHostBytes = proxyHost.getBytes(StandardCharsets.ISO_8859_1);
            var connRequest = ByteBuffer.allocate(4 + 1 + destHostBytes.length + 2);
            connRequest.put(SOCKS_VERSION_5);
            connRequest.put(CMD_CONNECT);
            connRequest.put((byte) 0x00);
            connRequest.put(ADDR_TYPE_DOMAIN);
            connRequest.put((byte) destHostBytes.length);
            connRequest.put(destHostBytes);
            connRequest.putShort((short) proxyPort);
            connRequest.flip();
            channel.write(connRequest);
        }

        private void handleAuthenticationResponse() throws IOException {
            var replyInfo = ByteBuffer.allocate(4);
            readFully(replyInfo);

            if (replyInfo.get() != SOCKS_VERSION_5) {
                throw new IOException("Invalid SOCKS version in server reply.");
            }
            var replyCode = replyInfo.get();
            if (replyCode != REPLY_SUCCESS) {
                throw new IOException("SOCKS proxy request failed with code: " + replyCode);
            }

            replyInfo.get();
            var addrType = replyInfo.get();

            int remainingBytes;
            switch (addrType) {
                case IPV4_REPLY -> remainingBytes = 4 + 2;
                case DOMAIN_REPLY -> {
                    var lenBuf = ByteBuffer.allocate(1);
                    readFully(lenBuf);
                    remainingBytes = (lenBuf.get() & 0xFF) + 2;
                }
                case IPV6_REPLY -> remainingBytes = 16 + 2;
                default -> throw new IOException("Proxy returned an unsupported address type in reply: " + addrType);
            }
            readFully(ByteBuffer.allocate(remainingBytes));
        }

        private void readFully(ByteBuffer buffer) throws IOException {
            while (buffer.hasRemaining()) {
                if (channel.read(buffer) == -1) {
                    throw new IOException("Proxy connection closed unexpectedly during handshake.");
                }
            }
            buffer.flip();
        }
    }

    private static final class CentralSelector implements Runnable{
        private static final CentralSelector INSTANCE = new CentralSelector();

        private final Selector selector;

        private volatile Thread selectorThread;

        private CentralSelector() {
            try {
                selector = Selector.open();
            } catch (IOException e) {
                throw new RuntimeException("Cannot open selector", e);
            }
        }

        public void register(SocketChannel channel, int ops, ConnectionContext context) throws IOException {
            synchronized (this) {
                channel.register(selector, ops, context);
                if (selectorThread == null || !selectorThread.isAlive()) {
                    selectorThread = Thread.startVirtualThread(this);
                }
                selector.wakeup();
            }
        }

        private void unregister(SocketChannel channel) {
            var key = channel.keyFor(selector);
            if(key != null) {
                ((ConnectionContext) key.attachment()).disposed = true;
                key.cancel();
                selector.wakeup();
            }
        }

        public void addWrite(SocketChannel channel, ByteBuffer buffer) {
            var key = channel.keyFor(selector);
            if (key == null) {
                return;
            }
            var ctx = (ConnectionContext) key.attachment();
            ctx.pendingWrites.add(buffer);
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            selector.wakeup();
        }

        @Override
        public void run() {
            try {
                while (selector.isOpen()) {
                    var readyChannels = selector.select();
                    if (readyChannels > 0) {
                        var iterator = selector.selectedKeys().iterator();
                        while (iterator.hasNext()) {
                            var key = iterator.next();
                            iterator.remove();
                            handleKey(key);
                        }
                    }
                    if (selector.keys().isEmpty()) {
                        synchronized (this) {
                            if (selector.keys().isEmpty()) {
                                selectorThread = null;
                                break;
                            }
                        }
                    }
                }
            }catch (Throwable exception) {
                exception.printStackTrace(); // Should never happen
            }
        }

        private void handleKey(SelectionKey key) {
            var attachment = key.attachment();
            if (!(attachment instanceof ConnectionContext ctx)) {
                return;
            }

            var channel = (SocketChannel) key.channel();
            try {
                if (key.isConnectable()) {
                    if (channel.finishConnect()) {
                        key.interestOps(SelectionKey.OP_READ);
                        synchronized (ctx.connectionLock) {
                            ctx.connectionLock.notifyAll();
                        }
                    }
                }
                if (key.isReadable()) {
                    var ok = processRead(channel, ctx);
                    if (!ok) {
                        unregister(channel);
                        return;
                    }
                }
                if (key.isWritable()) {
                    var ok = processWrite(channel, ctx);
                    if(ok) {
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                    }
                }
            }catch (IOException exception) {
                unregister(channel);
            }
        }

        private boolean processRead(SocketChannel channel, ConnectionContext ctx) throws IOException {
            if (ctx.messageLengthBuffer.hasRemaining()) {
                var bytesRead = channel.read(ctx.messageLengthBuffer);
                if (bytesRead == -1) {
                    return false;
                }

                if (ctx.messageLengthBuffer.hasRemaining()) {
                    return true;
                }

                ctx.messageLengthBuffer.flip();
                var length = ((ctx.messageLengthBuffer.get() & 0xFF) << 16)
                        | ((ctx.messageLengthBuffer.get() & 0xFF) << 8)
                        | (ctx.messageLengthBuffer.get() & 0xFF);
                if (length > MAX_MESSAGE_LENGTH) {
                    return false;
                }

                ctx.messageBuffer = ByteBuffer.allocate(length);
                return true;
            }else {
                var bytesRead = channel.read(ctx.messageBuffer);
                if (bytesRead == -1) {
                    return false;
                }

                if (ctx.messageBuffer.hasRemaining()) {
                    return true;
                }

                ctx.messageBuffer.flip();
                ctx.messageLengthBuffer.clear();
                var buffer = ctx.messageBuffer;
                Thread.startVirtualThread(() -> {
                    var e = channel;
                    ctx.onMessage.accept(buffer);
                });
                ctx.messageBuffer = null;
                return true;
            }
        }

        private boolean processWrite(SocketChannel channel, ConnectionContext ctx) throws IOException {
            var queue = ctx.pendingWrites;
            while (!queue.isEmpty()) {
                ByteBuffer buf = queue.peek();
                channel.write(buf);
                if (buf.hasRemaining()) {
                    break;
                }
                queue.poll();
            }

            return queue.isEmpty();
        }
    }

    private static final class ConnectionContext {
        private final Object connectionLock;
        private final ByteBuffer messageLengthBuffer;
        public boolean disposed;
        private ByteBuffer messageBuffer;
        private final Queue<ByteBuffer> pendingWrites;
        private final Consumer<ByteBuffer> onMessage;
        private ConnectionContext(Consumer<ByteBuffer> onMessage) {
            this.onMessage = onMessage;
            this.messageLengthBuffer = ByteBuffer.allocate(3);
            this.pendingWrites = new ConcurrentLinkedQueue<>();
            this.connectionLock = new Object();
        }
    }
}
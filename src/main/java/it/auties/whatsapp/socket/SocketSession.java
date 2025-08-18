package it.auties.whatsapp.socket;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

abstract sealed class SocketSession {
    private static final String HOST_NAME = "g.whatsapp.net";
    private static final int PORT = 443;
    private static final int MAX_MESSAGE_LENGTH = 1048576;
    private static final int DEFAULT_RCV_BUF = 8192;
    private static final int DEFAULT_READ_TIMEOUT = 10_000;

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

    abstract void connect(Consumer<ByteBuffer> onMessage);

    private void connect(InetSocketAddress endpoint, boolean ready, Consumer<ByteBuffer> onMessage) {
        try {
            this.channel = SocketChannel.open();
            channel.configureBlocking(false);
            var ctx = new ConnectionContext(onMessage, ready);
            if (channel.connect(endpoint)) {
                CentralSelector.INSTANCE.register(channel, SelectionKey.OP_READ, ctx);
            } else {
                CentralSelector.INSTANCE.register(channel, SelectionKey.OP_CONNECT, ctx);
                synchronized (ctx.connectionLock) {
                    ctx.connectionLock.wait();
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

    void sendBinary(ByteBuffer buffer) {
        if (!isConnected()) {
            throw new IllegalStateException("Socket is not connected");
        }

        if(!CentralSelector.INSTANCE.addWrite(channel, buffer)) {
            throw new IllegalStateException("Failed to send binary");
        }
    }

    // Should only be used while connecting the proxy
    private int readBinary(ByteBuffer buffer, boolean fully) throws IOException {
        if (!isConnected()) {
            throw new IllegalStateException("Socket is not connected");
        }

        var read = new PendingRead(buffer, fully);
        if(!CentralSelector.INSTANCE.addRead(channel, read)) {
            throw new IllegalStateException("Failed to read binary");
        }

        synchronized (read.lock) {
            try {
                read.lock.wait(DEFAULT_READ_TIMEOUT);
            }catch (InterruptedException exception) {
                throw new RuntimeException("Interrupted while waiting for read", exception);
            }
        }

        if(read.length == -1) {
            throw new IOException("Unexpected end of stream");
        }

        return read.length;
    }

    private static final class DirectSession extends SocketSession {
        @Override
        void connect(Consumer<ByteBuffer> onMessage) {
            if (isConnected()) {
                return;
            }

            var endpoint = new InetSocketAddress(HOST_NAME, PORT); // Don't resolve this statically
            super.connect(endpoint, true, onMessage);
        }
    }

    private static final class ProxiedHttpSession extends SocketSession {
        private static final char CARRIAGE_RETURN = '\r';
        private static final char LINE_FEED = '\n';
        private static final char SPACE = ' ';
        public static final int HTTP_VERSION_MINOR = 1;
        public static final int HTTP_VERSION_MAJOR = 1;
        private static final int SUCCESS_STATUS_CODE = 200;

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
            super.connect(new InetSocketAddress(host, port), false, onMessage);
            authenticate();
            if(!CentralSelector.INSTANCE.markReady(channel)) {
                throw new IllegalStateException("Failed to authenticate with proxy: rejected");
            }
        }

        private void authenticate() {
            try {
                sendAuthenticationRequest();
                handleAuthenticationResponse();
            }catch (IOException exception) {
                throw new UncheckedIOException("Failed to authenticate with proxy", exception);
            }
        }

        private void sendAuthenticationRequest() throws IOException {
            var builder = new StringBuilder();
            builder.append("CONNECT ")
                    .append(HOST_NAME)
                    .append(":")
                    .append(PORT)
                    .append(" HTTP/" + HTTP_VERSION_MAJOR + "." + HTTP_VERSION_MINOR + "\r\n");
            builder.append("Host: ")
                    .append(HOST_NAME)
                    .append(":")
                    .append(PORT)
                    .append("\r\n");
            var authInfo = proxy.getUserInfo();
            if (authInfo != null) {
                builder.append("Proxy-Authorization: Basic ")
                        .append(Base64.getEncoder().encodeToString(authInfo.getBytes()))
                        .append("\r\n");
            }
            builder.append("\r\n");
            super.sendBinary(ByteBuffer.wrap(builder.toString().getBytes()));
        }

        // Optimized method that just tries to confirm we got a 200 status code
        // Skips everything else
        private void handleAuthenticationResponse() throws IOException {
            // Skip junk before the actual HTTP response starts
            var reader = ByteBuffer.allocate(12);
            do {
                super.readBinary(reader, true);
            }while(isJunk(reader));

            // Read again if we read junk
            if(reader.position() != 0) {
                reader.compact();
                super.readBinary(reader, true);
            }

            // Parse the HTTP/ part
            if(reader.get() != 'H'
                    || reader.get() != 'T'
                    || reader.get() != 'T'
                    || reader.get() != 'P'
                    || reader.get() != '/') {
                throw new IOException("Invalid HTTP response: expected HTTP/1.1");
            }

            // Parse the HTTP version, we don't care about receiving a specific version back
            var major = reader.get() - '0';
            if(major != HTTP_VERSION_MAJOR) {
                throw new IOException("Invalid HTTP response: expected HTTP/1.1");
            }
            var versionSeparator = reader.get();
            if(versionSeparator != '.') {
                throw new IOException("Invalid HTTP response: expected HTTP/1.1");
            }

            var minor = reader.get() - '0';
            if(minor != HTTP_VERSION_MINOR) {
                throw new IOException("Invalid HTTP response: expected HTTP/1.1");
            }

            var space = reader.get();
            if(space != SPACE) {
                throw new IOException("Invalid HTTP response: expected separator between HTTP version and status code");
            }

            // Make sure we are getting a 200 status code
            var statusCode = (reader.get() - '0') * 100
                    +  (reader.get() - '0') * 10
                    +  (reader.get() - '0');
            if(statusCode != SUCCESS_STATUS_CODE) {
                throw new IOException("Invalid HTTP response: expected status code " + SUCCESS_STATUS_CODE + ", but got " +  statusCode);
            }

            // Read the payload until the HTTP response ends (\r\n\r\n)
            // If the stream end, throw an error
            reader = ByteBuffer.allocate(512); // The http response is usually very small
            var length = super.readBinary(reader, false);
            reader.position(0);
            reader.limit(length);
            byte current;
            while (isConnected()) {
                if(reader.remaining() < 4) {
                    var available = reader.remaining();
                    reader.compact();
                    while (available < 4) {
                        available += super.readBinary(reader, false);
                    }
                    reader.position(0);
                    reader.limit(available);
                }

                current = reader.get();
                if (current != CARRIAGE_RETURN) {
                    continue;
                }

                current = reader.get();
                if(current != LINE_FEED) {
                    continue;
                }

                current = reader.get();
                if (current != CARRIAGE_RETURN) {
                    continue;
                }

                current = reader.get();
                if(current != LINE_FEED) {
                    continue;
                }

                return;
            }
            throw new IOException("Unexpected end of stream");
        }

        private boolean isJunk(ByteBuffer header) {
            while (header.hasRemaining()) {
                var current = header.get();
                if (current != SPACE && current != CARRIAGE_RETURN && current != LINE_FEED) {
                    header.position(header.position() - 1);
                    return false;
                }
            }
            return true;
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
        private static final int REPLY_GENERAL_FAILURE = 0x01;
        private static final int REPLY_CONNECTION_NOT_ALLOWED = 0x02;
        private static final int REPLY_NETWORK_UNREACHABLE = 0x03;
        private static final int REPLY_HOST_UNREACHABLE = 0x04;
        private static final int REPLY_CONNECTION_REFUSED = 0x05;
        private static final int REPLY_TTL_EXPIRED = 0x06;
        private static final int REPLY_COMMAND_NOT_SUPPORTED = 0x07;
        private static final int REPLY_ADDRESS_TYPE_UNSUPPORTED = 0x08;
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
            super.connect(new InetSocketAddress(proxyHost, proxyPort), false, onMessage);
            authenticate();
            if(!CentralSelector.INSTANCE.markReady(channel)) {
                throw new IllegalStateException("Failed to authenticate with proxy: rejected");
            }
        }

        private void authenticate() {
            try {
                sendAuthenticationRequest();
                handleAuthenticationResponse();
            }catch (IOException exception) {
                throw new UncheckedIOException("Failed to authenticate with proxy", exception);
            }
        }

        private void sendAuthenticationRequest() throws IOException {
            var greeting = ByteBuffer.wrap(new byte[]{
                    SOCKS_VERSION_5,
                    2,
                    METHOD_NO_AUTH,
                    METHOD_USER_PASS
            });
            super.sendBinary(greeting);

            var serverChoice = ByteBuffer.allocate(2);
            super.readBinary(serverChoice, true);
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
                super.sendBinary(authRequest);

                var authResponse = ByteBuffer.allocate(2);
                super.readBinary(authResponse, true);
                authResponse.get(); // Skip version
                if (authResponse.get() != AUTH_SUCCESS) {
                    throw new IOException("SOCKS proxy authentication failed.");
                }
            } else if (chosenMethod != METHOD_NO_AUTH) {
                throw new IOException("Proxy selected an unsupported authentication method: " + chosenMethod);
            }
            var destHostBytes = HOST_NAME.getBytes(StandardCharsets.ISO_8859_1);
            var connRequest = ByteBuffer.allocate(4 + 1 + destHostBytes.length + 2);
            connRequest.put(SOCKS_VERSION_5);
            connRequest.put(CMD_CONNECT);
            connRequest.put((byte) 0x00);
            connRequest.put(ADDR_TYPE_DOMAIN);
            connRequest.put((byte) destHostBytes.length);
            connRequest.put(destHostBytes);
            connRequest.putShort((short) PORT);
            connRequest.flip();
            super.sendBinary(connRequest);
        }

        private void handleAuthenticationResponse() throws IOException {
            var replyInfo = ByteBuffer.allocate(2);
            super.readBinary(replyInfo, true);

            if (replyInfo.get() != SOCKS_VERSION_5) {
                throw new IOException("Invalid SOCKS version in server reply.");
            }
            var replyCode = replyInfo.get();
            if (replyCode != REPLY_SUCCESS) {
                var reason = getReplyReason(replyCode);
                throw new IOException("SOCKS proxy request failed: " + reason + " (Code: " + replyCode + ")");
            }

            var addrInfo = ByteBuffer.allocate(2);
            super.readBinary(addrInfo, true);

            addrInfo.get();

            var addrType = addrInfo.get();
            int remainingBytes;
            switch (addrType) {
                case IPV4_REPLY -> remainingBytes = 4 + 2;
                case DOMAIN_REPLY -> {
                    var lenBuf = ByteBuffer.allocate(1);
                    super.readBinary(lenBuf, true);
                    remainingBytes = (lenBuf.get() & 0xFF) + 2;
                }
                case IPV6_REPLY -> remainingBytes = 16 + 2;
                default -> throw new IOException("Proxy returned an unsupported address type in reply: " + addrType);
            }
            super.readBinary(ByteBuffer.allocate(remainingBytes), true);
        }

        public static String getReplyReason(int replyCode) {
            return switch (replyCode) {
                case REPLY_GENERAL_FAILURE -> "General SOCKS server failure";
                case REPLY_CONNECTION_NOT_ALLOWED -> "Connection not allowed by ruleset";
                case REPLY_NETWORK_UNREACHABLE -> "Network unreachable";
                case REPLY_HOST_UNREACHABLE -> "Host unreachable";
                case REPLY_CONNECTION_REFUSED -> "Connection refused";
                case REPLY_TTL_EXPIRED -> "TTL expired";
                case REPLY_COMMAND_NOT_SUPPORTED -> "Command not supported";
                case REPLY_ADDRESS_TYPE_UNSUPPORTED -> "Address type not supported";
                default -> "Unknown failure";
            };
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

        @SuppressWarnings("MagicConstant")
        public synchronized void register(SocketChannel channel, int ops, ConnectionContext context) throws IOException {
            channel.register(selector, ops, context);
            if (selectorThread == null || !selectorThread.isAlive()) {
                selectorThread = Thread.startVirtualThread(this);
            }
            selector.wakeup();
        }

        private void unregister(SocketChannel channel) {
            var key = channel.keyFor(selector);
            if(key != null) {
                if(key.attachment() instanceof ConnectionContext ctx) {
                    ctx.dispatcher.shutdownNow();
                    ctx.dispatcher.close();
                }
                key.cancel();
                selector.wakeup();
            }
        }

        public boolean addRead(SocketChannel channel, PendingRead read) {
            var key = channel.keyFor(selector);
            if (key == null) {
                return false;
            }
            var ctx = (ConnectionContext) key.attachment();
            ctx.pendingReads.add(read);
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            selector.wakeup();
            return true;
        }

        public boolean markReady(SocketChannel channel) {
            var key = channel.keyFor(selector);
            if (key == null) {
                return false;
            }
            var ctx = (ConnectionContext) key.attachment();
            ctx.ready = true;
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            selector.wakeup();
            return true;
        }

        public boolean addWrite(SocketChannel channel, ByteBuffer buffer) {
            var key = channel.keyFor(selector);
            if (key == null) {
                return false;
            }
            var ctx = (ConnectionContext) key.attachment();
            ctx.pendingWrites.add(buffer);
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            selector.wakeup();
            return true;
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
            }catch (Throwable ignored) {
                // Should be safe to ignore
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
                    var ok = processRead(channel, ctx, key);
                    if (!ok) {
                        unregister(channel);
                        return;
                    }
                }
                if (key.isWritable()) {
                    var done = processWrite(channel, ctx);
                    if(done) {
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                    }
                }
            }catch (IOException exception) {
                unregister(channel);
            }
        }

        private boolean processRead(SocketChannel channel, ConnectionContext ctx, SelectionKey key) throws IOException {
            if(!ctx.ready) {
                var pendingRead = ctx.pendingReads.peek();
                if(pendingRead == null) {
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                    return true;
                }

                var bytesRead = channel.read(pendingRead.buffer);
                if (bytesRead == -1) {
                    pendingRead.length = -1;
                    synchronized (pendingRead.lock) {
                        pendingRead.lock.notifyAll();
                    }
                    return false;
                }

                pendingRead.length += bytesRead;
                if(!pendingRead.fullRead || !pendingRead.buffer.hasRemaining()) {
                    if(pendingRead.fullRead) {
                        pendingRead.buffer.flip();
                    }
                    ctx.pendingReads.remove();
                    synchronized (pendingRead.lock) {
                        pendingRead.lock.notifyAll();
                    }
                }

                return true;
            } else if (ctx.messageLengthBuffer.hasRemaining()) {
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
                ctx.dispatcher.execute(() -> ctx.onMessage.accept(buffer));
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
        // Lock to synchronize the connect method
        private final Object connectionLock;
        // Whether the connection is ready
        // If the client is not using a proxy, this is instantly true, otherwise only after the proxy auth is done this is true
        private boolean ready;
        // List of buffers to read, used while tunneled = false
        private final Queue<PendingRead> pendingReads;
        // LIst of buffers to write, always used
        private final Queue<ByteBuffer> pendingWrites;
        // Buffer used to read the length of the current WhatsApp message
        // Only used when tunneled = true
        private final ByteBuffer messageLengthBuffer;
        // Buffer used to read the current WhatsApp message
        // Only used when tunneled = true
        private ByteBuffer messageBuffer;
        // Callback for a WhatsApp message
        // ONly used whe tunneled = true
        private final Consumer<ByteBuffer> onMessage;
        // The dispatcher used for onMessage
        // Prevents the next message from being processed if the previous is not done processing
        private final ExecutorService dispatcher;
        private ConnectionContext(Consumer<ByteBuffer> onMessage, boolean ready) {
            this.connectionLock = new Object();
            this.ready = ready;
            this.onMessage = onMessage;
            this.pendingReads = new ConcurrentLinkedQueue<>();
            this.pendingWrites = new ConcurrentLinkedQueue<>();
            this.messageLengthBuffer = ByteBuffer.allocate(3);
            this.dispatcher = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
        }
    }

    private static class PendingRead {
        private final ByteBuffer buffer;
        private final boolean fullRead;
        private final Object lock;
        private int length;

        private PendingRead(ByteBuffer buffer, boolean fullRead) {
            this.buffer = buffer;
            this.fullRead = fullRead;
            this.lock = new Object();
        }
    }
}
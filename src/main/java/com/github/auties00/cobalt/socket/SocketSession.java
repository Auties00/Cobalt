package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.exception.SessionClosedException;
import com.github.auties00.cobalt.model.auth.*;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeEncoder;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public abstract sealed class SocketSession {
    private static final String HOST_NAME = "g.whatsapp.net";
    private static final int PORT = 443;
    private static final int MAX_MESSAGE_LENGTH = 1048576;
    private static final int DEFAULT_READ_TIMEOUT = 10_000;

    private static final int HEADER_LENGTH = Integer.BYTES + Short.BYTES;

    private static int writeRequestHeader(int requestLength, byte[] message, int offset) {
        var mss = requestLength >> 16;
        message[offset++] = (byte) (mss >> 24);
        message[offset++] = (byte) (mss >> 16);
        message[offset++] = (byte) (mss >> 8);
        message[offset++] = (byte) mss;
        var lss = requestLength & 65535;
        message[offset++] = (byte) (lss >> 8);
        message[offset++] = (byte) lss;
        return offset;
    }

    private static GCMParameterSpec createGcmIv(long counter) {
        var iv = new byte[12];
        iv[4] = (byte) (counter >> 56);
        iv[5] = (byte) (counter >> 48);
        iv[6] = (byte) (counter >> 40);
        iv[7] = (byte) (counter >> 32);
        iv[8] = (byte) (counter >> 24);
        iv[9] = (byte) (counter >> 16);
        iv[10] = (byte) (counter >> 8);
        iv[11] = (byte) (counter);
        return new GCMParameterSpec(128, iv);
    }

    SocketChannel channel;
    private final SignalIdentityKeyPair noiseKeyPair;
    private final byte[] handshakePrologue;
    private final ClientPayload handshakePayload;

    protected SocketSession(SignalIdentityKeyPair noiseKeyPair, byte[] handshakePrologue, ClientPayload handshakePayload) {
        this.noiseKeyPair = noiseKeyPair;
        this.handshakePrologue = handshakePrologue;
        this.handshakePayload = handshakePayload;
    }

    public static SocketSession of(SignalIdentityKeyPair noiseKeyPair, byte[] handshakePrologue, ClientPayload handshakePayload, URI proxy) {
        Objects.requireNonNull(noiseKeyPair, "noiseKeyPair cannot be null");
        Objects.requireNonNull(handshakePrologue, "handshakePrologue cannot be null");
        if(proxy == null) {
            return new DirectSession(noiseKeyPair, handshakePrologue, handshakePayload);
        }

        var scheme = proxy.getScheme();
        Objects.requireNonNull(scheme, "Malformed proxy: scheme cannot be null");
        return switch (scheme.toLowerCase()) {
            case "http", "https" -> new ProxiedHttpSession(noiseKeyPair, handshakePrologue, handshakePayload, proxy);
            case "socks5", "socks5h" -> new ProxiedSocksSession(noiseKeyPair, handshakePrologue, handshakePayload, proxy);
            default -> throw new IllegalArgumentException("Malformed proxy: unknown scheme " + scheme);
        };
    }

    public abstract void connect(Consumer<ByteBuffer> onMessage);

    private ConnectionContext openConnection(InetSocketAddress endpoint, boolean tunnelled, Consumer<ByteBuffer> onMessage) {
        if(isConnected()) {
            throw new IllegalStateException("Socket is already connected");
        }

        try {
            this.channel = SocketChannel.open();
            channel.configureBlocking(false);
            var ctx = new ConnectionContext(tunnelled, handshakePrologue, handshakePayload, noiseKeyPair, onMessage);
            if (channel.connect(endpoint)) {
                CentralSelector.INSTANCE.register(channel, SelectionKey.OP_READ, ctx);
            } else {
                CentralSelector.INSTANCE.register(channel, SelectionKey.OP_CONNECT, ctx);
                synchronized (ctx.connectionLock) {
                    ctx.connectionLock.wait();
                }
            }
            ctx.connected = true;
            return ctx;
        }catch (Throwable exception) {
            throw new RuntimeException("Cannot connect to socket", exception);
        }
    }

    private void startHandshake(ConnectionContext ctx) {
        if(ctx.handshakeEphemeralKeyPair != null) {
            throw new IllegalStateException("Handshake already started");
        }

        var ephemeralKeyPair = SignalIdentityKeyPair.random();
        ctx.handshakeEphemeralKeyPair = ephemeralKeyPair;
        var clientHello = new ClientHelloBuilder()
                .ephemeral(ephemeralKeyPair.publicKey().toEncodedPoint())
                .build();
        var handshakeMessage = new HandshakeMessageBuilder()
                .clientHello(clientHello)
                .build();
        var requestLength = HandshakeMessageSpec.sizeOf(handshakeMessage);
        var message = new byte[handshakePrologue.length + HEADER_LENGTH + requestLength];
        System.arraycopy(handshakePrologue, 0, message, 0, handshakePrologue.length);
        var offset = writeRequestHeader(requestLength, message, handshakePrologue.length);
        HandshakeMessageSpec.encode(handshakeMessage, ProtobufOutputStream.toBytes(message, offset));
        sendBinary(ByteBuffer.wrap(message));
    }

    public void disconnect() {
        if(!isConnected()) {
            return;
        }

        CentralSelector.INSTANCE.unregister(channel);
        try {
            if(channel != null) {
                channel.close();
            }
        }catch (IOException _) {

        }
    }

    public synchronized void sendNode(Node node) {
        var ctx = CentralSelector.INSTANCE.getContext(channel);
        if(ctx == null || !ctx.connected || !ctx.secured) {
            throw new SessionClosedException();
        }

        try {
            var writeCipher = Cipher.getInstance("AES/GCM/NoPadding");
            writeCipher.init(
                    Cipher.ENCRYPT_MODE,
                    ctx.writeKey,
                    createGcmIv(ctx.writeCounter++)
            );
            var plaintextLength = NodeEncoder.sizeOf(node);
            var ciphertextLength = writeCipher.getOutputSize(plaintextLength);
            var ciphertext = new byte[HEADER_LENGTH + ciphertextLength];
            var offset = writeRequestHeader(ciphertextLength, ciphertext, 0);
            NodeEncoder.encode(node, ciphertext, offset, plaintextLength);
            writeCipher.doFinal(ciphertext, offset, plaintextLength, ciphertext, offset);
            sendBinary(ByteBuffer.wrap(ciphertext));
        }catch (GeneralSecurityException exception) {
            throw new InternalError("Failed to encrypt node", exception);
        }
    }

    private void sendBinary(ByteBuffer buffer) {
        if (!isConnected()) {
            throw new IllegalStateException("Socket is not connected");
        }

        if(!CentralSelector.INSTANCE.addWrite(channel, buffer)) {
            throw new IllegalStateException("Failed to send binary");
        }
    }

    public boolean isConnected() {
        var ctx = CentralSelector.INSTANCE.getContext(channel);
        return ctx != null && ctx.connected;
    }

    private int readPlainBinary(ByteBuffer buffer, boolean fully) throws IOException {
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
            } catch (InterruptedException exception) {
                throw new RuntimeException("Interrupted while waiting for read", exception);
            }
        }

        if(read.length == -1) {
            throw new IOException("Unexpected end of stream");
        }

        return read.length;
    }

    private static final class DirectSession extends SocketSession {

        DirectSession(SignalIdentityKeyPair noiseKeyPair, byte[] handshakePrologue, ClientPayload handshakePayload) {
            super(noiseKeyPair, handshakePrologue, handshakePayload);
        }

        @Override
        public void connect(Consumer<ByteBuffer> onMessage) {
            var endpoint = new InetSocketAddress(HOST_NAME, PORT); // Don't resolve this statically
            var ctx = super.openConnection(endpoint, true, onMessage);
            super.startHandshake(ctx);
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

        ProxiedHttpSession(SignalIdentityKeyPair noiseKeyPair, byte[] handshakePrologue, ClientPayload handshakePayload, URI proxy) {
            super(noiseKeyPair, handshakePrologue, handshakePayload);
            this.proxy = proxy;
        }


        @Override
        public void connect(Consumer<ByteBuffer> onMessage) {
            var host = proxy.getHost();
            var port = proxy.getPort();
            if(port == -1) {
                port = switch (proxy.getScheme().toLowerCase()) {
                    case "http" -> 80;
                    case "https" -> 443;
                    default -> throw new InternalError();
                };
            }
            var ctx = super.openConnection(new InetSocketAddress(host, port), false, onMessage);
            authenticate();
            super.startHandshake(ctx);
        }

        private void authenticate() {
            try {
                sendAuthenticationRequest();
                handleAuthenticationResponse();
                if(!CentralSelector.INSTANCE.markReady(channel)) {
                    throw new IllegalStateException("Failed to authenticate with proxy: rejected");
                }
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
                super.readPlainBinary(reader, true);
            }while(isJunk(reader));

            // Read again if we read junk
            if(reader.position() != 0) {
                reader.compact();
                super.readPlainBinary(reader, true);
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
            var length = super.readPlainBinary(reader, false);
            reader.position(0);
            reader.limit(length);
            byte current;
            while (isConnected()) {
                if(reader.remaining() < 4) {
                    var available = reader.remaining();
                    reader.compact();
                    while (available < 4) {
                        available += super.readPlainBinary(reader, false);
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

        ProxiedSocksSession(SignalIdentityKeyPair noiseKeyPair, byte[] handshakePrologue, ClientPayload handshakePayload, URI proxy) {
            super(noiseKeyPair, handshakePrologue, handshakePayload);
            this.proxy = proxy;
        }


        @Override
        public void connect(Consumer<ByteBuffer> onMessage) {
            var proxyHost = proxy.getHost();
            var proxyPort = proxy.getPort() == -1 ? 1080 : proxy.getPort();
            var ctx = super.openConnection(new InetSocketAddress(proxyHost, proxyPort), false, onMessage);
            authenticate();
            super.startHandshake(ctx);
        }

        private void authenticate() {
            try {
                sendAuthenticationRequest();
                handleAuthenticationResponse();
                if(!CentralSelector.INSTANCE.markReady(channel)) {
                    throw new IllegalStateException("Failed to authenticate with proxy: rejected");
                }
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
            super.readPlainBinary(serverChoice, true);
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
                super.readPlainBinary(authResponse, true);
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
            super.readPlainBinary(replyInfo, true);

            if (replyInfo.get() != SOCKS_VERSION_5) {
                throw new IOException("Invalid SOCKS version in server reply.");
            }
            var replyCode = replyInfo.get();
            if (replyCode != REPLY_SUCCESS) {
                var reason = getReplyReason(replyCode);
                throw new IOException("SOCKS proxy request failed: " + reason + " (Code: " + replyCode + ")");
            }

            var addrInfo = ByteBuffer.allocate(2);
            super.readPlainBinary(addrInfo, true);

            addrInfo.get();

            var addrType = addrInfo.get();
            int remainingBytes;
            switch (addrType) {
                case IPV4_REPLY -> remainingBytes = 4 + 2;
                case DOMAIN_REPLY -> {
                    var lenBuf = ByteBuffer.allocate(1);
                    super.readPlainBinary(lenBuf, true);
                    remainingBytes = (lenBuf.get() & 0xFF) + 2;
                }
                case IPV6_REPLY -> remainingBytes = 16 + 2;
                default -> throw new IOException("Proxy returned an unsupported address type in reply: " + addrType);
            }
            super.readPlainBinary(ByteBuffer.allocate(remainingBytes), true);
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
            if (key == null) {
                return;
            }

            var ctx = (ConnectionContext) key.attachment();
            ctx.connected = false;
            key.cancel();
            selector.wakeup();
        }

        public ConnectionContext getContext(SocketChannel channel) {
            if(channel == null) {
                return null;
            }

            var key = channel.keyFor(selector);
            if(key == null) {
                return null;
            }

            return (ConnectionContext) key.attachment();
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
            ctx.tunnelled = true;
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
                        return;
                    }
                }
                if (key.isWritable()) {
                    var done = processWrite(channel, ctx);
                    if(done) {
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                    }
                }
            }catch (IOException _) {

            }
        }

        private boolean processRead(SocketChannel channel, ConnectionContext ctx, SelectionKey key) throws IOException {
            if(!ctx.tunnelled) {
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
            } else {
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
                } else {
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
                    ctx.messageBuffer = null;

                    var secured = ctx.secured;
                    var readKey = ctx.readKey;
                    var readCounter = ctx.readCounter++;

                    Thread.startVirtualThread(() -> {
                        if(secured) {
                            var output = decryptRead(readKey, readCounter, buffer);
                            ctx.onMessage.accept(output);
                        }else {
                            finishHandshake(channel, ctx, buffer);
                        }
                    });

                    return true;
                }
            }
        }

        private ByteBuffer decryptRead(SecretKeySpec readKey, long readCounter, ByteBuffer buffer) {
            try {
                var output = buffer.duplicate();
                var readCipher = Cipher.getInstance("AES/GCM/NoPadding");
                readCipher.init(
                        Cipher.DECRYPT_MODE,
                        readKey,
                        createGcmIv(readCounter)
                );
                readCipher.doFinal(buffer, output);
                output.flip();
                return output;
            }catch (GeneralSecurityException exception) {
                throw new RuntimeException(exception);
            }
        }

        public void finishHandshake(SocketChannel channel, ConnectionContext ctx, ByteBuffer serverHelloPayload) {
            var ephemeralKeyPair = ctx.handshakeEphemeralKeyPair;
            if(ephemeralKeyPair == null) {
                throw new IllegalStateException("Handshake has not started");
            }

            var serverHandshake = HandshakeMessageSpec.decode(ProtobufInputStream.fromBuffer(serverHelloPayload));
            var serverHello = serverHandshake.serverHello();
            try(var handshake = new SocketHandshake(ctx.handshakePrologue)) {
                handshake.updateHash(ephemeralKeyPair.publicKey().toEncodedPoint());
                handshake.updateHash(serverHello.ephemeral());
                var sharedEphemeral = Curve25519.sharedKey(ephemeralKeyPair.privateKey().toEncodedPoint(), serverHello.ephemeral());
                handshake.mixIntoKey(sharedEphemeral);
                var decodedStaticText = handshake.cipher(serverHello.staticText(), false);
                var sharedStatic = Curve25519.sharedKey(ephemeralKeyPair.privateKey().toEncodedPoint(), decodedStaticText);
                handshake.mixIntoKey(sharedStatic);
                handshake.cipher(serverHello.payload(), false);
                var noiseKeyPair = ctx.handshakeNoiseKeyPair;
                var encodedKey = handshake.cipher(noiseKeyPair.publicKey().toEncodedPoint(), true);
                var sharedPrivate = Curve25519.sharedKey(noiseKeyPair.privateKey().toEncodedPoint(), serverHello.ephemeral());
                handshake.mixIntoKey(sharedPrivate);
                var encodedPayload = handshake.cipher(ClientPayloadSpec.encode(ctx.handshakePayload), true);
                var clientFinish = new ClientFinish(encodedKey, encodedPayload);
                var clientHandshake = new HandshakeMessageBuilder()
                        .clientFinish(clientFinish)
                        .build();
                var requestLength = HandshakeMessageSpec.sizeOf(clientHandshake);
                var message = new byte[HEADER_LENGTH + requestLength];
                var offset = writeRequestHeader(requestLength, message, 0);
                HandshakeMessageSpec.encode(clientHandshake, ProtobufOutputStream.toBytes(message, offset));
                addWrite(channel, ByteBuffer.wrap(message));
                var keys = handshake.finish();

                ctx.writeCounter = 0;
                ctx.writeKey = new SecretKeySpec(keys, 0, 32, "AES");

                ctx.readCounter = 0;
                ctx.readKey = new SecretKeySpec(keys, 32, 32, "AES");

                ctx.secured = true;
            }catch (GeneralSecurityException exception) {
                throw new RuntimeException("Cannot finish handshake", exception);
            }finally {
                ctx.handshakePrologue = null;
                ctx.handshakePayload = null;
                ctx.handshakeEphemeralKeyPair = null;
            }
        }

        private boolean processWrite(SocketChannel channel, ConnectionContext ctx) throws IOException {
            var queue = ctx.pendingWrites;
            while (!queue.isEmpty()) {
                var buf = queue.peek();
                channel.write(buf);
                if (buf.hasRemaining()) {
                    break;
                }
                queue.poll();
            }

            return queue.isEmpty();
        }
    }

    // Lifecycle:
    // Open connection -> Open tunnel (proxy) -> Handshake -> Connected
    private static final class ConnectionContext {
        // Flag to indicate whether the connection is connected
        private boolean connected;

        // Lock to synchronize the connect method
        private final Object connectionLock;

        // Whether the connection is tunneled
        // If the client is not using a proxy, this is instantly true, otherwise only after the proxy auth is done this is true
        private boolean tunnelled;

        // The handshake prologue used for the handshake
        // Becomes null after the handshake is done
        private byte[] handshakePrologue;

        // The handshake payload to send to WhatsApp
        // Becomes null after the handshake is done
        private ClientPayload handshakePayload;

        // The ephemeral key pair used for the handshake
        // Becomes null after the handshake is done
        private SignalIdentityKeyPair handshakeEphemeralKeyPair;

        // The noise key pair used for the handshake
        private final SignalIdentityKeyPair handshakeNoiseKeyPair;

        // The read key used to decrypt messages
        private SecretKeySpec readKey;

        // The GCM counter to decrypt messages
        private long readCounter;

        // The write key used to encrypt the connection
        private SecretKeySpec writeKey;

        // The GCM counter to encrypt the connection
        private long writeCounter;

        // Flag to indicate whether the connection has finished the handshake
        private boolean secured;

        // List of buffers to read, used while ready = false
        private final Queue<PendingRead> pendingReads;

        // List of buffers to write, always used
        private final Queue<ByteBuffer> pendingWrites;

        // Buffer used to read the length of the current WhatsApp message
        // Only used when ready = true
        private final ByteBuffer messageLengthBuffer;

        // Buffer used to read the current WhatsApp message
        // Only used when ready = true
        private ByteBuffer messageBuffer;

        // Callback for a WhatsApp message
        // Only used when ready = true
        private final Consumer<ByteBuffer> onMessage;

        private ConnectionContext(boolean tunnelled, byte[] handshakePrologue, ClientPayload handshakePayload, SignalIdentityKeyPair handshakeNoiseKeyPair, Consumer<ByteBuffer> onMessage) {
            this.handshakePrologue = handshakePrologue;
            this.handshakeNoiseKeyPair = handshakeNoiseKeyPair;
            this.handshakePayload = handshakePayload;
            this.connectionLock = new Object();
            this.tunnelled = tunnelled;
            this.onMessage = onMessage;
            this.pendingReads = new ConcurrentLinkedQueue<>();
            this.pendingWrites = new ConcurrentLinkedQueue<>();
            this.messageLengthBuffer = ByteBuffer.allocate(3);
        }
    }

    // Pending reads used while the proxy is authenticating
    // Then we switch to Whatsapp's datagram model
    private static class PendingRead {
        // The buffer where the data should be read
        private final ByteBuffer buffer;

        // Whether the minimum amount of reads necessary to fill the read buffer should be performed
        // Otherwise performs a single read
        private final boolean fullRead;

        // The lock to wait/notify the operation's result
        private final Object lock;

        // The length of the data read
        // After the result has been notified, this property will have a value
        private int length;

        private PendingRead(ByteBuffer buffer, boolean fullRead) {
            this.buffer = buffer;
            this.fullRead = fullRead;
            this.lock = new Object();
            this.length = -1;
        }
    }
}
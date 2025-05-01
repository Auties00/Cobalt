package it.auties.whatsapp.socket;

import it.auties.whatsapp.util.Proxies;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;

abstract sealed class SocketSession {
    private static final URI WEB_SOCKET = URI.create("wss://web.whatsapp.com/ws/chat");
    private static final InetSocketAddress MOBILE_SOCKET_ENDPOINT = new InetSocketAddress("g.whatsapp.net", 443);
    private static final int MESSAGE_LENGTH = 3;

    final URI proxy;
    SocketListener listener;

    private SocketSession(URI proxy) {
        this.proxy = proxy;
    }

    public CompletableFuture<Void> connect(SocketListener listener) {
        this.listener = listener;
        return CompletableFuture.completedFuture(null);
    }

    abstract void disconnect();

    public abstract CompletableFuture<?> sendBinary(byte[] bytes);

    static SocketSession of(URI proxy, boolean webSocket) {
        if (webSocket) {
            return new WebSocketSession(proxy);
        }

        return new RawSocketSession(proxy);
    }

    private static final class WebSocketSession extends SocketSession implements WebSocket.Listener {
        private WebSocket session;
        private byte[] message;
        private int messageOffset;

        WebSocketSession(URI proxy) {
            super(proxy);
        }

        @SuppressWarnings("resource") // Not needed
        @Override
        public CompletableFuture<Void> connect(SocketListener listener) {
            if (session != null) {
                return CompletableFuture.completedFuture(null);
            }

            super.connect(listener);
            var builder = HttpClient.newBuilder();
            if(proxy != null) {
                builder.proxy(Proxies.toProxySelector(proxy));
                builder.authenticator(Proxies.toAuthenticator(proxy));
            }
            return builder.build()
                    .newWebSocketBuilder()
                    .buildAsync(WEB_SOCKET, this)
                    .thenAcceptAsync(webSocket -> {
                        this.session = webSocket;
                        listener.onOpen(this);
                    });
        }

        @Override
        void disconnect() {
            if (session == null) {
                return;
            }

            session.sendClose(WebSocket.NORMAL_CLOSURE, "");
        }

        @Override
        public CompletableFuture<?> sendBinary(byte[] bytes) {
            if (session == null) {
                return CompletableFuture.completedFuture(null);
            }

            return session.sendBinary(ByteBuffer.wrap(bytes), true);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if(session != null) {
                message = null;
                listener.onClose();
                session = null;
            }
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            listener.onError(error);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            try {
                while (data.hasRemaining()) {
                    if(messageOffset == 0) {
                        if(data.remaining() < MESSAGE_LENGTH) {
                            break;
                        }

                        var messageLength = (data.get() << 16) | Short.toUnsignedInt(data.getShort());
                        if (messageLength < 0) {
                            disconnect();
                            break;
                        }

                        message = new byte[messageLength];
                    }

                    var remaining = Math.min(message.length - messageOffset, data.remaining());
                    data.get(message, messageOffset, remaining);
                    messageOffset += remaining;
                    if (messageOffset == message.length) {
                        messageOffset = 0;
                        listener.onMessage(message);
                    }
                }
            } catch (Throwable throwable) {
                listener.onError(throwable);
            }

            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }
    }

    private static final class RawSocketSession extends SocketSession {
        private SocketChannel channel;

        RawSocketSession(URI proxy) {
            super(proxy);
        }

        @Override
        public CompletableFuture<Void> connect(SocketListener listener) {
            super.connect(listener);
            if (isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            if(proxy != null) {
                return CompletableFuture.failedFuture(new UnsupportedOperationException("Proxies are not supported on the mobile api"));
            }

            try {
                channel = SocketChannel.open();

                channel.configureBlocking(false);

                var context = new ConnectionContext(this, listener, new CompletableFuture<>());

                if (channel.connect(MOBILE_SOCKET_ENDPOINT)) {
                    context.connectFuture.complete(null);
                    listener.onOpen(this);
                    CentralSelector.INSTANCE.register(channel, SelectionKey.OP_READ, context);
                } else {
                    CentralSelector.INSTANCE.register(channel, SelectionKey.OP_CONNECT, context);
                }
                return context.connectFuture;
            } catch (IOException exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }

        @Override
        void disconnect() {
            try {
                if (channel == null) {
                    return;
                }
                channel.close();
                listener.onClose();
            } catch (Throwable ignored) {

            }
        }

        private boolean isOpen() {
            return channel != null && channel.isOpen();
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            if (channel == null) {
                return CompletableFuture.completedFuture(null);
            }

            CentralSelector.INSTANCE
                    .addWrite(channel, ByteBuffer.wrap(bytes));
            return CompletableFuture.completedFuture(null);
        }

        private static final class CentralSelector implements Runnable{
            private static final CentralSelector INSTANCE = new CentralSelector();

            private final Selector selector;
            private final Object lock = new Object();

            private volatile Thread selectorThread;

            private CentralSelector() {
                try {
                    selector = Selector.open();
                } catch (IOException e) {
                    throw new RuntimeException("Cannot open selector", e);
                }
            }

            public void register(SocketChannel channel, int ops, ConnectionContext context) {
                synchronized (lock) {
                    try {
                        channel.register(selector, ops, context);
                    } catch (ClosedChannelException e) {
                        context.listener.onError(e);
                    }
                    if (selectorThread == null || !selectorThread.isAlive()) {
                        selectorThread = Thread.startVirtualThread(this);
                    }
                    selector.wakeup();
                }
            }

            public void addWrite(SocketChannel channel, ByteBuffer buffer) {
                var key = channel.keyFor(selector);
                if (key == null) {
                    // Channel not registered.
                    return;
                }
                var ctx = (ConnectionContext) key.attachment();
                ctx.pendingWrites.add(buffer);
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                selector.wakeup();
            }

            @Override
            public void run() {
                while (selector.isOpen()) {
                    try {
                        var readyChannels = selector.select();
                        if (readyChannels > 0) {
                            var iter = selector.selectedKeys()
                                    .iterator();
                            while (iter.hasNext()) {
                                var key = iter.next();
                                iter.remove();
                                handleKey(key);
                            }
                        }
                        if (selector.keys().isEmpty()) {
                            synchronized (lock) {
                                if (selector.keys().isEmpty()) {
                                    selectorThread = null;
                                    break;
                                }
                            }
                        }
                    }catch (IOException ignored) {

                    }
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
                            ctx.connectFuture.complete(null);
                            ctx.listener.onOpen(ctx.session);
                        }
                    }
                    if (key.isReadable()) {
                        var ok = processRead(channel, ctx);
                        if (!ok) {
                            key.cancel();
                            channel.close();
                            ctx.listener.onClose();
                        }
                    }
                    if (key.isWritable()) {
                        processWrite(channel, key, ctx);
                    }
                } catch (IOException e) {
                    key.cancel();
                    try {
                        channel.close();
                    } catch (IOException ignored) {

                    }
                    ctx.listener.onError(e);
                }
            }

            private boolean processRead(SocketChannel channel, ConnectionContext ctx) throws IOException {
                if (ctx.lengthBuffer.hasRemaining()) {
                    var bytesRead = channel.read(ctx.lengthBuffer);
                    if (bytesRead == -1) {
                        return false;
                    }

                    if (ctx.lengthBuffer.hasRemaining()) {
                        return true;
                    }

                    ctx.lengthBuffer.flip();
                    var length = ((ctx.lengthBuffer.get() & 0xFF) << 16)
                            | ((ctx.lengthBuffer.get() & 0xFF) << 8)
                            | (ctx.lengthBuffer.get() & 0xFF);
                    ctx.payloadBuffer = ByteBuffer.allocate(length);
                }

                if (ctx.payloadBuffer != null && ctx.payloadBuffer.hasRemaining()) {
                    var bytesRead = channel.read(ctx.payloadBuffer);
                    if (bytesRead == -1) {
                        return false;
                    }

                    if (ctx.payloadBuffer.hasRemaining()) {
                        return true;
                    }

                    ctx.payloadBuffer.flip();
                    byte[] message = new byte[ctx.payloadBuffer.remaining()];
                    ctx.payloadBuffer.get(message);
                    ctx.lengthBuffer.clear();
                    ctx.payloadBuffer = null;
                    ctx.listener.onMessage(message);
                }
                return true;
            }

            private void processWrite(SocketChannel channel, SelectionKey key, ConnectionContext ctx) throws IOException {
                var queue = ctx.pendingWrites;
                while (!queue.isEmpty()) {
                    ByteBuffer buf = queue.peek();
                    channel.write(buf);
                    if (buf.hasRemaining()) {
                        break;
                    }
                    queue.poll();
                }

                if (queue.isEmpty()) {
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                }
            }
        }

        private static final class ConnectionContext {
            private final RawSocketSession session;
            private final SocketListener listener;
            private final CompletableFuture<Void> connectFuture;
            private final Queue<ByteBuffer> pendingWrites = new ConcurrentLinkedQueue<>();
            private final ByteBuffer lengthBuffer = ByteBuffer.allocate(3);
            private ByteBuffer payloadBuffer = null;
            private ConnectionContext(RawSocketSession session, SocketListener listener, CompletableFuture<Void> connectFuture) {
                this.session = session;
                this.listener = listener;
                this.connectFuture = connectFuture;
            }
        }
    }
}
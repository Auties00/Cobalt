package it.auties.whatsapp.implementation;

import it.auties.whatsapp.net.SocketClient;
import it.auties.whatsapp.net.WebSocketClient;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public abstract sealed class SocketSession permits SocketSession.WebSocketSession, SocketSession.RawSocketSession {
    private static final String WEB_SOCKET_HOST = "web.whatsapp.com";
    private static final int WEB_SOCKET_PORT = 443;
    private static final String WEB_SOCKET_PATH = "/ws/chat";
    private static final String MOBILE_SOCKET_HOST = "g.whatsapp.net";
    private static final int MOBILE_SOCKET_PORT = 443;
    private static final int MESSAGE_LENGTH = 3;

    final URI proxy;
    SocketListener listener;

    private SocketSession(URI proxy) {
        this.proxy = proxy;
    }

    CompletableFuture<Void> connect(SocketListener listener) {
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

    public static final class WebSocketSession extends SocketSession implements WebSocketClient.Listener {
        private WebSocketClient webSocket;
        private final MessageOutputStream messageOutputStream;

        WebSocketSession(URI proxy) {
            super(proxy);
            this.messageOutputStream = new MessageOutputStream();
        }

        @Override
        CompletableFuture<Void> connect(SocketListener listener) {
            if (webSocket != null) {
                return CompletableFuture.completedFuture(null);
            }

            super.connect(listener);
            try {
                var sslContext = SSLContext.getInstance("TLSv1.3");
                sslContext.init(null, null, null);
                var sslEngine = sslContext.createSSLEngine(WEB_SOCKET_HOST, WEB_SOCKET_PORT);
                sslEngine.setUseClientMode(true);
                this.webSocket = WebSocketClient.newSecureClient(sslEngine, proxy, this);
                var endpoint = proxy != null ? InetSocketAddress.createUnresolved(WEB_SOCKET_HOST, WEB_SOCKET_PORT) : new InetSocketAddress(WEB_SOCKET_HOST, WEB_SOCKET_PORT);
                return webSocket.connectAsync(endpoint, WEB_SOCKET_PATH)
                        .thenRunAsync(() -> listener.onOpen(this));
            }catch (Throwable throwable) {
                return CompletableFuture.failedFuture(throwable);
            }
        }

        @Override
        void disconnect() {
            if (webSocket == null) {
                return;
            }

            webSocket.close();
        }

        @Override
        public CompletableFuture<?> sendBinary(byte[] bytes) {
            if (webSocket == null) {
                return CompletableFuture.completedFuture(null);
            }

            return webSocket.sendBinary(ByteBuffer.wrap(bytes));
        }

        @Override
        public void onClose(int statusCode, String reason) {
            listener.onClose();
            this.webSocket = null;
        }

        @Override
        public void onMessage(ByteBuffer data, boolean last) {
            try {
                while (data.hasRemaining()) {
                    if(messageOutputStream.isReady()) {
                        if(data.remaining() < MESSAGE_LENGTH) {
                            break;
                        }

                        var messageLength = (data.get() << 16) | Short.toUnsignedInt(data.getShort());
                        if (messageLength < 0) {
                            disconnect();
                            return;
                        }

                        messageOutputStream.init(messageLength);
                    }

                    var result = messageOutputStream.write(data);
                    if(result != null) {
                        listener.onMessage(result, messageOutputStream.length());
                    }
                }
            } catch (Throwable throwable) {
                listener.onError(throwable);
            }
        }

        private final static class MessageOutputStream {
            private byte[] buffer;
            private int position;
            private int limit;
            public void init(int limit) {
                if(buffer == null || buffer.length < limit) {
                    this.buffer = new byte[limit];
                }

                this.limit = limit;
            }

            public byte[] write(ByteBuffer input) {
                if(!input.hasRemaining()) {
                    return null;
                }

                var remaining = Math.min(limit - position, input.remaining());
                input.get(buffer, position, remaining);
                position += remaining;
                if (position != limit) {
                    return null;
                }

                position = 0;
                return buffer;
            }

            public boolean isReady() {
                return position == 0;
            }

            public int length() {
                return limit;
            }
        }
    }

    static final class RawSocketSession extends SocketSession {
        private volatile SocketClient socket;

        RawSocketSession(URI proxy) {
            super(proxy);
        }

        @Override
        CompletableFuture<Void> connect(SocketListener listener) {
            if (isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            super.connect(listener);
            try {
                this.socket = SocketClient.newPlainClient(proxy);
                var address = proxy != null ? InetSocketAddress.createUnresolved(MOBILE_SOCKET_HOST, MOBILE_SOCKET_PORT) : new InetSocketAddress(MOBILE_SOCKET_HOST, MOBILE_SOCKET_PORT);
                return socket.connectAsync(address).thenRunAsync(() -> {
                    listener.onOpen(RawSocketSession.this);
                    notifyNextMessage();
                });
            }catch (IOException exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }

        private void notifyNextMessage() {
            if(socket == null) {
                return;
            }

            socket.readFullyAsync(
                    MESSAGE_LENGTH,
                    this::readNextMessageLength
            );
        }

        private void readNextMessageLength(ByteBuffer lengthBuffer, Throwable error) {
            if(error != null) {
                disconnect();
                return;
            }

            var messageLength = (lengthBuffer.get() << 16) | ((lengthBuffer.get() & 0xFF) << 8) | (lengthBuffer.get() & 0xFF);
            if(messageLength < 0) {
                disconnect();
                return;
            }

            socket.readFullyAsync(
                    messageLength,
                    this::notifyNextMessage
            );
        }

        private void notifyNextMessage(ByteBuffer messageBuffer, Throwable error) {
            if(error != null) {
                disconnect();
                return;
            }

            try {
                var message = messageBuffer.array();
                listener.onMessage(message, message.length);
            }catch (Throwable throwable) {
                listener.onError(throwable);
            }finally {
                notifyNextMessage();
            }
        }

        @Override
        void disconnect() {
            try {
                if (socket == null) {
                    return;
                }

                listener.onClose();
                socket.close();
                this.socket = null;
            } catch (Throwable ignored) {
                // No need to handle this
            }
        }

        private boolean isOpen() {
            return socket != null && socket.isConnected();
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            if (socket == null) {
                return CompletableFuture.completedFuture(null);
            }

            return socket.writeAsync(bytes);
        }
    }
}
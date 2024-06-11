package it.auties.whatsapp.implementation;

import it.auties.whatsapp.net.AsyncSocket;
import it.auties.whatsapp.util.Exceptions;
import it.auties.whatsapp.util.Proxies;
import it.auties.whatsapp.util.Validate;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract sealed class SocketSession permits SocketSession.WebSocketSession, SocketSession.RawSocketSession {
    public static final URI WEB_SOCKET_ENDPOINT = URI.create("wss://web.whatsapp.com/ws/chat");
    private static final String MOBILE_SOCKET_ENDPOINT = "g.whatsapp.net";
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

    public static final class WebSocketSession extends SocketSession implements WebSocket.Listener {
        private WebSocket session;
        private byte[] message;
        private int messageOffset;

        WebSocketSession(URI proxy) {
            super(proxy);
        }

        @SuppressWarnings("resource") // Not needed
        @Override
        CompletableFuture<Void> connect(SocketListener listener) {
            if (session != null) {
                return CompletableFuture.completedFuture(null);
            }

            super.connect(listener);
            var builder = HttpClient.newBuilder();
            if(proxy != null) {
                Validate.isTrue(Objects.equals(proxy.getScheme(), "http") || Objects.equals(proxy.getScheme(), "https"),
                        "Only HTTP(S) proxies are supported on the web api");
                builder.proxy(Proxies.toProxySelector(proxy));
                builder.authenticator(Proxies.toAuthenticator(proxy));
            }
            return builder.build()
                    .newWebSocketBuilder()
                    .buildAsync(WEB_SOCKET_ENDPOINT, this)
                    .thenAcceptAsync(webSocket -> {
                        this.session = webSocket;
                        listener.onOpen(this);
                    })
                    .exceptionallyAsync(throwable -> {
                        if(throwable instanceof CompletionException && throwable.getCause() instanceof ConnectException) {
                            throw new RuntimeException("Cannot connect to Whatsapp: check your connection and whether it's available in your country");
                        }

                        Exceptions.rethrow(throwable);
                        return null;
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
            message = null;
            listener.onClose();
            session = null;
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            listener.onError(error);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            if (message == null) {
                var length = (data.get() << 16) | Short.toUnsignedInt(data.getShort());
                if(length < 0) {
                    return WebSocket.Listener.super.onBinary(webSocket, data, last);
                }

                this.message = new byte[length];
                this.messageOffset = 0;
            }

            var currentDataLength = data.remaining();
            var remainingDataLength = message.length - messageOffset;
            var actualDataLength = Math.min(currentDataLength, remainingDataLength);
            data.get(message, messageOffset, actualDataLength);
            messageOffset += actualDataLength;
            if (messageOffset != message.length) {
                return WebSocket.Listener.super.onBinary(webSocket, data, last);
            }

            notifyMessage();
            if(remainingDataLength - currentDataLength != 0) {
                return onBinary(webSocket, data, true);
            }


            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }

        private void notifyMessage() {
            try {
                listener.onMessage(message);
            } catch (Throwable throwable) {
                listener.onError(throwable);
            }finally {
                this.message = null;
            }
        }
    }

    static final class RawSocketSession extends SocketSession {
        private volatile AsyncSocket socket;
        private final AtomicBoolean paused;

        RawSocketSession(URI proxy) {
            super(proxy);
            this.paused = new AtomicBoolean(false);
        }

        @Override
        CompletableFuture<Void> connect(SocketListener listener) {
            if (isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            super.connect(listener);
            try {
                this.socket = AsyncSocket.of(proxy);
                var address = proxy != null ? InetSocketAddress.createUnresolved(MOBILE_SOCKET_ENDPOINT, MOBILE_SOCKET_PORT) : new InetSocketAddress(MOBILE_SOCKET_ENDPOINT, MOBILE_SOCKET_PORT);
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

            var lengthBuffer = ByteBuffer.allocate(MESSAGE_LENGTH);
            socket.channel().read(lengthBuffer, null, new CompletionHandler<>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    if(result == -1) {
                        if(isOpen()) {
                            paused.set(true);
                        }else {
                            disconnect();
                        }

                        return;
                    }

                    lengthBuffer.flip();
                    var messageLength = (lengthBuffer.get() << 16) | ((lengthBuffer.get() & 0xFF) << 8) | (lengthBuffer.get() & 0xFF);
                    if(messageLength < 0) {
                        disconnect();
                        return;
                    }

                    var messageBuffer = ByteBuffer.allocate(messageLength);
                    notifyNextMessage(messageBuffer);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    disconnect();
                }
            });
        }

        private void notifyNextMessage(ByteBuffer messageBuffer) {
            if(socket == null) {
                return;
            }

            socket.channel().read(messageBuffer, null, new CompletionHandler<>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    if(result == -1 || messageBuffer.hasRemaining()) {
                        notifyNextMessage(messageBuffer);
                        return;
                    }

                    try {
                        listener.onMessage(messageBuffer.array());
                    }catch (Throwable throwable) {
                        listener.onError(throwable);
                    }finally {
                        notifyNextMessage();
                    }
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    disconnect();
                }
            });
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

            return socket.sendAsync(bytes).thenRunAsync(() -> {
                if(paused.get()) {
                    paused.set(false);
                    notifyNextMessage();
                }
            });
        }
    }
}
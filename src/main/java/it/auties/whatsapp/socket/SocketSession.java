package it.auties.whatsapp.socket;

import it.auties.whatsapp.exception.RequestException;
import it.auties.whatsapp.util.ProxyAuthenticator;
import it.auties.whatsapp.util.Specification;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import static it.auties.whatsapp.util.Specification.Whatsapp.SOCKET_ENDPOINT;
import static it.auties.whatsapp.util.Specification.Whatsapp.SOCKET_PORT;

public abstract sealed class SocketSession permits SocketSession.WebSocketSession, SocketSession.RawSocketSession {
    private static final int MESSAGE_LENGTH = 3;

    final URI proxy;
    final ExecutorService executor;
    final ReentrantLock outputLock;
    SocketListener listener;

    private SocketSession(URI proxy, ExecutorService executor) {
        this.proxy = proxy;
        this.executor = executor;
        this.outputLock = new ReentrantLock(true);
    }

    abstract CompletableFuture<Void> connect(SocketListener listener);

    abstract void disconnect();

    public abstract CompletableFuture<Void> sendBinary(byte[] bytes);

    static SocketSession of(URI proxy, ExecutorService executor, boolean webSocket) {
        if (webSocket) {
            return new WebSocketSession(proxy, executor);
        }

        return new RawSocketSession(proxy, executor);
    }

    public static final class WebSocketSession extends SocketSession implements WebSocket.Listener {
        private WebSocket session;
        private byte[] message;
        private int messageOffset;

        WebSocketSession(URI proxy, ExecutorService executor) {
            super(proxy, executor);
        }

        @SuppressWarnings("resource") // Not needed
        @Override
        CompletableFuture<Void> connect(SocketListener listener) {
            if (session != null) {
                return CompletableFuture.completedFuture(null);
            }

            this.listener = listener;
            return HttpClient.newBuilder()
                    .executor(executor)
                    .proxy(ProxySelector.of((InetSocketAddress) ProxyAuthenticator.getProxy(proxy).address()))
                    .authenticator(new ProxyAuthenticator())
                    .build()
                    .newWebSocketBuilder()
                    .buildAsync(Specification.Whatsapp.WEB_SOCKET_ENDPOINT, this)
                    .thenAccept(webSocket -> {
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
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            if (session == null) {
                return CompletableFuture.completedFuture(null);
            }

            outputLock.lock();
            return session.sendBinary(ByteBuffer.wrap(bytes), true)
                    .thenRun(outputLock::unlock)
                    .exceptionally(exception -> {
                        outputLock.unlock();
                        throw new RequestException(exception);
                    });
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
        static {
            Authenticator.setDefault(new ProxyAuthenticator());
        }

        private AsynchronousSocketChannel socket;
        private boolean closed;

        RawSocketSession(URI proxy, ExecutorService executor) {
            super(proxy, executor);
        }

        // TODO: Use proxy
        @Override
        CompletableFuture<Void> connect(SocketListener listener) {
            this.listener = listener;
            if (isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            var future = new CompletableFuture<Void>();
            try {
                this.socket = AsynchronousSocketChannel.open(AsynchronousChannelGroup.withThreadPool(executor));
                socket.connect(new InetSocketAddress(SOCKET_ENDPOINT, SOCKET_PORT), null, new ConnectionHandler(future));
            } catch (IOException exception) {
                future.completeExceptionally(exception);
            }

            return future;
        }

        private void readNextMessage() {
            if(!isOpen()) {
                disconnect();
                return;
            }

            var buffer = ByteBuffer.allocate(MESSAGE_LENGTH);
            socket.read(buffer, null, new MessageLengthHandler(buffer));
        }

        @Override
        void disconnect() {
            if (closed) {
                return;
            }

            try {
                this.closed = true;
                this.socket = null;
                listener.onClose();
                socket.close();
            } catch (Throwable ignored) {
                // Normal
            }
        }

        private boolean isOpen() {
            return socket != null && socket.isOpen();
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            try {
                outputLock.lock();
                if (socket == null) {
                    return CompletableFuture.completedFuture(null);
                }

                socket.write(ByteBuffer.wrap(bytes));
                return CompletableFuture.completedFuture(null);
            } catch (Throwable throwable) {
                return CompletableFuture.failedFuture(throwable);
            } finally {
                outputLock.unlock();
            }
        }

        private class ConnectionHandler implements CompletionHandler<Void, Void> {
            private final CompletableFuture<Void> future;
            private ConnectionHandler(CompletableFuture<Void> future) {
                this.future = future;
            }

            @Override
            public void completed(Void result, Void attachment) {
                listener.onOpen(RawSocketSession.this);
                executor.execute(RawSocketSession.this::readNextMessage);
                future.complete(null);
            }

            @Override
            public void failed(Throwable throwable, Void attachment) {
                future.completeExceptionally(throwable);
            }
        }

        private class MessageLengthHandler implements CompletionHandler<Integer, Void> {
            private final ByteBuffer lengthBuffer;
            private MessageLengthHandler(ByteBuffer lengthBuffer) {
                this.lengthBuffer = lengthBuffer;
            }

            @Override
            public void completed(Integer bytesRead, Void attachment) {
                if(lengthBuffer.remaining() != 0) {
                    socket.read(lengthBuffer, null, this);
                    return;
                }

                lengthBuffer.flip();
                var length = (lengthBuffer.get() << 16) | Short.toUnsignedInt(lengthBuffer.getShort());
                lengthBuffer.clear();
                if (length < 0) {
                    return;
                }

                var messageBuffer = ByteBuffer.allocate(length);
                socket.read(messageBuffer, null, new MessageValueHandler(messageBuffer));
            }

            @Override
            public void failed(Throwable throwable, Void attachment) {
                listener.onError(throwable);
            }
        }

        private class MessageValueHandler implements CompletionHandler<Integer, Void> {
            private final ByteBuffer messageBuffer;
            private MessageValueHandler(ByteBuffer messageBuffer) {
                this.messageBuffer = messageBuffer;
            }

            @Override
            public void completed(Integer bytesRead, Void attachment) {
                if(messageBuffer.remaining() != 0) {
                    socket.read(messageBuffer, null, this);
                    return;
                }

                try {
                    listener.onMessage(messageBuffer.array());
                    readNextMessage();
                } catch (Throwable throwable) {
                    listener.onError(throwable);
                }finally {
                    messageBuffer.clear();
                }
            }

            @Override
            public void failed(Throwable throwable, Void attachment) {
                listener.onError(throwable);
            }
        }
    }
}
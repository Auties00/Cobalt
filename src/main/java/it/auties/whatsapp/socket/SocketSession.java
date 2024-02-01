package it.auties.whatsapp.socket;

import it.auties.whatsapp.exception.RequestException;
import it.auties.whatsapp.util.ProxyAuthenticator;
import it.auties.whatsapp.util.Specification;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
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

        private Socket socket;

        RawSocketSession(URI proxy, ExecutorService executor) {
            super(proxy, executor);
        }

        @Override
        CompletableFuture<Void> connect(SocketListener listener) {
            this.listener = listener;
            if (isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.runAsync(() -> {
                try {
                    this.socket = new Socket(ProxyAuthenticator.getProxy(proxy));
                    socket.connect(new InetSocketAddress(SOCKET_ENDPOINT, SOCKET_PORT));
                    listener.onOpen(RawSocketSession.this);
                    executor.execute(this::readNextMessage);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        }

        private void readNextMessage() {
            while (isOpen()) {
                try {
                    var lengthBytes = readBytes(MESSAGE_LENGTH);
                    if (lengthBytes == null) {
                        continue;
                    }

                    var length = (lengthBytes[0] << 16) | ((lengthBytes[1] & 0xFF) << 8) | (lengthBytes[2] & 0xFF);
                    if (length < 0) {
                        continue;
                    }

                    var data = readBytes(length);
                    if (data == null) {
                        continue;
                    }

                    listener.onMessage(data);
                } catch (Throwable throwable) {
                    listener.onError(throwable);
                }
            }

            disconnect();
        }

        private byte[] readBytes(int size) {
            try {
                var data = new byte[size];
                var read = 0;
                while (read != data.length) {
                    var chunk = socket.getInputStream().read(data, read, data.length - read);
                    read += chunk;
                }

                return data;
            } catch (Throwable exception) {
                return null;
            }
        }

        @Override
        void disconnect() {
            if (socket == null) {
                return;
            }

            try {
                listener.onClose();
                socket.close();
                this.socket = null;
            } catch (IOException ignored) {

            }
        }

        private boolean isOpen() {
            return socket != null && !socket.isClosed();
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            if (socket == null) {
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.runAsync(() -> {
                try {
                    outputLock.lock();
                    socket.getOutputStream().write(bytes);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                } finally {
                    outputLock.unlock();
                }
            });
        }
    }
}
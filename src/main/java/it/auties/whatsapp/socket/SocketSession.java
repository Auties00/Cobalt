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

public abstract sealed class SocketSession permits SocketSession.WebSocketSession, SocketSession.RawSocketSession {
    private static final int MESSAGE_LENGTH = 3;
    static {
        ProxyAuthenticator.allowAll();
    }

    final URI proxy;
    final ExecutorService executor;
    final ReentrantLock outputLock;
    SocketListener listener;

    private SocketSession(URI proxy, ExecutorService executor) {
        this.proxy = proxy;
        this.executor = executor;
        this.outputLock = new ReentrantLock(true);
    }

    CompletableFuture<Void> connect(SocketListener listener) {
        this.listener = listener;
        return CompletableFuture.completedFuture(null);
    }

    abstract void disconnect();

    public abstract CompletableFuture<?> sendBinary(byte[] bytes);

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

            super.connect(listener);
            return HttpClient.newBuilder()
                    .executor(executor)
                    .proxy(ProxySelector.of((InetSocketAddress) ProxyAuthenticator.getProxy(proxy).address()))
                    .authenticator(ProxyAuthenticator.globalAuthenticator())
                    .build()
                    .newWebSocketBuilder()
                    .buildAsync(Specification.Whatsapp.WEB_SOCKET_ENDPOINT, this)
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

            outputLock.lock();
            return session.sendBinary(ByteBuffer.wrap(bytes), true).whenComplete((result, error) -> {
                outputLock.unlock();
                if(error != null) {
                    throw new RequestException(error);
                }
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
            Authenticator.setDefault(ProxyAuthenticator.globalAuthenticator());
        }

        private Socket socket;

        RawSocketSession(URI proxy, ExecutorService executor) {
            super(proxy, executor);
        }

        @Override
        CompletableFuture<Void> connect(SocketListener listener) {
            if (isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            super.connect(listener);
            return CompletableFuture.runAsync(() -> createConnection(listener));
        }

        private void createConnection(SocketListener listener) {
            try {
                this.socket = new Socket(ProxyAuthenticator.getProxy(proxy));
                socket.setKeepAlive(true);
                socket.connect(proxy != null ? InetSocketAddress.createUnresolved(SOCKET_ENDPOINT.getHost(), SOCKET_ENDPOINT.getPort()) : new InetSocketAddress(SOCKET_ENDPOINT.getHost(), SOCKET_ENDPOINT.getPort()));
                listener.onOpen(RawSocketSession.this);
                executor.execute(this::readNextMessage);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        private void readNextMessage() {
            var lengthBytes = new byte[MESSAGE_LENGTH];
            int length;
            while (isOpen()) {
                try {
                    var lengthResult = readBytes(lengthBytes);
                    if(!lengthResult) {
                        break;
                    }

                    length = (lengthBytes[0] << 16) | ((lengthBytes[1] & 0xFF) << 8) | (lengthBytes[2] & 0xFF);
                    if (length < 0) {
                        break;
                    }

                    var messageBytes = new byte[length];
                    var messageResult = readBytes(messageBytes);
                    if(!messageResult) {
                        break;
                    }

                    listener.onMessage(messageBytes);
                } catch (Throwable throwable) {
                    listener.onError(throwable);
                }
            }

            disconnect();
        }

        private boolean readBytes(byte[] data) {
            try {
                var read = 0;
                while (read != data.length) {
                    var chunk = socket.getInputStream().read(data, read, data.length - read);
                    if (chunk < 0) {
                        return false;
                    }

                    read += chunk;
                }
                return true;
            }catch (SocketException exception) {
                return false;
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
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

            return CompletableFuture.runAsync(() -> {
                try {
                    outputLock.lock();
                    socket.getOutputStream().write(bytes);
                    socket.getOutputStream().flush();
                } catch (IOException exception) {
                    System.out.println("error");
                    throw new UncheckedIOException(exception);
                } finally {
                    outputLock.unlock();
                }
            });
        }
    }
}
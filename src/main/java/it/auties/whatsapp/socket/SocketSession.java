package it.auties.whatsapp.socket;

import it.auties.whatsapp.exception.RequestException;
import it.auties.whatsapp.util.ProxyAuthenticator;
import it.auties.whatsapp.util.Specification;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.net.Proxy.Type;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

import static it.auties.whatsapp.util.Specification.Whatsapp.SOCKET_ENDPOINT;
import static it.auties.whatsapp.util.Specification.Whatsapp.SOCKET_PORT;

public abstract sealed class SocketSession permits SocketSession.WebSocketSession, SocketSession.RawSocketSession {
    final URI proxy;
    final Executor executor;
    final ReentrantLock outputLock;
    SocketListener listener;

    private SocketSession(URI proxy, Executor executor) {
        this.proxy = proxy;
        this.executor = executor;
        this.outputLock = new ReentrantLock(true);
    }

    abstract CompletableFuture<Void> connect(SocketListener listener);

    abstract void disconnect();

    public abstract CompletableFuture<Void> sendBinary(byte[] bytes);

    abstract boolean isOpen();

    static SocketSession of(URI proxy, Executor executor, boolean webSocket) {
        if (webSocket) {
            return new WebSocketSession(proxy, executor);
        }

        return new RawSocketSession(proxy, executor);
    }

    Proxy getProxy() {
        if (proxy == null) {
            return Proxy.NO_PROXY;
        }

        var scheme = Objects.requireNonNull(proxy.getScheme(), "Invalid proxy, expected a scheme: %s".formatted(proxy));
        var host = Objects.requireNonNull(proxy.getHost(), "Invalid proxy, expected a host: %s".formatted(proxy));
        var port = getProxyPort(scheme).orElseThrow(() -> new NullPointerException("Invalid proxy, expected a port: %s".formatted(proxy)));
        return switch (scheme) {
            case "http", "https" -> new Proxy(Type.HTTP, new InetSocketAddress(host, port));
            case "socks4", "socks5" -> new Proxy(Type.SOCKS, new InetSocketAddress(host, port));
            default -> throw new IllegalStateException("Unexpected scheme: " + scheme);
        };
    }

    private OptionalInt getProxyPort(String scheme) {
        return proxy.getPort() != -1 ? OptionalInt.of(proxy.getPort()) : switch (scheme) {
            case "http" -> OptionalInt.of(80);
            case "https" -> OptionalInt.of(443);
            default -> OptionalInt.empty();
        };
    }

    public static final class WebSocketSession extends SocketSession implements WebSocket.Listener {
        private WebSocket session;
        private final List<ByteBuffer> inputParts;
        private int readableBytes;

        WebSocketSession(URI proxy, Executor executor) {
            super(proxy, executor);
            this.inputParts = new ArrayList<>(5);
        }

        @SuppressWarnings("resource") // Not needed
        @Override
        CompletableFuture<Void> connect(SocketListener listener) {
            if (isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            this.listener = listener;
            return HttpClient.newBuilder()
                    .executor(executor)
                    .proxy(ProxySelector.of((InetSocketAddress) getProxy().address()))
                    .authenticator(new ProxyAuthenticator())
                    .build()
                    .newWebSocketBuilder()
                    .buildAsync(Specification.Whatsapp.WEB_SOCKET_ENDPOINT, this)
                    .thenRun(() -> listener.onOpen(this));
        }

        @Override
        void disconnect() {
            if (!isOpen()) {
                return;
            }

            session.sendClose(WebSocket.NORMAL_CLOSURE, "");
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            outputLock.lock();
            return session.sendBinary(ByteBuffer.wrap(bytes), true)
                    .thenRun(outputLock::unlock)
                    .exceptionally(exception -> {
                        outputLock.unlock();
                        throw new RequestException(exception);
                    });
        }

        @Override
        boolean isOpen() {
            return session != null && !session.isInputClosed() && !session.isOutputClosed();
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            this.session = webSocket;
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            inputParts.clear();
            readableBytes = 0;
            listener.onClose();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            listener.onError(error);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            inputParts.add(data);
            readableBytes += data.remaining();
            if (!last) {
                return WebSocket.Listener.super.onBinary(webSocket, data, false);
            }

            var inputPartsCounter = 0;
            while (inputPartsCounter < inputParts.size()) {
                var inputPart = inputParts.get(inputPartsCounter);
                if(inputPart.remaining() < 3) {
                    inputPartsCounter++;
                    continue;
                }

                var length = (inputPart.get() << 16) | Short.toUnsignedInt(inputPart.getShort());
                if (length < 0) {
                    break;
                }

                readableBytes -= length;
                var result = new byte[length];
                var remaining = length;
                while (remaining > 0) {
                    var inputPartSize = inputPart.remaining();
                    inputPart.get(result, length - remaining, Math.min(inputPartSize, length));
                    if(inputPartSize < remaining) {
                        inputPart = inputParts.get(++inputPartsCounter);
                    }

                    remaining -= inputPartSize;
                }

                try {
                    listener.onMessage(result);
                } catch (Throwable throwable) {
                    listener.onError(throwable);
                }
            }

            readableBytes = 0;
            inputParts.clear();
            return WebSocket.Listener.super.onBinary(webSocket, data, true);
        }
    }

    static final class RawSocketSession extends SocketSession {
        static {
            Authenticator.setDefault(new ProxyAuthenticator());
        }

        private Socket socket;
        private boolean closed;

        RawSocketSession(URI proxy, Executor executor) {
            super(proxy, executor);
        }

        @Override
        CompletableFuture<Void> connect(SocketListener listener) {
            if (isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.runAsync(() -> {
                try {
                    this.listener = listener;
                    this.socket = new Socket(getProxy());
                    socket.setKeepAlive(true);
                    socket.connect(new InetSocketAddress(SOCKET_ENDPOINT, SOCKET_PORT));
                    executor.execute(this::readMessages);
                    this.closed = false;
                    listener.onOpen(this);
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot connect to host", exception);
                }
            }, executor);
        }

        @Override
        void disconnect() {
            if (closed) {
                return;
            }

            try {
                socket.close();
                this.closed = true;
                this.socket = null;
                listener.onClose();
            } catch (IOException exception) {
                listener.onError(exception);
            }
        }

        @Override
        public boolean isOpen() {
            return socket != null && socket.isConnected();
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            return CompletableFuture.runAsync(() -> {
                try {
                    outputLock.lock();
                    if (socket == null) {
                        return;
                    }
                    var stream = socket.getOutputStream();
                    stream.write(bytes);
                    stream.flush();
                } catch (SocketException exception) {
                    disconnect();
                } catch (IOException exception) {
                    throw new RequestException(exception);
                } finally {
                    outputLock.unlock();
                }
            }, executor);
        }

        private void readMessages() {
            try (var input = new DataInputStream(socket.getInputStream())) {
                while (isOpen()) {
                    var length = (input.readByte() << 16) | input.readUnsignedShort();
                    if (length < 0) {
                        break;
                    }

                    var message = new byte[length];
                    input.readFully(message);
                    System.out.println(message.length);
                    try {
                        listener.onMessage(message);
                    }catch (Throwable throwable) {
                        listener.onError(throwable);
                    }
                }
            } catch (EOFException ignored) {

            } catch (Throwable throwable) {
                listener.onError(throwable);
            } finally {
                disconnect();
            }
        }
    }
}
package it.auties.whatsapp.socket;

import io.netty.buffer.ByteBuf;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.ProxyAuthenticator;
import it.auties.whatsapp.util.Specification;
import jakarta.websocket.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.net.Proxy.Type;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static it.auties.whatsapp.util.Specification.Whatsapp.SOCKET_ENDPOINT;
import static it.auties.whatsapp.util.Specification.Whatsapp.SOCKET_PORT;

public abstract sealed class SocketSession permits SocketSession.WebSocketSession, SocketSession.RawSocketSession {
    final URI proxy;
    final Executor executor;
    SocketListener listener;
    private SocketSession(URI proxy, Executor executor) {
        this.proxy = proxy;
        this.executor = executor;
    }

    abstract CompletableFuture<Void> connect(SocketListener listener);
    abstract CompletableFuture<Void> disconnect();
    public abstract CompletableFuture<Void> sendBinary(byte[] bytes);
    abstract boolean isOpen();

    int decodeLength(ByteBuf buffer) {
        return (buffer.readByte() << 16) | buffer.readUnsignedShort();
    }

    static SocketSession of(URI proxy, Executor executor, boolean webSocket){
        if(webSocket) {
            return new WebSocketSession(proxy, executor);
        }

        return new RawSocketSession(proxy, executor);
    }

    @ClientEndpoint(configurator = WebSocketSession.OriginPatcher.class)
    public static final class WebSocketSession extends SocketSession {
        private Session session;

        WebSocketSession(URI proxy, Executor executor) {
            super(proxy, executor);
        }

        @Override
        public CompletableFuture<Void> connect(SocketListener listener) {
            return CompletableFuture.runAsync(() -> {
                try {
                    this.listener = listener;
                    this.session = ContainerProvider.getWebSocketContainer().connectToServer(this, Specification.Whatsapp.WEB_SOCKET_ENDPOINT);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                } catch (DeploymentException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }

        @Override
        public CompletableFuture<Void> disconnect() {
            try {
                session.close();
                return CompletableFuture.completedFuture(null);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            var future = new CompletableFuture<Void>();
            try {
                session.getAsyncRemote().sendBinary(ByteBuffer.wrap(bytes), result -> {
                    if (result.isOK()) {
                        future.complete(null);
                        return;
                    }

                    future.completeExceptionally(result.getException());
                });
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            return future;
        }

        @Override
        public boolean isOpen() {
            return session == null || session.isOpen();
        }

        @OnOpen
        @SuppressWarnings("unused")
        public void onOpen(Session session) {
            this.session = session;
            listener.onOpen(this);
        }

        @OnClose
        @SuppressWarnings("unused")
        public void onClose() {
            listener.onClose();
        }

        @OnError
        @SuppressWarnings("unused")
        public void onError(Throwable throwable) {
            listener.onError(throwable);
        }

        @OnMessage
        @SuppressWarnings("unused")
        public void onBinary(byte[] message) {
            var buffer = BytesHelper.newBuffer(message);
            while (buffer.readableBytes() >= 3) {
                var length = decodeLength(buffer);
                if (length < 0) {
                    continue;
                }

                var result = buffer.readBytes(length);
                listener.onMessage(BytesHelper.readBuffer(result));
                result.release();
            }
            buffer.release();
        }

        public static class OriginPatcher extends ClientEndpointConfig.Configurator {
            @Override
            public void beforeRequest(Map<String, List<String>> headers) {
                headers.put("Origin", List.of(Specification.Whatsapp.WEB_ORIGIN));
                headers.put("Host", List.of(Specification.Whatsapp.WEB_HOST));
            }
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
            if (socket != null && isOpen()) {
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

        private Proxy getProxy() {
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

        @Override
        @SuppressWarnings("UnusedReturnValue")
        CompletableFuture<Void> disconnect() {
            if (socket == null || !isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.runAsync(() -> {
                try {
                    socket.close();
                    closeResources();
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot close connection to host", exception);
                }
            }, executor);
        }

        @Override
        public boolean isOpen() {
            return socket != null && socket.isConnected();
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            return CompletableFuture.runAsync(() -> {
                try {
                    if (socket == null) {
                        return;
                    }
                    var stream = socket.getOutputStream();
                    stream.write(bytes);
                    stream.flush();
                }catch (SocketException exception) {
                    closeResources();
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot send message", exception);
                }
            }, executor);
        }

        private void readMessages() {
            try (var input = new DataInputStream(socket.getInputStream())) {
                while (isOpen()) {
                    var length = decodeLength(input);
                    if (length < 0) {
                        break;
                    }
                    var message = new byte[length];
                    if(isOpen()) {
                        input.readFully(message);
                    }
                    listener.onMessage(message);
                }
            } catch(Throwable throwable) {
                listener.onError(throwable);
            } finally {
                closeResources();
            }
        }

        private int decodeLength(DataInputStream input) {
            try {
                var lengthBytes = new byte[3];
                input.readFully(lengthBytes);
                return decodeLength(BytesHelper.newBuffer(lengthBytes));
            } catch (IOException exception) {
                return -1;
            }
        }

        private void closeResources() {
            if(closed) {
                return;
            }

            this.closed = true;
            this.socket = null;
            if (executor instanceof ExecutorService service) {
                service.shutdownNow();
            }

            listener.onClose();
        }
    }
}
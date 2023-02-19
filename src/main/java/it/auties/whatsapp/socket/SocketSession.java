package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.socket.SocketSession.AppSocketSession;
import it.auties.whatsapp.socket.SocketSession.WebSocketSession;
import it.auties.whatsapp.socket.SocketSession.WebSocketSession.OriginPatcher;
import it.auties.whatsapp.util.Specification;
import jakarta.websocket.*;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import lombok.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static it.auties.whatsapp.util.Specification.Whatsapp.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract sealed class SocketSession permits WebSocketSession, AppSocketSession {
    protected SocketListener listener;
    protected boolean closed;

    static SocketSession of(ClientType type) {
        return switch (type) {
            case WEB_CLIENT -> new WebSocketSession();
            case APP_CLIENT -> new AppSocketSession();
        };
    }

    public CompletableFuture<Void> connect(SocketListener listener) {
        this.listener = listener;
        this.closed = false;
        return null;
    }

    public CompletableFuture<Void> close() {
        this.closed = true;
        return null;
    }

    abstract boolean isOpen();

    public abstract CompletableFuture<Void> sendBinary(byte[] bytes);

    @ClientEndpoint(configurator = OriginPatcher.class)
    public final static class WebSocketSession extends SocketSession {
        private Session session;

        @Override
        public CompletableFuture<Void> connect(SocketListener listener) {
            return CompletableFuture.runAsync(() -> {
                try {
                    super.connect(listener);
                    ContainerProvider.getWebSocketContainer().connectToServer(this, WEB_ENDPOINT);
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot connect to host", exception);
                } catch (DeploymentException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }

        @Override
        public CompletableFuture<Void> close() {
            return CompletableFuture.runAsync(() -> {
                try {
                    if (closed) {
                        return;
                    }

                    session.close();
                    super.close();
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot close connection to host", exception);
                }
            });
        }

        @Override
        public boolean isOpen() {
            return session == null || session.isOpen();
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

        @OnOpen
        public void onOpen(Session session) {
            this.session = session;
            listener.onOpen(this);
        }

        @OnClose
        public void onClose() {
            listener.onClose();
        }

        @OnError
        public void onError(Throwable throwable) {
            listener.onError(throwable);
        }

        @OnMessage
        public void onBinary(byte[] message) {
            listener.onMessage(message);
        }

        public static class OriginPatcher extends Configurator {
            @Override
            public void beforeRequest(@NonNull Map<String, List<String>> headers) {
                headers.put("Origin", List.of(Specification.Whatsapp.WEB_ORIGIN));
                headers.put("Host", List.of(Specification.Whatsapp.WEB_HOST));
            }
        }
    }

    public final static class AppSocketSession extends SocketSession {
        private static final int MAX_READ_SIZE = 65535;

        private Socket socket;
        private ExecutorService service;

        @Override
        public CompletableFuture<Void> connect(SocketListener listener) {
            return CompletableFuture.runAsync(() -> {
                try {
                    super.connect(listener);
                    this.socket = new Socket();
                    socket.setKeepAlive(true);
                    socket.connect(new InetSocketAddress(APP_ENDPOINT_HOST, APP_ENDPOINT_PORT));
                    this.service = Executors.newSingleThreadScheduledExecutor();
                    service.execute(this::readMessages);
                    listener.onOpen(this);
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot connect to host", exception);
                }
            });
        }

        @Override
        public CompletableFuture<Void> close() {
            return CompletableFuture.runAsync(() -> {
                try {
                    super.close();
                    socket.close();
                    closeResources();
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot close connection to host", exception);
                }
            });
        }

        @Override
        public boolean isOpen() {
            return !socket.isClosed();
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            return CompletableFuture.runAsync(() -> {
                try {
                    System.out.println("Sending: " + Arrays.toString(bytes));
                    var stream = socket.getOutputStream();
                    stream.write(bytes);
                    stream.flush();
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot send message", exception);
                }
            });
        }

        private void readMessages() {
            var bytes = new byte[MAX_READ_SIZE];
            while (isOpen()) {
                try {
                    var stream = socket.getInputStream();
                    var size = stream.read(bytes);
                    System.out.println("Received: " + Arrays.toString(Arrays.copyOf(bytes, size)));
                    listener.onMessage(Arrays.copyOf(bytes, size));
                } catch (SocketException exception) {
                    closeResources();
                } catch (IOException exception) {
                    listener.onError(exception);
                }
            }
            closeResources();
        }

        private void closeResources() {
            this.socket = null;
            service.shutdownNow();
            listener.onClose();
        }
    }
}

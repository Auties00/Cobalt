package it.auties.whatsapp.socket;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.api.WhatsappOptions;
import it.auties.whatsapp.socket.SocketSession.AppSocketSession;
import it.auties.whatsapp.socket.SocketSession.WebSocketSession;
import it.auties.whatsapp.socket.SocketSession.WebSocketSession.OriginPatcher;
import it.auties.whatsapp.util.Spec;
import jakarta.websocket.*;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static it.auties.whatsapp.util.Spec.Whatsapp.*;

@RequiredArgsConstructor
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract sealed class SocketSession permits WebSocketSession, AppSocketSession {
    protected final Executor executor;
    protected UUID uuid;
    protected SocketListener listener;
    protected boolean closed;

    static SocketSession of(WhatsappOptions options) {
        return switch (options.clientType()) {
            case WEB_CLIENT -> new WebSocketSession(options.socketService());
            case APP_CLIENT -> new AppSocketSession(options.socketService());
        };
    }

    public CompletableFuture<Void> connect(SocketListener listener) {
        this.uuid = UUID.randomUUID();
        this.listener = listener;
        this.closed = false;
        return null;
    }

    protected CompletableFuture<Void> close() {
        this.closed = true;
        return null;
    }

    protected abstract boolean isOpen();

    public abstract CompletableFuture<Void> sendBinary(byte[] bytes);

    int decodeLength(Bytes buffer) {
        return (buffer.readByte() << 16) | buffer.readUnsignedShort();
    }

    @ClientEndpoint(configurator = OriginPatcher.class)
    public final static class WebSocketSession extends SocketSession {
        private Session session;

        public WebSocketSession(Executor executor) {
            super(executor);
        }

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
            }, executor);
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
            }, executor);
        }

        @Override
        public boolean isOpen() {
            return session == null || session.isOpen();
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            var currentUuid = this.uuid;
            var future = new CompletableFuture<Void>();
            try {
                session.getAsyncRemote().sendBinary(ByteBuffer.wrap(bytes), result -> {
                    if(!Objects.equals(this.uuid, currentUuid)){
                        future.completeExceptionally(new IllegalStateException("Cannot send request: session was closed"));
                        return;
                    }

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
        public void onClose(Session session, CloseReason closeReason) {
            listener.onClose();
        }

        @OnError
        public void onError(Throwable throwable) {
            listener.onError(throwable);
        }

        @OnMessage
        public void onBinary(byte[] message) {
            var raw = Bytes.of(message);
            while (raw.readableBytes() >= 3) {
                var length = decodeLength(raw);
                if (length < 0 || length > raw.readableBytes()) {
                    continue;
                }
                listener.onMessage(raw.readBytes(length));
            }
        }

        public static class OriginPatcher extends Configurator {
            @Override
            public void beforeRequest(@NonNull Map<String, List<String>> headers) {
                headers.put("Origin", List.of(Spec.Whatsapp.WEB_ORIGIN));
                headers.put("Host", List.of(Spec.Whatsapp.WEB_HOST));
            }
        }
    }

    public final static class AppSocketSession extends SocketSession {
        private static final int MAX_READ_SIZE = 65535;

        private Socket socket;

        public AppSocketSession(Executor executor) {
            super(executor);
        }

        @Override
        public CompletableFuture<Void> connect(SocketListener listener) {
            if (socket != null && isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.runAsync(() -> {
                try {
                    super.connect(listener);
                    this.socket = new Socket();
                    socket.setKeepAlive(true);
                    socket.connect(new InetSocketAddress(APP_ENDPOINT_HOST, APP_ENDPOINT_PORT));
                    executor.execute(this::readMessages);
                    listener.onOpen(this);
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot connect to host", exception);
                }
            }, executor);
        }

        @Override
        public CompletableFuture<Void> close() {
            if (socket == null || !isOpen()) {
                return CompletableFuture.completedFuture(null);
            }

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
            return socket.isConnected();
        }

        @Override
        public CompletableFuture<Void> sendBinary(byte[] bytes) {
            return CompletableFuture.runAsync(() -> {
                try {
                    var stream = socket.getOutputStream();
                    stream.write(bytes);
                    stream.flush();
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot send message", exception);
                }
            }, executor);
        }


        private void readMessages() {
            try(var input = new DataInputStream(socket.getInputStream())) {
                while (isOpen()) {
                    var length = decodeLength(input);
                    if(length < 0){
                        break;
                    }
                    var message = new byte[length];
                    input.readFully(message);
                    listener.onMessage(message);
                }
            }catch (SocketException exception) {
                closeResources();
            }catch (IOException exception) {
                listener.onError(exception);
            }

            closeResources();
        }

        private int decodeLength(DataInputStream input) {
           try {
               var lengthBytes = new byte[3];
               input.readFully(lengthBytes);
               var buffer = Bytes.of(lengthBytes);
               return decodeLength(buffer);
           }catch (IOException exception){
               return -1;
           }
        }

        private void closeResources() {
            this.socket = null;
            if(executor instanceof ExecutorService service) {
                service.shutdownNow();
            }
            listener.onClose();
        }
    }
}

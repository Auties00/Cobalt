package it.auties.whatsapp.socket;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.util.ProxyAuthenticator;
import it.auties.whatsapp.util.Validate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.net.Proxy.Type;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static it.auties.whatsapp.util.Spec.Whatsapp.APP_ENDPOINT_HOST;
import static it.auties.whatsapp.util.Spec.Whatsapp.APP_ENDPOINT_PORT;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class SocketSession {
    static {
        Authenticator.setDefault(new ProxyAuthenticator());
    }

    private final URI proxy;
    private final Executor executor;
    private Socket socket;
    private SocketListener listener;
    private boolean closed;

    CompletableFuture<Void> connect(SocketListener listener) {
        if (socket != null && isOpen()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                this.listener = listener;
                this.closed = false;
                this.socket = new Socket(getProxy());
                socket.setKeepAlive(true);
                socket.connect(new InetSocketAddress(APP_ENDPOINT_HOST, APP_ENDPOINT_PORT));
                executor.execute(this::readMessages);
                listener.onOpen(this);
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot connect to host", exception);
            }
        }, executor);
    }

    @SuppressWarnings("DataFlowIssue")
    private Proxy getProxy() {
        if (proxy == null) {
            return Proxy.NO_PROXY;
        }

        var scheme = proxy.getScheme();
        Validate.isTrue(scheme != null, "Invalid proxy, expected a scheme: %s".formatted(proxy));
        var host = proxy.getHost();
        Validate.isTrue(host != null, "Invalid proxy, expected a host: %s".formatted(proxy));
        var port = getProxyPort(scheme).orElseThrow(() -> new IllegalArgumentException("Invalid proxy, expected a port: %s".formatted(proxy)));
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

    @SuppressWarnings("UnusedReturnValue")
    CompletableFuture<Void> close() {
        if (socket == null || !isOpen()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                this.closed = true;
                socket.close();
                closeResources();
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot close connection to host", exception);
            }
        }, executor);
    }

    public boolean isOpen() {
        return socket != null && socket.isConnected();
    }

    public CompletableFuture<Void> sendBinary(byte[] bytes) {
        return CompletableFuture.runAsync(() -> {
            try {
                if(socket == null){
                    return;
                }
                var stream = socket.getOutputStream();
                stream.write(bytes);
                stream.flush();
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
                input.readFully(message);
                listener.onMessage(message);
            }
        } catch (SocketException ignored){

        } catch(Throwable throwable) {
            listener.onError(throwable);
        } finally {
            if (!closed) {
                closeResources();
            }
        }
    }

    private int decodeLength(DataInputStream input) {
        try {
            var lengthBytes = new byte[3];
            input.readFully(lengthBytes);
            var buffer = Bytes.of(lengthBytes);
            return decodeLength(buffer);
        } catch (IOException exception) {
            return -1;
        }
    }

    private int decodeLength(Bytes buffer) {
        return (buffer.readByte() << 16) | buffer.readUnsignedShort();
    }

    private void closeResources() {
        this.socket = null;
        if (executor instanceof ExecutorService service) {
            service.shutdownNow();
        }

        listener.onClose();
    }
}
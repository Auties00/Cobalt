package it.auties.whatsapp.util;

import sun.misc.Unsafe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.OptionalInt;

// Treat all addresses as unresolved
public abstract class SocketWithProxy extends Socket {
    static {
        allowBasicAuth();
    }

    public static void allowBasicAuth() {
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    }

    public static ProxySelector toProxySelector(URI uri) {
        if (uri == null) {
            return null;
        }

        var scheme = Objects.requireNonNull(uri.getScheme(), "Invalid proxy, expected a scheme: %s".formatted(uri));
        Validate.isTrue(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"),
        "Only HTTP and HTTPS proxies are supported in this context");
        var host = Objects.requireNonNull(uri.getHost(), "Invalid proxy, expected a host: %s".formatted(uri));
        var port = getProxyPort(scheme, uri.getPort()).orElseThrow(() -> new NullPointerException("Invalid proxy, expected a port: %s".formatted(uri)));
        return ProxySelector.of(InetSocketAddress.createUnresolved(host, port));
    }
    
    public static Proxy toProxy(URI uri) {
        if (uri == null) {
            return Proxy.NO_PROXY;
        }

        var scheme = Objects.requireNonNull(uri.getScheme(), "Invalid proxy, expected a scheme: %s".formatted(uri));
        var host = Objects.requireNonNull(uri.getHost(), "Invalid proxy, expected a host: %s".formatted(uri));
        var port = getProxyPort(scheme, uri.getPort()).orElseThrow(() -> new NullPointerException("Invalid proxy, expected a port: %s".formatted(uri)));
        return switch (scheme) {
            case "http", "https" -> new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, port));
            case "socks5", "socks5h" -> new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(host, port));
            default -> throw new IllegalStateException("Unexpected scheme: " + scheme);
        };
    }

    private static OptionalInt getProxyPort(String scheme, int port) {
        return port != -1 ? OptionalInt.of(port) : switch (scheme) {
            case "http" -> OptionalInt.of(80);
            case "https" -> OptionalInt.of(443);
            default -> OptionalInt.empty();
        };
    }

    private static UserInfo parseUserInfo(String userInfo) {
        if(userInfo == null || userInfo.isEmpty()) {
            return null;
        }

        var data = userInfo.split(":", 2);
        if(data.length > 2) {
            throw new IllegalArgumentException("Invalid proxy authentication: " + userInfo);
        }

        return new UserInfo(data[0], data.length == 2 ? data[1] : null);
    }

    private record UserInfo(String username, String password) {

    }

    public static Authenticator toAuthenticator(URI proxy) {
        if(proxy == null) {
            return null;
        }

        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                Validate.isTrue(Objects.equals(getRequestingHost(), proxy.getHost()) && Objects.equals(getRequestingPort(), proxy.getPort()),
                        "Unexpected proxy request: %s:%s", getRequestingHost(), getRequestingPort());
                var userInfo = parseUserInfo(proxy.getUserInfo());
                if(userInfo == null) {
                    return null;
                }

                return new PasswordAuthentication(userInfo.username(), userInfo.password().toCharArray());
            }
        };
    }

    final URI proxy;
    private SocketWithProxy(URI proxy) {
        this.proxy = proxy;
    }

    public static SocketWithProxy of(URI proxy) throws IOException {
        if(proxy == null) {
            return new Direct();
        }

        var javaProxy = toProxy(proxy);
        if(javaProxy == Proxy.NO_PROXY) {
            return new Direct();
        }

        return switch (javaProxy.type()) {
            case DIRECT -> new Direct();
            case HTTP -> new Http(proxy);
            case SOCKS -> new Socks5(proxy);
        };
    }


    private static final class Http extends SocketWithProxy {
        private static final Unsafe unsafe;
        private static final long offset;
        static {
            unsafe = initUnsafe();
            offset = initOffset();
        }

        private static Unsafe initUnsafe() {
            try {
                var unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                return (Unsafe) unsafeField.get(null);
            }catch (ReflectiveOperationException exception) {
                throw new RuntimeException("Cannot access unsafe");
            }
        }

        @SuppressWarnings("all")
        private static long initOffset() {
            try {
                class AccessibleObjectPlaceholder {
                    boolean override;
                    Object accessCheckCache;
                }

                var offsetField = AccessibleObjectPlaceholder.class.getDeclaredField("override");
                return unsafe.objectFieldOffset(offsetField);
            }catch (ReflectiveOperationException exception) {
                throw new RuntimeException("Cannot access override field", exception);
            }
        }

        private HttpURLConnection httpConnection;
        private Object serverSocket;
        private boolean disconnected;
        Http(URI proxy) {
            super(proxy);
        }

        @Override
        public void connect(SocketAddress endpoint) throws IOException {
            connect(endpoint, -1);
        }

        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            if(!(endpoint instanceof InetSocketAddress address)) {
                throw new IllegalArgumentException("Unsupported address type");
            }

            var uri = URI.create("http://%s:%s/".formatted(address.getHostName(), address.getPort()));
            this.httpConnection = (HttpURLConnection) uri.toURL().openConnection(toProxy(proxy));
            httpConnection.setAuthenticator(toAuthenticator(proxy));
            if(timeout > 0 ) {
                httpConnection.setConnectTimeout(timeout);
            }
            httpConnection.connect();
            doTunneling();
        }

        private void doTunneling() {
            try {
                var doTunnellingMethod = httpConnection.getClass().getMethod("doTunneling");
                unsafe.putBoolean(doTunnellingMethod, offset, true);
                doTunnellingMethod.invoke(httpConnection);
            }catch (ReflectiveOperationException exception) {
                throw new RuntimeException("Cannot enable tunneling", exception);
            }
        }

        @Override
        public boolean isClosed() {
            return disconnected;
        }

        @Override
        public boolean isInputShutdown() {
            return disconnected;
        }

        @Override
        public boolean isOutputShutdown() {
            return disconnected;
        }

        @Override
        public boolean isBound() {
            return !disconnected;
        }

        @Override
        public void close() {
            disconnected = true;
            try {
                httpConnection.disconnect();
            }catch (Throwable ignored) {
                // Ignored
            }
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            try {
                var httpServer = getServerSocket();
                var outputStream = httpServer.getClass().getMethod("getOutputStream");
                return (OutputStream) outputStream.invoke(httpServer);
            }catch (ReflectiveOperationException exception) {
                throw new IOException("Cannot access output stream", exception);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            try {
                var httpServer = getServerSocket();
                var inputStream = httpServer.getClass().getMethod("getInputStream");
                return (InputStream) inputStream.invoke(httpServer);
            }catch (ReflectiveOperationException exception) {
                throw new IOException("Cannot access input stream", exception);
            }
        }

        private Object getServerSocket() throws ReflectiveOperationException {
            if(serverSocket != null) {
                return serverSocket;
            }

            var httpClientField = httpConnection.getClass().getDeclaredField("http");
            unsafe.putBoolean(httpClientField, offset, true);
            var httpClient = httpClientField.get(httpConnection);
            var httpServerField = httpClient.getClass().getSuperclass().getDeclaredField("serverSocket");
            unsafe.putBoolean(httpServerField, offset, true);
            return serverSocket = httpServerField.get(httpClient);
        }

        @Override
        public boolean isConnected() {
            return !disconnected;
        }

        @Override
        public InetAddress getInetAddress() {
            return super.getInetAddress();
        }

        @Override
        public int getPort() {
            return super.getPort();
        }

        @Override
        public int getLocalPort() {
            return super.getLocalPort();
        }

        @Override
        public void shutdownInput() throws IOException {
            getInputStream().close();
        }

        @Override
        public void shutdownOutput() throws IOException {
            getOutputStream().close();
        }
    }

    // No Socks4 support, nobody uses it
    private static final class Socks5 extends SocketWithProxy {
        private static final int PROTO_VERS = 5;

        private static final int NO_AUTH = 0;
        private static final int USER_PASSW = 2;
        private static final int NO_METHODS = -1;

        private static final int CONNECT = 1;

        private static final int IPV4 = 1;
        private static final int DOMAIN_NAME = 3;
        private static final int IPV6 = 4;

        private static final int REQUEST_OK = 0;
        private static final int GENERAL_FAILURE = 1;
        private static final int NOT_ALLOWED = 2;
        private static final int NET_UNREACHABLE = 3;
        private static final int HOST_UNREACHABLE = 4;
        private static final int CONN_REFUSED = 5;
        private static final int TTL_EXPIRED = 6;
        private static final int CMD_NOT_SUPPORTED = 7;
        private static final int ADDR_TYPE_NOT_SUP = 8;

        private Socket socket;

        Socks5(URI proxy) {
            super(proxy);
        }

        @Override
        public void connect(SocketAddress endpoint) throws IOException {
            connect(endpoint, -1);
        }

        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            if(!(endpoint instanceof InetSocketAddress address)) {
                throw new IllegalArgumentException("Unsupported address type");
            }

            this.socket = new Socket();
            socket.setKeepAlive(true);
            socket.connect(new InetSocketAddress(proxy.getHost(), proxy.getPort()));
            getOutputStream().write(PROTO_VERS);
            getOutputStream().write(2);
            getOutputStream().write(NO_AUTH);
            getOutputStream().write(USER_PASSW);
            getOutputStream().flush();
            var data = new byte[2];
            readSocksReply(data);
            var socksVersion = data[0];
            if (socksVersion != PROTO_VERS) {
                throw new SocketException("SOCKS : Unsupported socks version " + socksVersion);
            }

            var methods = data[1];
            if (methods == NO_METHODS) {
                throw new SocketException("SOCKS : No acceptable methods");
            }

            if (!authenticate(data[1])) {
                throw new SocketException("SOCKS : authentication failed");
            }

            getOutputStream().write(PROTO_VERS);
            getOutputStream().write(CONNECT);
            getOutputStream().write(0);
            getOutputStream().write(DOMAIN_NAME);
            getOutputStream().write(address.getHostName().length());
            getOutputStream().write(address.getHostName().getBytes(StandardCharsets.ISO_8859_1));
            getOutputStream().write((address.getPort() >> 8) & 0xff);
            getOutputStream().write((address.getPort()) & 0xff);
            getOutputStream().flush();
            data = new byte[4];
            var success = readSocksReply(data);
            if (!success) {
                throw new SocketException("Reply from SOCKS server has bad length");
            }

            switch (data[1]) {
                case REQUEST_OK -> onConnection(data);
                case GENERAL_FAILURE ->throw new SocketException("SOCKS server general failure");
                case NOT_ALLOWED ->throw new SocketException("SOCKS: Connection not allowed by ruleset");
                case NET_UNREACHABLE ->throw new SocketException("SOCKS: Network unreachable");
                case HOST_UNREACHABLE ->throw new SocketException("SOCKS: Host unreachable");
                case CONN_REFUSED ->throw new SocketException("SOCKS: Connection refused");
                case TTL_EXPIRED ->throw new SocketException("SOCKS: TTL expired");
                case CMD_NOT_SUPPORTED ->throw new SocketException("SOCKS: Command not supported");
                case ADDR_TYPE_NOT_SUP ->throw new SocketException("SOCKS: address type not supported");
            }
        }

        private void onConnection(byte[] data) throws IOException {
            switch (data[3]) {
                case IPV4 -> {
                    if (!readSocksReply(new byte[4])) {
                        throw new SocketException("Reply from SOCKS server badly formatted");
                    }

                    if (!readSocksReply(new byte[2])) {
                        throw new SocketException("Reply from SOCKS server badly formatted");
                    }
                }
                case DOMAIN_NAME -> {
                    var lenByte = new byte[1];
                    var lenSuccess = readSocksReply(lenByte);
                    if (!lenSuccess) {
                        throw new SocketException("Reply from SOCKS server badly formatted");
                    }

                    var len = lenByte[0] & 0xFF;
                    if (!readSocksReply(new byte[len])) {
                        throw new SocketException("Reply from SOCKS server badly formatted");
                    }

                    if (!readSocksReply(new byte[2])) {
                        throw new SocketException("Reply from SOCKS server badly formatted");
                    }
                }
                case IPV6 -> {
                    if (!readSocksReply(new byte[16])) {
                        throw new SocketException("Reply from SOCKS server badly formatted");
                    }

                    if (!readSocksReply(new byte[2])) {
                        throw new SocketException("Reply from SOCKS server badly formatted");
                    }
                }
                default -> throw new SocketException("Reply from SOCKS server contains wrong code");
            }
        }

        private boolean authenticate(byte method) throws IOException {
            if (method == NO_AUTH) {
                return true;
            }

            if (method != USER_PASSW) {
                return false;
            }

            var userInfo = parseUserInfo(proxy.getUserInfo());
            if (userInfo == null) {
                return false;
            }

            getOutputStream().write(1);
            getOutputStream().write(userInfo.username().length());
            getOutputStream().write(userInfo.username().getBytes(StandardCharsets.ISO_8859_1));
            if (userInfo.password() != null) {
                getOutputStream().write(userInfo.password().length());
                getOutputStream().write(userInfo.password().getBytes(StandardCharsets.ISO_8859_1));
            } else {
                getOutputStream().write(0);
            }
            getOutputStream().flush();

            var data = new byte[2];
            var success = readSocksReply(data);
            return success && data[1] == 0;
        }

        private boolean readSocksReply(byte[] data) throws IOException {
            var read = 0;
            while (read < data.length) {
                var chunkRead = getInputStream().read(data, read, data.length - read);
                if(chunkRead == -1) {
                    break;
                }

                read += chunkRead;
            }

            return read == data.length;
        }

        @Override
        public boolean isClosed() {
            return socket != null && socket.isClosed();
        }

        @Override
        public boolean isInputShutdown() {
            return socket != null && socket.isInputShutdown();
        }

        @Override
        public boolean isOutputShutdown() {
            return socket != null && socket.isOutputShutdown();
        }

        @Override
        public boolean isBound() {
            return socket != null && socket.isBound();
        }

        @Override
        public void close() {
            try {
                if(socket == null) {
                    return;
                }

                socket.close();
            }catch (Throwable ignored) {
                // Ignored
            }
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return Objects.requireNonNull(socket).getOutputStream();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Objects.requireNonNull(socket).getInputStream();
        }

        @Override
        public boolean isConnected() {
            return socket != null && socket.isConnected();
        }
    }

    private static final class Direct extends SocketWithProxy {
        private Socket socket;
        Direct() {
            super(null);
        }

        @Override
        public void connect(SocketAddress endpoint) throws IOException {
            connect(endpoint, -1);
        }

        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            if(!(endpoint instanceof InetSocketAddress address)) {
                throw new IllegalArgumentException("Unsupported address type");
            }

            this.socket = new Socket();
            socket.setKeepAlive(true);
            socket.connect(new InetSocketAddress(address.getHostName(), address.getPort()));
        }

        @Override
        public boolean isClosed() {
            return socket != null && socket.isClosed();
        }

        @Override
        public boolean isInputShutdown() {
            return socket != null && socket.isInputShutdown();
        }

        @Override
        public boolean isOutputShutdown() {
            return socket != null && socket.isOutputShutdown();
        }

        @Override
        public boolean isBound() {
            return socket != null && socket.isBound();
        }

        @Override
        public void close() {
            try {
                if(socket == null) {
                    return;
                }

                socket.close();
            }catch (Throwable ignored) {
                // Ignored
            }
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return Objects.requireNonNull(socket).getOutputStream();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Objects.requireNonNull(socket).getInputStream();
        }

        @Override
        public boolean isConnected() {
            return socket != null && socket.isConnected();
        }
    }
}

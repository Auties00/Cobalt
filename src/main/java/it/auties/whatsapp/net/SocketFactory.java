package it.auties.whatsapp.net;

import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Proxies;
import it.auties.whatsapp.util.Reflection;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class SocketFactory {
    static {
        Proxies.allowBasicAuth();
    }

    public static Socket of(URI proxy) throws IOException {
        var javaProxy = Proxies.toProxy(proxy);
        if (javaProxy == Proxy.NO_PROXY) {
            return new Socket();
        }

        return switch (javaProxy.type()) {
            case DIRECT -> new Socket();
            case HTTP -> new Http(proxy);
            case SOCKS -> new Socks5(proxy);
        };
    }

    // This approach is not fantastic, but it's the same that the internal Java API uses
    // Though I can't use reflection directly as I'm not in the java.base module, so I'm using unsafe
    private static final class Http extends Socket {
        private final URI proxy;
        private HttpURLConnection httpConnection;
        private Socket serverSocket;
        private boolean disconnected;

        Http(URI proxy) {
            this.proxy = proxy;
        }

        @Override
        public void connect(SocketAddress endpoint) throws IOException {
            connect(endpoint, -1);
        }

        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            if (!(endpoint instanceof InetSocketAddress address)) {
                throw new IllegalArgumentException("Unsupported address type");
            }

            var uri = URI.create("http://%s:%s/".formatted(address.getHostName(), address.getPort()));
            this.httpConnection = (HttpURLConnection) uri.toURL().openConnection(Proxies.toProxy(proxy));
            httpConnection.setAuthenticator(Proxies.toAuthenticator(proxy));
            if (timeout > 0) {
                httpConnection.setConnectTimeout(timeout);
            }
            httpConnection.connect();
            doTunneling();
        }

        private void doTunneling() {
            try {
                var doTunnellingMethod = httpConnection.getClass().getMethod("doTunneling");
                Reflection.open(doTunnellingMethod);
                doTunnellingMethod.invoke(httpConnection);
            } catch (ReflectiveOperationException exception) {
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
            } catch (Throwable ignored) {
                // Ignored
            }
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            try {
                return getServerSocket().getOutputStream();
            } catch (ReflectiveOperationException exception) {
                throw new IOException("Cannot access output stream", exception);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            try {
                return getServerSocket().getInputStream();
            } catch (ReflectiveOperationException exception) {
                throw new IOException("Cannot access input stream", exception);
            }
        }

        private Socket getServerSocket() throws ReflectiveOperationException {
            if (serverSocket != null) {
                return serverSocket;
            }

            var httpClientField = httpConnection.getClass().getDeclaredField("http");
            Reflection.open(httpClientField);
            var httpClient = httpClientField.get(httpConnection);
            var httpServerField = httpClient.getClass().getSuperclass().getDeclaredField("serverSocket");
            Reflection.open(httpServerField);
            return serverSocket = (Socket) httpServerField.get(httpClient);
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
    private static final class Socks5 extends Socket {
        private static final int VERSION_5 = 5;

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

        private final URI proxy;
        private java.net.Socket socket;

        Socks5(URI proxy) {
            this.proxy = proxy;
        }

        @Override
        public void connect(SocketAddress endpoint) throws IOException {
            connect(endpoint, -1);
        }

        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            if (!(endpoint instanceof InetSocketAddress address)) {
                throw new IllegalArgumentException("Unsupported address type");
            }

            this.socket = new java.net.Socket();
            socket.setKeepAlive(true);
            var proxyAddress = new InetSocketAddress(proxy.getHost(), proxy.getPort());
            if(timeout >= 0) {
                socket.connect(proxyAddress, timeout);
            }else {
                socket.connect(proxyAddress);
            }
            var outputStream = new BufferedOutputStream(getOutputStream(), 512);
            outputStream.write(VERSION_5);
            outputStream.write(2);
            outputStream.write(NO_AUTH);
            outputStream.write(USER_PASSW);
            outputStream.flush();
            var data = new byte[2];
            readSocksReply(data);
            var socksVersion = data[0];
            if (socksVersion != VERSION_5) {
                var errorMessage = new String(Bytes.concat(data, getInputStream().readAllBytes()));
                throw new SocketException(errorMessage);
            }

            var methods = data[1];
            if (methods == NO_METHODS) {
                throw new SocketException("SOCKS : No acceptable methods");
            }

            if (!authenticate(outputStream, data[1])) {
                throw new SocketException("SOCKS : authentication failed");
            }

            outputStream.write(VERSION_5);
            outputStream.write(CONNECT);
            outputStream.write(0);
            outputStream.write(DOMAIN_NAME);
            outputStream.write(address.getHostName().length());
            outputStream.write(address.getHostName().getBytes(StandardCharsets.ISO_8859_1));
            outputStream.write((address.getPort() >> 8) & 0xff);
            outputStream.write((address.getPort()) & 0xff);
            outputStream.flush();
            data = new byte[4];
            var success = readSocksReply(data);
            if (!success) {
                throw new SocketException("Reply from SOCKS server has bad length");
            }

            switch (data[1]) {
                case REQUEST_OK -> onConnection(data);
                case GENERAL_FAILURE -> throw new SocketException("SOCKS server general failure");
                case NOT_ALLOWED -> throw new SocketException("SOCKS: Connection not allowed by ruleset");
                case NET_UNREACHABLE -> throw new SocketException("SOCKS: Network unreachable");
                case HOST_UNREACHABLE -> throw new SocketException("SOCKS: Host unreachable");
                case CONN_REFUSED -> throw new SocketException("SOCKS: Connection refused");
                case TTL_EXPIRED -> throw new SocketException("SOCKS: TTL expired");
                case CMD_NOT_SUPPORTED -> throw new SocketException("SOCKS: Command not supported");
                case ADDR_TYPE_NOT_SUP -> throw new SocketException("SOCKS: address type not supported");
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

        private boolean authenticate(OutputStream outputStream, byte method) throws IOException {
            if (method == NO_AUTH) {
                return true;
            }

            if (method != USER_PASSW) {
                return false;
            }

            var userInfo = Proxies.parseUserInfo(proxy.getUserInfo());
            if (userInfo == null) {
                return false;
            }

            outputStream.write(1);
            outputStream.write(userInfo.username().length());
            outputStream.write(userInfo.username().getBytes(StandardCharsets.ISO_8859_1));
            if (userInfo.password() != null) {
                outputStream.write(userInfo.password().length());
                outputStream.write(userInfo.password().getBytes(StandardCharsets.ISO_8859_1));
            } else {
                outputStream.write(0);
            }
            outputStream.flush();

            var data = new byte[2];
            var success = readSocksReply(data);
            return success && data[1] == 0;
        }

        private boolean readSocksReply(byte[] data) throws IOException {
            var read = 0;
            while (read < data.length) {
                var chunkRead = getInputStream().read(data, read, data.length - read);
                if (chunkRead == -1) {
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
                if (socket == null) {
                    return;
                }

                System.out.println("Disconnecting");
                socket.close();
                System.out.println("Disconnected");
            } catch (Throwable ignored) {
                System.out.println("error");
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

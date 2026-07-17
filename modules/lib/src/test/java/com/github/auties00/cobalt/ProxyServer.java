package com.github.auties00.cobalt;

import com.github.auties00.cobalt.client.WhatsAppClientProxy;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test-harness proxy server that lets integration tests route a {@link WhatsAppClientProxy} through a
 * minimal local HTTP {@code CONNECT} (plaintext or TLS), SOCKS4/4a, or SOCKS5/5h endpoint. Each
 * proxy binds a random ephemeral port, exposes itself as a {@link WhatsAppClientProxy} via
 * {@link #toProxy()}, and releases the port on {@link #close()}.
 */
public sealed abstract class ProxyServer implements Closeable {
    protected final ExecutorService executor;

    protected final ServerSocket serverSocket;

    protected ProxyServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        executor.submit(this::acceptLoop);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    /**
     * Returns the {@link WhatsAppClientProxy} configuration pointing at this local server.
     *
     * @return the proxy configuration
     */
    public abstract WhatsAppClientProxy toProxy();

    @Override
    public void close() throws IOException {
        executor.shutdownNow();
        serverSocket.close();
    }

    private void acceptLoop() {
        try {
            while (!serverSocket.isClosed()) {
                var client = serverSocket.accept();
                executor.submit(() -> {
                    try {
                        handleClient(client);
                    } catch (IOException _) {
                    } finally {
                        try {
                            client.close();
                        } catch (IOException _) {
                        }
                    }
                });
            }
        } catch (IOException _) {
            // Server socket closed
        }
    }

    /**
     * Handles a single client connection.
     *
     * @param client the client socket
     * @throws IOException if an I/O error occurs
     */
    protected abstract void handleClient(Socket client) throws IOException;

    /**
     * Relays bytes in both directions between two sockets until either side closes.
     *
     * @param a the first socket
     * @param b the second socket
     */
    protected static void relay(Socket a, Socket b) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> pipe(a, b));
            pipe(b, a);
        }
    }

    private static void pipe(Socket source, Socket destination) {
        try {
            var buf = new byte[8192];
            var in = source.getInputStream();
            var out = destination.getOutputStream();
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (IOException _) {
        } finally {
            try {
                destination.shutdownOutput();
            } catch (IOException _) {
            }
        }
    }

    /**
     * Creates a plain HTTP CONNECT proxy.
     *
     * @return a new proxy server
     * @throws IOException if the server socket cannot be bound
     */
    public static ProxyServer http() throws IOException {
        return new HttpProxy(new ServerSocket(0));
    }

    /**
     * Creates a SOCKS4 proxy server.
     *
     * @return a new proxy server
     * @throws IOException if the server socket cannot be bound
     */
    public static ProxyServer socks4() throws IOException {
        return new Socks4Proxy(new ServerSocket(0));
    }

    /**
     * Creates a SOCKS5 proxy server.
     *
     * @return a new proxy server
     * @throws IOException if the server socket cannot be bound
     */
    public static ProxyServer socks5() throws IOException {
        return new Socks5Proxy(new ServerSocket(0));
    }

    /**
     * Creates an HTTPS CONNECT proxy with a self-signed certificate.
     *
     * @return a new proxy server
     * @throws Exception if the server socket or TLS context cannot be created
     */
    public static ProxyServer https() throws Exception {
        var kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var keyPair = kpg.generateKeyPair();

        var now = Instant.now();
        var subject = new X500Name("CN=localhost");
        var certBuilder = new JcaX509v3CertificateBuilder(
                subject, BigInteger.ONE,
                Date.from(now), Date.from(now.plus(Duration.ofDays(1))),
                subject, keyPair.getPublic()
        );
        var signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        var cert = new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));

        var ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("test", keyPair.getPrivate(), new char[0], new Certificate[]{cert});

        var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, new char[0]);

        var sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        var serverSocket = sslContext.getServerSocketFactory().createServerSocket(0);
        return new HttpsProxy(serverSocket);
    }

    private static final class HttpProxy extends ProxyServer {
        private HttpProxy(ServerSocket serverSocket) {
            super(serverSocket);
        }

        @Override
        public WhatsAppClientProxy toProxy() {
            return WhatsAppClientProxy.ofHttp("127.0.0.1", port());
        }

        @Override
        protected void handleClient(Socket client) throws IOException {
            var in = client.getInputStream();
            var line = readLine(in);
            if (line == null || !line.toUpperCase().startsWith("CONNECT ")) {
                sendResponse(client, 400, "Bad Request");
                return;
            }

            var target = parseConnectTarget(line);
            if (target == null) {
                sendResponse(client, 400, "Bad Request");
                return;
            }

            // Consume remaining headers
            while (true) {
                var header = readLine(in);
                if (header == null || header.isEmpty()) {
                    break;
                }
            }

            try {
                var remote = new Socket(target.getHostString(), target.getPort());
                sendResponse(client, 200, "Connection Established");
                relay(client, remote);
                remote.close();
            } catch (IOException e) {
                sendResponse(client, 502, "Bad Gateway");
            }
        }

        private static void sendResponse(Socket client, int code, String reason) throws IOException {
            var response = "HTTP/1.1 " + code + " " + reason + "\r\n\r\n";
            client.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));
            client.getOutputStream().flush();
        }
    }

    private static final class HttpsProxy extends ProxyServer {
        private HttpsProxy(ServerSocket serverSocket) {
            super(serverSocket);
        }

        @Override
        public WhatsAppClientProxy toProxy() {
            return WhatsAppClientProxy.ofHttps("127.0.0.1", port());
        }

        @Override
        protected void handleClient(Socket client) throws IOException {
            // Client already speaks TLS (SSLServerSocket handles it)
            var in = client.getInputStream();
            var line = readLine(in);
            if (line == null || !line.toUpperCase().startsWith("CONNECT ")) {
                var response = "HTTP/1.1 400 Bad Request\r\n\r\n";
                client.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));
                client.getOutputStream().flush();
                return;
            }

            var target = parseConnectTarget(line);
            if (target == null) {
                var response = "HTTP/1.1 400 Bad Request\r\n\r\n";
                client.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));
                client.getOutputStream().flush();
                return;
            }

            // Consume remaining headers
            while (true) {
                var header = readLine(in);
                if (header == null || header.isEmpty()) {
                    break;
                }
            }

            try {
                var remote = new Socket(target.getHostString(), target.getPort());
                var response = "HTTP/1.1 200 Connection Established\r\n\r\n";
                client.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));
                client.getOutputStream().flush();
                relay(client, remote);
                remote.close();
            } catch (IOException e) {
                var response = "HTTP/1.1 502 Bad Gateway\r\n\r\n";
                client.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));
                client.getOutputStream().flush();
            }
        }
    }

    private static String readLine(InputStream in) throws IOException {
        var sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                return sb.toString().stripTrailing();
            }
            sb.append((char) b);
        }
        return sb.isEmpty() ? null : sb.toString().stripTrailing();
    }

    private static InetSocketAddress parseConnectTarget(String line) {
        var parts = line.split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        var authority = parts[1];
        var colon = authority.lastIndexOf(':');
        if (colon <= 0) {
            return null;
        }
        try {
            var host = authority.substring(0, colon);
            var port = Integer.parseInt(authority.substring(colon + 1));
            return new InetSocketAddress(host, port);
        } catch (NumberFormatException _) {
            return null;
        }
    }

    /**
     * Minimal SOCKS4 / SOCKS4a server: handles the no-auth CONNECT
     * request (RFC 1928 predecessor) and relays once the upstream
     * connection is established.
     */
    private static final class Socks4Proxy extends ProxyServer {
        private Socks4Proxy(ServerSocket serverSocket) {
            super(serverSocket);
        }

        @Override
        public WhatsAppClientProxy toProxy() {
            return WhatsAppClientProxy.ofSocks4a("127.0.0.1", port());
        }

        @Override
        protected void handleClient(Socket client) throws IOException {
            var in = new DataInputStream(client.getInputStream());
            var out = client.getOutputStream();

            var version = in.readUnsignedByte();
            if (version != 0x04) {
                writeSocks4Reply(out, (byte) 0x5B);
                return;
            }
            var command = in.readUnsignedByte();
            if (command != 0x01) {
                writeSocks4Reply(out, (byte) 0x5B);
                return;
            }
            var port = in.readUnsignedShort();
            var ipv4 = new byte[4];
            in.readFully(ipv4);
            // Read the user-id, null-terminated.
            //noinspection StatementWithEmptyBody
            while (in.readUnsignedByte() != 0) {
                // skip
            }
            String host;
            if (ipv4[0] == 0 && ipv4[1] == 0 && ipv4[2] == 0 && ipv4[3] != 0) {
                // SOCKS4a: hostname follows the user-id null.
                var hostBytes = new ByteArrayOutputStream();
                int b;
                while ((b = in.readUnsignedByte()) != 0) {
                    hostBytes.write(b);
                }
                host = hostBytes.toString(StandardCharsets.US_ASCII);
            } else {
                host = InetAddress.getByAddress(ipv4).getHostAddress();
            }
            try {
                var remote = new Socket(host, port);
                writeSocks4Reply(out, (byte) 0x5A);
                relay(client, remote);
                remote.close();
            } catch (IOException _) {
                writeSocks4Reply(out, (byte) 0x5B);
            }
        }

        private static void writeSocks4Reply(OutputStream out, byte status) throws IOException {
            out.write(new byte[]{0x00, status, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
            out.flush();
        }
    }

    /**
     * Minimal SOCKS5 / SOCKS5h server: handles the no-auth CONNECT
     * request (RFC 1928) and relays once the upstream connection is
     * established.
     */
    private static final class Socks5Proxy extends ProxyServer {
        private Socks5Proxy(ServerSocket serverSocket) {
            super(serverSocket);
        }

        @Override
        public WhatsAppClientProxy toProxy() {
            return WhatsAppClientProxy.ofSocks5h("127.0.0.1", port());
        }

        @Override
        protected void handleClient(Socket client) throws IOException {
            var in = new DataInputStream(client.getInputStream());
            var out = client.getOutputStream();

            var version = in.readUnsignedByte();
            if (version != 0x05) {
                return;
            }
            var nMethods = in.readUnsignedByte();
            var methods = new byte[nMethods];
            in.readFully(methods);
            // Reply with no-auth selected.
            out.write(new byte[]{0x05, 0x00});
            out.flush();

            // CONNECT request.
            version = in.readUnsignedByte();
            if (version != 0x05) {
                writeSocks5Reply(out, (byte) 0x01);
                return;
            }
            var command = in.readUnsignedByte();
            in.readUnsignedByte(); // reserved
            var addressType = in.readUnsignedByte();
            String host;
            switch (addressType) {
                case 0x01 -> {
                    var ipv4 = new byte[4];
                    in.readFully(ipv4);
                    host = InetAddress.getByAddress(ipv4).getHostAddress();
                }
                case 0x03 -> {
                    var len = in.readUnsignedByte();
                    var hostBytes = new byte[len];
                    in.readFully(hostBytes);
                    host = new String(hostBytes, StandardCharsets.US_ASCII);
                }
                case 0x04 -> {
                    var ipv6 = new byte[16];
                    in.readFully(ipv6);
                    host = InetAddress.getByAddress(ipv6).getHostAddress();
                }
                default -> {
                    writeSocks5Reply(out, (byte) 0x08);
                    return;
                }
            }
            var port = in.readUnsignedShort();

            if (command != 0x01) {
                writeSocks5Reply(out, (byte) 0x07);
                return;
            }

            try {
                var remote = new Socket(host, port);
                writeSocks5Reply(out, (byte) 0x00);
                relay(client, remote);
                remote.close();
            } catch (IOException _) {
                writeSocks5Reply(out, (byte) 0x05);
            }
        }

        private static void writeSocks5Reply(OutputStream out, byte status) throws IOException {
            // VER, REP, RSV, ATYP=IPv4, BND.ADDR=0.0.0.0, BND.PORT=0
            out.write(new byte[]{0x05, status, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
            out.flush();
        }
    }
}

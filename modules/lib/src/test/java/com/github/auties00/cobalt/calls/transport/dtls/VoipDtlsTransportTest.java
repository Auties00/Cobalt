package com.github.auties00.cobalt.calls.transport.dtls;

import com.github.auties00.cobalt.util.certificate.X509CertificateGenerator;
import com.github.auties00.cobalt.util.certificate.X509CertificateSpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the JDK DTLS relay handshake end to end by driving the real {@link VoipDtlsCertificates#createEngine}
 * (both roles) through the real {@link VoipDtlsTransport} over an in-memory datagram seam. Each case runs the two
 * peers on separate threads, since the transport's handshake blocks on the datagram receive; the peer opposite the
 * engine under test presents a certificate whose SHA-256 the engine was told to pin and trusts any client, so the
 * pin and mutual-auth paths are both driven without a live relay. A lossy seam that drops a datagram exercises the
 * DTLS retransmission, and a mismatched pin exercises the rejection path.
 */
@DisplayName("VoipDtlsTransport relay DTLS handshake")
class VoipDtlsTransportTest {
    private record Peer(KeyPair keyPair, X509Certificate certificate, byte[] fingerprint) {}

    private static Peer newPeer() throws Exception {
        var generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        var keyPair = generator.generateKeyPair();
        var now = Instant.now();
        var spec = X509CertificateSpec.selfSigned()
                .keyPair(keyPair)
                .subject(new X500Principal("CN=WebRTC"))
                .serialNumber(new BigInteger(64, new SecureRandom()).abs().add(BigInteger.ONE))
                .validity(now.minus(1, ChronoUnit.DAYS), now.plus(30, ChronoUnit.DAYS))
                .build();
        var certificate = X509CertificateGenerator.getInstance("SHA256withECDSA").generate(spec);
        var fingerprint = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
        return new Peer(keyPair, certificate, fingerprint);
    }

    // a controlled peer that presents its own certificate and trusts any peer certificate
    private static SSLEngine trustAllEngine(boolean clientRole, Peer own) throws Exception {
        var keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("k", own.keyPair.getPrivate(), new char[0], new X509Certificate[]{own.certificate});
        var keyManagerFactory = KeyManagerFactory.getInstance("NewSunX509");
        keyManagerFactory.init(keyStore, new char[0]);
        var trustAll = new X509ExtendedTrustManager() {
            public void checkClientTrusted(X509Certificate[] c, String a) {}
            public void checkServerTrusted(X509Certificate[] c, String a) {}
            public void checkClientTrusted(X509Certificate[] c, String a, Socket s) {}
            public void checkServerTrusted(X509Certificate[] c, String a, Socket s) {}
            public void checkClientTrusted(X509Certificate[] c, String a, SSLEngine e) {}
            public void checkServerTrusted(X509Certificate[] c, String a, SSLEngine e) {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };
        var context = SSLContext.getInstance("DTLSv1.2");
        context.init(keyManagerFactory.getKeyManagers(), new TrustManager[]{trustAll}, new SecureRandom());
        var engine = context.createSSLEngine();
        engine.setUseClientMode(clientRole);
        if (!clientRole) {
            engine.setNeedClientAuth(true);
        }
        var parameters = engine.getSSLParameters();
        parameters.setCipherSuites(new String[]{
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"});
        parameters.setMaximumPacketSize(1500);
        engine.setSSLParameters(parameters);
        return engine;
    }

    // paired in-memory datagram seam; dropWhich > 0 drops that outbound datagram to force a retransmission
    private static VoipDtlsTransport.Datagrams seam(BlockingQueue<byte[]> in, BlockingQueue<byte[]> out, int dropWhich) {
        var counter = new AtomicInteger();
        return new VoipDtlsTransport.Datagrams() {
            @Override
            public void send(byte[] record) {
                if (counter.incrementAndGet() == dropWhich) {
                    return;
                }
                out.add(record);
            }

            @Override
            public byte[] receive(int waitMillis) {
                try {
                    return in.poll(waitMillis, TimeUnit.MILLISECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        };
    }

    private record Established(VoipDtlsTransport a, VoipDtlsTransport b) {}

    // runs both handshakes concurrently; propagates the handshake exception if either side fails
    private static Established handshake(SSLEngine a, SSLEngine b, int dropOnA) throws Exception {
        BlockingQueue<byte[]> aToB = new LinkedBlockingQueue<>();
        BlockingQueue<byte[]> bToA = new LinkedBlockingQueue<>();
        var ta = new VoipDtlsTransport(a, seam(bToA, aToB, dropOnA));
        var tb = new VoipDtlsTransport(b, seam(aToB, bToA, 0));
        try (var pool = Executors.newFixedThreadPool(2)) {
            var fa = pool.submit(() -> {
                ta.handshake(5000);
                return null;
            });
            var fb = pool.submit(() -> {
                tb.handshake(5000);
                return null;
            });
            fa.get(15, TimeUnit.SECONDS);
            fb.get(15, TimeUnit.SECONDS);
        }
        return new Established(ta, tb);
    }

    // mirror the DTLS receive loop: retry while a record yields no application data
    private static byte[] receive(VoipDtlsTransport transport) throws Exception {
        var buffer = new byte[4096];
        var deadline = System.nanoTime() + 2_000_000_000L;
        while (System.nanoTime() < deadline) {
            var read = transport.receive(buffer, 0, buffer.length, 200);
            if (read > 0) {
                return Arrays.copyOf(buffer, read);
            }
        }
        throw new AssertionError("no application data received");
    }

    private static void assertAppData(Established established) throws Exception {
        var request = "hello-relay-data-channel".getBytes();
        established.a.send(request, 0, request.length);
        assertArrayEquals(request, receive(established.b), "a->b application data");
        var reply = "reply".getBytes();
        established.b.send(reply, 0, reply.length);
        assertArrayEquals(reply, receive(established.a), "b->a application data");
        established.a.close();
        established.b.close();
    }

    @Test
    @DisplayName("the client engine handshakes against a pinned server and carries application data")
    void clientRoleHandshake() throws Exception {
        var server = newPeer();
        var client = VoipDtlsCertificates.createEngine(true, server.fingerprint);
        var established = handshake(client, trustAllEngine(false, server), 0);
        assertTrue(client.getSession().getCipherSuite().startsWith("TLS_ECDHE_ECDSA"));
        assertTrue(client.getSession().getPeerCertificates().length > 0, "client authenticated the server");
        assertAppData(established);
    }

    @Test
    @DisplayName("the server engine handshakes against a pinned client and carries application data")
    void serverRoleHandshake() throws Exception {
        var client = newPeer();
        var server = VoipDtlsCertificates.createEngine(false, client.fingerprint);
        var established = handshake(trustAllEngine(true, client), server, 0);
        assertTrue(server.getSession().getPeerCertificates().length > 0, "server pinned the client certificate");
        assertAppData(established);
    }

    @Test
    @DisplayName("a dropped ClientHello is recovered by DTLS retransmission")
    void retransmissionRecoversLoss() throws Exception {
        var server = newPeer();
        var client = VoipDtlsCertificates.createEngine(true, server.fingerprint);
        var established = handshake(client, trustAllEngine(false, server), 1);
        assertAppData(established);
    }

    @Test
    @DisplayName("a certificate that does not match the pin fails the handshake")
    void wrongPinIsRejected() throws Exception {
        var server = newPeer();
        var wrong = newPeer();
        var client = VoipDtlsCertificates.createEngine(true, wrong.fingerprint);
        assertThrows(ExecutionException.class, () -> handshake(client, trustAllEngine(false, server), 0));
    }
}

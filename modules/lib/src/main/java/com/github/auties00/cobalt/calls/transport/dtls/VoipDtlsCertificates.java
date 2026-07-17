package com.github.auties00.cobalt.calls.transport.dtls;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.util.certificate.X509CertificateGenerator;
import com.github.auties00.cobalt.util.certificate.X509CertificateSpec;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.math.BigInteger;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Builds the fingerprint pinning DTLS {@link SSLEngine} for the WhatsApp Web relay leg's DTLS handshake and
 * concentrates the certificate and pinning logic both DTLS roles share.
 *
 * <p>Both relay DTLS roles present the same kind of credential and pin the relay's certificate to the same
 * fixed fingerprint: in the common passive mode the web client is the DTLS client and pins the relay's server
 * certificate, and in the defensive active mode (the relay block's {@code enable_edgeray_dtls_active_mode}) the
 * web client is the DTLS server and pins the relay's client certificate. Both roles authenticate with a freshly
 * generated self signed ECDSA P-256 certificate, the credential WebRTC carries, and both verify the relay's
 * leaf certificate against the same SHA-256 fingerprint. This holder concentrates that common logic so the two
 * roles cannot drift apart: the key pair and certificate generation, the key manager that presents the
 * credential, and the constant time pin check, all wired into a configured DTLS 1.2 engine.
 *
 * @implNote This implementation realises the certificate side of WhatsApp Web's synthesized relay answer, which
 *           carries one hardcoded {@code a=fingerprint:sha-256} and a self signed ECDSA leaf for the browser
 *           end of the relay leg. The whole DTLS record and handshake layer is the JDK's own {@code DTLSv1.2}
 *           {@link SSLEngine}; the credential is presented through a {@link KeyManagerFactory} over an in memory
 *           {@link KeyStore}, and the relay pin is enforced by a bespoke {@link X509ExtendedTrustManager} that
 *           accepts any chain structurally and instead compares the SHA-256 of the leaf certificate's DER
 *           encoding, the same digest the {@code a=fingerprint} carries, through
 *           {@link MessageDigest#isEqual(byte[], byte[])} so the comparison is constant time. The key pair is
 *           generated through the JCA ({@code KeyPairGenerator("EC")} on {@code secp256r1}) and the certificate
 *           through {@link X509CertificateGenerator}, a pure JDK generator, so no third party security provider
 *           is involved.
 */
public final class VoipDtlsCertificates {
    /**
     * The logger for {@link VoipDtlsCertificates}.
     */
    private static final System.Logger LOGGER = Log.get(VoipDtlsCertificates.class);

    /**
     * The length, in bytes, of a SHA-256 certificate fingerprint.
     */
    public static final int SHA256_FINGERPRINT_LENGTH = 32;

    /**
     * The JCA standard name of the SHA-256 digest used to fingerprint the relay certificate.
     */
    private static final String SHA256_ALGORITHM = "SHA-256";

    /**
     * The named elliptic curve the self signed certificate key pair is generated on.
     */
    private static final String P256_CURVE = "secp256r1";

    /**
     * The JCA key pair algorithm of the self signed certificate key.
     */
    private static final String EC_ALGORITHM = "EC";

    /**
     * The JCA signature algorithm the self signed certificate is signed with.
     */
    private static final String CERT_SIGNATURE_ALGORITHM = "SHA256withECDSA";

    /**
     * The X.500 subject and issuer name of the self signed certificate.
     *
     * @implNote This implementation uses a fixed common name; WebRTC certificates are self signed and their
     * subject is never validated, only their fingerprint, so the name is cosmetic.
     */
    private static final String CERT_SUBJECT = "CN=WebRTC";

    /**
     * The validity window of the self signed certificate, in days from issuance.
     */
    private static final int CERT_VALIDITY_DAYS = 30;

    /**
     * The number of days before issuance the certificate's not before instant is backdated, to tolerate clock
     * skew.
     */
    private static final int CERT_BACKDATE_DAYS = 1;

    /**
     * The DTLS protocol version the relay leg negotiates.
     */
    private static final String DTLS_PROTOCOL = "DTLSv1.2";

    /**
     * The ECDHE ECDSA AES GCM cipher suites, matching the relay's self signed ECDSA certificate, offered in the
     * order WebRTC negotiates them.
     */
    private static final String[] CIPHER_SUITES = {
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"
    };

    /**
     * The path MTU, in bytes, the DTLS engine fragments its handshake and application records to.
     *
     * @implNote This implementation uses {@code 1500}, the value WhatsApp Web rewrites the synthesized answer's
     * {@code a=max-message-size} to, so a DTLS record never exceeds one host datagram.
     */
    private static final int PATH_MTU = 1500;

    /**
     * The alias the self signed credential is stored under in the in memory key store.
     */
    private static final String KEY_ALIAS = "dtls";

    /**
     * The empty password guarding the in memory key store, which never leaves the process.
     */
    private static final char[] EMPTY_PASSWORD = new char[0];

    /**
     * The shared secure random source the DTLS engine draws nonces and key material from.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Prevents instantiation of this static holder.
     */
    private VoipDtlsCertificates() {
        throw new AssertionError("VoipDtlsCertificates is a utility holder and cannot be instantiated");
    }

    /**
     * Builds a configured DTLS 1.2 {@link SSLEngine} for the given role that presents a freshly generated
     * self signed ECDSA P-256 certificate and pins the peer's leaf certificate to {@code pinnedFingerprint}.
     *
     * <p>The engine offers DTLS 1.2 only and the ECDHE ECDSA AES GCM cipher suites, fragments to the relay path
     * MTU, and installs a {@link #pinningTrustManager(byte[]) pinning trust manager}. In the server role it also
     * requests and requires the peer's certificate so the relay's client certificate can be pinned; a relay that
     * presents no certificate, or one whose fingerprint does not pin, fails the handshake.
     *
     * @param clientRole        whether the engine takes the DTLS client role rather than the server role
     * @param pinnedFingerprint the SHA-256 fingerprint the peer certificate is pinned to, the 32 raw digest
     *                          bytes
     * @return the configured DTLS engine
     * @throws GeneralSecurityException if the platform cannot provide DTLS 1.2, EC key generation, or the
     *                                  certificate cannot be generated or adopted
     * @throws IOException              if the certificate cannot be encoded
     */
    public static SSLEngine createEngine(boolean clientRole, byte[] pinnedFingerprint)
            throws GeneralSecurityException, IOException {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "building relay dtls engine, clientRole={0}", clientRole);
        }
        var keyPair = generateKeyPair();
        var certificate = selfSignCertificate(keyPair);
        // TODO: bind this DTLS certificate fingerprint over the Web P2P relay data channel; at Accept build and verify, key an HMAC with a salted HKDF SHA256 derive (SfuKeyDeriver.Domain.CERT_FINGERPRINT_HMAC label, 16 byte salt taken from the raw e2e key, remaining e2e bytes as IKM, not a zero salt derive()), generating the outbound HMAC here and verifying the peer's on handle_accept
        var sslContext = buildContext(keyPair, certificate, pinnedFingerprint);
        var engine = sslContext.createSSLEngine();
        engine.setUseClientMode(clientRole);
        if (!clientRole) {
            engine.setNeedClientAuth(true);
        }
        var parameters = engine.getSSLParameters();
        parameters.setCipherSuites(CIPHER_SUITES);
        parameters.setMaximumPacketSize(PATH_MTU);
        engine.setSSLParameters(parameters);
        return engine;
    }

    /**
     * Builds a DTLS {@link SSLContext} whose key manager presents the self signed credential and whose trust
     * manager pins the peer certificate.
     *
     * @param keyPair           the self signed certificate key pair, used as the credential's private key
     * @param certificate       the self signed leaf certificate
     * @param pinnedFingerprint the SHA-256 fingerprint the peer certificate is pinned to
     * @return the configured DTLS context
     * @throws GeneralSecurityException if the platform cannot provide DTLS 1.2 or assemble the key store
     * @throws IOException              if the key store cannot be initialised
     */
    private static SSLContext buildContext(KeyPair keyPair, X509Certificate certificate, byte[] pinnedFingerprint)
            throws GeneralSecurityException, IOException {
        var keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry(KEY_ALIAS, keyPair.getPrivate(), EMPTY_PASSWORD, new X509Certificate[]{certificate});
        var keyManagerFactory = KeyManagerFactory.getInstance("NewSunX509");
        keyManagerFactory.init(keyStore, EMPTY_PASSWORD);
        var sslContext = SSLContext.getInstance(DTLS_PROTOCOL);
        sslContext.init(keyManagerFactory.getKeyManagers(),
                new TrustManager[]{pinningTrustManager(pinnedFingerprint)}, SECURE_RANDOM);
        return sslContext;
    }

    /**
     * Returns a trust manager that ignores chain validation and instead pins the peer's leaf certificate to the
     * fixed SHA-256 fingerprint.
     *
     * <p>The relay certificate is self signed and validated by fingerprint rather than by chain, so the trust
     * manager names no accepted issuers and every {@code check*Trusted} entry point delegates to the same pin
     * check: an empty chain, or a leaf whose SHA-256 does not equal the pin, fails with a
     * {@link CertificateException} that aborts the handshake with a fatal alert.
     *
     * @param pinnedFingerprint the SHA-256 fingerprint the peer certificate is pinned to, the 32 raw digest
     *                          bytes
     * @return the pinning trust manager
     */
    private static X509ExtendedTrustManager pinningTrustManager(byte[] pinnedFingerprint) {
        var pin = pinnedFingerprint.clone();
        return new X509ExtendedTrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                verify(chain);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                verify(chain);
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
                verify(chain);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
                verify(chain);
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
                verify(chain);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
                verify(chain);
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            private void verify(X509Certificate[] chain) throws CertificateException {
                if (chain == null || chain.length == 0) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "relay dtls handshake failed: peer presented no certificate");
                    }
                    throw new CertificateException("relay presented no certificate");
                }
                byte[] actual;
                try {
                    actual = MessageDigest.getInstance(SHA256_ALGORITHM).digest(chain[0].getEncoded());
                } catch (NoSuchAlgorithmException exception) {
                    if (Log.ERROR) {
                        LOGGER.log(Level.ERROR, "sha-256 digest unavailable for relay dtls pin check", exception);
                    }
                    throw new CertificateException("SHA-256 unavailable", exception);
                }
                if (!MessageDigest.isEqual(actual, pin)) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "relay dtls handshake failed: certificate does not match pinned fingerprint");
                    }
                    throw new CertificateException("relay certificate does not pin");
                }
            }
        };
    }

    /**
     * Generates an ECDSA key pair on the P-256 curve.
     *
     * @return the generated key pair
     * @throws NoSuchAlgorithmException if the platform lacks EC key generation
     */
    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        try {
            var generator = KeyPairGenerator.getInstance(EC_ALGORITHM);
            generator.initialize(new ECGenParameterSpec(P256_CURVE), SECURE_RANDOM);
            return generator.generateKeyPair();
        } catch (InvalidAlgorithmParameterException exception) {
            throw new NoSuchAlgorithmException("EC P-256 key generation unavailable", exception);
        }
    }

    /**
     * Builds a self signed X.509 certificate over the key pair.
     *
     * @param keyPair the certificate key pair, used as both subject key and signing key
     * @return the self signed certificate
     * @throws GeneralSecurityException if the certificate cannot be built or signed
     */
    private static X509Certificate selfSignCertificate(KeyPair keyPair) throws GeneralSecurityException {
        var serial = new BigInteger(64, SECURE_RANDOM).abs().add(BigInteger.ONE);
        var now = Instant.now();
        var spec = X509CertificateSpec.selfSigned()
                .keyPair(keyPair)
                .subject(new X500Principal(CERT_SUBJECT))
                .serialNumber(serial)
                .validity(now.minus(CERT_BACKDATE_DAYS, ChronoUnit.DAYS), now.plus(CERT_VALIDITY_DAYS, ChronoUnit.DAYS))
                .build();
        return X509CertificateGenerator.getInstance(CERT_SIGNATURE_ALGORITHM).generate(spec);
    }
}

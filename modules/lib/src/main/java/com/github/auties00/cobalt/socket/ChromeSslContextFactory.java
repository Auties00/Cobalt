package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.telemetry.log.Log;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.lang.System.Logger.Level;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Default {@link WhatsAppSslContextFactory} that produces an
 * {@link SSLContext} and matching {@link SSLParameters} which mimic
 * Chrome's TLS client hello, so JA3-fingerprinting endpoints accept
 * the connection.
 *
 * <p>This factory is returned by {@link WhatsAppSslContextFactory#chrome()}
 * and is used for every outbound WhatsApp TLS hop unless the caller supplies
 * a custom factory. The parameters advertise ALPN {@code http/1.1}, enable
 * HTTPS hostname verification, and pin the cipher suite ordering to the live
 * Chrome 147 list, minus GREASE (which the JDK cannot emit) and minus the
 * legacy {@code TLS_RSA_*} suites (which the JDK pins on
 * {@code jdk.tls.disabledAlgorithms} too early to override reliably).
 *
 * @implNote
 * This implementation initialises the {@link SSLContext} once at
 * construction with the JDK's default trust and key managers, then
 * publishes the singleton via {@link #INSTANCE}. JA3 fingerprinting is
 * sensitive to the order ciphers appear in the client hello, so the
 * cipher list is laid down byte-for-byte and must never be sorted.
 */
final class ChromeSslContextFactory implements WhatsAppSslContextFactory {
    /** The logger for {@link ChromeSslContextFactory}. */
    private static final System.Logger LOGGER = Log.get(ChromeSslContextFactory.class);

    /**
     * The Chrome 147 cipher suite ordering, captured from
     * {@code https://www.howsmyssl.com/a/check}, with the four
     * deprecated {@code TLS_RSA_*} entries dropped.
     *
     * <p>Chrome's hello also offers the legacy {@code TLS_RSA_WITH_AES_*}
     * suites for backward compatibility with old servers; WhatsApp's edge
     * negotiates ECDHE in practice so the legacy entries are never actually
     * used, and dropping them keeps the wire and the configuration in
     * agreement.
     *
     * @implNote
     * This implementation must not sort or otherwise reorder the
     * array; JA3 hashes the cipher list as encountered and a sorted
     * variant would surface as a different fingerprint, triggering
     * server-side rejection. Modern OpenJDK pins {@code TLS_RSA_*} on
     * {@code jdk.tls.disabledAlgorithms} and caches that constraint at
     * JSSE class-init time, before any user code can reset the
     * property reliably (especially in IDE runs with a debugger agent
     * loaded earlier), which is why those suites are absent here.
     */
    private static final String[] CHROME_CIPHER_SUITES = {
            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA"
    };

    /**
     * The ALPN protocol identifier advertised inside the client hello.
     *
     * <p>WhatsApp's edge only accepts {@code http/1.1}; HTTP/2 was not
     * adopted in the public web bundle.
     */
    private static final String[] ALPN_PROTOCOLS = {"http/1.1"};

    /**
     * The endpoint identification algorithm used for hostname
     * verification against the server certificate.
     *
     * <p>The value {@code "HTTPS"} engages the JDK's RFC 2818 / RFC 6125
     * logic so leaf certificates are matched against the SNI server name;
     * leaving it unset would skip hostname verification entirely.
     */
    private static final String ENDPOINT_IDENTIFICATION_ALGORITHM = "HTTPS";

    /**
     * The shared singleton instance returned by
     * {@link WhatsAppSslContextFactory#chrome()}.
     */
    static final ChromeSslContextFactory INSTANCE = new ChromeSslContextFactory();

    /**
     * The shared default TLS context, initialised once at
     * construction.
     */
    private final SSLContext sslContext;

    /**
     * Constructs the singleton, pre-initialising the {@link SSLContext}.
     *
     * <p>The constructor is private so callers obtain the factory through
     * {@link #INSTANCE}. It pre-initialises the {@link SSLContext} with the
     * JDK default trust and key managers so {@link #sslContext()} can return
     * without re-throwing checked exceptions.
     *
     * @throws IllegalStateException if the JDK cannot provide a TLS
     *         {@link SSLContext}
     */
    private ChromeSslContextFactory() {
        try {
            var context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            this.sslContext = context;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chrome ssl context initialized");
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to initialize chrome ssl context", e);
            throw new IllegalStateException("Failed to create SSL context", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SSLContext sslContext() {
        return sslContext;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns a fresh {@link SSLParameters}
     * carrying {@link #CHROME_CIPHER_SUITES}, {@link #ALPN_PROTOCOLS}
     * and {@link #ENDPOINT_IDENTIFICATION_ALGORITHM} on every call so
     * caller-side mutation (typically setting SNI server names) does
     * not affect other connections sharing the same factory.
     */
    @Override
    public SSLParameters sslParameters() {
        var params = new SSLParameters();
        params.setCipherSuites(CHROME_CIPHER_SUITES);
        params.setApplicationProtocols(ALPN_PROTOCOLS);
        params.setEndpointIdentificationAlgorithm(ENDPOINT_IDENTIFICATION_ALGORITHM);
        return params;
    }
}

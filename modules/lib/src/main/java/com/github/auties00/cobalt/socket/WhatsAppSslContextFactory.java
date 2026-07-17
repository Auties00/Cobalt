package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * Produces the {@link SSLContext} and {@link SSLParameters} that the
 * Cobalt socket stack and the JDK {@link java.net.http.HttpClient} apply
 * to outbound WhatsApp connections.
 *
 * <p>The factory is pluggable so the socket layer can pick a
 * Chrome-fingerprinted context for endpoints behind JA3 inspection (the
 * default WhatsApp Web hop and the optional TLS-to-proxy hop) and a stock
 * or custom context for peers that do not fingerprint, without leaking the
 * choice into the caller. Test code substitutes a trust-all factory to
 * drive locally-signed proxy servers; enterprise embedders substitute a
 * custom-truststore factory for CA pinning. The factory is supplied at
 * client construction through
 * {@link WhatsAppSocketClient#newCipheredSocketClient(LinkedWhatsAppStore, WhatsAppSslContextFactory)}.
 *
 * @implSpec
 * The two methods deliberately split because the JDK reuses one
 * {@link SSLContext} across hosts but applies {@link SSLParameters}
 * per call site via
 * {@link javax.net.ssl.SSLSocket#setSSLParameters(SSLParameters)} or
 * {@link java.net.http.HttpClient.Builder#sslParameters(SSLParameters)}.
 * Implementations must return a single shared {@link SSLContext} and
 * a fresh {@link SSLParameters} per call so callers may freely set
 * per-host fields (typically SNI) without polluting unrelated
 * connections.
 */
public interface WhatsAppSslContextFactory {
    /**
     * Returns the {@link SSLContext} backing this factory.
     *
     * <p>The same instance may be returned across invocations and may be
     * shared across threads; callers must not mutate its state. Per-connection
     * objects are obtained from it via {@link SSLContext#getSocketFactory()}
     * or {@link SSLContext#createSSLEngine()}.
     *
     * @implSpec
     * Implementations must return a context that has already been
     * initialised; lazy initialisation would force every caller to
     * recover from {@link java.security.KeyManagementException} on
     * each use.
     *
     * @return a configured {@link SSLContext}
     */
    SSLContext sslContext();

    /**
     * Returns the {@link SSLParameters} to apply to an outbound TLS
     * session opened from this factory's context.
     *
     * <p>The returned object carries the cipher suite ordering, ALPN
     * advertisement and endpoint identification algorithm that the caller
     * copies onto the socket or {@link java.net.http.HttpClient}. Callers
     * commonly mutate the SNI server names via
     * {@link SSLParameters#setServerNames(java.util.List)} on the returned
     * instance.
     *
     * @implSpec
     * A fresh instance must be returned on every invocation so the
     * caller-side mutation does not bleed into unrelated connections.
     *
     * @return the parameters carrying the cipher suite ordering, ALPN
     *         protocols and endpoint identification algorithm
     */
    SSLParameters sslParameters();

    /**
     * Returns the shared Chrome-fingerprinted factory that reproduces
     * the cipher suite ordering Chrome advertises so JA3-fingerprinting
     * endpoints accept the connection.
     *
     * <p>This is the default for every {@link WhatsAppSocketClient} unless
     * the caller passes a custom factory. WhatsApp's edge JA3-screens
     * unrecognised TLS clients; a stock JDK fingerprint is rejected with a
     * TCP RST before the application layer sees anything.
     *
     * @return the singleton Chrome-fingerprinted factory
     */
    static WhatsAppSslContextFactory chrome() {
        return ChromeSslContextFactory.INSTANCE;
    }
}

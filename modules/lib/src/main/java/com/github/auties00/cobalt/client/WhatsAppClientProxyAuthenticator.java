package com.github.auties00.cobalt.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * The authentication strategy used by a {@link WhatsAppClientProxy}.
 *
 * <p>Three families are supported: HTTP {@code CONNECT} proxies produce a
 * {@code Proxy-Authorization} header via {@link Http}; SOCKS4 proxies carry
 * a plain user ID via {@link Socks.V4}; SOCKS5 proxies participate in a
 * method-specific sub-negotiation via {@link Socks.V5}. The socket stack
 * negotiates these credentials with the proxy before the TLS and Noise
 * tunnel to the WhatsApp server is established.
 *
 * @see WhatsAppClientProxy
 */
public sealed interface WhatsAppClientProxyAuthenticator {

    /**
     * Authentication strategy for HTTP {@code CONNECT} proxies.
     *
     * <p>Implementations produce the value of the
     * {@code Proxy-Authorization} header that is appended to the
     * {@code CONNECT} request issued to the proxy.
     */
    sealed interface Http extends WhatsAppClientProxyAuthenticator {

        /**
         * Computes the value to send in the {@code Proxy-Authorization}
         * header.
         *
         * @implSpec
         * Called once per connect attempt during the proxy handshake;
         * implementations that mint short-lived tokens compute a fresh value
         * on every call.
         *
         * @return the header value to be sent in the
         *         {@code Proxy-Authorization} field
         */
        String authorization();

        /**
         * HTTP Basic authentication as defined by RFC 7617.
         *
         * <p>The username and password are concatenated with a colon
         * separator, UTF-8 encoded, and Base64 encoded to produce the
         * {@code Proxy-Authorization: Basic <credentials>} value. A
         * {@code null} password is normalised to the empty string so callers
         * may always invoke {@link #authorization()} without a null guard.
         *
         * @param username the username, must not be {@code null}
         * @param password the password; {@code null} is treated as the empty
         *                 string
         */
        record Basic(String username, String password) implements Http {

            /**
             * Rejects a {@code null} username and normalises a {@code null}
             * password to the empty string.
             *
             * @throws NullPointerException if {@code username} is
             *                              {@code null}
             */
            public Basic {
                Objects.requireNonNull(username, "username");
                if (password == null) {
                    password = "";
                }
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation builds {@code Basic <credentials>} with
             * credentials {@code Base64(UTF-8(username + ":" + password))}
             * per RFC 7617; padding characters are preserved.
             */
            @Override
            public String authorization() {
                var pair = username + ":" + password;
                return "Basic " + Base64.getEncoder()
                        .encodeToString(pair.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Authentication strategy for the SOCKS family of proxies.
     *
     * <p>Two variants are supported: SOCKS4 (and its 4a extension) carries a
     * bare user ID via {@link V4}; SOCKS5 negotiates an authentication
     * method code during the initial handshake and may run a method-specific
     * sub-negotiation via {@link V5}.
     */
    sealed interface Socks extends WhatsAppClientProxyAuthenticator {

        /**
         * SOCKS4 user ID authentication.
         *
         * <p>The user ID is serialised as a null-terminated ISO 8859-1
         * string and embedded into the SOCKS4 connect request. SOCKS4 does
         * not actually authenticate the request; the identifier is
         * informational for the proxy logs.
         *
         * @param userId the user identifier sent during the SOCKS4
         *               handshake, must not be {@code null}
         */
        record V4(String userId) implements Socks {

            /**
             * Rejects a {@code null} user identifier.
             *
             * @throws NullPointerException if {@code userId} is {@code null}
             */
            public V4 {
                Objects.requireNonNull(userId, "userId");
            }
        }

        /**
         * Authentication strategy for SOCKS5 proxies as defined by RFC 1928.
         *
         * <p>Implementations advertise a SOCKS5 method number via
         * {@link #methodId()} during the initial method negotiation and then
         * handle any method-specific sub-negotiation.
         */
        sealed interface V5 extends Socks {

            /**
             * Returns the SOCKS5 method identifier announced by this
             * authenticator during method negotiation.
             *
             * <p>Common values are {@code 0x00} for no authentication and
             * {@code 0x02} for the username and password sub-negotiation
             * defined by RFC 1929.
             *
             * @return the SOCKS5 method identifier
             */
            int methodId();

            /**
             * SOCKS5 username and password authentication, method
             * {@code 0x02} as defined by RFC 1929.
             *
             * <p>Both fields are ISO 8859-1 encoded and capped at 255 bytes
             * each by the wire format. A {@code null} password is tolerated
             * and treated as the empty string to match common proxy
             * deployments that only enforce the username.
             *
             * @param username the username, must not be {@code null} and
             *                 must be at most 255 ISO 8859-1 bytes
             * @param password the password, may be {@code null} (treated as
             *                 the empty string) and, when non-null, must be
             *                 at most 255 ISO 8859-1 bytes
             */
            record UserPassword(String username, String password) implements V5 {

                /**
                 * The SOCKS5 method identifier for the username and password
                 * sub-negotiation defined by RFC 1929.
                 */
                private static final int METHOD_ID = 0x02;

                /**
                 * The maximum number of ISO 8859-1 bytes allowed in either
                 * field by the RFC 1929 wire format.
                 */
                private static final int MAX_LENGTH = 255;

                /**
                 * Constructs a new {@code UserPassword} authenticator,
                 * validating both fields against the 255-byte RFC 1929
                 * wire-format cap.
                 *
                 * @param username the username, must not be {@code null} and
                 *                 must fit in 255 ISO 8859-1 bytes
                 * @param password the password, may be {@code null} (treated
                 *                 as empty) and, when non-null, must fit in
                 *                 255 ISO 8859-1 bytes
                 * @throws NullPointerException     if {@code username} is
                 *                                  {@code null}
                 * @throws IllegalArgumentException if either field exceeds
                 *                                  255 ISO 8859-1 bytes
                 */
                public UserPassword(String username, String password) {
                    Objects.requireNonNull(username, "username");
                    if (username.getBytes(StandardCharsets.ISO_8859_1).length > MAX_LENGTH) {
                        throw new IllegalArgumentException(
                                "username must be at most 255 bytes (ISO_8859_1)");
                    }
                    this.username = username;

                    if (password == null) {
                        this.password = "";
                    } else {
                        if (password.getBytes(StandardCharsets.ISO_8859_1).length > MAX_LENGTH) {
                            throw new IllegalArgumentException(
                                    "password must be at most 255 bytes (ISO_8859_1)");
                        }
                        this.password = password;
                    }
                }

                /**
                 * {@inheritDoc}
                 *
                 * @return {@code 0x02}, as defined by RFC 1929
                 */
                @Override
                public int methodId() {
                    return METHOD_ID;
                }
            }
        }
    }
}

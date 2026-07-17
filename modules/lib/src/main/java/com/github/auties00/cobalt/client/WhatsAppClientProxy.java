package com.github.auties00.cobalt.client;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * The proxy configuration a {@link WhatsAppClient} uses when opening outbound
 * connections to the WhatsApp servers.
 *
 * <p>Two protocol families are supported: HTTP {@code CONNECT} tunnels
 * (plaintext via {@link Http.Plain} or TLS via {@link Http.Secure}) and
 * SOCKS tunnels (4, 4a, 5, and 5h via {@link Socks.V4} and
 * {@link Socks.V5}). Construct instances via the typed factories
 * ({@link #ofHttp(String, int)}, {@link #ofSocks5(String, int)},
 * {@link #ofSocks5h(String, int)}, and the others) or parse a URI via
 * {@link #of(URI)}.
 *
 * @apiNote
 * Wire this into the client through the proxy setter on the flavour-specific
 * builders when all outbound traffic must be routed through an intermediate
 * hop. Prefer {@link #of(URI)} when the proxy is supplied through an
 * environment variable.
 *
 * @implNote
 * On the Linked transport this implementation negotiates the proxy tunnel
 * before the Noise handshake runs; the tunnel client opens the socket and
 * only then hands it to the WhatsApp protocol layer. On the Cloud transport
 * the proxy is installed on the underlying HTTP client.
 *
 * @see WhatsAppClientProxyAuthenticator
 */
public sealed interface WhatsAppClientProxy {

    /**
     * Returns the proxy server hostname or IP address.
     *
     * @return the hostname or IP address used to reach the proxy
     */
    String host();

    /**
     * Returns the proxy server TCP port.
     *
     * @return the TCP port between {@code 1} and {@code 65535}
     */
    int port();

    /**
     * Returns the authentication strategy configured for this proxy, if
     * any.
     *
     * @return an {@link Optional} wrapping the authenticator, or empty if
     *         no authentication is required
     */
    Optional<? extends WhatsAppClientProxyAuthenticator> authenticator();

    /**
     * Creates a plain HTTP {@code CONNECT} proxy configuration without
     * authentication.
     *
     * @apiNote
     * Use when the proxy accepts anonymous connections; for credential
     * support see {@link #ofHttp(String, int, WhatsAppClientProxyAuthenticator.Http)}.
     *
     * @param host the proxy hostname
     * @param port the proxy port
     * @return a new plain HTTP configuration
     */
    static Http.Plain ofHttp(String host, int port) {
        return new Http.Plain(host, port, null);
    }

    /**
     * Creates a plain HTTP {@code CONNECT} proxy configuration with the
     * given authenticator.
     *
     * @param host          the proxy hostname
     * @param port          the proxy port
     * @param authenticator the authentication strategy
     * @return a new plain HTTP configuration
     */
    static Http.Plain ofHttp(String host, int port, WhatsAppClientProxyAuthenticator.Http authenticator) {
        return new Http.Plain(host, port, authenticator);
    }

    /**
     * Creates a TLS-encrypted HTTP {@code CONNECT} proxy configuration
     * without authentication.
     *
     * <p>The WhatsApp Noise tunnel runs inside the TLS-secured proxy tunnel
     * for a double-encrypted transport.
     *
     * @apiNote
     * Use when the proxy itself terminates TLS and accepts anonymous
     * connections.
     *
     * @param host the proxy hostname
     * @param port the proxy port
     * @return a new TLS HTTP configuration
     */
    static Http.Secure ofHttps(String host, int port) {
        return new Http.Secure(host, port, null);
    }

    /**
     * Creates a TLS-encrypted HTTP {@code CONNECT} proxy configuration with
     * the given authenticator.
     *
     * @param host          the proxy hostname
     * @param port          the proxy port
     * @param authenticator the authentication strategy
     * @return a new TLS HTTP configuration
     */
    static Http.Secure ofHttps(String host, int port, WhatsAppClientProxyAuthenticator.Http authenticator) {
        return new Http.Secure(host, port, authenticator);
    }

    /**
     * Creates a SOCKS5 proxy configuration that resolves destination
     * hostnames on the client side.
     *
     * @apiNote
     * Use when the local resolver should drive DNS; for proxy-side DNS
     * resolution see {@link #ofSocks5h(String, int)}.
     *
     * @param host the proxy hostname
     * @param port the proxy port
     * @return a new SOCKS5 local-DNS configuration
     */
    static Socks.V5.Local ofSocks5(String host, int port) {
        return new Socks.V5.Local(host, port, null);
    }

    /**
     * Creates a SOCKS5 proxy configuration with local DNS resolution and
     * the given authenticator.
     *
     * @param host          the proxy hostname
     * @param port          the proxy port
     * @param authenticator the SOCKS5 authenticator
     * @return a new SOCKS5 local-DNS configuration
     */
    static Socks.V5.Local ofSocks5(String host, int port, WhatsAppClientProxyAuthenticator.Socks.V5 authenticator) {
        return new Socks.V5.Local(host, port, authenticator);
    }

    /**
     * Creates a SOCKS5 proxy configuration that resolves destination
     * hostnames on the proxy side.
     *
     * @apiNote
     * The {@code socks5h} variant defers DNS to the proxy; use it when the
     * local network cannot resolve the destination or when DNS leakage must
     * be avoided.
     *
     * @param host the proxy hostname
     * @param port the proxy port
     * @return a new SOCKS5 remote-DNS configuration
     */
    static Socks.V5.Remote ofSocks5h(String host, int port) {
        return new Socks.V5.Remote(host, port, null);
    }

    /**
     * Creates a SOCKS5 proxy configuration with remote DNS resolution and
     * the given authenticator.
     *
     * @param host          the proxy hostname
     * @param port          the proxy port
     * @param authenticator the SOCKS5 authenticator
     * @return a new SOCKS5 remote-DNS configuration
     */
    static Socks.V5.Remote ofSocks5h(String host, int port, WhatsAppClientProxyAuthenticator.Socks.V5 authenticator) {
        return new Socks.V5.Remote(host, port, authenticator);
    }

    /**
     * Creates a SOCKS4 proxy configuration without a user ID.
     *
     * @apiNote
     * SOCKS4 supports only IPv4 destinations; pick
     * {@link #ofSocks4a(String, int)} if the proxy must resolve the
     * destination hostname.
     *
     * @param host the proxy hostname
     * @param port the proxy port
     * @return a new SOCKS4 local-DNS configuration
     */
    static Socks.V4.Local ofSocks4(String host, int port) {
        return new Socks.V4.Local(host, port, null);
    }

    /**
     * Creates a SOCKS4 proxy configuration with the given user ID.
     *
     * @param host          the proxy hostname
     * @param port          the proxy port
     * @param authenticator the SOCKS4 user ID authenticator
     * @return a new SOCKS4 local-DNS configuration
     */
    static Socks.V4.Local ofSocks4(String host, int port, WhatsAppClientProxyAuthenticator.Socks.V4 authenticator) {
        return new Socks.V4.Local(host, port, authenticator);
    }

    /**
     * Creates a SOCKS4a proxy configuration with remote DNS resolution and
     * no user ID.
     *
     * <p>The 4a extension uses the {@code 0.0.0.x} sentinel IP to defer
     * hostname resolution to the proxy.
     *
     * @param host the proxy hostname
     * @param port the proxy port
     * @return a new SOCKS4a remote-DNS configuration
     */
    static Socks.V4.Remote ofSocks4a(String host, int port) {
        return new Socks.V4.Remote(host, port, null);
    }

    /**
     * Creates a SOCKS4a proxy configuration with remote DNS resolution and
     * the given user ID.
     *
     * @param host          the proxy hostname
     * @param port          the proxy port
     * @param authenticator the SOCKS4 user ID authenticator
     * @return a new SOCKS4a remote-DNS configuration
     */
    static Socks.V4.Remote ofSocks4a(String host, int port, WhatsAppClientProxyAuthenticator.Socks.V4 authenticator) {
        return new Socks.V4.Remote(host, port, authenticator);
    }

    /**
     * Parses a proxy configuration from a URI.
     *
     * <p>Supported schemes are {@code http} (default port {@code 80}),
     * {@code https} (default port {@code 443}), {@code socks4},
     * {@code socks4a}, {@code socks5}, and {@code socks5h} (all defaulting
     * to port {@code 1080}). Credentials in the URI's user-info component
     * become a Basic or RFC 1929 authenticator automatically.
     *
     * @apiNote
     * Use this when the proxy is supplied via an environment variable such
     * as {@code HTTPS_PROXY} or {@code ALL_PROXY}.
     *
     * @implNote
     * This implementation extracts the user-info eagerly and produces an
     * authenticator only when a username is present; an empty password is
     * preserved as the empty string.
     *
     * @param uri the proxy URI
     * @return the parsed proxy configuration
     * @throws NullPointerException     if {@code uri}, its scheme, or its
     *                                  host is {@code null}
     * @throws IllegalArgumentException if the scheme is not one of the
     *                                  supported values
     */
    static WhatsAppClientProxy of(URI uri) {
        Objects.requireNonNull(uri, "uri");
        var scheme = Objects.requireNonNull(uri.getScheme(), "Proxy URI scheme cannot be null").toLowerCase();
        var host = Objects.requireNonNull(uri.getHost(), "Proxy URI host cannot be null");
        var port = uri.getPort();
        var userInfo = uri.getUserInfo();

        String username = null;
        String password = null;
        if (userInfo != null) {
            var colon = userInfo.indexOf(':');
            username = colon == -1 ? userInfo : userInfo.substring(0, colon);
            password = colon == -1 ? "" : userInfo.substring(colon + 1);
        }

        return switch (scheme) {
            case "http" -> {
                var p = port == -1 ? 80 : port;
                yield username != null
                        ? ofHttp(host, p, new WhatsAppClientProxyAuthenticator.Http.Basic(username, password))
                        : ofHttp(host, p);
            }
            case "https" -> {
                var p = port == -1 ? 443 : port;
                yield username != null
                        ? ofHttps(host, p, new WhatsAppClientProxyAuthenticator.Http.Basic(username, password))
                        : ofHttps(host, p);
            }
            case "socks4" -> {
                var p = port == -1 ? 1080 : port;
                yield username != null
                        ? ofSocks4(host, p, new WhatsAppClientProxyAuthenticator.Socks.V4(username))
                        : ofSocks4(host, p);
            }
            case "socks4a" -> {
                var p = port == -1 ? 1080 : port;
                yield username != null
                        ? ofSocks4a(host, p, new WhatsAppClientProxyAuthenticator.Socks.V4(username))
                        : ofSocks4a(host, p);
            }
            case "socks5" -> {
                var p = port == -1 ? 1080 : port;
                yield username != null
                        ? ofSocks5(host, p, new WhatsAppClientProxyAuthenticator.Socks.V5.UserPassword(username, password))
                        : ofSocks5(host, p);
            }
            case "socks5h" -> {
                var p = port == -1 ? 1080 : port;
                yield username != null
                        ? ofSocks5h(host, p, new WhatsAppClientProxyAuthenticator.Socks.V5.UserPassword(username, password))
                        : ofSocks5h(host, p);
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported proxy scheme: " + scheme);
        };
    }

    /**
     * An HTTP {@code CONNECT} proxy configuration.
     *
     * <p>The tunnel to the proxy is either plaintext ({@link Plain}) or
     * TLS-encrypted ({@link Secure}). The TLS variant negotiates a TLS
     * session with the proxy before the {@code CONNECT} request, giving a
     * double-encrypted transport once the WhatsApp Noise session runs
     * inside.
     */
    sealed interface Http extends WhatsAppClientProxy {

        /**
         * {@inheritDoc}
         *
         * @implSpec
         * Narrows the return type to the HTTP-specific authenticator.
         */
        @Override
        Optional<WhatsAppClientProxyAuthenticator.Http> authenticator();

        /**
         * A plain HTTP {@code CONNECT} proxy configuration whose tunnel to
         * the proxy itself is not encrypted.
         */
        final class Plain implements Http {

            /**
             * The proxy hostname.
             */
            private final String host;

            /**
             * The proxy TCP port.
             */
            private final int port;

            /**
             * The authentication strategy, or {@code null} for anonymous
             * connections.
             */
            private final WhatsAppClientProxyAuthenticator.Http authenticator;

            /**
             * Constructs a plain HTTP proxy configuration, reachable through
             * {@link #ofHttp(String, int)} or its overload.
             *
             * @param host          the proxy hostname, must not be
             *                      {@code null}
             * @param port          the proxy port, must be between {@code 1}
             *                      and {@code 65535}
             * @param authenticator the authentication strategy, or
             *                      {@code null}
             * @throws NullPointerException     if {@code host} is
             *                                  {@code null}
             * @throws IllegalArgumentException if {@code port} is out of
             *                                  range
             */
            private Plain(String host, int port, WhatsAppClientProxyAuthenticator.Http authenticator) {
                Objects.requireNonNull(host, "host");
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException(
                            "port must be between 1 and 65535: " + port);
                }
                this.host = host;
                this.port = port;
                this.authenticator = authenticator;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String host() {
                return host;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int port() {
                return port;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Optional<WhatsAppClientProxyAuthenticator.Http> authenticator() {
                return Optional.ofNullable(authenticator);
            }

            /**
             * Compares this proxy to another object for structural
             * equality.
             *
             * @param obj the object to compare with
             * @return {@code true} if {@code obj} is another {@code Plain}
             *         with the same host, port, and authenticator
             */
            @Override
            public boolean equals(Object obj) {
                return obj == this
                       || (obj instanceof Plain other
                           && host.equals(other.host)
                           && port == other.port
                           && Objects.equals(authenticator, other.authenticator));
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(host, port, authenticator);
            }

            /**
             * Returns a human-readable description suitable for logs.
             *
             * @return the string representation
             */
            @Override
            public String toString() {
                return "Http.Plain[host=" + host
                       + ", port=" + port
                       + ", authenticator=" + authenticator + "]";
            }
        }

        /**
         * A TLS-encrypted HTTP {@code CONNECT} proxy configuration.
         *
         * <p>The tunnel to the proxy is established over TLS using the
         * configured hostname for SNI and certificate verification. The
         * WhatsApp Noise session runs inside, producing a double-encrypted
         * transport.
         */
        final class Secure implements Http {

            /**
             * The proxy hostname, also used as the TLS SNI value.
             */
            private final String host;

            /**
             * The proxy TCP port.
             */
            private final int port;

            /**
             * The authentication strategy, or {@code null} for anonymous
             * connections.
             */
            private final WhatsAppClientProxyAuthenticator.Http authenticator;

            /**
             * Constructs a TLS-encrypted HTTP proxy configuration, reachable
             * through {@link #ofHttps(String, int)} or its overload.
             *
             * @param host          the proxy hostname, must not be
             *                      {@code null}
             * @param port          the proxy port, must be between {@code 1}
             *                      and {@code 65535}
             * @param authenticator the authentication strategy, or
             *                      {@code null}
             * @throws NullPointerException     if {@code host} is
             *                                  {@code null}
             * @throws IllegalArgumentException if {@code port} is out of
             *                                  range
             */
            private Secure(String host, int port, WhatsAppClientProxyAuthenticator.Http authenticator) {
                Objects.requireNonNull(host, "host");
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException(
                            "port must be between 1 and 65535: " + port);
                }
                this.host = host;
                this.port = port;
                this.authenticator = authenticator;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String host() {
                return host;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int port() {
                return port;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Optional<WhatsAppClientProxyAuthenticator.Http> authenticator() {
                return Optional.ofNullable(authenticator);
            }

            /**
             * Compares this proxy to another object for structural
             * equality.
             *
             * @param obj the object to compare with
             * @return {@code true} if {@code obj} is another {@code Secure}
             *         with the same host, port, and authenticator
             */
            @Override
            public boolean equals(Object obj) {
                return obj == this || (obj instanceof Secure other
                                       && host.equals(other.host)
                                       && port == other.port
                                       && Objects.equals(authenticator, other.authenticator));
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(host, port, authenticator);
            }

            /**
             * Returns a human-readable description suitable for logs.
             *
             * @return the string representation
             */
            @Override
            public String toString() {
                return "Http.Secure[host=" + host
                       + ", port=" + port
                       + ", authenticator=" + authenticator + "]";
            }
        }
    }

    /**
     * A SOCKS proxy configuration covering SOCKS4, the 4a extension, and
     * SOCKS5.
     *
     * <p>SOCKS4 variants ({@link V4.Local} and {@link V4.Remote}) carry an
     * optional user ID. SOCKS5 variants ({@link V5.Local} and
     * {@link V5.Remote}) support RFC 1929 username and password
     * authentication and optional remote DNS resolution (the
     * {@code socks5h} scheme).
     */
    sealed interface Socks extends WhatsAppClientProxy {

        /**
         * {@inheritDoc}
         *
         * @implSpec
         * Narrows the return type to the SOCKS-specific authenticator.
         */
        @Override
        Optional<? extends WhatsAppClientProxyAuthenticator.Socks> authenticator();

        /**
         * A SOCKS4 proxy configuration.
         *
         * <p>Hostnames are resolved either by the client ({@link Local}) or
         * by the proxy ({@link Remote}, commonly referred to as SOCKS4a).
         */
        sealed interface V4 extends Socks {

            /**
             * {@inheritDoc}
             *
             * @implSpec
             * Narrows the return type to the SOCKS4 user-ID authenticator.
             */
            @Override
            Optional<WhatsAppClientProxyAuthenticator.Socks.V4> authenticator();

            /**
             * A SOCKS4 proxy configuration that resolves destination
             * hostnames on the client side.
             *
             * <p>Only IPv4 destinations are supported by SOCKS4; for
             * proxy-side hostname resolution use {@link Remote}.
             */
            final class Local implements V4 {

                /**
                 * The proxy hostname.
                 */
                private final String host;

                /**
                 * The proxy TCP port.
                 */
                private final int port;

                /**
                 * The SOCKS4 user-ID authenticator, or {@code null} for an
                 * anonymous SOCKS4 connect.
                 */
                private final WhatsAppClientProxyAuthenticator.Socks.V4 authenticator;

                /**
                 * Constructs a SOCKS4 local-DNS proxy configuration,
                 * reachable through {@link #ofSocks4(String, int)} or its
                 * overload.
                 *
                 * @param host          the proxy hostname, must not be
                 *                      {@code null}
                 * @param port          the proxy port, must be between
                 *                      {@code 1} and {@code 65535}
                 * @param authenticator the user-ID authenticator, or
                 *                      {@code null}
                 * @throws NullPointerException     if {@code host} is
                 *                                  {@code null}
                 * @throws IllegalArgumentException if {@code port} is out of
                 *                                  range
                 */
                private Local(String host, int port, WhatsAppClientProxyAuthenticator.Socks.V4 authenticator) {
                    Objects.requireNonNull(host, "host");
                    if (port < 1 || port > 65535) {
                        throw new IllegalArgumentException(
                                "port must be between 1 and 65535: " + port);
                    }
                    this.host = host;
                    this.port = port;
                    this.authenticator = authenticator;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String host() {
                    return host;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public int port() {
                    return port;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Optional<WhatsAppClientProxyAuthenticator.Socks.V4> authenticator() {
                    return Optional.ofNullable(authenticator);
                }

                /**
                 * Compares this proxy to another object for structural
                 * equality.
                 *
                 * @param obj the object to compare with
                 * @return {@code true} if {@code obj} is another
                 *         {@code Local} with the same host, port, and
                 *         authenticator
                 */
                @Override
                public boolean equals(Object obj) {
                    return obj == this || (obj instanceof Local other
                                           && host.equals(other.host)
                                           && port == other.port
                                           && Objects.equals(authenticator, other.authenticator));
                }

                /**
                 * Returns a hash code consistent with
                 * {@link #equals(Object)}.
                 *
                 * @return the hash code
                 */
                @Override
                public int hashCode() {
                    return Objects.hash(host, port, authenticator);
                }

                /**
                 * Returns a human-readable description suitable for logs.
                 *
                 * @return the string representation
                 */
                @Override
                public String toString() {
                    return "Socks.V4.Local[host=" + host
                           + ", port=" + port
                           + ", authenticator=" + authenticator + "]";
                }
            }

            /**
             * A SOCKS4a proxy configuration that resolves destination
             * hostnames on the proxy side via the {@code 0.0.0.x} sentinel
             * IP.
             */
            final class Remote implements V4 {

                /**
                 * The proxy hostname.
                 */
                private final String host;

                /**
                 * The proxy TCP port.
                 */
                private final int port;

                /**
                 * The SOCKS4 user-ID authenticator, or {@code null} for an
                 * anonymous SOCKS4a connect.
                 */
                private final WhatsAppClientProxyAuthenticator.Socks.V4 authenticator;

                /**
                 * Constructs a SOCKS4a remote-DNS proxy configuration,
                 * reachable through {@link #ofSocks4a(String, int)} or its
                 * overload.
                 *
                 * @param host          the proxy hostname, must not be
                 *                      {@code null}
                 * @param port          the proxy port, must be between
                 *                      {@code 1} and {@code 65535}
                 * @param authenticator the user-ID authenticator, or
                 *                      {@code null}
                 * @throws NullPointerException     if {@code host} is
                 *                                  {@code null}
                 * @throws IllegalArgumentException if {@code port} is out of
                 *                                  range
                 */
                private Remote(String host, int port, WhatsAppClientProxyAuthenticator.Socks.V4 authenticator) {
                    Objects.requireNonNull(host, "host");
                    if (port < 1 || port > 65535) {
                        throw new IllegalArgumentException(
                                "port must be between 1 and 65535: " + port);
                    }
                    this.host = host;
                    this.port = port;
                    this.authenticator = authenticator;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String host() {
                    return host;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public int port() {
                    return port;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Optional<WhatsAppClientProxyAuthenticator.Socks.V4> authenticator() {
                    return Optional.ofNullable(authenticator);
                }

                /**
                 * Compares this proxy to another object for structural
                 * equality.
                 *
                 * @param obj the object to compare with
                 * @return {@code true} if {@code obj} is another
                 *         {@code Remote} with the same host, port, and
                 *         authenticator
                 */
                @Override
                public boolean equals(Object obj) {
                    return obj == this || (obj instanceof Remote other
                                           && host.equals(other.host)
                                           && port == other.port
                                           && Objects.equals(authenticator, other.authenticator));
                }

                /**
                 * Returns a hash code consistent with
                 * {@link #equals(Object)}.
                 *
                 * @return the hash code
                 */
                @Override
                public int hashCode() {
                    return Objects.hash(host, port, authenticator);
                }

                /**
                 * Returns a human-readable description suitable for logs.
                 *
                 * @return the string representation
                 */
                @Override
                public String toString() {
                    return "Socks.V4.Remote[host=" + host
                           + ", port=" + port
                           + ", authenticator=" + authenticator + "]";
                }
            }
        }

        /**
         * A SOCKS5 proxy configuration as defined by RFC 1928.
         *
         * <p>Hostnames are resolved either by the client ({@link Local}) or
         * by the proxy ({@link Remote}, commonly referred to as
         * {@code socks5h}).
         */
        sealed interface V5 extends Socks {

            /**
             * {@inheritDoc}
             *
             * @implSpec
             * Narrows the return type to the SOCKS5 authenticator (typically
             * RFC 1929 username and password).
             */
            @Override
            Optional<WhatsAppClientProxyAuthenticator.Socks.V5> authenticator();

            /**
             * A SOCKS5 proxy configuration that resolves destination
             * hostnames on the client side.
             */
            final class Local implements V5 {

                /**
                 * The proxy hostname.
                 */
                private final String host;

                /**
                 * The proxy TCP port.
                 */
                private final int port;

                /**
                 * The SOCKS5 authenticator, or {@code null} for an anonymous
                 * SOCKS5 connect.
                 */
                private final WhatsAppClientProxyAuthenticator.Socks.V5 authenticator;

                /**
                 * Constructs a SOCKS5 local-DNS proxy configuration,
                 * reachable through {@link #ofSocks5(String, int)} or its
                 * overload.
                 *
                 * @param host          the proxy hostname, must not be
                 *                      {@code null}
                 * @param port          the proxy port, must be between
                 *                      {@code 1} and {@code 65535}
                 * @param authenticator the SOCKS5 authenticator, or
                 *                      {@code null}
                 * @throws NullPointerException     if {@code host} is
                 *                                  {@code null}
                 * @throws IllegalArgumentException if {@code port} is out of
                 *                                  range
                 */
                private Local(String host, int port, WhatsAppClientProxyAuthenticator.Socks.V5 authenticator) {
                    Objects.requireNonNull(host, "host");
                    if (port < 1 || port > 65535) {
                        throw new IllegalArgumentException(
                                "port must be between 1 and 65535: " + port);
                    }
                    this.host = host;
                    this.port = port;
                    this.authenticator = authenticator;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String host() {
                    return host;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public int port() {
                    return port;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Optional<WhatsAppClientProxyAuthenticator.Socks.V5> authenticator() {
                    return Optional.ofNullable(authenticator);
                }

                /**
                 * Compares this proxy to another object for structural
                 * equality.
                 *
                 * @param obj the object to compare with
                 * @return {@code true} if {@code obj} is another
                 *         {@code Local} with the same host, port, and
                 *         authenticator
                 */
                @Override
                public boolean equals(Object obj) {
                    return obj == this || (obj instanceof Local other
                                           && host.equals(other.host)
                                           && port == other.port
                                           && Objects.equals(authenticator, other.authenticator));
                }

                /**
                 * Returns a hash code consistent with
                 * {@link #equals(Object)}.
                 *
                 * @return the hash code
                 */
                @Override
                public int hashCode() {
                    return Objects.hash(host, port, authenticator);
                }

                /**
                 * Returns a human-readable description suitable for logs.
                 *
                 * @return the string representation
                 */
                @Override
                public String toString() {
                    return "Socks.V5.Local[host=" + host
                           + ", port=" + port
                           + ", authenticator=" + authenticator + "]";
                }
            }

            /**
             * A SOCKS5 proxy configuration with remote DNS resolution,
             * commonly referred to as {@code socks5h}.
             */
            final class Remote implements V5 {

                /**
                 * The proxy hostname.
                 */
                private final String host;

                /**
                 * The proxy TCP port.
                 */
                private final int port;

                /**
                 * The SOCKS5 authenticator, or {@code null} for an anonymous
                 * SOCKS5 connect.
                 */
                private final WhatsAppClientProxyAuthenticator.Socks.V5 authenticator;

                /**
                 * Constructs a SOCKS5 remote-DNS proxy configuration,
                 * reachable through {@link #ofSocks5h(String, int)} or its
                 * overload.
                 *
                 * @param host          the proxy hostname, must not be
                 *                      {@code null}
                 * @param port          the proxy port, must be between
                 *                      {@code 1} and {@code 65535}
                 * @param authenticator the SOCKS5 authenticator, or
                 *                      {@code null}
                 * @throws NullPointerException     if {@code host} is
                 *                                  {@code null}
                 * @throws IllegalArgumentException if {@code port} is out of
                 *                                  range
                 */
                private Remote(String host, int port, WhatsAppClientProxyAuthenticator.Socks.V5 authenticator) {
                    Objects.requireNonNull(host, "host");
                    if (port < 1 || port > 65535) {
                        throw new IllegalArgumentException(
                                "port must be between 1 and 65535: " + port);
                    }
                    this.host = host;
                    this.port = port;
                    this.authenticator = authenticator;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public String host() {
                    return host;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public int port() {
                    return port;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Optional<WhatsAppClientProxyAuthenticator.Socks.V5> authenticator() {
                    return Optional.ofNullable(authenticator);
                }

                /**
                 * Compares this proxy to another object for structural
                 * equality.
                 *
                 * @param obj the object to compare with
                 * @return {@code true} if {@code obj} is another
                 *         {@code Remote} with the same host, port, and
                 *         authenticator
                 */
                @Override
                public boolean equals(Object obj) {
                    return obj == this || (obj instanceof Remote other
                                           && host.equals(other.host)
                                           && port == other.port
                                           && Objects.equals(authenticator, other.authenticator));
                }

                /**
                 * Returns a hash code consistent with
                 * {@link #equals(Object)}.
                 *
                 * @return the hash code
                 */
                @Override
                public int hashCode() {
                    return Objects.hash(host, port, authenticator);
                }

                /**
                 * Returns a human-readable description suitable for logs.
                 *
                 * @return the string representation
                 */
                @Override
                public String toString() {
                    return "Socks.V5.Remote[host=" + host
                           + ", port=" + port
                           + ", authenticator=" + authenticator + "]";
                }
            }
        }
    }
}

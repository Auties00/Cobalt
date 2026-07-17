package com.github.auties00.cobalt.wire.cloud;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The WhatsApp Cloud API Calling feature configuration of a phone number.
 *
 * <p>This model projects the {@code calling} settings object: the master enable status, the visibility
 * of the in-app call icon, the call-icon country restrictions ({@link CallIcons}), the
 * callback-permission status, the SRTP key-exchange protocol, the business-hours configuration
 * ({@link CloudCallHours}), and the optional SIP bridge ({@link Sip}) that routes calls to an external
 * PBX.
 */
public final class CloudCallSettings {
    /**
     * The master calling enable status, for example {@code "ENABLED"}, or {@code null} when unset.
     */
    private final String status;

    /**
     * The in-app call-icon visibility, for example {@code "DEFAULT"}, or {@code null} when unset.
     */
    private final String callIconVisibility;

    /**
     * The call-icon country restrictions, or {@code null} when unset.
     */
    private final CallIcons callIcons;

    /**
     * The callback-permission status, for example {@code "ENABLED"}, or {@code null} when unset.
     */
    private final String callbackPermissionStatus;

    /**
     * The SRTP key-exchange protocol, for example {@code "DTLS"} or {@code "SDES"}, or {@code null} when
     * unset.
     */
    private final String srtpKeyExchangeProtocol;

    /**
     * The business-hours configuration, or {@code null} when unset.
     */
    private final CloudCallHours callHours;

    /**
     * The SIP bridge configuration, or {@code null} when unset.
     */
    private final Sip sip;

    /**
     * Constructs a new calling configuration.
     *
     * @param status                   the master enable status, or {@code null} when unset
     * @param callIconVisibility       the call-icon visibility, or {@code null} when unset
     * @param callIcons                the call-icon country restrictions, or {@code null} when unset
     * @param callbackPermissionStatus the callback-permission status, or {@code null} when unset
     * @param srtpKeyExchangeProtocol  the SRTP key-exchange protocol, or {@code null} when unset
     * @param callHours                the business-hours configuration, or {@code null} when unset
     * @param sip                      the SIP bridge configuration, or {@code null} when unset
     */
    public CloudCallSettings(String status, String callIconVisibility, CallIcons callIcons,
                             String callbackPermissionStatus, String srtpKeyExchangeProtocol,
                             CloudCallHours callHours, Sip sip) {
        this.status = status;
        this.callIconVisibility = callIconVisibility;
        this.callIcons = callIcons;
        this.callbackPermissionStatus = callbackPermissionStatus;
        this.srtpKeyExchangeProtocol = srtpKeyExchangeProtocol;
        this.callHours = callHours;
        this.sip = sip;
    }

    /**
     * Returns the master calling enable status.
     *
     * @return an {@link Optional} carrying the status, or empty when unset
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the in-app call-icon visibility.
     *
     * @return an {@link Optional} carrying the visibility, or empty when unset
     */
    public Optional<String> callIconVisibility() {
        return Optional.ofNullable(callIconVisibility);
    }

    /**
     * Returns the call-icon country restrictions.
     *
     * @return an {@link Optional} carrying the call-icon restrictions, or empty when unset
     */
    public Optional<CallIcons> callIcons() {
        return Optional.ofNullable(callIcons);
    }

    /**
     * Returns the callback-permission status.
     *
     * @return an {@link Optional} carrying the status, or empty when unset
     */
    public Optional<String> callbackPermissionStatus() {
        return Optional.ofNullable(callbackPermissionStatus);
    }

    /**
     * Returns the SRTP key-exchange protocol.
     *
     * @return an {@link Optional} carrying the protocol, or empty when unset
     */
    public Optional<String> srtpKeyExchangeProtocol() {
        return Optional.ofNullable(srtpKeyExchangeProtocol);
    }

    /**
     * Returns the business-hours configuration.
     *
     * @return an {@link Optional} carrying the call hours, or empty when unset
     */
    public Optional<CloudCallHours> callHours() {
        return Optional.ofNullable(callHours);
    }

    /**
     * Returns the SIP bridge configuration.
     *
     * @return an {@link Optional} carrying the SIP configuration, or empty when unset
     */
    public Optional<Sip> sip() {
        return Optional.ofNullable(sip);
    }

    /**
     * The call-icon country restrictions of the calling configuration.
     *
     * <p>The restriction limits the in-app call icon to recipients in the listed countries; an empty
     * list places no country restriction.
     */
    public static final class CallIcons {
        /**
         * The two-letter country codes the call icon is restricted to.
         */
        private final List<String> restrictToUserCountries;

        /**
         * Constructs a new call-icon restriction.
         *
         * @param restrictToUserCountries the country codes the call icon is restricted to, or
         *                                {@code null} for none
         */
        public CallIcons(List<String> restrictToUserCountries) {
            this.restrictToUserCountries = restrictToUserCountries == null
                    ? List.of()
                    : List.copyOf(restrictToUserCountries);
        }

        /**
         * Returns the country codes the call icon is restricted to.
         *
         * @return an unmodifiable list of country codes, empty when there is no restriction
         */
        public List<String> restrictToUserCountries() {
            return restrictToUserCountries;
        }
    }

    /**
     * The SIP bridge configuration that routes calls to an external PBX.
     */
    public static final class Sip {
        /**
         * The SIP enable status, for example {@code "ENABLED"}, or {@code null} when unset.
         */
        private final String status;

        /**
         * The configured SIP servers.
         */
        private final List<SipServer> servers;

        /**
         * Constructs a new SIP bridge configuration.
         *
         * @param status  the enable status, or {@code null} when unset
         * @param servers the SIP servers, or {@code null} for none
         */
        public Sip(String status, List<SipServer> servers) {
            this.status = status;
            this.servers = servers == null ? List.of() : List.copyOf(servers);
        }

        /**
         * Returns the SIP enable status.
         *
         * @return an {@link Optional} carrying the status, or empty when unset
         */
        public Optional<String> status() {
            return Optional.ofNullable(status);
        }

        /**
         * Returns the configured SIP servers.
         *
         * @return an unmodifiable list of SIP servers, empty when none were configured
         */
        public List<SipServer> servers() {
            return servers;
        }
    }

    /**
     * A single SIP server endpoint of the SIP bridge.
     */
    public static final class SipServer {
        /**
         * The SIP server hostname, or {@code null} when unset.
         */
        private final String hostname;

        /**
         * The SIP server port, or {@code null} when unset.
         */
        private final Integer port;

        /**
         * The server-generated SIP user password, or {@code null} when absent or not requested.
         */
        private final String sipUserPassword;

        /**
         * The Request-URI user parameters, or {@code null} when none are set. These are appended to the
         * SIP Request-URI as user parameters.
         */
        private final Map<String, String> requestUriUserParams;

        /**
         * The Meta app id associated with the server, present on read responses, or {@code null} when
         * absent. This field is not sent on a write.
         */
        private final Integer appId;

        /**
         * Constructs a new SIP server endpoint.
         *
         * @param hostname             the hostname, or {@code null} when unset
         * @param port                 the port, or {@code null} when unset
         * @param sipUserPassword      the server-generated SIP user password, or {@code null} when absent
         * @param requestUriUserParams the Request-URI user parameters, or {@code null} for none
         * @param appId                the Meta app id, or {@code null} when absent
         */
        public SipServer(String hostname, Integer port, String sipUserPassword,
                         Map<String, String> requestUriUserParams, Integer appId) {
            this.hostname = hostname;
            this.port = port;
            this.sipUserPassword = sipUserPassword;
            this.requestUriUserParams = requestUriUserParams == null
                    ? Map.of()
                    : Map.copyOf(requestUriUserParams);
            this.appId = appId;
        }

        /**
         * Returns the SIP server hostname.
         *
         * @return an {@link Optional} carrying the hostname, or empty when unset
         */
        public Optional<String> hostname() {
            return Optional.ofNullable(hostname);
        }

        /**
         * Returns the SIP server port.
         *
         * @return an {@link Optional} carrying the port, or empty when unset
         */
        public Optional<Integer> port() {
            return Optional.ofNullable(port);
        }

        /**
         * Returns the server-generated SIP user password.
         *
         * @return an {@link Optional} carrying the password, or empty when absent or not requested
         */
        public Optional<String> sipUserPassword() {
            return Optional.ofNullable(sipUserPassword);
        }

        /**
         * Returns the Request-URI user parameters.
         *
         * @return an unmodifiable map of user parameters, empty when none are set
         */
        public Map<String, String> requestUriUserParams() {
            return requestUriUserParams;
        }

        /**
         * Returns the Meta app id associated with the server.
         *
         * @return an {@link Optional} carrying the app id, or empty when absent
         */
        public Optional<Integer> appId() {
            return Optional.ofNullable(appId);
        }
    }
}

package com.github.auties00.cobalt.wire.stanza.iq.media;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the inbound reply variants the relay produces in response to an
 * {@link IqQueryMediaConnsRequest}.
 *
 * <p>The hierarchy is sealed over exactly three variants: {@link Success} carries the parsed
 * media-conn configuration, while {@link ClientError} and {@link ServerError} carry the two
 * failure classes. Splitting failures by class lets the media pipeline decide whether to retry
 * the query (a server-side failure) or to surface the failure to the user (a client-side
 * failure).
 *
 * @implNote
 * This implementation deliberately diverges from WA Web, which collapses every failure to
 * either an {@code E507} ("Insufficient Storage" with a backoff hint) or a generic
 * {@code ServerStatusCodeError}. Splitting into {@link ClientError} versus {@link ServerError}
 * preserves the retry-versus-surface decision that WA Web folds into inline recovery.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryMediaConnsJob")
public sealed interface IqQueryMediaConnsResponse extends IqStanza.Response
        permits IqQueryMediaConnsResponse.Success, IqQueryMediaConnsResponse.ClientError, IqQueryMediaConnsResponse.ServerError {

    /**
     * Tries each variant in priority order and returns the first that parses cleanly.
     *
     * <p>Variants are attempted as {@link Success}, then {@link ClientError}, then
     * {@link ServerError}; only one ever populates because the per-variant parsing helpers
     * match disjoint stanza shapes. An empty result means the stanza matched no documented
     * variant.
     *
     * @param stanza    the inbound IQ stanza received from the relay
     * @param request the original outbound stanza, used to validate echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
     *         when no documented variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob",
            exports = "queryMediaConn", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqQueryMediaConnsResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Carries the projected {@code <media_conn>} grandchild for a successful media-conn lookup.
     *
     * <p>The variant holds the bearer token routed to the {@code Authorization} header on
     * media-CDN requests, the absolute expiry timestamps for the token and the host routes, the
     * retry budgets the CDN advertises, and the list of {@link Host} endpoints with their
     * per-host upload, download, and bucket rules.
     */
    @WhatsAppWebModule(moduleName = "WAWebQueryMediaConnsJob")
    @WhatsAppWebModule(moduleName = "WAMediaConnParser")
    final class Success implements IqQueryMediaConnsResponse {
        /**
         * Holds the relay-issued auth token routed verbatim to the {@code Authorization} header
         * on media-CDN requests.
         */
        private final String authToken;

        /**
         * Holds the absolute unix-second expiry timestamp of {@link #authToken}, computed by
         * folding the relay-supplied {@code auth_ttl} delta into the local clock.
         */
        private final long authTokenExpiry;

        /**
         * Holds the absolute unix-second expiry timestamp of the host routes, computed by
         * folding the relay-supplied {@code ttl} delta into the local clock.
         */
        private final long routesExpiry;

        /**
         * Holds the maximum bucket cardinality the relay supports for sharded downloads.
         *
         * <p>The value is mandatory in the reply and is not range-clipped, despite the
         * four-bucket convention seen in production.
         */
        private final int maxBuckets;

        /**
         * Holds the maximum manual-retry budget per upload.
         *
         * <p>The value is constrained to the closed interval {@code [0, 4]} and defaults to
         * three when the relay omits the attribute or returns a value outside that range.
         */
        private final int maxManualRetry;

        /**
         * Holds the maximum auto-download-retry budget per download.
         *
         * <p>The value follows the same {@code [0, 4]} constraint and the same default of three
         * as {@link #maxManualRetry}.
         */
        private final int maxAutoDownloadRetry;

        /**
         * Holds the list of media-server host endpoints returned by the relay.
         */
        private final List<Host> hosts;

        /**
         * Constructs a new successful reply.
         *
         * <p>The {@code hosts} list is defensively copied; the remaining fields are primitives
         * or immutable strings and are stored directly.
         *
         * @param authToken            the auth token
         * @param authTokenExpiry      the absolute auth-token expiry timestamp, in unix seconds
         * @param routesExpiry         the absolute host-routes expiry timestamp, in unix seconds
         * @param maxBuckets           the bucket cardinality
         * @param maxManualRetry       the manual-retry budget
         * @param maxAutoDownloadRetry the auto-download-retry budget
         * @param hosts                the host endpoints
         * @throws NullPointerException if {@code authToken} or {@code hosts} is {@code null}
         */
        public Success(String authToken, long authTokenExpiry, long routesExpiry,
                       int maxBuckets, int maxManualRetry, int maxAutoDownloadRetry,
                       List<Host> hosts) {
            this.authToken = Objects.requireNonNull(authToken, "authToken cannot be null");
            this.authTokenExpiry = authTokenExpiry;
            this.routesExpiry = routesExpiry;
            this.maxBuckets = maxBuckets;
            this.maxManualRetry = maxManualRetry;
            this.maxAutoDownloadRetry = maxAutoDownloadRetry;
            Objects.requireNonNull(hosts, "hosts cannot be null");
            this.hosts = List.copyOf(hosts);
        }

        /**
         * Returns the auth token.
         *
         * @return the token, never {@code null}
         */
        public String authToken() {
            return authToken;
        }

        /**
         * Returns the absolute auth-token expiry timestamp.
         *
         * @return the timestamp in unix seconds
         */
        public long authTokenExpiry() {
            return authTokenExpiry;
        }

        /**
         * Returns the absolute host-routes expiry timestamp.
         *
         * @return the timestamp in unix seconds
         */
        public long routesExpiry() {
            return routesExpiry;
        }

        /**
         * Returns the bucket cardinality.
         *
         * @return the cardinality
         */
        public int maxBuckets() {
            return maxBuckets;
        }

        /**
         * Returns the manual-retry budget.
         *
         * @return the budget
         */
        public int maxManualRetry() {
            return maxManualRetry;
        }

        /**
         * Returns the auto-download-retry budget.
         *
         * @return the budget
         */
        public int maxAutoDownloadRetry() {
            return maxAutoDownloadRetry;
        }

        /**
         * Returns the unmodifiable list of host endpoints.
         *
         * @return the hosts, never {@code null}
         */
        public List<Host> hosts() {
            return hosts;
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the stanza is not a result envelope echoing
         * the request id, when the {@code <media_conn>} grandchild is absent, or when any
         * mandatory attribute ({@code auth}, {@code auth_ttl}, {@code ttl}, {@code max_buckets})
         * is missing or malformed. Callers treat an empty result as a soft failure and may
         * retry the query.
         *
         * @implNote
         * This implementation folds the relay-supplied {@code auth_ttl} and {@code ttl} deltas
         * into absolute unix-second timestamps via {@link Instant#now()}, mirroring WA Web's
         * {@code attrFutureTime} (which computes {@code now + delta}). The retry caps are
         * silently clamped to {@code [0, 4]} and defaulted to three on out-of-range or missing
         * attributes; the {@code [0, 4]} bound and default of three match WA Web's
         * {@code maybeAttrInt("max_manual_retry", 0, 4)} idiom.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or
         *         {@link Optional#empty()} when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAMediaConnParser",
                exports = "mediaConnParser", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var mediaConnChild = stanza.getChild("media_conn").orElse(null);
            if (mediaConnChild == null) {
                return Optional.empty();
            }
            var auth = mediaConnChild.getAttributeAsString("auth").orElse(null);
            if (auth == null) {
                return Optional.empty();
            }
            var authTtlDelta = mediaConnChild.getAttributeAsLong("auth_ttl", -1L);
            var ttlDelta = mediaConnChild.getAttributeAsLong("ttl", -1L);
            if (authTtlDelta < 0 || ttlDelta < 0) {
                return Optional.empty();
            }
            var nowSeconds = Instant.now().getEpochSecond();
            var authTtl = nowSeconds + authTtlDelta;
            var ttl = nowSeconds + ttlDelta;
            var maxBucketsValue = mediaConnChild.getAttributeAsInt("max_buckets", -1);
            if (maxBucketsValue < 0) {
                return Optional.empty();
            }
            var maxBuckets = maxBucketsValue;
            var maxManualRetry = mediaConnChild.getAttributeAsInt("max_manual_retry", 3);
            if (maxManualRetry < 0 || maxManualRetry > 4) {
                maxManualRetry = 3;
            }
            var maxAutoDownloadRetry = mediaConnChild.getAttributeAsInt("max_auto_download_retry", 3);
            if (maxAutoDownloadRetry < 0 || maxAutoDownloadRetry > 4) {
                maxAutoDownloadRetry = 3;
            }
            var hostNodes = mediaConnChild.getChildren("host");
            var hosts = new ArrayList<Host>(hostNodes.size());
            for (var hostNode : hostNodes) {
                hosts.add(Host.of(hostNode));
            }
            return Optional.of(new Success(auth, authTtl, ttl, maxBuckets,
                    maxManualRetry, maxAutoDownloadRetry, hosts));
        }

        /**
         * Indicates whether the given object is a {@link Success} with equal field values.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link Success} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return this.authTokenExpiry == that.authTokenExpiry
                    && this.routesExpiry == that.routesExpiry
                    && this.maxBuckets == that.maxBuckets
                    && this.maxManualRetry == that.maxManualRetry
                    && this.maxAutoDownloadRetry == that.maxAutoDownloadRetry
                    && Objects.equals(this.authToken, that.authToken)
                    && Objects.equals(this.hosts, that.hosts);
        }

        /**
         * Returns a hash combining every field, consistent with {@link #equals(Object)}.
         *
         * @return the combined hash
         */
        @Override
        public int hashCode() {
            return Objects.hash(authToken, authTokenExpiry, routesExpiry, maxBuckets,
                    maxManualRetry, maxAutoDownloadRetry, hosts);
        }

        /**
         * Returns a string rendering every field of this reply.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqQueryMediaConnsResponse.Success[authToken=" + authToken
                    + ", authTokenExpiry=" + authTokenExpiry
                    + ", routesExpiry=" + routesExpiry
                    + ", maxBuckets=" + maxBuckets
                    + ", maxManualRetry=" + maxManualRetry
                    + ", maxAutoDownloadRetry=" + maxAutoDownloadRetry
                    + ", hosts=" + hosts + ']';
        }

        /**
         * Projects a single {@code <host hostname class ip4 ip6 type>...</host>} subtree into one
         * media-server endpoint with its per-host upload, download, and bucket rules.
         *
         * <p>The media pipeline selects a host from {@link Success#hosts()} based on the media
         * type being transferred ({@link #uploadable()} / {@link #downloadable()}) and on the
         * per-bucket sharding indices ({@link #downloadBuckets()}). The {@link #fallback()} flag
         * marks the host as a last-resort route when the primary hosts are unreachable.
         */
        @WhatsAppWebModule(moduleName = "WAMediaConnParser")
        @WhatsAppWebModule(moduleName = "WAWebQueryMediaConnsJob")
        public static final class Host {
            /**
             * Holds the host DNS hostname routed from the {@code hostname} attribute.
             */
            private final String hostname;

            /**
             * Holds the optional host class label routed from the {@code class} attribute.
             */
            private final String hostClass;

            /**
             * Holds the optional host IPv4 address routed from the {@code ip4} attribute.
             */
            private final String ip4;

            /**
             * Holds the optional host IPv6 address routed from the {@code ip6} attribute.
             */
            private final String ip6;

            /**
             * Holds whether this host is the documented fallback route.
             *
             * <p>The flag is {@code true} when the {@code type} attribute equals
             * {@code "fallback"}.
             */
            private final boolean fallback;

            /**
             * Holds the media-type tags supported by uploads to this host.
             *
             * <p>Tags are routed from {@code <upload/>} grandchild descriptions (for example
             * {@code "image"}, {@code "video"}, {@code "ptt"}). An empty list left by an absent
             * wrapper signals the SERVER_MEDIA default of every documented media type; an empty
             * list from a present wrapper means explicitly no supported types.
             */
            private final List<String> uploadable;

            /**
             * Holds the media-type tags supported by downloads from this host.
             *
             * <p>Tags follow the same shape and the same SERVER_MEDIA default as
             * {@link #uploadable}.
             */
            private final List<String> downloadable;

            /**
             * Holds the bucket indices eligible for downloads through this host.
             *
             * <p>Indices are routed from {@code <download_buckets/>} grandchild descriptions
             * parsed as integers; an empty list means the host serves every bucket.
             */
            private final List<Integer> downloadBuckets;

            /**
             * Constructs a new host endpoint.
             *
             * <p>The {@code uploadable}, {@code downloadable}, and {@code downloadBuckets} lists
             * are defensively copied.
             *
             * @param hostname        the DNS hostname
             * @param hostClass       the optional class label, or {@code null}
             * @param ip4             the optional IPv4 address, or {@code null}
             * @param ip6             the optional IPv6 address, or {@code null}
             * @param fallback        the fallback flag
             * @param uploadable      the upload media-type tags
             * @param downloadable    the download media-type tags
             * @param downloadBuckets the download bucket indices
             * @throws NullPointerException if {@code hostname}, {@code uploadable},
             *                              {@code downloadable}, or {@code downloadBuckets}
             *                              is {@code null}
             */
            public Host(String hostname, String hostClass, String ip4, String ip6,
                        boolean fallback, List<String> uploadable, List<String> downloadable,
                        List<Integer> downloadBuckets) {
                this.hostname = Objects.requireNonNull(hostname, "hostname cannot be null");
                this.hostClass = hostClass;
                this.ip4 = ip4;
                this.ip6 = ip6;
                this.fallback = fallback;
                Objects.requireNonNull(uploadable, "uploadable cannot be null");
                Objects.requireNonNull(downloadable, "downloadable cannot be null");
                Objects.requireNonNull(downloadBuckets, "downloadBuckets cannot be null");
                this.uploadable = List.copyOf(uploadable);
                this.downloadable = List.copyOf(downloadable);
                this.downloadBuckets = List.copyOf(downloadBuckets);
            }

            /**
             * Returns the DNS hostname.
             *
             * @return the hostname, never {@code null}
             */
            public String hostname() {
                return hostname;
            }

            /**
             * Returns the optional class label.
             *
             * @return an {@link Optional} carrying the label, or {@link Optional#empty()}
             *         when absent
             */
            public Optional<String> hostClass() {
                return Optional.ofNullable(hostClass);
            }

            /**
             * Returns the optional IPv4 address.
             *
             * @return an {@link Optional} carrying the address, or {@link Optional#empty()}
             *         when absent
             */
            public Optional<String> ip4() {
                return Optional.ofNullable(ip4);
            }

            /**
             * Returns the optional IPv6 address.
             *
             * @return an {@link Optional} carrying the address, or {@link Optional#empty()}
             *         when absent
             */
            public Optional<String> ip6() {
                return Optional.ofNullable(ip6);
            }

            /**
             * Returns the fallback flag.
             *
             * @return {@code true} when this host is the documented fallback route
             */
            public boolean fallback() {
                return fallback;
            }

            /**
             * Returns the unmodifiable list of upload media-type tags.
             *
             * @return the tags, never {@code null}
             */
            public List<String> uploadable() {
                return uploadable;
            }

            /**
             * Returns the unmodifiable list of download media-type tags.
             *
             * @return the tags, never {@code null}
             */
            public List<String> downloadable() {
                return downloadable;
            }

            /**
             * Returns the unmodifiable list of download bucket indices.
             *
             * @return the indices, never {@code null}
             */
            public List<Integer> downloadBuckets() {
                return downloadBuckets;
            }

            /**
             * Parses a host endpoint from the given {@code <host/>} subtree.
             *
             * <p>The {@code hostname} attribute is mandatory; every other attribute and
             * grandchild is optional and defaults to {@code null}, {@code false}, or the empty
             * list as documented on the respective fields.
             *
             * @implNote
             * This implementation rejects a missing {@code hostname} attribute by letting
             * {@code getRequiredAttributeAsString} throw, mirroring WA Web's
             * {@code attrString("hostname")} contract; a malformed {@code <host/>} subtree
             * therefore surfaces as a parse failure rather than a silently-empty host.
             * Non-integer {@code <download_buckets/>} grandchildren are skipped to mirror WA
             * Web's best-effort {@code parseInt(..., 10)} projection.
             *
             * @param hostStanza the {@code <host/>} subtree
             * @return the parsed host
             * @throws NullPointerException if {@code hostStanza} is {@code null}
             */
            @WhatsAppWebExport(moduleName = "WAMediaConnParser",
                    exports = "mediaConnParser", adaptation = WhatsAppAdaptation.ADAPTED)
            public static Host of(Stanza hostStanza) {
                Objects.requireNonNull(hostStanza, "hostStanza cannot be null");
                var hostname = hostStanza.getRequiredAttributeAsString("hostname");
                var hostClass = hostStanza.getAttributeAsString("class").orElse(null);
                var ip4 = hostStanza.getAttributeAsString("ip4").orElse(null);
                var ip6 = hostStanza.getAttributeAsString("ip6").orElse(null);
                var fallback = "fallback".equals(hostStanza.getAttributeAsString("type").orElse(null));
                var uploadable = collectMediaTypes(hostStanza.getChild("upload").orElse(null));
                var downloadable = collectMediaTypes(hostStanza.getChild("download").orElse(null));
                var bucketsNode = hostStanza.getChild("download_buckets").orElse(null);
                var buckets = new ArrayList<Integer>();
                if (bucketsNode != null) {
                    for (var bucketChild : bucketsNode.children()) {
                        try {
                            buckets.add(Integer.parseInt(bucketChild.description()));
                        } catch (NumberFormatException _) {
                        }
                    }
                }
                return new Host(hostname, hostClass, ip4, ip6, fallback,
                        uploadable, downloadable, buckets);
            }

            /**
             * Collects the media-type tag descriptions from a {@code <upload/>} or
             * {@code <download/>} wrapper subtree.
             *
             * <p>Returns the empty list when the wrapper is {@code null}, deferring the
             * SERVER_MEDIA default semantics to the caller; an empty result from a present
             * wrapper therefore means explicitly no supported types, not all types.
             *
             * @param wrapper the wrapper stanza, may be {@code null}
             * @return the collected tags
             */
            private static List<String> collectMediaTypes(Stanza wrapper) {
                if (wrapper == null) {
                    return List.of();
                }
                var result = new ArrayList<String>();
                for (var child : wrapper.children()) {
                    result.add(child.description());
                }
                return result;
            }

            /**
             * Indicates whether the given object is a {@link Host} with equal field values.
             *
             * @param obj the object to compare against
             * @return {@code true} when {@code obj} is a {@link Host} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Host) obj;
                return this.fallback == that.fallback
                        && Objects.equals(this.hostname, that.hostname)
                        && Objects.equals(this.hostClass, that.hostClass)
                        && Objects.equals(this.ip4, that.ip4)
                        && Objects.equals(this.ip6, that.ip6)
                        && Objects.equals(this.uploadable, that.uploadable)
                        && Objects.equals(this.downloadable, that.downloadable)
                        && Objects.equals(this.downloadBuckets, that.downloadBuckets);
            }

            /**
             * Returns a hash combining every field, consistent with {@link #equals(Object)}.
             *
             * @return the combined hash
             */
            @Override
            public int hashCode() {
                return Objects.hash(hostname, hostClass, ip4, ip6, fallback,
                        uploadable, downloadable, downloadBuckets);
            }

            /**
             * Returns a string rendering every field of this host.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "IqQueryMediaConnsResponse.Success.Host[hostname=" + hostname
                        + ", hostClass=" + hostClass
                        + ", ip4=" + ip4
                        + ", ip6=" + ip6
                        + ", fallback=" + fallback
                        + ", uploadable=" + uploadable
                        + ", downloadable=" + downloadable
                        + ", downloadBuckets=" + downloadBuckets + ']';
            }
        }
    }

    /**
     * Signals that the relay rejected the media-conn query as malformed or unauthorised.
     *
     * <p>This variant maps to the {@code 4xx} class of the media-conn reply pipeline and is a
     * hard failure; the media pipeline surfaces it to the user without retrying.
     */
    @WhatsAppWebModule(moduleName = "WAWebQueryMediaConnsJob")
    final class ClientError implements IqQueryMediaConnsResponse {
        /**
         * Holds the numeric server-side error code from the {@code <error code>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the {@code <error text>} attribute.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text, or {@code null} when omitted
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when
         *         the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>Returns a populated {@link Optional} only when the stanza is a {@code type="error"}
         * envelope echoing the request id and carrying an {@code <error/>} child whose
         * {@code code} attribute falls in the {@code 4xx} range, per the partitioning contract
         * of {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or
         *         {@link Optional#empty()} when the stanza does not match the client-error
         *         schema
         */
        @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob",
                exports = "queryMediaConn", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Indicates whether the given object is a {@link ClientError} with equal field values.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link ClientError} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash combining every field, consistent with {@link #equals(Object)}.
         *
         * @return the combined hash
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a string rendering every field of this reply.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqQueryMediaConnsResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Signals that the relay encountered a transient internal failure while processing the
     * media-conn query.
     *
     * <p>This variant maps to the {@code 5xx} class of the media-conn reply pipeline. Code
     * {@code 507} ("Insufficient Storage") carries a server-supplied backoff hint, and code
     * {@code 503} ("Service Unavailable") is the standard transient failure the media pipeline
     * retries after a short backoff.
     */
    @WhatsAppWebModule(moduleName = "WAWebQueryMediaConnsJob")
    final class ServerError implements IqQueryMediaConnsResponse {
        /**
         * Holds the numeric server-side error code from the {@code <error code>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the {@code <error text>} attribute.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text, or {@code null} when omitted
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when
         *         the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>Returns a populated {@link Optional} only when the stanza is a {@code type="error"}
         * envelope echoing the request id and carrying an {@code <error/>} child whose
         * {@code code} attribute falls outside the {@code 4xx} range, per the partitioning
         * contract of {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or
         *         {@link Optional#empty()} when the stanza does not match the server-error
         *         schema
         */
        @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob",
                exports = "queryMediaConn", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Indicates whether the given object is a {@link ServerError} with equal field values.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link ServerError} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash combining every field, consistent with {@link #equals(Object)}.
         *
         * @return the combined hash
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a string rendering every field of this reply.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqQueryMediaConnsResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

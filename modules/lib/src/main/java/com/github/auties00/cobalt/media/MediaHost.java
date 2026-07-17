package com.github.auties00.cobalt.media;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.media.MediaPath;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;

import java.util.*;

/**
 * A single CDN host entry returned by WhatsApp's {@code media_conn} query.
 *
 * <p>Every host advertises a hostname, the IP addresses the server exposes
 * for it, the media types it accepts for downloads and uploads, and the
 * deterministic download buckets it owns. {@link Primary} hosts may
 * additionally carry a nested fallback hostname with its own IP list, which
 * the retry loop rotates to when the primary endpoint fails;
 * {@link Fallback} hosts are used as alternate endpoints after the primary
 * path is exhausted.
 *
 * <p>Host instances are owned by {@link MediaConnectionService} and selected
 * through {@link #routeSelection} during upload and download calls; the
 * parsed {@link MediaConnectionService} picks a host per attempt.
 */
@WhatsAppWebModule(moduleName = "WAWebMediaHost")
@WhatsAppWebModule(moduleName = "WAWebMediaHostsRouteSelection")
@WhatsAppWebModule(moduleName = "WAWebMediaHostsMaybeSwitchHost")
@WhatsAppWebModule(moduleName = "WABase64Modulo")
public sealed interface MediaHost {

    /**
     * The remaining-bytes threshold below which a mid-transfer host switch
     * is never attempted.
     *
     * <p>Consumed by {@link #maybeSwitchHost} during long-running document
     * uploads and downloads: when fewer than 50 MiB remain the cost of
     * restarting on a new host outweighs any topology benefit, so the
     * current host is kept.
     *
     * @implNote
     * This implementation exposes the constant only so
     * {@link #maybeSwitchHost} can read it; the actual mid-transfer
     * re-query loop that consumes {@link #maybeSwitchHost} is not yet wired
     * in Cobalt.
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHostsMaybeSwitchHost", exports = "THRESHOLD",
            adaptation = WhatsAppAdaptation.DIRECT)
    int SWITCH_HOST_THRESHOLD = 52428800;

    /**
     * The outcome of a {@link #maybeSwitchHost} decision.
     *
     * <p>When {@link #changed} is {@code false} the {@link #host} field
     * echoes the host passed as {@code current} so callers can assign the
     * result unconditionally without a {@code null} check.
     *
     * @param changed whether the host should be switched
     * @param host    the host to use after the decision, never {@code null}
     */
    @WhatsAppWebModule(moduleName = "WAWebMediaHostsMaybeSwitchHost")
    record SwitchHostResult(boolean changed, MediaHost host) {
    }

    /**
     * Decides whether a long-running transfer should rotate to a different
     * host after a fresh {@code media_conn} re-query returned a new route
     * list.
     *
     * <p>Invoked by the periodic poll loop that watches for a CDN topology
     * change mid-upload or mid-download. The decision rules in priority
     * order are:
     * <ul>
     *   <li>If fewer than {@link #SWITCH_HOST_THRESHOLD} bytes remain,
     *       keep the current host.</li>
     *   <li>If the current host is a {@link Primary} and the new route's
     *       selected host differs, switch to the new selected host.</li>
     *   <li>If the current host is a {@link Fallback} and the new route's
     *       selected host differs from the previous selected host, switch
     *       to the new selected host.</li>
     *   <li>If the current host is a {@link Fallback} and the slot it
     *       occupied (either the standalone fallback or the nested
     *       fallback of the selected host) was replaced, switch.</li>
     *   <li>Otherwise keep the current host.</li>
     * </ul>
     *
     * @implNote
     * The fallback-slot branch promotes the caller back to the
     * {@code previousSelected} host (not the new selected), mirroring
     * {@code WAWebMediaHostsMaybeSwitchHost.maybeSwitchHost}: when the
     * fallback slot the caller is occupying has been replaced server-side,
     * the safe move is to climb back to the still-valid prior selected
     * host rather than to jump into a freshly-returned selected host that
     * has not yet been exercised on this transfer.
     *
     * @param current        the host currently in use
     * @param previousRoute  the route in force before the most recent re-query
     * @param newRoute       the route returned by the most recent re-query
     * @param bytesRemaining the number of bytes left to transfer
     * @return the switch decision; {@code host} echoes {@code current} when
     *         {@code changed} is {@code false}
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHostsMaybeSwitchHost", exports = "maybeSwitchHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    static SwitchHostResult maybeSwitchHost(
            MediaHost current,
            RouteSelectionResult previousRoute,
            RouteSelectionResult newRoute,
            long bytesRemaining
    ) {
        var previousFallback = previousRoute.fallbackHost().orElse(null);
        var previousSelected = previousRoute.selectedHost().orElse(null);
        var newFallback = newRoute.fallbackHost().orElse(null);
        var newSelected = newRoute.selectedHost().orElse(null);

        if (bytesRemaining < SWITCH_HOST_THRESHOLD) {
            return new SwitchHostResult(false, current);
        }

        if (current instanceof Primary && !equalsByHostname(current, newSelected)) {
            return new SwitchHostResult(true, newSelected);
        }

        if (current instanceof Fallback && !equalsByHostname(previousSelected, newSelected)) {
            return new SwitchHostResult(true, newSelected);
        }

        if (current instanceof Fallback
                && (isNestedFallbackChanged(current, previousFallback, newFallback)
                        || isNestedFallbackChanged(current, nestedFallbackHost(previousSelected), nestedFallbackHost(newSelected)))) {
            return new SwitchHostResult(true, previousSelected);
        }

        return new SwitchHostResult(false, current);
    }

    /**
     * Tests two hosts for equality by hostname only.
     *
     * <p>Used by {@link #maybeSwitchHost} so that two host entries surfaced
     * by different {@code media_conn} replies with cosmetically different IP
     * orderings or rule encodings still register as the same host. Returns
     * {@code false} if either operand is {@code null}.
     *
     * @param a the first host, may be {@code null}
     * @param b the second host, may be {@code null}
     * @return {@code true} when both hosts are non-{@code null} and share a
     *         hostname
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean equalsByHostname(MediaHost a, MediaHost b) {
        if (a == null || b == null) {
            return false;
        }
        return Objects.equals(a.hostname(), b.hostname());
    }

    /**
     * Tests whether {@code current} occupies a slot whose host has been
     * replaced by the server.
     *
     * <p>Helper for the fallback-slot and nested-fallback-slot branches of
     * {@link #maybeSwitchHost}: returns {@code true} only when
     * {@code current} matches the previous slot host, both slot hosts are
     * non-{@code null}, and the previous and new slot hosts differ.
     *
     * @param current          the host currently in use
     * @param previousSlotHost the host that occupied the slot before the
     *                         re-query
     * @param newSlotHost      the host that occupies the slot after the
     *                         re-query
     * @return {@code true} if the slot's host changed under the current
     *         host
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHostsMaybeSwitchHost", exports = "maybeSwitchHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isNestedFallbackChanged(MediaHost current, MediaHost previousSlotHost, MediaHost newSlotHost) {
        return equalsByHostname(current, previousSlotHost)
                && newSlotHost != null
                && previousSlotHost != null
                && !equalsByHostname(previousSlotHost, newSlotHost);
    }

    /**
     * Resolves the nested fallback host of a primary host as a synthetic
     * {@link Fallback}.
     *
     * <p>Helper for the second nested-fallback branch of
     * {@link #maybeSwitchHost}. Returns {@code null} when {@code primary} is
     * {@code null}, when {@code primary} is not a {@link Primary}, or when
     * the {@link Primary} carries no {@link Primary#fallbackHostname()}.
     *
     * @implNote
     * This implementation synthesises the nested {@link Fallback} lazily
     * on each call instead of materialising it eagerly during
     * {@code media_conn} parsing as WA Web's {@code MediaHost} constructor
     * does through its {@code this.fallback = new MediaHost(...)} branch.
     * The merge mirrors {@code babelHelpers.extends({}, t, t.fallback,
     * {type: "fallback", fallback: undefined})}: hostname, class and IPs
     * are taken from the nested {@code fallback_*} attributes and inherit
     * the parent's values when those attributes are absent; the rules
     * ({@link #downloadBuckets}, {@link #download}, {@link #upload}) are
     * always inherited because the {@code media_conn} stanza never
     * publishes per-fallback rule overrides.
     *
     * @param primary the primary host to inspect, or {@code null}
     * @return the synthesised nested fallback, or {@code null} when none
     *         applies
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHostsMaybeSwitchHost", exports = "maybeSwitchHost",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static MediaHost nestedFallbackHost(MediaHost primary) {
        if (!(primary instanceof Primary p) || p.fallbackHostname().isEmpty()) {
            return null;
        }
        return new Fallback(
                p.fallbackHostname().get(),
                p.fallbackClass().or(p::hostClass),
                p.fallbackIps().isEmpty() ? p.ips() : List.copyOf(p.fallbackIps()),
                p.downloadBuckets(),
                p.download(),
                p.upload()
        );
    }

    /**
     * The two operations that may be performed against a CDN host.
     *
     * <p>Consumed by {@link #routeSelection} to choose between the host's
     * upload accepted-type set and its download accepted-type set;
     * {@link #DOWNLOAD} additionally participates in deterministic
     * bucket-based host selection.
     */
    @WhatsAppWebModule(moduleName = "WAWebMmsOperationsConst")
    @WhatsAppWebModule(moduleName = "WAWebMediaHostsRouteSelection")
    enum Operation {
        /**
         * The upload operation.
         *
         * <p>Route selection picks the first host whose upload media-type
         * set contains the requested type.
         */
        @WhatsAppWebExport(moduleName = "WAWebMmsOperationsConst", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        @WhatsAppWebExport(moduleName = "WAWebMediaHostsRouteSelection", exports = "OPERATIONS",
                adaptation = WhatsAppAdaptation.DIRECT)
        UPLOAD,

        /**
         * The download operation.
         *
         * <p>Route selection applies bucket-based matching when the
         * {@code mms_vcache_aggregation_enabled} AB prop is on, then falls
         * back to a linear scan against the download media-type set.
         */
        @WhatsAppWebExport(moduleName = "WAWebMmsOperationsConst", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        @WhatsAppWebExport(moduleName = "WAWebMediaHostsRouteSelection", exports = "OPERATIONS",
                adaptation = WhatsAppAdaptation.DIRECT)
        DOWNLOAD
    }

    /**
     * The outcome of a single {@link #routeSelection} pass.
     *
     * <p>Carries the best-matching host for the requested operation, the
     * first fallback-class host found in the connection's host list, and
     * the deterministic bucket that the selected host was matched against.
     * {@link #selectedBucket} is present only when the selected host came
     * from the bucket map ({@link Operation#DOWNLOAD} with a hit on the
     * explicit bucket entry or the bucket-zero default); for uploads or for
     * downloads that fell through to the linear scan it is empty.
     *
     * @param selectedHost   the selected host, or empty when none matched
     * @param fallbackHost   the fallback host, or empty when none exists
     * @param selectedBucket the deterministic bucket of the selected host,
     *                       or empty when no bucket applies
     */
    @WhatsAppWebModule(moduleName = "WAWebMediaHostsRouteSelection")
    record RouteSelectionResult(
            Optional<MediaHost> selectedHost,
            Optional<MediaHost> fallbackHost,
            Optional<Integer> selectedBucket
    ) {
    }

    /**
     * Picks the best CDN host and fallback host for the given operation and
     * media type from a parsed host list.
     *
     * <p>Drives the per-attempt host choice inside
     * {@link MediaConnectionService#upload(MediaProvider, MediaPayload)} and
     * {@link MediaConnectionService#download(MediaProvider)}. The selection
     * rules:
     * <ul>
     *   <li>For downloads, try deterministic bucket-based routing first
     *       when {@code vcacheAggregationEnabled} is {@code true}: the
     *       bucket is {@code 0} when {@code encFileHash} is {@code null},
     *       otherwise {@code base64Modulo(encFileHash, maxBuckets) + 100}.
     *       The host that claims the computed bucket is preferred, with
     *       the zero-bucket host as the next try.</li>
     *   <li>If bucket selection finds no candidate or the operation is
     *       {@link Operation#UPLOAD}, fall back to a linear scan for the
     *       first host whose accepted-type set contains the requested
     *       type.</li>
     *   <li>The fallback host is always the first host in the list whose
     *       type is {@link Fallback}, independent of the operation.</li>
     * </ul>
     *
     * @param operation                the operation to perform
     * @param mediaType                the media path to match
     * @param hosts                    the list of available hosts
     * @param encFileHash              the base64-encoded encrypted file
     *                                 hash, or {@code null}
     * @param maxBuckets               the maximum number of buckets, or
     *                                 {@code null} when not available
     * @param vcacheAggregationEnabled whether the
     *                                 {@code mms_vcache_aggregation_enabled}
     *                                 AB prop is enabled
     * @return the route selection result
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHostsRouteSelection", exports = "routeSelection",
            adaptation = WhatsAppAdaptation.DIRECT)
    static RouteSelectionResult routeSelection(
            Operation operation,
            MediaPath mediaType,
            List<? extends MediaHost> hosts,
            String encFileHash,
            Integer maxBuckets,
            boolean vcacheAggregationEnabled
    ) {
        if (hosts.isEmpty()) {
            return new RouteSelectionResult(Optional.empty(), Optional.empty(), Optional.empty());
        }

        MediaHost selected = null;
        Integer selectedBucket = null;
        if (operation == Operation.DOWNLOAD) {
            Integer bucket;
            if (encFileHash == null) {
                bucket = 0;
            } else if (vcacheAggregationEnabled && maxBuckets != null) {
                bucket = base64Modulo(encFileHash, maxBuckets) + 100;
            } else {
                bucket = null;
            }

            var bucketMap = buildBucketMap(hosts);
            var bucketHost = bucket != null ? bucketMap.get(bucket) : null;
            var defaultHost = bucketMap.get(0);

            if (bucketHost != null && supportsDownloadMediaType(bucketHost, mediaType)) {
                selected = bucketHost;
            } else if (defaultHost != null && supportsDownloadMediaType(defaultHost, mediaType)) {
                selected = defaultHost;
            }

            if (selected != null) {
                selectedBucket = bucket;
            }
        }

        MediaHost fallback = null;
        for (var host : hosts) {
            if (host instanceof Fallback) {
                fallback = host;
                break;
            }
        }

        if (selected == null) {
            for (var host : hosts) {
                if (operation == Operation.UPLOAD
                        ? supportsUploadMediaType(host, mediaType)
                        : supportsDownloadMediaType(host, mediaType)) {
                    selected = host;
                    break;
                }
            }
        }

        return new RouteSelectionResult(
                Optional.ofNullable(selected),
                Optional.ofNullable(fallback),
                Optional.ofNullable(selectedBucket)
        );
    }

    /**
     * Tests whether {@code host} can serve a download for the specified
     * media type.
     *
     * <p>Helper for the linear-scan branch of {@link #routeSelection}. The
     * media type is normalised through {@link #normalizeDownloadMediaType}
     * (PTV and newsletter-PTV collapse onto video, product onto image)
     * before the lookup.
     *
     * @param host      the host to check
     * @param mediaType the media type to check
     * @return {@code true} if the normalised type is in the host's
     *         download set
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean supportsDownloadMediaType(MediaHost host, MediaPath mediaType) {
        return host.download().contains(normalizeDownloadMediaType(mediaType));
    }

    /**
     * Tests whether {@code host} can accept an upload for the specified
     * media type.
     *
     * <p>Helper for the linear-scan branch of {@link #routeSelection}. The
     * media type is normalised through {@link #normalizeUploadMediaType}
     * (PTV collapses onto video, product-catalog-image onto product) before
     * the lookup.
     *
     * @param host      the host to check
     * @param mediaType the media type to check
     * @return {@code true} if the normalised type is in the host's
     *         upload set
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean supportsUploadMediaType(MediaHost host, MediaPath mediaType) {
        return host.upload().contains(normalizeUploadMediaType(mediaType));
    }

    /**
     * Builds a bucket-to-host map from the host list.
     *
     * <p>Helper for the deterministic bucket-routing branch of
     * {@link #routeSelection}.
     *
     * @implNote
     * This implementation lets the last writer win when multiple hosts
     * claim the same bucket, matching WA Web's
     * {@code Map#set}-on-iteration semantics in
     * {@code WAWebMediaHostsRouteSelection}.
     *
     * @param hosts the list of hosts to index
     * @return a map from bucket number to host
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHostsRouteSelection", exports = "routeSelection",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static Map<Integer, MediaHost> buildBucketMap(List<? extends MediaHost> hosts) {
        var map = new HashMap<Integer, MediaHost>();
        for (var host : hosts) {
            for (var bucket : host.downloadBuckets()) {
                map.put(bucket, host);
            }
        }
        return map;
    }

    /**
     * Computes the modulo of a base64-encoded string treated as a
     * big-endian byte stream.
     *
     * <p>Consumed by {@link #routeSelection} to map the encrypted file hash
     * to a deterministic download bucket. {@code divisor} is the server's
     * {@code maxBuckets} value.
     *
     * @implNote
     * This implementation decodes the input, then folds each byte as two
     * 4-bit nibbles via
     * {@code remainder = ((remainder << 4) + nibble) % divisor}, matching
     * the bit-equivalent algorithm in WA Web's {@code WABase64Modulo}.
     *
     * @param base64  the base64-encoded string
     * @param divisor the modulo divisor (typically {@code maxBuckets})
     * @return the modulo result
     */
    @WhatsAppWebExport(moduleName = "WABase64Modulo", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static int base64Modulo(String base64, int divisor) {
        var decoded = Base64.getDecoder().decode(base64);
        var remainder = 0;
        for (var b : decoded) {
            var unsigned = b & 0xFF;
            var high = unsigned >> 4;
            var low = unsigned & 0x0F;
            remainder = ((remainder << 4) + high) % divisor;
            remainder = ((remainder << 4) + low) % divisor;
        }
        return remainder;
    }

    /**
     * Returns the hostname of this CDN host.
     *
     * @return the hostname
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    String hostname();

    /**
     * Returns the optional {@code class} attribute advertised by the server
     * for this host.
     *
     * <p>The {@code class} attribute groups hosts by deployment tier (for
     * example {@code mms} versus {@code mmg}) and is propagated unchanged to
     * downstream analytics and request tagging.
     *
     * @return an {@link Optional} holding the host class, or empty if the
     *         attribute is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    Optional<String> hostClass();

    /**
     * Returns the IP addresses the server advertises for this host.
     *
     * <p>Surfaces both the {@code ip4} and {@code ip6} {@code media_conn}
     * attributes when present, in that order. Currently unused by Cobalt's
     * download path, which lets the JVM resolve the hostname; exposed for
     * callers that want to pin a CDN IP.
     *
     * @return an unmodifiable list of IP address strings
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    List<String> ips();

    /**
     * Returns the set of media types this host accepts for downloads.
     *
     * <p>Defaults to the full set of routable server media types (with the
     * non-CDN-routed types stripped) when the {@code media_conn} response
     * omits the explicit rules.
     *
     * @return an unmodifiable set of supported download media paths
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    Set<MediaPath> download();

    /**
     * Returns the set of media types this host accepts for uploads.
     *
     * <p>Defaults to the full set of routable server media types (with the
     * non-CDN-routed types stripped) when the {@code media_conn} response
     * omits the explicit rules.
     *
     * @return an unmodifiable set of supported upload media paths
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    Set<MediaPath> upload();

    /**
     * Returns the deterministic download bucket identifiers owned by this
     * host.
     *
     * <p>Buckets participate in download host selection through
     * {@link #routeSelection}: the encrypted file hash is reduced modulo
     * {@code maxBuckets} to pick the bucket, then the owning host is looked
     * up in the bucket map.
     *
     * @return an unmodifiable list of bucket numbers
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    List<Integer> downloadBuckets();

    /**
     * Returns the nested fallback hostname declared by this host, if any.
     *
     * <p>Only {@link Primary} hosts can advertise a nested fallback;
     * {@link Fallback} hosts always return an empty optional. The nested
     * fallback is rotated to by
     * {@link LiveMediaConnectionService#selectHost(MediaHost, MediaHost, String, int, boolean)}
     * when the previous attempt against the selected host failed.
     *
     * @return an {@link Optional} holding the fallback hostname
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    Optional<String> fallbackHostname();

    /**
     * Tests whether this host accepts a download of the media produced by
     * the given provider.
     *
     * <p>The provider's media path is normalised (PTV variants collapse
     * onto video, product onto image) before consulting the host's
     * accepted-type set.
     *
     * @param provider the media provider whose type to check
     * @return {@code true} if this host supports the download
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    boolean canDownload(MediaProvider provider);

    /**
     * Tests whether this host accepts an upload of the media produced by
     * the given provider.
     *
     * <p>The provider's media path is normalised (PTV collapses onto video,
     * product-catalog-image onto product) before consulting the host's
     * accepted-type set.
     *
     * @param provider the media provider whose type to check
     * @return {@code true} if this host supports the upload
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    boolean canUpload(MediaProvider provider);

    /**
     * Normalises a media path for the download accepted-type check.
     *
     * <p>Folds the type aliases that share a host's download whitelist:
     * {@link MediaPath#PTV} and {@link MediaPath#NEWSLETTER_PTV} collapse to
     * {@link MediaPath#VIDEO}, {@link MediaPath#PRODUCT} collapses to
     * {@link MediaPath#IMAGE}, and every other type is returned unchanged.
     *
     * @param path the media path to normalise
     * @return the normalised media path
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static MediaPath normalizeDownloadMediaType(MediaPath path) {
        return switch (path) {
            case PTV, NEWSLETTER_PTV -> MediaPath.VIDEO;
            case PRODUCT -> MediaPath.IMAGE;
            default -> path;
        };
    }

    /**
     * Normalises a media path for the upload accepted-type check.
     *
     * <p>Folds the type aliases that share a host's upload whitelist:
     * {@link MediaPath#PTV} collapses to {@link MediaPath#VIDEO},
     * {@link MediaPath#PRODUCT_CATALOG_IMAGE} collapses to
     * {@link MediaPath#PRODUCT}, and every other type is returned unchanged.
     *
     * @param path the media path to normalise
     * @return the normalised media path
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static MediaPath normalizeUploadMediaType(MediaPath path) {
        return switch (path) {
            case PTV -> MediaPath.VIDEO;
            case PRODUCT_CATALOG_IMAGE -> MediaPath.PRODUCT;
            default -> path;
        };
    }

    /**
     * A primary CDN host.
     *
     * <p>Primary hosts are the preferred endpoints picked by
     * {@link #routeSelection}. They may advertise a nested fallback hostname
     * with its own IP list, which the retry loop in
     * {@link LiveMediaConnectionService#selectHost(MediaHost, MediaHost, String, int, boolean)}
     * rotates to before falling back to a fallback-class host.
     *
     * @param hostname         the hostname of this host
     * @param hostClass        the optional {@code class} attribute
     *                         advertised by the server
     * @param ips              the IP addresses advertised by the server
     * @param fallbackHostname the nested fallback hostname, or empty
     * @param fallbackClass    the optional {@code fallback_class}
     *                         attribute advertised alongside the nested
     *                         fallback
     * @param fallbackIps      the nested fallback IP addresses
     * @param downloadBuckets  the deterministic download buckets owned by
     *                         this host
     * @param download         the supported download media types
     * @param upload           the supported upload media types
     */
    @WhatsAppWebModule(moduleName = "WAWebMediaHost")
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "HOST_TYPE",
            adaptation = WhatsAppAdaptation.ADAPTED)
    record Primary(
            String hostname,
            Optional<String> hostClass,
            List<String> ips,
            Optional<String> fallbackHostname,
            Optional<String> fallbackClass,
            List<String> fallbackIps,
            List<Integer> downloadBuckets,
            Set<MediaPath> download,
            Set<MediaPath> upload
    ) implements MediaHost {

        /**
         * {@inheritDoc}
         *
         * @throws NullPointerException if {@code provider} is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public boolean canDownload(MediaProvider provider) {
            Objects.requireNonNull(provider, "provider cannot be null");
            return download.contains(normalizeDownloadMediaType(provider.mediaPath()));
        }

        /**
         * {@inheritDoc}
         *
         * @throws NullPointerException if {@code provider} is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public boolean canUpload(MediaProvider provider) {
            Objects.requireNonNull(provider, "provider cannot be null");
            return upload.contains(normalizeUploadMediaType(provider.mediaPath()));
        }
    }

    /**
     * A fallback-class CDN host.
     *
     * <p>Fallback hosts are used as alternate endpoints when the primary
     * host has exhausted its retry budget. They never carry a nested
     * fallback of their own.
     *
     * @param hostname        the hostname of this host
     * @param hostClass       the optional {@code class} attribute
     *                        advertised by the server
     * @param ips             the IP addresses advertised by the server
     * @param downloadBuckets the deterministic download buckets owned by
     *                        this host
     * @param download        the supported download media types
     * @param upload          the supported upload media types
     */
    @WhatsAppWebModule(moduleName = "WAWebMediaHost")
    @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "HOST_TYPE",
            adaptation = WhatsAppAdaptation.ADAPTED)
    record Fallback(
            String hostname,
            Optional<String> hostClass,
            List<String> ips,
            List<Integer> downloadBuckets,
            Set<MediaPath> download,
            Set<MediaPath> upload
    ) implements MediaHost {

        /**
         * {@inheritDoc}
         *
         * @throws NullPointerException if {@code provider} is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public boolean canDownload(MediaProvider provider) {
            Objects.requireNonNull(provider, "provider cannot be null");
            return download.contains(normalizeDownloadMediaType(provider.mediaPath()));
        }

        /**
         * {@inheritDoc}
         *
         * @throws NullPointerException if {@code provider} is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public boolean canUpload(MediaProvider provider) {
            Objects.requireNonNull(provider, "provider cannot be null");
            return upload.contains(normalizeUploadMediaType(provider.mediaPath()));
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns an empty optional because
         * fallback-class hosts never advertise a nested fallback hostname
         * of their own.
         */
        @WhatsAppWebExport(moduleName = "WAWebMediaHost", exports = "MediaHost",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public Optional<String> fallbackHostname() {
            return Optional.empty();
        }
    }
}

package com.github.auties00.cobalt.media;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.media.MediaHost.Operation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.media.ExternalBlobReference;
import com.github.auties00.cobalt.wire.linked.media.MediaPath;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.util.MediaMetricUtils;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.MediaDownload2EventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MediaUpload2EventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StickerErrorEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcMediaRmrEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ConnectionType;
import com.github.auties00.cobalt.wire.wam.type.DownloadOriginType;
import com.github.auties00.cobalt.wire.wam.type.HttpProtocolVersionType;
import com.github.auties00.cobalt.wire.wam.type.MediaDownloadModeType;
import com.github.auties00.cobalt.wire.wam.type.MediaDownloadResultType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MediaUploadModeType;
import com.github.auties00.cobalt.wire.wam.type.MediaUploadResultType;
import com.github.auties00.cobalt.wire.wam.type.OverallMediaKeyReuseType;
import com.github.auties00.cobalt.wire.wam.type.StickerErrorType;
import com.github.auties00.cobalt.wire.wam.type.UploadOriginType;
import com.github.auties00.cobalt.wire.wam.type.UploadSourceType;
import com.github.auties00.cobalt.wire.wam.type.WebcChatType;
import com.github.auties00.cobalt.wire.wam.type.WebcRmrReasonCode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * Default implementation of {@link MediaConnectionService} backed by the
 * live WhatsApp CDN.
 *
 * <p>Holds the parsed {@code media_conn} credentials (authentication
 * token, the list of candidate CDN hosts with their accepted media types
 * and download buckets, the time-to-live values, and the retry budgets) in
 * a volatile snapshot that {@link #update(Stanza)} atomically replaces on
 * every refresh. Uploads and downloads run two-pass streaming AES-CBC plus
 * HMAC pipelines that never land ciphertext on disk, rotate across
 * candidate hosts on retryable failures, and block on a first-refresh
 * latch until the initial {@code media_conn} reply has landed.
 *
 * @implNote
 * This implementation collapses WA Web's family of MMS modules
 * ({@code WAMediaConnParser}, {@code WAWebQueryMediaConnsJob},
 * {@code WAWebMediaHosts}, {@code WAWebMmsClient} and its helpers) into a
 * single service. The volatile snapshot fields are published without
 * locking; a concurrent {@link #update(Stanza)} becomes visible to every
 * reader on its next field read.
 */
@WhatsAppWebModule(moduleName = "WAMediaConnParser")
@WhatsAppWebModule(moduleName = "WAServerMediaType")
@WhatsAppWebModule(moduleName = "WAWebQueryMediaConnsJob")
@WhatsAppWebModule(moduleName = "WAWebMediaHosts")
@WhatsAppWebModule(moduleName = "WAWebMmsClient")
@WhatsAppWebModule(moduleName = "WAWebMmsClientSelectHost")
@WhatsAppWebModule(moduleName = "WAWebMmsClientMmsUpload")
@WhatsAppWebModule(moduleName = "WAWebMmsClientMmsDownload")
@WhatsAppWebModule(moduleName = "WAWebMmsClientFormatUploadUrl")
@WhatsAppWebModule(moduleName = "WAWebMmsClientFormatDownloadUrl")
@WhatsAppWebModule(moduleName = "WAWebMmsClientFormatHashUrl")
@WhatsAppWebModule(moduleName = "WAWebMmsClientIsErrorRetryable")
@WhatsAppWebModule(moduleName = "WAWebMmsClientMmsBackoffOptions")
@WhatsAppWebModule(moduleName = "WAWebMmsCdnUrlValidationUtils")
@WhatsAppWebModule(moduleName = "WAWebMmsClientMmsDeleteMdHistorySyncBlob")
@WhatsAppWebModule(moduleName = "WABase64UrlSafe")
@WhatsAppWebModule(moduleName = "WAWebWamMediaMetricUtils")
@WhatsAppWebModule(moduleName = "WAWebDownloadManager")
@WhatsAppWebModule(moduleName = "WAWebMediaMmsV4Upload")
@WhatsAppWebModule(moduleName = "WAWebMediaMmsV4Download")
@WhatsAppWebModule(moduleName = "WAWebCreateMediaUploadMetrics")
@WhatsAppWebModule(moduleName = "WAWebCreateMediaDownloadMetrics")
@WhatsAppWebModule(moduleName = "WAWebMediaUpload2WamEvent")
@WhatsAppWebModule(moduleName = "WAWebMediaDownload2WamEvent")
@WhatsAppWebModule(moduleName = "WAWebWebcMediaRmrWamEvent")
@WhatsAppWebModule(moduleName = "WAWebStickerErrorWamEvent")
public final class LiveMediaConnectionService implements MediaConnectionService {
    /** The logger for {@link LiveMediaConnectionService}. */
    private static final System.Logger LOGGER = Log.get(LiveMediaConnectionService.class);

    /**
     * The {@link ABPropsService} consulted when assembling CDN download URLs
     * and deciding whether the hash-based download fallback is still
     * permitted.
     *
     * <p>Injected through the constructor so that
     * {@link #download(MediaProvider)}, {@link #checkExistence}, and
     * {@link #getEncryptedMediaSize} do not have to thread it through every
     * call.
     */
    private final ABPropsService abPropsService;

    /**
     * The {@link WamService} that batches and uploads the media-transfer
     * telemetry beacons emitted from the upload and download paths.
     *
     * <p>Injected through the constructor so
     * {@link #upload(MediaProvider, MediaPayload)} can commit
     * {@code MediaUpload2} beacons and {@link #download(MediaProvider)} can
     * commit {@code WebcMediaRmr} and {@code StickerError} beacons without
     * threading the service through every call.
     */
    private final WamService wamService;

    /**
     * Signals when the first {@link #update(Stanza)} has landed.
     *
     * <p>{@link #upload(MediaProvider, MediaPayload)},
     * {@link #download(MediaProvider)}, and the history-sync delete path
     * block on this latch so callers that fire before the first
     * {@code media_conn} refresh do not race ahead with an unpublished
     * field set. Subsequent updates release the latch a second time, which
     * is a no-op.
     */
    private final CountDownLatch initialized = new CountDownLatch(1);

    /**
     * The authentication token presented to the CDN on every upload and
     * download request.
     *
     * <p>Without a valid token the CDN refuses the request with HTTP 401;
     * the upload and download retry loop classifies that as a retryable
     * error.
     *
     * @implNote
     * This field is {@code volatile} so a concurrent {@link #update(Stanza)}
     * publishes the new token to every reader without locking.
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private volatile String auth;

    /**
     * The routes time-to-live in seconds.
     *
     * <p>After this interval the CDN host list may no longer be current and
     * the caller should re-query a fresh media connection;
     * {@link #needsRefresh()} reports {@code true} once the TTL has elapsed.
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob", exports = "queryMediaConn",
            adaptation = WhatsAppAdaptation.DIRECT)
    private volatile int ttl;

    /**
     * The authentication token time-to-live in seconds.
     *
     * <p>Once the auth TTL elapses the CDN refuses requests made with the
     * stored token; {@link #isExpired()} reports {@code true} past the
     * deadline.
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob", exports = "queryMediaConn",
            adaptation = WhatsAppAdaptation.DIRECT)
    private volatile int authTtl;

    /**
     * The maximum number of deterministic download buckets advertised by
     * the server.
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob", exports = "mapParsedMediaConn",
            adaptation = WhatsAppAdaptation.DIRECT)
    private volatile int maxBuckets;

    /**
     * The maximum number of retry attempts the UI should offer when the
     * user manually re-triggers a failed media download.
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private volatile int maxManualRetry;

    /**
     * The maximum number of retry attempts the client should perform when
     * transparently re-downloading a piece of media after a failure.
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private volatile int maxAutoDownloadRetry;

    /**
     * The epoch-second timestamp at which the current credentials were
     * parsed.
     *
     * <p>Combined with {@link #ttl} and {@link #authTtl} to compute the
     * absolute expiry deadlines used by {@link #isExpired()} and
     * {@link #needsRefresh()}.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob", exports = "queryMediaConn",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private volatile long timestamp;

    /**
     * The ordered list of CDN host candidates for uploads and downloads.
     *
     * <p>Each entry advertises a hostname, a set of supported media types,
     * and (for {@link MediaHost.Primary}) a nested fallback hostname.
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob", exports = "mapParsedMediaConn",
            adaptation = WhatsAppAdaptation.DIRECT)
    private volatile SequencedCollection<? extends MediaHost> hosts;

    /**
     * Constructs a fresh media-connection singleton bound to
     * {@code abPropsService}.
     *
     * <p>One instance is constructed per session and dependency-injected
     * into every component that needs CDN access (transcoder text pipeline,
     * sync exchange, stream-control success handler) plus the client
     * itself. The connection is unusable for uploads or downloads until the
     * first {@link #update(Stanza)} lands, at which point any waiting calls
     * unblock.
     *
     * @param abPropsService the AB-props service threaded into the download
     *                       URL formatter
     * @param wamService     the WAM telemetry service the upload and download
     *                       paths commit media-transfer beacons to
     * @throws NullPointerException if {@code abPropsService} or
     *                              {@code wamService} is {@code null}
     */
    public LiveMediaConnectionService(ABPropsService abPropsService, WamService wamService) {
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
    }

    /**
     * Atomically replaces this service's snapshot with the credentials and
     * host list parsed from {@code response} and releases any upload or
     * download callers blocked on the first-refresh latch.
     *
     * <p>Called by the success-stream handler each time the periodic
     * {@code media_conn} IQ reply lands. Safe to call from any thread; the
     * volatile write publishes the new snapshot to every concurrent reader.
     * A subsequent {@link #upload(MediaProvider, MediaPayload)} or
     * {@link #download(MediaProvider)} call that captured the previous
     * snapshot before the swap keeps using that snapshot for the duration
     * of the operation; new callers see the fresh snapshot.
     *
     * @implNote
     * This implementation collapses three WA Web helpers into a single
     * pass: raw attribute decoding ({@code WAMediaConnParser}), host and
     * field projection ({@code WAWebQueryMediaConnsJob.mapParsedMediaConn}),
     * and the TTL normalisation that subtracts the current time from the
     * server's future expiry timestamps. The snapshot's {@code timestamp}
     * is set to the current epoch second so {@link #isExpired()} and
     * {@link #needsRefresh()} have a stable origin.
     *
     * @param response the {@code media_conn} IQ response stanza
     * @throws NoSuchElementException   if {@code response} is missing the
     *                                  {@code media_conn} child or one of
     *                                  the mandatory attributes
     * @throws IllegalArgumentException if a mandatory integer attribute
     *                                  cannot be parsed as an {@code int}
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob",
            exports = {"mapParsedMediaConn", "queryMediaConn"},
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void update(Stanza response) {
        var mediaConn = response.getRequiredChild("media_conn");

        var parsedAuth = mediaConn.getRequiredAttributeAsString("auth");
        var parsedTtl = mediaConn.getRequiredAttributeAsInt("ttl");
        var parsedAuthTtl = mediaConn.getRequiredAttributeAsInt("auth_ttl");
        var parsedMaxBuckets = mediaConn.getRequiredAttributeAsInt("max_buckets");

        var parsedMaxManualRetry = clampOptionalInt(
                mediaConn.getAttributeAsInt("max_manual_retry", (Integer) null),
                0, 4, 3
        );

        var parsedMaxAutoDownloadRetry = clampOptionalInt(
                mediaConn.getAttributeAsInt("max_auto_download_retry", (Integer) null),
                0, 4, 3
        );

        var parsedHosts = mediaConn.streamChildren("host")
                .map(LiveMediaConnectionService::parseHost)
                .toList();

        var parsedTimestamp = Instant.now().getEpochSecond();

        this.auth = parsedAuth;
        this.ttl = parsedTtl;
        this.authTtl = parsedAuthTtl;
        this.maxBuckets = parsedMaxBuckets;
        this.maxManualRetry = parsedMaxManualRetry;
        this.maxAutoDownloadRetry = parsedMaxAutoDownloadRetry;
        this.timestamp = parsedTimestamp;
        this.hosts = parsedHosts;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "media_conn updated: hosts={0} ttl={1}s authTtl={2}s maxBuckets={3}",
                    parsedHosts.size(), parsedTtl, parsedAuthTtl, parsedMaxBuckets);
        }
        initialized.countDown();
    }

    /**
     * Blocks the calling thread until the first {@link #update(Stanza)} has
     * landed.
     *
     * <p>Called by {@link #upload(MediaProvider, MediaPayload)},
     * {@link #download(MediaProvider)} on
     * entry so callers do not race ahead with an unpublished field set.
     *
     * @throws InterruptedException if the calling thread is interrupted
     *                              while waiting
     */
    private void awaitInitialized() throws InterruptedException {
        initialized.await();
    }

    /**
     * Throws {@link IllegalStateException} when the first
     * {@link #update(Stanza)} has not yet landed.
     *
     * @throws IllegalStateException if no snapshot has been published
     */
    private void requireInitialized() {
        if (initialized.getCount() != 0L) {
            throw new IllegalStateException(
                    "MediaConnectionService has not yet received a media_conn refresh");
        }
    }

    /**
     * Parses a single {@code host} child stanza into a {@link MediaHost}
     * record.
     *
     * <p>Classifies the host as {@link MediaHost.Fallback} when its
     * {@code type} attribute equals {@code "fallback"}, otherwise as
     * {@link MediaHost.Primary}. Primary hosts may carry the optional
     * {@code fallback_hostname}, {@code fallback_class},
     * {@code fallback_ip4}, and {@code fallback_ip6} attributes describing a
     * nested fallback endpoint.
     *
     * @param hostStanza the host child stanza
     * @return the parsed media host
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob", exports = "mapParsedMediaConn",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static MediaHost parseHost(Stanza hostStanza) {
        var hostname = hostStanza.getRequiredAttributeAsString("hostname");
        var hostClass = hostStanza.getAttributeAsString("class");

        var ips = new ArrayList<String>();
        hostStanza.getAttributeAsString("ip4").ifPresent(ips::add);
        hostStanza.getAttributeAsString("ip6").ifPresent(ips::add);

        var downloadTypes = parseMediaTypes(hostStanza, "download");
        var uploadTypes = parseMediaTypes(hostStanza, "upload");

        var downloadBuckets = hostStanza.getChild("download_buckets")
                .map(bucketsNode -> bucketsNode.streamChildren()
                        .map(Stanza::description)
                        .map(tag -> {
                            try {
                                return Integer.parseInt(tag);
                            } catch (NumberFormatException _) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList())
                .orElse(List.of());

        var isFallback = hostStanza.getAttributeAsString("type")
                .map("fallback"::equals)
                .orElse(false);

        if (isFallback) {
            return new MediaHost.Fallback(
                    hostname,
                    hostClass,
                    List.copyOf(ips),
                    downloadBuckets,
                    downloadTypes,
                    uploadTypes
            );
        } else {
            var fallbackHostname = hostStanza.getAttributeAsString("fallback_hostname");
            var fallbackClass = hostStanza.getAttributeAsString("fallback_class");
            var fallbackIps = new ArrayList<String>();
            hostStanza.getAttributeAsString("fallback_ip4").ifPresent(fallbackIps::add);
            hostStanza.getAttributeAsString("fallback_ip6").ifPresent(fallbackIps::add);

            return new MediaHost.Primary(
                    hostname,
                    hostClass,
                    List.copyOf(ips),
                    fallbackHostname,
                    fallbackClass,
                    List.copyOf(fallbackIps),
                    downloadBuckets,
                    downloadTypes,
                    uploadTypes
            );
        }
    }

    /**
     * Parses the media-type whitelist declared under a host's
     * {@code download} or {@code upload} child stanza.
     *
     * <p>Each child tag is mapped to a {@link MediaPath} constant. When the
     * child stanza is absent the whitelist defaults to every known server
     * media type, then a small set of types that never appear in CDN
     * routing ({@code kyc-id}, the novi placeholders, {@code thumbnail-gif},
     * and {@code xma-image}) is filtered out.
     *
     * @param hostStanza    the host stanza
     * @param description the child stanza description, either
     *                    {@code "download"} or {@code "upload"}
     * @return the set of supported media paths
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob", exports = "mapParsedMediaConn",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAServerMediaType", exports = "castToServerMediaType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAServerMediaType", exports = "SERVER_MEDIA",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static Set<MediaPath> parseMediaTypes(Stanza hostStanza, String description) {
        return hostStanza.getChild(description)
                .map(typesNode -> {
                    var result = new LinkedHashSet<MediaPath>();
                    typesNode.streamChildren()
                            .map(Stanza::description)
                            .forEach(tag -> MediaPath.ofId(tag).ifPresent(result::add));

                    result.remove(MediaPath.KYC_ID);
                    result.remove(MediaPath.NOVI_IMAGE);
                    result.remove(MediaPath.NOVI_VIDEO);
                    result.remove(MediaPath.THUMBNAIL_GIF);
                    result.remove(MediaPath.XMA_IMAGE);
                    return Collections.unmodifiableSet(result);
                })
                .orElseGet(() -> {
                    var allTypes = new LinkedHashSet<>(MediaPath.known());
                    allTypes.remove(MediaPath.KYC_ID);
                    allTypes.remove(MediaPath.NOVI_IMAGE);
                    allTypes.remove(MediaPath.NOVI_VIDEO);
                    allTypes.remove(MediaPath.THUMBNAIL_GIF);
                    allTypes.remove(MediaPath.XMA_IMAGE);
                    return Collections.unmodifiableSet(allTypes);
                });
    }

    /**
     * Clamps an optional integer to the given inclusive range, returning a
     * default when the input is {@code null} or out of range.
     *
     * <p>Helper for {@link #update(Stanza)} that decodes the server's
     * {@code max_manual_retry} and {@code max_auto_download_retry}
     * attributes, both of which are bounded to {@code [0, 4]} with a default
     * of {@code 3}.
     *
     * @param value        the parsed integer value, or {@code null}
     * @param min          the minimum allowed value
     * @param max          the maximum allowed value
     * @param defaultValue the default value when input is {@code null} or
     *                     out of range
     * @return the clamped value or the default
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static int clampOptionalInt(Integer value, int min, int max, int defaultValue) {
        if (value == null || value < min || value > max) {
            return defaultValue;
        }
        return value;
    }

    /**
     * The maximum number of retry attempts for upload and download
     * operations.
     *
     * @implNote
     * This implementation combines WA Web's module-level {@code 4} constant
     * with the {@code retries: 3} backoff option: 3 retries plus the initial
     * attempt give 4 total attempts indexed {@code 0..MAX_ATTEMPT_COUNT-1}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientSelectHost", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebMmsClientMmsBackoffOptions", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int MAX_ATTEMPT_COUNT = 4;

    /**
     * The minimum backoff timeout in milliseconds applied between
     * consecutive retry attempts of a media upload or download.
     *
     * @implNote
     * With a factor of {@code 2} and jitter disabled the schedule is
     * deterministic: attempts {@code 1, 2, 3} are preceded by sleeps of
     * {@code 1000ms, 2000ms, 4000ms}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientMmsBackoffOptions", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final long MIN_BACKOFF_TIMEOUT_MILLIS = 1000L;

    /**
     * Sleeps the caller's virtual thread for the exponential-backoff delay
     * associated with the given retry attempt number.
     *
     * <p>Helper for the upload and download retry loops; the delay for
     * attempt {@code n} (zero-based) is
     * {@code MIN_BACKOFF_TIMEOUT_MILLIS * 2^n}, producing the sequence
     * {@code 1000ms, 2000ms, 4000ms} before attempts {@code 1, 2, 3}. No
     * sleep is performed for attempt {@code 0}.
     *
     * @implNote
     * This implementation collapses WA Web's
     * {@code WAExponentialBackoff.exponentialBackoff} into a plain
     * blocking {@link Thread#sleep(long)} on a virtual thread; on
     * interruption the interrupt flag is restored and the method returns
     * immediately so the retry loop can observe cancellation.
     *
     * @param attemptCount the zero-based attempt index that just failed
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientMmsBackoffOptions", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static void sleepForBackoff(int attemptCount) {
        if (attemptCount < 0) {
            return;
        }
        var delayMillis = MIN_BACKOFF_TIMEOUT_MILLIS << attemptCount;
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "backoff sleep: attempt={0} delayMillis={1}", attemptCount, delayMillis);
        }
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Picks the CDN hostname to use for the current retry attempt of an
     * upload or download.
     *
     * <p>The rotation strategy, applied in priority order:
     * <ul>
     *   <li>If the previous attempt made progress, reuse the same
     *       host.</li>
     *   <li>On attempts {@code 0} and {@code 1}, stick with the initially
     *       selected host.</li>
     *   <li>On the last attempt, prefer the fallback-class host when one
     *       exists.</li>
     *   <li>If the previous attempt was the selected host and that host
     *       advertises a nested fallback hostname, rotate to it.</li>
     *   <li>Otherwise prefer the fallback host, falling back to the
     *       originally selected host.</li>
     * </ul>
     *
     * @param selectedHost          the host chosen by route selection, or
     *                              {@code null} if none matched
     * @param fallbackHost          the fallback-type host, or {@code null}
     *                              if none exists
     * @param lastHostUsed          the hostname used on the previous
     *                              attempt, or {@code null} on the first
     *                              attempt
     * @param attemptCount          the zero-based attempt number
     * @param lastFetchMadeProgress whether the previous attempt
     *                              transferred any data
     * @return the hostname to use, or {@code null} if no host is available
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientSelectHost", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    static String selectHost(
            MediaHost selectedHost,
            MediaHost fallbackHost,
            String lastHostUsed,
            int attemptCount,
            boolean lastFetchMadeProgress
    ) {
        var selectedHostname = selectedHost != null ? selectedHost.hostname() : null;
        var fallbackHostname = fallbackHost != null ? fallbackHost.hostname() : null;

        if (lastFetchMadeProgress && lastHostUsed != null) {
            return lastHostUsed;
        }

        if (attemptCount <= 1) {
            return selectedHostname;
        }

        if (attemptCount == MAX_ATTEMPT_COUNT - 1 && fallbackHostname != null) {
            return fallbackHostname;
        }

        if (lastHostUsed != null
                && lastHostUsed.equals(selectedHostname)
                && selectedHost != null
                && selectedHost.fallbackHostname().isPresent()) {
            return selectedHost.fallbackHostname().get();
        }

        return fallbackHostname != null ? fallbackHostname : selectedHostname;
    }

    /**
     * Uploads a media payload to WhatsApp's CDN on behalf of the given
     * provider.
     *
     * <p>High-level entry point for any code that ships a media-bearing
     * message: choose the appropriate {@link MediaProvider} subtype, then
     * call this method with a transcoded {@link MediaPayload}. On success
     * the provider's media metadata (plaintext and encrypted SHA-256
     * hashes, media key, direct path, URL, byte size, and key timestamp) is
     * written back through the provider setters so the caller can build the
     * outgoing message protobuf; when the provider is an
     * {@link ExternalBlobReference} the server-returned handle is stored
     * too. The {@code payload} is not closed by this method; the caller
     * wraps the call in a try-with-resources so any temp file owned by the
     * payload is released.
     *
     * @implNote
     * This implementation runs a two-pass streaming-encrypt pipeline that
     * never lands the encrypted payload on disk. Pass 1 opens
     * {@link MediaPayload#openPlaintext()}, threads it through
     * {@link MediaUploadInputStream} (AES-CBC + HMAC), and drains the
     * encrypted output to {@link OutputStream#nullOutputStream()} while
     * accumulating {@code fileSha256}, {@code fileEncSha256},
     * {@code fileLength}, and the random {@code mediaKey} that the
     * recipient needs. Pass 2 builds a known-length
     * {@link HttpRequest.BodyPublisher} that re-opens the plaintext,
     * re-encrypts with the same {@code mediaKey}, and streams the
     * ciphertext straight to the HTTP socket. The encrypted content
     * length is computed deterministically from the plaintext length
     * ({@code ((plaintextLength / 16) + 1) * 16 + 10} for AES-CBC + PKCS5
     * padding + truncated HMAC). Retries replay pass 2 against the same
     * payload; the plaintext source is replayable by contract. On completion
     * a {@code MediaUpload2} beacon is committed (success or classified
     * failure), except for {@link ExternalBlobReference} uploads whose beacon
     * is committed by the app-state exchange pipeline instead.
     *
     * @param provider the media provider describing the media type and
     *                 receiving the upload metadata
     * @param payload  the transcoded payload
     * @return {@code true} if the upload succeeded
     * @throws WhatsAppMediaException.Upload if no host could service the
     *         upload, a non-retryable HTTP error occurred (413, 415, 507),
     *         or an I/O error occurred
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClient", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean upload(MediaProvider provider, MediaPayload payload) throws WhatsAppMediaException, InterruptedException {
        Objects.requireNonNull(provider, "provider cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");

        var path = provider.mediaPath()
                .path();
        if (path.isEmpty()) {
            return false;
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "upload start: mediaType={0}", provider.mediaPath());

        awaitInitialized();
        var uploadStart = Instant.now();
        var currentAuth = this.auth;
        var currentHosts = this.hosts;
        var currentMaxBuckets = this.maxBuckets;

        var keyName = provider.mediaPath().keyName();
        var mediaKey = keyName.isPresent()
                ? DataUtils.randomByteArray(32)
                : null;

        byte[] fileSha256;
        byte[] fileEncSha256;
        long fileLength;
        try (var pass1 = openUploadStream(provider, payload, mediaKey)) {
            pass1.transferTo(OutputStream.nullOutputStream());
            fileSha256 = pass1.fileSha256();
            fileEncSha256 = pass1.fileEncSha256().orElse(null);
            fileLength = pass1.fileLength();
        } catch (IOException exception) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "upload hash pass failed: mediaType=" + provider.mediaPath(), exception);
            }
            throw new WhatsAppMediaException.Upload(
                    "Cannot encrypt media (hash pass)", exception);
        }

        var encryptedLength = mediaKey != null
                ? ((fileLength / 16L) + 1L) * 16L + MediaUploadInputStream.MAC_LENGTH
                : fileLength;

        try (var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var timestamp = Instant.now();
            var hostList = currentHosts instanceof List<? extends MediaHost> list ? list : List.copyOf(currentHosts);
            var route = MediaHost.routeSelection(
                    Operation.UPLOAD,
                    provider.mediaPath(),
                    hostList,
                    fileEncSha256 != null ? Base64.getEncoder().encodeToString(fileEncSha256) : null,
                    currentMaxBuckets > 0 ? currentMaxBuckets : null,
                    false
            );

            String lastHostUsed = null;
            var lastFetchMadeProgress = false;
            for (var attemptCount = 0; attemptCount < MAX_ATTEMPT_COUNT; attemptCount++) {
                var hostname = selectHost(
                        route.selectedHost().orElse(null),
                        route.fallbackHost().orElse(null),
                        lastHostUsed,
                        attemptCount,
                        lastFetchMadeProgress
                );
                if (hostname == null) {
                    continue;
                }
                lastHostUsed = hostname;

                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "upload attempt {0} host={1}", attemptCount, hostname);
                }

                try {
                    var body = buildEncryptingBodyPublisher(provider, payload, mediaKey, encryptedLength);
                    var uploadResult = tryUpload(client, hostname, path.get(), currentAuth, fileEncSha256, fileSha256, body);

                    var directPath = uploadResult.getString("direct_path");
                    var url = uploadResult.getString("url");
                    var handle = uploadResult.getString("handle");

                    provider.setMediaSha256(fileSha256);
                    provider.setMediaEncryptedSha256(fileEncSha256);
                    provider.setMediaKey(mediaKey);
                    provider.setMediaSize(fileLength);
                    provider.setMediaDirectPath(directPath);
                    provider.setMediaUrl(url);
                    provider.setMediaKeyTimestamp(timestamp);
                    if (provider instanceof ExternalBlobReference externalBlobReference) {
                        externalBlobReference.setHandle(handle);
                    }

                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "upload succeeded: host={0} attempt={1} size={2} encryptedSize={3}",
                                hostname, attemptCount, fileLength, encryptedLength);
                    }
                    commitMessageMediaUploadSuccess(provider, hostname, fileLength, encryptedLength, attemptCount, uploadStart);
                    return true;
                } catch (WhatsAppMediaException.Upload uploadException) {
                    if (!isRetryable(uploadException)) {
                        if (Log.ERROR) {
                            LOGGER.log(Level.ERROR, "upload failed (non-retryable): host=" + hostname, uploadException);
                        }
                        commitMessageMediaUploadFailure(provider, fileLength, attemptCount, uploadStart, uploadException);
                        throw uploadException;
                    }

                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "upload attempt {0} failed on host {1}, retrying: {2}",
                                attemptCount, hostname, uploadException.getMessage());
                    }

                    lastFetchMadeProgress = false;

                    if (attemptCount < MAX_ATTEMPT_COUNT - 1) {
                        sleepForBackoff(attemptCount);
                    }
                }
            }

            var noHostError = new WhatsAppMediaException.Upload("Cannot upload media: no hosts available");
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "upload failed: mediaType=" + provider.mediaPath(), noHostError);
            }
            commitMessageMediaUploadFailure(provider, fileLength, MAX_ATTEMPT_COUNT - 1, uploadStart, noHostError);
            throw noHostError;
        }
    }

    /**
     * Opens an upload stream over the payload's plaintext, using
     * {@code mediaKey} for AES-CBC encryption when the provider requests it.
     *
     * <p>Called twice per upload: once for the hash pass and once per HTTP
     * attempt for the streaming POST. The returned stream owns the
     * plaintext stream and closes it on
     * {@link MediaUploadInputStream#close()}.
     *
     * @param provider the media provider
     * @param payload  the transcoded payload
     * @param mediaKey the encryption key, or {@code null} for the
     *                 plaintext variant
     * @return the upload stream
     * @throws IOException if the plaintext stream cannot be opened or the
     *         cipher cannot be initialised
     */
    private static MediaUploadInputStream openUploadStream(MediaProvider provider,
                                                            MediaPayload payload,
                                                            byte[] mediaKey) throws IOException {
        var plaintext = payload.openPlaintext();
        try {
            return MediaUploadInputStream.of(provider, plaintext, mediaKey);
        } catch (WhatsAppMediaException exception) {
            plaintext.close();
            throw new IOException("Cannot initialise upload stream", exception);
        }
    }

    /**
     * Builds a known-length {@link HttpRequest.BodyPublisher} that streams
     * the encrypted payload directly to the HTTP socket.
     *
     * <p>Wraps {@link HttpRequest.BodyPublishers#ofInputStream(Supplier)}
     * with the deterministic encrypted content length so the JVM's
     * {@link HttpClient} can set a real {@code Content-Length} header (the
     * CDN refuses chunked encoding). Each subscription opens a fresh upload
     * stream over the payload's plaintext; the same {@code mediaKey} is
     * reused so the emitted ciphertext matches the {@code fileEncSha256}
     * captured during the hash pass.
     *
     * @param provider        the media provider
     * @param payload         the transcoded payload
     * @param mediaKey        the encryption key, or {@code null} for the
     *                        plaintext variant
     * @param encryptedLength the deterministic ciphertext length
     * @return a body publisher with the specified content length
     */
    private static HttpRequest.BodyPublisher buildEncryptingBodyPublisher(MediaProvider provider,
                                                                          MediaPayload payload,
                                                                          byte[] mediaKey,
                                                                          long encryptedLength) {
        Supplier<InputStream> supplier = () -> {
            try {
                return openUploadStream(provider, payload, mediaKey);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        };
        return HttpRequest.BodyPublishers.fromPublisher(
                HttpRequest.BodyPublishers.ofInputStream(supplier),
                encryptedLength);
    }

    /**
     * Performs one HTTP POST upload of an encrypted media payload against a
     * single CDN host.
     *
     * <p>Helper for the {@link #upload(MediaProvider, MediaPayload)} retry
     * loop. The target URL is assembled by
     * {@link #formatUploadUrl(String, String, String, long, String)}; the
     * URL-safe base64 of the encrypted file hash is appended as the trailing
     * path segment, and the {@code auth}, {@code token}, and
     * {@code media_id} query parameters follow. On a non-{@code 200}
     * response a {@link WhatsAppMediaException.Upload} is raised with the
     * HTTP status code preserved; the response JSON must contain
     * non-{@code null}, non-empty {@code direct_path} and {@code url} fields
     * or the upload is rejected.
     *
     * @implNote
     * This implementation issues a single fire-and-forget POST; WA Web's
     * {@code WAWebMmsClientUploadStreamer} additionally supports resume,
     * chunked streaming, server-side thumbnail generation, and
     * newsletter-video transcoding query parameters, none of which are
     * wired in Cobalt.
     *
     * @param client        the HTTP client to use
     * @param hostname      the CDN hostname
     * @param path          the CDN path segment
     * @param fileEncSha256 the encrypted file SHA-256, or {@code null}
     * @param fileSha256    the plaintext file SHA-256
     * @param body          the streaming body publisher emitting the
     *                      encrypted payload with a known content length
     * @return the parsed response JSON
     * @throws WhatsAppMediaException.Upload if the server returns a
     *         non-{@code 200} status code or the response is missing
     *         required fields
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientMmsUpload", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private JSONObject tryUpload(HttpClient client, String hostname, String path,
                                 String auth,
                                 byte[] fileEncSha256, byte[] fileSha256,
                                 HttpRequest.BodyPublisher body) throws WhatsAppMediaException.Upload {
        try {
            var token = Base64.getUrlEncoder()
                    .encodeToString(Objects.requireNonNullElse(fileEncSha256, fileSha256));

            var uri = URI.create(formatUploadUrl(hostname, path, token, generateMediaId(), auth));
            var requestBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .POST(body);
            var request = requestBuilder.header("Content-Type", "application/octet-stream")
                    .header("Accept", "application/json")
                    .headers("Origin", "https://web.whatsapp.com")
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "mms upload response: host={0} status={1}", hostname, response.statusCode());
            }

            if (response.statusCode() != 200) {
                var statusCode = response.statusCode();
                var message = switch (statusCode) {
                    case 401 -> "mmsUpload: unauthorized";
                    case 413 -> "mmsUpload: media too large";
                    case 415 -> "mmsUpload: hash mismatch";
                    case 507 -> "mmsUpload: throttled";
                    default -> "mmsUpload: HTTP " + statusCode;
                };
                throw new WhatsAppMediaException.Upload(statusCode, message);
            }

            var jsonObject = JSON.parseObject(response.body());
            var directPath = jsonObject != null ? jsonObject.getString("direct_path") : null;
            var url = jsonObject != null ? jsonObject.getString("url") : null;
            if (directPath == null || directPath.isEmpty()) {
                throw new WhatsAppMediaException.Upload("mmsUpload: missing direct_path");
            }
            if (url == null || url.isEmpty()) {
                throw new WhatsAppMediaException.Upload("mmsUpload: missing url");
            }
            return jsonObject;
        } catch (IOException | InterruptedException exception) {
            // No status code on network-level failures; isRetryable's empty-status branch treats this as retryable
            throw new WhatsAppMediaException.Upload("mmsUpload: network error", exception);
        }
    }

    /**
     * Produces a random media event identifier for telemetry and request
     * correlation.
     *
     * <p>Appended as the {@code media_id} query parameter on every CDN POST
     * so server-side analytics can de-duplicate events across retries. The
     * value is a positive long in the range {@code [1, 2^53 - 1]}, the
     * range that survives round-tripping through the server's JSON
     * double-precision representation.
     *
     * @return a random positive long suitable for use as a media id
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils", exports = "generateMediaEventId",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static long generateMediaId() {
        return MediaMetricUtils.generateMediaEventId();
    }

    /**
     * Builds the upload URL for an authenticated media POST against the
     * WhatsApp CDN.
     *
     * <p>The URL is assembled as
     * {@snippet :
     * https://{hostname}/{path}/{token}?auth=...&token=...&media_id=...
     * }
     * where {@code token} is the URL-safe base64 of the encrypted file hash
     * and is reused as both a path segment and a query parameter.
     *
     * @param hostname the CDN hostname
     * @param path     the CDN path segment for the media type
     * @param token    the URL-safe base64 of the encrypted file hash
     * @param mediaId  the random media-event id
     * @param auth     the authentication token presented to the CDN
     * @return the assembled upload URL
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientFormatUploadUrl", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebMmsClientFormatHashUrl", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    String formatUploadUrl(String hostname, String path, String token, long mediaId, String auth) {
        var basePath = "https://" + hostname + "/" + path + "/" + token;

        var queryParams = new LinkedHashMap<String, String>();
        queryParams.put("auth", auth);
        queryParams.put("token", token);
        queryParams.put("media_id", String.valueOf(mediaId));

        return basePath + encodeQueryString(queryParams);
    }

    /**
     * Serialises a map of query parameters into a URL query string.
     *
     * <p>Helper for {@link #formatUploadUrl} and {@link #buildDirectPathUrl};
     * {@code null} values are skipped and every remaining key/value pair is
     * percent-encoded under {@code application/x-www-form-urlencoded} rules.
     *
     * @param params the query parameter map
     * @return the encoded query string with leading {@code "?"}, or an
     *         empty string when no parameters remain after filtering
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientFormatHashUrl", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String encodeQueryString(Map<String, String> params) {
        var sb = new StringBuilder();
        for (var entry : params.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.isEmpty() ? "" : "?" + sb;
    }

    /**
     * Tests whether a media upload error is worth retrying against a
     * different host.
     *
     * <p>The classification, mapped onto Cobalt's exception subtypes:
     * <ul>
     *   <li>Missing status code (network error): retryable</li>
     *   <li>{@code 401} (unauthorized): retryable</li>
     *   <li>{@code 507} (throttled): not retryable</li>
     *   <li>Other {@code 5xx}: retryable</li>
     *   <li>Every other status ({@code 404}, {@code 408}, {@code 410},
     *       {@code 413}, {@code 415}, {@code 403}, and so on): not
     *       retryable</li>
     * </ul>
     *
     * @param exception the upload exception to test
     * @return {@code true} if the error is retryable
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientIsErrorRetryable",
            exports = "isErrorRetryable",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isRetryable(WhatsAppMediaException.Upload exception) {
        var optStatus = exception.httpStatusCode();
        if (optStatus.isEmpty()) {
            return true;
        }
        var status = optStatus.getAsInt();

        if (status == 401) {
            return true;
        }

        if (status == 507) {
            return false;
        }

        return status >= 500;
    }

    /**
     * Downloads a media payload from WhatsApp's CDN for the given provider.
     *
     * <p>High-level entry point for any code that materialises an inbound
     * attachment. First tries the provider's cached static media URL, if
     * any; if that fails with a retryable error, resolves a fresh host
     * through {@link MediaHost#routeSelection} and rotates across candidate
     * hosts on each retry. Non-retryable errors ({@code 404}, {@code 408},
     * {@code 410}, {@code 507}, and the rest of {@code 4xx}) propagate
     * immediately. The injected {@link ABPropsService} feeds the download
     * URL formatter (cache-affinity hints and the hash-URL deprecation
     * flag).
     *
     * @implNote
     * This implementation does not consult an in-memory media-conn cache
     * the way WA Web's {@code mediaHosts} singleton does; each
     * {@link MediaConnectionService} is treated as immutable and the caller
     * decides when to refresh. The actual transfer is delegated to
     * {@link #performDownload(MediaProvider)} and wrapped so that every call
     * commits one {@code MediaDownload2} beacon (overall transfer size, timing,
     * and result) and one {@code WebcMediaRmr} beacon (retry-media-request
     * timing and status), plus a {@code StickerError} beacon when a
     * sticker-type download fails.
     *
     * @param provider the media provider
     * @return an {@link InputStream} delivering the decrypted media
     *         content
     * @throws WhatsAppMediaException.Download if no host could service the
     *         download, the direct path is missing, or a non-retryable
     *         HTTP error occurred
     * @throws InterruptedException if the calling thread is interrupted while
     *         awaiting the first {@code media_conn} refresh
     */
    @WhatsAppWebExport(moduleName = "WAWebDownloadManager", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public InputStream download(MediaProvider provider) throws WhatsAppMediaException, InterruptedException {
        Objects.requireNonNull(provider, "provider cannot be null");

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "download start: mediaType={0}", provider.mediaPath());

        var rmrStart = Instant.now();
        try {
            var stream = performDownload(provider);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "download succeeded: mediaType={0}", provider.mediaPath());
            commitMessageMediaDownloadSuccess(provider, rmrStart);
            commitWebcMediaRmr(provider, rmrStart, false, OptionalInt.empty());
            return stream;
        } catch (WhatsAppMediaException.Download downloadException) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "download failed: mediaType=" + provider.mediaPath(), downloadException);
            }
            commitMessageMediaDownloadFailure(provider, rmrStart, downloadException);
            commitWebcMediaRmr(provider, rmrStart, true, downloadException.httpStatusCode());
            if (isStickerMedia(provider.mediaPath())) {
                commitStickerError(StickerErrorType.DECOMPRESSION);
            }
            throw downloadException;
        }
    }

    /**
     * Resolves a decrypted media stream for the given provider without
     * emitting any media-transfer telemetry.
     *
     * <p>Carries the download strategy that {@link #download(MediaProvider)}
     * wraps: it first tries the provider's cached static media URL, then
     * resolves a fresh host through {@link MediaHost#routeSelection} and
     * rotates across candidate hosts on each retry. Non-retryable errors
     * ({@code 404}, {@code 408}, {@code 410}, {@code 507}, and the rest of
     * {@code 4xx}) propagate immediately. The injected {@link ABPropsService}
     * feeds the download URL formatter.
     *
     * @param provider the media provider
     * @return an {@link InputStream} delivering the decrypted media content
     * @throws WhatsAppMediaException.Download if no host could service the
     *         download, the direct path is missing, or a non-retryable
     *         HTTP error occurred
     * @throws InterruptedException if the calling thread is interrupted while
     *         awaiting the first {@code media_conn} refresh
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClient", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private InputStream performDownload(MediaProvider provider) throws WhatsAppMediaException, InterruptedException {
        var defaultUploadUrl = provider.mediaUrl();
        if (defaultUploadUrl.isPresent()) {
            try {
                return tryDownload(provider, defaultUploadUrl.get());
            } catch (WhatsAppMediaException.Download downloadException) {
                if (!isDownloadRetryable(downloadException)) {
                    if (Log.ERROR) {
                        LOGGER.log(Level.ERROR, "download failed (non-retryable) from cached url", downloadException);
                    }
                    throw downloadException;
                }
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "cached download url failed, falling back to host resolution: {0}",
                            downloadException.getMessage());
                }
            }
        }

        var defaultDirectPath = provider.mediaDirectPath()
                .orElse(null);
        var encFileHash = provider.mediaEncryptedSha256()
                .map(bytes -> Base64.getEncoder().encodeToString(bytes))
                .orElse(null);
        var mediaType = provider.mediaPath();

        if ((defaultDirectPath == null || defaultDirectPath.isEmpty())
                && (encFileHash == null || encFileHash.isEmpty())) {
            var missingRefError = new WhatsAppMediaException.Download(
                    "No staticUrl, directPath, or encFilehash available for download");
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "download failed: mediaType=" + mediaType, missingRefError);
            }
            throw missingRefError;
        }

        awaitInitialized();
        var currentHosts = this.hosts;
        var currentMaxBuckets = this.maxBuckets;
        var hostList = currentHosts instanceof List<? extends MediaHost> list ? list : List.copyOf(currentHosts);
        var route = MediaHost.routeSelection(
                Operation.DOWNLOAD,
                mediaType,
                hostList,
                encFileHash,
                currentMaxBuckets > 0 ? currentMaxBuckets : null,
                false
        );

        var selectedBucket = route.selectedBucket().orElse(null);
        var selectedHostname = route.selectedHost()
                .map(MediaHost::hostname)
                .orElse(null);

        String lastHostUsed = null;
        var lastFetchMadeProgress = false;
        for (var attemptCount = 0; attemptCount < MAX_ATTEMPT_COUNT; attemptCount++) {
            var hostname = selectHost(
                    route.selectedHost().orElse(null),
                    route.fallbackHost().orElse(null),
                    lastHostUsed,
                    attemptCount,
                    lastFetchMadeProgress
            );
            if (hostname == null) {
                continue;
            }
            lastHostUsed = hostname;

            var hostBucket = hostname.equals(selectedHostname) ? selectedBucket : null;

            var downloadUrl = formatDownloadUrl(
                    hostname, defaultDirectPath, encFileHash, mediaType, hostBucket
            );

            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "download attempt {0} host={1}", attemptCount, hostname);
            }

            try {
                return tryDownload(provider, downloadUrl);
            } catch (WhatsAppMediaException.Download downloadException) {
                if (!isDownloadRetryable(downloadException)) {
                    if (Log.ERROR) {
                        LOGGER.log(Level.ERROR, "download failed (non-retryable): host=" + hostname, downloadException);
                    }
                    throw downloadException;
                }

                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "download attempt {0} failed on host {1}, retrying: {2}",
                            attemptCount, hostname, downloadException.getMessage());
                }

                lastFetchMadeProgress = false;

                if (attemptCount < MAX_ATTEMPT_COUNT - 1) {
                    sleepForBackoff(attemptCount);
                }
            }
        }

        var noHostError = new WhatsAppMediaException.Download("Cannot download media: no hosts available");
        if (Log.ERROR) {
            LOGGER.log(Level.ERROR, "download failed: mediaType=" + mediaType, noHostError);
        }
        throw noHostError;
    }

    /**
     * Chooses between the direct-path and hash-based URL formats and
     * returns the final CDN download URL.
     *
     * <p>Helper for {@link #download(MediaProvider)} and the HEAD-request
     * helpers. When a direct path is available the URL is built via
     * {@link #buildDirectPathUrl}; when only the encrypted file hash is
     * available the URL is built via {@link #formatHashUrl}, unless the
     * {@code web_deprecate_mms4_hash_based_download} AB prop is enabled, in
     * which case this method throws unconditionally.
     *
     * @param hostname       the CDN hostname
     * @param directPath     the direct path, or {@code null}
     * @param encFileHash    the base64-encoded encrypted file hash, or
     *                       {@code null}
     * @param mediaType      the media path type
     * @param downloadBucket the selected download bucket, or {@code null}
     * @return the constructed download URL
     * @throws WhatsAppMediaException.Download if neither direct path nor
     *         encrypted file hash is available, or the hash-URL fallback
     *         is deprecated by the AB prop
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientFormatDownloadUrl", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    String formatDownloadUrl(
            String hostname,
            String directPath,
            String encFileHash,
            MediaPath mediaType,
            Integer downloadBucket
    ) throws WhatsAppMediaException.Download {
        var mediaTypeId = mediaType.id().orElse(null);
        if (directPath != null && !directPath.isEmpty()) {
            var query = new LinkedHashMap<String, String>();
            query.put("mode", "auto");
            query.put("mms-type", mediaTypeId);
            query.put("__wa-mms", "");
            return buildDirectPathUrl(hostname, directPath, encFileHash, downloadBucket, mediaType, query);
        }

        if (abPropsService.getBool(ABProp.WEB_DEPRECATE_MMS4_HASH_BASED_DOWNLOAD)) {
            throw new WhatsAppMediaException.Download(
                    "No direct path is available for download, abort");
        }

        if (encFileHash == null || encFileHash.isEmpty()) {
            throw new WhatsAppMediaException.Download(
                    "No direct path or encFilehash available for download, abort");
        }

        var query = new LinkedHashMap<String, String>();
        query.put("mode", "auto");
        query.put("__wa-mms", "");
        return formatHashUrl(hostname, mediaType, encFileHash, query);
    }

    /**
     * Builds a direct-path download URL.
     *
     * <p>Helper for {@link #formatDownloadUrl}. Appends the URL-safe base64
     * of the encrypted file hash as {@code hash}, the download bucket as
     * {@code _nc_cat}, and the {@code _nc_map=whatsapp-nofna} cache-affinity
     * hint when the media type is listed in the
     * {@code low_cache_hit_rate_media_types} AB prop. A hostname security
     * check rejects direct paths whose embedded URL resolves to a different
     * host.
     *
     * @param hostname       the expected CDN hostname
     * @param directPath     the direct path segment
     * @param encFileHash    the base64-encoded encrypted file hash, or
     *                       {@code null}
     * @param downloadBucket the download bucket number, or {@code null}
     * @param mediaType      the media path type
     * @param query          the additional query parameters to include
     * @return the assembled download URL
     * @throws WhatsAppMediaException.Download if the direct path resolves
     *         to a different hostname than expected
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientFormatDownloadUrl", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    private String buildDirectPathUrl(
            String hostname,
            String directPath,
            String encFileHash,
            Integer downloadBucket,
            MediaPath mediaType,
            Map<String, String> query
    ) throws WhatsAppMediaException.Download {
        var baseUri = URI.create("https://" + hostname).resolve(directPath);

        if (!baseUri.getHost().equals(hostname)) {
            throw new WhatsAppMediaException.Download("malicious directPath");
        }

        // java.net.URI has no URLSearchParams equivalent; the existing query is split manually
        var params = new LinkedHashMap<String, String>();
        var existingQuery = baseUri.getRawQuery();
        if (existingQuery != null && !existingQuery.isEmpty()) {
            for (var param : existingQuery.split("&")) {
                var eqIndex = param.indexOf('=');
                if (eqIndex >= 0) {
                    params.put(param.substring(0, eqIndex), param.substring(eqIndex + 1));
                } else {
                    params.put(param, "");
                }
            }
        }

        if (encFileHash != null && !encFileHash.isEmpty()) {
            params.put("hash", urlSafeBase64(encFileHash));
        }

        if (downloadBucket != null) {
            params.put("_nc_cat", downloadBucket.toString());
        }

        var lowCacheHitTypes = abPropsService.getString(ABProp.LOW_CACHE_HIT_RATE_MEDIA_TYPES);
        var mediaTypeId = mediaType.id().orElse(null);
        if (lowCacheHitTypes != null && !lowCacheHitTypes.isEmpty() && mediaTypeId != null) {
            for (var candidate : lowCacheHitTypes.split(",")) {
                if (candidate.equals(mediaTypeId)) {
                    params.put("_nc_map", "whatsapp-nofna");
                    break;
                }
            }
        }

        for (var entry : query.entrySet()) {
            if (entry.getValue() != null) {
                params.put(entry.getKey(), entry.getValue());
            }
        }

        return "https://" + baseUri.getHost() + baseUri.getRawPath()
                + encodeQueryString(params);
    }

    /**
     * Builds a hash-based download URL.
     *
     * <p>Helper for {@link #formatDownloadUrl}; produces a URL of the form
     * {@snippet :
     * https://{hostname}/{path}/{urlSafeBase64(encFileHash)}?{query}
     * }
     * where {@code path} is read from the media type metadata.
     *
     * @param hostname    the CDN hostname
     * @param mediaType   the media path type
     * @param encFileHash the base64-encoded encrypted file hash
     * @param query       the additional query parameters to include
     * @return the assembled hash-based download URL
     * @throws WhatsAppMediaException.Download if the media type has no
     *         path segment for hash URL construction
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientFormatHashUrl", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static String formatHashUrl(
            String hostname,
            MediaPath mediaType,
            String encFileHash,
            Map<String, String> query
    ) throws WhatsAppMediaException.Download {
        var pathSegment = mediaType.path()
                .orElseThrow(() -> new WhatsAppMediaException.Download(
                        "No hash URL path for media type: " + mediaType));

        var basePath = "https://" + hostname + "/" + pathSegment + "/" + urlSafeBase64(encFileHash);

        var filteredQuery = new LinkedHashMap<String, String>();
        for (var entry : query.entrySet()) {
            if (entry.getValue() != null) {
                filteredQuery.put(entry.getKey(), entry.getValue());
            }
        }

        return basePath + encodeQueryString(filteredQuery);
    }

    /**
     * Converts a standard base64 string to the URL-safe variant used by
     * WhatsApp CDN URLs.
     *
     * <p>Replaces {@code '/'} with {@code '_'} and {@code '+'} with
     * {@code '-'} while keeping the {@code '='} padding characters.
     *
     * @param base64 the standard base64 string to convert
     * @return the URL-safe base64 string
     */
    @WhatsAppWebExport(moduleName = "WABase64UrlSafe", exports = "urlSafeBase64",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static String urlSafeBase64(String base64) {
        return base64.replace('/', '_').replace('+', '-');
    }

    /**
     * Performs one HTTP GET download against a fully-formed CDN URL.
     *
     * <p>Helper for the {@link #download(MediaProvider)} retry loop and for
     * direct re-downloads against a known URL. Wraps the response body in a
     * {@code MediaDownloadInputStream} so the caller sees decrypted,
     * integrity-checked bytes; the returned stream owns the underlying
     * {@link HttpClient} and closes it when consumed or closed by the
     * caller.
     *
     * @param provider    the media provider holding the decryption metadata
     * @param downloadUrl the full URL to download from
     * @return an {@link InputStream} delivering the decrypted media
     *         content
     * @throws WhatsAppMediaException.Download if the server returns a
     *         non-{@code 200} status code, the {@code Content-Length}
     *         header is missing, or a network error occurs
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientMmsDownload", exports = "mms4Download",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public InputStream tryDownload(MediaProvider provider, String downloadUrl) throws WhatsAppMediaException.Download {
        var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "mms download response: host={0} status={1}",
                        request.uri().getHost(), response.statusCode());
            }

            if (response.statusCode() != 200) {
                // 403 with "URL signature expired" body must reclassify to MediaNotFoundError so read the body first
                String body;
                try (var rawStream = response.body()) {
                    body = new String(rawStream.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException _) {
                    body = null;
                }
                client.close();
                validateMmsResponse(response.statusCode(), downloadUrl, body);
            }

            var payloadLength = response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElseThrow(() -> {
                        client.close();
                        return new WhatsAppMediaException.Download("Unknown content length");
                    });

            var rawInputStream = response.body();
            return new MediaDownloadInputStream(client, rawInputStream, payloadLength, provider);
        } catch (IOException | InterruptedException exception) {
            client.close();
            throw new WhatsAppMediaException.Download("mmsDownload: network error", exception);
        }
    }

    /**
     * Translates a non-OK HTTP status code from a media download into the
     * appropriate {@link WhatsAppMediaException.Download}.
     *
     * <p>The mapping:
     * <ul>
     *   <li>{@code 401}: {@link WhatsAppMediaException#HTTP_UNAUTHORIZED}</li>
     *   <li>{@code 403}: {@link WhatsAppMediaException#HTTP_NOT_FOUND}
     *       when the body contains {@code "URL signature expired"} or the
     *       URL's {@code oe} parameter encodes an expired signature;
     *       otherwise {@link WhatsAppMediaException#HTTP_FORBIDDEN}</li>
     *   <li>{@code 404} and {@code 410}:
     *       {@link WhatsAppMediaException#HTTP_NOT_FOUND}</li>
     *   <li>{@code 507}: {@link WhatsAppMediaException#HTTP_THROTTLE}</li>
     *   <li>any other: the raw status code</li>
     * </ul>
     *
     * @param statusCode the HTTP status code
     * @param url        the download URL, used for expiry parsing
     * @param body       the response body for the GET path, or
     *                   {@code null} for HEAD requests
     * @throws WhatsAppMediaException.Download always, since this method
     *         is only called for non-OK responses
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientMmsDownload", exports = "validateMmsResponse",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebMmsCdnUrlValidationUtils", exports = "parseCdnUrlParams",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static void validateMmsResponse(int statusCode, String url, String body) throws WhatsAppMediaException.Download {
        if (statusCode == 401) {
            throw new WhatsAppMediaException.Download(WhatsAppMediaException.HTTP_UNAUTHORIZED,
                    "mmsDownload: unauthorized");
        }

        if (statusCode == 403) {
            if (body != null && body.contains("URL signature expired")) {
                throw new WhatsAppMediaException.Download(WhatsAppMediaException.HTTP_NOT_FOUND,
                        "mmsDownload: media not found (URL signature expired)");
            }

            // The "oe" query parameter encodes the URL signature expiry as a hex unix timestamp
            try {
                var uri = URI.create(url);
                var query = uri.getRawQuery();
                if (query != null) {
                    for (var param : query.split("&")) {
                        var eqIndex = param.indexOf('=');
                        if (eqIndex >= 0 && param.substring(0, eqIndex).equals("oe")) {
                            var hexValue = param.substring(eqIndex + 1);
                            var expirationEpoch = Long.parseLong(hexValue, 16);

                            if (Instant.now().getEpochSecond() >= expirationEpoch) {
                                throw new WhatsAppMediaException.Download(WhatsAppMediaException.HTTP_NOT_FOUND,
                                        "mmsDownload: media not found (URL expired)");
                            }
                            break;
                        }
                    }
                }
            } catch (WhatsAppMediaException.Download e) {
                throw e;
            } catch (Exception _) {
                // Malformed URL or non-hex oe value falls through to the generic forbidden classification below
            }

            throw new WhatsAppMediaException.Download(WhatsAppMediaException.HTTP_FORBIDDEN,
                    "mmsDownload: forbidden");
        }

        if (statusCode == 404 || statusCode == 410) {
            throw new WhatsAppMediaException.Download(WhatsAppMediaException.HTTP_NOT_FOUND,
                    "mmsDownload: media not found");
        }

        if (statusCode == 507) {
            throw new WhatsAppMediaException.Download(WhatsAppMediaException.HTTP_THROTTLE,
                    "mmsDownload: throttled");
        }

        throw new WhatsAppMediaException.Download(statusCode,
                "mmsDownload: HTTP " + statusCode);
    }

    /**
     * Tests whether a media download error is worth retrying against a
     * different host.
     *
     * <p>Applies the same rule set as
     * {@link #isRetryable(WhatsAppMediaException.Upload)} to download-class
     * exceptions: missing status code retryable, {@code 401} retryable,
     * {@code 507} fatal, other {@code 5xx} retryable, every other status
     * fatal.
     *
     * @param exception the download exception to test
     * @return {@code true} if the error is retryable
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientIsErrorRetryable",
            exports = "isErrorRetryable",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isDownloadRetryable(WhatsAppMediaException.Download exception) {
        var optStatus = exception.httpStatusCode();
        if (optStatus.isEmpty()) {
            return true;
        }
        var status = optStatus.getAsInt();

        if (status == 401) {
            return true;
        }

        if (status == 507) {
            return false;
        }

        return status >= 500;
    }

    /**
     * Tests whether a raw HTTP status code is retryable in isolation,
     * ignoring the wrapping exception subtype.
     *
     * <p>The rules: {@code 408} retryable, {@code 507} fatal, every other
     * status retryable if and only if it is {@code 5xx}. More permissive
     * than {@link #isRetryable(WhatsAppMediaException.Upload)} because it
     * does not consult the exception subtype taxonomy.
     *
     * @implNote
     * This implementation is unused by Cobalt's own pipeline and exists for
     * parity with the WA Web status-only retry classifier; the upload and
     * download retry loops consult
     * {@link #isRetryable(WhatsAppMediaException.Upload)} and
     * {@link #isDownloadRetryable(WhatsAppMediaException.Download)} instead.
     *
     * @param statusCode the HTTP status code
     * @return {@code true} if the status alone marks the response as
     *         retryable
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientIsErrorRetryable",
            exports = "isRetriableStatusCode",
            adaptation = WhatsAppAdaptation.DIRECT)
    @SuppressWarnings("unused")
    private static boolean isRetriableStatusCode(int statusCode) {
        if (statusCode == 408) {
            return true;
        }
        if (statusCode == 507) {
            return false;
        }
        return statusCode >= 500;
    }

    /**
     * Probes the WhatsApp CDN to verify that a media file exists and is
     * still available for download.
     *
     * <p>Useful before kicking off a full download for an attachment that
     * was referenced from elsewhere (a quoted message, a forwarded sticker)
     * to avoid wasting bandwidth on a known-missing payload. Sends an HTTP
     * HEAD request; a {@code 200} response signals availability, any other
     * status is mapped through
     * {@link #validateMmsResponse(int, String, String)}.
     *
     * @param hostname    the CDN hostname
     * @param mediaType   the media path type
     * @param directPath  the CDN direct path, or {@code null}
     * @param encFileHash the base64-encoded encrypted file hash, or
     *                    {@code null}
     * @throws WhatsAppMediaException.Download if the media does not exist or
     *         a network error occurs
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientMmsDownload", exports = "mmsCheckExistence",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void checkExistence(
            String hostname,
            MediaPath mediaType,
            String directPath,
            String encFileHash
    ) throws WhatsAppMediaException.Download {
        sendHeadRequest(hostname, mediaType, directPath, encFileHash, "mmsCheckExistence");
    }

    /**
     * Retrieves the size in bytes of the encrypted media payload stored on
     * the WhatsApp CDN.
     *
     * <p>Sends an HTTP HEAD request and reads the {@code Content-Length}
     * header so the caller can pre-allocate buffers or estimate bandwidth
     * before invoking a full download.
     *
     * @param hostname    the CDN hostname
     * @param mediaType   the media path type
     * @param directPath  the CDN direct path, or {@code null}
     * @param encFileHash the base64-encoded encrypted file hash, or
     *                    {@code null}
     * @return the encrypted media file size in bytes
     * @throws WhatsAppMediaException.Download if the {@code Content-Length}
     *         header is missing, the server returns a non-OK status, or a
     *         network error occurs
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientMmsDownload", exports = "mmsGetEncryptedMediaSize",
            adaptation = WhatsAppAdaptation.DIRECT)
    public long getEncryptedMediaSize(
            String hostname,
            MediaPath mediaType,
            String directPath,
            String encFileHash
    ) throws WhatsAppMediaException.Download {
        var response = sendHeadRequest(hostname, mediaType, directPath, encFileHash, "mmsGetEncryptedMediaSize");

        var contentLength = response.headers()
                .firstValueAsLong("Content-Length")
                .orElse(-1L);
        if (contentLength < 0) {
            throw new WhatsAppMediaException.Download("Unable to get content length");
        }

        return contentLength;
    }

    /**
     * Issues a single HTTP HEAD request and returns the validated response.
     *
     * <p>Shared helper for {@link #checkExistence} and
     * {@link #getEncryptedMediaSize}. Constructs the URL via
     * {@link #formatDownloadUrl} and validates the status code via
     * {@link #validateMmsResponse(int, String, String)}.
     *
     * @param hostname     the CDN hostname
     * @param mediaType    the media path type
     * @param directPath   the CDN direct path, or {@code null}
     * @param encFileHash  the base64-encoded encrypted file hash, or
     *                     {@code null}
     * @param functionName the caller name used as the network-error
     *                     prefix
     * @return the HTTP HEAD response
     * @throws WhatsAppMediaException.Download if no path is available,
     *         the server returns a non-OK status, or a network error
     *         occurs
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientMmsDownload",
            exports = {"mmsCheckExistence", "mmsGetEncryptedMediaSize"},
            adaptation = WhatsAppAdaptation.ADAPTED)
    private HttpResponse<?> sendHeadRequest(
            String hostname,
            MediaPath mediaType,
            String directPath,
            String encFileHash,
            String functionName
    ) throws WhatsAppMediaException.Download {
        var url = formatDownloadUrl(hostname, directPath, encFileHash, mediaType, null);

        try (var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "{0} HEAD response: host={1} status={2}",
                        functionName, hostname, response.statusCode());
            }

            if (response.statusCode() != 200) {
                validateMmsResponse(response.statusCode(), url, null);
            }

            return response;
        } catch (IOException | InterruptedException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, functionName + " HEAD request failed: host=" + hostname, exception);
            }
            throw new WhatsAppMediaException.Download(functionName + ": network error", exception);
        }
    }

    /**
     * Returns the authentication token presented to the CDN on every
     * upload and download request.
     *
     * @return the authentication token
     * @throws IllegalStateException if the first {@link #update(Stanza)}
     *         has not yet landed
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public String auth() {
        requireInitialized();
        return auth;
    }

    /**
     * Returns the routes time-to-live in seconds.
     *
     * @return the TTL in seconds
     * @throws IllegalStateException if the first {@link #update(Stanza)}
     *         has not yet landed
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob", exports = "queryMediaConn",
            adaptation = WhatsAppAdaptation.DIRECT)
    public int ttl() {
        requireInitialized();
        return ttl;
    }

    /**
     * Returns the authentication token time-to-live in seconds.
     *
     * @return the auth TTL in seconds
     * @throws IllegalStateException if the first {@link #update(Stanza)}
     *         has not yet landed
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob", exports = "queryMediaConn",
            adaptation = WhatsAppAdaptation.DIRECT)
    public int authTtl() {
        requireInitialized();
        return authTtl;
    }

    /**
     * Returns the maximum number of deterministic download buckets.
     *
     * @return the maximum bucket count
     * @throws IllegalStateException if the first {@link #update(Stanza)}
     *         has not yet landed
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public int maxBuckets() {
        requireInitialized();
        return maxBuckets;
    }

    /**
     * Returns the server-advertised budget for manual media download
     * retries.
     *
     * @return the maximum manual retry count
     * @throws IllegalStateException if the first {@link #update(Stanza)}
     *         has not yet landed
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public int maxManualRetry() {
        requireInitialized();
        return maxManualRetry;
    }

    /**
     * Returns the server-advertised budget for automatic media download
     * retries.
     *
     * @return the maximum auto-download retry count
     * @throws IllegalStateException if the first {@link #update(Stanza)}
     *         has not yet landed
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public int maxAutoDownloadRetry() {
        requireInitialized();
        return maxAutoDownloadRetry;
    }

    /**
     * Returns the epoch-second timestamp at which the current credentials
     * were parsed.
     *
     * @return the creation timestamp in epoch seconds
     * @throws IllegalStateException if the first {@link #update(Stanza)}
     *         has not yet landed
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob", exports = "queryMediaConn",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public long timestamp() {
        requireInitialized();
        return timestamp;
    }

    /**
     * Returns the list of CDN host entries available for uploads and
     * downloads.
     *
     * @return an unmodifiable collection of hosts
     * @throws IllegalStateException if the first {@link #update(Stanza)}
     *         has not yet landed
     */
    @WhatsAppWebExport(moduleName = "WAMediaConnParser", exports = "mediaConnParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob", exports = "mapParsedMediaConn",
            adaptation = WhatsAppAdaptation.DIRECT)
    public SequencedCollection<? extends MediaHost> hosts() {
        requireInitialized();
        return hosts;
    }

    /**
     * Tests whether the current authentication token has expired.
     *
     * <p>Returns {@code true} when the service has never been updated, or
     * when the current clock is at or past {@code timestamp + authTtl}.
     * Callers that observe {@code true} must request a fresh
     * {@code media_conn} via {@link #update(Stanza)} before issuing new CDN
     * requests.
     *
     * @return {@code true} if no credentials are published or the auth
     *         token has expired
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHosts", exports = "mediaHosts",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean isExpired() {
        if (initialized.getCount() != 0L) {
            return true;
        }
        return Instant.now().getEpochSecond() >= timestamp + authTtl;
    }

    /**
     * Tests whether the current credentials should be proactively
     * refreshed.
     *
     * <p>Returns {@code true} when the service has never been updated, or
     * when either the routes TTL has elapsed, or 80% of the authentication
     * TTL has elapsed, whichever happens first.
     *
     * @return {@code true} if no credentials are published or refresh is
     *         due
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaHosts", exports = "mediaHosts",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean needsRefresh() {
        if (initialized.getCount() != 0L) {
            return true;
        }
        var now = Instant.now().getEpochSecond();
        if (now >= timestamp + ttl) {
            return true;
        }
        var authRefreshThreshold = (long) Math.floor(authTtl * 0.8);
        return now >= timestamp + authRefreshThreshold;
    }

    /**
     * Commits a {@code MediaUpload2} beacon for a message-media upload that
     * just landed on the CDN.
     *
     * <p>Emitted once from {@link #upload(MediaProvider, MediaPayload)} after
     * the CDN accepts the ciphertext and the provider's media metadata has
     * been written back. External-blob uploads (the app-state syncd external
     * patches) are skipped because their beacon is committed by the app-state
     * exchange pipeline; a second beacon here would double-count the upload.
     *
     * @implNote
     * This implementation collapses WA Web's separate resume, upload, and
     * finalize legs into one {@code overallT} duration because Cobalt streams
     * the whole upload as a single POST rather than the WA Web
     * resume-then-upload-then-finalize sequence; the {@code resumeHttpCode} of
     * {@code 404} and the {@code uploadHttpCode}/{@code finalizeHttpCode} of
     * {@code 200} are synthetic stand-ins for that collapsed sequence.
     * {@code estimatedBandwidth} is the ciphertext byte count over the
     * measured wall-clock duration, {@code overallMediaKeyReuse} is always
     * {@link OverallMediaKeyReuseType#NONE_NEW_CONTENT} because a fresh media
     * key is minted per upload, and {@code overallUploadOrigin} defaults to
     * {@link UploadOriginType#CHAT_PERSONAL} since the chat-agnostic media
     * layer cannot observe the destination thread class.
     *
     * @param provider      the uploaded media provider
     * @param hostname      the CDN hostname that accepted the upload
     * @param originalSize  the plaintext media size in bytes
     * @param encryptedSize the ciphertext size in bytes transferred
     * @param attemptCount  the zero-based index of the successful attempt
     * @param uploadStart   the instant the upload began
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaMmsV4Upload", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCreateMediaUploadMetrics", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMessageMediaUploadSuccess(MediaProvider provider, String hostname,
                                                 long originalSize, long encryptedSize,
                                                 int attemptCount, Instant uploadStart) {
        if (provider instanceof ExternalBlobReference) {
            return;
        }
        var elapsed = Duration.between(uploadStart, Instant.now());
        var overallT = Instant.ofEpochMilli(elapsed.toMillis());
        var seconds = Math.max(elapsed.toMillis(), 1L) / 1000.0;
        wamService.commit(new MediaUpload2EventBuilder()
                .overallMediaType(wamMediaType(provider.mediaPath()))
                .overallMmsVersion(4)
                .overallUploadOrigin(UploadOriginType.CHAT_PERSONAL)
                .overallUploadMode(MediaUploadModeType.REGULAR)
                .overallUploadResult(MediaUploadResultType.OK)
                .overallMediaKeyReuse(OverallMediaKeyReuseType.NONE_NEW_CONTENT)
                .overallIsFinal(Boolean.TRUE)
                .overallIsManual(Boolean.FALSE)
                .overallIsForward(Boolean.FALSE)
                .overallDomain(hostname)
                .connectionType(ConnectionType.HOSTNAME)
                .httpProtocolVersionType(HttpProtocolVersionType.HTTP2)
                .uploadSource(UploadSourceType.OTHER)
                .originalSize(originalSize)
                .overallMediaSize((double) encryptedSize)
                .uploadBytesTransferred((double) encryptedSize)
                .estimatedBandwidth(encryptedSize / seconds)
                .mediaId(generateMediaId())
                .resumeHttpCode(404)
                .uploadHttpCode(200)
                .finalizeHttpCode(200)
                .overallT(overallT)
                .overallAttemptCount(attemptCount + 1L)
                .overallRetryCount(attemptCount)
                .build());
    }

    /**
     * Commits a {@code MediaUpload2} beacon for a message-media upload that
     * aborted.
     *
     * <p>Emitted from {@link #upload(MediaProvider, MediaPayload)} on the
     * non-retryable and host-exhaustion failure paths. The
     * {@link MediaUploadResultType} bucket is chosen by
     * {@link #classifyUploadResult(Throwable)} and, when the error carries an
     * HTTP status code, that code is stamped on the upload and finalize legs.
     * External-blob uploads are skipped for the same reason as
     * {@link #commitMessageMediaUploadSuccess}.
     *
     * @param provider     the media provider whose upload failed
     * @param originalSize the plaintext media size in bytes
     * @param attemptCount the zero-based index of the last attempt made
     * @param uploadStart  the instant the upload began
     * @param throwable    the error that aborted the upload
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaMmsV4Upload", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCreateMediaUploadMetrics", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMessageMediaUploadFailure(MediaProvider provider, long originalSize,
                                                 int attemptCount, Instant uploadStart,
                                                 Throwable throwable) {
        if (provider instanceof ExternalBlobReference) {
            return;
        }
        var overallT = Instant.ofEpochMilli(Duration.between(uploadStart, Instant.now()).toMillis());
        var builder = new MediaUpload2EventBuilder()
                .overallMediaType(wamMediaType(provider.mediaPath()))
                .overallMmsVersion(4)
                .overallUploadOrigin(UploadOriginType.CHAT_PERSONAL)
                .overallUploadMode(MediaUploadModeType.REGULAR)
                .overallUploadResult(classifyUploadResult(throwable))
                .overallMediaKeyReuse(OverallMediaKeyReuseType.NONE_NEW_CONTENT)
                .overallIsFinal(Boolean.TRUE)
                .overallIsManual(Boolean.FALSE)
                .overallIsForward(Boolean.FALSE)
                .connectionType(ConnectionType.HOSTNAME)
                .uploadSource(UploadSourceType.OTHER)
                .originalSize(originalSize)
                .mediaId(generateMediaId())
                .overallT(overallT)
                .overallAttemptCount(attemptCount + 1L)
                .overallRetryCount(attemptCount)
                .debugMediaException(throwable.getMessage());
        if (throwable instanceof WhatsAppMediaException.Upload uploadException) {
            var statusCode = uploadException.httpStatusCode();
            if (statusCode.isPresent()) {
                builder.uploadHttpCode(statusCode.getAsInt());
                builder.finalizeHttpCode(statusCode.getAsInt());
            }
        }
        wamService.commit(builder.build());
    }

    /**
     * Classifies an upload error into a {@link MediaUploadResultType} bucket
     * for the {@code MediaUpload2} failure beacon.
     *
     * <p>Routes {@link WhatsAppMediaException.Upload} instances by HTTP status
     * code ({@code 401}, {@code 413}, {@code 415}, {@code 507}, and the
     * {@code 5xx} family) and every status-less or otherwise unmapped media
     * error to the generic upload bucket; an {@link InterruptedException}
     * yields the cancel bucket.
     *
     * @param throwable the error that aborted the upload
     * @return the matching {@link MediaUploadResultType}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getMetricUploadErrorResultType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static MediaUploadResultType classifyUploadResult(Throwable throwable) {
        if (throwable instanceof InterruptedException) {
            return MediaUploadResultType.ERROR_CANCEL;
        }
        if (throwable instanceof WhatsAppMediaException.Upload uploadException) {
            var statusCode = uploadException.httpStatusCode();
            if (statusCode.isPresent()) {
                return switch (statusCode.getAsInt()) {
                    case 401 -> MediaUploadResultType.ERROR_REQUEST;
                    case 413 -> MediaUploadResultType.ERROR_TOO_LARGE;
                    case 415 -> MediaUploadResultType.ERROR_BAD_MEDIA;
                    case 507 -> MediaUploadResultType.ERROR_THROTTLE;
                    default -> statusCode.getAsInt() >= 500
                            ? MediaUploadResultType.ERROR_SERVER
                            : MediaUploadResultType.ERROR_UPLOAD;
                };
            }
        }
        return MediaUploadResultType.ERROR_UPLOAD;
    }

    /**
     * Commits a {@code MediaDownload2} beacon for a message-media download
     * that just completed successfully.
     *
     * <p>Emitted once from {@link #download(MediaProvider)} after
     * {@link #performDownload(MediaProvider)} hands back the decrypted media
     * stream, mirroring the {@code MediaUpload2} beacon committed on the upload
     * side so the two media-transfer legs are symmetric. The overall result is
     * fixed to {@link MediaDownloadResultType#OK}, the HTTP code to
     * {@code 200}, and the transferred-byte and bandwidth figures are derived
     * from the provider's decoded media size.
     *
     * @implNote
     * This implementation reports the transfer as a single {@code overallT}
     * leg because Cobalt streams the whole download as one GET rather than WA
     * Web's queue-connect-network-decrypt timeline; {@code overallMediaSize}
     * and {@code downloadBytesTransferred} both carry the provider's decoded
     * media size, {@code estimatedBandwidth} is that size over the measured
     * wall-clock duration, and {@code overallBackendStore} is derived from the
     * direct path via
     * {@link MediaMetricUtils#getMetricBackendStore(String)}.
     * {@code overallDownloadMode} defaults to
     * {@link MediaDownloadModeType#FULL} and {@code overallDownloadOrigin} to
     * {@link DownloadOriginType#CHAT_PERSONAL} because the chat-agnostic media
     * layer observes neither the download reason nor the destination thread
     * class. The {@code overallAttemptCount} of {@code 1} and
     * {@code overallRetryCount} of {@code 0} are synthetic stand-ins since the
     * retry bookkeeping lives inside {@link #performDownload(MediaProvider)}
     * and is not surfaced to this commit point.
     *
     * @param provider      the downloaded media provider
     * @param downloadStart the instant the download began
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaMmsV4Download", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCreateMediaDownloadMetrics",
            exports = "createMediaDownloadMetrics",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMessageMediaDownloadSuccess(MediaProvider provider, Instant downloadStart) {
        var elapsed = Duration.between(downloadStart, Instant.now());
        var overallT = Instant.ofEpochMilli(elapsed.toMillis());
        var seconds = Math.max(elapsed.toMillis(), 1L) / 1000.0;
        var builder = new MediaDownload2EventBuilder()
                .overallMediaType(wamMediaType(provider.mediaPath()))
                .overallMmsVersion(4)
                .overallDownloadOrigin(DownloadOriginType.CHAT_PERSONAL)
                .overallDownloadMode(MediaDownloadModeType.FULL)
                .overallDownloadResult(MediaDownloadResultType.OK)
                .overallIsFinal(Boolean.TRUE)
                .connectionType(ConnectionType.HOSTNAME)
                .httpProtocolVersionType(HttpProtocolVersionType.HTTP2)
                .mediaId(generateMediaId())
                .downloadHttpCode(200)
                .overallT(overallT)
                .overallAttemptCount(1L)
                .overallRetryCount(0L);
        var backendStore = MediaMetricUtils.getMetricBackendStore(provider.mediaDirectPath().orElse(null));
        if (backendStore != null) {
            builder.overallBackendStore(backendStore);
        }
        var mediaSize = provider.mediaSize();
        if (mediaSize.isPresent()) {
            var size = mediaSize.getAsLong();
            builder.overallMediaSize((double) size)
                    .downloadBytesTransferred((double) size)
                    .estimatedBandwidth(size / seconds);
        }
        wamService.commit(builder.build());
    }

    /**
     * Commits a {@code MediaDownload2} beacon for a message-media download
     * that aborted.
     *
     * <p>Emitted from {@link #download(MediaProvider)} on the non-retryable
     * and host-exhaustion failure paths, mirroring the {@code MediaUpload2}
     * failure beacon. The {@link MediaDownloadResultType} bucket is chosen by
     * {@link MediaMetricUtils#getMetricDownloadErrorResultType(Throwable)} and,
     * when the error carries an HTTP status code, that code is stamped on the
     * download leg.
     *
     * @implNote
     * This implementation routes the error classification and HTTP status
     * extraction through {@link MediaMetricUtils} so the failure bucket matches
     * the one WA Web would have picked; {@code overallDownloadMode},
     * {@code overallDownloadOrigin}, {@code overallBackendStore},
     * {@code overallAttemptCount}, and {@code overallRetryCount} carry the same
     * defaults as
     * {@link #commitMessageMediaDownloadSuccess(MediaProvider, Instant)}.
     *
     * @param provider      the media provider whose download failed
     * @param downloadStart the instant the download began
     * @param throwable     the error that aborted the download
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaMmsV4Download", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCreateMediaDownloadMetrics",
            exports = "createMediaDownloadMetrics",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMessageMediaDownloadFailure(MediaProvider provider, Instant downloadStart,
                                                   Throwable throwable) {
        var overallT = Instant.ofEpochMilli(Duration.between(downloadStart, Instant.now()).toMillis());
        var builder = new MediaDownload2EventBuilder()
                .overallMediaType(wamMediaType(provider.mediaPath()))
                .overallMmsVersion(4)
                .overallDownloadOrigin(DownloadOriginType.CHAT_PERSONAL)
                .overallDownloadMode(MediaDownloadModeType.FULL)
                .overallDownloadResult(MediaMetricUtils.getMetricDownloadErrorResultType(throwable))
                .overallIsFinal(Boolean.TRUE)
                .connectionType(ConnectionType.HOSTNAME)
                .mediaId(generateMediaId())
                .overallT(overallT)
                .overallAttemptCount(1L)
                .overallRetryCount(0L)
                .debugMediaException(throwable.getMessage());
        var backendStore = MediaMetricUtils.getMetricBackendStore(provider.mediaDirectPath().orElse(null));
        if (backendStore != null) {
            builder.overallBackendStore(backendStore);
        }
        var statusCode = MediaMetricUtils.getStatusCode(throwable);
        if (statusCode != null) {
            builder.downloadHttpCode(statusCode);
        }
        wamService.commit(builder.build());
    }

    /**
     * Commits a {@code WebcMediaRmr} beacon for a media download that just
     * completed or failed.
     *
     * <p>Emitted once per {@link #download(MediaProvider)} call. WhatsApp Web
     * models every media download as a retry-media-request (RMR) round trip to
     * the primary device, so the beacon records the elapsed RMR timer, the
     * observed HTTP status code, the media type and size, and whether the
     * download failed.
     *
     * @implNote
     * This implementation reports the effective network type as {@code "4g"}
     * and derives the browser storage-quota figures from the host filesystem
     * because Cobalt is a headless library with no {@code navigator.connection}
     * or {@code navigator.storage} surface to sample. The chat-viewport fields
     * WA Web sources from the on-screen message list ({@code webcChatPosition},
     * {@code webcMessageIndex}) are derived deterministically from the
     * provider's encrypted-hash identity so the values are stable per media
     * rather than random per commit, and {@code webcRmrReason} is always
     * {@link WebcRmrReasonCode#OTHER} because the media layer receives no
     * user-intent hint.
     *
     * @param provider   the downloaded media provider
     * @param rmrStart   the instant the download began
     * @param error      whether the download failed
     * @param statusCode the HTTP status code observed, when available
     */
    @WhatsAppWebExport(moduleName = "WAWebDownloadManager", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitWebcMediaRmr(MediaProvider provider, Instant rmrStart,
                                    boolean error, OptionalInt statusCode) {
        var elapsed = Instant.ofEpochMilli(Duration.between(rmrStart, Instant.now()).toMillis());
        var mediaIdentity = provider.mediaEncryptedSha256()
                .or(provider::mediaSha256)
                .map(Arrays::hashCode)
                .orElse(0);
        var builder = new WebcMediaRmrEventBuilder()
                .messageMediaType(wamMediaType(provider.mediaPath()))
                .webcMediaRmrError(error)
                .webcMediaRmrT(elapsed)
                .webcRmrReason(WebcRmrReasonCode.OTHER)
                .webcBrowserNetworkType("4g")
                .webcBrowserStorageQuotaBytes(browserStorageQuotaBytes())
                .webcBrowserStorageQuotaUsedBytes(browserStorageUsedBytes())
                .webcChatType(WebcChatType.INDIVIDUAL)
                .webcChatPosition(Math.floorMod(mediaIdentity, 32) + 1L)
                .webcMessageIndex(Math.floorMod(mediaIdentity, 512) + 1L);
        provider.mediaSize().ifPresent(builder::webcMediaSize);
        statusCode.ifPresent(code -> builder.webcRmrStatusCode(code));
        wamService.commit(builder.build());
    }

    /**
     * Commits a {@code StickerError} beacon recording a sticker download or
     * decode failure.
     *
     * <p>Emitted from {@link #download(MediaProvider)} when a sticker-type
     * media download fails, mirroring WA Web's {@code WAWebMediaMmsV4Download}
     * catch block that commits {@link StickerErrorType#DECOMPRESSION} when the
     * downloaded Lottie payload cannot be extracted.
     *
     * @param type the sticker error classification
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaMmsV4Download", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitStickerError(StickerErrorType type) {
        wamService.commit(new StickerErrorEventBuilder()
                .stickerErrorType(type)
                .build());
    }

    /**
     * Maps a {@link MediaPath} routing classification to the WAM
     * {@link MediaType} enum reported on media-transfer beacons.
     *
     * @param path the media routing classification
     * @return the corresponding WAM media type, or {@link MediaType#NONE}
     *         when the path has no telemetry counterpart
     */
    @WhatsAppWebExport(moduleName = "WAServerMediaType", exports = "castToServerMediaType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static MediaType wamMediaType(MediaPath path) {
        return switch (path) {
            case IMAGE, PRODUCT, THUMBNAIL_IMAGE, THUMBNAIL_LINK, NEWSLETTER_IMAGE,
                 NEWSLETTER_THUMBNAIL_LINK, XMA_IMAGE, WAFFLE_IMAGE, NATIVE_AD_IMAGE,
                 PRODUCT_CATALOG_IMAGE, PREVIEW -> MediaType.PHOTO;
            case VIDEO, THUMBNAIL_VIDEO, NEWSLETTER_VIDEO, NATIVE_AD_VIDEO, WAFFLE_VIDEO -> MediaType.VIDEO;
            case GIF, THUMBNAIL_GIF, NEWSLETTER_GIF -> MediaType.GIF;
            case AUDIO, NEWSLETTER_AUDIO -> MediaType.AUDIO;
            case VOICE, NEWSLETTER_VOICE -> MediaType.PTT;
            case DOCUMENT, THUMBNAIL_DOCUMENT, NEWSLETTER_DOCUMENT, MAIBA_FILE -> MediaType.DOCUMENT;
            case STICKER, NEWSLETTER_STICKER -> MediaType.STICKER;
            case STICKER_PACK, THUMBNAIL_STICKER_PACK, NEWSLETTER_STICKER_PACK -> MediaType.STICKER_PACK;
            case PROFILE_PICTURE, BUSINESS_COVER_PHOTO -> MediaType.PROFILE_PIC;
            case APP_STATE -> MediaType.MD_APP_STATE;
            case HISTORY_SYNC -> MediaType.MD_HISTORY_SYNC;
            case GROUP_HISTORY -> MediaType.GROUP_HISTORY;
            case PTV, NEWSLETTER_PTV -> MediaType.PUSH_TO_VIDEO;
            case MUSIC_ARTWORK, NEWSLETTER_MUSIC_ARTWORK -> MediaType.MUSIC_ARTWORK;
            default -> MediaType.NONE;
        };
    }

    /**
     * Tests whether a media routing classification denotes sticker content.
     *
     * @param path the media routing classification
     * @return {@code true} when the path is a sticker or sticker-pack type
     */
    private static boolean isStickerMedia(MediaPath path) {
        return switch (path) {
            case STICKER, NEWSLETTER_STICKER, STICKER_PACK, THUMBNAIL_STICKER_PACK,
                 NEWSLETTER_STICKER_PACK -> true;
            default -> false;
        };
    }

    /**
     * Estimates the browser storage quota in bytes from the host filesystem.
     *
     * <p>Stand-in for WA Web's {@code navigator.storage.estimate().quota}.
     * Cobalt has no browser storage manager, so the value is derived from the
     * total capacity of the volume backing the user's home directory, scaled
     * by the same fraction Chromium reserves for per-origin quota.
     *
     * @return the estimated storage quota in bytes
     */
    @WhatsAppWebExport(moduleName = "WAWebBrowserApi", exports = "getStorageQuota",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static long browserStorageQuotaBytes() {
        var total = new File(System.getProperty("user.home", ".")).getTotalSpace();
        return total > 0 ? (long) (total * 0.6) : 10L * 1024 * 1024 * 1024;
    }

    /**
     * Estimates the used browser storage in bytes from the host filesystem.
     *
     * <p>Stand-in for WA Web's {@code navigator.storage.estimate().usage}.
     * Derived from the used capacity (total minus usable) of the volume
     * backing the user's home directory.
     *
     * @return the estimated used storage in bytes
     */
    @WhatsAppWebExport(moduleName = "WAWebBrowserApi", exports = "getStorageUsage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static long browserStorageUsedBytes() {
        var home = new File(System.getProperty("user.home", "."));
        var used = home.getTotalSpace() - home.getUsableSpace();
        return used > 0 ? used : 1L;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * Prints either the placeholder {@code LiveMediaConnectionService[uninitialized]}
     * when no {@link #update(Stanza)} has landed, or every field of the
     * current credentials. Do not log the result in production builds:
     * the {@code auth} token is included.
     */
    @Override
    public String toString() {
        if (initialized.getCount() != 0L) {
            return "LiveMediaConnectionService[uninitialized]";
        }
        return "LiveMediaConnectionService[" +
               "auth=" + auth + ", " +
               "ttl=" + ttl + ", " +
               "authTtl=" + authTtl + ", " +
               "maxBuckets=" + maxBuckets + ", " +
               "maxManualRetry=" + maxManualRetry + ", " +
               "maxAutoDownloadRetry=" + maxAutoDownloadRetry + ", " +
               "timestamp=" + timestamp + ", " +
               "hosts=" + hosts + ']';
    }
}

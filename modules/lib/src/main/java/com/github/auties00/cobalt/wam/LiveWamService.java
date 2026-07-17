package com.github.auties00.cobalt.wam;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.util.ScheduledTask;
import com.github.auties00.cobalt.wam.binary.WamEventDecoder;
import com.github.auties00.cobalt.wam.binary.WamEventEncoder;
import com.github.auties00.cobalt.wam.binary.WamGlobalEncoder;
import com.github.auties00.cobalt.wire.wam.event.PsIdUpdateEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WamClientErrorsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WamDroppedEventEvent;
import com.github.auties00.cobalt.wire.wam.event.WamDroppedEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WamEventRegistry;
import com.github.auties00.cobalt.wire.wam.event.WebWamForceFlushEvent;
import com.github.auties00.cobalt.wire.wam.event.WebWamForceFlushEventBuilder;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.privatestats.WamPrivateStatsId;
import com.github.auties00.cobalt.wam.privatestats.WamPrivateStatsTokenIssuer;
import com.github.auties00.cobalt.wam.privatestats.WamPrivateStatsUploader;
import com.github.auties00.cobalt.wire.wam.type.*;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.lang.System.Logger.Level;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collects, batches, and uploads WhatsApp Metrics (WAM) telemetry
 * events across the regular, realtime, and private transport channels.
 *
 * <p>The lifecycle is two-phase. Construction wires the dependencies
 * but leaves {@code initialized} {@code false}; events committed in this
 * phase are deferred to an internal init queue, mirroring WA Web's
 * subclass-side fallback when
 * {@code WAWebWamRuntimeProvider.getWamRuntime()} returns {@code null}.
 * The first call to {@link #initialize()} snapshots session globals
 * from the bound store, loads sampling overrides from
 * {@link ABPropsService}, drains the init queue, and arms the two
 * recurring schedulers (five-second mid-cycle check, 120-second flush).
 * The {@link #close()} call cancels both schedulers and performs a
 * final {@link #flush()}.
 *
 * <p>During the active phase, every {@link #commit(WamEventSpec)} runs
 * the per-event validator, applies the effective sampling weight
 * (runtime override first, static {@link WamEventSpec#releaseWeight()}
 * second), and appends the kept event to the pending list keyed by
 * {@link WamChannel}. {@link WamChannel#REALTIME} events additionally
 * spawn a virtual-thread {@link #flushChannel(WamChannel)} so they
 * surface within one event-loop tick instead of waiting for the next
 * rotation. Regular and private events accumulate until either the
 * 120-second timer fires or {@link #checkMidCycleUpload()} observes
 * a pending list whose encoded size has crossed {@link #MAX_BUFFER_SIZE}.
 *
 * <p>On flush, events drain through {@link #swapPending(WamChannel)},
 * are partitioned into buffers capped at {@link #MAX_BUFFER_SIZE},
 * encoded with a {@link #HEADER_SIZE}-byte WAM header plus the dirty
 * session globals computed by {@link #encodeGlobals(WamChannel)}, and
 * dispatched: the regular and realtime channels go through an XMPP
 * {@code <iq xmlns="w:stats">} stanza, while private buffers go through
 * the HTTP-only {@link WamPrivateStatsUploader} which performs the
 * Ed25519 blinded-token VOPRF round-trip. Both transports retry up to
 * {@link #MAX_RETRIES} times on {@code 5xx}-equivalent failures with
 * exponential backoff; permanent failures account a
 * {@code WamClientErrors} drop-counter event.
 *
 * <p>The client constructs and drives this service. Its direct entry
 * points are the sampling-override methods
 * ({@link #setSamplingOverride(int, int)},
 * {@link #removeSamplingOverride(int)}, {@link #replaceSamplingOverrides(Map)})
 * for overriding WA Web sampling weights at runtime. The stateless WAM
 * message-classification helpers that other Cobalt modules use to tag
 * their events now live in {@link WamMsgUtils}.
 *
 * @implNote
 * This implementation flushes unconditionally on every scheduler tick.
 * WA Web gates each tick on
 * {@code WAWebUserPrefsTabMutex.currentTabHasMutex()} so a background
 * tab skips WAM processing in favour of the foreground tab; Cobalt has
 * no tab concept and is always the sole writer. The
 * {@code WAWebWamFalcoLogger} shadow-logger pipeline (a parallel A/B
 * sink) is also not run.
 *
 * @see WamEventSpec
 * @see WamGlobalEncoder
 * @see WamChannel
 * @see WamPrivateStatsUploader
 */
@WhatsAppWebModule(moduleName = "WAWebWam")
@WhatsAppWebModule(moduleName = "WAWebWamCodegenWamEvent")
@WhatsAppWebModule(moduleName = "WAWebL10NCountryCodes")
@WhatsAppWebModule(moduleName = "WAWebBrowserApi")
@WhatsAppWebModule(moduleName = "WAWebProcessRawMediaLogging")
public class LiveWamService implements WamService {
    /**
     * The logger for {@link LiveWamService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveWamService.class);

    /**
     * Holds the size in bytes of the fixed-shape WAM buffer header that
     * {@link #writeHeader(WamEventEncoder, WamChannel)} prepends to
     * every flushed buffer.
     *
     * <p>The wire shape is {@code "WAM"(3) + version(1) + streamId(1) +
     * seqNum(2 LE) + channel(1)}, so the magic plus the four scalar
     * header fields together occupy exactly eight bytes.
     */
    private static final int HEADER_SIZE = 8;

    /**
     * Holds the three-byte magic prefix that opens every WAM buffer.
     *
     * <p>The server reads this to recognise WAM-formatted payloads on the
     * {@code w:stats} IQ surface and on the
     * {@code /deidentified_telemetry} private endpoint.
     */
    private static final byte[] WAM_MAGIC = {'W', 'A', 'M'};

    /**
     * Holds the protocol version byte written into the WAM buffer header.
     *
     * <p>Matches the WA Web {@code WAM_PROTOCOL_VERSION} constant; bumped
     * in lockstep with the upstream encoder when the wire shape
     * changes.
     */
    private static final int PROTOCOL_VERSION = 5;

    /**
     * Holds the stream identifier byte written into the WAM buffer header.
     *
     * <p>Always {@code 1} for the regular client stream; the WAM wire
     * format reserves higher stream ids for parallel encoder pipelines
     * that Cobalt does not run.
     */
    private static final int STREAM_ID = 1;

    /**
     * Holds the interval in seconds between mid-cycle serialization checks.
     *
     * <p>Matches the upstream {@code WAM_IN_MEMORY_BUFFERING_DURATION_IN_SECS}
     * constant. {@link #checkMidCycleUpload()} runs on this cadence and
     * triggers an early flush for any non-realtime channel whose
     * pending aggregate has crossed {@link #MAX_BUFFER_SIZE}.
     */
    private static final int SERIALIZE_INTERVAL_SECONDS = 5;

    /**
     * Holds the interval in seconds between full rotation and upload cycles.
     *
     * <p>Matches the upstream {@code WAM_BUFFER_ROTATE_INTERVAL_IN_SECS}
     * constant. {@link #flush()} runs on this cadence and drains every
     * channel unconditionally.
     */
    private static final int FLUSH_INTERVAL_SECONDS = 120;

    /**
     * Holds the size in bytes at which a buffer is closed off and a fresh
     * buffer is started during {@link #flushEventList}.
     *
     * <p>Matches the upstream {@code WAM_MAX_BUFFER_SIZE} constant. The
     * rotation is a post-check: the event that pushes the buffer over
     * the threshold stays in the current buffer, and any subsequent
     * event opens a new buffer.
     */
    private static final int MAX_BUFFER_SIZE = 50_000;

    /**
     * Holds the hard upper size in bytes for a buffer that may be uploaded.
     *
     * <p>Matches the upstream {@code WAM_MAX_BUFFER_SIZE_FOR_UPLOAD}
     * constant. Buffers above this size are dropped before any network
     * call and a {@code WamClientErrors} drop-counter event is
     * accounted for the loss.
     */
    private static final int MAX_UPLOAD_SIZE = 64_000;

    /**
     * Holds the maximum number of retry attempts for a failed buffer upload.
     *
     * <p>This permits three total attempts (the initial send plus two retries)
     * on transient {@code 5xx}-class failures; permanent failures (the
     * {@code 4xx} family on the regular channel, the per-result
     * permanent error codes on the private channel) terminate
     * immediately.
     */
    private static final int MAX_RETRIES = 2;

    /**
     * Holds the minimum delay in milliseconds between successive upload
     * retries.
     *
     * <p>Acts as the lower clamp of the exponential-backoff curve computed by
     * {@link #computeBackoffDelay(int)}. Matches the
     * {@code WABackoffUtils.expBackoff} third argument in WA Web's
     * {@code WAWebUploadStatsBackend} call site.
     */
    private static final long RETRY_BASE_DELAY_MS = 1_000;

    /**
     * Holds the maximum delay in milliseconds between successive upload
     * retries.
     *
     * <p>Acts as the upper clamp of the exponential-backoff curve computed by
     * {@link #computeBackoffDelay(int)}. Matches the
     * {@code WABackoffUtils.expBackoff} second argument in WA Web's
     * {@code WAWebUploadStatsBackend} call site.
     */
    private static final long RETRY_MAX_DELAY_MS = 120_000;

    /**
     * Holds the inclusive upper bound of the {@code uint16} sequence number
     * written into the buffer header before the per-channel counter
     * wraps back to {@code 1}.
     *
     * <p>The wire field is a little-endian {@code uint16}; the counter
     * skips {@code 0} on wrap so that the server can use {@code 0} as
     * an unset sentinel.
     */
    private static final int MAX_SEQUENCE_NUMBER = 0xFFFF;

    /**
     * Holds the wire field id of the per-event {@code commitTime} global.
     *
     * <p>{@link #restorePendingBuffers()} uses this field id to recognise
     * the leading entry that {@link #persistBuffer} writes before each
     * persisted event; a mismatch signals a corrupt or out-of-date
     * persisted buffer and aborts the restore loop for that file.
     */
    private static final int COMMIT_TIME_FIELD_ID = 47;

    /**
     * Holds the wire value of the {@code deviceClassification} global
     * (id {@code 14507}) reported by Cobalt as DESKTOP.
     *
     * @implNote
     * This implementation always reports DESKTOP regardless of host
     * environment because there is no equivalent of WA Web's
     * browser/native build fork;
     * {@code WAWebFalcoCanonicalDeviceClassification.default} returns the
     * literal {@code "desktop"} only on the desktop binary.
     */
    @WhatsAppWebExport(moduleName = "WAWebFalcoCanonicalDeviceClassification", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    private static final int DEVICE_CLASSIFICATION_DESKTOP = 4;

    /**
     * Holds the wire value of the {@code appBuild} global (id {@code 1657})
     * reported by Cobalt as RELEASE.
     *
     * <p>Matches WA Web's {@code APP_BUILD_TYPE.RELEASE} enum value;
     * Cobalt has no debug or canary build distinction at runtime so the
     * value is fixed.
     */
    private static final int APP_BUILD_RELEASE = 4;

    /**
     * Holds the wire value of the {@code webcEnv} global (id {@code 633})
     * reported by Cobalt as PROD.
     *
     * <p>Matches WA Web's {@code WEBC_ENV_CODE.PROD} enum value; Cobalt
     * has no staging or test environment to report.
     */
    private static final int WEBC_ENV_PROD = 0;

    /**
     * Holds the wire value of the {@code webcWebPlatform} global
     * (id {@code 899}) reported by Cobalt as WEB.
     *
     * <p>Matches WA Web's {@code WEBC_WEB_PLATFORM_TYPE.WEB} enum value;
     * other values in the enum tag the upstream native-shell builds
     * that Cobalt does not embed.
     */
    private static final int PLATFORM_WEBCLIENT = 1;

    /**
     * Holds the maximum time in milliseconds that
     * {@link #waitIfDisconnected()} blocks waiting for the client to
     * become connected before letting the upload attempt proceed
     * regardless.
     *
     * <p>Matches WA Web's {@code WAWebUploadStatsBackend.waitIfOffline}
     * 30-second timeout: the upload still fires after the deadline
     * elapses so the retry and backoff loop drives the actual failure
     * handling rather than the connectivity wait.
     */
    private static final long CONNECTIVITY_WAIT_TIMEOUT_MS = 30_000;

    /**
     * Holds the bound WhatsApp client used to dispatch the {@code w:stats}
     * IQ stanzas and to read connectivity state for
     * {@link #waitIfDisconnected()}.
     */
    private final LinkedWhatsAppClient client;

    /**
     * Holds the AB-props service queried at {@link #initialize()} for
     * per-event sampling-weight overrides and for the
     * {@link ABProp#WAM_DISABLE_ABKEY_ATTRIBUTE} and
     * {@link ABProp#SERVICE_IMPROVEMENT_OPT_OUT_FLAG} feature flags
     * that gate the {@code abKey2} and {@code serviceImprovementOptOut}
     * globals.
     */
    private final ABPropsService abPropsService;

    /**
     * Holds the per-channel pending-event queues keyed by {@link WamChannel}.
     *
     * <p>Each value is the FIFO list of uncommitted events for that
     * channel; {@link #swapPending(WamChannel)} atomically detaches the
     * list inside a {@link ConcurrentMap#compute} so that concurrent
     * {@link #commit(WamEventSpec)} callers either land in the
     * pre-swap list or the post-swap fresh list, never split between
     * them.
     */
    private final ConcurrentMap<WamChannel, List<WamPendingEvent>> pending;

    /**
     * Holds the per-channel sequence counters written into the WAM buffer
     * header.
     *
     * <p>Each channel has its own counter, mirroring WA Web's
     * {@code WAWebWamStorage.getNextSequenceNumberForStream} which
     * keys per stream id. {@link #nextSequenceNumber(WamChannel)}
     * advances by one on every flush and wraps from
     * {@link #MAX_SEQUENCE_NUMBER} back to {@code 1} (not {@code 0}).
     */
    private final Map<WamChannel, AtomicInteger> sequenceNumbers;

    /**
     * Holds the daily-sampled beaconing state consulted by
     * {@link #flushEventList} to mint the per-event
     * {@code beaconSessionId} global (id {@code 18529}).
     *
     * <p>{@link WamBeaconingService#nextSequenceNumber(String)} returns an empty
     * {@link OptionalLong} when the buffer-key bucket is unsampled for
     * the day, in which case the per-event global is omitted.
     */
    private final WamBeaconingService beaconing;

    /**
     * Holds the rotating-pseudonymous-id set queried for every
     * private-channel event to mint the per-event {@code psId} global.
     *
     * <p>There is one id per bucket name from WA Web's
     * {@code WAWebWamGlobals.PrivateStatsAllIds} (DefaultPsId,
     * GroupSafetyCheckId, IdTtlDaily, and so on); each id rotates on its
     * configured period and the rotation is reported as a
     * {@code PsIdUpdateEvent} during {@link #flushChannel(WamChannel)}.
     */
    private final WamPrivateStatsId privateStatsId;

    /**
     * Holds the HTTP uploader used by {@link #sendPrivateWithRetry(byte[])}
     * for private-channel buffers.
     *
     * <p>It performs the Ed25519 blinded-token VOPRF round-trip
     * ({@code <sign_credential>} IQ) followed by the multipart POST to
     * {@code https://dit.whatsapp.net/deidentified_telemetry}, isolating
     * the private channel from the {@code w:stats} IQ pipeline.
     */
    private final WamPrivateStatsUploader privateStatsUploader;

    /**
     * Holds the runtime sampling-weight override map consulted by
     * {@link #effectiveWeight(WamEventSpec)} before falling back to the
     * static {@link WamEventSpec#releaseWeight()}.
     *
     * <p>It is seeded at {@link #initialize()} from
     * {@link ABPropsService#samplingConfigs()} (matching WA Web's
     * {@code WAWebEventSampling.getClientEventSamplingWeight}) and
     * mutated at runtime via the public sampling-override API.
     */
    private final WamSamplingOverride samplingOverride;

    /**
     * Holds the per-channel snapshot of the globals written on the previous
     * flush, consulted by {@link #encodeGlobals(WamChannel)} to skip
     * unchanged globals on subsequent flushes for the same channel.
     *
     * <p>This mirrors WA Web's per-{@code WamContext} {@code prevGlobals}
     * dirty-tracking: the first flush for a channel writes every
     * global, subsequent flushes write only the values that have
     * changed plus any field that has transitioned from a value to
     * {@code null}.
     */
    private final Map<WamChannel, Map<Integer, Object>> prevSessionGlobals;

    /**
     * Holds the pre-init action queue drained by
     * {@link #drainInitQueue()} on the first call to
     * {@link #initialize()}.
     *
     * <p>Pre-init commits ({@link #commit(WamEventSpec)} and
     * {@link #commitAndWaitForFlush(WamEventSpec)}) push a re-invocation
     * lambda here rather than entering the pending list, mirroring WA
     * Web's {@code WAWebWamInitQueue.queueEvent} fallback used when
     * {@code WAWebWamRuntimeProvider.getWamRuntime()} returns
     * {@code null}.
     */
    private final ConcurrentLinkedQueue<Runnable> initQueue;

    /**
     * Holds the {@link ScheduledTask} handles returned by
     * {@link #scheduleRecurring(Runnable, long, long)}, tracked so
     * {@link #cancelAllScheduled()} can cancel them all in one pass.
     */
    private final List<ScheduledTask> futures;

    /**
     * Holds the initialization gate that switches the
     * {@link #commit(WamEventSpec)} dispatch from deferral-into-init-queue
     * to direct-into-pending.
     *
     * <p>It is set {@code true} by {@link #initialize()} as the
     * second-to-last step (before the queue drain) and cleared by
     * {@link #close()}.
     */
    private volatile boolean initialized;

    /**
     * Holds the wire value of the {@code platform} global (id {@code 11}).
     *
     * <p>It is {@code 8L} when the bound client is
     * {@link LinkedWhatsAppClientType#WEB}, {@code 2L} otherwise; matches WA
     * Web's {@code WAWebWamPlatform.getWamPlatform()} indirection
     * through the {@code PLATFORM_TYPE} enum.
     */
    private volatile long platform;

    /**
     * Holds the wire value of the {@code appVersion} global
     * (id {@code 17}).
     *
     * <p>It is the stringified
     * {@link com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion}
     * captured at {@link #initialize()}; mirrors WA Web's
     * {@code WAWebBuildConstants.VERSION_BASE_WITH_WINDOWS_BUILD}.
     */
    private volatile String appVersion;

    /**
     * Holds the wire value of the {@code deviceName} global
     * (id {@code 13}).
     *
     * <p>It is sourced from
     * {@link LinkedWhatsAppAccountStore#name()} at
     * {@link #initialize()}; mirrors WA Web's
     * {@code WAWebBrowserInfo().os}.
     */
    private volatile String deviceName;

    /**
     * Holds the wire value of the {@code memClass} global
     * (id {@code 655}).
     *
     * <p>It captures the JVM's {@link Runtime#maxMemory()} divided by one
     * mebibyte at {@link #initialize()}.
     *
     * @implNote
     * This implementation reports JVM heap headroom in megabytes; WA
     * Web's {@code WAWebBrowserApi.getMemClass} reads
     * {@code navigator.deviceMemory} (a coarse RAM-bucket estimate in
     * gigabytes scaled by 1000) and Cobalt has no browser-side
     * equivalent.
     */
    @WhatsAppWebExport(
            moduleName = "WAWebBrowserApi",
            exports = "getMemClass",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    private volatile int memClass;

    /**
     * Holds the wire value of the {@code numCpu} global
     * (id {@code 10317}).
     *
     * <p>It captures {@link Runtime#availableProcessors()} at
     * {@link #initialize()}.
     *
     * @implNote
     * This implementation reports JVM-visible logical processors; WA
     * Web's {@code WAWebBrowserApi.getNumCpu} reads
     * {@code navigator.hardwareConcurrency} and Cobalt has no
     * browser-side equivalent.
     */
    @WhatsAppWebExport(
            moduleName = "WAWebBrowserApi",
            exports = "getNumCpu",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    private volatile int numCpu;

    /**
     * Holds the wire value of the {@code browser} global
     * (id {@code 779}).
     *
     * <p>It is always reported as {@code "Chrome"} because the WAM regular
     * channel is, per WA Web validation, gated on a Chrome user-agent
     * shape; Cobalt has no real browser to inspect but must claim one
     * for the upload to be accepted.
     */
    private volatile String browser;

    /**
     * Holds the wire value of the {@code browserVersion} global
     * (id {@code 295}).
     *
     * <p>It is reported as the stringified WhatsApp client version because
     * Cobalt has no browser version of its own to surface.
     */
    private volatile String browserVersion;

    /**
     * Holds the wire value of the {@code osVersion} global
     * (id {@code 15}).
     *
     * <p>It is computed from the JVM {@code os.name} and {@code os.version}
     * system properties at {@link #initialize()}.
     */
    private volatile String osVersion;

    /**
     * Holds the wire value of the {@code deviceVersion} global
     * (id {@code 4505}).
     *
     * <p>It is set to the same value as {@link #osVersion} because Cobalt
     * has no separate browser-device-version concept.
     */
    private volatile String deviceVersion;

    /**
     * Holds the wire value of the {@code webcTabId} global
     * (id {@code 3727}).
     *
     * <p>It is a per-session random UUID minted at {@link #initialize()};
     * mirrors WA Web's {@code WAWebUserPrefsTabMutex.THIS_TAB} which
     * is unique per browser tab and persists for the tab's lifetime.
     * Cobalt has no tab concept so the id is unique per service
     * instance.
     */
    private volatile String webcTabId;

    /**
     * Holds the wire value of the {@code abKey2} global
     * (id {@code 4473}), or {@code null} when AB-key reporting is
     * suppressed.
     *
     * <p>It is sourced from {@link ABPropsService#abKey()} at
     * {@link #initialize()}; forced to {@code null} when
     * {@link ABProp#WAM_DISABLE_ABKEY_ATTRIBUTE} is enabled, mirroring
     * WA Web's {@code WAWebABProps.getABPropConfigValue("wam_disable_abkey_attribute")}
     * gate.
     */
    private volatile String abKey2;

    /**
     * Holds the wire value of the {@code webcRevision} global
     * (id {@code 18491}).
     *
     * <p>It is the tertiary component of the client version, mirroring WA
     * Web's {@code SiteData.client_revision}; defaults to {@code 0} when
     * the version has no tertiary component.
     */
    private volatile int webcRevision;

    /**
     * Holds the wire value of the {@code webcPhoneAppVersion} global
     * (id {@code 1005}).
     *
     * <p>It is the companion device's WhatsApp version reported by the
     * companion-pairing handshake; {@code null} when no companion is
     * paired.
     */
    private volatile String companionAppVersion;

    /**
     * Holds the wire value of the {@code psCountryCode} global
     * (id {@code 6833}), emitted only on the private channel.
     *
     * <p>It is the ISO 3166-1 alpha-2 country code derived from the bound
     * account's phone number via {@link #derivePsCountryCode()}; mirrors
     * WA Web's {@code WAWebL10NCountryCodes.getCountryShortcodeByPhone}.
     */
    private volatile String psCountryCode;

    /**
     * Holds the wire value of the {@code serviceImprovementOptOut} global
     * (id {@code 13293}).
     *
     * <p>It is sourced from {@link ABProp#SERVICE_IMPROVEMENT_OPT_OUT_FLAG}
     * at {@link #initialize()}; flips when the user toggles the opt-out
     * preference and is observed by the server-side WAM consumers.
     */
    private volatile boolean serviceImprovementOptOut;

    /**
     * Holds the wire value of the {@code webcWebArch} global
     * (id {@code 6605}), or {@code null} when no push-phase tag
     * applies.
     *
     * <p>It mirrors WA Web's {@code getPushPhase} which maps the
     * {@code WAWebBuildConstants.PUSH_PHASE} build constant through the
     * alias table {@code sandcastle -> dev}, {@code trunkstable -> C1};
     * Cobalt has no equivalent build phase so the value is always
     * {@code null}.
     */
    private volatile String pushPhase;

    /**
     * Constructs a production WAM service bound to the given client and
     * AB-props facade, using {@link LiveWamBeaconingService} for the
     * per-event beacon sequence numbers.
     *
     * <p>The service is dormant until {@link #initialize()} is called after
     * the client has finished its initial handshake; commits made before
     * that point queue into the init buffer and drain on initialize.
     *
     * @param client         the WhatsApp client this service emits events
     *                       for, must not be {@code null}
     * @param abPropsService the AB-props facade used to read the AB key,
     *                       sampling configs, and Falco feature flags, must
     *                       not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public LiveWamService(LinkedWhatsAppClient client, ABPropsService abPropsService) {
        this(client, abPropsService, new LiveWamBeaconingService());
    }

    /**
     * Constructs a new {@code LiveWamService} bound to the given client,
     * AB-props service, and beaconing strategy.
     *
     * <p>The service starts in the pre-init phase: any commits made before
     * {@link #initialize()} are deferred to the init queue (mirroring WA
     * Web's {@code WAWebWamInitQueue} fallback), and the recurring serialize
     * and flush schedulers are not armed until {@link #initialize()} runs.
     * This constructor is {@code protected} so test doubles can inject a
     * controllable {@link WamBeaconingService} and override the timing and
     * scheduling seams.
     *
     * @param client         the bound WhatsApp client, must not be
     *                       {@code null}
     * @param abPropsService the AB-props service consulted at
     *                       {@link #initialize()} for sampling overrides
     *                       and global-gating flags, must not be
     *                       {@code null}
     * @param beaconing      the daily-sampled beaconing strategy
     *                       queried per event during
     *                       {@link #flushEventList}, must not be
     *                       {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    protected LiveWamService(LinkedWhatsAppClient client, ABPropsService abPropsService, WamBeaconingService beaconing) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
        this.pending = new ConcurrentHashMap<>();
        this.sequenceNumbers = new EnumMap<>(WamChannel.class);
        for (var channel : WamChannel.values()) {
            sequenceNumbers.put(channel, new AtomicInteger(1));
        }
        this.beaconing = Objects.requireNonNull(beaconing, "beaconing cannot be null");
        this.privateStatsId = new WamPrivateStatsId();
        this.privateStatsUploader = new WamPrivateStatsUploader(new WamPrivateStatsTokenIssuer(client), this);
        this.samplingOverride = new WamSamplingOverride();
        this.prevSessionGlobals = new EnumMap<>(WamChannel.class);
        this.initQueue = new ConcurrentLinkedQueue<>();
        this.futures = new ArrayList<>();
    }

    /**
     * Returns the current instant on the underlying clock.
     *
     * <p>This is the wall-clock source consumed by the per-event
     * {@code commitTime} global (id {@code 47}), the {@code t}
     * attribute of the {@code <iq xmlns="w:stats">} upload stanza, and
     * the deadline arithmetic in {@link #waitIfDisconnected()}.
     *
     * @implSpec
     * Overriders must return a non-null {@link Instant}; this
     * implementation returns {@link Instant#now()} and test doubles
     * return a controlled clock value driven by their stub
     * {@link #sleep(long)} so the connectivity-wait deadline check
     * terminates deterministically.
     *
     * @implNote
     * This implementation returns {@link Instant#now()}, the unmocked UTC
     * clock; identical to {@code WATimeUtils.unixTimeWithoutClockSkewCorrection}
     * apart from the unit (Cobalt callers convert to epoch seconds where the
     * WAM wire format demands it).
     *
     * @return the current instant
     */
    protected Instant now() {
        return Instant.now();
    }

    /**
     * Suspends the current thread for at least the given number of
     * milliseconds.
     *
     * <p>The upload retry loop in {@link #sendWithRetry(byte[])} and
     * {@link #sendPrivateWithRetry(byte[])} calls this for backoff, and
     * {@link #waitIfDisconnected()} calls it for the one-second poll
     * cadence.
     *
     * @implSpec
     * Overriders must block the calling virtual thread for the
     * requested duration; this implementation delegates to
     * {@link Thread#sleep(long)} and test doubles record the request
     * without sleeping while advancing their virtual clock.
     *
     * @implNote
     * This implementation delegates to {@link Thread#sleep(long)}, which on
     * a virtual carrier parks the carrier without blocking; it propagates any
     * {@link InterruptedException} so the caller's retry-backoff loop can
     * unwind cleanly.
     *
     * @param millis the duration to sleep, in milliseconds
     * @throws InterruptedException if the thread is interrupted while
     *                              sleeping
     */
    protected void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    /**
     * Schedules a recurring task on the WAM scheduler.
     *
     * <p>{@link #initialize()} calls this twice to arm the five-second
     * mid-cycle check ({@link #checkMidCycleUpload()}) and the
     * 120-second rotation flush ({@link #flush()}).
     *
     * @implSpec
     * Overriders must invoke {@code task} after
     * {@code initialDelaySeconds} have elapsed and then every
     * {@code periodSeconds} thereafter until
     * {@link #cancelAllScheduled()} is called; this implementation backs
     * each tick with a virtual-thread {@link ScheduledTask} and test
     * doubles record the schedule and drive ticks deterministically.
     *
     * @implNote
     * This implementation schedules the tick through
     * {@link ScheduledTask#schedule(Duration, Duration, Runnable)} with
     * fixed-delay semantics, so each tick starts {@code periodSeconds} after
     * the previous tick completes (not after it began), keeping ticks from
     * piling up if a flush stalls.
     *
     * @param task                the task to invoke on every tick
     * @param initialDelaySeconds the delay before the first tick, in
     *                            seconds
     * @param periodSeconds       the delay between successive ticks,
     *                            in seconds
     */
    protected void scheduleRecurring(Runnable task, long initialDelaySeconds, long periodSeconds) {
        var handle = ScheduledTask.schedule(Duration.ofSeconds(initialDelaySeconds), Duration.ofSeconds(periodSeconds), task);
        futures.add(handle);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "wam recurring task scheduled: initialDelay={0}s period={1}s", initialDelaySeconds, periodSeconds);
        }
    }

    /**
     * Cancels every recurring task previously scheduled through
     * {@link #scheduleRecurring(Runnable, long, long)}.
     *
     * <p>{@link #close()} calls this to tear down the scheduler before the
     * final {@link #flush()}.
     *
     * @implSpec
     * Overriders must drop every still-running scheduled task such
     * that no further ticks fire after the method returns; subsequent
     * calls to {@link #scheduleRecurring(Runnable, long, long)} arm a
     * fresh task set.
     *
     * @implNote
     * This implementation cancels each tracked handle, which wakes a pending
     * tick so it never fires and interrupts one already running, then clears
     * the list so a later re-initialize sequence schedules fresh handles.
     */
    protected void cancelAllScheduled() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cancelling {0} wam scheduled tasks", futures.size());
        for (var handle : futures) {
            handle.cancel();
        }
        futures.clear();
    }

    /**
     * Returns the number of events currently queued in the pending
     * list for the given channel.
     *
     * <p>This is an observation hook for behavioural tests; the production
     * flush pipeline never reads this state externally and embedders have
     * no API that surfaces pending counts.
     *
     * @param channel the channel to query
     * @return the number of queued events for {@code channel}, or
     *         {@code 0} when no list has been allocated yet
     */
    int pendingCount(WamChannel channel) {
        var list = pending.get(channel);
        return list == null ? 0 : list.size();
    }

    /**
     * Returns the number of deferred actions sitting in the init
     * queue and awaiting the next {@link #drainInitQueue()}.
     *
     * <p>This is an observation hook for behavioural tests covering the
     * pre-init {@link #commit(WamEventSpec)} and
     * {@link #commitAndWaitForFlush(WamEventSpec)} deferral path.
     *
     * @return the init-queue size
     */
    int initQueueSize() {
        return initQueue.size();
    }

    /**
     * Returns the current value of the per-channel sequence number
     * counter (the value the next call to
     * {@link #nextSequenceNumber(WamChannel)} would observe before
     * incrementing).
     *
     * <p>This is an observation hook for behavioural tests covering the
     * per-channel counter wrap from {@link #MAX_SEQUENCE_NUMBER} back to
     * {@code 1} and the independence-across-channels guarantee.
     *
     * @param channel the channel to query
     * @return the next sequence number to be emitted on a flush
     */
    int sequenceNumberFor(WamChannel channel) {
        return sequenceNumbers.get(channel).get();
    }

    /**
     * Sets the per-channel sequence counter to an arbitrary value.
     *
     * <p>This is a mutation hook for behavioural tests covering the
     * wraparound from {@link #MAX_SEQUENCE_NUMBER} back to {@code 1}
     * without paying for 65535 prior flushes; no production caller
     * exists.
     *
     * @param channel the channel to update
     * @param value   the new counter value
     */
    void setSequenceNumberForTesting(WamChannel channel, int value) {
        sequenceNumbers.get(channel).set(value);
    }

    /**
     * Drains every deferred action sitting in the init queue and
     * re-runs each one against the now-ready service.
     *
     * <p>It is invoked once from {@link #initialize()} after the
     * {@code initialized} flag is set, and is package-private so
     * behavioural tests that bypass the full {@link #initialize()}
     * sequence can trigger the drain in isolation.
     *
     * @implNote
     * This implementation calls {@link Runnable#run()} synchronously
     * on the draining thread; the deferred {@code commit} closure
     * re-enters {@link #commit(WamEventSpec)} or
     * {@link #commitAndWaitForFlush(WamEventSpec)} which now sees
     * {@code initialized == true} and lands the event in the pending
     * list (subject to validation and sampling against the live
     * runtime state).
     */
    @WhatsAppWebExport(moduleName = "WAWebWamInitQueue", exports = "processQueuedJobs", adaptation = WhatsAppAdaptation.ADAPTED)
    void drainInitQueue() {
        Runnable action;
        while ((action = initQueue.poll()) != null) {
            action.run();
        }
    }

    /**
     * Forces the {@code initialized} flag to {@code true} without
     * running the full {@link #initialize()} sequence.
     *
     * <p>This is a mutation hook for behavioural tests that drive a
     * deterministic {@link #flushChannel(WamChannel)} or {@link #flush()}
     * cycle without paying for the store-priming, AB-props snapshot, and
     * scheduler-arming work that {@link #initialize()} performs in
     * production.
     *
     * @implNote
     * This implementation leaves every volatile global at its
     * compile-time default ({@code null} / {@code 0}); tests rely on
     * {@link #buildFullCurrentGlobals(WamChannel)} omitting absent
     * entries so the buffer remains parseable.
     */
    void markInitializedForTesting() {
        this.initialized = true;
    }

    /**
     * Overwrites the {@link #deviceName} global without going through
     * {@link #initialize()}.
     *
     * <p>This is a mutation hook used by
     * {@code WamServiceGlobalsDirtyWriteTest} to mutate a single global
     * between flushes and assert the per-channel dirty-tracking
     * maintained by {@link #encodeGlobals(WamChannel)} via
     * {@link #prevSessionGlobals}.
     *
     * @param deviceName the new device name, or {@code null} to clear
     *                   it (which next flush emits as a null-transition
     *                   for field id {@code 13})
     */
    void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation re-announces every private-stats id as
     * CREATED on each {@code initialize()} call because Cobalt does
     * not persist the per-id rotation state across sessions; WA Web's
     * {@code WAWebWamPrivateStats.initPrivateStats} emits CREATED only
     * for ids that have no prior stored value, but Cobalt has nothing
     * stored to compare against and treats every rotation group as
     * freshly created.
     */
    @Override
    public void initialize() {
        var store = client.store();
        var version = store.accountStore().clientVersion();
        this.appVersion = version != null ? version.toString() : null;
        this.platform = store.accountStore().clientType() == LinkedWhatsAppClientType.WEB ? 8L : 2L;
        this.deviceName = store.accountStore().name().orElse(null);
        this.memClass = (int) (Runtime.getRuntime().maxMemory() / (1024 * 1024));
        this.numCpu = Runtime.getRuntime().availableProcessors();
        this.browser = "Chrome";
        this.browserVersion = appVersion;
        this.osVersion = System.getProperty("os.name", "") + " " + System.getProperty("os.version", "");
        this.deviceVersion = osVersion;
        this.webcTabId = UUID.randomUUID().toString();
        this.abKey2 = abPropsService.getBool(ABProp.WAM_DISABLE_ABKEY_ATTRIBUTE)
                ? null
                : abPropsService.abKey().orElse("");
        this.webcRevision = version != null ? version.tertiary().orElse(0) : 0;
        this.companionAppVersion = store.accountStore().companionVersion()
                .map(Object::toString)
                .orElse(null);
        this.psCountryCode = derivePsCountryCode();
        this.serviceImprovementOptOut = abPropsService.getBool(ABProp.SERVICE_IMPROVEMENT_OPT_OUT_FLAG);
        this.pushPhase = getPushPhase();

        var configs = abPropsService.samplingConfigs();
        if (!configs.isEmpty()) {
            samplingOverride.replaceAll(configs);
        }

        primeSequenceNumbers();
        restorePendingBuffers();

        this.initialized = true;

        drainInitQueue();

        scheduleRecurring(this::checkMidCycleUpload, SERIALIZE_INTERVAL_SECONDS, SERIALIZE_INTERVAL_SECONDS);
        scheduleRecurring(this::flush, FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS);

        for (var info : privateStatsId.snapshotAll()) {
            commit(new PsIdUpdateEventBuilder()
                    .psIdAction(PsIdAction.CREATED)
                    .psIdKey(info.keyHashInt())
                    .psIdRotationFrequence(info.rotationDays())
                    .build());
        }

        if (Log.INFO) LOGGER.log(Level.INFO, "wam initialized platform={0} appVersion={1}", platform, appVersion);
    }

    /**
     * Derives the value of the {@code psCountryCode} global from the
     * bound account's phone number.
     *
     * <p>It resolves the regional ISO 3166-1 alpha-2 code via Google's
     * {@code libphonenumber} parser and returns it lowercased to match WA
     * Web's wire shape.
     *
     * @implNote
     * This implementation defers entirely to {@code libphonenumber}'s
     * region table, whereas WA Web ships a hand-rolled prefix trie
     * ({@code WAWebL10NCountryCodes}) that hardcodes the same data and
     * applies one post-rule ({@code RU} prefix {@code 6} or {@code 7}
     * remapped to {@code KZ}). Both tables resolve every valid PSTN
     * prefix.
     *
     * @return the lowercase ISO 3166-1 alpha-2 country code, or
     *         {@code null} when no phone number is bound or the parser
     *         rejects the value
     */
    @WhatsAppWebExport(moduleName = "WAWebL10NCountryCodes",
            exports = "getCountryShortcodeByPhone",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private String derivePsCountryCode() {
        var phoneNumber = client.store().accountStore().phoneNumber();
        if (phoneNumber.isEmpty()) {
            return null;
        }

        try {
            var parsed = PhoneNumberUtil.getInstance().parse("+" + phoneNumber.getAsLong(), null);
            var regionCode = PhoneNumberUtil.getInstance().getRegionCodeForNumber(parsed);
            return regionCode != null ? regionCode.toLowerCase(Locale.ROOT) : null;
        } catch (Exception _) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation mirrors WA Web's
     * {@code WAWebWamCodegenWamEvent.WamEvent.commit}: a failed
     * pre-commit validation is logged and then accounted with a
     * {@link WamDroppedEventEvent} counter
     * ({@code droppedEventCode=id, droppedEventCount=1}) committed in
     * the rejected event's place through {@link #emitDroppedEvent(int)}.
     * The redundant-commit and sampling-drop paths carry no such
     * counter because WA Web has no equivalent accounting for them.
     */
    @Override
    public void commit(WamEventSpec event) {
        Objects.requireNonNull(event, "event cannot be null");

        if (!initialized) {
            initQueue.offer(() -> commit(event));
            return;
        }

        if (!event.markCommitted()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "wam redundant commit {0}", event.getClass().getSimpleName());
            return;
        }

        if (!event.validate()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "wam event failed validation {0}", event.getClass().getSimpleName());
            emitDroppedEvent(event.id());
            return;
        }

        var weight = effectiveWeight(event);
        if (weight > 1) {
            if (DataUtils.randomInt(weight) != 0) {
                return;
            }
        }

        var pe = new WamPendingEvent(event, now().getEpochSecond());
        if (Log.TRACE) LOGGER.log(Level.TRACE, "wam commit event={0} channel={1}", event.getClass().getSimpleName(), event.channel());
        pending.compute(event.channel(), (_, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(pe);
            return list;
        });

        if (event.channel() == WamChannel.REALTIME) {
            Thread.ofVirtual().start(() -> flushChannel(WamChannel.REALTIME));
        }
    }

    /**
     * Commits the WAM self-telemetry counter that accounts a single
     * event dropped for failing pre-commit validation.
     *
     * <p>{@link #commit(WamEventSpec)} calls this from its
     * validation-failure branch, passing the rejected event's numeric
     * id as {@code droppedEventCode} and a fixed count of one. The
     * emitted {@link WamDroppedEventEvent} declares no validators of its
     * own, so it always clears {@link #commit(WamEventSpec)} and lands
     * on the regular channel; the drop accounting can therefore never
     * itself be dropped and recurse.
     *
     * @implNote
     * This implementation leaves {@code isFromWamsys} unset because
     * Cobalt has no {@code wamsys} native pipeline to attribute the
     * drop to; WA Web's {@code WAWebWamCodegenWamEvent.WamEvent.commit}
     * likewise commits only {@code droppedEventCode} and
     * {@code droppedEventCount} from this call site.
     *
     * @param droppedEventCode the numeric id of the event that failed
     *                         validation
     */
    @WhatsAppWebExport(moduleName = "WAWebWamCodegenWamEvent", exports = "WamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamDroppedEventWamEvent", exports = "WamDroppedEventWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitDroppedEvent(int droppedEventCode) {
        commit(new WamDroppedEventEventBuilder()
                .droppedEventCode(droppedEventCode)
                .droppedEventCount(1)
                .build());
    }

    /**
     * Commits the empty {@link WebWamForceFlushEvent} sentinel that
     * marks a forced drain of the WAM buffers.
     *
     * <p>{@link #close()} calls this immediately before its terminal
     * {@link #flush()} so the sentinel lands in the regular channel's
     * pending list and ships in the same drain. The event carries no
     * fields; its presence on the wire is the sole signal that the
     * buffer was force-flushed on shutdown rather than rotated by the
     * 120-second scheduler.
     *
     * @implNote
     * This implementation emits the sentinel only from {@link #close()},
     * the sole Cobalt forced-flush path; WA Web's
     * {@code WAWebForceFlushWamBuffers.forceFlushAllWamAndQplBuffers}
     * additionally fires it ten seconds after page load, a lifecycle
     * hook Cobalt has no equivalent for. The scheduled 120-second
     * {@link #flush()} rotation deliberately does not emit it.
     */
    @WhatsAppWebExport(moduleName = "WAWebForceFlushWamBuffers", exports = "forceFlushAllWamAndQplBuffers", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitForceFlush() {
        commit(new WebWamForceFlushEventBuilder().build());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation never completes the future exceptionally on
     * upload failure; the retry/drop pipeline in
     * {@link #sendWithRetry(byte[])} either eventually succeeds or
     * accounts a {@code WamClientErrors} drop-counter event, and the
     * waiter sees a normal completion in both cases. Bridging upload
     * failures back to the caller is not implemented.
     */
    @Override
    public CompletableFuture<Void> commitAndWaitForFlush(WamEventSpec event) {
        Objects.requireNonNull(event, "event cannot be null");
        if (!initialized) {
            var deferred = new CompletableFuture<Void>();
            initQueue.offer(() -> {
                var inner = commitAndWaitForFlush(event);
                inner.whenComplete((value, error) -> {
                    if (error != null) {
                        deferred.completeExceptionally(error);
                    } else {
                        deferred.complete(value);
                    }
                });
            });
            return deferred;
        }
        if (!event.markCommitted()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "wam redundant commit {0}", event.getClass().getSimpleName());
            return CompletableFuture.completedFuture(null);
        }

        if (!event.validate()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "wam event failed validation {0}", event.getClass().getSimpleName());
            return CompletableFuture.completedFuture(null);
        }

        var weight = effectiveWeight(event);
        if (weight > 1) {
            if (DataUtils.randomInt(weight) != 0) {
                return CompletableFuture.completedFuture(null);
            }
        }

        var future = new CompletableFuture<Void>();
        var pe = new WamPendingEvent(event, now().getEpochSecond(), future);
        pending.compute(event.channel(), (_, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(pe);
            return list;
        });

        if (event.channel() == WamChannel.REALTIME) {
            Thread.ofVirtual().start(() -> flushChannel(WamChannel.REALTIME));
        }

        return future;
    }

    @Override
    public void setSamplingOverride(int eventId, int weight) {
        samplingOverride.put(eventId, weight);
    }

    @Override
    public void removeSamplingOverride(int eventId) {
        samplingOverride.remove(eventId);
    }

    @Override
    public void replaceSamplingOverrides(Map<Integer, Integer> overrides) {
        samplingOverride.replaceAll(overrides);
    }

    @Override
    public void flush() {
        if (!initialized) {
            return;
        }

        for (var channel : WamChannel.values()) {
            flushChannel(channel);
        }
    }

    /**
     * Triggers an early flush for any non-realtime channel whose
     * pending aggregate has crossed {@link #MAX_BUFFER_SIZE}.
     *
     * <p>This is bound to the five-second serialize scheduler armed by
     * {@link #initialize()}, and is package-private so tests can drive a
     * serialize check deterministically without waiting on the scheduler.
     * {@link WamChannel#REALTIME} is skipped because its commits already
     * trigger immediate per-event flushes in {@link #commit(WamEventSpec)}.
     * It is a no-op when the service is not initialised.
     */
    void checkMidCycleUpload() {
        if (!initialized) {
            return;
        }

        for (var channel : WamChannel.values()) {
            if (channel == WamChannel.REALTIME) {
                continue;
            }

            var oversized = new boolean[1];
            pending.compute(channel, (_, list) -> {
                if (list != null && !list.isEmpty()) {
                    var size = HEADER_SIZE;
                    for (var pe : list) {
                        size += pe.event().sizeOf();
                        if (size > MAX_BUFFER_SIZE) {
                            oversized[0] = true;
                            break;
                        }
                    }
                }
                return list;
            });

            if (oversized[0]) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "wam mid-cycle flush triggered channel={0}", channel);
                flushChannel(channel);
            }
        }
    }

    /**
     * Drains and uploads every pending event for the given channel.
     *
     * <p>This is package-private so behavioural tests can drive a single
     * channel's flush deterministically without going through the full
     * {@link #flush()} sweep. The pending list is atomically swapped
     * before encoding, so commits arriving during the flush land in a
     * fresh list rather than being lost. For {@link WamChannel#PRIVATE},
     * rotation of the {@link WamPrivateStatsId} set happens first and any
     * newly rotated id is announced with a {@code PsIdUpdateEvent} whose
     * action is {@link PsIdAction#ROTATED}.
     *
     * @implNote
     * This implementation routes
     * {@link WamChannel#PRIVATE} flushes through
     * {@link #flushPrivateByPsIdGroup(List)} which buckets events by
     * their private-stats id before encoding so each bucket can be
     * authenticated under its own HMAC; regular and realtime channels
     * use a single shared buffer key ({@code "regular"} or
     * {@code "realtime"}) for beaconing.
     *
     * @param channel the channel to flush
     */
    void flushChannel(WamChannel channel) {
        var events = swapPending(channel);
        if (events.isEmpty()) {
            return;
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "wam flush channel={0} events={1}", channel, events.size());

        if (channel == WamChannel.PRIVATE) {
            var rotated = privateStatsId.rotateAndReportChanges();
            for (var info : rotated) {
                commit(new PsIdUpdateEventBuilder()
                        .psIdAction(PsIdAction.ROTATED)
                        .psIdKey(info.keyHashInt())
                        .psIdRotationFrequence(info.rotationDays())
                        .build());
            }
            flushPrivateByPsIdGroup(events);
        } else {
            var bufferKey = channel == WamChannel.REGULAR ? "regular" : "realtime";
            flushEventList(channel, events, bufferKey);
        }
    }

    /**
     * Groups private events by their private-stats id hash and
     * flushes each group as a separate buffer.
     *
     * <p>This mirrors WA Web's {@code _executePendingForContext} where each
     * private-channel buffer is keyed by the
     * {@code WAWebWamPrivateStats.getPrivateStatsKeyFromInt} bucket name so
     * the upstream {@code WamContext} stays per bucket; it keeps every
     * {@code psId} in a buffer identical and lets the server credit metrics
     * to the right rotation group.
     *
     * @param events the drained private events
     */
    private void flushPrivateByPsIdGroup(List<WamPendingEvent> events) {
        var groups = new LinkedHashMap<Integer, List<WamPendingEvent>>();
        for (var pe : events) {
            groups.computeIfAbsent(pe.event().privateStatsId(), _ -> new ArrayList<>()).add(pe);
        }
        for (var entry : groups.entrySet()) {
            var bufferKey = privateStatsId.getKeyNameForHash(entry.getKey());
            flushEventList(WamChannel.PRIVATE, entry.getValue(), bufferKey);
        }
    }

    /**
     * Flushes a list of events for the given channel, building one or
     * more buffers capped at {@link #MAX_BUFFER_SIZE}.
     *
     * <p>The per-event weights and beacon sequence numbers are computed up
     * front so that the second encoding pass in {@link #buildAndSend} sees
     * a stable view; the dirty-globals byte sequence is also computed once
     * per buffer so the cross-buffer dirty-tracking stays accurate even
     * when the flush splits.
     *
     * @implNote
     * This implementation uses a post-check rotation: the event whose
     * marginal size pushes the running buffer over
     * {@link #MAX_BUFFER_SIZE} stays in the current buffer (which may
     * momentarily exceed the threshold by one event), and the next
     * event opens a fresh buffer. This matches WA Web's
     * {@code WAWebWamLibContext.WamContext.write} behaviour where the
     * size check fires after the write.
     *
     * @param channel   the transport channel
     * @param events    the events to flush
     * @param bufferKey the beaconing buffer key used by
     *                  {@link WamBeaconingService#nextSequenceNumber(String)}
     */
    private void flushEventList(WamChannel channel, List<WamPendingEvent> events, String bufferKey) {
        var beacons = new OptionalLong[events.size()];
        var weights = new int[events.size()];
        for (var i = 0; i < events.size(); i++) {
            beacons[i] = beaconing.nextSequenceNumber(bufferKey);
            weights[i] = effectiveWeight(events.get(i).event());
        }

        var batchStart = 0;
        var globalsBytes = encodeGlobals(channel);
        var batchSize = HEADER_SIZE + globalsBytes.length;

        for (var i = 0; i < events.size(); i++) {
            var pe = events.get(i);
            var eventSize = computePerEventGlobalsSize(pe, channel, beacons[i]) + pe.event().sizeOf(weights[i]);
            batchSize += eventSize;

            if (batchSize > MAX_BUFFER_SIZE) {
                buildAndSend(channel, events, weights, beacons, globalsBytes, batchStart, i + 1, batchSize);
                batchStart = i + 1;
                if (batchStart < events.size()) {
                    globalsBytes = encodeGlobals(channel);
                    batchSize = HEADER_SIZE + globalsBytes.length;
                }
            }
        }

        if (batchStart < events.size()) {
            buildAndSend(channel, events, weights, beacons, globalsBytes, batchStart, events.size(), batchSize);
        }
    }

    /**
     * Encodes the {@code [from, to)} slice of {@code events} into a
     * single WAM buffer of the pre-computed {@code size} and dispatches
     * it on the channel's transport.
     *
     * <p>The buffer layout is the {@link #HEADER_SIZE}-byte WAM header, the
     * pre-encoded session globals, then for each event the per-event
     * globals (commit time, optional beacon, optional private-stats id)
     * followed by the event payload. Buffers above {@link #MAX_UPLOAD_SIZE}
     * are dropped before any network call and a {@code WamClientErrors}
     * drop-counter event is committed in their place.
     *
     * @implNote
     * This implementation persists the slice via
     * {@link #persistBuffer(List, int[], int, int)} before sending and
     * removes the persisted entry only on dispatch return; any crash
     * mid-flight leaves the slice on disk for the next session's
     * {@link #restorePendingBuffers()} to recover.
     *
     * @param channel      the transport channel
     * @param events       the full pending event list
     * @param weights      pre-computed sampling weights parallel to
     *                     {@code events}
     * @param beacons      pre-computed beacon sequence numbers parallel
     *                     to {@code events}
     * @param globalsBytes the pre-encoded session globals
     * @param from         the inclusive start index into {@code events}
     * @param to           the exclusive end index into {@code events}
     * @param size         the pre-computed buffer size in bytes
     */
    private void buildAndSend(WamChannel channel, List<WamPendingEvent> events, int[] weights, OptionalLong[] beacons, byte[] globalsBytes, int from, int to, int size) {
        if (size > MAX_UPLOAD_SIZE) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "wam buffer dropped channel={0} size={1} limit={2}", channel, size, MAX_UPLOAD_SIZE);
            commit(new WamClientErrorsEventBuilder()
                    .wamClientBufferDropErrorCount(1)
                    .build());
            completeFutures(events, from, to);
            return;
        }

        var buffer = new byte[size];
        var encoder = WamEventEncoder.toBytes(buffer);
        writeHeader(encoder, channel);
        encoder.writeRaw(globalsBytes, 0, globalsBytes.length);

        for (var i = from; i < to; i++) {
            var pe = events.get(i);
            writePerEventGlobals(pe, channel, beacons[i], encoder);
            pe.event().encode(encoder, weights[i]);
        }

        assert encoder.written() == size : "Buffer size mismatch: wrote " + encoder.written() + " but allocated " + size;

        var saveKey = persistBuffer(events, weights, from, to);

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "wam buffer dispatch channel={0} events={1} size={2}", channel, to - from, size);
        if (channel == WamChannel.PRIVATE) {
            sendPrivateWithRetry(buffer);
        } else {
            sendWithRetry(buffer);
        }

        removePersistedBuffer(saveKey);
        completeFutures(events, from, to);
    }

    /**
     * Completes every flush future attached to the events in the
     * {@code [from, to)} slice.
     *
     * <p>{@link #buildAndSend} calls this both on a successful upload and
     * on an oversized-buffer drop; the future is the one returned by
     * {@link #commitAndWaitForFlush(WamEventSpec)} so the waiter unblocks
     * in both cases.
     *
     * @param events the full pending event list
     * @param from   the inclusive start index
     * @param to     the exclusive end index
     */
    private static void completeFutures(List<WamPendingEvent> events, int from, int to) {
        for (var i = from; i < to; i++) {
            var future = events.get(i).flushFuture();
            if (future != null) {
                future.complete(null);
            }
        }
    }

    /**
     * Encodes the session globals for the given channel, emitting
     * only entries that have changed since the last flush on this
     * channel.
     *
     * <p>The first flush for a channel writes every global because the
     * previous snapshot is {@code null}; subsequent flushes write the
     * dirty entries plus a null-transition entry for every field that was
     * present in the previous snapshot but is absent from the current one.
     *
     * @implNote
     * This implementation keeps one snapshot per
     * {@link WamChannel} via {@link #prevSessionGlobals}, mirroring WA
     * Web's per-{@code WamContext} {@code prevGlobals} field; the
     * snapshot is updated unconditionally at the end of the call even
     * if the encoded byte array ends up empty.
     *
     * @param channel the transport channel
     * @return the encoded dirty-globals bytes (length {@code 0} when
     *         nothing changed)
     */
    private byte[] encodeGlobals(WamChannel channel) {
        var current = buildFullCurrentGlobals(channel);
        var prev = prevSessionGlobals.get(channel);

        var dirty = new ArrayList<Map.Entry<Integer, Object>>();
        var nullTransitions = new ArrayList<Integer>();

        for (var entry : current.entrySet()) {
            var prevValue = prev != null ? prev.get(entry.getKey()) : null;
            if (!Objects.equals(prevValue, entry.getValue())) {
                dirty.add(entry);
            }
        }

        if (prev != null) {
            for (var fieldId : prev.keySet()) {
                if (!current.containsKey(fieldId)) {
                    nullTransitions.add(fieldId);
                }
            }
        }

        var size = 0;
        for (var entry : dirty) {
            size += WamGlobalEncoder.dynamicGlobalSize(entry.getKey(), entry.getValue());
        }
        for (var fieldId : nullTransitions) {
            size += WamGlobalEncoder.nullGlobalSize(fieldId);
        }

        var bytes = new byte[size];
        var encoder = WamEventEncoder.toBytes(bytes);
        for (var entry : dirty) {
            WamGlobalEncoder.writeDynamicGlobal(entry.getKey(), entry.getValue(), encoder);
        }
        for (var fieldId : nullTransitions) {
            WamGlobalEncoder.writeNullGlobal(fieldId, encoder);
        }

        prevSessionGlobals.put(channel, current);
        return bytes;
    }

    /**
     * Builds the full session-globals snapshot for the given channel
     * keyed by field id.
     *
     * <p>The map is the input to {@link #encodeGlobals(WamChannel)}'s
     * dirty-tracking. The field-id-to-name mapping mirrors WA Web's
     * {@code WAWebWamGlobals.defineGlobal} table: each entry is keyed by
     * the numeric id declared there and conditionally inserted based on
     * whether the global is declared for the channel ({@code regular}
     * only, {@code private} only, or both).
     *
     * @implNote
     * This implementation skips entries whose backing field is
     * {@code null} so the dirty-tracking treats the missing field as
     * absent rather than as a null value; {@link #encodeGlobals}
     * detects the "present-then-absent" transition by comparing
     * {@code prev.keySet()} against the current snapshot and emits a
     * null entry for the missing key.
     *
     * @param channel the transport channel
     * @return the current globals snapshot
     */
    private Map<Integer, Object> buildFullCurrentGlobals(WamChannel channel) {
        var globals = new LinkedHashMap<Integer, Object>();
        // 11 - platform (regular, private)
        globals.put(11, platform);
        // 13 - deviceName (regular, private)
        if (deviceName != null) globals.put(13, deviceName);
        // 15 - osVersion (regular, private)
        if (osVersion != null) globals.put(15, osVersion);
        // 17 - appVersion (regular, private)
        if (appVersion != null) globals.put(17, appVersion);
        // 21 - appIsBetaRelease (regular, private)
        globals.put(21, false);
        if (channel != WamChannel.PRIVATE) {
            // 23 - networkIsWifi (regular)
            globals.put(23, true);
            // 295 - browserVersion (regular)
            if (browserVersion != null) globals.put(295, browserVersion);
            // 633 - webcEnv (regular)
            globals.put(633, (long) WEBC_ENV_PROD);
        }
        // 655 - memClass (regular, private)
        globals.put(655, (long) memClass);
        if (channel != WamChannel.PRIVATE) {
            // 779 - browser (regular)
            if (browser != null) globals.put(779, browser);
        }
        // 899 - webcWebPlatform (regular, private)
        globals.put(899, (long) PLATFORM_WEBCLIENT);
        if (channel != WamChannel.PRIVATE) {
            // 1005 - webcPhoneAppVersion (regular)
            if (companionAppVersion != null) globals.put(1005, companionAppVersion);
        }
        // 1657 - appBuild (regular, private)
        globals.put(1657, (long) APP_BUILD_RELEASE);
        // 3543 - streamId (regular, private)
        globals.put(3543, (long) STREAM_ID);
        if (channel != WamChannel.PRIVATE) {
            // 3727 - webcTabId (regular)
            if (webcTabId != null) globals.put(3727, webcTabId);
            // 4473 - abKey2 (regular)
            if (abKey2 != null) globals.put(4473, abKey2);
            // 4505 - deviceVersion (regular)
            if (deviceVersion != null) globals.put(4505, deviceVersion);
            // 6605 - webcWebArch (regular)
            if (pushPhase != null) globals.put(6605, pushPhase);
        }
        // 6251 - ocVersion (regular, private)
        globals.put(6251, 1L);
        if (channel == WamChannel.PRIVATE) {
            // 6833 - psCountryCode (private)
            if (psCountryCode != null) globals.put(6833, psCountryCode);
        }
        if (channel != WamChannel.PRIVATE) {
            // 10317 - numCpu (regular)
            globals.put(10317, (long) numCpu);
        }
        // 13293 - serviceImprovementOptOut (regular, private)
        globals.put(13293, serviceImprovementOptOut);
        if (channel != WamChannel.PRIVATE) {
            // 14507 - deviceClassification (regular)
            globals.put(14507, (long) DEVICE_CLASSIFICATION_DESKTOP);
            // 18491 - webcRevision (regular)
            globals.put(18491, (long) webcRevision);
        }
        return globals;
    }

    /**
     * Returns the byte count of the per-event globals (commit time,
     * optional beacon, optional private-stats id) for the given
     * pending event.
     *
     * <p>{@link #flushEventList} uses this as the upfront sizing pass; the
     * matching encoder is
     * {@link #writePerEventGlobals(WamPendingEvent, WamChannel, OptionalLong, WamEventEncoder)},
     * which writes exactly this many bytes.
     *
     * @param pe      the pending event
     * @param channel the transport channel
     * @param beacon  the pre-computed beacon sequence number, empty
     *                when the buffer-key bucket is unsampled today
     * @return the per-event globals size in bytes
     */
    private int computePerEventGlobalsSize(WamPendingEvent pe, WamChannel channel, OptionalLong beacon) {
        var size = WamGlobalEncoder.commitTimeSize(pe.commitTimeSeconds());
        if (beacon.isPresent()) {
            size += WamGlobalEncoder.beaconSessionIdSize(beacon.getAsLong());
        }
        if (channel == WamChannel.PRIVATE) {
            var psId = privateStatsId.getValueForHash(pe.event().privateStatsId());
            size += WamGlobalEncoder.psIdSize(psId);
        }
        return size;
    }

    /**
     * Writes the per-event globals (commit time, optional beacon,
     * optional private-stats id) for the given pending event into the
     * encoder.
     *
     * <p>The bytes written must equal
     * {@link #computePerEventGlobalsSize(WamPendingEvent, WamChannel, OptionalLong)}
     * for the size-then-encode contract in {@link #flushEventList} to
     * hold; both methods key off the same {@code beacon.isPresent()} and
     * {@code channel == PRIVATE} guards to keep the calculations in sync.
     *
     * @param pe      the pending event
     * @param channel the transport channel
     * @param beacon  the pre-computed beacon sequence number, empty
     *                when the buffer-key bucket is unsampled today
     * @param encoder the destination encoder
     */
    private void writePerEventGlobals(WamPendingEvent pe, WamChannel channel, OptionalLong beacon, WamEventEncoder encoder) {
        WamGlobalEncoder.writeCommitTime(pe.commitTimeSeconds(), encoder);
        if (beacon.isPresent()) {
            WamGlobalEncoder.writeBeaconSessionId(beacon.getAsLong(), encoder);
        }
        if (channel == WamChannel.PRIVATE) {
            var psId = privateStatsId.getValueForHash(pe.event().privateStatsId());
            WamGlobalEncoder.writePsId(psId, encoder);
        }
    }

    /**
     * Atomically swaps the pending list for the given channel with
     * {@code null} and returns the old contents.
     *
     * <p>The atomic swap is the load-bearing primitive that keeps
     * concurrent {@link #commit(WamEventSpec)} calls from appending to a
     * list mid-flush; commits arriving during the swap either run before
     * the {@code compute} block ({@code list} is returned in the snapshot)
     * or after ({@code list} is freshly allocated under the same
     * {@link ConcurrentMap} entry).
     *
     * @param channel the channel to drain
     * @return the drained events, never {@code null} (empty list when
     *         the channel had no pending entry or an empty list)
     */
    private List<WamPendingEvent> swapPending(WamChannel channel) {
        var result = new ArrayList<WamPendingEvent>();
        pending.compute(channel, (_, list) -> {
            if (list != null && !list.isEmpty()) {
                result.addAll(list);
            }
            return null;
        });
        return result;
    }

    /**
     * Returns the effective sampling weight for the given event.
     *
     * <p>This consults {@link #samplingOverride} first (the runtime
     * override table seeded from AB-props at {@link #initialize()} and
     * mutated via the public sampling-override API) and falls back to
     * {@link WamEventSpec#releaseWeight()}. The override value is passed
     * through {@link Math#abs(int)} to match WA Web's defensive-coercion
     * behaviour where a negative AB-prop value is still treated as a
     * positive weight.
     *
     * @param event the event to query
     * @return the effective sampling weight; {@code 1} means
     *         "always keep", higher values keep {@code 1/weight} of
     *         commits in expectation
     */
    private int effectiveWeight(WamEventSpec event) {
        var override = samplingOverride.get(event.id());
        return override.isPresent() ? Math.abs(override.getAsInt()) : event.releaseWeight();
    }

    /**
     * Writes the eight-byte WAM buffer header for the given channel
     * into the encoder.
     *
     * <p>The header layout occupies exactly {@link #HEADER_SIZE} bytes:
     * {@snippet :
     *     // offset  field            value
     *     // 0..2    magic            "WAM"
     *     // 3       protocol         5         (PROTOCOL_VERSION)
     *     // 4       streamId         1         (STREAM_ID)
     *     // 5..6    sequenceNumber   uint16 LE (per-channel counter)
     *     // 7       channel          channel.id()
     * }
     *
     * @implNote
     * This implementation advances the per-channel counter via
     * {@link #nextSequenceNumber(WamChannel)} before writing, so the
     * counter increment is observable by the next caller even if the
     * encoder throws partway through.
     *
     * @param encoder the destination encoder
     * @param channel the transport channel
     */
    private void writeHeader(WamEventEncoder encoder, WamChannel channel) {
        var headerBytes = new byte[HEADER_SIZE];
        System.arraycopy(WAM_MAGIC, 0, headerBytes, 0, WAM_MAGIC.length);
        headerBytes[3] = (byte) PROTOCOL_VERSION;
        headerBytes[4] = (byte) STREAM_ID;
        DataUtils.putShort(headerBytes, 5, (short) nextSequenceNumber(channel), ByteOrder.LITTLE_ENDIAN);
        headerBytes[7] = (byte) channel.id();
        encoder.writeRaw(headerBytes, 0, HEADER_SIZE);
    }

    /**
     * Returns the next sequence number for the given channel,
     * wrapping from {@link #MAX_SEQUENCE_NUMBER} back to {@code 1}.
     *
     * <p>This mirrors WA Web's
     * {@code WAWebWamStorage.getNextSequenceNumberForStream}: the returned
     * value is the pre-increment counter value (so the first call returns
     * {@code 1}), and the post-increment value is immediately persisted to
     * the store so that the next session resumes the sequence rather than
     * restarting at {@code 1}.
     *
     * @implNote
     * This implementation skips zero on wrap; the counter resets to
     * {@code 1}, not {@code 0}, so the server can use {@code 0} as an
     * unset sentinel.
     *
     * @param channel the transport channel
     * @return the next sequence number in {@code [1, MAX_SEQUENCE_NUMBER]}
     */
    private int nextSequenceNumber(WamChannel channel) {
        var counter = sequenceNumbers.get(channel);
        var oldValue = counter.getAndUpdate(current -> {
            var next = current + 1;
            return next > MAX_SEQUENCE_NUMBER ? 1 : next;
        });
        client.store().wamStore().putSequenceNumber(channel, counter.get());
        return oldValue;
    }

    /**
     * Primes the in-memory per-channel sequence counters from the
     * values previously persisted by
     * {@link #nextSequenceNumber(WamChannel)}.
     *
     * <p>It is called once from {@link #initialize()} so the wire-level
     * sequence carries across sessions and the server does not see a
     * counter reset (which would be indistinguishable from a fresh stream
     * and would invalidate any deduplication or ordering the server
     * tracks).
     */
    private void primeSequenceNumbers() {
        var store = client.store();
        for (var channel : WamChannel.values()) {
            var stored = store.wamStore().findSequenceNumber(channel);
            if (stored.isPresent()) {
                sequenceNumbers.get(channel).set(stored.getAsInt());
            }
        }
    }

    /**
     * Persists the events in {@code [from, to)} as a new staged buffer and returns the save key that
     * identifies it.
     *
     * <p>This mirrors WA Web's {@code WAWebWamStorage.add} which writes the unsaved-portion buffer to the
     * IndexedDB {@code wamBuffers} object store. The buffer is a stream of WAM event entries, each
     * preceded by a {@code commitTime} global with field id {@link #COMMIT_TIME_FIELD_ID} written via
     * {@link WamGlobalEncoder#writeCommitTime(long, WamEventEncoder)}; the next session's
     * {@link #restorePendingBuffers()} decodes it back into pending entries. The encoded bytes are staged
     * through {@link com.github.auties00.cobalt.store.linked.LinkedWhatsAppWamStore#putPendingBuffer},
     * which the disk-backed store offloads to its embedded message store and the transient store holds in
     * memory.
     *
     * @implNote
     * This implementation sizes the buffer up front with {@link WamGlobalEncoder#commitTimeSize(long)}
     * and each event's own {@code sizeOf} so it can encode straight into a fixed byte array, the same
     * two-pass shape the upload path in {@link #buildAndSend} uses.
     *
     * @param events  the full pending event list
     * @param weights the resolved sampling weights parallel to
     *                {@code events}
     * @param from    the inclusive start index
     * @param to      the exclusive end index
     * @return the save key on success, or {@code null} when the slice is empty
     */
    private String persistBuffer(List<WamPendingEvent> events, int[] weights, int from, int to) {
        if (from >= to) {
            return null;
        }
        var size = 0;
        for (var i = from; i < to; i++) {
            var pe = events.get(i);
            size += WamGlobalEncoder.commitTimeSize(pe.commitTimeSeconds()) + pe.event().sizeOf(weights[i]);
        }
        var buffer = new byte[size];
        var encoder = WamEventEncoder.toBytes(buffer);
        for (var i = from; i < to; i++) {
            var pe = events.get(i);
            WamGlobalEncoder.writeCommitTime(pe.commitTimeSeconds(), encoder);
            pe.event().encode(encoder, weights[i]);
        }
        assert encoder.written() == size : "Buffer size mismatch: wrote " + encoder.written() + " but allocated " + size;
        var saveKey = generateSaveKey();
        client.store().wamStore().putPendingBuffer(saveKey, buffer);
        return saveKey;
    }

    /**
     * Removes a previously-persisted buffer file by save key.
     *
     * <p>{@link #buildAndSend} calls this after the transport returns; on a
     * successful upload it drops the durable copy so the next session does
     * not re-attempt the upload, and on a permanent drop the durable copy
     * is still removed because the in-memory pipeline has already accounted
     * for it via a {@code WamClientErrors} event.
     *
     * @param saveKey the save key from
     *                {@link #persistBuffer(List, int[], int, int)},
     *                or {@code null} for a no-op
     */
    private void removePersistedBuffer(String saveKey) {
        if (saveKey == null) {
            return;
        }
        client.store().wamStore().removePendingBuffer(saveKey);
    }

    /**
     * Reads every previously-persisted buffer file, decodes its
     * events back into {@link WamPendingEvent}s, and re-injects them
     * into the pending list for their channel.
     *
     * <p>It is called once from {@link #initialize()} before the schedulers
     * arm. Each file is deleted as soon as its contents have been
     * re-injected so the next flush cycle re-persists whatever still needs
     * to ship; a corrupted leading entry (where the first field is not
     * {@link #COMMIT_TIME_FIELD_ID}) aborts the decode loop for that file
     * and skips the rest of its content.
     *
     * @implNote
     * This implementation logs and continues on any decode failure for
     * a file, mirroring WA Web's
     * {@code WAWebWamStorage.deleteAll}-catching behaviour where a
     * single broken IndexedDB entry does not prevent the rest of the
     * persisted store from being recovered.
     */
    private void restorePendingBuffers() {
        var keys = client.store().wamStore().pendingBufferKeys();
        for (var saveKey : keys) {
            try {
                var buffer = client.store().wamStore().findPendingBuffer(saveKey);
                if (buffer.isEmpty()) {
                    continue;
                }
                try (var decoder = WamEventDecoder.fromBytes(buffer.get())) {
                    while (decoder.hasMore()) {
                        var header = decoder.readHeader();
                        if (WamEventDecoder.fieldIdOf(header) != COMMIT_TIME_FIELD_ID) {
                            if (Log.WARNING) LOGGER.log(Level.WARNING, "wam persisted buffer has unexpected leading field key={0} field={1}", saveKey, WamEventDecoder.fieldIdOf(header));
                            break;
                        }
                        var commitTime = decoder.readInt(header);
                        var event = WamEventRegistry.decode(decoder);
                        var pendingEvent = new WamPendingEvent(event, commitTime);
                        pending.compute(event.channel(), (_, list) -> {
                            if (list == null) {
                                list = new ArrayList<>();
                            }
                            list.add(pendingEvent);
                            return list;
                        });
                    }
                }
                client.store().wamStore().removePendingBuffer(saveKey);
            } catch (Exception error) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "wam restore persisted buffer failed key=" + saveKey, error);
            }
        }
    }

    /**
     * Generates a fresh save key for a persisted buffer.
     *
     * <p>The key has the shape {@code <random36>_<unixmillis>}, combining a
     * base-36-encoded random {@code long} prefix with a millisecond
     * timestamp suffix; the random prefix avoids collisions when many
     * buffers are flushed within the same millisecond and the timestamp
     * suffix makes the key sortable for ad-hoc inspection.
     *
     * @return a filesystem-safe save key
     */
    private static String generateSaveKey() {
        return Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36)
                + "_" + System.currentTimeMillis();
    }


    /**
     * Blocks the caller until the bound client is connected or
     * {@link #CONNECTIVITY_WAIT_TIMEOUT_MS} elapses, whichever comes
     * first.
     *
     * <p>{@link #sendWithRetry(byte[])} and
     * {@link #sendPrivateWithRetry(byte[])} call this before each upload
     * attempt; it mirrors WA Web's
     * {@code WAWebUploadStatsBackend.waitIfOffline} which avoids burning
     * retry budget on attempts made while the socket is known-disconnected.
     * After the deadline elapses the upload still proceeds; the retry loop
     * then handles the actual transport failure.
     *
     * @implNote
     * This implementation polls
     * {@link LinkedWhatsAppClient#isConnected()} every
     * {@code 1000} milliseconds via the abstract {@link #sleep(long)}
     * hook; the deadline is computed from {@link #now()} so the
     * testable subclass can drive the loop deterministically by
     * advancing its virtual clock inside {@code sleep}.
     */
    private void waitIfDisconnected() {
        if (client.isConnected()) {
            return;
        }

        var deadline = now().toEpochMilli() + CONNECTIVITY_WAIT_TIMEOUT_MS;
        try {
            while (!client.isConnected() && now().toEpochMilli() < deadline) {
                sleep(1_000);
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sends a WAM buffer via the {@code <iq xmlns="w:stats">} stanza
     * and retries with exponential backoff on transient server
     * errors.
     *
     * <p>The dispatch protocol matches WA Web's
     * {@code WASmaxStatsSendBufferRPC.sendSendBufferRPC}: a
     * {@code type="result"} response terminates successfully, a
     * {@code type="error"} response with a {@code 5xx}-class status is
     * retried up to {@link #MAX_RETRIES} times, and any other error is
     * permanent and the buffer is dropped with a {@code WamClientErrors}
     * drop-counter event re-committed in its place.
     *
     * @implNote
     * This implementation runs {@link #waitIfDisconnected()} once
     * before the first attempt and not between retries; the per-retry
     * backoff already provides enough headroom for the socket to
     * reconnect, and WA Web behaves the same way (the
     * {@code waitIfOffline} is called by the outer
     * {@code WAWebUploadStatsBackend} once, not per attempt).
     *
     * @param buffer the encoded WAM buffer
     */
    private void sendWithRetry(byte[] buffer) {
        waitIfDisconnected();

        for (var attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                var response = sendViaIq(buffer);
                var type = response.getAttributeAsString("type", "");
                if ("result".equals(type)) {
                    return;
                }

                var errorCode = parseErrorCode(response);
                if (errorCode >= 500 && attempt < MAX_RETRIES) {
                    var delay = computeBackoffDelay(attempt);
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "wam upload retry error={0} delay={1}ms attempt={2}", errorCode, delay, attempt + 1);
                    sleep(delay);
                    continue;
                }

                if (Log.WARNING) LOGGER.log(Level.WARNING, "wam upload dropped error={0}", errorCode);
                commit(new WamClientErrorsEventBuilder()
                        .wamClientBufferDropErrorCount(1)
                        .build());
                return;
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                if (attempt < MAX_RETRIES) {
                    try {
                        var delay = computeBackoffDelay(attempt);
                        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "wam upload retry after failure delay={0}ms", delay);
                        sleep(delay);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "wam upload failed after " + MAX_RETRIES + " retries", e);
                    commit(new WamClientErrorsEventBuilder()
                            .wamClientBufferDropErrorCount(1)
                            .build());
                }
            }
        }
    }

    /**
     * Sends a private-channel WAM buffer through the
     * {@link WamPrivateStatsUploader} and retries with exponential
     * backoff on transient server errors.
     *
     * <p>This mirrors WA Web's {@code WAWebUploadPrivateStatsBackend}. Each
     * attempt acquires a fresh single-use authentication token via
     * {@link WamPrivateStatsTokenIssuer} (the {@code <sign_credential>} IQ
     * round-trip with the Ed25519 blinded-token VOPRF) and then POSTs a
     * multipart {@code message} body to
     * {@code https://dit.whatsapp.net/deidentified_telemetry} with the
     * buffer authenticated by {@code HMAC-SHA256(sharedSecret, buffer)}.
     * The retry policy is: {@code 200} returns immediately;
     * {@code ERROR_SERVER_OTHER}, {@code ERROR_OTHER}, and
     * {@code ERROR_CREDENTIAL} retry up to {@link #MAX_RETRIES} times with
     * exponential backoff; permanent classifications drop the buffer and
     * re-commit a {@code WamClientErrors} drop-counter event.
     *
     * @implNote
     * This implementation runs {@link #waitIfDisconnected()} once
     * before the first attempt; the inner uploader fetches the token
     * via the bound client's IQ surface, so a closed socket also
     * fails the token round-trip and the retry loop captures the same
     * connectivity failure that {@code waitIfDisconnected} aims to
     * pre-empt.
     *
     * @param buffer the encoded WAM buffer
     */
    private void sendPrivateWithRetry(byte[] buffer) {
        waitIfDisconnected();

        for (var attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            var result = privateStatsUploader.upload(buffer);
            switch (result.result()) {
                case SUCCESS -> {
                    return;
                }
                case ERROR_SERVER_OTHER, ERROR_OTHER, ERROR_CREDENTIAL -> {
                    if (attempt < MAX_RETRIES) {
                        try {
                            var delay = computeBackoffDelay(attempt);
                            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "private wam upload retry result={0} http={1} delay={2}ms attempt={3}", result.result(), result.httpResponseCode(), delay, attempt + 1);
                            sleep(delay);
                        } catch (InterruptedException _) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        continue;
                    }
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "private wam upload failed after " + MAX_RETRIES + " retries result={0} http={1}", result.result(), result.httpResponseCode());
                    commit(new WamClientErrorsEventBuilder()
                            .wamClientBufferDropErrorCount(1)
                            .build());
                    return;
                }
                default -> {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "private wam upload failed permanently result={0} http={1}", result.result(), result.httpResponseCode());
                    commit(new WamClientErrorsEventBuilder()
                            .wamClientBufferDropErrorCount(1)
                            .build());
                    return;
                }
            }
        }
    }

    /**
     * Sends a single WAM buffer via an {@code <iq xmlns="w:stats">}
     * stanza and returns the server response.
     *
     * <p>The wire shape is
     * {@snippet lang=xml :
     *     <iq xmlns="w:stats" to="s.whatsapp.net" type="set">
     *         <add t="<unix-seconds>">
     *             <!-- raw WAM buffer bytes -->
     *         </add>
     *     </iq>
     * }
     *
     * @param buffer the encoded WAM buffer
     * @return the server response stanza
     */
    private Stanza sendViaIq(byte[] buffer) {
        var add = new StanzaBuilder()
                .description("add")
                .attribute("t", String.valueOf(now().getEpochSecond()))
                .content(buffer)
                .build();
        var iq = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:stats")
                .attribute("to", "s.whatsapp.net")
                .attribute("type", "set")
                .content(add);
        var response = client.sendNode(iq);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "wam stats iq response {0}", response);
        return response;
    }

    /**
     * Parses the HTTP-style error code from a {@code type="error"}
     * IQ response.
     *
     * <p>The error code lives in the {@code code} attribute of the first
     * {@code <error>} child of the IQ; classifications above {@code 500}
     * drive retry, everything else drives a permanent drop. The method
     * returns {@code 0} when no {@code <error>} child is present, which the
     * caller treats as permanent.
     *
     * @param response the server response stanza
     * @return the error code, or {@code 0} when no error child exists
     */
    private static int parseErrorCode(Stanza response) {
        var children = response.getChildren("error");
        if (children.isEmpty()) {
            return 0;
        }
        var error = children.getFirst();
        return (int) error.getAttributeAsLong("code", 0L);
    }

    /**
     * Computes the exponential-backoff delay in milliseconds for the
     * given zero-based retry attempt.
     *
     * <p>This mirrors WA Web's {@code WABackoffUtils.expBackoff(attempt,
     * 120000, 1000, 0.1)} call inside {@code WAWebUploadStatsBackend}: it
     * returns {@code 2^attempt} clamped into
     * {@code [RETRY_BASE_DELAY_MS, RETRY_MAX_DELAY_MS]} plus up to ten
     * percent uniform jitter on top, capped from above at
     * {@code RETRY_MAX_DELAY_MS * 1.1}.
     *
     * @implNote
     * This implementation preserves WA Web's formula where
     * {@code 2^attempt} is computed without multiplying by the base
     * delay; for small attempt values ({@code 2^attempt} below
     * {@link #RETRY_BASE_DELAY_MS}) the lower clamp dominates and the
     * curve plateaus at the base delay. Package-private so the unit
     * test in {@code WamServiceTest.RetryBackoff} can verify the
     * three documented regions (low clamp, unclamped middle, high
     * clamp).
     *
     * @param attempt the zero-based retry attempt number
     * @return the delay in milliseconds, in
     *         {@code [RETRY_BASE_DELAY_MS, RETRY_MAX_DELAY_MS * 1.1]}
     */
    static long computeBackoffDelay(int attempt) {
        var delay = attempt == 0 ? RETRY_BASE_DELAY_MS : (long) Math.pow(2, attempt);
        if (delay > RETRY_MAX_DELAY_MS) delay = RETRY_MAX_DELAY_MS;
        if (delay < RETRY_BASE_DELAY_MS) delay = RETRY_BASE_DELAY_MS;
        var jitter = (long) (delay * 0.1 * DataUtils.randomDouble());
        return delay + jitter;
    }

    /**
     * Returns the {@code webcWebArch} push-phase string for the
     * current build, or {@code null} when no phase tag applies.
     *
     * <p>This mirrors WA Web's {@code WAWebWam.getPushPhase}, which maps the
     * {@code WAWebBuildConstants.PUSH_PHASE} build constant through the
     * alias table {@code sandcastle -> dev}, {@code trunkstable -> C1}
     * (other values pass through), and forces {@code "jest-e2e"} when the
     * {@code 26256} gatekeeper is set.
     *
     * @implNote
     * This implementation always returns {@code null} because Cobalt
     * has no equivalent of WA Web's push-phase build pipeline; the
     * regular-channel {@code webcWebArch} global is therefore never
     * emitted.
     *
     * @return the push-phase string, or {@code null} when not
     *         applicable
     */
    @WhatsAppWebExport(moduleName = "WAWebWam", exports = "getPushPhase", adaptation = WhatsAppAdaptation.ADAPTED)
    private static String getPushPhase() {
        return null;
    }

    @Override
    public void close() {
        cancelAllScheduled();
        emitForceFlush();
        persistPending();
        initialized = false;
        if (Log.INFO) LOGGER.log(Level.INFO, "wam closed");
    }

    /**
     * Persists every channel's pending events to the durable WAM store without shipping them.
     *
     * <p>This is the terminal drain {@link #close()} runs in place of a networked {@link #flush()}:
     * it atomically swaps out each channel's pending list and writes the events through
     * {@link #persistBuffer(List, int[], int, int)} with no transport round-trip and without removing
     * the persisted copy, so the next {@link #initialize()} recovers them via
     * {@link #restorePendingBuffers()} and ships them on a live connection. Shipping is deliberately
     * not attempted here: {@link #close()} runs inside the client's disconnect path, where awaiting a
     * {@code w:stats} acknowledgement would block teardown against a closing socket and any events a
     * bounded wait could not ship would be dropped. Persisting instead mirrors WA Web, which
     * serialises unshipped buffers to durable storage on unload and ships them the next session rather
     * than blocking teardown on the network.
     *
     * @implNote
     * This implementation persists each channel's events flat, without the {@link #MAX_BUFFER_SIZE}
     * batching or private-channel {@code psId} grouping that {@link #flushChannel(WamChannel)} applies
     * for transmission; those only shape the wire buffers, and {@link #restorePendingBuffers()}
     * re-injects the events into {@link #pending} so the next flush re-batches and re-groups them
     * before sending.
     */
    private void persistPending() {
        for (var channel : WamChannel.values()) {
            var events = swapPending(channel);
            if (events.isEmpty()) {
                continue;
            }
            var weights = new int[events.size()];
            for (var i = 0; i < events.size(); i++) {
                weights[i] = effectiveWeight(events.get(i).event());
            }
            persistBuffer(events, weights, 0, events.size());
        }
    }
}

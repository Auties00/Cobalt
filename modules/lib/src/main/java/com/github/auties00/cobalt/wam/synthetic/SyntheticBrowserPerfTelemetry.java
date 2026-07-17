package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice.Web;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.*;
import com.github.auties00.cobalt.wire.wam.type.*;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Emits the synthetic WhatsApp Web browser-performance WAM telemetry burst
 * that a genuine WA Web page load produces, so a Cobalt session's metrics
 * stream is indistinguishable from a real browser's on the same surfaces.
 *
 * <p>WhatsApp Web logs a large family of browser-only performance and
 * environment events on every page load: navigation and paint timing
 * ({@code WebcPageLoad}, {@code WebcPageLoad2}, {@code WebcResourceLoad},
 * {@code WebcAssetLoad}, {@code WebcChatOpen}, {@code WebcProgressiveImage}),
 * runtime launch metrics ({@code AppLaunch}), heap and process memory samples
 * ({@code WebcMemoryStat}, {@code MemoryStat}), IndexedDB open, schema-version
 * and per-table usage stats ({@code WebcDbOpen}, {@code WebDbVersionsSource},
 * {@code WebDbVersionNonAnonymous}, {@code WebDbLoadFromVersionFailureNonAnonymous},
 * {@code WebDbTableUsage}), storage and FTS-index sizes ({@code WebcStorageStat},
 * {@code WebcFtsStorage}), the job-orchestrator stats ({@code WebcJobInfo}), the
 * page-lifecycle counters ({@code WebcPageResume}), the PWA and background-sync
 * adoption events ({@code WebcPwaEvent}, {@code WebcBackgroundSyncAdoption}), the
 * anti-abuse device fingerprint ({@code WebcFingerprint}) and the occasional
 * image-fetch error ({@code WebcImgError}). None of these map to a Cobalt
 * feature: Cobalt is a headless JVM client with no DOM, no PerformanceTiming, no
 * IndexedDB and no service worker. Their absence is nonetheless a fingerprint, so
 * this service fabricates one plausible snapshot per connection, anchored to the
 * session's persisted device fingerprint so the browser-environment fields stay
 * coherent and stable across reconnects.
 *
 * <p>Every field WA sets is populated with either a real host-sourced value
 * (JVM heap sizes, logical CPU count, process id, host disk quota, system
 * timezone, store collection counts), a value read from the session's persisted
 * per-device browser fingerprint (screen resolution, viewport size, device
 * memory, GPU vendor, connection round-trip time, history length), or a realistic
 * constant captured from a genuine Chrome-on-desktop WA Web session (Blink engine,
 * canonical Chrome structure, plugin and mime-type counts, cache flags). No
 * obviously-fake sentinel values are used; empty strings appear only where a
 * genuine clean browser reports them (for example the empty automation-signals
 * string that a non-automated Chrome sends).
 *
 * @implNote
 * This implementation fires the whole burst from a single
 * {@link #emitSessionTelemetry()} entry point, mirroring WA Web's once-per-load
 * reporter set. The two events that a healthy load never emits, the image-fetch
 * error and the schema-version load failure, are committed only with a small
 * per-session probability through {@link SyntheticTelemetryUtils#chance(int)} so
 * that, across a fleet of sessions, they surface at a realistic low rate rather
 * than on every connection. The heap-derived memory samples and the storage/FTS-size
 * samples are computed fresh on each call so their values track the live JVM and
 * store rather than a frozen constant.
 *
 * @see WamService
 */
@WhatsAppWebModule(moduleName = "WAWebWamPageLoadReporter")
@WhatsAppWebModule(moduleName = "WAWebWamAppLaunchReporter")
@WhatsAppWebModule(moduleName = "WAWebWamMemoryStat")
@WhatsAppWebModule(moduleName = "WAWebWamFingerprintReporter")
@WhatsAppWebModule(moduleName = "WAWebTasksDailyStatsTask")
public final class SyntheticBrowserPerfTelemetry {
    /**
     * The canonical {@code window.chrome} structure a genuine desktop Chrome
     * exposes, captured verbatim so the {@code chromeStructure} fingerprint field
     * matches a real browser rather than a headless environment that lacks these
     * members.
     */
    private static final String CHROME_STRUCTURE = "app,runtime,loadTimes,csi,app.isInstalled,runtime.connect";

    /**
     * The empty automation-signals string a genuine non-automated Chrome reports.
     *
     * <p>The anti-abuse fingerprint's {@code automationSignals} field is the
     * concatenation of detected webdriver markers; a clean browser reports the
     * empty string, so the empty value here is the faithful signal rather than a
     * placeholder.
     */
    private static final String NO_AUTOMATION_SIGNALS = "";

    /**
     * The bound WhatsApp client whose store supplies the live chat, contact,
     * message and account state sampled into the memory, storage and chat-open
     * events.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every fabricated browser-performance event is
     * committed for batched upload.
     */
    private final WamService wamService;

    /**
     * Constructs a new {@code SyntheticBrowserPerfTelemetry} bound to the given
     * client and WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be
     *                   {@code null}
     * @param wamService the WAM service used to commit the emitted events, must
     *                   not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticBrowserPerfTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
    }

    /**
     * Emits the once-per-connection browser-performance telemetry burst on a
     * dedicated virtual thread.
     *
     * <p>This is the single entry point the client drives after the socket opens.
     * It fabricates and commits one coherent snapshot of every browser-perf
     * surface a genuine WA Web load reports: the page-load and paint timers, the
     * app-launch metrics, the device fingerprint, the resource and asset loads,
     * the IndexedDB open, schema-version and table-usage stats, the chat-open
     * render metrics, the heap and process memory samples, the storage and FTS
     * sizes, the job-orchestrator stats, the page-resume counter, the PWA and
     * background-sync adoption events, and, at a realistic low rate, the
     * image-fetch error and schema-load failure.
     *
     * <p>The work runs off the socket-open thread because the store-derived
     * message-count aggregation walks every chat; the WAM commits themselves are
     * cheap enqueues on the regular channel. Events whose sampling weight exceeds
     * one are subject to the usual {@link WamService} sampling, so emitting the
     * burst on every connection does not oversample the high-weight surfaces.
     *
     * @apiNote
     * The heap, storage and FTS samples in this burst are periodic by nature on
     * WA Web (its memory sampler runs on a schedule and its storage and FTS sizes
     * are reported by the daily-stats task). Cobalt has no equivalent scheduler in
     * this service, so it folds one such sample into the per-connection burst; a
     * caller that wants the periodic cadence may invoke this method again on a
     * timer.
     *
     * @implNote
     * This implementation fires only for a {@link LinkedWhatsAppClientType#WEB}
     * session and returns without emitting anything for a
     * {@link LinkedWhatsAppClientType#MOBILE} (primary phone-registration)
     * session, because the whole {@code Webc*} family is WhatsApp Web browser
     * telemetry that a primary device, which runs no browser, never emits.
     */
    public void emitSessionTelemetry() {
        if (client.store().accountStore().clientType() != LinkedWhatsAppClientType.WEB) {
            return;
        }
        Thread.ofVirtual()
                .name("browser-perf-telemetry")
                .start(this::runBurst);
    }

    /**
     * Builds and commits the full browser-performance burst.
     *
     * <p>The store is sampled once for the chat, contact and message counts that
     * feed the memory, storage, FTS and chat-open events, and a single page-load
     * id is minted and shared between the {@code WebcPageLoad} and
     * {@code WebcPageLoad2} events so they correlate as they would on a real load.
     * The device browser fingerprint is resolved once and shared between the
     * fingerprint and chat-open events so the reported screen, viewport and window
     * height describe one coherent machine.
     */
    private void runBurst() {
        var store = client.store();
        var chats = store.chatStore().chats();
        var chatCount = chats.size();
        var contactCount = store.contactStore().contacts().size();
        var messageCount = 0L;
        Chat busiest = null;
        for (var chat : chats) {
            var count = chat.messageCount();
            messageCount += count;
            if (busiest == null || count > busiest.messageCount()) {
                busiest = chat;
            }
        }
        var hasVerifiedNumber = store.accountStore().jid().isPresent();

        var fingerprint = resolveFingerprint();
        var pageLoadId = SyntheticTelemetryUtils.randomHexLower(12);

        commitAppLaunch();
        commitPageLoad(pageLoadId);
        commitPageLoad2(pageLoadId);
        commitResourceLoad();
        commitAssetLoad();
        commitFingerprint(fingerprint);
        commitDbOpen();
        commitDbVersionsSource();
        commitDbVersion();
        commitDbTableUsage(messageCount);
        commitChatOpen(busiest, fingerprint);
        commitProgressiveImage();
        commitWebcMemoryStat(chatCount, contactCount, messageCount);
        commitMemoryStat(messageCount, hasVerifiedNumber);
        commitStorageStat(messageCount);
        commitFtsStorage(messageCount);
        commitJobInfo();
        commitPwaEvent();
        commitBackgroundSyncAdoption();
        commitPageResume();

        if (SyntheticTelemetryUtils.chance(3)) {
            commitImgError();
        }
        if (SyntheticTelemetryUtils.chance(1)) {
            commitDbLoadFailure();
        }
    }

    /**
     * Resolves the browser fingerprint to advertise for this burst.
     *
     * <p>A {@link Web} session's device already carries the fingerprint that was
     * sampled once at pairing and persisted with the account store, so it is
     * byte-stable across reconnects and re-logins. When the device is not a
     * {@link Web} descriptor (for example a mobile one), a fresh coherent
     * fingerprint is sampled for the device platform through
     * {@link Web#random(com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType)}.
     *
     * @return the persisted web descriptor, or a freshly sampled one when the
     *         device is not a {@link Web}
     */
    private Web resolveFingerprint() {
        var device = client.store().accountStore().device();
        return device instanceof Web web ? web : Web.random(device.platform());
    }

    /**
     * Fabricates and commits the {@code AppLaunch} (id 1094) runtime launch
     * metrics.
     *
     * <p>The launch and CPU timers, main-thread pre/run splits and IndexedDB
     * read/write counts and durations describe a cold start into the chat list.
     * The process identifier is the real host process id; the trace id is a fresh
     * random correlation token, matching how WA Web mints one per launch.
     */
    @WhatsAppWebExport(moduleName = "WAWebAppLaunchWamEvent", exports = "AppLaunchWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamAppLaunchReporter", exports = "reportAppLaunch", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitAppLaunch() {
        wamService.commit(new AppLaunchEventBuilder()
                .appLaunchTypeT(AppLaunchType.COLD)
                .appLaunchDestination(AppLaunchDestinationType.CHATLIST)
                .appLaunchT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(2200, 900)))
                .appLaunchCpuT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(650, 350)))
                .appLaunchMainPreT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(180, 120)))
                .appLaunchMainRunT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(1400, 500)))
                .dbReadsCount(SyntheticTelemetryUtils.jitter(320, 180))
                .dbWritesCount(SyntheticTelemetryUtils.jitter(90, 60))
                .dbMainThreadCount(SyntheticTelemetryUtils.jitter(40, 30))
                .dbBgThreadReadsDurationT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(120, 90)))
                .dbBgThreadWritesDurationT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(60, 50)))
                .dbMainThreadReadsDurationT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(45, 40)))
                .dbMainThreadWritesDurationT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(25, 25)))
                .lowPowerModeEnabled(false)
                .processIdentifier(ProcessHandle.current().pid())
                .traceIdInt(ThreadLocalRandom.current().nextLong(1, Integer.MAX_VALUE))
                .appContext("chatlist")
                .appContextBitfield(3)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcPageLoad} (id 642) navigation and
     * paint timing event.
     *
     * <p>The navigation-timing offsets form a coherent monotonically increasing
     * timeline from fetch through DNS, connect, request, response, DOM parse and
     * load, and the WA-specific websocket phase timers (opening, pairing, syncing,
     * normal), script and panel mount/render timers reproduce a genuine WA Web
     * bootstrap. The redirect count is zero because a direct navigation performs
     * no redirects, matching what a real load reports.
     *
     * @param pageLoadId the correlation id shared with the {@code WebcPageLoad2}
     *                   event for this load
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcPageLoadWamEvent", exports = "WebcPageLoadWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamPageLoadReporter", exports = "reportPageLoad", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitPageLoad(String pageLoadId) {
        wamService.commit(new WebcPageLoadEventBuilder()
                .webcRedirectCount(0.0)
                .webcFetchStart(SyntheticTelemetryUtils.timer(2))
                .webcDomainLookupStart(SyntheticTelemetryUtils.timer(3))
                .webcDomainLookupEnd(SyntheticTelemetryUtils.timer(4))
                .webcConnectStart(SyntheticTelemetryUtils.timer(4))
                .webcSecureConnectionStart(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(18, 12)))
                .webcConnectEnd(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(55, 25)))
                .webcRequestStart(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(60, 20)))
                .webcResponseStart(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(115, 40)))
                .webcResponseEnd(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(175, 50)))
                .webcDomLoading(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(200, 40)))
                .webcDomInteractive(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(880, 200)))
                .webcDomContentLoadedEventStart(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(905, 30)))
                .webcDomContentLoadedEventEnd(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(945, 40)))
                .webcDomComplete(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(2180, 300)))
                .webcLoadEventStart(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(2200, 30)))
                .webcLoadEventEnd(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(2210, 30)))
                .webcExeStart(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(205, 30)))
                .webcExeDone(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(1760, 200)))
                .webcMainScriptStart(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(210, 30)))
                .webcMainScriptEnd(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(1750, 200)))
                .webcJsLoadT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(1740, 200)))
                .webcNativeLoadT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(255, 80)))
                .webcInitialMountT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(1900, 150)))
                .webcInitialNavMountT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(1950, 120)))
                .webcInitialPanelMountStartT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(1960, 60)))
                .webcInitialPanelMountT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(2000, 120)))
                .webcInitialPanelRenderT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(2100, 150)))
                .webcPageLoadT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(2400, 400)))
                .webcWsOpening(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(300, 120)))
                .webcWsPairing(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(700, 250)))
                .webcWsSyncing(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(1200, 400)))
                .webcWsNormal(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(2300, 400)))
                .webcWsAttempts(1.0)
                .webcAppcacheStatus(WebcAppcacheStatusCode.UNCACHED)
                .webcCached(false)
                .webcNavigation(WebcNavigationType.NAVIGATE_NEXT)
                .webcQrCode(false)
                .webcParallellyFetched(true)
                .webcLoadInForeground(true)
                .webcInitialPanel("chat-list")
                .webcPageLoadId(pageLoadId)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcPageLoad2} (id 5392) page-load event,
     * which carries only the generated page-load id.
     *
     * @param pageLoadId the correlation id shared with the {@code WebcPageLoad}
     *                   event for this load
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcPageLoad2WamEvent", exports = "WebcPageLoad2WamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebPageLoadLoggingImpl", exports = "logPageLoad", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitPageLoad2(String pageLoadId) {
        wamService.commit(new WebcPageLoad2EventBuilder()
                .webcPageLoadId(pageLoadId)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcResourceLoad} (id 688) per-resource
     * load-timing event for a named bootstrap script.
     *
     * <p>The resource is reported as a fresh network fetch (not cached) so its
     * duration is a plausible download-plus-parse span.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcResourceLoadWamEvent", exports = "WebcResourceLoadWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamResourceLoadReporter", exports = "reportResourceLoad", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitResourceLoad() {
        wamService.commit(new WebcResourceLoadEventBuilder()
                .webcResourceName("bootstrap_main." + SyntheticTelemetryUtils.randomHexLower(4) + ".js")
                .webcResourceDuration(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(90, 120)))
                .webcResourceCached(false)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcAssetLoad} (id 1358) asset-load event
     * for a service-worker-cached emoji sprite.
     *
     * <p>The asset is reported as served from the service-worker cache, which is
     * the steady-state path once the sprite has been fetched at least once.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcAssetLoadWamEvent", exports = "WebcAssetLoadWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebAssetLoader", exports = "loadAsset", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitAssetLoad() {
        wamService.commit(new WebcAssetLoadEventBuilder()
                .webcAssetName("emoji/spritemap/64/0.png")
                .webcAssetFromCache(true)
                .webcAssetCacheType(WebcAssetCacheTypeCode.SW)
                .webcAssetLoadT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(12, 30)))
                .webcAssetSize((double) SyntheticTelemetryUtils.jitter(180_000, 60_000))
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcFingerprint} (id 1704) anti-abuse
     * device fingerprint.
     *
     * <p>The report reproduces a genuine Chrome-on-desktop signature: the Blink
     * engine, a false (not undefined) {@code navigator.webdriver}, an empty
     * automation-signals string, the canonical {@code window.chrome} structure,
     * present notification/PDF/web-share/taskbar/chrome capabilities, plausible
     * plugin and mime-type counts, the host platform estimate and CPU platform
     * string, the system timezone, and the browser-environment fields read from
     * the session's persisted device fingerprint (screen resolution, viewport
     * size, device memory, GPU vendor, connection round-trip time and history
     * length) so they stay coherent and stable across reconnects. The canvas and
     * WebGL hashes are seeded from the fingerprint's GPU vendor so they co-vary
     * with the reported renderer string, keeping the WebGL fingerprint consistent
     * with the advertised GPU. The webdriver, automation-signals and
     * chrome-structure fields are the ones the abuse system keys on, so their
     * genuine-browser values are the point of the whole event.
     *
     * @implNote
     * This implementation sets exactly the fields WA Web's
     * {@code WAWebWamFingerprintReporter.logFingerprintToWam} populates from its
     * collector and leaves the collector-absent fields (audio fingerprint,
     * battery level, WebGL vendor/renderer, peripherals) unset, so the emitted
     * shape matches the reporter rather than the full schema. The canvas and WebGL
     * hashes fold the GPU vendor and screen resolution into their seeds so the two
     * digests co-vary with the reported renderer and remain byte-stable for a
     * given persisted fingerprint.
     *
     * @param fingerprint the resolved device browser fingerprint whose
     *                    browser-environment values are reported
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcFingerprintWamEvent", exports = "WebcFingerprintWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamFingerprintReporter", exports = "logFingerprintToWam", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitFingerprint(Web fingerprint) {
        var canvasSeed = "canvas|" + fingerprint.gpuMake() + '|' + fingerprint.screenResolution();
        var webglSeed = "webgl|" + fingerprint.gpuMake() + '|' + fingerprint.screenResolution();
        wamService.commit(new WebcFingerprintEventBuilder()
                .webcWindowNavigatorWebdriver(WebcWindowNavigatorWebdriverType.FALSE)
                .browserEngine(BrowserEngineName.BLINK)
                .hasChrome(true)
                .hasTaskbar(true)
                .hasWebShare(true)
                .notificationPermission(true)
                .pdfViewerEnabled(true)
                .pluginCount(5)
                .mimeTypeCount(2)
                .platformEstimate(platformEstimate())
                .historyLength(fingerprint.historyLength())
                .viewportSize(fingerprint.viewportSize())
                .screenResolution(fingerprint.screenResolution())
                .cpuMake(cpuPlatform())
                .deviceMemory(fingerprint.deviceMemory())
                .gpuMake(fingerprint.gpuMake())
                .connectionRtt(fingerprint.connectionRttMs())
                .touchPresence(false)
                .sessionStorageLength(1)
                .timezone(ZoneId.systemDefault().getId())
                .webcCanvasFingerprint(SyntheticTelemetryUtils.md5Hex(canvasSeed))
                .webcWebglFingerprint(SyntheticTelemetryUtils.md5Hex(webglSeed))
                .automationSignals(NO_AUTOMATION_SIGNALS)
                .chromeStructure(CHROME_STRUCTURE)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcDbOpen} (id 1940) IndexedDB open
     * outcome for the primary model-storage database.
     *
     * <p>The open is reported as a first-attempt success, the overwhelmingly
     * common outcome on a healthy client.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcDbOpenWamEvent", exports = "WebcDbOpenWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWAWCStorage", exports = "open", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitDbOpen() {
        wamService.commit(new WebcDbOpenEventBuilder()
                .webcDbName("model-storage")
                .webcDbOpenWasSuccess(true)
                .webcDbOpenNumAttempts(1)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebDbVersionsSource} (id 4784) schema
     * rollout event.
     *
     * <p>The version is reported as coming from the static baked-in schema, driven
     * by the main thread, the default rollout path for a client that is not under
     * a knob override.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebDbVersionsSourceWamEvent", exports = "WebDbVersionsSourceWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebDbRolloutUtil", exports = "reportVersionsSource", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitDbVersionsSource() {
        wamService.commit(new WebDbVersionsSourceEventBuilder()
                .webDbVersionSource(WebDbVersionSourceType.STATIC)
                .webSchemaInitiator(WebSchemaInitiatorType.MAIN)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebDbVersionNonAnonymous} (id 4816) schema
     * version report for the model-storage database.
     *
     * <p>The version number is a plausible current WA Web IndexedDB schema
     * version.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebDbVersionNonAnonymousWamEvent", exports = "WebDbVersionNonAnonymousWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebReportDbVerionsJob", exports = "reportDbVersions", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitDbVersion() {
        wamService.commit(new WebDbVersionNonAnonymousEventBuilder()
                .webDbName(WebDbNameType.MODEL_STORAGE)
                .webDbVersionNumber(1300)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebDbTableUsage} (id 5074) per-table
     * read/write report for the message table during an offline-resume scenario.
     *
     * <p>WA Web commits this event for the message table unconditionally and for
     * any other table that crosses its read/write threshold; the message table is
     * therefore the faithful subject. The read count is derived from the live
     * message total so it scales with the store.
     *
     * @param messageCount the total number of stored messages, used to scale the
     *                     reported read count
     */
    @WhatsAppWebExport(moduleName = "WAWebWebDbTableUsageWamEvent", exports = "WebDbTableUsageWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebDBTableUsage", exports = "beginDBTableUsage", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitDbTableUsage(long messageCount) {
        wamService.commit(new WebDbTableUsageEventBuilder()
                .offlineSessionId(SyntheticTelemetryUtils.randomHexLower(8))
                .webScenario(WebScenarioCode.OFFLINE_RESUME)
                .webTable("message")
                .webTableLogReason(WebTableLogReasonCode.BASE)
                .webTableReadCount(Math.max(1, messageCount) + SyntheticTelemetryUtils.jitter(50, 100))
                .webTableWriteCount(SyntheticTelemetryUtils.jitter(10, 40))
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcChatOpen} (id 864) chat-open render
     * metrics.
     *
     * <p>When a chat is available its real message count drives the rendered and
     * final-rendered counts and its unread count is reported verbatim; otherwise a
     * small plausible virtual conversation is described. The paint timers form a
     * coherent before-paint, painted and open sequence, and the window height is
     * the height parsed from the device fingerprint's viewport size so it agrees
     * with the viewport reported in the fingerprint event.
     *
     * @param chat        the busiest chat to describe, or {@code null} when the
     *                    store holds no chats
     * @param fingerprint the resolved device browser fingerprint whose viewport
     *                    height is reported as the window height
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcChatOpenWamEvent", exports = "WebcChatOpenWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebChatOpenMetricsStore", exports = "commitChatOpen", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitChatOpen(Chat chat, Web fingerprint) {
        var total = chat != null ? chat.messageCount() : (int) SyntheticTelemetryUtils.jitter(40, 120);
        var rendered = Math.min(total, 30);
        var unread = chat != null ? chat.unreadCount().orElse(0) : 0;
        wamService.commit(new WebcChatOpenEventBuilder()
                .webcChatOpenBeforePaintT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(18, 20)))
                .webcChatOpenPaintedT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(55, 40)))
                .webcChatOpenT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(90, 60)))
                .webcRenderedMessageCount(rendered)
                .webcFinalRenderedMessageCount(total)
                .webcUnreadCount((double) unread)
                .webcWindowHeightFloat(parseViewportHeight(fingerprint.viewportSize()))
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcProgressiveImage} (id 2226)
     * progressive-JPEG render event.
     *
     * <p>The scan count and first/mid/full quality timers describe an incremental
     * paint of a downloaded photo, the surface WA Web instruments when a browser
     * decodes a progressive JPEG.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcProgressiveImageWamEvent", exports = "WebcProgressiveImageWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebMediaMmsV4Download", exports = "reportProgressiveImage", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitProgressiveImage() {
        wamService.commit(new WebcProgressiveImageEventBuilder()
                .webcFirstRenderScans(SyntheticTelemetryUtils.jitter(2, 3))
                .webcFirstRenderT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(60, 60)))
                .webcMidQualityT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(140, 90)))
                .webcFullQualityT(SyntheticTelemetryUtils.timer(SyntheticTelemetryUtils.jitter(260, 140)))
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcMemoryStat} (id 1188) JS-heap and
     * collection-size sample.
     *
     * <p>The heap figures are the live JVM values ({@link Runtime#maxMemory()} as
     * the limit, {@link Runtime#totalMemory()} as the total, and total minus free
     * as the used heap), reported as the browser's {@code performance.memory} would
     * be. The collection sizes are the real store counts, and the per-collection
     * byte sizes are derived from those counts with plausible per-record widths.
     *
     * @param chatCount    the number of chats in the store
     * @param contactCount the number of contacts in the store
     * @param messageCount the number of stored messages
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcMemoryStatWamEvent", exports = "WebcMemoryStatWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamMemoryStat", exports = "sampleWebcMemory", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitWebcMemoryStat(int chatCount, int contactCount, long messageCount) {
        var runtime = Runtime.getRuntime();
        var used = runtime.totalMemory() - runtime.freeMemory();
        wamService.commit(new WebcMemoryStatEventBuilder()
                .jsHeapSizeLimit(runtime.maxMemory())
                .totalJsHeapSize(runtime.totalMemory())
                .usedJsHeapSize(used)
                .peakUsedJsHeapSize(used + SyntheticTelemetryUtils.jitter(4_000_000, 8_000_000))
                .usedJsHeapSizeDelta(SyntheticTelemetryUtils.jitter(500_000, 3_000_000))
                .uptime(uptimeSeconds())
                .isForeground(true)
                .scenario(WebcScenarioType.IDLE)
                .webcRuntimeEnv(WebcRuntimeEnvCode.MAIN)
                .chatCollectionSize(chatCount)
                .chatDbSize((long) chatCount * 2048)
                .contactCollectionSize(contactCount)
                .contactDbSize((long) contactCount * 512)
                .messageCollectionSize(messageCount)
                .messageDbSize(messageCount * 1024)
                .appContext("chatlist")
                .appContextBitfield(3)
                .build());
    }

    /**
     * Fabricates and commits the {@code MemoryStat} (id 1336) process-memory
     * sample.
     *
     * <p>The working-set, private and shared byte figures are derived from the
     * live JVM heap plus a native-overhead allowance so they read like a browser
     * tab's resident set. The message count and verified-number flag are the real
     * store values.
     *
     * @param messageCount      the number of stored messages
     * @param hasVerifiedNumber whether the bound account has a verified phone
     *                          number
     */
    @WhatsAppWebExport(moduleName = "WAWebMemoryStatWamEvent", exports = "MemoryStatWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamMemoryStat", exports = "sampleProcessMemory", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMemoryStat(long messageCount, boolean hasVerifiedNumber) {
        var runtime = Runtime.getRuntime();
        var used = runtime.totalMemory() - runtime.freeMemory();
        var workingSet = (double) used + 150_000_000.0;
        wamService.commit(new MemoryStatEventBuilder()
                .workingSetSize(workingSet)
                .workingSetPeakSize(workingSet + SyntheticTelemetryUtils.jitter(20_000_000, 40_000_000))
                .privateBytes(workingSet * 0.75)
                .sharedBytes(workingSet * 0.25)
                .uptime(uptimeSeconds())
                .processType("browser")
                .numMessages((double) messageCount)
                .hasVerifiedNumber(hasVerifiedNumber)
                .appContext("chatlist")
                .appContextBitfield(3)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcStorageStat} (id 1504) browser-storage
     * sample.
     *
     * <p>The quota is derived from the host disk (sixty percent of the home
     * volume, the fraction Chrome grants an origin) so it is plausible and
     * host-specific, and the usage scales with the live message total over a
     * captured baseline. Packing is reported enabled, the steady state once the
     * client has compacted its store at least once.
     *
     * @param messageCount the number of stored messages, used to scale the
     *                     reported usage
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcStorageStatWamEvent", exports = "WebcStorageStatWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCleanAssets", exports = "reportStorageStat", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitStorageStat(long messageCount) {
        wamService.commit(new WebcStorageStatEventBuilder()
                .webcStorageQuota(browserStorageQuota())
                .webcStorageUsage(120_000_000L + messageCount * 1024)
                .webcAgeOfStorage(SyntheticTelemetryUtils.jitter(7L * 86_400_000L, 53L * 86_400_000L))
                .webcPackingEnabled(true)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcFtsStorage} (id 3642) full-text-search
     * index size sample.
     *
     * <p>The reported index size scales with the live message total over a
     * captured baseline, mirroring how the daily-stats task estimates the FTS
     * index size from the message corpus.
     *
     * @param messageCount the number of stored messages, used to scale the
     *                     reported index size
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcFtsStorageWamEvent", exports = "WebcFtsStorageWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebTasksDailyStatsTask", exports = "reportFtsStorage", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitFtsStorage(long messageCount) {
        wamService.commit(new WebcFtsStorageEventBuilder()
                .ftsTotalSize(4_000_000L + messageCount * 256)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcJobInfo} (id 3054) job-orchestrator
     * stats event for an offline-resume job.
     *
     * <p>The job is described as a completed high-priority offline-resume task with
     * a coherent added/started/completed timeline and a small pending-queue depth,
     * the shape the WA Web job orchestrator logs on job completion.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcJobInfoWamEvent", exports = "WebcJobInfoWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebOrchestratorJobStatsLogger", exports = "logJobStats", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitJobInfo() {
        var added = System.currentTimeMillis();
        wamService.commit(new WebcJobInfoEventBuilder()
                .jobName("offline-resume")
                .jobPriority("HIGH")
                .jobResultType(WebcJobResultTypeCode.COMPLETED)
                .scenario(WebcScenarioType.OFFLINE_RESUME)
                .pendingJobsCount(SyntheticTelemetryUtils.jitter(1, 5))
                .webcJobAddedT(added)
                .webcJobStartedT(added + SyntheticTelemetryUtils.jitter(3, 12))
                .webcJobCompletedT(added + SyntheticTelemetryUtils.jitter(80, 200))
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcPwaEvent} (id 4116) progressive-web-app
     * lifecycle event.
     *
     * <p>The only defined action is the install signal, reported here to describe
     * an installable-app session.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcPwaEventWamEvent", exports = "WebcPwaEventWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebPwaEventListeners", exports = "onPwaEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitPwaEvent() {
        wamService.commit(new WebcPwaEventEventBuilder()
                .webcPwaAction(WebcPwaActionType.INSTALL)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcBackgroundSyncAdoption} (id 5302)
     * push/background-sync adoption event.
     *
     * <p>The report describes a default onboarding with the notification shown and
     * OS notifications allowed, the common adoption path for a session that has
     * opted into background sync.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcBackgroundSyncAdoptionWamEvent", exports = "WebcBackgroundSyncAdoptionWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebBackgroundSyncReporter", exports = "reportAdoption", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitBackgroundSyncAdoption() {
        wamService.commit(new WebcBackgroundSyncAdoptionEventBuilder()
                .onboardSource(OnboardSources.DEFAULT)
                .pushNotificationInteraction(PushNotificationInteractions.SHOWN)
                .webOsNotificationSetting(WebNotificationSettingType.ALLOWED)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcPageResume} (id 884) page-resume
     * counter.
     *
     * <p>The count reports how many times the page has resumed from a suspended
     * state; a small positive value describes a session that has been backgrounded
     * and foregrounded a handful of times.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcPageResumeWamEvent", exports = "WebcPageResumeWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebStreamModel", exports = "onPageResume", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitPageResume() {
        wamService.commit(new WebcPageResumeEventBuilder()
                .webcResumeCount(SyntheticTelemetryUtils.jitter(1, 4))
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcImgError} (id 1700) image-fetch error.
     *
     * <p>This is emitted only rarely from {@link #runBurst()} because a healthy
     * load renders its images without exhausting retries; the reported code is a
     * plausible HTTP not-found status for the failed fetch.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcImgErrorWamEvent", exports = "WebcImgErrorWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebImgRetry", exports = "onImgError", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitImgError() {
        wamService.commit(new WebcImgErrorEventBuilder()
                .webcImgErrorCode(404.0)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebDbLoadFromVersionFailureNonAnonymous}
     * (id 4814) schema-load failure.
     *
     * <p>This is emitted only rarely from {@link #runBurst()} because a healthy
     * client opens its databases without a version-load failure; the report names
     * the main-thread loader and the signal-storage database as the failing pair.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebDbLoadFromVersionFailureNonAnonymousWamEvent", exports = "WebDbLoadFromVersionFailureNonAnonymousWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSchemaVersions", exports = "loadFromVersion", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitDbLoadFailure() {
        wamService.commit(new WebDbLoadFromVersionFailureNonAnonymousEventBuilder()
                .webDbLoader(WebDbLoaderType.MAIN)
                .webDbName(WebDbNameType.SIGNAL_STORAGE)
                .build());
    }

    /**
     * Parses the inner window height, in pixels, from a {@code WIDTHxHEIGHT}
     * viewport-size string.
     *
     * <p>The device fingerprint reports its viewport as a {@code WIDTHxHEIGHT}
     * string (for example {@code "1903x945"}); this returns the height component as
     * the {@code double} the {@code webcWindowHeightFloat} chat-open field expects,
     * yielding {@code 0.0} when the string carries no separator.
     *
     * @param viewportSize the {@code WIDTHxHEIGHT} viewport-size string
     * @return the parsed height component, or {@code 0.0} when the string is
     *         malformed
     */
    private static double parseViewportHeight(String viewportSize) {
        var separator = viewportSize.indexOf('x');
        return separator < 0 ? 0.0 : Double.parseDouble(viewportSize.substring(separator + 1));
    }

    /**
     * Returns the JVM uptime in seconds.
     *
     * <p>This is the real host uptime since JVM start, reported in the memory
     * samples' {@code uptime} field so the value tracks the live process rather
     * than a frozen constant.
     *
     * @return the JVM uptime in seconds
     */
    private static double uptimeSeconds() {
        return ProcessHandle.current()
                .info()
                .startInstant()
                .map(start -> Duration.between(start, Instant.now()).toSeconds())
                .orElse(0L);
    }

    /**
     * Estimates the browser-storage quota a real WhatsApp Web session would
     * advertise.
     *
     * <p>Chrome grants an origin roughly sixty percent of the host's total disk as
     * its storage quota, so this returns sixty percent of the home volume's total
     * space, falling back to a captured constant when the size cannot be read.
     * Deriving the value from the actual host disk keeps the reported quota
     * plausible and host-specific.
     *
     * @return the estimated browser-storage quota in bytes
     */
    private static long browserStorageQuota() {
        var total = new File(System.getProperty("user.home", ".")).getTotalSpace();
        return total > 0 ? total / 10 * 6 : 323172190617L;
    }

    /**
     * Maps the host operating system to the {@code platformEstimate} fingerprint
     * enum.
     *
     * @return the {@link PlatformName} matching the host OS, or
     *         {@link PlatformName#UNKNOWN} when it cannot be classified
     */
    private static PlatformName platformEstimate() {
        var os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return PlatformName.WINDOWS;
        }
        if (os.contains("mac")) {
            return PlatformName.MAC;
        }
        if (os.contains("nux") || os.contains("nix") || os.contains("aix")) {
            return PlatformName.LINUX;
        }
        return PlatformName.UNKNOWN;
    }

    /**
     * Maps the host operating system to the {@code cpuMake} fingerprint string,
     * mirroring the {@code navigator.userAgentData.platform} value a browser
     * reports.
     *
     * @return the platform label matching the host OS
     */
    private static String cpuPlatform() {
        var os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "Windows";
        }
        if (os.contains("mac")) {
            return "macOS";
        }
        if (os.contains("nux") || os.contains("nix") || os.contains("aix")) {
            return "Linux";
        }
        return "Unknown";
    }
}

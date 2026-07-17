package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.CrashLogEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdExpansionAgentBrowserMdIdEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdLinkedDevicesWindowsXdrEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcMinorEventLogEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcWebtpPdfViewerEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.CrashApplicationState;
import com.github.auties00.cobalt.wire.wam.type.CrashType;
import com.github.auties00.cobalt.wire.wam.type.LogType;
import com.github.auties00.cobalt.wire.wam.type.MdLinkedDevicesWindowsXdrStage;
import com.github.auties00.cobalt.wire.wam.type.MdXdrTransportType;
import com.github.auties00.cobalt.wire.wam.type.MultideviceActionType;
import com.github.auties00.cobalt.wire.wam.type.WebtpEventType;
import com.github.auties00.cobalt.wire.wam.type.WebtpSourceType;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Emits the synthetic WhatsApp Web crash, minor-event-log, multi-device agent,
 * PDF-viewer and Windows cross-device-resume WAM telemetry that a genuine WA Web
 * or WA-on-Windows-desktop session produces, so a Cobalt session's metrics stream
 * is indistinguishable from a real client's on these diagnostic surfaces.
 *
 * <p>WhatsApp Web reports a family of diagnostic and shell-lifecycle events that
 * have no counterpart feature in a headless JVM client:
 * <ul>
 * <li>{@code CrashLog} (494) and {@code WebcMinorEventLog} (5440), committed from
 * the browser crash-logger ({@code WAWebCrashlog.upload}) whenever the JS runtime
 * records a "sad" fatal path (uncaught error, unhandled rejection, main-thread
 * stall, forced logout) or an ordinary minor event / counting stat.</li>
 * <li>{@code MdExpansionAgentBrowserMdId} (3390), committed from the business
 * multi-device agent lifecycle ({@code WAWebBizAgentAction.initializeAgentLog})
 * when a business agent's browser session logs in, becomes active, is logged out
 * or opens message-info.</li>
 * <li>{@code WebcWebtpPdfViewer} (7506), committed from the embedded third-party
 * PDF viewer SDK logging util ({@code WAWebTPLoggingUtils.logDocumentOpenEvent})
 * as the viewer opens, closes, errors or emits telemetry while rendering a PDF.</li>
 * <li>{@code MdLinkedDevicesWindowsXdr} (7804), committed from the Windows desktop
 * hybrid-app activation bridge ({@code WAWebExecApiCmd}) as it drives a
 * cross-device-resume (XDR) deep-link between linked devices.</li>
 * </ul>
 * None of these map to a Cobalt feature: Cobalt has a deliberately redesigned
 * pluggable error model rather than a JS-runtime crash reporter, no browser UI in
 * which a business agent's tab logs in and out, no in-app PDF viewer SDK, and no
 * Windows desktop shell. Their absence is nonetheless a fingerprint, so this
 * service fabricates one plausible, host-derived occurrence of each per
 * connection.
 *
 * <p>Every field WA sets is populated with either a real host-sourced value (the
 * live process id, the account's companion device id and linked-device count, the
 * host operating system) or a realistic constant captured from a genuine
 * Chrome-on-desktop WA Web session (crash reasons, log reasons, PDF file sizes,
 * XDR transport). No obviously-fake sentinel values are used, and the iOS/Android
 * only crash fields (iPhone process, time-spent surface, UFAD report type) are
 * left unset because a browser session never carries them, so the emitted shape
 * matches the WA Web reporters rather than the full cross-platform schema.
 *
 * @implNote
 * This implementation fires the whole set from a single
 * {@link #emitSessionTelemetry()} entry point. The counting-stat minor-event log
 * is committed on every connection because counting stats are the steady-state
 * output of a healthy WA Web session; the business-agent login is committed only
 * for a business account, mirroring the gate on the WA Web feature; and the
 * crash, PDF-open and Windows-XDR events are committed only with a small
 * per-session probability through {@link SyntheticTelemetryUtils#chance(int)} so that, across a fleet of
 * sessions, they surface at a realistic low rate rather than on every connection.
 * The Windows-XDR event is additionally gated on a Windows host because only the
 * Windows desktop shell, not a browser on another operating system, ever emits it.
 *
 * @see WamService
 */
@WhatsAppWebModule(moduleName = "WAWebCrashlog")
@WhatsAppWebModule(moduleName = "WAWebBizAgentAction")
@WhatsAppWebModule(moduleName = "WAWebTPLoggingUtils")
@WhatsAppWebModule(moduleName = "WAWebExecApiCmd")
public final class SyntheticCrashTelemetry {
    /**
     * The application-context label reported in the crash event's
     * {@code appContext} field, matching the chat-list context a WA Web session
     * reports for a crash raised while the chat list is the active surface.
     */
    private static final String APP_CONTEXT = "chatlist";

    /**
     * The application-context bitfield reported alongside {@link #APP_CONTEXT}; the
     * canonical value a WA Web chat-list session reports.
     */
    private static final long APP_CONTEXT_BITFIELD = 3L;

    /**
     * The tag string reported in the crash event's {@code crashContext} field,
     * matching the {@code sad} tag the WA Web crash logger prepends for a "sad"
     * (fatal-path) upload.
     */
    private static final String CRASH_CONTEXT = "sad";

    /**
     * The crash reason reported in the crash event's {@code crashReason} field; a
     * realistic snake-case reason of the shape the WA Web crash logger records for
     * a recovered runtime exception.
     */
    private static final String CRASH_REASON = "unhandled_rejection_gracefully_recovered";

    /**
     * The log reason reported in the minor-event log's {@code logReason} field; a
     * realistic counting-stat key of the shape a healthy WA Web session emits on a
     * routine app-state sync.
     */
    private static final String MINOR_LOG_REASON = "counting_stat:syncd_mutation_applied";

    /**
     * The tag string reported in the minor-event log's {@code logContext} field,
     * matching the {@code counting} tag the WA Web crash logger attaches to a
     * counting-stat log.
     */
    private static final String MINOR_LOG_CONTEXT = "counting";

    /**
     * The bound WhatsApp client whose store supplies the live account, device and
     * business state sampled into the emitted events.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every fabricated diagnostic event is committed
     * for batched upload.
     */
    private final WamService wamService;

    /**
     * Constructs a new {@code SyntheticCrashTelemetry} bound to the given client
     * and WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be
     *                   {@code null}
     * @param wamService the WAM service used to commit the emitted events, must not
     *                   be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticCrashTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
    }

    /**
     * Emits the once-per-connection diagnostic telemetry set on a dedicated virtual
     * thread.
     *
     * <p>This is the single entry point the client drives after the socket opens.
     * It always commits one counting-stat minor-event log (the steady-state output
     * of a healthy WA Web session), commits the business multi-device agent login
     * when the bound account is a business account, and, at a realistic low rate,
     * commits a recovered-crash report and a PDF-viewer open; on a Windows host it
     * additionally commits, at a low rate, one Windows cross-device-resume stage.
     *
     * @apiNote
     * The crash and minor-event logs are event-driven by nature on WA Web (its
     * crash logger fires them from error handlers and counting-stat call sites
     * rather than on a fixed schedule); Cobalt has no such call sites, so it folds
     * one representative sample into the per-connection set. A caller that wants a
     * higher counting-stat cadence may invoke this method again on a timer.
     *
     * @implNote
     * This implementation fires only for a {@link LinkedWhatsAppClientType#WEB}
     * session and returns without emitting anything for a
     * {@link LinkedWhatsAppClientType#MOBILE} (primary phone-registration) session,
     * because the whole crash-logger, agent, PDF-viewer and XDR family is WhatsApp
     * Web browser and Windows-desktop telemetry that a primary device, which runs
     * no browser or desktop shell, never emits.
     */
    public void emitSessionTelemetry() {
        if (client.store().accountStore().clientType() != LinkedWhatsAppClientType.WEB) {
            return;
        }
        Thread.ofVirtual()
                .name("crash-telemetry")
                .start(this::runBurst);
    }

    /**
     * Builds and commits the full diagnostic set.
     *
     * <p>The store is sampled once for the account and device state that feeds the
     * agent-login event; the always-on minor-event log is committed first, then the
     * conditional and low-rate events.
     */
    private void runBurst() {
        commitMinorEventLog();

        var accountStore = client.store().accountStore();
        if (isBusinessAccount()) {
            commitAgentLogin(accountStore.jid().map(Jid::device).orElse(0), accountStore.linkedDevices().size());
        }

        if (SyntheticTelemetryUtils.chance(4)) {
            commitCrashLog();
        }
        if (SyntheticTelemetryUtils.chance(6)) {
            commitPdfViewerOpen();
        }
        if (isWindowsHost() && SyntheticTelemetryUtils.chance(8)) {
            commitWindowsXdr();
        }
    }

    /**
     * Fabricates and commits the {@code CrashLog} (id 494) recovered-crash report.
     *
     * <p>The report describes a gracefully recovered unhandled rejection with the
     * foreground application state, a single occurrence, the chat-list application
     * context and the {@code sad} crash-context tag, matching the shape the WA Web
     * crash logger commits on its fatal-path ("sad") upload. The process identifier
     * is the real host process id, and the {@code iPhone}/UFAD/time-spent fields
     * are left unset because a browser session never carries them.
     */
    @WhatsAppWebExport(moduleName = "WAWebCrashLogWamEvent", exports = "CrashLogWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCrashlog", exports = "upload", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitCrashLog() {
        wamService.commit(new CrashLogEventBuilder()
                .crashReason(CRASH_REASON)
                .crashType(CrashType.UX_GRACEFUL_RECOVERY_EXCEPTION)
                .crashCount(1)
                .crashApplicationState(CrashApplicationState.FOREGROUND)
                .crashContext(CRASH_CONTEXT)
                .appContext(APP_CONTEXT)
                .appContextBitfield(APP_CONTEXT_BITFIELD)
                .lowPowerModeEnabled(false)
                .processIdentifier(ProcessHandle.current().pid())
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcMinorEventLog} (id 5440) counting-stat
     * log.
     *
     * <p>The log describes a routine counting stat with a plausible occurrence
     * count, matching the non-"sad" path of the WA Web crash logger that records
     * ordinary minor events and counting stats. This is the steady-state diagnostic
     * output of a healthy session, so it is committed on every connection rather
     * than at a reduced rate.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcMinorEventLogWamEvent", exports = "WebcMinorEventLogWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCrashlog", exports = "upload", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMinorEventLog() {
        wamService.commit(new WebcMinorEventLogEventBuilder()
                .logType(LogType.COUNTING_STAT)
                .logReason(MINOR_LOG_REASON)
                .logContext(MINOR_LOG_CONTEXT)
                .logCount(SyntheticTelemetryUtils.jitter(1, 6))
                .build());
    }

    /**
     * Fabricates and commits the {@code MdExpansionAgentBrowserMdId} (id 3390)
     * business multi-device agent login event.
     *
     * <p>The event describes a login action for the agent's browser session: a
     * host-stable expiring browser id, a host-stable agent id, the real companion
     * device id, the real linked-device count, the current login timestamp and the
     * login action, matching the payload the WA Web business client commits from
     * {@code initializeAgentLog} when an agent's tab logs in. This mirrors WA Web,
     * where the event is emitted only for a business account under the multi-device
     * message-attribution gate.
     *
     * @param companionDeviceId the account's companion device id, reported as the
     *                          {@code companionMdId} field
     * @param linkedDeviceCount the number of linked devices, reported as the
     *                          {@code mdLinkedCount} field
     */
    @WhatsAppWebExport(moduleName = "WAWebMdExpansionAgentBrowserMdIdWamEvent", exports = "MdExpansionAgentBrowserMdIdWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebBizAgentAction", exports = "initializeAgentLog", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitAgentLogin(int companionDeviceId, int linkedDeviceCount) {
        wamService.commit(new MdExpansionAgentBrowserMdIdEventBuilder()
                .agentId(agentId())
                .browserId(browserId())
                .companionMdId(companionDeviceId)
                .mdLinkedCount(linkedDeviceCount)
                .loginTimestamp(Instant.now().getEpochSecond())
                .isCustomAgentName(true)
                .isNewAgent(false)
                .multideviceAction(MultideviceActionType.LOGIN)
                .build());
    }

    /**
     * Fabricates and commits the {@code WebcWebtpPdfViewer} (id 7506) PDF-viewer
     * open event.
     *
     * <p>The event describes the embedded PDF viewer opening a received document: a
     * per-open session id, a plausible file size, the PDF-viewer source and the
     * telemetry-data blob carrying the sample rate, matching the payload the WA Web
     * third-party PDF logging util commits from {@code logDocumentOpenEvent}. It is
     * committed only at a low rate because a session opens a PDF only occasionally.
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcWebtpPdfViewerWamEvent", exports = "WebcWebtpPdfViewerWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebTPLoggingUtils", exports = "logDocumentOpenEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitPdfViewerOpen() {
        wamService.commit(new WebcWebtpPdfViewerEventBuilder()
                .webtpEvent(WebtpEventType.OPEN)
                .webtpSessionId(SyntheticTelemetryUtils.randomHexLower(8))
                .webtpFileSize((double) SyntheticTelemetryUtils.jitter(180_000, 4_000_000))
                .webtpSource(WebtpSourceType.PDF_VIEWER)
                .webtpTelemetryData("{\"sampleRate\":1}")
                .build());
    }

    /**
     * Fabricates and commits the {@code MdLinkedDevicesWindowsXdr} (id 7804)
     * cross-device-resume stage event.
     *
     * <p>The event describes a successful deep-link navigation stage of the Windows
     * desktop cross-device-resume flow with a per-resume session uuid over the
     * Windows push transport, matching the payload the Windows hybrid-app activation
     * bridge commits from {@code WAWebExecApiCmd} on a deep-link resume. It is
     * emitted only on a Windows host and only at a low rate because it belongs to
     * the Windows desktop shell rather than a browser session.
     */
    @WhatsAppWebExport(moduleName = "WAWebMdLinkedDevicesWindowsXdrWamEvent", exports = "MdLinkedDevicesWindowsXdrWamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebExecApiCmd", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitWindowsXdr() {
        wamService.commit(new MdLinkedDevicesWindowsXdrEventBuilder()
                .mdLinkedDevicesWindowsXdrStage(MdLinkedDevicesWindowsXdrStage.DEEPLINK_NAVIGATION_SUCCESS)
                .mdXdrSessionUuid(UUID.randomUUID().toString())
                .mdXdrTransportType(MdXdrTransportType.WNS)
                .build());
    }

    /**
     * Reports whether the bound account is a WhatsApp Business account.
     *
     * <p>A business account is identified by the presence of a verified business
     * name or at least one business category on the account store; the multi-device
     * agent login event is emitted only for such accounts, matching the WA Web gate.
     *
     * @return {@code true} when the bound account is a business account
     */
    private boolean isBusinessAccount() {
        var accountStore = client.store().accountStore();
        return accountStore.verifiedName().isPresent() || !accountStore.businessCategories().isEmpty();
    }

    /**
     * Reports whether the host operating system is Windows.
     *
     * <p>Used to gate the Windows cross-device-resume event, which only the Windows
     * desktop shell emits, so that a Cobalt session on a non-Windows host does not
     * report a Windows-only surface it would never legitimately produce.
     *
     * @return {@code true} when the host operating system is Windows
     */
    private static boolean isWindowsHost() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /**
     * Builds the host-stable expiring browser id reported in the agent-login event.
     *
     * <p>WhatsApp Web derives this from {@code persistentExpiringId}, a value that
     * is stable for a browser install but rotates over time. Hashing the host
     * identity together with the current day bucket reproduces that behaviour: the
     * id is constant within a day for a given host (so an agent's re-logins share
     * it) yet rotates across days and differs across installs.
     *
     * @return the host-stable expiring browser id
     */
    private static String browserId() {
        var dayBucket = Instant.now().getEpochSecond() / 86_400L;
        return SyntheticTelemetryUtils.md5Hex("browser|" + hostIdentity() + '|' + dayBucket);
    }

    /**
     * Builds the host-stable agent id reported in the agent-login event.
     *
     * <p>WhatsApp Web's agent id is the agent collection's stable identifier;
     * hashing the host identity produces a value that persists across reconnects
     * for a given install yet differs across installs, of the same shape.
     *
     * @return the host-stable agent id
     */
    private static String agentId() {
        return SyntheticTelemetryUtils.md5Hex("agent|" + hostIdentity()).substring(0, 16);
    }



    /**
     * Builds the stable per-host identity seed hashed into the browser and agent
     * ids.
     *
     * <p>Combining the OS name, architecture, version and user name yields a value
     * that is constant for a given install yet differs across hosts, so the derived
     * ids persist as a real install's would while distinct installs do not collide.
     *
     * @return the host identity seed
     */
    private static String hostIdentity() {
        return System.getProperty("os.name", "") + '|'
                + System.getProperty("os.arch", "") + '|'
                + System.getProperty("os.version", "") + '|'
                + System.getProperty("user.name", "");
    }


}

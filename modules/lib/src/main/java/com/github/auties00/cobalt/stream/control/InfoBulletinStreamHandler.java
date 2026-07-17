package com.github.auties00.cobalt.stream.control;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.linked.LinkedTosNoticesChangedListener;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientOfflineResumeState;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.clientexpiration.SmaxClientExpirationResponse;
import com.github.auties00.cobalt.stream.NodeStreamService;
import com.github.auties00.cobalt.sync.WebAppStateService;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.MdAppStateDirtyBitsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.OfflineResumeEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcOfflineNotificationProcessEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.OfflineProcessRunReasons;
import com.github.auties00.cobalt.wire.wam.type.OfflineProcessStages;
import com.github.auties00.cobalt.wire.wam.type.OfflineResumeResultType;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Handles {@code <ib>} (info bulletin) stanzas, the server's catch-all control-plane channel for asynchronous
 * notifications that do not fit any other stanza tag.
 *
 * <p>The handler is registered under the {@code "ib"} tag inside {@link NodeStreamService} and inspects child tags in a
 * fixed priority order: {@code dirty} dirty-bit bundles that drive re-fetch of out-of-date subsystems,
 * {@code edge_routing} updates that steer the next reconnect, {@code offline} backlog counters that close the
 * offline-resume state machine, {@code priority_offline_complete} markers, {@code offline_preview} pre-delivery
 * snapshots, {@code tos} Terms-of-Service notice lists, {@code thread_metadata} per-thread offline timestamps and
 * {@code client_expiration} server-mandated client expiration overrides. The first matching child drives dispatch;
 * later children are ignored. The offline-resume side effects surface through {@link LinkedWhatsAppClient}.
 *
 * @implNote This implementation collapses WA Web's two-phase parse-then-dispatch flow into a single ordered
 * {@code if/else} chain, one private method per branch, and wraps the dispatch in a {@link Throwable} catch so a
 * malformed bulletin cannot propagate up through the socket reader.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleInfoBulletin")
@WhatsAppWebModule(moduleName = "WAWebHandleDirtyBits")
@WhatsAppWebModule(moduleName = "WAWebClearDirtyBitsJob")
@WhatsAppWebModule(moduleName = "WAWebHandleRoutingInfo")
@WhatsAppWebModule(moduleName = "WAWebHandleServerClientExpiration")
@WhatsAppWebModule(moduleName = "WASmaxClientExpirationClientExpirationRPC")
@WhatsAppWebModule(moduleName = "WAWebWamOfflineResumeReporter")
@WhatsAppWebModule(moduleName = "WAWebWamWorkerOfflineProcessReporter")
public final class InfoBulletinStreamHandler extends SocketStreamHandler.Concurrent {
    /**
     * The logger for {@link InfoBulletinStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(InfoBulletinStreamHandler.class);

    /**
     * The child tag carrying dirty-bit notifications.
     */
    private static final String INFO_TYPE_DIRTY = "dirty";

    /**
     * The child tag carrying an {@code edge_routing} routing-info bundle.
     */
    private static final String INFO_TYPE_ROUTING = "edge_routing";

    /**
     * The child tag carrying the offline-delivery completion counter.
     */
    private static final String INFO_TYPE_OFFLINE = "offline";

    /**
     * The child tag carrying the priority-offline completion marker.
     */
    private static final String INFO_TYPE_OFFLINE_PRIORITY_COMPLETE = "priority_offline_complete";

    /**
     * The child tag carrying categorised offline message counts pushed before the backlog drains.
     */
    private static final String INFO_TYPE_OFFLINE_PREVIEW = "offline_preview";

    /**
     * The child tag carrying pending Terms-of-Service notices.
     */
    private static final String INFO_TYPE_TOS = "tos";

    /**
     * The child tag carrying per-thread offline timestamps.
     */
    private static final String INFO_TYPE_THREAD_META = "thread_metadata";

    /**
     * The child tag carrying the server-mandated client expiration override.
     */
    private static final String INFO_TYPE_CLIENT_EXPIRATION = "client_expiration";

    /**
     * The {@code dirty.type} value that triggers an app-state syncd collection pull.
     */
    private static final String DIRTY_TYPE_SYNCD_APP_STATE = "syncd_app_state";

    /**
     * The {@code dirty.type} value that triggers account-level subsystem refreshes ({@code devices}, {@code picture},
     * {@code privacy}, {@code blocklist}, {@code notice}, {@code optoutlist}).
     */
    private static final String DIRTY_TYPE_ACCOUNT_SYNC = "account_sync";

    /**
     * The {@code dirty.type} value that triggers a deferred group metadata refresh after offline delivery ends.
     */
    private static final String DIRTY_TYPE_GROUPS = "groups";

    /**
     * The {@code dirty.type} value that triggers a deferred newsletter metadata refresh after offline delivery ends.
     */
    private static final String DIRTY_TYPE_NEWSLETTER_METADATA = "newsletter_metadata";

    /**
     * The set of supported {@code account_sync} child protocol names that map to a Cobalt-side refresh action.
     *
     * <p>Any value outside this set is ignored when iterating the children of an {@code account_sync} dirty entry.
     */
    private static final Set<String> SUPPORTED_DIRTY_PROTOCOLS = Set.of(
            "devices", "picture", "privacy", "blocklist", "notice"
    );

    /**
     * The fallback routing domain applied when the stanza omits {@code dns_domain} and no domain is already stored.
     */
    private static final String DEFAULT_ROUTING_DOMAIN = "fb";

    /**
     * The lower bound in seconds applied to an accepted client expiration override.
     *
     * @implNote This value is three days expressed in seconds ({@code 3 * 86400}); the server cannot push an
     * expiration that fires sooner than this many seconds from now.
     */
    private static final long CLIENT_EXPIRATION_MIN_FLOOR_SECONDS = 3L * 86_400L;

    /**
     * The nominal encrypted wire size, in bytes, attributed to each delivered offline stanza when synthesizing the
     * offline-resume {@code offlineSizeBytes} figure.
     *
     * @implNote This implementation multiplies the delivered envelope count by this constant and rounds to the nearest
     * kilobyte because Cobalt does not accumulate the true per-stanza byte lengths during offline delivery; the value
     * approximates a typical Signal-encrypted message envelope.
     */
    private static final long OFFLINE_AVERAGE_STANZA_BYTES = 256L;

    /**
     * The {@link LinkedWhatsAppClient} used for store access, outbound stanza dispatch and delegated service calls.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * The {@link WebAppStateService} used to retry orphan app-state mutations whenever a bulletin signals that
     * previously missing referents may now exist.
     */
    private final WebAppStateService webAppStateService;

    /**
     * The shared {@link OfflineNotificationsReporter} that accumulates per-collection offline {@code server_sync}
     * notification counts and is flushed as a WAM event when the offline bulletin arrives.
     */
    private final OfflineNotificationsReporter offlineNotificationsReporter;

    /**
     * The {@link WamService} used to commit the dirty-bits false-positive event after an app-state pull resolves.
     */
    private final WamService wamService;

    /**
     * The {@link DeviceService} used to drive the pending-device-sync retry that closes the offline-resume state
     * machine when the {@code offline} bulletin arrives.
     */
    private final DeviceService deviceService;

    /**
     * The epoch-millis timestamp of the {@code offline_preview} bulletin that drove the current
     * {@link LinkedWhatsAppClientOfflineResumeState#RESUME_ON_RESTART} transition, or {@code 0L} when no preview has been
     * observed since the last completion.
     *
     * <p>Gates repeated previews against the {@link LinkedWhatsAppClientOfflineResumeState#OFFLINE_PREVIEW_PERIOD_MS}
     * debounce window: previews inside the window are accepted as cumulative updates, previews outside the window are
     * rejected as noise.
     */
    private volatile long firstOfflinePreviewMillis;

    /**
     * The {@code count} (envelope total) attribute carried by the most recent {@code <offline_preview/>} bulletin, or
     * {@code 0} when no preview has been observed since the last offline-resume completion.
     *
     * <p>Captured so the {@code OfflineResume} summary committed by {@link #emitOfflineResumeTelemetry(int)} can report
     * the pre-delivery backlog size that WA Web's offline-resume reporter records from the same preview.
     */
    private volatile int lastOfflinePreviewEnvelopeCount;

    /**
     * The {@code message} count carried by the most recent {@code <offline_preview/>} bulletin, or {@code 0} when no
     * preview has been observed since the last offline-resume completion.
     */
    private volatile int lastOfflinePreviewMessageCount;

    /**
     * The {@code receipt} count carried by the most recent {@code <offline_preview/>} bulletin, or {@code 0} when no
     * preview has been observed since the last offline-resume completion.
     */
    private volatile int lastOfflinePreviewReceiptCount;

    /**
     * The {@code notification} count carried by the most recent {@code <offline_preview/>} bulletin, or {@code 0} when
     * no preview has been observed since the last offline-resume completion.
     */
    private volatile int lastOfflinePreviewNotificationCount;

    /**
     * The {@code call} count carried by the most recent {@code <offline_preview/>} bulletin, or {@code 0} when no
     * preview has been observed since the last offline-resume completion.
     */
    private volatile int lastOfflinePreviewCallCount;

    /**
     * Constructs a new info bulletin handler bound to the given client, web app-state service, shared reporter, WAM
     * service and device service.
     *
     * @param whatsapp                     the {@link LinkedWhatsAppClient}; must not be {@code null}
     * @param webAppStateService           the {@link WebAppStateService} used for orphan-mutation retries; must not be
     *                                     {@code null}
     * @param offlineNotificationsReporter the shared reporter flushed when the offline bulletin arrives; must not be
     *                                     {@code null}
     * @param wamService                   the {@link WamService} used to commit the dirty-bits event; must not be
     *                                     {@code null}
     * @param deviceService                the {@link DeviceService} used to run the post-resume pending device sync;
     *                                     must not be {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleInfoBulletin", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public InfoBulletinStreamHandler(LinkedWhatsAppClient whatsapp, WebAppStateService webAppStateService, OfflineNotificationsReporter offlineNotificationsReporter, WamService wamService, DeviceService deviceService) {
        this.whatsapp = whatsapp;
        this.webAppStateService = webAppStateService;
        this.offlineNotificationsReporter = offlineNotificationsReporter;
        this.wamService = wamService;
        this.deviceService = deviceService;
        this.firstOfflinePreviewMillis = 0L;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Dispatches the {@code <ib>} stanza to the first recognised info-type branch in priority order: {@code dirty},
     * {@code edge_routing}, {@code offline}, {@code priority_offline_complete}, {@code offline_preview}, {@code tos},
     * {@code thread_metadata}, then {@code client_expiration}. A stanza whose children contain no recognised info type
     * is logged as a warning.
     *
     * @implNote This implementation wraps the dispatch in a {@link Throwable} catch so a malformed bulletin cannot
     * poison the socket reader.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebHandleInfoBulletin", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void handle(Stanza stanza) {
        try {
            if (stanza.hasChild(INFO_TYPE_DIRTY)) {
                handleDirty(stanza);
                return;
            }

            var routing = stanza.getChild(INFO_TYPE_ROUTING);
            if (routing.isPresent()) {
                handleRouting(routing.get());
                return;
            }

            var offline = stanza.getChild(INFO_TYPE_OFFLINE);
            if (offline.isPresent()) {
                handleOffline(offline.get());
                return;
            }

            if (stanza.hasChild(INFO_TYPE_OFFLINE_PRIORITY_COMPLETE)) {
                handleOfflinePriorityComplete();
                return;
            }

            var preview = stanza.getChild(INFO_TYPE_OFFLINE_PREVIEW);
            if (preview.isPresent()) {
                handleOfflinePreview(preview.get());
                return;
            }

            var tos = stanza.getChild(INFO_TYPE_TOS);
            if (tos.isPresent()) {
                handleTos(tos.get());
                return;
            }

            var threadMeta = stanza.getChild(INFO_TYPE_THREAD_META);
            if (threadMeta.isPresent()) {
                handleThreadMeta(threadMeta.get());
                return;
            }

            if (stanza.hasChild(INFO_TYPE_CLIENT_EXPIRATION)) {
                handleClientExpiration(stanza);
                return;
            }

            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "unrecognized info bulletin id={0}",
                        stanza.getAttributeAsString("id", "[missing-id]"));
            }
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "failed to handle info bulletin id=" + stanza.getAttributeAsString("id", "[missing-id]"),
                        throwable);
            }
        }
    }

    /**
     * Processes every {@code <dirty/>} child of the {@code <ib>} stanza and acknowledges the batch back to the server.
     *
     * <p>For each dirty entry the {@code type} attribute selects the action: {@code syncd_app_state} flags every
     * collection for the next app-state pull, {@code account_sync} iterates the supported protocol children and flags
     * the corresponding store sync booleans, and {@code groups} and {@code newsletter_metadata} are logged for the
     * deferred metadata refresh path. Entries whose {@code type} is unsupported are still included in the ack batch
     * sent to the server.
     *
     * @implNote This implementation aggregates the {@code syncd_app_state} collections into a single
     * {@link LinkedWhatsAppClient#pullWebAppState(SyncPatchType...)} call rather than firing one mark-for-sync per entry, and
     * commits the dirty-bits WAM event inline based on the pull's return value rather than via a sync-completed
     * subscription. Account-sync subsystem refreshes are deferred to the next caller through the
     * {@code setSyncedXxx(false)} flags rather than issued imperatively.
     *
     * @param stanza the parent {@code <ib>} stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleDirtyBits", exports = "handleDirtyBits",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleDirty(Stanza stanza) {
        var collectionsToSync = new LinkedHashSet<SyncPatchType>();
        var allDirtyEntries = new ArrayList<Stanza>();
        var supportedTypes = new ArrayList<String>();
        var unsupportedTypes = new ArrayList<String>();
        var syncOwnDevices = false;

        for (var dirtyNode : stanza.getChildren(INFO_TYPE_DIRTY)) {
            allDirtyEntries.add(dirtyNode);
            var type = dirtyNode.getAttributeAsString("type", null);

            if (DIRTY_TYPE_ACCOUNT_SYNC.equals(type)) {
                supportedTypes.add(type);
                for (var child : dirtyNode.children()) {
                    var protocol = child.description();
                    if (!SUPPORTED_DIRTY_PROTOCOLS.contains(protocol)) {
                        continue;
                    }
                    switch (protocol) {
                        case "devices" -> {
                            syncOwnDevices = true;
                            if (Log.DEBUG) {
                                LOGGER.log(Level.DEBUG,
                                        "dirty bit account_sync/devices: syncing own device list");
                            }
                        }
                        case "picture" -> {
                            if (Log.DEBUG) {
                                LOGGER.log(Level.DEBUG,
                                        "dirty bit account_sync/picture: profile picture refresh needed");
                            }
                        }
                        case "privacy" -> {
                            whatsapp.store().syncStore().setSyncedContacts(false);
                            if (Log.DEBUG) {
                                LOGGER.log(Level.DEBUG,
                                        "dirty bit account_sync/privacy: privacy settings refresh needed");
                            }
                        }
                        case "blocklist" -> {
                            whatsapp.store().syncStore().setSyncedContacts(false);
                            if (Log.DEBUG) {
                                LOGGER.log(Level.DEBUG,
                                        "dirty bit account_sync/blocklist: block list refresh needed");
                            }
                        }
                        case "notice" -> {
                            if (Log.DEBUG) {
                                LOGGER.log(Level.DEBUG,
                                        "dirty bit account_sync/notice: notice refresh needed");
                            }
                        }
                        default -> {
                        }
                    }
                }
                whatsapp.store().syncStore().setSyncedStatus(false);
            } else if (DIRTY_TYPE_SYNCD_APP_STATE.equals(type)) {
                supportedTypes.add(type);
                Collections.addAll(collectionsToSync, SyncPatchType.values());
            } else if (DIRTY_TYPE_GROUPS.equals(type)) {
                supportedTypes.add(type);
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "dirty bit groups: group metadata refresh needed");
                }
            } else if (DIRTY_TYPE_NEWSLETTER_METADATA.equals(type)) {
                supportedTypes.add(type);
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "dirty bit newsletter_metadata: newsletter metadata refresh needed");
                }
            } else {
                unsupportedTypes.add(type == null ? "" : type);
            }
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "handleDirtyBits supported={0} unsupported={1}",
                    String.join(",", supportedTypes),
                    String.join(",", unsupportedTypes));
        }

        if (!collectionsToSync.isEmpty()) {
            var hasAppStateChanges = whatsapp.pullWebAppState(collectionsToSync.toArray(SyncPatchType[]::new));
            wamService.commit(new MdAppStateDirtyBitsEventBuilder()
                    .dirtyBitsFalsePositive(!hasAppStateChanges)
                    .build());
        }

        if (syncOwnDevices) {
            deviceService.syncMyDeviceList();
        }

        clearDirtyBits(allDirtyEntries);
        webAppStateService.retryAllOrphanMutations();
    }

    /**
     * Sends a {@code <iq type="set" xmlns="urn:xmpp:whatsapp:dirty">} containing one {@code <clean/>} child per
     * processed dirty entry, acknowledging the bits back to the server so they can be cleared.
     *
     * <p>The IQ preserves each entry's original {@code type} and {@code timestamp} attributes so the server can match
     * the ack to the dirty record. An empty batch skips the IQ.
     *
     * @implNote This implementation swallows transport failures at {@code WARNING} because the server retransmits the
     * same dirty bits on the next connection; reraising would tear down the in-progress dispatch on the {@code <ib>}
     * stanza for no recoverable benefit.
     *
     * @param dirtyEntries the dirty entries to acknowledge; must not be {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebClearDirtyBitsJob", exports = "clearDirtyBits",
            adaptation = WhatsAppAdaptation.DIRECT)
    private void clearDirtyBits(List<Stanza> dirtyEntries) {
        if (dirtyEntries.isEmpty()) {
            return;
        }

        var cleanChildren = dirtyEntries.stream()
                .map(dirty -> new StanzaBuilder()
                        .description("clean")
                        .attribute("type", dirty.getAttributeAsString("type", null))
                        .attribute("timestamp", dirty.getAttributeAsString("timestamp", null))
                        .build())
                .toList();

        try {
            whatsapp.sendNode(new StanzaBuilder()
                    .description("iq")
                    .attribute("to", Jid.userServer())
                    .attribute("type", "set")
                    .attribute("xmlns", "urn:xmpp:whatsapp:dirty")
                    .content(cleanChildren));
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "clearDirtyBits acked types={0}",
                        dirtyEntries.stream()
                                .map(d -> d.getAttributeAsString("type", "unknown"))
                                .reduce((a, b) -> a + "," + b)
                                .orElse(""));
            }
        } catch (Exception e) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "clearDirtyBits failed", e);
            }
        }
    }

    /**
     * Persists the {@code edge_routing} routing-info payload and DNS domain in the store so the next reconnect uses
     * them.
     *
     * <p>The {@code <edge_routing/>} child carries a mandatory {@code <routing_info>} byte payload (the steering blob
     * the client sends in the noise handshake) and an optional {@code <dns_domain>} enum selecting between the
     * {@code fb} and {@code sl} domains. When {@code dns_domain} is absent or unknown, the existing stored domain is
     * reused, falling back to {@link #DEFAULT_ROUTING_DOMAIN} when neither is set.
     *
     * @implNote This implementation rejects domain values outside {@code {"fb", "sl"}} inline; the domain decoder
     * cannot produce other values.
     *
     * @param routingStanza the {@code <edge_routing/>} child stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleRoutingInfo", exports = "handleRoutingInfo",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleRouting(Stanza routingStanza) {
        var edgeRouting = routingStanza.getChild("routing_info")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        var domain = routingStanza.getChild("dns_domain")
                .flatMap(Stanza::toContentString)
                .orElse(null);
        if (domain != null && !"fb".equals(domain) && !"sl".equals(domain)) {
            domain = null;
        }
        if (domain == null) {
            domain = whatsapp.store().connectionStore().routingDomain().orElse(DEFAULT_ROUTING_DOMAIN);
        }
        whatsapp.store().connectionStore().setRoutingInfo(edgeRouting);
        whatsapp.store().connectionStore().setRoutingDomain(domain);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "edge routing updated domain={0} edgeRoutingBytes={1}",
                    domain, edgeRouting == null ? 0 : edgeRouting.length);
        }
    }

    /**
     * Closes the offline-resume state machine when the server announces via the {@code <offline/>} child that the
     * queued backlog has finished delivering.
     *
     * <p>The {@code count} attribute is the total number of offline messages the server has delivered. The transition
     * follows three branches:
     * <ul>
     *   <li>If the state is already {@link LinkedWhatsAppClientOfflineResumeState#COMPLETE}, the bulletin is acknowledged for
     *       telemetry but no further work runs.</li>
     *   <li>If the state is {@link LinkedWhatsAppClientOfflineResumeState#RESUME_WITH_OPEN_TAB}, the live-tab disconnect path
     *       runs the pending device sync inline and then transitions to
     *       {@link LinkedWhatsAppClientOfflineResumeState#COMPLETE}.</li>
     *   <li>Otherwise the post-restart path transitions to {@link LinkedWhatsAppClientOfflineResumeState#COMPLETE} immediately
     *       and schedules the pending device sync after
     *       {@link LinkedWhatsAppClientOfflineResumeState#OFFLINE_DEVICE_SYNC_DELAY}.</li>
     * </ul>
     *
     * @implNote This implementation always flushes the accumulated offline {@code server_sync} notification counts
     * through {@link OfflineNotificationsReporter#report()} and, when {@code count == 0}, drives a best-effort
     * {@link WebAppStateService#retryAllOrphanMutations()} retry to pick up app-state changes that landed just before
     * connect. The completing transition additionally emits the offline-resume telemetry pair through
     * {@link #emitOfflineResumeTelemetry(int)}. WA Web's UI bookkeeping has no Cobalt analogue. The scheduled device
     * sync runs on a fresh virtual thread; an {@link InterruptedException} during the delay sets the interrupt flag and
     * returns quietly.
     *
     * @param offlineStanza the {@code <offline/>} child stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleInfoBulletin", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebOfflineHandler",
            exports = "processOfflineIb",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebOfflineHandler",
            exports = "OfflineMessageHandlerImpl",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleOffline(Stanza offlineStanza) {
        var count = offlineStanza.getAttributeAsInt("count", 0);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "received offline bulletin count={0}", count);
        }
        offlineNotificationsReporter.report();
        if (count == 0) {
            webAppStateService.retryAllOrphanMutations();
        }

        var store = whatsapp.store();
        var current = store.connectionStore().offlineResumeState();
        if (current == LinkedWhatsAppClientOfflineResumeState.COMPLETE) {
            return;
        }

        emitOfflineResumeTelemetry(count);

        if (current == LinkedWhatsAppClientOfflineResumeState.RESUME_WITH_OPEN_TAB) {
            try {
                deviceService.retryPendingSyncs();
            } catch (Throwable throwable) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "pending device sync failed during open-tab resume completion", throwable);
                }
            }
            store.connectionStore().setOfflineResumeState(LinkedWhatsAppClientOfflineResumeState.COMPLETE);
            firstOfflinePreviewMillis = 0L;
            return;
        }

        store.connectionStore().setOfflineResumeState(LinkedWhatsAppClientOfflineResumeState.COMPLETE);
        firstOfflinePreviewMillis = 0L;
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(LinkedWhatsAppClientOfflineResumeState.OFFLINE_DEVICE_SYNC_DELAY);
                deviceService.retryPendingSyncs();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable throwable) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "pending device sync failed after offline resume completion", throwable);
                }
            }
        });
    }

    /**
     * Opens or advances the offline-resume state machine in response to the {@code <offline_preview/>} pre-delivery
     * snapshot of the pending backlog.
     *
     * <p>The transition follows three branches:
     * <ul>
     *   <li>If the resume-from-restart phase is already complete (state is past
     *       {@link LinkedWhatsAppClientOfflineResumeState#RESUME_ON_RESTART}), a live socket disconnect is in progress; the
     *       state moves to {@link LinkedWhatsAppClientOfflineResumeState#RESUME_WITH_OPEN_TAB}.</li>
     *   <li>If the current state is {@link LinkedWhatsAppClientOfflineResumeState#INIT}, this is the first preview after a
     *       cold start; the state moves to {@link LinkedWhatsAppClientOfflineResumeState#RESUME_ON_RESTART} and
     *       {@link #firstOfflinePreviewMillis} is set for the debounce window.</li>
     *   <li>Otherwise the state is already {@link LinkedWhatsAppClientOfflineResumeState#RESUME_ON_RESTART} and repeated
     *       previews are gated by {@link LinkedWhatsAppClientOfflineResumeState#OFFLINE_PREVIEW_PERIOD_MS}: previews inside
     *       the window are accepted as cumulative updates, previews outside the window are rejected and logged.</li>
     * </ul>
     *
     * @implNote This implementation skips WA Web's chat-sort listener throttle and the open-tab-limit refresh path
     * because both are UI-only side effects with no Cobalt analogue on the headless client.
     *
     * @param previewStanza the {@code <offline_preview/>} child stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleInfoBulletin", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebOfflineHandler",
            exports = "processOfflinePreviewIb",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebOfflineHandler",
            exports = "OfflineMessageHandlerImpl",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleOfflinePreview(Stanza previewStanza) {
        var envelopeCount = previewStanza.getAttributeAsInt("count", 0);
        var messageCount = previewStanza.getAttributeAsInt("message", 0);
        var receiptCount = previewStanza.getAttributeAsInt("receipt", 0);
        var notificationCount = previewStanza.getAttributeAsInt("notification", 0);
        var callCount = previewStanza.getAttributeAsInt("call", 0);
        lastOfflinePreviewEnvelopeCount = envelopeCount;
        lastOfflinePreviewMessageCount = messageCount;
        lastOfflinePreviewReceiptCount = receiptCount;
        lastOfflinePreviewNotificationCount = notificationCount;
        lastOfflinePreviewCallCount = callCount;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "received offline preview bulletin count={0} message={1} receipt={2} notification={3} call={4}",
                    envelopeCount,
                    messageCount,
                    receiptCount,
                    notificationCount,
                    callCount);
        }

        var store = whatsapp.store();
        if (store.connectionStore().isResumeFromRestartComplete()) {
            store.connectionStore().setOfflineResumeState(LinkedWhatsAppClientOfflineResumeState.RESUME_WITH_OPEN_TAB);
            return;
        }

        var current = store.connectionStore().offlineResumeState();
        if (current == LinkedWhatsAppClientOfflineResumeState.INIT) {
            firstOfflinePreviewMillis = System.currentTimeMillis();
            store.connectionStore().setOfflineResumeState(LinkedWhatsAppClientOfflineResumeState.RESUME_ON_RESTART);
            return;
        }

        var firstMillis = firstOfflinePreviewMillis;
        if (firstMillis == 0L) {
            return;
        }
        var delay = System.currentTimeMillis() - firstMillis;
        if (delay < LinkedWhatsAppClientOfflineResumeState.OFFLINE_PREVIEW_PERIOD_MS) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "accepted repeated offline preview ib delay={0} message={1}",
                        delay, messageCount);
            }
        } else if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "rejected repeated offline preview ib delay={0}",
                    delay);
        }
    }

    /**
     * Handles the {@code <priority_offline_complete/>} bulletin that announces every high-priority offline stanza has
     * been delivered.
     *
     * <p>Drives a best-effort {@link WebAppStateService#retryAllOrphanMutations()} retry because peer dependencies for
     * queued orphans may now be satisfied.
     *
     * @implNote WA Web only logs on this branch because its resume manager does not track priority completion; Cobalt
     * additionally retries orphan mutations.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleInfoBulletin", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleOfflinePriorityComplete() {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "received priority_offline_complete bulletin");
        }
        webAppStateService.retryAllOrphanMutations();
    }

    /**
     * Stores the set of pending Terms-of-Service notice IDs carried by the {@code <tos/>} child of the bulletin.
     *
     * <p>The raw notice IDs are recorded on the store so embedder code can render or acknowledge each notice at the
     * appropriate time.
     *
     * @implNote This implementation does not run the dirty-bit-driven consent-collection pipeline that WA Web fires
     * from the {@code account_sync/notice} branch; the IDs are pure metadata and the embedder owns the consent flow.
     *
     * @param tosStanza the {@code <tos/>} child stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleInfoBulletin", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleTos(Stanza tosStanza) {
        var notices = tosStanza.getChildren("notice").stream()
                .map(entry -> entry.getAttributeAsString("id", null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        whatsapp.store().settingsStore().setAcknowledgedTosNotices(notices);
        var snapshot = Set.copyOf(notices);
        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof LinkedTosNoticesChangedListener typed) {
                Thread.startVirtualThread(() -> typed.onTosNoticesChanged(whatsapp, snapshot));
            }
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "received tos bulletin noticeCount={0}", notices.size());
        }
    }

    /**
     * Parses the {@code <thread_metadata/>} bulletin and logs each per-thread offline timestamp without persisting it.
     *
     * <p>The per-thread last-seen-offline timestamps drive the unread-divider position in WA Web's chat view. Cobalt
     * has no equivalent UI state and so the payload is parsed for validation only; each {@code <item/>} carries a
     * {@code from} JID plus a {@code t} timestamp.
     *
     * @param threadMetaStanza the {@code <thread_metadata/>} child stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleInfoBulletin", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleThreadMeta(Stanza threadMetaStanza) {
        var itemCount = 0;
        for (var item : threadMetaStanza.getChildren("item")) {
            var from = item.getAttributeAsJid("from").orElse(null);
            var timestamp = item.getAttributeAsLong("t", (Long) null);
            if (from == null || timestamp == null) {
                continue;
            }
            itemCount++;
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE,
                        "thread_metadata item chat={0} timestamp={1}",
                        from, timestamp);
            }
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "received thread_metadata bulletin itemCount={0}", itemCount);
        }
    }

    /**
     * Applies or clears the server-mandated client expiration override carried by the {@code <client_expiration/>}
     * bulletin.
     *
     * <p>The {@code <ib>} envelope is parsed via the typed {@link SmaxClientExpirationResponse} parser. When the parsed
     * {@code t} attribute is absent the stored override is cleared. Otherwise the new timestamp is normalised through
     * {@link #castToUnixTime(long)} and compared to any existing override: if the new value is not earlier than the
     * current override the update is ignored, because the server never extends the expiration window. Accepted values
     * are floored to at least {@link #CLIENT_EXPIRATION_MIN_FLOOR_SECONDS} in the future. Parse failures are logged at
     * {@code WARNING}.
     *
     * @implNote This implementation has no hard-expire build constant, so the upper-bound clamp is skipped; the final
     * value is {@code max(minFloor, newExpiration)}.
     *
     * @param stanza the {@code <ib>} envelope; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxClientExpirationClientExpirationRPC",
            exports = "receiveClientExpirationRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebHandleServerClientExpiration",
            exports = "handleServerClientExpiration", adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleClientExpiration(Stanza stanza) {
        var parsed = SmaxClientExpirationResponse.of(stanza).orElse(null);
        if (!(parsed instanceof SmaxClientExpirationResponse.Inbound inbound)) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "failed to parse client_expiration bulletin id={0}",
                        stanza.getAttributeAsString("id", "[missing-id]"));
            }
            return;
        }

        var newExpiration = inbound.clientExpirationT()
                .map(InfoBulletinStreamHandler::castToUnixTime)
                .orElse(null);
        if (newExpiration == null) {
            whatsapp.store().accountStore().setClientExpiration(null);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "cleared client expiration override");
            }
            return;
        }

        var existingExpiration = whatsapp.store().accountStore().clientExpiration().orElse(null);

        if (existingExpiration != null && newExpiration >= existingExpiration.getEpochSecond()) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "ignoring client expiration {0}, not earlier than existing {1}",
                        newExpiration, existingExpiration);
            }
            return;
        }

        var minFloor = Instant.now().plusSeconds(CLIENT_EXPIRATION_MIN_FLOOR_SECONDS);

        var clampedExpiration = newExpiration < minFloor.getEpochSecond()
                ? minFloor
                : Instant.ofEpochSecond(newExpiration);

        whatsapp.store().accountStore().setClientExpiration(clampedExpiration);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "applied client expiration bulletin, clamped to {0}", clampedExpiration);
        }
    }

    /**
     * Commits the {@code OfflineResume} summary event and the staged {@code WebcOfflineNotificationProcess} completion
     * event when the {@code <offline/>} bulletin closes an offline-resume cycle.
     *
     * <p>The message, receipt, notification and call counts are taken from the counts captured on the last
     * {@code <offline_preview/>} bulletin (fields such as {@link #lastOfflinePreviewMessageCount}), falling back to the
     * {@code <offline/>} {@code count} attribute for the message and envelope totals. The chat thread count is read live
     * from {@link LinkedWhatsAppClient#store()} and rounded to the nearest ten; the processing duration is the wall-clock
     * interval since {@link #firstOfflinePreviewMillis}. The whole cycle is skipped when neither the bulletin nor any
     * preview reported an offline envelope, mirroring WA Web's "no envelopes" short-circuit. The captured preview counts
     * are cleared on return so a subsequent cycle starts clean.
     *
     * @implNote This implementation fabricates the browser page-lifecycle timers ({@code pageLoadT},
     * {@code socketConnectT}, {@code passiveModeT}, {@code offlinePreviewT}, {@code mainScreenLoadT}) as a coherent
     * monotonic sequence anchored at a notional navigation start because a headless client has no navigation timeline;
     * {@code offlineProcessingT} and {@code lastStanzaT} fold in the real drain duration. The offline wire size is
     * synthesized from the envelope count at {@value #OFFLINE_AVERAGE_STANZA_BYTES} bytes per stanza. The decrypt-error,
     * pre-ack and mailbox-age figures are reported as zero, matching WA Web's initialised defaults for a clean resume.
     *
     * @param count the {@code count} attribute of the {@code <offline/>} bulletin (total delivered envelopes)
     */
    @WhatsAppWebExport(moduleName = "WAWebWamOfflineResumeReporter", exports = "commit",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamWorkerOfflineProcessReporter", exports = "logProcessComplete",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitOfflineResumeTelemetry(int count) {
        var messageCount = Math.max(lastOfflinePreviewMessageCount, count);
        var receiptCount = lastOfflinePreviewReceiptCount;
        var notificationCount = lastOfflinePreviewNotificationCount;
        var callCount = lastOfflinePreviewCallCount;
        var envelopeCount = Math.max(lastOfflinePreviewEnvelopeCount, count);
        try {
            if (envelopeCount == 0 && messageCount == 0 && receiptCount == 0 && notificationCount == 0 && callCount == 0) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "skipping offline resume telemetry, no envelopes observed");
                }
                return;
            }

            var previewMillis = firstOfflinePreviewMillis;
            var processingMillis = previewMillis > 0L
                    ? Math.max(0L, System.currentTimeMillis() - previewMillis)
                    : Math.max(200L, (long) envelopeCount * 5L);

            var pageLoadMillis = 1_450L;
            var socketConnectMillis = 1_650L;
            var passiveModeMillis = 1_700L;
            var offlinePreviewMillis = 1_900L;
            var mainScreenLoadMillis = 2_150L;
            var lastStanzaMillis = offlinePreviewMillis + processingMillis;

            var chatThreadCount = roundToNearest(whatsapp.store().chatStore().chats().size(), 10L);
            var offlineSizeBytes = roundToNearest((long) envelopeCount * OFFLINE_AVERAGE_STANZA_BYTES, 1_000L);

            wamService.commit(new OfflineResumeEventBuilder()
                    .offlineMessageCount(messageCount)
                    .offlineReceiptCount(receiptCount)
                    .offlineNotificationCount(notificationCount)
                    .offlineCallCount(callCount)
                    .offlineDecryptErrorCount(0L)
                    .offlineSizeBytes(offlineSizeBytes)
                    .chatThreadCount(chatThreadCount)
                    .preackMessageCount(0L)
                    .preackReceiptCount(0L)
                    .processedMessageCount(messageCount)
                    .processedReceiptCount(receiptCount)
                    .processedNotificationCount(notificationCount)
                    .processedCallCount(callCount)
                    .mailboxAge(0L)
                    .isOfflineCompleteMissed(Boolean.FALSE)
                    .isResumeStartedInForeground(Boolean.TRUE)
                    .isResumeInForeground(Boolean.TRUE)
                    .offlineResumeResult(OfflineResumeResultType.COMPLETE)
                    .pageLoadT(Instant.ofEpochMilli(pageLoadMillis))
                    .socketConnectT(Instant.ofEpochMilli(socketConnectMillis))
                    .passiveModeT(Instant.ofEpochMilli(passiveModeMillis))
                    .offlinePreviewT(Instant.ofEpochMilli(offlinePreviewMillis))
                    .mainScreenLoadT(Instant.ofEpochMilli(mainScreenLoadMillis))
                    .lastStanzaT(Instant.ofEpochMilli(lastStanzaMillis))
                    .offlineProcessingT(Instant.ofEpochMilli(processingMillis))
                    .build());

            wamService.commit(new WebcOfflineNotificationProcessEventBuilder()
                    .currentOfflineProcessStage(OfflineProcessStages.PROCESS_COMPLETE)
                    .runReason(OfflineProcessRunReasons.PUSH_NOTIFICATION)
                    .offlineProcessSessionId(nextOfflineProcessSessionId())
                    .offlineProcessStageTimestampMs(processingMillis)
                    .offlineProcessMessageCount(roundToNearest(messageCount, 10L))
                    .offlineProcessNotificationCount(roundToNearest(notificationCount, 10L))
                    .offlineProcessDecryptErrorCount(0L)
                    .offlineProcessMailboxAge(0L)
                    .swVersion(offlineProcessSwVersion())
                    .build());

            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "emitted offline resume telemetry envelopeCount={0} messageCount={1} processingMillis={2}",
                        envelopeCount, messageCount, processingMillis);
            }
        } finally {
            lastOfflinePreviewEnvelopeCount = 0;
            lastOfflinePreviewMessageCount = 0;
            lastOfflinePreviewReceiptCount = 0;
            lastOfflinePreviewNotificationCount = 0;
            lastOfflinePreviewCallCount = 0;
        }
    }

    /**
     * Generates a fresh offline-process session identifier of four random hexadecimal digits followed by the current
     * Unix time in seconds.
     *
     * @implNote This implementation mirrors WA Web's {@code randomHex(4) + unixTime} construction; the
     * {@code 0x1_0000 | random} then {@code substring(1)} idiom left-pads the random component to exactly four hex
     * digits.
     *
     * @return the generated session identifier
     */
    @WhatsAppWebExport(moduleName = "WAWebWamWorkerOfflineProcessReporter", exports = "offlineProcessSessionId",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String nextOfflineProcessSessionId() {
        var random = ThreadLocalRandom.current().nextInt(0x1_0000);
        return Integer.toHexString(0x1_0000 | random).substring(1) + Instant.now().getEpochSecond();
    }

    /**
     * Returns the client application version string reported as the offline-process software version, or {@code null}
     * when the version has not been resolved.
     *
     * @implNote This implementation reads the lazily-derived client version off the account store, standing in for WA
     * Web's {@code WAWebBuildConstants.VERSION_BASE} build constant.
     *
     * @return the version string, or {@code null} if unresolved
     */
    @WhatsAppWebExport(moduleName = "WAWebBuildConstants", exports = "VERSION_BASE",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private String offlineProcessSwVersion() {
        var version = whatsapp.store().accountStore().clientVersion();
        return version == null ? null : version.toString();
    }

    /**
     * Rounds a non-negative value to the nearest multiple of the given granularity.
     *
     * @implNote This implementation reproduces WA Web's {@code Math.round(value / granularity) * granularity} coarsening
     * applied to offline counts and byte totals before they are reported.
     *
     * @param value       the value to round
     * @param granularity the rounding step; must be positive
     * @return the value rounded to the nearest multiple of {@code granularity}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamOfflineResumeReporter", exports = "roundUp",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static long roundToNearest(long value, long granularity) {
        return Math.round((double) value / granularity) * granularity;
    }

    /**
     * Clamps a Unix-second timestamp to the signed 32-bit integer range.
     *
     * <p>Downstream paths multiply the timestamp through {@code Date}, so the explicit clamp prevents silent overflow.
     *
     * @implNote This implementation expresses the int32 truncation as {@code (long) (int) value}; the surrounding
     * clamp to the {@code [-(2^31 - 1), 2^31 - 1]} range is then a no-op within int32 range and is preserved for
     * documentation parity with WA Web's helper.
     *
     * @param value the raw timestamp in seconds
     * @return the clamped timestamp in seconds
     */
    @WhatsAppWebExport(moduleName = "WATimeUtils", exports = "castToUnixTime",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static long castToUnixTime(long value) {
        return Math.max(-2_147_483_647L, Math.min((long) (int) value, 2_147_483_647L));
    }
}

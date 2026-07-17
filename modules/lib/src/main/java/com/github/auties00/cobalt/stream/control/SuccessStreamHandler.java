package com.github.auties00.cobalt.stream.control;

import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebGraphQlException;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.linked.LinkedChatsListener;
import com.github.auties00.cobalt.listener.linked.LinkedContactsListener;
import com.github.auties00.cobalt.listener.LoggedInListener;
import com.github.auties00.cobalt.listener.linked.LinkedNameChangedListener;
import com.github.auties00.cobalt.listener.linked.LinkedNewslettersListener;
import com.github.auties00.cobalt.listener.linked.LinkedStatusListener;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.exception.WhatsAppException;
import com.github.auties00.cobalt.exception.linked.WhatsAppFacebookGraphQlException;
import com.github.auties00.cobalt.exception.linked.WhatsAppServerRuntimeException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.media.MediaConnectionService;
import com.github.auties00.cobalt.wire.linked.device.pairing.LinkedPrimaryPlatform;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.migration.InactiveGroupLidMigrationService;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessProfile;
import com.github.auties00.cobalt.wire.linked.contact.ContactStatus;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.wire.linked.tos.TosNotice;
import com.github.auties00.cobalt.wire.stanza.iq.media.IqQueryMediaConnsRequest;
import com.github.auties00.cobalt.wire.stanza.iq.media.IqQueryMediaConnsResponse;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.tos.TosService;
import com.github.auties00.cobalt.stream.NodeStreamService;
import com.github.auties00.cobalt.sync.WebAppStateService;
import com.github.auties00.cobalt.util.ScheduledTask;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.ClockSkewDifferenceTEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.LoginEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcRawPlatformsEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ConnectionOriginType;
import com.github.auties00.cobalt.wire.wam.type.ConnectionSequenceStepType;
import com.github.auties00.cobalt.wire.wam.type.DnsResolutionMethodType;
import com.github.auties00.cobalt.wire.wam.type.LoginDnsResolverType;
import com.github.auties00.cobalt.wire.wam.type.LoginHostType;
import com.github.auties00.cobalt.wire.wam.type.LoginPortNumber;
import com.github.auties00.cobalt.wire.wam.type.LoginResultType;
import com.github.auties00.cobalt.wire.wam.type.StreamSocketProviderType;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Handles the {@code <success>} stanza pushed by the WhatsApp server immediately after a successful authentication
 * handshake and drives the full client bootstrap that follows.
 *
 * <p>The handler is registered under the {@code "success"} tag inside {@link NodeStreamService}. One {@code <success>}
 * stanza per session drives a one-shot bootstrap: update the {@code me} identity (LID, display name, phone number) from
 * the parsed attributes, sync A/B props, enable or disable LID migration, start the WAM service, schedule the ADV
 * device check, resume web app-state syncing, send the {@code <iq xmlns="passive"><active/></iq>} stanza that
 * transitions the server out of passive mode, run the launch-time compliance probes and notify
 * {@link LoggedInListener#onLoggedIn(LinkedWhatsAppClient)}. A one-shot {@link AtomicBoolean} guard ensures the
 * bootstrap runs at most once per connection; {@link #reset()} clears it on socket teardown so the next reconnect
 * repeats the bootstrap.
 *
 * @implNote This implementation flattens WA Web's split between the success handler, the registered passive-task
 * pipeline and the IndexedDB-key derivation step into a single sequential virtual-thread call; WA Web's UI-only passive
 * tasks (collection action handlers, temporary-ban banner reset, offline push toggle, at-rest encryption key
 * derivation) are skipped because Cobalt is headless and persists the store without at-rest encryption.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleSuccess")
public final class SuccessStreamHandler extends SocketStreamHandler.Concurrent {
    /**
     * The {@link LinkedWhatsAppClient} used for store access, listener notification and outbound IQ sending.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * The {@link ABPropsService} used to sync A/B testing properties from the server during bootstrap.
     */
    private final ABPropsService abPropsService;

    /**
     * The {@link DeviceService} used to schedule the ADV check, retry pending device syncs and refresh missing-key
     * device tracking.
     */
    private final DeviceService deviceService;

    /**
     * The {@link LidMigrationService} consulted during bootstrap to flip LID-based one-on-one chat migration on or off
     * based on the synced AB-prop.
     */
    private final LidMigrationService lidMigrationService;

    /**
     * The {@link InactiveGroupLidMigrationService} started during bootstrap so inactive group chats migrate to LID
     * addressing in the background.
     */
    private final InactiveGroupLidMigrationService inactiveGroupLidMigrationService;

    /**
     * The {@link WamService} used to commit the launch-time {@code ClockSkewDifferenceT} event and to initialise
     * itself with the up-to-date AB-prop-derived globals.
     */
    private final WamService wamService;

    /**
     * The {@link WebAppStateService} resumed during bootstrap and given its periodic-sync job tick.
     */
    private final WebAppStateService webAppStateService;

    /**
     * The shared {@link MediaConnectionService} updated each time the periodic {@code media_conn} IQ reply lands.
     */
    private final MediaConnectionService mediaConnectionService;

    /**
     * The {@link TosService} used at bootstrap to refresh the interoperability Terms-of-Service acceptance state when
     * notice fetching is enabled.
     */
    private final TosService tosService;

    /**
     * The one-shot guard ensuring the bootstrap runs at most once per connection; cleared by {@link #reset()} on
     * socket teardown.
     */
    private final AtomicBoolean started;

    /**
     * The currently scheduled media-connection refresh job, or {@code null} when none is pending.
     *
     * <p>Held so {@link #reset()} can cancel the next refresh when the socket is torn down; re-armed at the end of
     * every {@link #refreshMediaConnection()} pass.
     */
    private volatile ScheduledTask mediaConnectionRefreshJob;

    /**
     * Constructs a new success stream handler bound to the given services.
     *
     * @param whatsapp                         the {@link LinkedWhatsAppClient}; must not be {@code null}
     * @param abPropsService                   the {@link ABPropsService}; must not be {@code null}
     * @param deviceService                    the {@link DeviceService}; must not be {@code null}
     * @param lidMigrationService              the {@link LidMigrationService}; must not be {@code null}
     * @param inactiveGroupLidMigrationService the {@link InactiveGroupLidMigrationService}; must not be {@code null}
     * @param wamService                       the {@link WamService}; must not be {@code null}
     * @param webAppStateService               the {@link WebAppStateService}; must not be {@code null}
     * @param mediaConnectionService           the {@link MediaConnectionService}; must not be {@code null}
     * @param tosService                       the {@link TosService}; must not be {@code null}
     * @throws NullPointerException if any service is {@code null}
     */
    public SuccessStreamHandler(
            LinkedWhatsAppClient whatsapp,
            ABPropsService abPropsService,
            DeviceService deviceService,
            LidMigrationService lidMigrationService,
            InactiveGroupLidMigrationService inactiveGroupLidMigrationService,
            WamService wamService,
            WebAppStateService webAppStateService,
            MediaConnectionService mediaConnectionService,
            TosService tosService
    ) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
        this.deviceService = Objects.requireNonNull(deviceService, "deviceService cannot be null");
        this.lidMigrationService = Objects.requireNonNull(lidMigrationService, "lidMigrationService cannot be null");
        this.inactiveGroupLidMigrationService = Objects.requireNonNull(inactiveGroupLidMigrationService, "inactiveGroupLidMigrationService cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.webAppStateService = Objects.requireNonNull(webAppStateService, "webAppStateService cannot be null");
        this.mediaConnectionService = Objects.requireNonNull(mediaConnectionService, "mediaConnectionService cannot be null");
        this.tosService = Objects.requireNonNull(tosService, "tosService cannot be null");
        this.started = new AtomicBoolean();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Drives {@link #bootstrap(Stanza)} the first time the {@code <success>} stanza is observed on the current
     * connection; any later {@code <success>} stanzas are no-ops until {@link #reset()} runs on socket teardown.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebHandleSuccess", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void handle(Stanza stanza) {
        if (started.compareAndSet(false, true)) {
            bootstrap(stanza);
        } else if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "duplicate success stanza ignored, bootstrap already ran");
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Clears the one-shot bootstrap guard so the next {@code <success>} stanza after a reconnection re-runs the full
     * sequence, and cancels any pending media-connection refresh job.
     */
    @Override
    public void reset() {
        started.set(false);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "success handler reset for reconnect");
        }
        var job = mediaConnectionRefreshJob;
        if (job != null) {
            job.cancel();
            mediaConnectionRefreshJob = null;
        }
    }

    /**
     * Drives the one-shot post-handshake bootstrap sequence: parses the {@code <success>} attributes, updates the
     * {@code me} identity, syncs A/B props, primes LID migration, starts the WAM, device and inactive-group services,
     * resumes app-state syncing, transitions the socket out of passive mode, fans out
     * {@link LoggedInListener#onLoggedIn(LinkedWhatsAppClient)} and persists the store.
     *
     * <p>Reachable only via {@link #handle(Stanza)} the first time a {@code <success>} stanza is observed on the current
     * connection.
     *
     * @implNote This implementation runs the clock-skew normalisation, AB-prop sync, WAM initialisation, device-service
     * kick-off, inactive-group migration, web-app-state resume, initial pre-key upload via
     * {@link #uploadInitialPreKeysIfNeeded()}, passive-mode iq, {@link LinkedWhatsAppClient#editPresence(ContactStatus)}
     * broadcast, compliance probes, collection listener replay, newsletter and business-profile bootstraps in the exact
     * order required by WA Web's await chain. The pre-key upload runs before {@link LinkedWhatsAppClient#enableActiveMode()}
     * so the primary device can establish the Signal session it needs to encrypt history-sync messages as soon as the
     * companion goes active. The {@link ContactStatus#AVAILABLE} presence broadcast that follows tells the relay the
     * companion is online so the server flushes the queued primary-to-companion {@code <message>} envelopes (history-
     * sync notifications among them) instead of parking them until the next presence broadcast. Newsletter and
     * business-profile bootstraps are fire-and-forget on virtual threads because their server round trips can be slow
     * and must not block the listener fan-out; the final store save is best-effort because the bootstrap must not fail
     * on a serializer hiccup.
     *
     * @param stanza the parsed {@code <success>} stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleSuccess", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void bootstrap(Stanza stanza) {
        var store = whatsapp.store();

        if (Log.INFO) {
            LOGGER.log(Level.INFO, "success stanza received, starting post-handshake bootstrap clientType={0}",
                    store.accountStore().clientType());
        }

        var replayChats = store.syncStore().syncedChats();
        var replayContacts = store.syncStore().syncedContacts();
        var replayNewsletters = store.syncStore().syncedNewsletters();
        var replayStatus = store.syncStore().syncedStatus();

        store.accountStore().setOnline(true);
        store.accountStore().setRegistered(true);

        var serverTimestampSeconds = stanza.getAttributeAsLong("t", 0L);
        if (serverTimestampSeconds > 0) {
            var localSeconds = Instant.now().getEpochSecond();
            var skewSeconds = serverTimestampSeconds - localSeconds;
            var clockSkewHourly = (int) Math.round(skewSeconds / 3600.0);
            store.syncStore().setClockSkewSeconds(skewSeconds);
            if (clockSkewHourly != 0) {
                Thread.ofVirtual().start(() -> {
                    try {
                        if (abPropsService.getBool(ABProp.LOG_CLOCK_SKEW)) {
                            wamService.commit(new ClockSkewDifferenceTEventBuilder()
                                    .clockSkewHourly(clockSkewHourly)
                                    .build());
                        }
                    } catch (WhatsAppException exception) {
                        whatsapp.handleFailure(exception);
                    }
                });
            }
        }

        stanza.getAttributeAsJid("lid").ifPresent(store.accountStore()::setLid);

        var displayName = stanza.getAttributeAsString("display_name", null);
        if (displayName != null && !displayName.isBlank()) {
            var oldName = store.accountStore().name().orElse(null);
            store.accountStore().setName(displayName);
            if (!Objects.equals(oldName, displayName)) {
                for (var listener : store.listeners()) {
                    if (listener instanceof LinkedNameChangedListener typed) {
                        Thread.startVirtualThread(() -> typed.onNameChanged(whatsapp, oldName, displayName));
                    }
                }
            }
        }

        lidMigrationService.initialize();

        var abPropsSynced = abPropsService.sync();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "ab props synced={0}", abPropsSynced);
        }

        if (abPropsSynced && abPropsService.getBool(ABProp.LID_ONE_ON_ONE_MIGRATION_ENABLED)) {
            lidMigrationService.enableMigration();
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "lid one-on-one migration enabled");
            }
        } else {
            lidMigrationService.disableMigration();
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "lid one-on-one migration disabled");
            }
        }

        wamService.initialize();

        emitLoginEvent();

        emitWebcRawPlatform();

        // Pull the interoperability Terms-of-Service acceptance state so the inbound-message gating
        // check has it; gated on the fetch flag and run off-thread so it does not delay bootstrap
        if (abPropsService.getBool(ABProp.TOS_CLIENT_STATE_FETCH_ENABLED)) {
            Thread.ofVirtual().start(() -> tosService.refresh(List.of(TosNotice.TOS_3)));
        }

        deviceService.startAdvCheckScheduler();
        deviceService.retryPendingSyncs();
        deviceService.updateMissingKeyDevices();

        inactiveGroupLidMigrationService.start();

        var groupAbpropsRefreshId = stanza.getAttributeAsLong("group_abprops", 0L);
        if (groupAbpropsRefreshId != 0L && serverTimestampSeconds > 0
                && groupAbpropsRefreshId != store.syncStore().groupAbPropsRefreshId()) {
            store.syncStore().setGroupAbPropsEmergencyPushTimestamp(Instant.ofEpochSecond(serverTimestampSeconds));
            store.syncStore().setGroupAbPropsRefreshId(groupAbpropsRefreshId);
        }

        webAppStateService.resumeAfterRestart();
        webAppStateService.startPeriodicSyncJob();

        refreshMediaConnection();

        uploadInitialPreKeysIfNeeded();

        whatsapp.enableActiveMode();

        whatsapp.editPresence(ContactStatus.AVAILABLE);

        runComplianceProbe("reachout timelock", whatsapp::queryReachoutTimelock);

        runComplianceProbe("new-chat capping info", whatsapp::queryNewChatMessageCappingInfo);

        syncPrivacySettings();

        syncPrivacyDisallowedListsMex();

        for (var listener : store.listeners()) {
            if (listener instanceof LoggedInListener typed) {
                Thread.startVirtualThread(() -> typed.onLoggedIn(whatsapp));
            }
        }

        replayCachedCollectionListeners(replayChats, replayContacts, replayNewsletters, replayStatus);

        bootstrapNewsletterBackend();

        bootstrapBusinessCertificate();

        if (store.accountStore().clientType() == LinkedWhatsAppClientType.WEB) {
            Thread.startVirtualThread(() -> {
                try {
                    whatsapp.refreshWhatsAppWebGraphQlSession();
                } catch (WhatsAppWebGraphQlException.SessionUnseeded _) {

                } catch (WhatsAppWebGraphQlException exception) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING,
                                "whatsapp web graphql credentials auto-refresh failed", exception);
                    }
                }
            });
            if (store.accountStore().primaryPlatform().filter(LinkedPrimaryPlatform::isBusiness).isPresent()
                    && store.webSessionStore().facebookGraphQlSession().isEmpty()) {
                Thread.startVirtualThread(() -> {
                    try {
                        whatsapp.refreshFacebookGraphQlSession();
                    } catch (WhatsAppFacebookGraphQlException.SessionUnseeded _) {

                    } catch (WhatsAppFacebookGraphQlException exception) {
                        if (Log.WARNING) {
                            LOGGER.log(Level.WARNING,
                                    "facebook graphql credentials auto-refresh failed", exception);
                        }
                    }
                });
            }
        }

        try {
            store.save();
        } catch (Exception exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "store save after bootstrap failed", exception);
            }
        }

        if (Log.INFO) {
            LOGGER.log(Level.INFO, "post-handshake bootstrap complete");
        }
    }

    /**
     * Replays the cached chats, contacts, newsletters and status collections to every registered listener for each
     * collection that was already synced when the connection was established.
     *
     * <p>Surfaces each dataset exactly once per login through
     * {@link LinkedChatsListener#onChats(LinkedWhatsAppClient, java.util.Collection)},
     * {@link LinkedContactsListener#onContacts(LinkedWhatsAppClient, java.util.Collection)},
     * {@link LinkedNewslettersListener#onNewsletters(LinkedWhatsAppClient, java.util.Collection)} and
     * {@link LinkedStatusListener#onStatus(LinkedWhatsAppClient, java.util.Collection)}. This path covers the reconnect
     * case, where the data was synced in a prior session and read back from the persisted store: the history-sync
     * pipeline will not re-deliver it, so a fresh listener would otherwise never see it. A collection that was still
     * unsynced at connection time is deliberately skipped here, because
     * {@link com.github.auties00.cobalt.sync.WebHistorySyncService} fires its callback once the first chunk lands;
     * replaying it as well would double-fire the listener. The callback fires even when the cached collection is empty
     * so embedders can rely on a one-shot post-login signal.
     *
     * @param replayChats       whether the chats collection was already synced at connection time
     * @param replayContacts    whether the contacts collection was already synced at connection time
     * @param replayNewsletters whether the newsletters collection was already synced at connection time
     * @param replayStatus      whether the status collection was already synced at connection time
     * @implNote This implementation gates on the connection-time snapshot rather than the live sync gates so that a
     * fresh login, whose gates flip mid-bootstrap while this thread blocks on compliance and privacy round-trips, does
     * not replay a collection that {@link com.github.auties00.cobalt.sync.WebHistorySyncService} has already delivered.
     * Each callback fans out on a fresh virtual thread so a slow listener cannot stall the bootstrap. WA Web has no
     * equivalent because its UI components subscribe directly to the reactive collections.
     */
    private void replayCachedCollectionListeners(boolean replayChats, boolean replayContacts,
                                                 boolean replayNewsletters, boolean replayStatus) {
        var store = whatsapp.store();
        var listeners = store.listeners();
        if (listeners.isEmpty()) {
            return;
        }
        if (replayChats) {
            var chats = store.chatStore().chats();
            for (var listener : listeners) {
                if (listener instanceof LinkedChatsListener typed) {
                    Thread.startVirtualThread(() -> typed.onChats(whatsapp, chats));
                }
            }
        }
        if (replayContacts) {
            var contacts = store.contactStore().contacts();
            for (var listener : listeners) {
                if (listener instanceof LinkedContactsListener typed) {
                    Thread.startVirtualThread(() -> typed.onContacts(whatsapp, contacts));
                }
            }
        }
        if (replayNewsletters) {
            var newsletters = store.chatStore().newsletters();
            for (var listener : listeners) {
                if (listener instanceof LinkedNewslettersListener typed) {
                    Thread.startVirtualThread(() -> typed.onNewsletters(whatsapp, newsletters));
                }
            }
        }
        if (replayStatus) {
            var status = store.chatStore().status();
            for (var listener : listeners) {
                if (listener instanceof LinkedStatusListener typed) {
                    Thread.startVirtualThread(() -> typed.onStatus(whatsapp, status));
                }
            }
        }
    }

    /**
     * Drives the one-shot newsletter metadata fetch that backfills the newsletter collection on first login.
     *
     * <p>Gated on three conditions: this is a web client (newsletters are a web-only companion feature), the configured
     * history settings include newsletters (see {@link com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore#hasHistoryNewsletters()}), and the
     * newsletter sync gate is still false. The {@link LinkedWhatsAppClient#refreshNewsletters()} call sets the gate and fans
     * out {@link LinkedNewslettersListener#onNewsletters(LinkedWhatsAppClient, java.util.Collection)} internally.
     *
     * @implNote This implementation runs the fetch on a fresh virtual thread because the round trip can be slow on
     * first install; failures are logged through {@link #LOGGER} and swallowed so the rest of the bootstrap
     * is unaffected.
     */
    @WhatsAppWebExport(moduleName = "WAWebBootstrapNewsletter", exports = "bootstrapNewsletterBackend",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void bootstrapNewsletterBackend() {
        var store = whatsapp.store();
        if (store.accountStore().clientType() != LinkedWhatsAppClientType.WEB || store.syncStore().syncedNewsletters()) {
            return;
        }
        if (!store.syncStore().hasHistoryNewsletters()) {
            return;
        }
        Thread.startVirtualThread(() -> {
            try {
                whatsapp.refreshNewsletters();
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "initial newsletter metadata fetch complete");
                }
            } catch (Exception exception) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "initial newsletter metadata fetch failed", exception);
                }
            }
        });
    }

    /**
     * Drives the one-shot business-profile fetch that backfills the verified-name and business-profile fields when the
     * business-certificate sync gate is still false on bootstrap.
     *
     * <p>The {@link com.github.auties00.cobalt.stream.notification.business.NotificationBusinessStreamHandler} already
     * flips the gate when the primary device pushes a {@code verified_name} or {@code profile} notification; this
     * proactive call covers the case where the companion has just paired (or has been re-paired after invalidation) and
     * the primary has not yet emitted the notification. The flag is set after the call regardless of result so
     * non-business accounts do not re-query on every reconnect.
     *
     * @implNote This implementation runs the fetch on a fresh virtual thread and applies the resulting
     * {@link BusinessProfile} via {@link #applyOwnBusinessProfile(BusinessProfile)} so the bootstrap-fetch path
     * produces the same store mutations as the notification path.
     */
    private void bootstrapBusinessCertificate() {
        var store = whatsapp.store();
        if (store.syncStore().syncedBusinessCertificate()) {
            return;
        }
        var self = store.accountStore().jid().orElse(null);
        if (self == null) {
            return;
        }
        Thread.startVirtualThread(() -> {
            try {
                whatsapp.queryBusinessProfile(self.withoutData())
                        .ifPresent(this::applyOwnBusinessProfile);
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "initial business certificate fetch complete");
                }
            } catch (Exception exception) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "initial business certificate fetch failed", exception);
                }
            } finally {
                store.syncStore().setSyncedBusinessCertificate(true);
            }
        });
    }

    /**
     * Applies the fields lifted from a freshly-fetched {@link BusinessProfile} onto the store so the bootstrap fetch
     * produces the same resulting state as the notification path.
     *
     * <p>Keeping the two paths field-aligned with the
     * {@link com.github.auties00.cobalt.stream.notification.business.NotificationBusinessStreamHandler} copy ensures
     * the embedder sees the same observable store state regardless of which path won the race on a fresh pair.
     *
     * @param profile the freshly-fetched {@link BusinessProfile}
     */
    private void applyOwnBusinessProfile(BusinessProfile profile) {
        whatsapp.store().accountStore().setBusinessDescription(profile.description().orElse(null))
                .setBusinessAddress(profile.address().orElse(null))
                .setBusinessEmail(profile.email().orElse(null))
                .setBusinessWebsites(profile.websites())
                .setBusinessCategories(profile.categories());
    }

    /**
     * The logger for {@link SuccessStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(SuccessStreamHandler.class);

    /**
     * The floor delay between successive media-connection refresh attempts.
     *
     * <p>Bounds the schedule away from zero when the server returns a tiny or zero TTL so a malformed response cannot
     * turn the refresh loop into a tight spin.
     */
    private static final Duration MEDIA_CONNECTION_MIN_REFRESH_DELAY = Duration.ofSeconds(30);

    /**
     * The delay before retrying a failed media-connection refresh.
     *
     * <p>Used when the {@code media_conn} query throws so a transient WhatsApp Web GraphQL error does not permanently halt the renewal
     * loop.
     */
    private static final Duration MEDIA_CONNECTION_RETRY_DELAY = Duration.ofMinutes(1);

    /**
     * Runs a single post-success MEX compliance probe and logs any failure without re-throwing.
     *
     * <p>Probes mirror the housekeeping the official client emits at app launch (reachout timelock, new-chat capping
     * info). They are fire-and-forget on Cobalt because the responses have no store slot yet; only the
     * server-observable round trip matters for compliance.
     *
     * @param probeName the human-readable probe name used in log output
     * @param probe     the probe to run; must not be {@code null}
     */
    private static void runComplianceProbe(String probeName, Supplier<?> probe) {
        try {
            probe.get();
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "compliance probe failed: " + probeName, throwable);
            }
        }
    }

    /**
     * The pre-key batch size uploaded once per device when the local store has not yet generated any pre-keys.
     *
     * @implNote This value matches WA Web's {@code UPLOAD_KEYS_COUNT}; a larger batch reduces the chance of pre-key
     * exhaustion before the steady-state pre-key-low replenishment can fire.
     */
    private static final long INITIAL_PRE_KEYS_COUNT = 812;

    /**
     * Uploads an initial Signal pre-key batch when the local store has none yet.
     *
     * <p>On a fresh pair the companion has not yet uploaded any one-time pre-keys to the server, so the primary device
     * cannot fetch a pre-key bundle to establish the Signal session it needs to encrypt the history-sync
     * {@code <message>} envelopes. The upload runs before {@link LinkedWhatsAppClient#enableActiveMode()} so the primary can
     * encrypt and push history-sync notifications as soon as the companion goes active. Subsequent reconnects observe
     * an already-populated pre-key store and skip the upload.
     *
     * @implNote WA Web runs the equivalent upload as a passive task registered before the passive-mode iq; Cobalt has
     * no passive-task pipeline, so the upload is inlined here. Failures are logged through {@link #LOGGER}
     * and swallowed so a transient WhatsApp Web GraphQL error does not abort the rest of the post-success bootstrap; the next
     * pre-key-low {@code <notification type="encrypt">} push retries the upload.
     */
    @WhatsAppWebExport(moduleName = "WAWebRegisterPassiveTasks",
            exports = "registerPassiveTaskForStartUp", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebUploadPrekeysForRegTask", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void uploadInitialPreKeysIfNeeded() {
        if (whatsapp.store().signalStore().hasPreKeys()) {
            return;
        }
        try {
            whatsapp.sendPreKeys(INITIAL_PRE_KEYS_COUNT);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "uploaded {0} initial pre-keys", INITIAL_PRE_KEYS_COUNT);
            }
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "initial pre-key upload after pairing failed", throwable);
            }
        }
    }

    /**
     * Fetches a fresh media connection, publishes it to the shared {@link MediaConnectionService}, and arms the next
     * refresh tick.
     *
     * <p>Invoked from {@link #bootstrap(Stanza)} so the very first upload or download path sees a populated connection.
     * The next refresh is re-armed at the end of every pass on the cadence advertised by the server, so a long quiet
     * period followed by a sudden download burst does not stall waiting for a fresh {@code media_conn} round trip. The
     * scheduled task is cancelled by {@link #reset()} on socket teardown.
     *
     * @implNote This implementation diverges from WA Web's lazy-on-demand refresh, which fires fresh queries only when
     * an upload or download path observes a stale singleton. Cobalt schedules the next refresh eagerly on a virtual
     * thread; the cadence follows {@link MediaConnectionService#needsRefresh()}'s
     * {@code min(ttl, floor(0.8 * authTtl))} formula, clamped to {@link #MEDIA_CONNECTION_MIN_REFRESH_DELAY} so a
     * malformed response cannot produce a zero or negative delay. On failure the next tick fires after
     * {@link #MEDIA_CONNECTION_RETRY_DELAY} so a transient WhatsApp Web GraphQL error does not permanently halt the loop.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob",
            exports = "queryMediaConn", adaptation = WhatsAppAdaptation.ADAPTED)
    private void refreshMediaConnection() {
        Duration nextDelay;
        try {
            var response = queryMediaConnection();
            mediaConnectionService.update(response);
            var ttlSeconds = (long) mediaConnectionService.ttl();
            var authThresholdSeconds = (long) Math.floor(mediaConnectionService.authTtl() * 0.8);
            var delay = Duration.ofSeconds(Math.min(ttlSeconds, authThresholdSeconds));
            nextDelay = delay.compareTo(MEDIA_CONNECTION_MIN_REFRESH_DELAY) < 0
                    ? MEDIA_CONNECTION_MIN_REFRESH_DELAY
                    : delay;
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "media connection refresh failed, retrying in " + MEDIA_CONNECTION_RETRY_DELAY.toSeconds() + "s",
                        throwable);
            }
            nextDelay = MEDIA_CONNECTION_RETRY_DELAY;
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "media connection refresh scheduled, nextRefreshSeconds={0}", nextDelay.toSeconds());
        }
        mediaConnectionRefreshJob = ScheduledTask.scheduleDelayed(nextDelay, this::refreshMediaConnection);
    }

    /**
     * Sends a fresh {@code media_conn} IQ and returns the response stanza.
     *
     * <p>The reply is fed into {@link MediaConnectionService#update(Stanza)} by {@link #refreshMediaConnection()}. Throws
     * when the server returns a malformed or error response so the caller can apply the retry-delay backoff.
     *
     * @return the IQ response stanza
     * @throws WhatsAppServerRuntimeException if the server rejects the query or returns an unrecognised response
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryMediaConnsJob",
            exports = "queryMediaConn", adaptation = WhatsAppAdaptation.DIRECT)
    private Stanza queryMediaConnection() {
        var request = new IqQueryMediaConnsRequest();
        var requestBuilder = request.toStanza();
        var response = whatsapp.sendNode(requestBuilder);
        return switch (IqQueryMediaConnsResponse.of(response, requestBuilder.build()).orElse(null)) {
            case IqQueryMediaConnsResponse.Success _ -> response;
            case IqQueryMediaConnsResponse.ClientError clientError ->
                    throw new WhatsAppServerRuntimeException(
                            "Query media conns rejected: code=" + clientError.errorCode()
                            + ", text=" + clientError.errorText().orElse(null));
            case IqQueryMediaConnsResponse.ServerError serverError ->
                    throw new WhatsAppServerRuntimeException(
                            "Query media conns server error: code=" + serverError.errorCode()
                            + ", text=" + serverError.errorText().orElse(null));
            case null -> throw new WhatsAppServerRuntimeException(
                    "Query media conns: response did not match any documented variant");
        };
    }

    /**
     * Fetches the account privacy settings during the post-success bootstrap so that
     * {@link LinkedWhatsAppSettingsStore#privacySettings()} is populated before the
     * {@code onLoggedIn} listeners fire.
     *
     * <p>Issues the {@code <iq xmlns="privacy" type="get">} query through
     * {@link LinkedWhatsAppClient#refreshPrivacySettings()}, which stores each returned category.
     *
     * @implNote This implementation swallows a failure through {@link #LOGGER} so a transient WhatsApp Web GraphQL error
     * does not abort the rest of the bootstrap; the settings can still be re-queried on demand.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncPrivacyDisallowedLists",
            exports = "syncPrivacyDisallowedLists", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebQueryPrivacySettingsJob", exports = "getPrivacySettings",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void syncPrivacySettings() {
        try {
            whatsapp.refreshPrivacySettings();
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "privacy settings refreshed");
            }
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "cannot fetch privacy settings", throwable);
            }
        }
    }

    private void syncPrivacyDisallowedListsMex() {
        var meLid = whatsapp.store().accountStore().lid().orElse(null);
        if (meLid == null) {
            return;
        }
        var categories = List.of("ABOUT", "GROUPADD", "LAST", "PROFILE");
        for (var category : categories) {
            try {
                whatsapp.queryPrivacyDisallowedList(meLid, "", category, "DENYLIST");
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "privacy disallowed list reconciled category={0}", category);
                }
            } catch (Throwable throwable) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "cannot reconcile privacy disallowed list " + category, throwable);
                }
            }
        }
    }

    /**
     * The Noise handshake version advertised by the web (companion) handshake shape.
     *
     * @implNote This value is the leading byte of Cobalt's web handshake version footer
     * ({@code {6, DICTIONARY_VERSION}} in {@code WhatsAppSocketClient}); it is reported in the
     * {@code Login} event's {@code noiseProtocolVersion} field for web clients.
     */
    private static final long WEB_NOISE_PROTOCOL_VERSION = 6;

    /**
     * The Noise handshake version advertised by the mobile (primary) handshake shape.
     *
     * @implNote This value is the leading byte of Cobalt's mobile handshake version footer
     * ({@code {5, DICTIONARY_VERSION}} in {@code WhatsAppSocketClient}); it is reported in the
     * {@code Login} event's {@code noiseProtocolVersion} field for mobile clients.
     */
    private static final long MOBILE_NOISE_PROTOCOL_VERSION = 5;

    /**
     * Commits the once-per-connection {@code Login} connection-diagnostics event that WhatsApp Web logs from its
     * connection worker after a successful authentication handshake.
     *
     * <p>Fires from {@link #bootstrap(Stanza)} at the point the {@code <success>} stanza confirms the login succeeded, so
     * {@link LoginResultType#OK} is the only result this path can report. The fields describe how Cobalt actually reached
     * the relay: a user-activated ({@link ConnectionOriginType#PERSON}) direct connection to the primary host
     * ({@link ConnectionSequenceStepType#PRIMARY}, {@link LoginHostType#G_WHATSAPP_NET}) over TCP 443
     * ({@link LoginPortNumber#P443}) using the JVM's system DNS resolver ({@link DnsResolutionMethodType#SYSTEM},
     * {@link LoginDnsResolverType#SYSTEM}) and a plain platform socket ({@link StreamSocketProviderType#PLATFORM_SOCKET});
     * {@code passive} mirrors the login client-payload flag (a web companion that already holds a JID logs in passively,
     * then this handler transitions it to active), and {@code noiseProtocolVersion} is the handshake-shape version footer
     * (web {@value #WEB_NOISE_PROTOCOL_VERSION}, mobile {@value #MOBILE_NOISE_PROTOCOL_VERSION}). The queue counters are
     * zero because a freshly-authenticated connection has neither pending acks nor unprocessed messages, and
     * {@code retryCount} is zero because the {@code <success>} arrived on the current attempt.
     *
     * @implNote The connection and login timers and the resolved-address counts are not tracked by Cobalt's socket layer
     * and are not reachable from this handler, so they are fabricated within the plausible bands a real browser session
     * reports (a sub-second connect, a login a few hundred milliseconds longer, a handful of A and AAAA records) with
     * per-connection jitter drawn from {@link ThreadLocalRandom} so the beacon does not fingerprint every Cobalt session
     * identically; {@code traceIdInt} is a fresh random per-connection correlation id, matching WA's randomly-generated
     * connection trace id. Fields WhatsApp only populates on Android, on error, or after an explicit logout
     * ({@code androidKeystoreState}, {@code serverErrorCode}, {@code logoutSessionId}) and the opaque server-assigned
     * point-of-presence string ({@code loginResolvedPop}) are left unset, exactly as a successful web login omits them.
     */
    @WhatsAppWebExport(moduleName = "WAWebLoginWamEvent", exports = "LoginWamEvent",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitLoginEvent() {
        var account = whatsapp.store().accountStore();
        var web = account.clientType() == LinkedWhatsAppClientType.WEB;
        var passive = web && account.jid().isPresent();
        var random = ThreadLocalRandom.current();
        var connectionMillis = random.nextLong(220L, 680L);
        var loginMillis = connectionMillis + random.nextLong(280L, 820L);
        wamService.commit(new LoginEventBuilder()
                .loginResult(LoginResultType.OK)
                .connectionOrigin(ConnectionOriginType.PERSON)
                .connectionSequenceStep(ConnectionSequenceStepType.PRIMARY)
                .sequenceStep(2)
                .dnsResolutionMethod(DnsResolutionMethodType.SYSTEM)
                .loginDnsResolver(LoginDnsResolverType.SYSTEM)
                .loginIpSource(LoginHostType.G_WHATSAPP_NET)
                .loginPort(LoginPortNumber.P443)
                .loginSocketProvider(StreamSocketProviderType.PLATFORM_SOCKET)
                .noiseProtocolVersion(web ? WEB_NOISE_PROTOCOL_VERSION : MOBILE_NOISE_PROTOCOL_VERSION)
                .passive(passive)
                .longConnect(false)
                .networkIsVpn(false)
                .loginHistoryStepResult(true)
                .retryCount(0)
                .pendingAcksCount(0)
                .unprocessedMessageCount(0)
                .numIpv4Addresses(random.nextLong(3L, 7L))
                .numIpv6Addresses(random.nextLong(2L, 5L))
                .connectionT(Instant.ofEpochMilli(connectionMillis))
                .loginT(Instant.ofEpochMilli(loginMillis))
                .traceIdInt(random.nextLong(1L, Long.MAX_VALUE))
                .build());
    }

    /**
     * Commits the {@code WebcRawPlatforms} diagnostic that records the raw platform token of the linked primary phone as
     * observed by a web companion.
     *
     * <p>WhatsApp Web commits this event from the connection model's {@code change:platform} handler the moment the
     * companion learns (or relearns) the primary device's platform. Cobalt learns the same token during the pairing
     * handshake and stores it as {@link com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore#primaryPlatform()};
     * this method surfaces it once per bootstrap. The event is web-companion-only, so it is skipped for mobile
     * (primary) clients and when the primary platform has not been recorded, and the committed value is the wire token
     * ({@code "android"}, {@code "iphone"}, {@code "ipad"}, {@code "smba"}, {@code "smbi"}) rather than the enum name.
     *
     * @implNote WhatsApp gates this commit behind the {@code gkx} rollout flag {@code 26259}, which Cobalt does not
     * model; the event is therefore emitted whenever the primary platform is known, which is the faithful headless
     * equivalent of the feature being enabled.
     */
    @WhatsAppWebExport(moduleName = "WAWebConnModel", exports = "$ConnImpl$p_1",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitWebcRawPlatform() {
        var account = whatsapp.store().accountStore();
        if (account.clientType() != LinkedWhatsAppClientType.WEB) {
            return;
        }
        account.primaryPlatform()
                .map(LinkedPrimaryPlatform::wireValue)
                .ifPresent(platform -> wamService.commit(new WebcRawPlatformsEventBuilder()
                        .webcRawPlatform(platform)
                        .build()));
    }
}

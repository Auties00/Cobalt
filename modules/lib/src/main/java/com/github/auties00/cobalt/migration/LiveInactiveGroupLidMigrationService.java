package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMetadata;
import com.github.auties00.cobalt.wire.linked.chat.community.CommunityMetadata;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.util.ScheduledTask;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Live implementation of {@link InactiveGroupLidMigrationService}: a background service that
 * re-queries inactive group metadata so that groups still on phone-number addressing flip to LID
 * addressing.
 *
 * <p>WhatsApp's group-fanout flow flips a group from PN to LID addressing as
 * soon as a member sends a message, but inactive groups stay on PN
 * indefinitely until someone explicitly asks the server for fresh metadata.
 * This service walks the local chat store, picks every group still on PN,
 * issues a metadata query for each one, and re-checks once all queries land.
 * Once no PN-mode groups remain, the migration is marked complete through
 * {@link #setInactiveGroupLidMigrationComplete()}, which persists across
 * restarts so a re-connecting client does not re-run the sweep; callers observe
 * completion via {@link #isInactiveGroupLidMigrationComplete()}.
 *
 * <p>The service is started once per session through {@link #start()} and is
 * stopped through {@link #reset()} when the client disconnects. Callers never
 * invoke the migration pass directly.
 *
 * @implNote
 * This implementation diverges from WhatsApp Web's batched job in three ways.
 * First, queries are issued one-by-one through
 * {@link LinkedWhatsAppClient#queryChatMetadata(com.github.auties00.cobalt.wire.core.jid.JidProvider)}
 * instead of through a batched group-metadata query, because the per-chat
 * query is the only public Cobalt API. Second, the group membership check is
 * satisfied by the per-chat metadata gate (a group whose metadata is missing
 * is excluded) rather than by a separate bulk membership pass. Third, the
 * failure handling does not emit telemetry; an in-flight {@link Exception}
 * from the per-group query is logged and the next group is tried.
 */
@WhatsAppWebModule(moduleName = "WAWebInactiveGroupLidMigrationJob")
@WhatsAppWebModule(moduleName = "WAWebInactiveGroupLidMigration")
public final class LiveInactiveGroupLidMigrationService implements InactiveGroupLidMigrationService {
    /**
     * The logger for {@link LiveInactiveGroupLidMigrationService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveInactiveGroupLidMigrationService.class);

    /**
     * Delay before the first migration pass after {@link #start()}.
     *
     * @implNote
     * This implementation uses 60 seconds so the first sweep does not race the
     * initial offline-resume burst, matching WhatsApp Web's one-minute task
     * pacing.
     */
    private static final Duration INITIAL_DELAY = Duration.ofSeconds(60);

    /**
     * Delay before a retry pass when some groups remain on PN after the
     * initial sweep.
     *
     * @implNote
     * This implementation uses a 24-hour cadence, matching the daily retry
     * budget WhatsApp Web applies.
     */
    private static final Duration RETRY_DELAY = Duration.ofDays(1);

    /**
     * Client used to look up the local chat store and to issue the per-group
     * metadata queries.
     */
    private final LinkedWhatsAppClient client;

    /**
     * Service used to read the AB-prop snapshot.
     *
     * @implNote
     * This implementation keeps the {@link ABPropsService} as a field for
     * symmetry with the other migration services and to support a future
     * AB-prop gate; the current code path does not read any AB prop because
     * WhatsApp Web removed its gating prop from the live bundle.
     */
    private final ABPropsService abPropsService;

    /**
     * The currently scheduled migration task, or {@code null} when no task is
     * pending.
     *
     * <p>The reference is held so {@link #reset()} can cancel a pending retry
     * when the client disconnects.
     */
    private volatile ScheduledTask scheduledTask;

    /**
     * Constructs a new service bound to the given client and AB-props service.
     *
     * @param client         the client used to query group metadata
     * @param abPropsService the AB-props service held for future gating
     * @throws NullPointerException if either argument is {@code null}
     */
    public LiveInactiveGroupLidMigrationService(LinkedWhatsAppClient client, ABPropsService abPropsService) {
        this.client = Objects.requireNonNull(client, "client");
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        if (isInactiveGroupLidMigrationComplete()) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "inactive-group lid migration: already complete, skipping start");
            }
            return;
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "inactive-group lid migration: scheduling first pass in {0}", INITIAL_DELAY);
        }
        scheduledTask = ScheduledTask.scheduleDelayed(INITIAL_DELAY, this::run);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        var task = scheduledTask;
        if (task != null) {
            task.cancel();
            scheduledTask = null;
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "inactive-group lid migration: cancelled pending scheduled pass");
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation reads the persisted flag from the account sub-store, so a client that
     * finished the sweep in an earlier session reports completion immediately after restart and never
     * re-runs the migration.
     */
    @WhatsAppWebExport(moduleName = "WAWebInactiveGroupLidMigration",
            exports = "isInactiveGroupLidMigrationComplete",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public boolean isInactiveGroupLidMigrationComplete() {
        return client.store().accountStore().inactiveGroupLidMigrationComplete();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation latches the flag in the account sub-store, which is snapshotted to
     * {@code store.proto}, so completion survives restarts.
     */
    @WhatsAppWebExport(moduleName = "WAWebInactiveGroupLidMigration",
            exports = "setInactiveGroupLidMigrationComplete",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void setInactiveGroupLidMigrationComplete() {
        client.store().accountStore().setInactiveGroupLidMigrationComplete(true);
    }

    /**
     * Executes a single migration pass over the local chat store.
     *
     * <p>The pass first short-circuits when the migration is already complete.
     * It then collects every PN group through {@link #findPnGroups()}, marking
     * the migration complete and returning if none remain. Otherwise it issues
     * a metadata query for each PN group, re-runs the scan, and either marks
     * the migration complete (no PN groups left) or schedules a retry after
     * {@link #RETRY_DELAY}. This method is package-private so the test suite
     * can exercise the body directly, bypassing the {@link #INITIAL_DELAY}
     * scheduler; production callers go through {@link #start()}.
     *
     * @implNote
     * This implementation drops the AB-prop check WhatsApp Web's predecessor
     * used because the gating prop has been retired in the live bundle; only
     * the completion-state guard remains. Exceptions from individual
     * {@link LinkedWhatsAppClient#queryChatMetadata(com.github.auties00.cobalt.wire.core.jid.JidProvider)}
     * calls are caught and logged so a single failure does not abort the whole
     * sweep, and the outer catch swallows everything.
     */
    @WhatsAppWebExport(moduleName = "WAWebInactiveGroupLidMigrationJob",
            exports = "migrateInactiveGroupsToLid",
            adaptation = WhatsAppAdaptation.ADAPTED)
    void run() {
        try {
            if (isInactiveGroupLidMigrationComplete()) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "inactive-group lid migration: already complete, skipping pass");
                }
                return;
            }

            if (Log.INFO) {
                LOGGER.log(Level.INFO, "inactive-group lid migration: starting pass");
            }

            var pnGroups = findPnGroups();
            if (pnGroups.isEmpty()) {
                if (Log.INFO) {
                    LOGGER.log(Level.INFO, "inactive-group lid migration: no PN groups found, complete");
                }
                setInactiveGroupLidMigrationComplete();
                return;
            }

            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "inactive-group lid migration: found {0} PN groups", pnGroups.size());
            }

            for (var groupJid : pnGroups) {
                try {
                    client.queryChatMetadata(groupJid);
                } catch (Exception e) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING,
                                "inactive-group lid migration: metadata query failed for " + new LogRedactable.User(String.valueOf(groupJid)),
                                e);
                    }
                }
            }

            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "inactive-group lid migration: metadata queries issued for {0} groups",
                        pnGroups.size());
            }

            var remaining = findPnGroups();
            if (remaining.isEmpty()) {
                if (Log.INFO) {
                    LOGGER.log(Level.INFO, "inactive-group lid migration: no PN groups left, complete");
                }
                setInactiveGroupLidMigrationComplete();
            } else {
                if (Log.INFO) {
                    LOGGER.log(Level.INFO, "inactive-group lid migration: {0} PN groups remain, retrying in {1}",
                            remaining.size(), RETRY_DELAY);
                }
                scheduledTask = ScheduledTask.scheduleDelayed(RETRY_DELAY, this::run);
            }
        } catch (Exception e) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "inactive-group lid migration: pass failed", e);
            }
        }
    }

    /**
     * Returns every locally cached group that has not yet flipped to LID
     * addressing.
     *
     * <p>This method walks the chat store filtering on
     * {@link JidServer#groupOrCommunity()}, then drops groups whose cached
     * metadata is missing, already on LID, or marked suspended or terminated.
     * The returned list is the input to the per-group metadata refresh loop in
     * {@link #run()}.
     *
     * @implNote
     * This implementation collapses WhatsApp Web's bulk membership filter into
     * the "metadata is non-{@code null}" gate; in Cobalt a group's cached
     * metadata implies membership.
     *
     * @return the JIDs of the groups still on phone-number addressing
     */
    @WhatsAppWebExport(moduleName = "WAWebInactiveGroupLidMigrationJob",
            exports = "migrateInactiveGroupsToLid",
            adaptation = WhatsAppAdaptation.ADAPTED)
    List<Jid> findPnGroups() {
        var store = client.store();
        return store.chatStore().chats()
                .stream()
                .map(chat -> chat.jid())
                .filter(jid -> jid.hasServer(JidServer.groupOrCommunity()))
                .filter(jid -> {
                    var metadata = store.chatStore().findChatMetadata(jid).orElse(null);
                    return metadata != null
                            && !metadata.isLidAddressingMode()
                            && !isSuspendedOrTerminated(metadata);
                })
                .toList();
    }

    /**
     * Returns whether the given group or community metadata is suspended or
     * terminated.
     *
     * <p>This method is used by {@link #findPnGroups()} to skip groups the
     * server has flagged as inactive; suspended or terminated groups will not
     * flip to LID even if their metadata is refreshed.
     *
     * @implNote
     * This implementation uses sealed-interface pattern matching against
     * {@link GroupMetadata} and {@link CommunityMetadata}, the only two
     * permitted subtypes of {@link ChatMetadata} that carry suspended or
     * terminated state.
     *
     * @param metadata the chat metadata to inspect
     * @return {@code true} if the group or community is suspended or terminated
     */
    @WhatsAppWebExport(moduleName = "WAWebInactiveGroupLidMigrationJob",
            exports = "migrateInactiveGroupsToLid",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isSuspendedOrTerminated(ChatMetadata metadata) {
        return switch (metadata) {
            case GroupMetadata gm -> gm.isSuspended() || gm.isTerminated();
            case CommunityMetadata cm -> cm.isSuspended() || cm.isTerminated();
        };
    }
}

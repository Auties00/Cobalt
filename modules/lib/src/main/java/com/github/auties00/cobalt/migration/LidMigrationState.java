package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Position of a single account inside the 1:1 LID migration pipeline.
 *
 * <p>This enum drives the {@link LidMigrationService} state machine. The
 * pipeline starts when a paired client receives the
 * {@code lid_one_on_one_migration_enabled} AB prop, waits for the primary
 * device to deliver its mapping tables, runs the rewrite over the local chat
 * store, and finally records the account as fully migrated. Indices 1 to 5
 * round-trip 1:1 with WhatsApp Web's {@code LidThreadMigrationStatus} enum;
 * indices 0, 6, and 7 ({@link #NOT_STARTED}, {@link #FAILED},
 * {@link #DISABLED}) are Cobalt-only because Cobalt drives the migration
 * through an explicit state machine rather than the ad-hoc UserPrefs flags
 * WhatsApp Web reads.
 *
 * @implNote
 * This implementation keeps the underlying protobuf index of the shared
 * values identical to WhatsApp Web's so persisted state from a JS export could
 * be read back without re-mapping; the Cobalt-only values are appended at the
 * end so they never collide with future additions on the JS side.
 */
@ProtobufEnum
@WhatsAppWebModule(moduleName = "WAWebLid1X1ThreadAccountMigrations.flow")
public enum LidMigrationState {
    /**
     * Initial state before any migration activity has begun.
     *
     * <p>This value is Cobalt-only. WhatsApp Web's pipeline implicitly enters
     * {@link #WAITING_PROP} on first read because the JS code initialises its
     * status UserPrefs entry to {@code WAITING_PROP} on miss. Cobalt models
     * the pre-init phase explicitly so the state-machine transitions are
     * observable in tests.
     */
    NOT_STARTED(0),

    /**
     * Waiting for the {@code lid_one_on_one_migration_enabled} AB prop to flip
     * on.
     *
     * <p>This is the initial state once {@link LidMigrationService} is wired
     * in. {@link LidMigrationService#enableMigration()} advances it to
     * {@link #WAITING_MAPPINGS} when the AB prop reports the migration is
     * enabled.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations.flow",
            exports = "LidThreadMigrationStatus", adaptation = WhatsAppAdaptation.DIRECT)
    WAITING_PROP(1),

    /**
     * Waiting for the primary device to send the LID mapping sync message.
     *
     * <p>This state is reached after the AB-prop gate fires. Mappings arrive
     * through the peer-message channel and are ingested by
     * {@link LidMigrationService#processProtocolMessage(com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMappingSyncPayload)},
     * which advances the state to {@link #READY}.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations.flow",
            exports = "LidThreadMigrationStatus", adaptation = WhatsAppAdaptation.DIRECT)
    WAITING_MAPPINGS(2),

    /**
     * Mappings have been received and validated; the rewrite is ready to run.
     *
     * <p>This state is set by {@link LidMigrationService} after the
     * peer-mapping payload has been cached and the pre-migration sanity checks
     * have passed.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations.flow",
            exports = "LidThreadMigrationStatus", adaptation = WhatsAppAdaptation.DIRECT)
    READY(3),

    /**
     * The rewrite is currently moving threads from PN to LID addressing.
     *
     * <p>This state is set immediately before the per-chat resolution loop in
     * {@link LidMigrationService#executeMigration()} starts producing
     * {@link LidMigrationResolution} values, so concurrent transitions cannot
     * reorder the sweep.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations.flow",
            exports = "LidThreadMigrationStatus", adaptation = WhatsAppAdaptation.DIRECT)
    IN_PROGRESS(4),

    /**
     * The rewrite finished successfully and every eligible thread has been
     * migrated.
     *
     * <p>This state is terminal and is reported by {@link #isTerminal()} as
     * {@code true}.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations.flow",
            exports = "LidThreadMigrationStatus", adaptation = WhatsAppAdaptation.DIRECT)
    COMPLETE(5),

    /**
     * The rewrite aborted because of a fatal error surfaced through the
     * configurable error handler.
     *
     * <p>This value is Cobalt-only and is terminal. WhatsApp Web responds to
     * fatal failures by logging out with one of several reasons; Cobalt routes
     * the same failure through the configurable error handler and parks the
     * state here so the embedder can decide whether to retry, log out, or
     * ignore.
     *
     * @implNote
     * This implementation is the terminal sink for every fatal exception
     * thrown out of {@link LidMigrationService}; the configurable error
     * handler's verdict is applied separately and does not change the
     * {@link LidMigrationState}.
     */
    FAILED(6),

    /**
     * Migration is disabled and will not run for this session.
     *
     * <p>This value is Cobalt-only and is terminal. It is set by
     * {@link LidMigrationService#disableMigration()} when the embedder
     * explicitly opts out, or when the AB-prop gate reports the migration as
     * not enabled for this account.
     */
    DISABLED(7);

    /**
     * Protobuf wire index assigned to this state.
     */
    final int index;

    /**
     * Constructs a new state with the given protobuf index.
     *
     * @param index the protobuf wire index
     */
    LidMigrationState(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns whether this state is terminal.
     *
     * <p>{@link LidMigrationService} uses this to short-circuit further
     * transitions once the pipeline has settled.
     *
     * @return {@code true} if this state is {@link #COMPLETE},
     *         {@link #FAILED}, or {@link #DISABLED}
     */
    public boolean isTerminal() {
        return this == DISABLED || this == COMPLETE || this == FAILED;
    }
}

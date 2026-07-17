package com.github.auties00.cobalt.migration;

/**
 * Background service that re-queries inactive group metadata so that groups still on phone-number
 * addressing flip to LID addressing.
 *
 * <p>WhatsApp's group-fanout flow flips a group from PN to LID addressing as soon as a member
 * sends a message, but inactive groups stay on PN indefinitely until someone explicitly asks the
 * server for fresh metadata. This service walks the local chat store, picks every group still on
 * PN, issues a metadata query for each one, and re-checks once all queries land. Once no PN-mode
 * groups remain, the migration is marked complete through
 * {@link #setInactiveGroupLidMigrationComplete()}, which persists across restarts so a re-connecting
 * client does not re-run the sweep; callers observe completion via
 * {@link #isInactiveGroupLidMigrationComplete()}.
 *
 * <p>The service is started once per session through {@link #start()} and is stopped through
 * {@link #reset()} when the client disconnects. Callers never invoke the migration pass directly.
 *
 * @implSpec
 * Implementations must run the migration off the caller's thread, must mark completion once no
 * PN-mode groups remain, and must tolerate {@link #start()} being called after completion as a
 * no-op.
 */
public interface InactiveGroupLidMigrationService {
    /**
     * Schedules the first migration pass after the initial delay.
     *
     * <p>This method is invoked once per session by the pairing flow. Invocations made after the
     * migration has been marked complete are no-ops.
     *
     * @implSpec
     * Implementations must run the pass asynchronously and must be a no-op once the migration is
     * complete.
     */
    void start();

    /**
     * Cancels any pending scheduled task so no further passes run until {@link #start()} is called
     * again.
     *
     * <p>This method is invoked when the client disconnects so a queued retry does not fire after
     * the socket has been torn down.
     *
     * @implSpec
     * Implementations must tolerate being called when no task is scheduled.
     */
    void reset();

    /**
     * Returns whether the inactive-group LID migration has been recorded as complete for this
     * client.
     *
     * @implSpec
     * Implementations must report {@code true} once {@link #setInactiveGroupLidMigrationComplete()}
     * has been called.
     *
     * @return {@code true} if the migration has been marked complete
     */
    boolean isInactiveGroupLidMigrationComplete();

    /**
     * Marks the inactive-group LID migration as complete for this client.
     *
     * <p>Called once no PN groups remain so subsequent {@link #start()} calls short-circuit.
     *
     * @implSpec
     * Implementations must latch completion so {@link #isInactiveGroupLidMigrationComplete()}
     * returns {@code true} thereafter.
     */
    void setInactiveGroupLidMigrationComplete();
}

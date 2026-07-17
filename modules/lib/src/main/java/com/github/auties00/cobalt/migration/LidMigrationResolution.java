package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * Sealed discriminated union describing the decision the 1:1 LID migration
 * takes for a single chat thread.
 *
 * <p>An instance is produced by the per-chat resolution step in
 * {@link LiveLidMigrationService#resolveThread(com.github.auties00.cobalt.wire.linked.chat.Chat, java.util.Set)}
 * and consumed by the executor that applies the decision to the local store.
 * The three permitted variants are mutually exclusive: {@link Migrate} re-keys
 * the chat to LID, {@link Keep} leaves it untouched (because it already uses
 * LID, because its type does not participate in 1:1 migration, or because a
 * duplicate LID thread will absorb it), and {@link Delete} removes it (when no
 * LID can be resolved and the chat passes the deletability heuristics).
 * Consumers pattern-match exhaustively against the permitted subclasses.
 *
 * @implNote
 * This implementation collapses WhatsApp Web's untyped resolution return shape
 * into a sealed Java type. The thread-LID shape becomes {@link Migrate}, the
 * already-LID and not-eligible cases become {@link Keep}, the delete-chat shape
 * becomes {@link Delete}, and the logout shape is rethrown by the caller as a
 * {@link com.github.auties00.cobalt.exception.linked.WhatsAppLidMigrationException}
 * rather than being modelled here, because logout decisions live in the
 * configurable error handler instead of the per-chat resolution result.
 */
@WhatsAppWebModule(moduleName = "WAWebLid1X1ThreadAccountMigrations")
public sealed interface LidMigrationResolution
        permits LidMigrationResolution.Migrate,
                LidMigrationResolution.Keep,
                LidMigrationResolution.Delete{
    /**
     * Returns the JID the chat had before any migration rewrite was applied.
     *
     * <p>This value is available on every variant so the executor can
     * correlate the resolution with the originating store row regardless of
     * which branch fired.
     *
     * @return the original JID of the chat
     */
    Jid originalJid();

    /**
     * Resolution that re-keys a chat to LID addressing.
     *
     * <p>This variant is emitted by the resolver when a LID mapping is
     * available for the chat's original phone-number JID. When the migration
     * service executes it, the chat row's primary key is rewritten to
     * {@link #targetLid()} and the previous JID is preserved as metadata so
     * historical references continue to resolve.
     *
     * @param originalJid the phone-number JID the chat previously used
     * @param targetLid   the LID the chat is rewritten to
     */
    record Migrate(Jid originalJid, Jid targetLid) implements LidMigrationResolution {

    }

    /**
     * Resolution that leaves a chat untouched.
     *
     * <p>This variant is emitted when the chat already uses LID, when the chat
     * type is not part of 1:1 migration (group, community, newsletter,
     * broadcast, status broadcast, bot), or when a duplicate LID thread will
     * absorb the chat during the sweep. The {@link #reason()} discriminator
     * records which of the seven cases applied so the executor can keep
     * migration counters in sync with the migration's telemetry.
     *
     * @param originalJid the JID currently held by the chat
     * @param reason      the reason the chat is being kept as is
     */
    record Keep(Jid originalJid, KeepReason reason) implements LidMigrationResolution {

    }

    /**
     * Resolution that removes a chat from the store.
     *
     * <p>This variant is emitted only when no LID mapping can be resolved for a
     * 1:1 chat and the chat passes the deletability heuristics (no ephemeral
     * state, not locked, not archived, not muted, and only safe stub messages
     * or call-log entries). The {@link #reason()} discriminator records why the
     * mapping could not be resolved.
     *
     * @param originalJid the phone-number JID of the chat being removed
     * @param reason      the reason the chat is being deleted
     */
    record Delete(Jid originalJid, DeleteReason reason) implements LidMigrationResolution {

    }

    /**
     * Discriminator for the cases handled by {@link Keep}.
     *
     * <p>These constants lift the implicit branch labels of WhatsApp Web's
     * thread-resolution routine into named values so the executor's pattern
     * match is exhaustive.
     */
    enum KeepReason {
        /**
         * The chat already uses LID addressing.
         */
        ALREADY_LID,

        /**
         * The chat is a group or community and does not participate in 1:1
         * migration.
         */
        GROUP_OR_COMMUNITY,

        /**
         * The chat is a newsletter and does not participate in 1:1 migration.
         */
        NEWSLETTER,

        /**
         * The chat is a regular broadcast list and does not participate in 1:1
         * migration.
         */
        BROADCAST,

        /**
         * The chat is the dedicated status broadcast and does not participate
         * in 1:1 migration.
         */
        STATUS_BROADCAST,

        /**
         * The chat belongs to a bot account.
         */
        BOT,

        /**
         * The chat has a duplicate LID thread that will be merged into it
         * during the sweep.
         */
        DUPLICATE_WILL_MERGE
    }

    /**
     * Discriminator for the cases handled by {@link Delete}.
     *
     * <p>These constants label the no-LID-available telemetry buckets;
     * Cobalt does not send the telemetry but keeps the labels so the executor
     * can log which path triggered the deletion.
     */
    enum DeleteReason {
        /**
         * No LID mapping was found in the primary device's cache or in the
         * local store.
         */
        NO_LID_MAPPING,

        /**
         * The contact has not yet completed LID migration on their end.
         */
        CONTACT_NOT_MIGRATED,

        /**
         * A split thread was detected that would result in a duplicate after
         * migration.
         */
        SPLIT_THREAD_MISMATCH
    }
}

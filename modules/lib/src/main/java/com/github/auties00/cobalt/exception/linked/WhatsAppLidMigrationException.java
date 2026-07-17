package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Sealed root for failures during WhatsApp's migration from phone-number
 * identifiers to LID (Linked Identity) identifiers.
 *
 * WhatsApp is gradually replacing phone-number-based JIDs with LID JIDs to
 * keep numbers private from group members and other peers. Companion
 * devices learn the mapping between the two identifier kinds from the
 * primary phone, migrate every chat and the blocklist accordingly, and
 * check at every step that the resulting state is consistent. Each nested
 * subtype names one abort reason that the primary's killswitch or the
 * companion's state machine raises before bad data is committed. The
 * permits list is closed, so a {@code switch} over a
 * {@code WhatsAppLidMigrationException} can be exhaustive.
 *
 * @apiNote
 * Every subtype reports {@link WhatsAppLinkedClientErrorResult#LOG_OUT} from
 * {@link #toErrorResult()}: the device's view of contacts and groups can no
 * longer be trusted to address messages correctly, so the configured error
 * handler is expected to log the device out and drive a fresh pairing.
 *
 * @implNote
 * This implementation always classifies the failure as
 * {@link WhatsAppLinkedClientErrorResult#LOG_OUT} because the partially migrated
 * identifier mapping cannot be reconciled in place.
 *
 * @see SplitThreadMismatch
 * @see PrimaryMappingsObsolete
 * @see PeerMappingsNotReceived
 * @see StateDiscrepancy
 * @see PeerMappingsMalformed
 * @see FailedToParseMappings
 * @see NoLidAvailable
 * @see IncompatibleClient
 * @see OneOnOneThreadMigrationInternalError
 * @see BlocklistPnWhenMigrated
 * @see BlocklistChatDbUnmigrated
 */
@WhatsAppWebModule(moduleName = "WAWebLogoutReasonConstants")
public sealed abstract class WhatsAppLidMigrationException
        extends WhatsAppLinkedException
        permits WhatsAppLidMigrationException.SplitThreadMismatch,
                WhatsAppLidMigrationException.PrimaryMappingsObsolete,
                WhatsAppLidMigrationException.PeerMappingsNotReceived,
                WhatsAppLidMigrationException.StateDiscrepancy,
                WhatsAppLidMigrationException.PeerMappingsMalformed,
                WhatsAppLidMigrationException.FailedToParseMappings,
                WhatsAppLidMigrationException.NoLidAvailable,
                WhatsAppLidMigrationException.IncompatibleClient,
                WhatsAppLidMigrationException.OneOnOneThreadMigrationInternalError,
                WhatsAppLidMigrationException.BlocklistPnWhenMigrated,
                WhatsAppLidMigrationException.BlocklistChatDbUnmigrated {

    /**
     * Constructs a new LID migration exception with the specified detail message.
     *
     * @param message the detail message describing the migration failure
     */
    protected WhatsAppLidMigrationException(String message) {
        super("LID migration failed: " + message);
    }

    /**
     * Constructs a new LID migration exception with a detail message and cause.
     *
     * @param message the detail message describing the migration failure
     * @param reason  the underlying cause of the migration failure
     */
    protected WhatsAppLidMigrationException(String message, Throwable reason) {
        super("LID migration failed: " + message, reason);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#LOG_OUT}: a partially migrated
     * identifier mapping cannot be reconciled in place, so WhatsApp Web maps
     * every {@code LogoutReason.LidMigration*} value to a forced logout.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.LOG_OUT;
    }

    /**
     * Thrown when the local device's split-thread bookkeeping does not
     * match the primary phone's view.
     *
     * During the migration each conversation may be split into a
     * pre-migration thread and a post-migration thread; a mismatch between
     * the two sides means messages would land in the wrong thread, so the
     * migration is aborted.
     */
    public static final class SplitThreadMismatch extends WhatsAppLidMigrationException {
        /**
         * Constructs a new split thread mismatch exception.
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public SplitThreadMismatch() {
            super("Split thread mismatch between local and primary device");
        }
    }

    /**
     * Thrown when the LID mappings the primary phone is publishing are
     * stale relative to the rest of the account state.
     *
     * Possible triggers include a primary phone that has been offline for
     * too long, a restore from an old backup, or local corruption of the
     * migration bookkeeping on the primary side.
     */
    public static final class PrimaryMappingsObsolete extends WhatsAppLidMigrationException {
        /**
         * Constructs a new primary mappings obsolete exception.
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public PrimaryMappingsObsolete() {
            super("Primary device mappings are obsolete");
        }
    }

    /**
     * Thrown when the migration timeout fires before any peer mapping has
     * arrived from the primary phone.
     *
     * The companion device gives up rather than migrating against a mapping
     * set it never received.
     */
    public static final class PeerMappingsNotReceived extends WhatsAppLidMigrationException {
        /**
         * Constructs a new peer mappings not received exception.
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public PeerMappingsNotReceived() {
            super("Peer LID mappings were not received before the migration timeout");
        }
    }

    /**
     * Thrown when the LID migration state computed locally does not match
     * the state advertised by the primary phone.
     *
     * The drift cannot be reconciled without re-bootstrapping the companion
     * device.
     */
    public static final class StateDiscrepancy extends WhatsAppLidMigrationException {
        /**
         * Constructs a new state discrepancy exception.
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public StateDiscrepancy() {
            super("LID migration state does not match primary device");
        }
    }

    /**
     * Thrown when peer LID mapping data passes initial parsing but fails
     * the subsequent structural validation (for example, an empty mapping
     * set).
     *
     * @apiNote
     * Distinct from {@link FailedToParseMappings}, which fires when the
     * bytes themselves do not parse at all.
     */
    public static final class PeerMappingsMalformed extends WhatsAppLidMigrationException {
        /**
         * Constructs a new peer mappings malformed exception.
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public PeerMappingsMalformed() {
            super("Peer LID mappings are malformed or empty");
        }

        /**
         * Constructs a new peer mappings malformed exception with additional context.
         *
         * @param message extra detail about why the mappings are malformed
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public PeerMappingsMalformed(String message) {
            super("Peer LID mappings are malformed or empty (" + message + ")");
        }
    }

    /**
     * Thrown when the serialized LID mapping payload cannot be decoded at
     * all.
     *
     * Common causes are corruption in transit, a protobuf version skew, or
     * required fields missing from the mapping record.
     */
    public static final class FailedToParseMappings extends WhatsAppLidMigrationException {
        /**
         * Constructs a new failed to parse mappings exception with a detail message.
         *
         * @param message extra detail about the parsing failure
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public FailedToParseMappings(String message) {
            super("Failed to parse migration mappings (" + message + ")");
        }

        /**
         * Constructs a new failed to parse mappings exception with a detail message and cause.
         *
         * @param message extra detail about the parsing failure
         * @param reason  the underlying parsing exception
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public FailedToParseMappings(String message, Throwable reason) {
            super("Failed to parse migration mappings (" + message + ")", reason);
        }
    }

    /**
     * Thrown when a chat that cannot be deleted (because it carries user
     * content, is archived, muted, or locked) has no LID mapping to migrate
     * to.
     *
     * Continuing without a mapping would lose data, so the entire migration
     * is aborted.
     */
    public static final class NoLidAvailable extends WhatsAppLidMigrationException {
        /**
         * Constructs a new no LID available exception.
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public NoLidAvailable() {
            super("Non-deletable chat has no LID mapping available");
        }
    }

    /**
     * Thrown when the server-side killswitch declares this companion client
     * incompatible with the LID migration.
     *
     * The migration must not run against an unsupported client, so the
     * device is logged out instead.
     */
    public static final class IncompatibleClient extends WhatsAppLidMigrationException {
        /**
         * Constructs a new incompatible client exception.
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public IncompatibleClient() {
            super("Companion client is not compatible with LID migration (killswitch)");
        }
    }

    /**
     * Thrown when migrating a one-on-one thread fails for an unexpected
     * reason that the migration runner could not handle.
     */
    public static final class OneOnOneThreadMigrationInternalError extends WhatsAppLidMigrationException {
        /**
         * Constructs a new one-on-one thread migration internal error.
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public OneOnOneThreadMigrationInternalError() {
            super("One-on-one thread migration failed with internal error");
        }

        /**
         * Constructs a new one-on-one thread migration internal error with a cause.
         *
         * @param reason the underlying cause of the migration failure
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public OneOnOneThreadMigrationInternalError(Throwable reason) {
            super("One-on-one thread migration failed with internal error", reason);
        }
    }

    /**
     * Thrown when the blocklist still contains a legacy phone-number JID
     * after the account has migrated to LID.
     *
     * A migrated blocklist must contain only LID identifiers; the presence
     * of a phone-number entry means the local blocklist is inconsistent and
     * cannot be enforced reliably.
     *
     * @implNote
     * WhatsApp Web declares this abort reason but no producer in the JS
     * bundle actually raises it; the only reference is the classification
     * switch. Cobalt preserves the subtype for 1:1 parity with the enum.
     */
    @SuppressWarnings("unused")
    public static final class BlocklistPnWhenMigrated extends WhatsAppLidMigrationException {
        /**
         * Constructs a new blocklist phone-number-when-migrated exception.
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public BlocklistPnWhenMigrated() {
            super("Blocklist contains a phone-number JID after LID migration");
        }
    }

    /**
     * Thrown when the blocklist requires LID-aware enforcement but the
     * local chat database has not yet been migrated to LID.
     */
    public static final class BlocklistChatDbUnmigrated extends WhatsAppLidMigrationException {
        /**
         * Constructs a new blocklist chat-db-unmigrated exception.
         */
        @WhatsAppWebExport(
                moduleName = "WAWebLogoutReasonConstants",
                exports = "LogoutReason",
                adaptation = WhatsAppAdaptation.ADAPTED
        )
        public BlocklistChatDbUnmigrated() {
            super("Chat database is not migrated to LID but blocklist requires it");
        }
    }
}

package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMappingSyncPayload;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.sync.history.HistorySync;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Drives the per-account 1:1 LID migration and exposes the LID/PN conversion helpers that the rest
 * of the client depends on.
 *
 * <p>WhatsApp is migrating 1:1 conversations from phone-number JIDs to privacy-preserving
 * Linked-Identity (LID) JIDs. A paired companion client waits for the
 * {@code lid_one_on_one_migration_enabled} AB prop, receives a {@link LIDMigrationMappingSyncPayload}
 * from the primary device that describes how every known phone number maps to its assigned LID,
 * rewrites every eligible local chat to the new address, deletes chats that no longer resolve, and
 * persists the mapping table so that outgoing and incoming stanzas can be translated between
 * addressing modes.
 *
 * <p>Beyond the migration itself, this service also exposes the conversion utilities
 * ({@link #toLid(Jid)}, {@link #toPn(Jid)}, {@link #getAlternateMsgKey(MessageKey)},
 * {@link #getMeUserLidOrJidForChat(Chat, TranslateMsgKeyType)}) that the rest of the client uses to
 * move freely between PN and LID representations for messages, chats, and the current user's
 * identity.
 *
 * @implSpec
 * Implementations must drive the migration off the caller's thread, must preserve learned mappings
 * across a session {@link #reset()}, and must keep terminal migration outcomes latched so a session
 * bounce cannot reopen a concluded migration.
 */
public interface LidMigrationService {
    /**
     * Returns whether the 1:1 LID migration has completed for this account.
     *
     * <p>Consumers use this flag to decide whether outgoing messages should be addressed by LID or
     * by phone number, and as a gate inside {@link #shouldHaveAccountLid(Jid)}.
     *
     * @return {@code true} if the migration has completed
     */
    boolean isLidMigrated();

    /**
     * Returns whether the Syncd session has been migrated to LID.
     *
     * <p>Surfaces a non-LID Syncd session to modules that gate Syncd traffic on this flag.
     *
     * @return {@code false}, always
     */
    boolean isSyncdSessionMigrated();

    /**
     * Returns whether the chat-creation path should still produce a PN-addressed chat.
     *
     * <p>Chat creation always favours LID addressing once the new pipeline is live.
     *
     * @return {@code false}, always
     */
    boolean shouldCreatePnChat();

    /**
     * Returns whether the runtime state disagrees with the persisted LID migration flag.
     *
     * <p>Cobalt has no separate persisted flag and so cannot diverge from itself.
     *
     * @return {@code false}, always
     */
    boolean hasStateDiscrepancy();

    /**
     * Returns whether the primary device has already delivered the peer-mapping sync to this
     * companion.
     *
     * <p>The blocklist fetch path uses this to decide whether a LID-addressed blocklist arriving on
     * an unmigrated device can be deferred until the 1:1 LID migration completes or must be treated
     * as a hard error.
     *
     * @implSpec
     * Implementations must report {@code true} once the mapping sync has been received and the
     * migration is ready, in progress, or complete.
     *
     * @return {@code true} when the peer mappings have been received
     */
    boolean hasReceivedPeerMappings();

    /**
     * Arms the state machine to react to the AB prop flip.
     *
     * <p>Called once after the connection is established and before any protocol traffic is
     * processed. It is idempotent; subsequent calls when the state has already advanced are
     * silently ignored.
     *
     * @implSpec
     * Implementations must tolerate repeated calls.
     */
    void initialize();

    /**
     * Activates the migration when the AB prop reports it is enabled and arms the peer-mapping
     * arrival timeout.
     *
     * <p>Schedules a deferred failure that fires if the primary device never delivers the mapping
     * sync within the AB-prop-defined window. A timeout value of zero disables the scheduled task.
     *
     * @implSpec
     * Implementations must arm the peer-mapping timeout unless the configured window is zero.
     */
    void enableMigration();

    /**
     * Parks the state machine at the disabled terminal state when the server-sent AB prop indicates
     * the migration is not enabled for this account.
     *
     * <p>The companion to {@link #enableMigration()}: it prevents the timeout from being armed and
     * keeps the service inert for the lifetime of the session. It is idempotent.
     *
     * @implSpec
     * Implementations must tolerate repeated calls.
     */
    void disableMigration();

    /**
     * Ingests a mapping-sync protocol message received from the primary device, populates the
     * caches, and auto-starts {@link #executeMigration()}.
     *
     * <p>Invoked by the protocol-message receiver after the primary device's mapping payload has
     * been decoded. A {@code null} payload is treated as a malformed peer message; an empty mapping
     * list is treated as malformed. Payloads delivered outside the waiting states are silently
     * dropped.
     *
     * @implSpec
     * Implementations must reject a {@code null} or empty payload as malformed and must auto-start
     * the migration sweep once the mappings are applied.
     *
     * @param payload the decoded mapping payload from the primary device, or {@code null} when
     *                parsing failed
     */
    void processProtocolMessage(LIDMigrationMappingSyncPayload payload);

    /**
     * Absorbs a candidate chat-DB migration timestamp, keeping the newest value seen across all
     * sources.
     *
     * <p>The recorded value feeds the staleness comparison that decides whether the primary's
     * mappings are obsolete compared to a local chat's last activity. A {@code null} input is a
     * tolerated no-op.
     *
     * @implSpec
     * Implementations must keep only the newest observed timestamp and must treat {@code null} as a
     * no-op.
     *
     * @param timestamp the observed timestamp, or {@code null} to leave the recorded value untouched
     */
    void observeChatDbMigrationTimestamp(Instant timestamp);

    /**
     * Harvests LID mappings, conversation pairings, and an optional chat-DB timestamp from a
     * {@link HistorySync} payload.
     *
     * <p>Invoked by the history-sync ingestion path so that mappings already present in the primary
     * device's database are reflected locally even before the primary delivers a dedicated
     * mapping-sync protocol message. A {@code null} payload is a tolerated no-op.
     *
     * @implSpec
     * Implementations must write only to the durable mapping table and contact LID, leaving the
     * primary-cache reserved for the mapping-sync protocol message.
     *
     * @param historySync the decoded {@link HistorySync}, or {@code null} for a tolerated no-op
     */
    void processHistorySync(HistorySync historySync);

    /**
     * Runs the migration sweep over the local chat store, rewriting eligible PN chats to LID,
     * deleting chats that no longer resolve, and marking the migration complete.
     *
     * <p>Auto-started from {@link #processProtocolMessage(LIDMigrationMappingSyncPayload)} once the
     * mappings are ready; it may also be invoked directly. Any failure is surfaced through the
     * configurable error handler.
     *
     * @implSpec
     * Implementations must run at most once per ready state and must promote the in-memory caches to
     * the durable mapping table on completion.
     */
    void executeMigration();

    /**
     * Applies a LID-change notification for an existing contact by updating the caches, the durable
     * mapping, the contact, and any chat keyed by the phone number.
     *
     * <p>Called when the server, the primary device, or a contact roster sync reports that a
     * contact's LID has rotated. A {@code null} {@code phoneJid} or {@code newLid} is a tolerated
     * no-op; {@code oldLid} is accepted only for logging.
     *
     * @param phoneJid the phone-number JID whose LID is rotating
     * @param newLid   the new LID
     * @param oldLid   the previous LID, or {@code null} when unknown
     */
    void changeLid(Jid phoneJid, Jid newLid, Jid oldLid);

    /**
     * Records the LID known at chat-creation time so it can serve as a last-resort fallback during
     * migration.
     *
     * <p>Consulted only when neither the primary cache nor the durable mapping has a LID for the
     * contact. A {@code null} argument is a tolerated no-op.
     *
     * @param phoneJid the phone-number JID of the chat
     * @param lid      the LID known at chat-creation time
     */
    void registerOriginalLid(Jid phoneJid, Jid lid);

    /**
     * Rewinds the state machine for a new session, preserving primary caches and terminal states.
     *
     * <p>Called from the client's reconnect handler so the next session can re-run
     * {@link #initialize()} without losing the learned mappings. Terminal states are deliberately
     * preserved so a session bounce cannot reopen a concluded migration.
     *
     * @implSpec
     * Implementations must cancel any armed timeout and must not rewind a terminal state.
     */
    void reset();

    /**
     * Returns the LID associated with the given phone-number JID, preferring the in-memory primary
     * cache before consulting the store.
     *
     * <p>A {@code null} input or a JID without a user part yields an empty {@link Optional}.
     *
     * @param phoneJid the phone-number JID to resolve
     * @return the LID for the JID, or {@link Optional#empty()} when none is known
     */
    Optional<Jid> lookupLid(Jid phoneJid);

    /**
     * Returns whether outgoing messages to the given recipient should be addressed using LID rather
     * than phone number.
     *
     * <p>The decision activates from the in-progress state onwards, not only at completion, so
     * messages sent while the migration is sweeping chats still land on the new addressing mode.
     *
     * @implSpec
     * Implementations must accept LID-server JIDs unconditionally, reject group, community,
     * newsletter, and broadcast servers, and otherwise gate on the migration state plus a positive
     * {@link #lookupLid(Jid)}.
     *
     * @param recipientJid the recipient JID, or {@code null}
     * @return {@code true} when LID addressing should be used
     */
    boolean shouldUseLidAddressing(Jid recipientJid);

    /**
     * Returns whether the given JID is eligible to carry an {@code account_lid} attribute on
     * outgoing stanzas.
     *
     * <p>Only regular users (see {@link #isRegularUser(Jid)}) may carry the attribute, and only
     * after the 1:1 migration has fully completed.
     *
     * @param jid the JID to evaluate, or {@code null}
     * @return {@code true} when the JID should carry an account LID
     */
    boolean shouldHaveAccountLid(Jid jid);

    /**
     * Returns the phone-number JID corresponding to the given JID, or the input itself when it is
     * already a PN.
     *
     * <p>The standard LID to PN converter. Returns {@code null} when the JID is a LID with no known
     * mapping, signalling that the conversion is impossible in the current state.
     *
     * @param jid the JID to convert, or {@code null}
     * @return the PN form, or {@code null} when the JID is a LID with no mapping (or the input is
     *         {@code null})
     */
    Jid toPn(Jid jid);

    /**
     * Returns the LID JID corresponding to the given JID, or the input itself when it is already a
     * LID.
     *
     * <p>The standard PN to LID converter. It strips device and agent data from the input before
     * the lookup.
     *
     * @param jid the JID to convert, or {@code null}
     * @return the LID form, or {@code null} when the JID is a PN with no mapping (or the input is
     *         {@code null})
     */
    Jid toLid(Jid jid);

    /**
     * Returns the user-level LID corresponding to the given JID, with device and agent data
     * stripped.
     *
     * @param jid the JID to convert, or {@code null}
     * @return the user LID, or {@code null} when no mapping is known
     */
    Jid toUserLid(Jid jid);

    /**
     * Returns the user-level LID for the given JID or throws when none can be resolved.
     *
     * <p>The non-nullable companion to {@link #toUserLid(Jid)} for call sites where a missing
     * mapping is a programming error.
     *
     * @param jid the JID to convert
     * @return the user LID, never {@code null}
     * @throws IllegalStateException when no LID mapping exists for the JID
     */
    Jid toUserLidOrThrow(Jid jid);

    /**
     * Returns the phone-number JID for the given JID or throws when none can be resolved.
     *
     * <p>The non-nullable companion to {@link #toPn(Jid)} for call sites where the LID must resolve
     * to a PN.
     *
     * @param jid the JID to convert
     * @return the phone-number JID, never {@code null}
     * @throws IllegalStateException when no PN mapping exists for the JID
     */
    Jid toPnOrThrow(Jid jid);

    /**
     * Returns a {@link Function} that converts JIDs to the requested addressing mode.
     *
     * <p>Lets callers pick the converter once and apply it across a collection of JIDs without
     * branching on the mode at every element.
     *
     * @param isLid {@code true} to obtain {@link #toLid(Jid)}, {@code false} to obtain
     *              {@link #toPn(Jid)}
     * @return a {@link Function} reference to the chosen converter
     */
    Function<Jid, Jid> toAddressingModeFactory(boolean isLid);

    /**
     * Normalises two JIDs to the same addressing mode by converting one side when they are user
     * wids on different server families.
     *
     * <p>The input pair is returned unchanged when neither side has a known alternate.
     *
     * @implSpec
     * Implementations must preserve the input order and must pass through non-user JIDs and JIDs
     * already sharing an addressing mode.
     *
     * @param first  the first JID, or {@code null}
     * @param second the second JID, or {@code null}
     * @return a two-element array with the (possibly converted) pair, preserving the input order
     */
    Jid[] toCommonAddressingMode(Jid first, Jid second);

    /**
     * Returns the addressing-mode mirror of the given {@link MessageKey}, swapping the participant
     * or remote JID into the opposite mode.
     *
     * <p>Used to reconcile two stored copies of the same message (one PN-keyed, one LID-keyed) so
     * receipts, edits, and reactions land against both. Returns {@code null} when no alternate is
     * resolvable.
     *
     * @param msgKey the message key, or {@code null}
     * @return the alternate {@link MessageKey}, or {@code null} when no alternate can be built
     */
    MessageKey getAlternateMsgKey(MessageKey msgKey);

    /**
     * Returns the current user's identity, in the addressing mode appropriate for the given chat and
     * translate type, that should appear as the participant of an outgoing message key.
     *
     * <p>The decision is driven by whether the chat is on the LID server, whether it is a group,
     * whether the group is a Community Announcement Group (default subgroup), whether the group's
     * {@link GroupMetadata} reports LID addressing mode, and the {@link TranslateMsgKeyType}.
     *
     * @param chat          the chat composing the outgoing message
     * @param translateType the message-key category that picks the addressing mode
     * @return the current user's JID in the chosen addressing mode
     * @throws IllegalStateException when the store has no JID configured for the chosen addressing
     *         mode
     */
    Jid getMeUserLidOrJidForChat(Chat chat, TranslateMsgKeyType translateType);

    /**
     * Returns both addressing-mode JIDs for the given JID, with the input itself first followed by
     * its alternate when known.
     *
     * <p>Lets callers iterate over both addressing modes in a single loop. A single-element list is
     * returned when no alternate is known and an empty list when the input is {@code null}.
     *
     * @param jid the JID whose addressing-mode pair is requested, or {@code null}
     * @return a list of one or two JIDs, with the input first
     */
    List<Jid> getPnAndLidToUpdate(Jid jid);

    /**
     * Returns whether the given chat uses LID addressing mode.
     *
     * <p>A 1:1 LID-server chat always uses LID addressing; a group uses LID addressing when its
     * {@link GroupMetadata#isLidAddressingMode()} is {@code true}. Other server families return
     * {@code false}.
     *
     * @param chat the chat to inspect, or {@code null}
     * @return {@code true} when the chat uses LID addressing
     */
    boolean chatIsLid(Chat chat);

    /**
     * Returns whether the given JID represents a regular user that is eligible for LID addressing.
     *
     * <p>A regular user lives on the user, LID, bot, hosted, or hosted-LID server, is not the PSA
     * announcements account, and is not a bot. This is used by {@link #shouldHaveAccountLid(Jid)}
     * and by external code paths that need the same eligibility check.
     *
     * @param jid the JID to inspect
     * @return {@code true} when the JID is a regular user
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isRegularUser",
            adaptation = WhatsAppAdaptation.DIRECT)
    static boolean isRegularUser(Jid jid) {
        if (!jid.hasUserServer() && !jid.hasLidServer() && !jid.hasBotServer()
                && !jid.hasHostedServer() && !jid.hasHostedLidServer()) {
            return false;
        }

        if (jid.equals(Jid.announcementsAccount())) {
            return false;
        }

        if (jid.isBot()) {
            return false;
        }

        return true;
    }

    /**
     * Categorises a message-key composition by addressing-mode sensitivity.
     *
     * <p>This enum is consumed by {@link #getMeUserLidOrJidForChat(Chat, TranslateMsgKeyType)} to
     * pick between the LID and PN form of the current user. Message addons (reactions, receipts) and
     * regular or edited messages follow slightly different rules in Community Announcement Groups,
     * which is why both categories exist.
     */
    @WhatsAppWebModule(moduleName = "WAWebMsgKeyUtils")
    enum TranslateMsgKeyType {
        /**
         * Identifies an outgoing message addon such as a reaction or a receipt.
         *
         * <p>In Community Announcement Groups this branch always selects LID for the current user
         * even when the group is on PN addressing.
         */
        @WhatsAppWebExport(moduleName = "WAWebMsgKeyUtils", exports = "TranslateMsgKeyType",
                adaptation = WhatsAppAdaptation.DIRECT)
        ADDON,

        /**
         * Identifies an outgoing regular message.
         *
         * <p>This drives a Community-Announcement-Group-specific PN selection branch in
         * {@link #getMeUserLidOrJidForChat(Chat, TranslateMsgKeyType)}; for non-CAG chats this is
         * equivalent to {@link #EDIT_MESSAGE}.
         */
        @WhatsAppWebExport(moduleName = "WAWebMsgKeyUtils", exports = "TranslateMsgKeyType",
                adaptation = WhatsAppAdaptation.DIRECT)
        MESSAGE,

        /**
         * Identifies an outgoing message edit.
         *
         * <p>This follows the same rules as {@link #MESSAGE} in
         * {@link #getMeUserLidOrJidForChat(Chat, TranslateMsgKeyType)}; the distinct constant exists
         * to mirror WhatsApp Web's three-value enum and to make call sites self-documenting.
         */
        @WhatsAppWebExport(moduleName = "WAWebMsgKeyUtils", exports = "TranslateMsgKeyType",
                adaptation = WhatsAppAdaptation.DIRECT)
        EDIT_MESSAGE
    }
}

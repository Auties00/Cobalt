package com.github.auties00.cobalt.wire.linked.sync.history;

import com.github.auties00.cobalt.wire.linked.call.CallLog;
import com.github.auties00.cobalt.wire.linked.chat.*;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipant;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupPastParticipants;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.jid.migration.PhoneNumberToLIDMapping;
import com.github.auties00.cobalt.wire.linked.media.MediaVisibility;
import com.github.auties00.cobalt.wire.linked.media.StickerMetadata;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.PrivacySystemMessage;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncType;
import com.github.auties00.cobalt.wire.linked.setting.GlobalSettings;
import com.github.auties00.cobalt.wire.linked.setting.WallpaperSettings;
import com.github.auties00.collections.ConcurrentLinkedHashMap;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Chunk of account history transferred from the primary device to a companion
 * during onboarding or on-demand history fetches.
 *
 * <p>A history sync payload carries everything a freshly-linked companion
 * needs to render the user's account: chats and their messages, push-names,
 * global settings, recent stickers, past group participants, call logs, AI
 * wait-list state, phone-number to LID mappings, companion-meta nonce, the
 * shareable chat identifier encryption key, and linked account descriptors.
 * Payloads come in two shapes: the full variant includes chats and status
 * messages ({@link Full}), while the light variant ({@link Light}) shares
 * the metadata-only subset used for incremental syncs.
 *
 * <p>Instances are produced by decoding a {@code HistorySyncNotification}
 * media blob and are then fanned out to the corresponding store collections.
 */
@ProtobufMessage(name = "HistorySync")
public abstract sealed class HistorySync {
    /**
     * Kind of history transfer this payload represents.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    HistorySyncType syncType;

    /**
     * Zero-based index of this chunk inside the transfer sequence.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    Integer chunkOrder;

    /**
     * Progress percentage (0-100) reported for this transfer.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    Integer progress;

    /**
     * Push-name records reported by the primary device.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    List<Pushname> pushnames;

    /**
     * Snapshot of account-wide user preferences (wallpapers,
     * auto-download policies, notification preferences, font and quality
     * settings, default disappearing-mode duration, security-notifications
     * flag, chat-lock settings, and related toggles) emitted with the
     * initial bootstrap chunk.
     *
     * <p>Decoded for protobuf parity only. WhatsApp Web does not read
     * this field from the history-sync chunk: the authoritative sources
     * for these preferences are the app-state (syncd) collections
     * instead. The {@code chatLockSettings} sub-message lands via
     * {@code WAWebChatLockSettingsSync}, the default disappearing-mode
     * duration and security-notifications flag arrive through separate
     * syncd / user-prefs flows, and no handler ever dereferences
     * {@code historySync.globalSettings}. Cobalt mirrors that behaviour
     * and exposes the field only so embedders can inspect the snapshot
     * for diagnostics.
     *
     * <p>Wire field {@code globalSettings} on the WhatsApp Web
     * history-sync proto.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    GlobalSettings globalSettings;

    /**
     * Per-account HMAC seed for WhatsApp's Chat Thread Logging
     * telemetry pipeline.
     *
     * <p>WhatsApp Web feeds this secret into the metadata store backing
     * {@code WAWebChatThreadLogging}, where it is used to HMAC every
     * chat identifier before the identifier is attached to an outgoing
     * Chat Thread Logging analytics event. Routing the JID through an
     * HMAC keyed by an account-private secret keeps the analytics
     * pipeline able to correlate activity within one account without
     * exposing the plaintext JID to the logging backend.
     *
     * <p>Wire field {@code threadIdUserSecret} on the WhatsApp Web
     * history-sync proto.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BYTES)
    byte[] chatThreadLoggingSecret;

    /**
     * Disappearing-mode timeframe offset for WhatsApp's Chat Thread
     * Logging telemetry pipeline.
     *
     * <p>WhatsApp Web feeds this offset into the same metadata store
     * that holds {@link #chatThreadLoggingSecret}, where the Chat
     * Thread Logging pipeline uses it to bucket per-thread activity
     * into the same disappearing-mode time windows the user sees in
     * their disappearing-messages settings, so that the analytics
     * events line up with the user's privacy window.
     *
     * <p>Wire field {@code threadDsTimeframeOffset} on the WhatsApp Web
     * history-sync proto.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.UINT32)
    Integer chatThreadLoggingDisappearingOffset;

    /**
     * Recently used stickers transferred for the user.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.MESSAGE)
    List<StickerMetadata> recentStickers;

    /**
     * Participants who have left groups that the user is still a member of.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.MESSAGE)
    List<GroupPastParticipants> pastParticipants;

    /**
     * Call-log records transferred for the user.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
    List<CallLog> callLogs;

    /**
     * Vestigial wait-list state for Meta AI bot access.
     *
     * <p>The field is still emitted on the wire but has no runtime
     * consumer on WhatsApp Web: the bundle exports the enum class
     * ({@code l.HistorySync$BotAIWaitListState}) but no handler reads
     * the value. Access to Meta AI is decided today by server-driven
     * AB props ({@code isAskMetaAiEnabled},
     * {@code isMetaAIChatInteractionEnabled} in {@code WAWebBotGating}),
     * not by a history-sync flag, and this field is left over from the
     * early Meta-AI access-gating phase. Decoded for protobuf parity
     * only; no consumer should depend on its value.
     *
     * <p>Wire field {@code aiWaitListState} on the WhatsApp Web
     * history-sync proto.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.ENUM)
    BotAIWaitListState aiWaitListState;

    /**
     * Phone-number to LID migration records.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    List<PhoneNumberToLIDMapping> phoneNumberToLidMappings;

    /**
     * Companion-issued nonce that authenticates this companion device
     * against WhatsApp's MMS (Media Management Service) when asking it
     * to release the history-sync blob from the CDN.
     *
     * <p>WhatsApp Web stores the nonce after the initial bootstrap and
     * passes it back as the {@code companionUserSecret} parameter to
     * {@code WAWebMmsClient.deleteMdHistorySyncBlob} the first time a
     * history-sync chunk has been applied; the server uses it as proof
     * that the same companion that received the blob is the one asking
     * to delete it.
     *
     * <p>Wire field {@code companionMetaNonce} on the WhatsApp Web
     * history-sync proto.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.STRING)
    String companionMmsAuthNonce;

    /**
     * Per-account key that protects the opaque chat identifier
     * embedded in WhatsApp's shareable-chat links (the
     * {@code wa.me/} and QR deep-link surface).
     *
     * <p>Decoded and persisted on the client purely for completeness.
     * The deep-link generator runs entirely server-side: there is no
     * client-side consumer on WhatsApp Web or on the Windows desktop
     * bundle (the latter shares the Web bundle), so neither WA Web nor
     * Cobalt ever invokes any local encryption routine with this key.
     * The client receives it, retains it, and never reads it back.
     *
     * <p>Wire field {@code shareableChatIdentifierEncryptionKey} on the
     * WhatsApp Web history-sync proto.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.BYTES)
    byte[] shareableChatLinkKey;

    /**
     * Linked account descriptors carried for users who have linked
     * multiple WhatsApp accounts (the multi-account / "sister accounts"
     * surface).
     *
     * <p>Decoded for protobuf parity only. Cobalt is a single-account
     * client today; the list is preserved on this payload so embedders
     * may inspect it, but no part of the stack projects it into a
     * dedicated store collection or fires a listener event. If Cobalt
     * grows multi-account support, the right place to land these is on
     * the store, not here.
     *
     * <p>Wire field {@code accounts} on the WhatsApp Web history-sync
     * proto.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.MESSAGE)
    List<Account> accounts;


    /**
     * Constructs the shared state carried by every history-sync variant.
     *
     * @param syncType the history sync kind
     * @param chunkOrder the chunk index within the transfer
     * @param progress the transfer progress percentage
     * @param pushnames the transferred push-names
     * @param globalSettings the transferred global settings
     * @param chatThreadLoggingSecret the Chat Thread Logging HMAC seed
     * @param chatThreadLoggingDisappearingOffset the Chat Thread Logging
     *                                            disappearing-mode timeframe offset
     * @param recentStickers the recently used stickers
     * @param pastParticipants the past-participants records
     * @param callLogs the call-log records
     * @param aiWaitListState the AI wait-list state
     * @param phoneNumberToLidMappings the phone-number to LID migration records
     * @param companionMmsAuthNonce the MMS authorization nonce
     * @param shareableChatLinkKey the shareable-chat-link encryption key
     * @param accounts the linked account descriptors
     */
    HistorySync(HistorySyncType syncType, Integer chunkOrder, Integer progress, List<Pushname> pushnames, GlobalSettings globalSettings, byte[] chatThreadLoggingSecret, Integer chatThreadLoggingDisappearingOffset, List<StickerMetadata> recentStickers, List<GroupPastParticipants> pastParticipants, List<CallLog> callLogs, BotAIWaitListState aiWaitListState, List<PhoneNumberToLIDMapping> phoneNumberToLidMappings, String companionMmsAuthNonce, byte[] shareableChatLinkKey, List<Account> accounts) {
        this.syncType = Objects.requireNonNull(syncType);
        this.chunkOrder = chunkOrder;
        this.progress = progress;
        this.pushnames = pushnames;
        this.globalSettings = globalSettings;
        this.chatThreadLoggingSecret = chatThreadLoggingSecret;
        this.chatThreadLoggingDisappearingOffset = chatThreadLoggingDisappearingOffset;
        this.recentStickers = recentStickers;
        this.pastParticipants = pastParticipants;
        this.callLogs = callLogs;
        this.aiWaitListState = aiWaitListState;
        this.phoneNumberToLidMappings = phoneNumberToLidMappings;
        this.companionMmsAuthNonce = companionMmsAuthNonce;
        this.shareableChatLinkKey = shareableChatLinkKey;
        this.accounts = accounts;
    }

    /**
     * Decodes a full history-sync payload (chats and status messages included)
     * from the supplied protobuf stream.
     *
     * @param stream the protobuf input stream to decode from
     * @return the decoded full history-sync payload
     */
    public static HistorySync ofFull(ProtobufInputStream stream) {
        return HistorySyncFullSpec.decode(stream);
    }

    /**
     * Decodes a lightweight history-sync payload (metadata only) from the
     * supplied protobuf stream.
     *
     * @param stream the protobuf input stream to decode from
     * @return the decoded light history-sync payload
     */
    public static HistorySync ofLight(ProtobufInputStream stream) {
        return HistorySyncLightSpec.decode(stream);
    }

    /**
     * Returns the kind of history transfer represented by this payload.
     *
     * @return the sync type
     */
    public HistorySyncType syncType() {
        return syncType;
    }

    /**
     * Returns the chats carried by this payload.
     *
     * <p>Light variants always return an empty list; full variants return the
     * transferred chats with their embedded message maps.
     *
     * @return the list of chats, never {@code null}
     */
    public abstract List<? extends Chat> chats();

    /**
     * Returns the status (status-v3) messages carried by this payload.
     *
     * <p>Light variants always return an empty list; full variants return the
     * transferred status messages.
     *
     * @return the list of status messages, never {@code null}
     */
    public abstract List<ChatMessageInfo> statusV3Messages();

    /**
     * Returns the chunk index inside the transfer sequence.
     *
     * @return the chunk order, or empty if absent
     */
    public OptionalInt chunkOrder() {
        return chunkOrder == null ? OptionalInt.empty() : OptionalInt.of(chunkOrder);
    }

    /**
     * Returns the transfer progress percentage.
     *
     * @return the progress value (0-100), or empty if absent
     */
    public OptionalInt progress() {
        return progress == null ? OptionalInt.empty() : OptionalInt.of(progress);
    }

    /**
     * Returns the push-name records transferred with this chunk.
     *
     * @return an unmodifiable list of push-names, never {@code null}
     */
    public List<Pushname> pushnames() {
        return pushnames == null ? List.of() : Collections.unmodifiableList(pushnames);
    }

    /**
     * Returns the global account settings transferred with this chunk.
     *
     * @return the global settings, or empty if absent
     */
    public Optional<GlobalSettings> globalSettings() {
        return Optional.ofNullable(globalSettings);
    }

    /**
     * Returns the HMAC seed feeding WhatsApp's Chat Thread Logging
     * telemetry pipeline.
     *
     * @return the secret bytes, or empty if absent
     */
    public Optional<byte[]> chatThreadLoggingSecret() {
        return Optional.ofNullable(chatThreadLoggingSecret);
    }

    /**
     * Returns the disappearing-mode timeframe offset feeding WhatsApp's
     * Chat Thread Logging telemetry pipeline.
     *
     * @return the offset, or empty if absent
     */
    public OptionalInt chatThreadLoggingDisappearingOffset() {
        return chatThreadLoggingDisappearingOffset == null ? OptionalInt.empty() : OptionalInt.of(chatThreadLoggingDisappearingOffset);
    }

    /**
     * Returns the recently used stickers transferred with this chunk.
     *
     * @return an unmodifiable list of sticker metadata, never {@code null}
     */
    public List<StickerMetadata> recentStickers() {
        return recentStickers == null ? List.of() : Collections.unmodifiableList(recentStickers);
    }

    /**
     * Returns the past-participants records transferred with this chunk.
     *
     * @return an unmodifiable list of past-participants entries, never {@code null}
     */
    public List<GroupPastParticipants> pastParticipants() {
        return pastParticipants == null ? List.of() : Collections.unmodifiableList(pastParticipants);
    }

    /**
     * Returns the call-log records transferred with this chunk.
     *
     * @return an unmodifiable list of call logs, never {@code null}
     */
    public List<CallLog> callLogRecords() {
        return callLogs == null ? List.of() : Collections.unmodifiableList(callLogs);
    }

    /**
     * Returns the vestigial Meta-AI wait-list state. Decoded for
     * protobuf parity only; Meta AI access is gated by server-driven
     * AB props rather than by this flag, and no current handler reads
     * the value.
     *
     * @return the wait-list state, or empty if absent
     */
    public Optional<BotAIWaitListState> aiWaitListState() {
        return Optional.ofNullable(aiWaitListState);
    }

    /**
     * Returns the phone-number to LID migration records transferred with this
     * chunk.
     *
     * @return an unmodifiable list of mappings, never {@code null}
     */
    public List<PhoneNumberToLIDMapping> phoneNumberToLidMappings() {
        return phoneNumberToLidMappings == null ? List.of() : Collections.unmodifiableList(phoneNumberToLidMappings);
    }

    /**
     * Returns the nonce that authenticates this companion against
     * WhatsApp's MMS when releasing the just-applied history-sync blob.
     *
     * @return the nonce, or empty if absent
     */
    public Optional<String> companionMmsAuthNonce() {
        return Optional.ofNullable(companionMmsAuthNonce);
    }

    /**
     * Returns the per-account key that protects the opaque chat
     * identifier embedded in WhatsApp's shareable-chat links.
     *
     * @return the key bytes, or empty if absent
     */
    public Optional<byte[]> shareableChatLinkKey() {
        return Optional.ofNullable(shareableChatLinkKey);
    }

    /**
     * Returns the linked account descriptors transferred with this chunk.
     *
     * @return an unmodifiable list of account descriptors, never {@code null}
     */
    public List<Account> accounts() {
        return accounts == null ? List.of() : Collections.unmodifiableList(accounts);
    }

    /**
     * Sets the sync type.
     *
     * @param syncType the sync type
     */
    public void setSyncType(HistorySyncType syncType) {
        this.syncType = syncType;
    }

    /**
     * Sets the chunk index inside the transfer sequence.
     *
     * @param chunkOrder the chunk order
     */
    public void setChunkOrder(Integer chunkOrder) {
        this.chunkOrder = chunkOrder;
    }

    /**
     * Sets the transfer progress percentage.
     *
     * @param progress the progress value
     */
    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    /**
     * Sets the push-name records.
     *
     * @param pushnames the push-names
     */
    public void setPushnames(List<Pushname> pushnames) {
        this.pushnames = pushnames;
    }

    /**
     * Sets the global account settings.
     *
     * @param globalSettings the settings
     */
    public void setGlobalSettings(GlobalSettings globalSettings) {
        this.globalSettings = globalSettings;
    }

    /**
     * Sets the Chat Thread Logging HMAC seed.
     *
     * @param chatThreadLoggingSecret the secret bytes
     */
    public void setChatThreadLoggingSecret(byte[] chatThreadLoggingSecret) {
        this.chatThreadLoggingSecret = chatThreadLoggingSecret;
    }

    /**
     * Sets the Chat Thread Logging disappearing-mode timeframe offset.
     *
     * @param chatThreadLoggingDisappearingOffset the offset
     */
    public void setChatThreadLoggingDisappearingOffset(Integer chatThreadLoggingDisappearingOffset) {
        this.chatThreadLoggingDisappearingOffset = chatThreadLoggingDisappearingOffset;
    }

    /**
     * Sets the recently used stickers.
     *
     * @param recentStickers the stickers
     */
    public void setRecentStickers(List<StickerMetadata> recentStickers) {
        this.recentStickers = recentStickers;
    }

    /**
     * Sets the past-participants records.
     *
     * @param pastParticipants the past participants
     */
    public void setPastParticipants(List<GroupPastParticipants> pastParticipants) {
        this.pastParticipants = pastParticipants;
    }

    /**
     * Sets the call-log records.
     *
     * @param callLogs the call logs
     */
    public void setCallLogRecords(List<CallLog> callLogs) {
        this.callLogs = callLogs;
    }

    /**
     * Sets the AI wait-list state.
     *
     * @param aiWaitListState the state
     */
    public void setAiWaitListState(BotAIWaitListState aiWaitListState) {
        this.aiWaitListState = aiWaitListState;
    }

    /**
     * Sets the phone-number to LID migration records.
     *
     * @param phoneNumberToLidMappings the mappings
     */
    public void setPhoneNumberToLidMappings(List<PhoneNumberToLIDMapping> phoneNumberToLidMappings) {
        this.phoneNumberToLidMappings = phoneNumberToLidMappings;
    }

    /**
     * Sets the MMS authorization nonce.
     *
     * @param companionMmsAuthNonce the nonce
     */
    public void setCompanionMmsAuthNonce(String companionMmsAuthNonce) {
        this.companionMmsAuthNonce = companionMmsAuthNonce;
    }

    /**
     * Sets the shareable-chat-link encryption key.
     *
     * @param shareableChatLinkKey the key bytes
     */
    public void setShareableChatLinkKey(byte[] shareableChatLinkKey) {
        this.shareableChatLinkKey = shareableChatLinkKey;
    }

    /**
     * Sets the linked account descriptors.
     *
     * @param accounts the accounts
     */
    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    /**
     * Serialises this payload to the supplied protobuf output stream,
     * choosing the encoding matching the concrete variant.
     *
     * @param out the output stream to write to
     * @throws IOException if writing fails
     */
    public abstract void writeTo(ProtobufOutputStream<?> out) throws IOException;

    /**
     * Full history-sync variant: carries chats and their messages in addition
     * to the shared metadata.
     */
    @ProtobufMessage
    static final class Full extends HistorySync {
        /**
         * Chats transferred in this chunk, complete with embedded messages.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        List<Chat> chats;

        /**
         * Status (status-v3) messages transferred in this chunk.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        List<ChatMessageInfo> statusV3Messages;

        /**
         * Constructs a full history-sync payload.
         *
         * @param syncType the sync type
         * @param chunkOrder the chunk index
         * @param progress the progress value
         * @param pushnames the push-names
         * @param globalSettings the global settings
         * @param chatThreadLoggingSecret the Chat Thread Logging HMAC seed
         * @param chatThreadLoggingDisappearingOffset the Chat Thread Logging
         *                                            timeframe offset
         * @param recentStickers the recent stickers
         * @param pastParticipants the past participants
         * @param callLogs the call logs
         * @param aiWaitListState the AI wait-list state
         * @param phoneNumberToLidMappings the PN to LID mappings
         * @param companionMmsAuthNonce the MMS authorization nonce
         * @param shareableChatLinkKey the shareable-chat-link encryption key
         * @param accounts the linked accounts
         * @param chats the transferred chats
         * @param statusV3Messages the transferred status messages
         */
        Full(HistorySyncType syncType, Integer chunkOrder, Integer progress, List<Pushname> pushnames, GlobalSettings globalSettings, byte[] chatThreadLoggingSecret, Integer chatThreadLoggingDisappearingOffset, List<StickerMetadata> recentStickers, List<GroupPastParticipants> pastParticipants, List<CallLog> callLogs, BotAIWaitListState aiWaitListState, List<PhoneNumberToLIDMapping> phoneNumberToLidMappings, String companionMmsAuthNonce, byte[] shareableChatLinkKey, List<Account> accounts, List<Chat> chats, List<ChatMessageInfo> statusV3Messages) {
            super(syncType, chunkOrder, progress, pushnames, globalSettings, chatThreadLoggingSecret, chatThreadLoggingDisappearingOffset, recentStickers, pastParticipants, callLogs, aiWaitListState, phoneNumberToLidMappings, companionMmsAuthNonce, shareableChatLinkKey, accounts);
            this.chats = chats;
            this.statusV3Messages = statusV3Messages;
        }

        /**
         * Returns the chats transferred in this chunk.
         *
         * @return an unmodifiable list of chats, never {@code null}
         */
        @Override
        public List<Chat> chats() {
            return chats == null ? List.of() : Collections.unmodifiableList(chats);
        }

        /**
         * Returns the status (status-v3) messages transferred in this chunk.
         *
         * @return an unmodifiable list of status messages, never {@code null}
         */
        @Override
        public List<ChatMessageInfo> statusV3Messages() {
            return statusV3Messages == null ? List.of() : Collections.unmodifiableList(statusV3Messages);
        }

        /**
         * Serialises this full payload using the corresponding protobuf spec.
         *
         * @param out the output stream to write to
         */
        @Override
        public void writeTo(ProtobufOutputStream<?> out) {
            HistorySyncFullSpec.encode(this, out);
        }

        /**
         * History-sync representation of a chat.
         *
         * <p>Extends the core {@link com.github.auties00.cobalt.wire.linked.chat.Chat}
         * with a map of messages keyed by message id and wrapped inside
         * {@link HistorySyncMsg} envelopes so that the transfer preserves the
         * monotonic order imposed by {@code msgOrderId}. The chat exposes the
         * message collection through the usual chat API by projecting the map
         * values on demand.
         */
        @ProtobufMessage
        static final class Chat extends com.github.auties00.cobalt.wire.linked.chat.Chat {
            /**
             * Messages keyed by id, ordered by insertion to preserve history
             * ordering.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
            final Messages<HistorySyncMsg> messages;

            /**
             * Constructs a history-sync chat with the inherited chat fields
             * plus the message map.
             *
             * @param jid the chat JID
             * @param newJid the migrated-to JID, if any
             * @param oldJid the previous JID, if any
             * @param lastMsgTimestamp the timestamp of the last known message
             * @param unreadCount the unread-message counter
             * @param readOnly whether the chat is read-only
             * @param endOfHistoryTransfer whether this is the end-of-history marker
             * @param ephemeralExpiration the ephemeral timer
             * @param ephemeralSettingTimestamp the ephemeral setting timestamp
             * @param endOfHistoryTransferType the end-of-history transfer type
             * @param conversationTimestamp the conversation activity timestamp
             * @param name the chat name
             * @param pHash the peer hash
             * @param notSpam whether the chat was marked not-spam
             * @param archived whether the chat is archived
             * @param disappearingMode the disappearing-mode setting
             * @param unreadMentionCount the number of unread mentions
             * @param markedAsUnread whether the chat was manually marked unread
             * @param participant the group participants
             * @param tcToken the trusted-contact token
             * @param tcTokenTimestamp the trusted-contact token timestamp
             * @param contactPrimaryIdentityKey the contact primary identity key
             * @param pinnedTimestamp the pin timestamp
             * @param mute the mute configuration
             * @param wallpaper the wallpaper settings
             * @param mediaVisibility the media visibility setting
             * @param tcTokenSenderTimestamp the sender-side TC token timestamp
             * @param suspended whether the chat is suspended
             * @param terminated whether the chat is terminated
             * @param createdAt the creation timestamp
             * @param createdBy the creator descriptor
             * @param description the chat description
             * @param support whether this is a support chat
             * @param isParentGroup whether this is a parent community group
             * @param parentGroupId the parent community group id, if any
             * @param isDefaultSubgroup whether this is the default subgroup
             * @param displayName the display name
             * @param phoneNumberJid the phone-number JID
             * @param shareOwnPhoneNumber whether to share own phone number
             * @param phoneNumberhDuplicateLidThread flag used during PN/LID deduplication
             * @param lid the chat LID
             * @param username the username
             * @param lidOriginType the LID origin type
             * @param commentsCount the comments counter
             * @param locked whether the chat is locked
             * @param systemMessageToInsert the pending privacy system message
             * @param capiCreatedGroup whether the group was created via CAPI
             * @param accountLid the account LID
             * @param limitSharing whether sharing is limited
             * @param limitSharingSettingTimestamp the limit-sharing setting timestamp
             * @param limitSharingTrigger the limit-sharing trigger type
             * @param limitSharingInitiatedByMe whether the user initiated limit-sharing
             * @param maibaAiThreadEnabled whether the Maiba AI thread is enabled
             * @param messages the ordered message map
             */
            Chat(Jid jid, Jid newJid, Jid oldJid, Instant lastMsgTimestamp, Integer unreadCount, Boolean readOnly, Boolean endOfHistoryTransfer, ChatEphemeralTimer ephemeralExpiration, Instant ephemeralSettingTimestamp, EndOfHistoryTransferType endOfHistoryTransferType, Instant conversationTimestamp, String name, String pHash, Boolean notSpam, Boolean archived, ChatDisappearingMode disappearingMode, Integer unreadMentionCount, Boolean markedAsUnread, List<GroupParticipant> participant, byte[] tcToken, Instant tcTokenTimestamp, byte[] contactPrimaryIdentityKey, Instant pinnedTimestamp, ChatMute mute, WallpaperSettings wallpaper, MediaVisibility mediaVisibility, Instant tcTokenSenderTimestamp, Boolean suspended, Boolean terminated, Instant createdAt, String createdBy, String description, Boolean support, Boolean isParentGroup, String parentGroupId, Boolean isDefaultSubgroup, String displayName, Jid phoneNumberJid, Boolean shareOwnPhoneNumber, Boolean phoneNumberhDuplicateLidThread, Jid lid, String username, String lidOriginType, Integer commentsCount, Boolean locked, PrivacySystemMessage systemMessageToInsert, Boolean capiCreatedGroup, Jid accountLid, Boolean limitSharing, Instant limitSharingSettingTimestamp, ChatLimitSharing.TriggerType limitSharingTrigger, Boolean limitSharingInitiatedByMe, Boolean maibaAiThreadEnabled, Messages messages) {
                super(jid, newJid, oldJid, lastMsgTimestamp, unreadCount, readOnly, endOfHistoryTransfer, ephemeralExpiration, ephemeralSettingTimestamp, endOfHistoryTransferType, conversationTimestamp, name, pHash, notSpam, archived, disappearingMode, unreadMentionCount, markedAsUnread, participant, tcToken, tcTokenTimestamp, contactPrimaryIdentityKey, pinnedTimestamp, mute, wallpaper, mediaVisibility, tcTokenSenderTimestamp, suspended, terminated, createdAt, createdBy, description, support, isParentGroup, parentGroupId, isDefaultSubgroup, displayName, phoneNumberJid, shareOwnPhoneNumber, phoneNumberhDuplicateLidThread, lid, username, lidOriginType, commentsCount, locked, systemMessageToInsert, capiCreatedGroup, accountLid, limitSharing, limitSharingSettingTimestamp, limitSharingTrigger, limitSharingInitiatedByMe, maibaAiThreadEnabled);
                this.messages = messages;
            }

            /**
             * Returns the messages of this chat as a {@link Stream} of
             * {@link ChatMessageInfo}, projected on the fly from the
             * underlying {@link HistorySyncMsg} envelopes.
             *
             * <p>The returned stream holds no external resource; closing it
             * is a no-op but callers are still expected to honour the
             * try-with-resources contract declared by the abstract
             * {@link com.github.auties00.cobalt.wire.linked.chat.Chat#messages()}.
             *
             * @return the stream of messages, in insertion order
             */
            @Override
            public Stream<ChatMessageInfo> messages() {
                return messages.toStream();
            }

            /**
             * Returns the number of messages currently stored in this
             * history-sync chat.
             *
             * @return the message count, in {@code O(1)}
             */
            @Override
            public int messageCount() {
                return messages.size();
            }

            /**
             * Appends a message to the history-sync message map, wrapping it
             * in a {@link HistorySyncMsg} envelope with no ordering id.
             *
             * @param info the message to add
             * @throws NullPointerException if {@code info} is {@code null}
             */
            @Override
            public void addMessage(ChatMessageInfo info) {
                Objects.requireNonNull(info, "info cannot be null");
                messages.add(info);
            }

            /**
             * Removes the message with the given id from the history-sync
             * chat.
             *
             * @param id the message id to remove
             * @return {@code true} if an entry was removed
             */
            @Override
            public boolean removeMessage(String id) {
                return messages.removeById(id);
            }

            /**
             * Clears all messages from the history-sync chat.
             */
            @Override
            public void removeMessages() {
                messages.clear();
            }

            /**
             * Looks up a message by its id.
             *
             * @param id the message id
             * @return the matching message, or empty if absent
             */
            @Override
            public Optional<ChatMessageInfo> getMessageById(String id) {
                return messages.getMessageById(id);
            }

            /**
             * Returns the newest (most recently inserted) message of the chat.
             *
             * @return the newest message, or empty if the chat has no messages
             */
            @Override
            public Optional<ChatMessageInfo> newestMessage() {
                return messages.lastMessage();
            }

            /**
             * Returns the oldest message of the chat.
             *
             * @return the oldest message, or empty if the chat has no messages
             */
            @Override
            public Optional<ChatMessageInfo> oldestMessage() {
                return messages.firstMessage();
            }

            /**
             * Ordered, thread-safe collection of {@link HistorySyncMsg}
             * envelopes keyed by message id.
             *
             * <p>Backed by a {@link ConcurrentLinkedHashMap} that preserves
             * insertion order, so iteration, {@link #firstMessage()} and
             * {@link #lastMessage()} reflect the conversation sequence as it
             * was received from the primary device.
             */
            // TODO: Remove me when daedalus is prod ready
            static final class Messages<T extends HistorySyncMsg> implements SequencedCollection<T> {
                /**
                 * Backing map keyed by the message id, preserving insertion
                 * order for sequenced access.
                 */
                private final ConcurrentLinkedHashMap<String, T> backing;

                /**
                 * Constructs an empty history-sync message collection.
                 */
                Messages() {
                    this.backing = new ConcurrentLinkedHashMap<>();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public SequencedCollection<T> reversed() {
                    return backing.sequencedValues().reversed();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public int size() {
                    return backing.size();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean isEmpty() {
                    return backing.isEmpty();
                }

                /**
                 * {@inheritDoc}
                 *
                 * <p>Membership is checked by the id carried by the envelope's
                 * {@link ChatMessageInfo#key()}, not by value equality.
                 */
                @Override
                public boolean contains(Object o) {
                    return o instanceof HistorySyncMsg historySyncMsg && historySyncMsg.message()
                            .map(ChatMessageInfo::key)
                            .flatMap(MessageKey::id)
                            .filter(backing::containsKey)
                            .isPresent();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Iterator<T> iterator() {
                    return backing.sequencedValues().iterator();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Object[] toArray() {
                    return backing.sequencedValues().toArray();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public <T> T[] toArray(T[] a) {
                    return backing.sequencedValues().toArray(a);
                }

                /**
                 * {@inheritDoc}
                 *
                 * @return {@code true} if the envelope was stored; {@code false}
                 *         if it carried no message or no key id
                 */
                @Override
                public boolean add(T messageInfo) {
                    Objects.requireNonNull(messageInfo);
                    var id = messageInfo.message()
                            .flatMap(message -> message.key().id());
                    if (id.isEmpty()) {
                        return false;
                    }
                    backing.put(id.get(), messageInfo);
                    return true;
                }

                /**
                 * Adds a bare {@link ChatMessageInfo} by wrapping it in a
                 * {@link HistorySyncMsg} envelope with no ordering id.
                 *
                 * @param info the chat message to add
                 * @return {@code true} if the entry was stored; {@code false}
                 *         if the message has no key id
                 * @throws NullPointerException if {@code info} is {@code null}
                 */
                public boolean add(ChatMessageInfo info) {
                    Objects.requireNonNull(info);
                    var id = info.key().id().orElse(null);
                    if (id == null) {
                        return false;
                    }
                    backing.put(id, (T) new HistorySyncMsg(info, null));
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean remove(Object o) {
                    return o instanceof HistorySyncMsg historySyncMsg && historySyncMsg.message()
                            .map(ChatMessageInfo::key)
                            .flatMap(MessageKey::id)
                            .filter(id -> backing.remove(id) != null)
                            .isPresent();
                }

                /**
                 * Removes the envelope stored under the given message id.
                 *
                 * @param id the message id
                 * @return {@code true} if an entry was removed
                 * @throws NullPointerException if {@code id} is {@code null}
                 */
                public boolean removeById(String id) {
                    Objects.requireNonNull(id);
                    return backing.remove(id) != null;
                }

                /**
                 * Returns the chat message stored under the given id, if any.
                 *
                 * @param id the message id
                 * @return the matching chat message, or empty if absent or if
                 *         the envelope carries no inner message
                 * @throws NullPointerException if {@code id} is {@code null}
                 */
                public Optional<ChatMessageInfo> getMessageById(String id) {
                    Objects.requireNonNull(id);
                    var envelope = backing.get(id);
                    return envelope == null
                            ? Optional.empty()
                            : envelope.message();
                }

                /**
                 * Returns the oldest chat message, following insertion order.
                 *
                 * @return the first chat message, or empty if the collection
                 *         is empty or its first envelope carries no inner
                 *         message
                 */
                public Optional<ChatMessageInfo> firstMessage() {
                    var entry = backing.firstEntry();
                    return entry == null
                            ? Optional.empty()
                            : entry.getValue().message();
                }

                /**
                 * Returns the newest chat message, following insertion order.
                 *
                 * @return the last chat message, or empty if the collection
                 *         is empty or its last envelope carries no inner
                 *         message
                 */
                public Optional<ChatMessageInfo> lastMessage() {
                    var entry = backing.lastEntry();
                    return entry == null
                            ? Optional.empty()
                            : entry.getValue().message();
                }

                /**
                 * Returns a lazy {@link Stream} of the chat messages carried
                 * by this collection, projected through
                 * {@link HistorySyncMsg#message()} on demand. Envelopes that
                 * carry no inner message are dropped.
                 *
                 * <p>Elements are unwrapped one at a time during iteration
                 * without any intermediate materialisation. The stream
                 * reflects subsequent mutations of the backing map.
                 *
                 * @return a non-null lazy stream of chat messages, in
                 *         insertion order
                 */
                public Stream<ChatMessageInfo> toStream() {
                    return backing.sequencedValues()
                            .stream()
                            .flatMap(envelope -> envelope.message().stream());
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean containsAll(Collection<?> collection) {
                    Objects.requireNonNull(collection);
                    return collection.stream()
                            .allMatch(this::contains);
                }

                /**
                 * {@inheritDoc}
                 *
                 * @return {@code true} if at least one envelope was stored
                 */
                @Override
                public boolean addAll(Collection<? extends T> collection) {
                    Objects.requireNonNull(collection);
                    var changed = false;
                    for (var entry : collection) {
                        changed |= add(entry);
                    }
                    return changed;
                }

                /**
                 * {@inheritDoc}
                 *
                 * @return {@code true} if at least one envelope was removed
                 */
                @Override
                public boolean removeAll(Collection<?> collection) {
                    Objects.requireNonNull(collection);
                    var changed = false;
                    for (var entry : collection) {
                        changed |= remove(entry);
                    }
                    return changed;
                }

                /**
                 * {@inheritDoc}
                 *
                 * @return {@code true} if at least one envelope was removed
                 */
                @Override
                public boolean retainAll(Collection<?> collection) {
                    Objects.requireNonNull(collection);
                    Map<String, HistorySyncMsg> lookup = HashMap.newHashMap(collection.size());
                    for (var entry : collection) {
                        if (!(entry instanceof HistorySyncMsg historySyncMsg)) {
                            continue;
                        }
                        historySyncMsg.message()
                                .map(ChatMessageInfo::key)
                                .flatMap(MessageKey::id)
                                .ifPresent(id -> lookup.put(id, historySyncMsg));
                    }
                    var changed = false;
                    var iterator = backing.entrySet().iterator();
                    while (iterator.hasNext()) {
                        var entry = iterator.next();
                        if (!lookup.containsKey(entry.getKey())) {
                            iterator.remove();
                            changed = true;
                        }
                    }
                    return changed;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void clear() {
                    backing.clear();
                }
            }
        }
    }

    /**
     * Lightweight history-sync variant: carries only the shared metadata and
     * omits chats and status messages. Used when the server pushes metadata
     * updates without re-sending message history.
     */
    @ProtobufMessage
    static final class Light extends HistorySync {
        /**
         * Constructs a light history-sync payload. All chat-bearing fields
         * inherited from {@link HistorySync} are left to default.
         *
         * @param syncType the sync type
         * @param chunkOrder the chunk index
         * @param progress the progress value
         * @param pushnames the push-names
         * @param globalSettings the global settings
         * @param chatThreadLoggingSecret the Chat Thread Logging HMAC seed
         * @param chatThreadLoggingDisappearingOffset the Chat Thread Logging
         *                                            timeframe offset
         * @param recentStickers the recent stickers
         * @param pastParticipants the past participants
         * @param callLogs the call logs
         * @param aiWaitListState the AI wait-list state
         * @param phoneNumberToLidMappings the PN to LID mappings
         * @param companionMmsAuthNonce the MMS authorization nonce
         * @param shareableChatLinkKey the shareable-chat-link encryption key
         * @param accounts the linked accounts
         */
        Light(HistorySyncType syncType, Integer chunkOrder, Integer progress, List<Pushname> pushnames, GlobalSettings globalSettings, byte[] chatThreadLoggingSecret, Integer chatThreadLoggingDisappearingOffset, List<StickerMetadata> recentStickers, List<GroupPastParticipants> pastParticipants, List<CallLog> callLogs, BotAIWaitListState aiWaitListState, List<PhoneNumberToLIDMapping> phoneNumberToLidMappings, String companionMmsAuthNonce, byte[] shareableChatLinkKey, List<Account> accounts) {
            super(syncType, chunkOrder, progress, pushnames, globalSettings, chatThreadLoggingSecret, chatThreadLoggingDisappearingOffset, recentStickers, pastParticipants, callLogs, aiWaitListState, phoneNumberToLidMappings, companionMmsAuthNonce, shareableChatLinkKey, accounts);
        }

        /**
         * Returns an empty list: the light variant never carries chats.
         *
         * @return an empty chat list
         */
        @Override
        public List<Chat> chats() {
            return List.of();
        }

        /**
         * Returns an empty list: the light variant never carries status
         * messages.
         *
         * @return an empty status-message list
         */
        @Override
        public List<ChatMessageInfo> statusV3Messages() {
            return List.of();
        }

        /**
         * Serialises this light payload using the corresponding protobuf spec.
         *
         * @param out the output stream to write to
         */
        @Override
        public void writeTo(ProtobufOutputStream<?> out) {
            HistorySyncLightSpec.encode(this, out);
        }
    }

    /**
     * Wait-list state for AI bot features on the account.
     */
    @ProtobufEnum(name = "HistorySync.BotAIWaitListState")
    public enum BotAIWaitListState {
        /**
         * Account is on the wait-list for AI features.
         */
        IN_WAITLIST(0),
        /**
         * AI features are available to the account.
         */
        AI_AVAILABLE(1);

        /**
         * Constructs a wait-list state constant.
         *
         * @param index the protobuf wire index
         */
        BotAIWaitListState(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Protobuf wire index for this state.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this state.
         *
         * @return the protobuf enum index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Descriptor of an account linked to the main WhatsApp identity, carried
     * alongside the main history sync when a user has multiple accounts.
     */
    @ProtobufMessage(name = "Account")
    public static final class Account {
        /**
         * LID of the linked account.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid lid;

        /**
         * Username of the linked account, if any.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String username;

        /**
         * Country code associated with the linked account.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String countryCode;

        /**
         * Whether the username has been deleted.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
        Boolean isUsernameDeleted;


        /**
         * Constructs a linked-account descriptor.
         *
         * @param lid the account LID
         * @param username the account username
         * @param countryCode the account country code
         * @param isUsernameDeleted whether the username has been deleted
         */
        Account(Jid lid, String username, String countryCode, Boolean isUsernameDeleted) {
            this.lid = lid;
            this.username = username;
            this.countryCode = countryCode;
            this.isUsernameDeleted = isUsernameDeleted;
        }

        /**
         * Returns the LID of the linked account.
         *
         * @return the LID, or empty if absent
         */
        public Optional<Jid> lid() {
            return Optional.ofNullable(lid);
        }

        /**
         * Returns the username of the linked account.
         *
         * @return the username, or empty if absent
         */
        public Optional<String> username() {
            return Optional.ofNullable(username);
        }

        /**
         * Returns the country code of the linked account.
         *
         * @return the country code, or empty if absent
         */
        public Optional<String> countryCode() {
            return Optional.ofNullable(countryCode);
        }

        /**
         * Returns whether the username has been deleted. Absent flags are
         * treated as {@code false}.
         *
         * @return {@code true} if the username is marked deleted
         */
        public boolean isUsernameDeleted() {
            return isUsernameDeleted != null && isUsernameDeleted;
        }

        /**
         * Sets the LID of the linked account.
         *
         * @param lid the LID
         */
        public void setLid(Jid lid) {
            this.lid = lid;
    }

        /**
         * Sets the username of the linked account.
         *
         * @param username the username
         */
        public void setUsername(String username) {
            this.username = username;
    }

        /**
         * Sets the country code of the linked account.
         *
         * @param countryCode the country code
         */
        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
    }

        /**
         * Sets whether the username has been deleted.
         *
         * @param isUsernameDeleted the deletion flag
         */
        public void setUsernameDeleted(Boolean isUsernameDeleted) {
            this.isUsernameDeleted = isUsernameDeleted;
    }
    }

    /**
     * Mapping from a contact identifier to its user-visible push name.
     *
     * <p>Delivered as part of a history sync so that the receiving device
     * can display correct sender names for messages whose senders are not in
     * the local contact list.
     */
    @ProtobufMessage(name = "Pushname")
    public static final class Pushname {
        /**
         * Identifier of the contact (usually a JID string).
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String id;

        /**
         * Push name advertised by that contact.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String pushname;


        /**
         * Constructs a push-name mapping.
         *
         * @param id the contact identifier
         * @param pushname the advertised push name
         */
        Pushname(String id, String pushname) {
            this.id = id;
            this.pushname = pushname;
        }

        /**
         * Returns the contact identifier.
         *
         * @return the identifier, or empty if absent
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the advertised push name.
         *
         * @return the push name, or empty if absent
         */
        public Optional<String> pushname() {
            return Optional.ofNullable(pushname);
        }

        /**
         * Sets the contact identifier.
         *
         * @param id the identifier
         */
        public void setId(String id) {
            this.id = id;
    }

        /**
         * Sets the advertised push name.
         *
         * @param pushname the push name
         */
        public void setPushname(String pushname) {
            this.pushname = pushname;
    }
    }
}

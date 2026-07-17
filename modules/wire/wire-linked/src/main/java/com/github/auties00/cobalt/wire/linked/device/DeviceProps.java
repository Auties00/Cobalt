package com.github.auties00.cobalt.wire.linked.device;

import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion;
import com.github.auties00.cobalt.wire.linked.device.pairing.DevicePlatformType;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Descriptor sent by a companion device to the primary during pairing that
 * identifies the companion and declares the history synchronisation features
 * it supports.
 *
 * <p>When a new device (phone, desktop app, browser, etc.) links to an
 * existing WhatsApp account, it must tell the primary device three things:
 * the operating system it is running on (shown in the "Linked devices"
 * screen as human readable text, for example {@code "Firefox (Linux)"}),
 * the general platform family (used for iconography and server side
 * feature gating) and the version of the client application. It must also
 * indicate whether it needs a full history sync (typical for a brand new
 * pairing) or can skip it, together with a detailed description of which
 * history sync features it is capable of consuming so that the primary can
 * tailor the history bundle.
 *
 * <p>This information is transmitted once during the pairing handshake and
 * is cached server side for the lifetime of the companion link. All fields
 * are optional on the wire: a missing field means the capability is either
 * unknown or not advertised.
 *
 * @see DevicePlatformType
 * @see ClientAppVersion
 */
@ProtobufMessage(name = "DeviceProps")
public final class DeviceProps {
    /**
     * Free form string describing the operating system of the companion
     * device, shown verbatim to the user on the primary's linked devices
     * screen (for example {@code "Mac OS 14"} or {@code "Chrome on Linux"}).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String os;

    /**
     * Version of the client application running on the companion device.
     * Expressed as a five part numeric tuple. Used by the server to decide
     * whether the companion must be forced to upgrade before it can operate.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    ClientAppVersion version;

    /**
     * Platform family of the companion, used by the primary to pick the
     * correct icon in the linked devices UI and by the server for feature
     * gating.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    DevicePlatformType platformType;

    /**
     * Whether the companion explicitly requires a full history sync from the
     * primary. {@code true} is the default for freshly paired devices that
     * do not have any local history to reuse.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean requireFullSync;

    /**
     * Fine grained descriptor of which history sync features the companion
     * is able to consume.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    HistorySyncConfig historySyncConfig;


    /**
     * Creates a new device properties payload. This constructor is package
     * private and is reserved for the protobuf deserialiser and the
     * generated builder.
     *
     * @param os                 the human readable OS label, or {@code null}
     * @param version            the client app version, or {@code null}
     * @param platformType       the platform family, or {@code null}
     * @param requireFullSync    whether a full history sync is required, or {@code null}
     * @param historySyncConfig  the history sync capability descriptor, or {@code null}
     */
    DeviceProps(String os, ClientAppVersion version, DevicePlatformType platformType, Boolean requireFullSync, HistorySyncConfig historySyncConfig) {
        this.os = os;
        this.version = version;
        this.platformType = platformType;
        this.requireFullSync = requireFullSync;
        this.historySyncConfig = historySyncConfig;
    }

    /**
     * Returns the human readable operating system label.
     *
     * @return the OS label, or {@code Optional.empty()} if not provided
     */
    public Optional<String> os() {
        return Optional.ofNullable(os);
    }

    /**
     * Returns the version of the client application running on the companion
     * device.
     *
     * @return the client app version, or {@code Optional.empty()} if not
     *         provided
     */
    public Optional<ClientAppVersion> version() {
        return Optional.ofNullable(version);
    }

    /**
     * Returns the platform family advertised by the companion device.
     *
     * @return the platform type, or {@code Optional.empty()} if not provided
     */
    public Optional<DevicePlatformType> platformType() {
        return Optional.ofNullable(platformType);
    }

    /**
     * Returns whether the companion explicitly requires a full history sync
     * from the primary.
     *
     * @return {@code true} if a full sync is required, {@code false}
     *         otherwise or if the flag is absent
     */
    public boolean requireFullSync() {
        return requireFullSync != null && requireFullSync;
    }

    /**
     * Returns the history sync capability descriptor.
     *
     * @return the history sync config, or {@code Optional.empty()} if the
     *         companion did not advertise one
     */
    public Optional<HistorySyncConfig> historySyncConfig() {
        return Optional.ofNullable(historySyncConfig);
    }

    /**
     * Overrides the operating system label.
     *
     * @param os the new label, or {@code null} to clear it
     */
    public void setOs(String os) {
        this.os = os;
    }

    /**
     * Overrides the client application version.
     *
     * @param version the new version, or {@code null} to clear it
     */
    public void setVersion(ClientAppVersion version) {
        this.version = version;
    }

    /**
     * Overrides the platform family.
     *
     * @param platformType the new platform type, or {@code null} to clear it
     */
    public void setPlatformType(DevicePlatformType platformType) {
        this.platformType = platformType;
    }

    /**
     * Overrides the full sync requirement flag.
     *
     * @param requireFullSync the new flag, or {@code null} to clear it
     */
    public void setRequireFullSync(Boolean requireFullSync) {
        this.requireFullSync = requireFullSync;
    }

    /**
     * Overrides the history sync capability descriptor.
     *
     * @param historySyncConfig the new descriptor, or {@code null} to clear it
     */
    public void setHistorySyncConfig(HistorySyncConfig historySyncConfig) {
        this.historySyncConfig = historySyncConfig;
    }

    /**
     * Detailed description of the history synchronisation capabilities of a
     * companion device.
     *
     * <p>When a new companion pairs with the primary, the primary can bundle
     * past chat history and ship it across in the background. The shape of
     * that bundle (how many days are included, whether calls are included,
     * whether message reactions are included, etc.) depends on what the
     * companion can understand. This structure carries those knobs.
     *
     * <p>The {@code *Limit} fields are integer limits expressed in the unit
     * encoded in their name (days, megabytes, messages). The
     * {@code support*} boolean flags enable specific content categories in
     * the history bundle. Most flags default to {@code false} when absent;
     * {@link #fullSyncDaysLimit()} and similar limits return
     * {@link OptionalInt#empty()} when absent, in which case the server
     * applies its own default.
     */
    @ProtobufMessage(name = "DeviceProps.HistorySyncConfig")
    public static final class HistorySyncConfig {
        /**
         * Maximum number of days of chat history to include in a full sync.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
        Integer fullSyncDaysLimit;

        /**
         * Maximum size, in megabytes, of the full history sync payload.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
        Integer fullSyncSizeMbLimit;

        /**
         * Total disk quota, in megabytes, that the companion is willing to
         * allocate for cached history data.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
        Integer storageQuotaMb;

        /**
         * Whether the primary should inline the initial history payload
         * inside the end to end encrypted pairing message rather than
         * uploading it as a separate media attachment.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
        Boolean inlineInitialPayloadInE2EeMsg;

        /**
         * Maximum number of days of chat history to include in the smaller
         * "recent" sync that is delivered first for immediate chat
         * availability.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
        Integer recentSyncDaysLimit;

        /**
         * Whether the companion can consume call log entries included in
         * the history bundle.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
        Boolean supportCallLogHistory;

        /**
         * Whether the companion understands chat history messages whose
         * sender is a bot operating through the bot user agent framework.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
        Boolean supportBotUserAgentChatHistory;

        /**
         * Whether the companion understands reactions and polls sent inside
         * community announcement groups (CAG).
         */
        @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
        Boolean supportCagReactionsAndPolls;

        /**
         * Whether the companion can render messages sent through business
         * hosted conversations (Cloud API routed messages).
         */
        @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
        Boolean supportBizHostedMsg;

        /**
         * Whether the companion supports the tuning of message counts per
         * chunk during the recent sync streaming phase.
         */
        @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
        Boolean supportRecentSyncChunkMessageCountTuning;

        /**
         * Whether the companion can render messages sent in groups whose
         * participants include business hosted devices.
         */
        @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
        Boolean supportHostedGroupMsg;

        /**
         * Whether the companion can render chat history with Meta bots
         * identified by an FBID rather than a phone number.
         */
        @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
        Boolean supportFbidBotChatHistory;

        /**
         * Whether the companion can handle the history sync migration of
         * message add ons (reactions, poll votes, comments) from legacy
         * formats to the new representation.
         */
        @ProtobufProperty(index = 13, type = ProtobufType.BOOL)
        Boolean supportAddOnHistorySyncMigration;

        /**
         * Whether the companion supports associated messages (replies,
         * comments and other message to message links) inside the history
         * bundle.
         */
        @ProtobufProperty(index = 14, type = ProtobufType.BOOL)
        Boolean supportMessageAssociation;

        /**
         * Whether the companion can consume group chat history in the
         * history bundle.
         */
        @ProtobufProperty(index = 15, type = ProtobufType.BOOL)
        Boolean supportGroupHistory;

        /**
         * Signals to the primary that the companion is ready to handle on
         * demand history sync requests (the user clicking "load more"
         * beyond the initial window).
         */
        @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
        Boolean onDemandReady;

        /**
         * Whether the companion supports conversations initiated through
         * the guest chat flow (chats started without the usual contact
         * save).
         */
        @ProtobufProperty(index = 17, type = ProtobufType.BOOL)
        Boolean supportGuestChat;

        /**
         * Signals that the companion is ready to receive complete
         * (non chunked) on demand history responses in a single payload.
         */
        @ProtobufProperty(index = 18, type = ProtobufType.BOOL)
        Boolean completeOnDemandReady;

        /**
         * Maximum number of days of message thumbnails to include in the
         * history bundle.
         */
        @ProtobufProperty(index = 19, type = ProtobufType.UINT32)
        Integer thumbnailSyncDaysLimit;

        /**
         * Maximum number of messages per chat that should be sent in the
         * initial history sync.
         */
        @ProtobufProperty(index = 20, type = ProtobufType.UINT32)
        Integer initialSyncMaxMessagesPerChat;

        /**
         * Whether the companion can consume chat history from Manus, Meta's
         * agentic assistant surface that participates in conversations as a
         * bot side.
         */
        @ProtobufProperty(index = 21, type = ProtobufType.BOOL)
        Boolean supportManusHistory;

        /**
         * Whether the companion can consume chat history from Hatch, Meta's
         * Imagine and creator centric chat surface, when its messages are
         * replayed inside the companion's main inbox.
         */
        @ProtobufProperty(index = 22, type = ProtobufType.BOOL)
        Boolean supportHatchHistory;

        /**
         * List of Meta bot channel FBIDs whose chat history the companion
         * supports rendering. The primary uses this list to filter which bot
         * channel conversations are included in the history bundle.
         */
        @ProtobufProperty(index = 23, type = ProtobufType.STRING)
        List<String> supportedBotChannelFbids;

        /**
         * Whether the companion supports inline contact rendering inside
         * historical messages, i.e. the presentation of phone numbers as
         * tappable contact tokens that resolve against the local address
         * book rather than as plain text.
         */
        @ProtobufProperty(index = 24, type = ProtobufType.BOOL)
        Boolean supportInlineContacts;


        /**
         * Creates a new history sync capability descriptor. This constructor
         * is package private and is reserved for the protobuf deserialiser
         * and the generated builder.
         *
         * @param fullSyncDaysLimit                          see {@link #fullSyncDaysLimit()}
         * @param fullSyncSizeMbLimit                        see {@link #fullSyncSizeMbLimit()}
         * @param storageQuotaMb                             see {@link #storageQuotaMb()}
         * @param inlineInitialPayloadInE2EeMsg              see {@link #inlineInitialPayloadInE2EeMsg()}
         * @param recentSyncDaysLimit                        see {@link #recentSyncDaysLimit()}
         * @param supportCallLogHistory                      see {@link #supportCallLogHistory()}
         * @param supportBotUserAgentChatHistory             see {@link #supportBotUserAgentChatHistory()}
         * @param supportCagReactionsAndPolls                see {@link #supportCagReactionsAndPolls()}
         * @param supportBizHostedMsg                        see {@link #supportBizHostedMsg()}
         * @param supportRecentSyncChunkMessageCountTuning   see {@link #supportRecentSyncChunkMessageCountTuning()}
         * @param supportHostedGroupMsg                      see {@link #supportHostedGroupMsg()}
         * @param supportFbidBotChatHistory                  see {@link #supportFbidBotChatHistory()}
         * @param supportAddOnHistorySyncMigration           see {@link #supportAddOnHistorySyncMigration()}
         * @param supportMessageAssociation                  see {@link #supportMessageAssociation()}
         * @param supportGroupHistory                        see {@link #supportGroupHistory()}
         * @param onDemandReady                              see {@link #onDemandReady()}
         * @param supportGuestChat                           see {@link #supportGuestChat()}
         * @param completeOnDemandReady                      see {@link #completeOnDemandReady()}
         * @param thumbnailSyncDaysLimit                     see {@link #thumbnailSyncDaysLimit()}
         * @param initialSyncMaxMessagesPerChat              see {@link #initialSyncMaxMessagesPerChat()}
         * @param supportManusHistory                        see {@link #supportManusHistory()}
         * @param supportHatchHistory                        see {@link #supportHatchHistory()}
         * @param supportedBotChannelFbids                   see {@link #supportedBotChannelFbids()}
         * @param supportInlineContacts                      see {@link #supportInlineContacts()}
         */
        HistorySyncConfig(Integer fullSyncDaysLimit, Integer fullSyncSizeMbLimit, Integer storageQuotaMb, Boolean inlineInitialPayloadInE2EeMsg, Integer recentSyncDaysLimit, Boolean supportCallLogHistory, Boolean supportBotUserAgentChatHistory, Boolean supportCagReactionsAndPolls, Boolean supportBizHostedMsg, Boolean supportRecentSyncChunkMessageCountTuning, Boolean supportHostedGroupMsg, Boolean supportFbidBotChatHistory, Boolean supportAddOnHistorySyncMigration, Boolean supportMessageAssociation, Boolean supportGroupHistory, Boolean onDemandReady, Boolean supportGuestChat, Boolean completeOnDemandReady, Integer thumbnailSyncDaysLimit, Integer initialSyncMaxMessagesPerChat, Boolean supportManusHistory, Boolean supportHatchHistory, List<String> supportedBotChannelFbids, Boolean supportInlineContacts) {
            this.fullSyncDaysLimit = fullSyncDaysLimit;
            this.fullSyncSizeMbLimit = fullSyncSizeMbLimit;
            this.storageQuotaMb = storageQuotaMb;
            this.inlineInitialPayloadInE2EeMsg = inlineInitialPayloadInE2EeMsg;
            this.recentSyncDaysLimit = recentSyncDaysLimit;
            this.supportCallLogHistory = supportCallLogHistory;
            this.supportBotUserAgentChatHistory = supportBotUserAgentChatHistory;
            this.supportCagReactionsAndPolls = supportCagReactionsAndPolls;
            this.supportBizHostedMsg = supportBizHostedMsg;
            this.supportRecentSyncChunkMessageCountTuning = supportRecentSyncChunkMessageCountTuning;
            this.supportHostedGroupMsg = supportHostedGroupMsg;
            this.supportFbidBotChatHistory = supportFbidBotChatHistory;
            this.supportAddOnHistorySyncMigration = supportAddOnHistorySyncMigration;
            this.supportMessageAssociation = supportMessageAssociation;
            this.supportGroupHistory = supportGroupHistory;
            this.onDemandReady = onDemandReady;
            this.supportGuestChat = supportGuestChat;
            this.completeOnDemandReady = completeOnDemandReady;
            this.thumbnailSyncDaysLimit = thumbnailSyncDaysLimit;
            this.initialSyncMaxMessagesPerChat = initialSyncMaxMessagesPerChat;
            this.supportManusHistory = supportManusHistory;
            this.supportHatchHistory = supportHatchHistory;
            this.supportedBotChannelFbids = supportedBotChannelFbids;
            this.supportInlineContacts = supportInlineContacts;
        }

        /**
         * Returns the upper bound on the number of days of chat history to
         * ship in the full sync payload.
         *
         * @return the day limit, or {@link OptionalInt#empty()} if the
         *         server's default should apply
         */
        public OptionalInt fullSyncDaysLimit() {
            return fullSyncDaysLimit == null ? OptionalInt.empty() : OptionalInt.of(fullSyncDaysLimit);
        }

        /**
         * Returns the upper bound, in megabytes, on the full sync payload
         * size.
         *
         * @return the size limit in megabytes, or {@link OptionalInt#empty()}
         *         if the server's default should apply
         */
        public OptionalInt fullSyncSizeMbLimit() {
            return fullSyncSizeMbLimit == null ? OptionalInt.empty() : OptionalInt.of(fullSyncSizeMbLimit);
        }

        /**
         * Returns the total disk budget, in megabytes, that the companion
         * device allocates for cached history content.
         *
         * @return the storage quota in megabytes, or
         *         {@link OptionalInt#empty()} if unspecified
         */
        public OptionalInt storageQuotaMb() {
            return storageQuotaMb == null ? OptionalInt.empty() : OptionalInt.of(storageQuotaMb);
        }

        /**
         * Returns whether the primary should inline the initial history
         * bundle inside the encrypted pairing message (as opposed to
         * uploading it as a separate media blob).
         *
         * @return {@code true} if inlining is supported, {@code false}
         *         otherwise
         */
        public boolean inlineInitialPayloadInE2EeMsg() {
            return inlineInitialPayloadInE2EeMsg != null && inlineInitialPayloadInE2EeMsg;
        }

        /**
         * Returns the day window for the small "recent" history sync that
         * is delivered first for immediate chat availability.
         *
         * @return the recent window in days, or {@link OptionalInt#empty()}
         *         if unspecified
         */
        public OptionalInt recentSyncDaysLimit() {
            return recentSyncDaysLimit == null ? OptionalInt.empty() : OptionalInt.of(recentSyncDaysLimit);
        }

        /**
         * Returns whether the companion can consume call log entries.
         *
         * @return {@code true} if call log history is supported,
         *         {@code false} otherwise
         */
        public boolean supportCallLogHistory() {
            return supportCallLogHistory != null && supportCallLogHistory;
        }

        /**
         * Returns whether the companion can consume chat history messages
         * sent by bot user agents.
         *
         * @return {@code true} if bot user agent chat history is
         *         supported, {@code false} otherwise
         */
        public boolean supportBotUserAgentChatHistory() {
            return supportBotUserAgentChatHistory != null && supportBotUserAgentChatHistory;
        }

        /**
         * Returns whether the companion supports reactions and polls inside
         * community announcement group history.
         *
         * @return {@code true} if CAG reactions and polls are supported,
         *         {@code false} otherwise
         */
        public boolean supportCagReactionsAndPolls() {
            return supportCagReactionsAndPolls != null && supportCagReactionsAndPolls;
        }

        /**
         * Returns whether the companion supports business hosted messages.
         *
         * @return {@code true} if hosted business messages are supported,
         *         {@code false} otherwise
         */
        public boolean supportBizHostedMsg() {
            return supportBizHostedMsg != null && supportBizHostedMsg;
        }

        /**
         * Returns whether the companion supports tuning of the per chunk
         * message count during recent sync streaming.
         *
         * @return {@code true} if chunk tuning is supported, {@code false}
         *         otherwise
         */
        public boolean supportRecentSyncChunkMessageCountTuning() {
            return supportRecentSyncChunkMessageCountTuning != null && supportRecentSyncChunkMessageCountTuning;
        }

        /**
         * Returns whether the companion supports rendering messages from
         * hosted participants inside groups.
         *
         * @return {@code true} if hosted group messages are supported,
         *         {@code false} otherwise
         */
        public boolean supportHostedGroupMsg() {
            return supportHostedGroupMsg != null && supportHostedGroupMsg;
        }

        /**
         * Returns whether the companion supports chat history with bots
         * identified by FBID.
         *
         * @return {@code true} if FBID bot chat history is supported,
         *         {@code false} otherwise
         */
        public boolean supportFbidBotChatHistory() {
            return supportFbidBotChatHistory != null && supportFbidBotChatHistory;
        }

        /**
         * Returns whether the companion can migrate message add ons
         * (reactions, poll votes, comments) from their legacy
         * representation during history sync.
         *
         * @return {@code true} if add on migration is supported,
         *         {@code false} otherwise
         */
        public boolean supportAddOnHistorySyncMigration() {
            return supportAddOnHistorySyncMigration != null && supportAddOnHistorySyncMigration;
        }

        /**
         * Returns whether the companion supports message association
         * relationships (replies, comments, links between messages) inside
         * the history bundle.
         *
         * @return {@code true} if message associations are supported,
         *         {@code false} otherwise
         */
        public boolean supportMessageAssociation() {
            return supportMessageAssociation != null && supportMessageAssociation;
        }

        /**
         * Returns whether the companion supports group history in the
         * history bundle.
         *
         * @return {@code true} if group history is supported,
         *         {@code false} otherwise
         */
        public boolean supportGroupHistory() {
            return supportGroupHistory != null && supportGroupHistory;
        }

        /**
         * Returns whether the companion is ready to issue and handle
         * on demand history sync requests.
         *
         * @return {@code true} if on demand sync is supported,
         *         {@code false} otherwise
         */
        public boolean onDemandReady() {
            return onDemandReady != null && onDemandReady;
        }

        /**
         * Returns whether the companion supports the guest chat flow.
         *
         * @return {@code true} if guest chat is supported, {@code false}
         *         otherwise
         */
        public boolean supportGuestChat() {
            return supportGuestChat != null && supportGuestChat;
        }

        /**
         * Returns whether the companion can receive a complete
         * (non chunked) on demand history payload in a single response.
         *
         * @return {@code true} if complete on demand sync is supported,
         *         {@code false} otherwise
         */
        public boolean completeOnDemandReady() {
            return completeOnDemandReady != null && completeOnDemandReady;
        }

        /**
         * Returns the day window for message thumbnails shipped in the
         * history bundle.
         *
         * @return the thumbnail day limit, or {@link OptionalInt#empty()}
         *         if the server's default should apply
         */
        public OptionalInt thumbnailSyncDaysLimit() {
            return thumbnailSyncDaysLimit == null ? OptionalInt.empty() : OptionalInt.of(thumbnailSyncDaysLimit);
        }

        /**
         * Returns the maximum number of messages per chat that should be
         * included in the initial sync.
         *
         * @return the initial sync per chat limit, or
         *         {@link OptionalInt#empty()} if unspecified
         */
        public OptionalInt initialSyncMaxMessagesPerChat() {
            return initialSyncMaxMessagesPerChat == null ? OptionalInt.empty() : OptionalInt.of(initialSyncMaxMessagesPerChat);
        }

        /**
         * Returns whether the companion can consume Manus chat history in
         * the history bundle.
         *
         * @return {@code true} if Manus history is supported, {@code false}
         *         otherwise
         */
        public boolean supportManusHistory() {
            return supportManusHistory != null && supportManusHistory;
        }

        /**
         * Returns whether the companion can consume Hatch chat history in
         * the history bundle.
         *
         * @return {@code true} if Hatch history is supported, {@code false}
         *         otherwise
         */
        public boolean supportHatchHistory() {
            return supportHatchHistory != null && supportHatchHistory;
        }

        /**
         * Returns the list of Meta bot channel FBIDs whose chat history the
         * companion supports rendering.
         *
         * @return the list of supported bot channel FBIDs; never {@code null},
         *         possibly empty when no entries were sent on the wire
         */
        public List<String> supportedBotChannelFbids() {
            return supportedBotChannelFbids == null ? List.of() : supportedBotChannelFbids;
        }

        /**
         * Returns whether the companion supports rendering inline contact
         * tokens inside historical messages.
         *
         * @return {@code true} if inline contacts are supported,
         *         {@code false} otherwise
         */
        public boolean supportInlineContacts() {
            return supportInlineContacts != null && supportInlineContacts;
        }

        /**
         * Overrides the full sync day limit.
         *
         * @param fullSyncDaysLimit the new limit, or {@code null} to clear it
         */
        public void setFullSyncDaysLimit(Integer fullSyncDaysLimit) {
            this.fullSyncDaysLimit = fullSyncDaysLimit;
    }

        /**
         * Overrides the full sync size limit in megabytes.
         *
         * @param fullSyncSizeMbLimit the new limit, or {@code null} to
         *                            clear it
         */
        public void setFullSyncSizeMbLimit(Integer fullSyncSizeMbLimit) {
            this.fullSyncSizeMbLimit = fullSyncSizeMbLimit;
    }

        /**
         * Overrides the storage quota in megabytes.
         *
         * @param storageQuotaMb the new quota, or {@code null} to clear it
         */
        public void setStorageQuotaMb(Integer storageQuotaMb) {
            this.storageQuotaMb = storageQuotaMb;
    }

        /**
         * Overrides the inline initial payload flag.
         *
         * @param inlineInitialPayloadInE2EeMsg the new flag, or {@code null}
         *                                      to clear it
         */
        public void setInlineInitialPayloadInE2EeMsg(Boolean inlineInitialPayloadInE2EeMsg) {
            this.inlineInitialPayloadInE2EeMsg = inlineInitialPayloadInE2EeMsg;
    }

        /**
         * Overrides the recent sync day limit.
         *
         * @param recentSyncDaysLimit the new limit, or {@code null} to
         *                            clear it
         */
        public void setRecentSyncDaysLimit(Integer recentSyncDaysLimit) {
            this.recentSyncDaysLimit = recentSyncDaysLimit;
    }

        /**
         * Overrides the call log history support flag.
         *
         * @param supportCallLogHistory the new flag, or {@code null} to
         *                              clear it
         */
        public void setSupportCallLogHistory(Boolean supportCallLogHistory) {
            this.supportCallLogHistory = supportCallLogHistory;
    }

        /**
         * Overrides the bot user agent chat history support flag.
         *
         * @param supportBotUserAgentChatHistory the new flag, or
         *                                       {@code null} to clear it
         */
        public void setSupportBotUserAgentChatHistory(Boolean supportBotUserAgentChatHistory) {
            this.supportBotUserAgentChatHistory = supportBotUserAgentChatHistory;
    }

        /**
         * Overrides the CAG reactions and polls support flag.
         *
         * @param supportCagReactionsAndPolls the new flag, or {@code null}
         *                                    to clear it
         */
        public void setSupportCagReactionsAndPolls(Boolean supportCagReactionsAndPolls) {
            this.supportCagReactionsAndPolls = supportCagReactionsAndPolls;
    }

        /**
         * Overrides the business hosted message support flag.
         *
         * @param supportBizHostedMsg the new flag, or {@code null} to
         *                            clear it
         */
        public void setSupportBizHostedMsg(Boolean supportBizHostedMsg) {
            this.supportBizHostedMsg = supportBizHostedMsg;
    }

        /**
         * Overrides the recent sync chunk message count tuning flag.
         *
         * @param supportRecentSyncChunkMessageCountTuning the new flag, or
         *                                                 {@code null} to
         *                                                 clear it
         */
        public void setSupportRecentSyncChunkMessageCountTuning(Boolean supportRecentSyncChunkMessageCountTuning) {
            this.supportRecentSyncChunkMessageCountTuning = supportRecentSyncChunkMessageCountTuning;
    }

        /**
         * Overrides the hosted group messages support flag.
         *
         * @param supportHostedGroupMsg the new flag, or {@code null} to
         *                              clear it
         */
        public void setSupportHostedGroupMsg(Boolean supportHostedGroupMsg) {
            this.supportHostedGroupMsg = supportHostedGroupMsg;
    }

        /**
         * Overrides the FBID bot chat history support flag.
         *
         * @param supportFbidBotChatHistory the new flag, or {@code null} to
         *                                  clear it
         */
        public void setSupportFbidBotChatHistory(Boolean supportFbidBotChatHistory) {
            this.supportFbidBotChatHistory = supportFbidBotChatHistory;
    }

        /**
         * Overrides the message add on history migration support flag.
         *
         * @param supportAddOnHistorySyncMigration the new flag, or
         *                                         {@code null} to clear it
         */
        public void setSupportAddOnHistorySyncMigration(Boolean supportAddOnHistorySyncMigration) {
            this.supportAddOnHistorySyncMigration = supportAddOnHistorySyncMigration;
    }

        /**
         * Overrides the message association support flag.
         *
         * @param supportMessageAssociation the new flag, or {@code null} to
         *                                  clear it
         */
        public void setSupportMessageAssociation(Boolean supportMessageAssociation) {
            this.supportMessageAssociation = supportMessageAssociation;
    }

        /**
         * Overrides the group history support flag.
         *
         * @param supportGroupHistory the new flag, or {@code null} to clear it
         */
        public void setSupportGroupHistory(Boolean supportGroupHistory) {
            this.supportGroupHistory = supportGroupHistory;
    }

        /**
         * Overrides the on demand history sync readiness flag.
         *
         * @param onDemandReady the new flag, or {@code null} to clear it
         */
        public void setOnDemandReady(Boolean onDemandReady) {
            this.onDemandReady = onDemandReady;
    }

        /**
         * Overrides the guest chat support flag.
         *
         * @param supportGuestChat the new flag, or {@code null} to clear it
         */
        public void setSupportGuestChat(Boolean supportGuestChat) {
            this.supportGuestChat = supportGuestChat;
    }

        /**
         * Overrides the complete on demand readiness flag.
         *
         * @param completeOnDemandReady the new flag, or {@code null} to
         *                              clear it
         */
        public void setCompleteOnDemandReady(Boolean completeOnDemandReady) {
            this.completeOnDemandReady = completeOnDemandReady;
    }

        /**
         * Overrides the thumbnail sync day limit.
         *
         * @param thumbnailSyncDaysLimit the new limit, or {@code null} to
         *                               clear it
         */
        public void setThumbnailSyncDaysLimit(Integer thumbnailSyncDaysLimit) {
            this.thumbnailSyncDaysLimit = thumbnailSyncDaysLimit;
    }

        /**
         * Overrides the initial sync per chat message cap.
         *
         * @param initialSyncMaxMessagesPerChat the new cap, or {@code null}
         *                                      to clear it
         */
        public void setInitialSyncMaxMessagesPerChat(Integer initialSyncMaxMessagesPerChat) {
            this.initialSyncMaxMessagesPerChat = initialSyncMaxMessagesPerChat;
    }

        /**
         * Overrides the Manus history support flag.
         *
         * @param supportManusHistory the new flag, or {@code null} to clear it
         */
        public void setSupportManusHistory(Boolean supportManusHistory) {
            this.supportManusHistory = supportManusHistory;
    }

        /**
         * Overrides the Hatch history support flag.
         *
         * @param supportHatchHistory the new flag, or {@code null} to clear it
         */
        public void setSupportHatchHistory(Boolean supportHatchHistory) {
            this.supportHatchHistory = supportHatchHistory;
    }

        /**
         * Overrides the supported bot channel FBIDs list.
         *
         * @param supportedBotChannelFbids the new list, or {@code null} to
         *                                 clear it
         */
        public void setSupportedBotChannelFbids(List<String> supportedBotChannelFbids) {
            this.supportedBotChannelFbids = supportedBotChannelFbids;
    }

        /**
         * Overrides the inline contacts support flag.
         *
         * @param supportInlineContacts the new flag, or {@code null} to
         *                              clear it
         */
        public void setSupportInlineContacts(Boolean supportInlineContacts) {
            this.supportInlineContacts = supportInlineContacts;
    }
    }
}

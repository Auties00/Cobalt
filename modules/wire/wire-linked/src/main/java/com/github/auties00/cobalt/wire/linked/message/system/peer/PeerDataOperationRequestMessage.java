package com.github.auties00.cobalt.wire.linked.message.system.peer;

import com.github.auties00.cobalt.wire.linked.device.DeviceProps;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.system.history.FullHistorySyncOnDemandConfig;
import com.github.auties00.cobalt.wire.linked.message.system.history.FullHistorySyncOnDemandRequestMetadata;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncType;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Carries a peer-to-peer data operation request from one of a user's devices
 * to another.
 *
 * <p>Peer data operations allow companion devices to delegate work to the
 * primary device (and occasionally the reverse) for tasks that cannot be
 * completed locally. The envelope carries a {@link PeerDataOperationRequestType}
 * discriminator together with a set of optional request bodies; exactly one
 * body is populated according to the discriminator value. Typical scenarios
 * include reuploading an expired sticker, generating a link preview, asking
 * for additional history, resending a placeholder message, fetching an
 * authentication nonce, recovering from a fatal app-state desync, or
 * notifying a Galaxy flow event.
 */
@ProtobufMessage(name = "Message.PeerDataOperationRequestMessage")
public final class PeerDataOperationRequestMessage implements Message {
    /**
     * The discriminator that identifies which request body is populated.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    PeerDataOperationRequestType peerDataOperationRequestType;

    /**
     * The list of sticker reupload requests carried by this message.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<RequestStickerReupload> requestStickerReupload;

    /**
     * The list of URL preview requests carried by this message.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    List<RequestUrlPreview> requestUrlPreview;

    /**
     * The on-demand history sync request carried by this message.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    HistorySyncOnDemandRequest historySyncOnDemandRequest;

    /**
     * The list of placeholder message resend requests carried by this
     * message.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    List<PlaceholderMessageResendRequest> placeholderMessageResendRequest;

    /**
     * The full on-demand history sync request carried by this message.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    FullHistorySyncOnDemandRequest fullHistorySyncOnDemandRequest;

    /**
     * The syncd collection fatal recovery request carried by this message.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    SyncDCollectionFatalRecoveryRequest syncdCollectionFatalRecoveryRequest;

    /**
     * The history sync chunk retry request carried by this message.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    HistorySyncChunkRetryRequest historySyncChunkRetryRequest;

    /**
     * The Galaxy flow action request carried by this message.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    GalaxyFlowAction galaxyFlowAction;


    /**
     * Constructs a new peer data operation request envelope.
     *
     * @param peerDataOperationRequestType         the request type discriminator
     * @param requestStickerReupload               sticker reupload requests
     * @param requestUrlPreview                    URL preview requests
     * @param historySyncOnDemandRequest           on-demand history sync request
     * @param placeholderMessageResendRequest      placeholder resend requests
     * @param fullHistorySyncOnDemandRequest       full on-demand history sync request
     * @param syncdCollectionFatalRecoveryRequest  syncd collection recovery request
     * @param historySyncChunkRetryRequest         history sync chunk retry request
     * @param galaxyFlowAction                     Galaxy flow action request
     */
    PeerDataOperationRequestMessage(PeerDataOperationRequestType peerDataOperationRequestType, List<RequestStickerReupload> requestStickerReupload, List<RequestUrlPreview> requestUrlPreview, HistorySyncOnDemandRequest historySyncOnDemandRequest, List<PlaceholderMessageResendRequest> placeholderMessageResendRequest, FullHistorySyncOnDemandRequest fullHistorySyncOnDemandRequest, SyncDCollectionFatalRecoveryRequest syncdCollectionFatalRecoveryRequest, HistorySyncChunkRetryRequest historySyncChunkRetryRequest, GalaxyFlowAction galaxyFlowAction) {
        this.peerDataOperationRequestType = peerDataOperationRequestType;
        this.requestStickerReupload = requestStickerReupload;
        this.requestUrlPreview = requestUrlPreview;
        this.historySyncOnDemandRequest = historySyncOnDemandRequest;
        this.placeholderMessageResendRequest = placeholderMessageResendRequest;
        this.fullHistorySyncOnDemandRequest = fullHistorySyncOnDemandRequest;
        this.syncdCollectionFatalRecoveryRequest = syncdCollectionFatalRecoveryRequest;
        this.historySyncChunkRetryRequest = historySyncChunkRetryRequest;
        this.galaxyFlowAction = galaxyFlowAction;
    }

    /**
     * Returns the discriminator that identifies which request body is
     * populated.
     *
     * @return an {@link Optional} containing the request type, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<PeerDataOperationRequestType> peerDataOperationRequestType() {
        return Optional.ofNullable(peerDataOperationRequestType);
    }

    /**
     * Returns an unmodifiable view of the sticker reupload requests carried
     * by this message.
     *
     * @return the list of sticker reupload requests, or an empty list if none
     *         were provided
     */
    public List<RequestStickerReupload> requestStickerReupload() {
        return requestStickerReupload == null ? List.of() : Collections.unmodifiableList(requestStickerReupload);
    }

    /**
     * Returns an unmodifiable view of the URL preview requests carried by
     * this message.
     *
     * @return the list of URL preview requests, or an empty list if none were
     *         provided
     */
    public List<RequestUrlPreview> requestUrlPreview() {
        return requestUrlPreview == null ? List.of() : Collections.unmodifiableList(requestUrlPreview);
    }

    /**
     * Returns the on-demand history sync request carried by this message.
     *
     * @return an {@link Optional} containing the request body, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<HistorySyncOnDemandRequest> historySyncOnDemandRequest() {
        return Optional.ofNullable(historySyncOnDemandRequest);
    }

    /**
     * Returns an unmodifiable view of the placeholder message resend requests
     * carried by this message.
     *
     * @return the list of placeholder resend requests, or an empty list if
     *         none were provided
     */
    public List<PlaceholderMessageResendRequest> placeholderMessageResendRequest() {
        return placeholderMessageResendRequest == null ? List.of() : Collections.unmodifiableList(placeholderMessageResendRequest);
    }

    /**
     * Returns the full on-demand history sync request carried by this
     * message.
     *
     * @return an {@link Optional} containing the request body, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<FullHistorySyncOnDemandRequest> fullHistorySyncOnDemandRequest() {
        return Optional.ofNullable(fullHistorySyncOnDemandRequest);
    }

    /**
     * Returns the syncd collection fatal recovery request carried by this
     * message.
     *
     * @return an {@link Optional} containing the request body, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<SyncDCollectionFatalRecoveryRequest> syncdCollectionFatalRecoveryRequest() {
        return Optional.ofNullable(syncdCollectionFatalRecoveryRequest);
    }

    /**
     * Returns the history sync chunk retry request carried by this message.
     *
     * @return an {@link Optional} containing the request body, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<HistorySyncChunkRetryRequest> historySyncChunkRetryRequest() {
        return Optional.ofNullable(historySyncChunkRetryRequest);
    }

    /**
     * Returns the Galaxy flow action request carried by this message.
     *
     * @return an {@link Optional} containing the request body, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<GalaxyFlowAction> galaxyFlowAction() {
        return Optional.ofNullable(galaxyFlowAction);
    }

    /**
     * Sets the discriminator that identifies which request body is populated.
     *
     * @param peerDataOperationRequestType the new request type, may be
     *                                     {@code null}
     */
    public void setPeerDataOperationRequestType(PeerDataOperationRequestType peerDataOperationRequestType) {
        this.peerDataOperationRequestType = peerDataOperationRequestType;
    }

    /**
     * Sets the list of sticker reupload requests carried by this message.
     *
     * @param requestStickerReupload the new list, may be {@code null}
     */
    public void setRequestStickerReupload(List<RequestStickerReupload> requestStickerReupload) {
        this.requestStickerReupload = requestStickerReupload;
    }

    /**
     * Sets the list of URL preview requests carried by this message.
     *
     * @param requestUrlPreview the new list, may be {@code null}
     */
    public void setRequestUrlPreview(List<RequestUrlPreview> requestUrlPreview) {
        this.requestUrlPreview = requestUrlPreview;
    }

    /**
     * Sets the on-demand history sync request carried by this message.
     *
     * @param historySyncOnDemandRequest the new request body, may be
     *                                   {@code null}
     */
    public void setHistorySyncOnDemandRequest(HistorySyncOnDemandRequest historySyncOnDemandRequest) {
        this.historySyncOnDemandRequest = historySyncOnDemandRequest;
    }

    /**
     * Sets the list of placeholder message resend requests carried by this
     * message.
     *
     * @param placeholderMessageResendRequest the new list, may be
     *                                        {@code null}
     */
    public void setPlaceholderMessageResendRequest(List<PlaceholderMessageResendRequest> placeholderMessageResendRequest) {
        this.placeholderMessageResendRequest = placeholderMessageResendRequest;
    }

    /**
     * Sets the full on-demand history sync request carried by this message.
     *
     * @param fullHistorySyncOnDemandRequest the new request body, may be
     *                                       {@code null}
     */
    public void setFullHistorySyncOnDemandRequest(FullHistorySyncOnDemandRequest fullHistorySyncOnDemandRequest) {
        this.fullHistorySyncOnDemandRequest = fullHistorySyncOnDemandRequest;
    }

    /**
     * Sets the syncd collection fatal recovery request carried by this
     * message.
     *
     * @param syncdCollectionFatalRecoveryRequest the new request body, may be
     *                                            {@code null}
     */
    public void setSyncdCollectionFatalRecoveryRequest(SyncDCollectionFatalRecoveryRequest syncdCollectionFatalRecoveryRequest) {
        this.syncdCollectionFatalRecoveryRequest = syncdCollectionFatalRecoveryRequest;
    }

    /**
     * Sets the history sync chunk retry request carried by this message.
     *
     * @param historySyncChunkRetryRequest the new request body, may be
     *                                     {@code null}
     */
    public void setHistorySyncChunkRetryRequest(HistorySyncChunkRetryRequest historySyncChunkRetryRequest) {
        this.historySyncChunkRetryRequest = historySyncChunkRetryRequest;
    }

    /**
     * Sets the Galaxy flow action request carried by this message.
     *
     * @param galaxyFlowAction the new request body, may be {@code null}
     */
    public void setGalaxyFlowAction(GalaxyFlowAction galaxyFlowAction) {
        this.galaxyFlowAction = galaxyFlowAction;
    }

    /**
     * Asks the peer to deliver the entire message history that the
     * requesting device does not already hold.
     *
     * <p>The request carries the originating request metadata together with
     * the user's configured history-sync policy so that the peer can decide
     * how far back to deliver messages.
     */
    @ProtobufMessage(name = "Message.PeerDataOperationRequestMessage.FullHistorySyncOnDemandRequest")
    public static final class FullHistorySyncOnDemandRequest {
        /**
         * The metadata that correlates this request with its eventual
         * responses.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        FullHistorySyncOnDemandRequestMetadata requestMetadata;

        /**
         * The user's history-sync policy, which controls how much history is
         * delivered in the response.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        DeviceProps.HistorySyncConfig historySyncConfig;

        /**
         * The on-demand window configuration, selecting the requested history
         * slice by an absolute start timestamp or a relative duration in days.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        FullHistorySyncOnDemandConfig fullHistorySyncOnDemandConfig;


        /**
         * Constructs a new full on-demand history sync request.
         *
         * @param requestMetadata               the correlating request metadata
         * @param historySyncConfig             the user's history-sync policy
         * @param fullHistorySyncOnDemandConfig the on-demand window configuration
         */
        FullHistorySyncOnDemandRequest(FullHistorySyncOnDemandRequestMetadata requestMetadata, DeviceProps.HistorySyncConfig historySyncConfig, FullHistorySyncOnDemandConfig fullHistorySyncOnDemandConfig) {
            this.requestMetadata = requestMetadata;
            this.historySyncConfig = historySyncConfig;
            this.fullHistorySyncOnDemandConfig = fullHistorySyncOnDemandConfig;
        }

        /**
         * Returns the correlating request metadata.
         *
         * @return an {@link Optional} containing the metadata, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<FullHistorySyncOnDemandRequestMetadata> requestMetadata() {
            return Optional.ofNullable(requestMetadata);
        }

        /**
         * Returns the user's history-sync policy.
         *
         * @return an {@link Optional} containing the sync configuration, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<DeviceProps.HistorySyncConfig> historySyncConfig() {
            return Optional.ofNullable(historySyncConfig);
        }

        /**
         * Returns the on-demand window configuration.
         *
         * @return an {@link Optional} containing the window configuration, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<FullHistorySyncOnDemandConfig> fullHistorySyncOnDemandConfig() {
            return Optional.ofNullable(fullHistorySyncOnDemandConfig);
        }

        /**
         * Sets the correlating request metadata.
         *
         * @param requestMetadata the new metadata, may be {@code null}
         */
        public void setRequestMetadata(FullHistorySyncOnDemandRequestMetadata requestMetadata) {
            this.requestMetadata = requestMetadata;
    }

        /**
         * Sets the user's history-sync policy.
         *
         * @param historySyncConfig the new configuration, may be {@code null}
         */
        public void setHistorySyncConfig(DeviceProps.HistorySyncConfig historySyncConfig) {
            this.historySyncConfig = historySyncConfig;
    }

        /**
         * Sets the on-demand window configuration.
         *
         * @param fullHistorySyncOnDemandConfig the new window configuration, may
         *                                      be {@code null}
         */
        public void setFullHistorySyncOnDemandConfig(FullHistorySyncOnDemandConfig fullHistorySyncOnDemandConfig) {
            this.fullHistorySyncOnDemandConfig = fullHistorySyncOnDemandConfig;
    }
    }

    /**
     * Notifies the peer of a Galaxy flow lifecycle event.
     *
     * <p>Galaxy flows are multi-step guided experiences (for example
     * onboarding wizards). This payload lets one device tell the other when
     * such a flow has been launched or advanced on its side so both ends can
     * stay in sync.
     */
    @ProtobufMessage(name = "Message.PeerDataOperationRequestMessage.GalaxyFlowAction")
    public static final class GalaxyFlowAction {
        /**
         * The specific flow lifecycle event being reported.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        GalaxyFlowAction.GalaxyFlowActionType type;

        /**
         * The identifier of the Galaxy flow that the event applies to.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String flowId;

        /**
         * The identifier of the stanza that triggered this event, used to
         * correlate it with the originating interaction.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String stanzaId;


        /**
         * Constructs a new Galaxy flow action payload.
         *
         * @param type      the lifecycle event type
         * @param flowId    the target flow identifier
         * @param stanzaId  the triggering stanza identifier
         */
        GalaxyFlowAction(GalaxyFlowActionType type, String flowId, String stanzaId) {
            this.type = type;
            this.flowId = flowId;
            this.stanzaId = stanzaId;
        }

        /**
         * Returns the lifecycle event type reported by this payload.
         *
         * @return an {@link Optional} containing the action type, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<GalaxyFlowActionType> type() {
            return Optional.ofNullable(type);
        }

        /**
         * Returns the identifier of the Galaxy flow that the event applies to.
         *
         * @return an {@link Optional} containing the flow identifier, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<String> flowId() {
            return Optional.ofNullable(flowId);
        }

        /**
         * Returns the identifier of the stanza that triggered this event.
         *
         * @return an {@link Optional} containing the stanza identifier, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<String> stanzaId() {
            return Optional.ofNullable(stanzaId);
        }

        /**
         * Sets the lifecycle event type reported by this payload.
         *
         * @param type the new action type, may be {@code null}
         */
        public void setType(GalaxyFlowActionType type) {
            this.type = type;
    }

        /**
         * Sets the identifier of the Galaxy flow that the event applies to.
         *
         * @param flowId the new flow identifier, may be {@code null}
         */
        public void setFlowId(String flowId) {
            this.flowId = flowId;
    }

        /**
         * Sets the identifier of the triggering stanza.
         *
         * @param stanzaId the new stanza identifier, may be {@code null}
         */
        public void setStanzaId(String stanzaId) {
            this.stanzaId = stanzaId;
    }

        /**
         * Enumerates the lifecycle events that can be reported through a
         * {@link GalaxyFlowAction}.
         */
        @ProtobufEnum(name = "Message.PeerDataOperationRequestMessage.GalaxyFlowAction.GalaxyFlowActionType")
        public static enum GalaxyFlowActionType {
            /**
             * Signals that the Galaxy flow has been launched on the sender's
             * device and is now active.
             */
            NOTIFY_LAUNCH(1);

            /**
             * Constructs a new constant with the given protobuf wire index.
             *
             * @param index the numeric index used on the wire
             */
            GalaxyFlowActionType(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The numeric index used to represent this constant on the
             * protobuf wire.
             */
            final int index;

            /**
             * Returns the protobuf wire index associated with this constant.
             *
             * @return the numeric index used on the wire
             */
            public int index() {
                return this.index;
            }
        }
    }

    /**
     * Asks the peer to regenerate or resend a specific history sync chunk
     * that failed to arrive or failed to decode.
     *
     * <p>The request is identified by the sync type and chunk order of the
     * original notification together with the identifier that the peer used
     * when it first announced the chunk.
     */
    @ProtobufMessage(name = "Message.PeerDataOperationRequestMessage.HistorySyncChunkRetryRequest")
    public static final class HistorySyncChunkRetryRequest {
        /**
         * The flavour of sync the missing chunk belongs to.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        HistorySyncType syncType;

        /**
         * The zero-based position of the missing chunk inside its sequence.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
        Integer chunkOrder;

        /**
         * The notification identifier associated with the original chunk
         * announcement, used to correlate the retry.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String chunkNotificationId;

        /**
         * Whether the peer should recompute the chunk from scratch ({@code true})
         * or simply resend the previously generated bytes ({@code false}).
         */
        @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
        Boolean regenerateChunk;


        /**
         * Constructs a new history sync chunk retry request.
         *
         * @param syncType             the sync flavour
         * @param chunkOrder           the chunk position
         * @param chunkNotificationId  the originating notification identifier
         * @param regenerateChunk      whether to recompute the chunk from
         *                             scratch
         */
        HistorySyncChunkRetryRequest(HistorySyncType syncType, Integer chunkOrder, String chunkNotificationId, Boolean regenerateChunk) {
            this.syncType = syncType;
            this.chunkOrder = chunkOrder;
            this.chunkNotificationId = chunkNotificationId;
            this.regenerateChunk = regenerateChunk;
        }

        /**
         * Returns the sync flavour the missing chunk belongs to.
         *
         * @return an {@link Optional} containing the sync type, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<HistorySyncType> syncType() {
            return Optional.ofNullable(syncType);
        }

        /**
         * Returns the zero-based position of the missing chunk.
         *
         * @return an {@link OptionalInt} containing the chunk order, or
         *         {@link OptionalInt#empty()} if it was not provided
         */
        public OptionalInt chunkOrder() {
            return chunkOrder == null ? OptionalInt.empty() : OptionalInt.of(chunkOrder);
        }

        /**
         * Returns the notification identifier associated with the original
         * chunk announcement.
         *
         * @return an {@link Optional} containing the notification identifier,
         *         or {@link Optional#empty()} if it was not provided
         */
        public Optional<String> chunkNotificationId() {
            return Optional.ofNullable(chunkNotificationId);
        }

        /**
         * Returns whether the peer should recompute the chunk from scratch.
         *
         * @return {@code true} if a full regeneration is requested,
         *         {@code false} otherwise or when the flag was not set
         */
        public boolean regenerateChunk() {
            return regenerateChunk != null && regenerateChunk;
        }

        /**
         * Sets the sync flavour the missing chunk belongs to.
         *
         * @param syncType the new sync type, may be {@code null}
         */
        public void setSyncType(HistorySyncType syncType) {
            this.syncType = syncType;
    }

        /**
         * Sets the zero-based position of the missing chunk.
         *
         * @param chunkOrder the new chunk order, may be {@code null}
         */
        public void setChunkOrder(Integer chunkOrder) {
            this.chunkOrder = chunkOrder;
    }

        /**
         * Sets the notification identifier associated with the original chunk
         * announcement.
         *
         * @param chunkNotificationId the new notification identifier, may be
         *                            {@code null}
         */
        public void setChunkNotificationId(String chunkNotificationId) {
            this.chunkNotificationId = chunkNotificationId;
    }

        /**
         * Sets whether the peer should recompute the chunk from scratch.
         *
         * @param regenerateChunk the new regenerate flag, may be {@code null}
         */
        public void setRegenerateChunk(Boolean regenerateChunk) {
            this.regenerateChunk = regenerateChunk;
    }
    }

    /**
     * Asks the peer to deliver an additional slice of older messages for a
     * specific chat.
     *
     * <p>The request identifies the chat by its JID and delimits the desired
     * window by the oldest message currently known on the requesting device;
     * the peer will reply with messages older than that boundary.
     */
    @ProtobufMessage(name = "Message.PeerDataOperationRequestMessage.HistorySyncOnDemandRequest")
    public static final class HistorySyncOnDemandRequest {
        /**
         * The JID of the chat whose history should be extended.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid chatJid;

        /**
         * The identifier of the oldest message currently held locally, which
         * acts as the upper bound for the requested range.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String oldestMsgId;

        /**
         * Whether the oldest locally held message was sent by the requesting
         * user.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
        Boolean oldestMsgFromMe;

        /**
         * The number of additional older messages the requester would like to
         * receive.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.INT32)
        Integer onDemandMsgCount;

        /**
         * The timestamp of the oldest locally held message, used as a
         * fallback boundary when the message identifier is not sufficient.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
        Instant oldestMsgTimestampMs;

        /**
         * The LID of the requesting account, used when the chat must be
         * looked up under the account's LID identity.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        String accountLid;


        /**
         * Constructs a new on-demand history sync request body.
         *
         * @param chatJid               the chat JID
         * @param oldestMsgId           the locally known oldest message id
         * @param oldestMsgFromMe       whether that message was sent by the
         *                              user
         * @param onDemandMsgCount      the number of additional messages
         *                              wanted
         * @param oldestMsgTimestampMs  the timestamp of the oldest local
         *                              message
         * @param accountLid            the requester's LID
         */
        HistorySyncOnDemandRequest(Jid chatJid, String oldestMsgId, Boolean oldestMsgFromMe, Integer onDemandMsgCount, Instant oldestMsgTimestampMs, String accountLid) {
            this.chatJid = chatJid;
            this.oldestMsgId = oldestMsgId;
            this.oldestMsgFromMe = oldestMsgFromMe;
            this.onDemandMsgCount = onDemandMsgCount;
            this.oldestMsgTimestampMs = oldestMsgTimestampMs;
            this.accountLid = accountLid;
        }

        /**
         * Returns the JID of the chat whose history should be extended.
         *
         * @return an {@link Optional} containing the chat JID, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<Jid> chatJid() {
            return Optional.ofNullable(chatJid);
        }

        /**
         * Returns the identifier of the oldest message currently held
         * locally.
         *
         * @return an {@link Optional} containing the message identifier, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<String> oldestMsgId() {
            return Optional.ofNullable(oldestMsgId);
        }

        /**
         * Returns whether the oldest locally held message was sent by the
         * requesting user.
         *
         * @return {@code true} if the message was sent by the user,
         *         {@code false} otherwise or when the flag was not set
         */
        public boolean oldestMsgFromMe() {
            return oldestMsgFromMe != null && oldestMsgFromMe;
        }

        /**
         * Returns the number of additional older messages requested.
         *
         * @return an {@link OptionalInt} containing the message count, or
         *         {@link OptionalInt#empty()} if it was not provided
         */
        public OptionalInt onDemandMsgCount() {
            return onDemandMsgCount == null ? OptionalInt.empty() : OptionalInt.of(onDemandMsgCount);
        }

        /**
         * Returns the timestamp of the oldest locally held message.
         *
         * @return an {@link Optional} containing the timestamp, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<Instant> oldestMsgTimestampMs() {
            return Optional.ofNullable(oldestMsgTimestampMs);
        }

        /**
         * Returns the LID of the requesting account.
         *
         * @return an {@link Optional} containing the account LID, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<String> accountLid() {
            return Optional.ofNullable(accountLid);
        }

        /**
         * Sets the JID of the chat whose history should be extended.
         *
         * @param chatJid the new chat JID, may be {@code null}
         */
        public void setChatJid(Jid chatJid) {
            this.chatJid = chatJid;
    }

        /**
         * Sets the identifier of the oldest locally held message.
         *
         * @param oldestMsgId the new message identifier, may be {@code null}
         */
        public void setOldestMsgId(String oldestMsgId) {
            this.oldestMsgId = oldestMsgId;
    }

        /**
         * Sets whether the oldest locally held message was sent by the user.
         *
         * @param oldestMsgFromMe the new flag, may be {@code null}
         */
        public void setOldestMsgFromMe(Boolean oldestMsgFromMe) {
            this.oldestMsgFromMe = oldestMsgFromMe;
    }

        /**
         * Sets the number of additional older messages requested.
         *
         * @param onDemandMsgCount the new message count, may be {@code null}
         */
        public void setOnDemandMsgCount(Integer onDemandMsgCount) {
            this.onDemandMsgCount = onDemandMsgCount;
    }

        /**
         * Sets the timestamp of the oldest locally held message.
         *
         * @param oldestMsgTimestampMs the new timestamp, may be {@code null}
         */
        public void setOldestMsgTimestampMs(Instant oldestMsgTimestampMs) {
            this.oldestMsgTimestampMs = oldestMsgTimestampMs;
    }

        /**
         * Sets the LID of the requesting account.
         *
         * @param accountLid the new account LID, may be {@code null}
         */
        public void setAccountLid(String accountLid) {
            this.accountLid = accountLid;
    }
    }

    /**
     * Asks the peer to resend a message that was received only as a
     * placeholder stub.
     *
     * <p>Placeholder stubs occur when the server delivered metadata for a
     * message but the ciphertext could not be decrypted locally (for example
     * after a missed retry). The request identifies the original message by
     * its {@link MessageKey} so the peer can look up the plaintext and send
     * it back.
     */
    @ProtobufMessage(name = "Message.PeerDataOperationRequestMessage.PlaceholderMessageResendRequest")
    public static final class PlaceholderMessageResendRequest {
        /**
         * The key that uniquely identifies the message to be resent.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        MessageKey messageKey;


        /**
         * Constructs a new placeholder message resend request.
         *
         * @param messageKey the key of the message to be resent
         */
        PlaceholderMessageResendRequest(MessageKey messageKey) {
            this.messageKey = messageKey;
        }

        /**
         * Returns the key of the message to be resent.
         *
         * @return an {@link Optional} containing the message key, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<MessageKey> messageKey() {
            return Optional.ofNullable(messageKey);
        }

        /**
         * Sets the key of the message to be resent.
         *
         * @param messageKey the new message key, may be {@code null}
         */
        public void setMessageKey(MessageKey messageKey) {
            this.messageKey = messageKey;
    }
    }

    /**
     * Asks the peer to reupload a sticker whose media has expired on the
     * WhatsApp media CDN.
     *
     * <p>The sticker is identified by the SHA-256 of its file so that the
     * peer can locate the local copy in its sticker cache and perform the
     * reupload on the requester's behalf.
     */
    @ProtobufMessage(name = "Message.PeerDataOperationRequestMessage.RequestStickerReupload")
    public static final class RequestStickerReupload {
        /**
         * The SHA-256 digest of the sticker file that should be reuploaded.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String fileSha256;


        /**
         * Constructs a new sticker reupload request.
         *
         * @param fileSha256 the sticker file digest
         */
        RequestStickerReupload(String fileSha256) {
            this.fileSha256 = fileSha256;
        }

        /**
         * Returns the SHA-256 digest of the sticker file to reupload.
         *
         * @return an {@link Optional} containing the digest, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<String> fileSha256() {
            return Optional.ofNullable(fileSha256);
        }

        /**
         * Sets the SHA-256 digest of the sticker file to reupload.
         *
         * @param fileSha256 the new digest, may be {@code null}
         */
        public void setFileSha256(String fileSha256) {
            this.fileSha256 = fileSha256;
    }
    }

    /**
     * Asks the peer to fetch and return a preview for a URL that the
     * requesting device cannot resolve on its own.
     *
     * <p>The peer performs the HTTP fetch and extracts the standard preview
     * fields (title, description, thumbnail). The requester can ask for a
     * high-quality thumbnail via {@link #includeHqThumbnail()}, which is
     * more expensive to compute.
     */
    @ProtobufMessage(name = "Message.PeerDataOperationRequestMessage.RequestUrlPreview")
    public static final class RequestUrlPreview {
        /**
         * The URL whose preview should be generated.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String url;

        /**
         * Whether the preview should include a high-quality thumbnail
         * suitable for large-format rendering.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
        Boolean includeHqThumbnail;


        /**
         * Constructs a new URL preview request.
         *
         * @param url                the target URL
         * @param includeHqThumbnail whether to include a high-quality
         *                           thumbnail
         */
        RequestUrlPreview(String url, Boolean includeHqThumbnail) {
            this.url = url;
            this.includeHqThumbnail = includeHqThumbnail;
        }

        /**
         * Returns the URL whose preview should be generated.
         *
         * @return an {@link Optional} containing the URL, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<String> url() {
            return Optional.ofNullable(url);
        }

        /**
         * Returns whether the preview should include a high-quality
         * thumbnail.
         *
         * @return {@code true} if a high-quality thumbnail is requested,
         *         {@code false} otherwise or when the flag was not set
         */
        public boolean includeHqThumbnail() {
            return includeHqThumbnail != null && includeHqThumbnail;
        }

        /**
         * Sets the URL whose preview should be generated.
         *
         * @param url the new URL, may be {@code null}
         */
        public void setUrl(String url) {
            this.url = url;
    }

        /**
         * Sets whether the preview should include a high-quality thumbnail.
         *
         * @param includeHqThumbnail the new flag, may be {@code null}
         */
        public void setIncludeHqThumbnail(Boolean includeHqThumbnail) {
            this.includeHqThumbnail = includeHqThumbnail;
    }
    }

    /**
     * Asks the peer to resync an app-state collection after the requesting
     * device has suffered a fatal desynchronisation.
     *
     * <p>App-state collections (starred messages, contacts, mutes, ...) are
     * normally kept in step via incremental patches. When the incremental
     * protocol enters an unrecoverable state the requester asks the peer for
     * a full snapshot; this payload identifies which collection to resync
     * and reports the timestamp at which the failure was observed.
     */
    @ProtobufMessage(name = "Message.PeerDataOperationRequestMessage.SyncDCollectionFatalRecoveryRequest")
    public static final class SyncDCollectionFatalRecoveryRequest {
        /**
         * The name of the app-state collection that must be resynced.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String collectionName;

        /**
         * The timestamp at which the fatal desynchronisation was detected.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
        Instant timestamp;


        /**
         * Constructs a new fatal recovery request.
         *
         * @param collectionName the name of the failing collection
         * @param timestamp      the time of failure
         */
        SyncDCollectionFatalRecoveryRequest(String collectionName, Instant timestamp) {
            this.collectionName = collectionName;
            this.timestamp = timestamp;
        }

        /**
         * Returns the name of the app-state collection to resync.
         *
         * @return an {@link Optional} containing the collection name, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<String> collectionName() {
            return Optional.ofNullable(collectionName);
        }

        /**
         * Returns the timestamp at which the fatal desync was detected.
         *
         * @return an {@link Optional} containing the timestamp, or
         *         {@link Optional#empty()} if it was not provided
         */
        public Optional<Instant> timestamp() {
            return Optional.ofNullable(timestamp);
        }

        /**
         * Sets the name of the app-state collection to resync.
         *
         * @param collectionName the new collection name, may be {@code null}
         */
        public void setCollectionName(String collectionName) {
            this.collectionName = collectionName;
    }

        /**
         * Sets the timestamp at which the fatal desync was detected.
         *
         * @param timestamp the new timestamp, may be {@code null}
         */
        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
    }
    }
}

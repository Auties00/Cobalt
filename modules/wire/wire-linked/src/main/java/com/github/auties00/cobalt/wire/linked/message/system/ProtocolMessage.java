package com.github.auties00.cobalt.wire.linked.message.system;

import com.github.auties00.cobalt.wire.linked.bot.ai.AIQueryFanout;
import com.github.auties00.cobalt.wire.linked.bot.feedback.BotFeedbackMessage;
import com.github.auties00.cobalt.wire.linked.chat.ChatDisappearingMode;
import com.github.auties00.cobalt.wire.linked.chat.ChatLimitSharing;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipantLabel;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMappingSyncMessage;
import com.github.auties00.cobalt.wire.linked.media.MediaNotifyMessage;
import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateFatalExceptionNotification;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyRequest;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyShare;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncNotification;
import com.github.auties00.cobalt.wire.linked.message.system.peer.PeerDataOperationRequestMessage;
import com.github.auties00.cobalt.wire.linked.message.system.peer.PeerDataOperationRequestResponseMessage;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A multiplexed envelope for all system-level, non-user-visible messages
 * that the WhatsApp protocol exchanges inside the conversation pipeline.
 *
 * <p>While regular chat messages carry text or media, a large number of
 * internal events ride on the same end-to-end-encrypted message
 * infrastructure: revocations, message edits, disappearing-message setting
 * changes, history-sync notifications, app-state sync key exchanges,
 * peer-device data operations, bot feedback, media-ready notifications,
 * Cloud API thread-control handoffs, LID migration syncs, and a growing
 * set of AI and community-related flows. Each such event is carried as a
 * {@code ProtocolMessage} whose {@link Type} field disambiguates the
 * active variant and tells the reader which of the sibling fields to
 * inspect.
 *
 * <p>Most fields of this class are mutually exclusive in practice: only
 * the one matching the {@link Type} carries data. Consumers should always
 * branch on {@link #type()} first and then read the single relevant
 * accessor.
 */
@ProtobufMessage(name = "Message.ProtocolMessage")
public final class ProtocolMessage implements Message {
    /**
     * The {@link MessageKey} of the message this protocol event refers to,
     * used for example when revoking or editing a previously sent message.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;

    /**
     * The variant discriminator identifying which protocol event this
     * message carries. Consumers should branch on this value before
     * reading any of the payload fields.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    Type type;

    /**
     * The new ephemeral-message expiration, expressed as a duration in
     * seconds, when the event is {@link Type#EPHEMERAL_SETTING}. A value
     * of {@code 0} disables the timer; otherwise the value indicates how
     * long messages should live before being deleted automatically.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    Integer ephemeralExpiration;

    /**
     * The timestamp at which the ephemeral-message setting was last
     * modified, used by clients to resolve concurrent setting updates.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant ephemeralSettingTimestamp;

    /**
     * The attached payload for a {@link Type#HISTORY_SYNC_NOTIFICATION}
     * event, indicating that a new history-sync blob is available for
     * download by the recipient device.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    HistorySyncNotification historySyncNotification;

    /**
     * The attached payload for an {@link Type#APP_STATE_SYNC_KEY_SHARE}
     * event, carrying the keys delivered to the requester.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    AppStateSyncKeyShare appStateSyncKeyShare;

    /**
     * The attached payload for an {@link Type#APP_STATE_SYNC_KEY_REQUEST}
     * event, enumerating which keys the sender is missing and would like
     * to receive from peer devices.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    AppStateSyncKeyRequest appStateSyncKeyRequest;

    /**
     * The attached payload for an
     * {@link Type#INITIAL_SECURITY_NOTIFICATION_SETTING_SYNC} event, used
     * when synchronising the initial value of the security-notification
     * preference to a newly linked device.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    InitialSecurityNotificationSettingSync initialSecurityNotificationSettingSync;

    /**
     * The attached payload for an
     * {@link Type#APP_STATE_FATAL_EXCEPTION_NOTIFICATION} event,
     * describing a non-recoverable app-state sync failure that requires
     * user intervention.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    AppStateFatalExceptionNotification appStateFatalExceptionNotification;

    /**
     * The disappearing-message configuration carried by
     * {@link Type#EPHEMERAL_SETTING} events, encoding both the new timer
     * value and the initiator of the change.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.MESSAGE)
    ChatDisappearingMode disappearingMode;

    /**
     * The replacement payload for a {@link Type#MESSAGE_EDIT} event,
     * holding the edited body that must replace the message identified by
     * {@link #key}.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.MESSAGE)
    LinkedMessageContainer editedMessageContainer;

    /**
     * The timestamp, in milliseconds, at which this protocol event was
     * issued, used for ordering and conflict resolution.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant timestampMs;

    /**
     * The attached payload for a
     * {@link Type#PEER_DATA_OPERATION_REQUEST_MESSAGE} event, describing
     * an operation that a peer device should perform on the user's behalf
     * (for example a historical message lookup).
     */
    @ProtobufProperty(index = 16, type = ProtobufType.MESSAGE)
    PeerDataOperationRequestMessage peerDataOperationRequestMessage;

    /**
     * The attached payload for a
     * {@link Type#PEER_DATA_OPERATION_REQUEST_RESPONSE_MESSAGE} event,
     * delivering the result of a previously requested peer data
     * operation.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    PeerDataOperationRequestResponseMessage peerDataOperationRequestResponseMessage;

    /**
     * The attached payload for a {@link Type#BOT_FEEDBACK_MESSAGE} event,
     * carrying user feedback (such as a thumbs-up/down rating) about a
     * previous bot response.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.MESSAGE)
    BotFeedbackMessage botFeedbackMessage;

    /**
     * The {@link Jid} of the participant that invoked this protocol event,
     * when relevant (for example, the user who triggered an AI flow or a
     * group action).
     */
    @ProtobufProperty(index = 19, type = ProtobufType.STRING)
    Jid invokerJid;

    /**
     * The attached payload for a {@link Type#REQUEST_WELCOME_MESSAGE}
     * event, requesting the delivery of a welcome message for a chat.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.MESSAGE)
    RequestWelcomeMessageMetadata requestWelcomeMessageMetadata;

    /**
     * The attached payload for a {@link Type#MEDIA_NOTIFY_MESSAGE} event,
     * notifying peer devices that a media upload has completed and is
     * ready for download.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.MESSAGE)
    MediaNotifyMessage mediaNotifyMessage;

    /**
     * The attached payload for a
     * {@link Type#CLOUD_API_THREAD_CONTROL_NOTIFICATION} event,
     * communicating a Cloud API thread-control handoff between
     * integrations.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.MESSAGE)
    CloudAPIThreadControlNotification cloudApiThreadControlNotification;

    /**
     * The attached payload for a {@link Type#LID_MIGRATION_MAPPING_SYNC}
     * event, informing devices of newly created LID-to-phone-number
     * mappings.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.MESSAGE)
    LIDMigrationMappingSyncMessage lidMigrationMappingSyncMessage;

    /**
     * The attached payload for a {@link Type#LIMIT_SHARING} event,
     * describing forwarding and sharing restrictions applied to a chat.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.MESSAGE)
    ChatLimitSharing limitSharing;

    /**
     * Raw AI private-set-intersection metadata carried by the
     * {@link Type#AI_PSI_METADATA} event. The bytes are opaque to the
     * client and forwarded verbatim between peers.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.BYTES)
    byte[] aiPsiMetadata;

    /**
     * The attached payload for an {@link Type#AI_QUERY_FANOUT} event,
     * fanning out an AI query to multiple endpoints.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.MESSAGE)
    AIQueryFanout aiQueryFanout;

    /**
     * The attached payload for a {@link Type#GROUP_MEMBER_LABEL_CHANGE}
     * event, recording a label change applied to a group member.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.MESSAGE)
    GroupParticipantLabel participantLabel;


    /**
     * Constructs a new protocol message. All fields are optional and only
     * the subset matching the event {@link Type} is expected to carry
     * data.
     *
     * @param key                                       the target message key, or {@code null}
     * @param type                                      the protocol event discriminator, or {@code null}
     * @param ephemeralExpiration                       the new ephemeral expiration in seconds, or {@code null}
     * @param ephemeralSettingTimestamp                 the ephemeral setting update timestamp, or {@code null}
     * @param historySyncNotification                   the history-sync payload, or {@code null}
     * @param appStateSyncKeyShare                      the app-state key share payload, or {@code null}
     * @param appStateSyncKeyRequest                    the app-state key request payload, or {@code null}
     * @param initialSecurityNotificationSettingSync    the initial security-notification sync payload, or {@code null}
     * @param appStateFatalExceptionNotification        the app-state fatal exception payload, or {@code null}
     * @param disappearingMode                          the disappearing-mode configuration, or {@code null}
     * @param editedMessageContainer                    the replacement payload for an edit event, or {@code null}
     * @param timestampMs                               the event timestamp in milliseconds, or {@code null}
     * @param peerDataOperationRequestMessage           the peer data operation request payload, or {@code null}
     * @param peerDataOperationRequestResponseMessage   the peer data operation response payload, or {@code null}
     * @param botFeedbackMessage                        the bot feedback payload, or {@code null}
     * @param invokerJid                                the invoker JID, or {@code null}
     * @param requestWelcomeMessageMetadata             the request welcome message metadata, or {@code null}
     * @param mediaNotifyMessage                        the media-notify payload, or {@code null}
     * @param cloudApiThreadControlNotification         the Cloud API thread-control notification payload, or {@code null}
     * @param lidMigrationMappingSyncMessage            the LID migration mapping sync payload, or {@code null}
     * @param limitSharing                              the chat limit-sharing configuration, or {@code null}
     * @param aiPsiMetadata                             the raw AI PSI metadata, or {@code null}
     * @param aiQueryFanout                             the AI query fanout payload, or {@code null}
     * @param participantLabel                          the group-participant label payload, or {@code null}
     */
    ProtocolMessage(MessageKey key, Type type, Integer ephemeralExpiration, Instant ephemeralSettingTimestamp, HistorySyncNotification historySyncNotification, AppStateSyncKeyShare appStateSyncKeyShare, AppStateSyncKeyRequest appStateSyncKeyRequest, InitialSecurityNotificationSettingSync initialSecurityNotificationSettingSync, AppStateFatalExceptionNotification appStateFatalExceptionNotification, ChatDisappearingMode disappearingMode, LinkedMessageContainer editedMessageContainer, Instant timestampMs, PeerDataOperationRequestMessage peerDataOperationRequestMessage, PeerDataOperationRequestResponseMessage peerDataOperationRequestResponseMessage, BotFeedbackMessage botFeedbackMessage, Jid invokerJid, RequestWelcomeMessageMetadata requestWelcomeMessageMetadata, MediaNotifyMessage mediaNotifyMessage, CloudAPIThreadControlNotification cloudApiThreadControlNotification, LIDMigrationMappingSyncMessage lidMigrationMappingSyncMessage, ChatLimitSharing limitSharing, byte[] aiPsiMetadata, AIQueryFanout aiQueryFanout, GroupParticipantLabel participantLabel) {
        this.key = key;
        this.type = type;
        this.ephemeralExpiration = ephemeralExpiration;
        this.ephemeralSettingTimestamp = ephemeralSettingTimestamp;
        this.historySyncNotification = historySyncNotification;
        this.appStateSyncKeyShare = appStateSyncKeyShare;
        this.appStateSyncKeyRequest = appStateSyncKeyRequest;
        this.initialSecurityNotificationSettingSync = initialSecurityNotificationSettingSync;
        this.appStateFatalExceptionNotification = appStateFatalExceptionNotification;
        this.disappearingMode = disappearingMode;
        this.editedMessageContainer = editedMessageContainer;
        this.timestampMs = timestampMs;
        this.peerDataOperationRequestMessage = peerDataOperationRequestMessage;
        this.peerDataOperationRequestResponseMessage = peerDataOperationRequestResponseMessage;
        this.botFeedbackMessage = botFeedbackMessage;
        this.invokerJid = invokerJid;
        this.requestWelcomeMessageMetadata = requestWelcomeMessageMetadata;
        this.mediaNotifyMessage = mediaNotifyMessage;
        this.cloudApiThreadControlNotification = cloudApiThreadControlNotification;
        this.lidMigrationMappingSyncMessage = lidMigrationMappingSyncMessage;
        this.limitSharing = limitSharing;
        this.aiPsiMetadata = aiPsiMetadata;
        this.aiQueryFanout = aiQueryFanout;
        this.participantLabel = participantLabel;
    }

    /**
     * Returns the {@link MessageKey} targeted by this protocol event.
     *
     * @return an {@link Optional} containing the key, or
     *         {@link Optional#empty()} if no key is set
     */
    public Optional<MessageKey> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the variant discriminator identifying which protocol event
     * this message carries.
     *
     * @return an {@link Optional} containing the event type, or
     *         {@link Optional#empty()} if no type is set
     */
    public Optional<Type> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the new ephemeral-message expiration, in seconds, when the
     * event is {@link Type#EPHEMERAL_SETTING}.
     *
     * @return an {@link OptionalInt} containing the expiration, or
     *         {@link OptionalInt#empty()} if it is not set
     */
    public OptionalInt ephemeralExpiration() {
        return ephemeralExpiration == null ? OptionalInt.empty() : OptionalInt.of(ephemeralExpiration);
    }

    /**
     * Returns the timestamp at which the ephemeral-message setting was
     * last modified.
     *
     * @return an {@link Optional} containing the timestamp, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<Instant> ephemeralSettingTimestamp() {
        return Optional.ofNullable(ephemeralSettingTimestamp);
    }

    /**
     * Returns the history-sync payload attached to this message.
     *
     * @return an {@link Optional} containing the {@link HistorySyncNotification},
     *         or {@link Optional#empty()} if no payload is set
     */
    public Optional<HistorySyncNotification> historySyncNotification() {
        return Optional.ofNullable(historySyncNotification);
    }

    /**
     * Returns the app-state sync key share payload attached to this message.
     *
     * @return an {@link Optional} containing the {@link AppStateSyncKeyShare},
     *         or {@link Optional#empty()} if no payload is set
     */
    public Optional<AppStateSyncKeyShare> appStateSyncKeyShare() {
        return Optional.ofNullable(appStateSyncKeyShare);
    }

    /**
     * Returns the app-state sync key request payload attached to this message.
     *
     * @return an {@link Optional} containing the {@link AppStateSyncKeyRequest},
     *         or {@link Optional#empty()} if no payload is set
     */
    public Optional<AppStateSyncKeyRequest> appStateSyncKeyRequest() {
        return Optional.ofNullable(appStateSyncKeyRequest);
    }

    /**
     * Returns the initial security-notification setting sync payload
     * attached to this message.
     *
     * @return an {@link Optional} containing the payload, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<InitialSecurityNotificationSettingSync> initialSecurityNotificationSettingSync() {
        return Optional.ofNullable(initialSecurityNotificationSettingSync);
    }

    /**
     * Returns the app-state fatal exception notification payload attached
     * to this message.
     *
     * @return an {@link Optional} containing the payload, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<AppStateFatalExceptionNotification> appStateFatalExceptionNotification() {
        return Optional.ofNullable(appStateFatalExceptionNotification);
    }

    /**
     * Returns the disappearing-message configuration carried by an
     * {@link Type#EPHEMERAL_SETTING} event.
     *
     * @return an {@link Optional} containing the {@link ChatDisappearingMode},
     *         or {@link Optional#empty()} if it is not set
     */
    public Optional<ChatDisappearingMode> disappearingMode() {
        return Optional.ofNullable(disappearingMode);
    }

    /**
     * Returns the replacement payload carried by a {@link Type#MESSAGE_EDIT}
     * event.
     *
     * @return an {@link Optional} containing the edited {@link LinkedMessageContainer},
     *         or {@link Optional#empty()} if no edit payload is set
     */
    public Optional<LinkedMessageContainer> editedMessage() {
        return Optional.ofNullable(editedMessageContainer);
    }

    /**
     * Returns the timestamp, in milliseconds, at which this protocol event
     * was issued.
     *
     * @return an {@link Optional} containing the timestamp, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<Instant> timestampMs() {
        return Optional.ofNullable(timestampMs);
    }

    /**
     * Returns the peer data operation request payload attached to this message.
     *
     * @return an {@link Optional} containing the payload, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<PeerDataOperationRequestMessage> peerDataOperationRequestMessage() {
        return Optional.ofNullable(peerDataOperationRequestMessage);
    }

    /**
     * Returns the peer data operation response payload attached to this message.
     *
     * @return an {@link Optional} containing the payload, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<PeerDataOperationRequestResponseMessage> peerDataOperationRequestResponseMessage() {
        return Optional.ofNullable(peerDataOperationRequestResponseMessage);
    }

    /**
     * Returns the bot feedback payload attached to this message.
     *
     * @return an {@link Optional} containing the {@link BotFeedbackMessage},
     *         or {@link Optional#empty()} if no payload is set
     */
    public Optional<BotFeedbackMessage> botFeedbackMessage() {
        return Optional.ofNullable(botFeedbackMessage);
    }

    /**
     * Returns the {@link Jid} of the participant that invoked this protocol event.
     *
     * @return an {@link Optional} containing the invoker JID, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<Jid> invokerJid() {
        return Optional.ofNullable(invokerJid);
    }

    /**
     * Returns the request-welcome-message metadata attached to this message.
     *
     * @return an {@link Optional} containing the metadata, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<RequestWelcomeMessageMetadata> requestWelcomeMessageMetadata() {
        return Optional.ofNullable(requestWelcomeMessageMetadata);
    }

    /**
     * Returns the media-notify payload attached to this message.
     *
     * @return an {@link Optional} containing the {@link MediaNotifyMessage},
     *         or {@link Optional#empty()} if no payload is set
     */
    public Optional<MediaNotifyMessage> mediaNotifyMessage() {
        return Optional.ofNullable(mediaNotifyMessage);
    }

    /**
     * Returns the Cloud API thread-control notification payload attached
     * to this message.
     *
     * @return an {@link Optional} containing the notification, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<CloudAPIThreadControlNotification> cloudApiThreadControlNotification() {
        return Optional.ofNullable(cloudApiThreadControlNotification);
    }

    /**
     * Returns the LID migration mapping sync payload attached to this message.
     *
     * @return an {@link Optional} containing the payload, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<LIDMigrationMappingSyncMessage> lidMigrationMappingSyncMessage() {
        return Optional.ofNullable(lidMigrationMappingSyncMessage);
    }

    /**
     * Returns the chat limit-sharing configuration attached to this message.
     *
     * @return an {@link Optional} containing the {@link ChatLimitSharing},
     *         or {@link Optional#empty()} if it is not set
     */
    public Optional<ChatLimitSharing> limitSharing() {
        return Optional.ofNullable(limitSharing);
    }

    /**
     * Returns the raw AI private-set-intersection metadata carried by this
     * message.
     *
     * @return an {@link Optional} containing the raw metadata bytes, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<byte[]> aiPsiMetadata() {
        return Optional.ofNullable(aiPsiMetadata);
    }

    /**
     * Returns the AI query fanout payload attached to this message.
     *
     * @return an {@link Optional} containing the {@link AIQueryFanout}, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<AIQueryFanout> aiQueryFanout() {
        return Optional.ofNullable(aiQueryFanout);
    }

    /**
     * Returns the group-participant label payload attached to this message.
     *
     * @return an {@link Optional} containing the {@link GroupParticipantLabel},
     *         or {@link Optional#empty()} if it is not set
     */
    public Optional<GroupParticipantLabel> memberLabel() {
        return Optional.ofNullable(participantLabel);
    }

    /**
     * Sets the {@link MessageKey} targeted by this protocol event.
     *
     * @param key the new key, or {@code null} to clear it
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }

    /**
     * Sets the variant discriminator identifying which protocol event this
     * message carries.
     *
     * @param type the new event type, or {@code null} to clear it
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Sets the new ephemeral-message expiration, in seconds.
     *
     * @param ephemeralExpiration the new expiration, or {@code null} to clear it
     */
    public void setEphemeralExpiration(Integer ephemeralExpiration) {
        this.ephemeralExpiration = ephemeralExpiration;
    }

    /**
     * Sets the timestamp at which the ephemeral-message setting was last
     * modified.
     *
     * @param ephemeralSettingTimestamp the new timestamp, or {@code null} to clear it
     */
    public void setEphemeralSettingTimestamp(Instant ephemeralSettingTimestamp) {
        this.ephemeralSettingTimestamp = ephemeralSettingTimestamp;
    }

    /**
     * Sets the history-sync payload attached to this message.
     *
     * @param historySyncNotification the new payload, or {@code null} to clear it
     */
    public void setHistorySyncNotification(HistorySyncNotification historySyncNotification) {
        this.historySyncNotification = historySyncNotification;
    }

    /**
     * Sets the app-state sync key share payload attached to this message.
     *
     * @param appStateSyncKeyShare the new payload, or {@code null} to clear it
     */
    public void setAppStateSyncKeyShare(AppStateSyncKeyShare appStateSyncKeyShare) {
        this.appStateSyncKeyShare = appStateSyncKeyShare;
    }

    /**
     * Sets the app-state sync key request payload attached to this message.
     *
     * @param appStateSyncKeyRequest the new payload, or {@code null} to clear it
     */
    public void setAppStateSyncKeyRequest(AppStateSyncKeyRequest appStateSyncKeyRequest) {
        this.appStateSyncKeyRequest = appStateSyncKeyRequest;
    }

    /**
     * Sets the initial security-notification setting sync payload attached
     * to this message.
     *
     * @param initialSecurityNotificationSettingSync the new payload, or {@code null} to clear it
     */
    public void setInitialSecurityNotificationSettingSync(InitialSecurityNotificationSettingSync initialSecurityNotificationSettingSync) {
        this.initialSecurityNotificationSettingSync = initialSecurityNotificationSettingSync;
    }

    /**
     * Sets the app-state fatal exception notification payload attached to
     * this message.
     *
     * @param appStateFatalExceptionNotification the new payload, or {@code null} to clear it
     */
    public void setAppStateFatalExceptionNotification(AppStateFatalExceptionNotification appStateFatalExceptionNotification) {
        this.appStateFatalExceptionNotification = appStateFatalExceptionNotification;
    }

    /**
     * Sets the disappearing-message configuration.
     *
     * @param disappearingMode the new configuration, or {@code null} to clear it
     */
    public void setDisappearingMode(ChatDisappearingMode disappearingMode) {
        this.disappearingMode = disappearingMode;
    }

    /**
     * Sets the replacement payload for a {@link Type#MESSAGE_EDIT} event.
     *
     * @param editedMessageContainer the new edited message container, or {@code null} to clear it
     */
    public void setEditedMessage(LinkedMessageContainer editedMessageContainer) {
        this.editedMessageContainer = editedMessageContainer;
    }

    /**
     * Sets the timestamp, in milliseconds, at which this protocol event
     * was issued.
     *
     * @param timestampMs the new timestamp, or {@code null} to clear it
     */
    public void setTimestampMs(Instant timestampMs) {
        this.timestampMs = timestampMs;
    }

    /**
     * Sets the peer data operation request payload.
     *
     * @param peerDataOperationRequestMessage the new payload, or {@code null} to clear it
     */
    public void setPeerDataOperationRequestMessage(PeerDataOperationRequestMessage peerDataOperationRequestMessage) {
        this.peerDataOperationRequestMessage = peerDataOperationRequestMessage;
    }

    /**
     * Sets the peer data operation response payload.
     *
     * @param peerDataOperationRequestResponseMessage the new payload, or {@code null} to clear it
     */
    public void setPeerDataOperationRequestResponseMessage(PeerDataOperationRequestResponseMessage peerDataOperationRequestResponseMessage) {
        this.peerDataOperationRequestResponseMessage = peerDataOperationRequestResponseMessage;
    }

    /**
     * Sets the bot feedback payload.
     *
     * @param botFeedbackMessage the new payload, or {@code null} to clear it
     */
    public void setBotFeedbackMessage(BotFeedbackMessage botFeedbackMessage) {
        this.botFeedbackMessage = botFeedbackMessage;
    }

    /**
     * Sets the {@link Jid} of the participant that invoked this protocol event.
     *
     * @param invokerJid the new invoker JID, or {@code null} to clear it
     */
    public void setInvokerJid(Jid invokerJid) {
        this.invokerJid = invokerJid;
    }

    /**
     * Sets the request-welcome-message metadata.
     *
     * @param requestWelcomeMessageMetadata the new metadata, or {@code null} to clear it
     */
    public void setRequestWelcomeMessageMetadata(RequestWelcomeMessageMetadata requestWelcomeMessageMetadata) {
        this.requestWelcomeMessageMetadata = requestWelcomeMessageMetadata;
    }

    /**
     * Sets the media-notify payload.
     *
     * @param mediaNotifyMessage the new payload, or {@code null} to clear it
     */
    public void setMediaNotifyMessage(MediaNotifyMessage mediaNotifyMessage) {
        this.mediaNotifyMessage = mediaNotifyMessage;
    }

    /**
     * Sets the Cloud API thread-control notification payload.
     *
     * @param cloudApiThreadControlNotification the new payload, or {@code null} to clear it
     */
    public void setCloudApiThreadControlNotification(CloudAPIThreadControlNotification cloudApiThreadControlNotification) {
        this.cloudApiThreadControlNotification = cloudApiThreadControlNotification;
    }

    /**
     * Sets the LID migration mapping sync payload.
     *
     * @param lidMigrationMappingSyncMessage the new payload, or {@code null} to clear it
     */
    public void setLidMigrationMappingSyncMessage(LIDMigrationMappingSyncMessage lidMigrationMappingSyncMessage) {
        this.lidMigrationMappingSyncMessage = lidMigrationMappingSyncMessage;
    }

    /**
     * Sets the chat limit-sharing configuration.
     *
     * @param limitSharing the new configuration, or {@code null} to clear it
     */
    public void setLimitSharing(ChatLimitSharing limitSharing) {
        this.limitSharing = limitSharing;
    }

    /**
     * Sets the raw AI private-set-intersection metadata.
     *
     * @param aiPsiMetadata the new metadata bytes, or {@code null} to clear it
     */
    public void setAiPsiMetadata(byte[] aiPsiMetadata) {
        this.aiPsiMetadata = aiPsiMetadata;
    }

    /**
     * Sets the AI query fanout payload.
     *
     * @param aiQueryFanout the new payload, or {@code null} to clear it
     */
    public void setAiQueryFanout(AIQueryFanout aiQueryFanout) {
        this.aiQueryFanout = aiQueryFanout;
    }

    /**
     * Sets the group-participant label payload.
     *
     * @param participantLabel the new payload, or {@code null} to clear it
     */
    public void setMemberLabel(GroupParticipantLabel participantLabel) {
        this.participantLabel = participantLabel;
    }

    /**
     * Enumerates every protocol event variant that can be carried by a
     * {@link ProtocolMessage}.
     *
     * <p>The value of this enum determines which of the sibling fields on
     * the enclosing message is meaningful: all other fields are expected
     * to be empty for a given event.
     */
    @ProtobufEnum(name = "Message.ProtocolMessage.Type")
    public static enum Type {
        /**
         * Revokes (deletes for everyone) a previously sent message
         * identified by {@link ProtocolMessage#key()}.
         */
        REVOKE(0),
        /**
         * Updates the disappearing-message expiration for the conversation.
         */
        EPHEMERAL_SETTING(3),
        /**
         * Synchronises the current disappearing-message setting to peer
         * devices that requested a refresh.
         */
        EPHEMERAL_SYNC_RESPONSE(4),
        /**
         * Notifies the client that a new history-sync blob is available
         * for download.
         */
        HISTORY_SYNC_NOTIFICATION(5),
        /**
         * Delivers requested app-state sync keys to a peer device.
         */
        APP_STATE_SYNC_KEY_SHARE(6),
        /**
         * Requests specific app-state sync keys from peer devices.
         */
        APP_STATE_SYNC_KEY_REQUEST(7),
        /**
         * Requests a backfill of a message fanout to recover missed
         * messages.
         */
        MSG_FANOUT_BACKFILL_REQUEST(8),
        /**
         * Synchronises the initial value of the security-notification
         * preference to newly linked devices.
         */
        INITIAL_SECURITY_NOTIFICATION_SETTING_SYNC(9),
        /**
         * Signals that an unrecoverable app-state sync failure has
         * occurred.
         */
        APP_STATE_FATAL_EXCEPTION_NOTIFICATION(10),
        /**
         * Shares the sender's phone number with the recipient.
         */
        SHARE_PHONE_NUMBER(11),
        /**
         * Replaces the body of a previously sent message.
         */
        MESSAGE_EDIT(14),
        /**
         * Requests a peer device to perform a data operation such as
         * historical message lookup.
         */
        PEER_DATA_OPERATION_REQUEST_MESSAGE(16),
        /**
         * Delivers the response to a peer data operation request.
         */
        PEER_DATA_OPERATION_REQUEST_RESPONSE_MESSAGE(17),
        /**
         * Requests the delivery of a welcome message for a chat.
         */
        REQUEST_WELCOME_MESSAGE(18),
        /**
         * Carries user feedback about a bot response.
         */
        BOT_FEEDBACK_MESSAGE(19),
        /**
         * Notifies peer devices that media has been uploaded and is
         * ready for download.
         */
        MEDIA_NOTIFY_MESSAGE(20),
        /**
         * Communicates a Cloud API thread-control handoff between
         * integrations.
         */
        CLOUD_API_THREAD_CONTROL_NOTIFICATION(21),
        /**
         * Distributes a newly created LID-to-phone-number mapping.
         */
        LID_MIGRATION_MAPPING_SYNC(22),
        /**
         * Carries a scheduled reminder message.
         */
        REMINDER_MESSAGE(23),
        /**
         * Delivers onboarding content for the bot mem-u experience.
         */
        BOT_MEMU_ONBOARDING_MESSAGE(24),
        /**
         * Notifies peers that a status post has been mentioned.
         */
        STATUS_MENTION_MESSAGE(25),
        /**
         * Requests that the receiving bot stop generating its current
         * response.
         */
        STOP_GENERATION_MESSAGE(26),
        /**
         * Applies a forwarding and sharing limit to a chat.
         */
        LIMIT_SHARING(27),
        /**
         * Carries AI private-set-intersection metadata.
         */
        AI_PSI_METADATA(28),
        /**
         * Fans out an AI query to multiple endpoints.
         */
        AI_QUERY_FANOUT(29),
        /**
         * Records a label change on a group member.
         */
        GROUP_MEMBER_LABEL_CHANGE(30);

        /**
         * Constructs a new enum constant with the given protobuf index.
         *
         * @param index the protobuf wire index for this constant
         */
        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index associated with this constant.
         *
         * @return the protobuf wire index
         */
        public int index() {
            return this.index;
        }
    }
}

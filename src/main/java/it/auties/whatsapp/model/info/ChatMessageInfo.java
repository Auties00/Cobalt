package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.business.BusinessPrivacyStatus;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.media.MediaData;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.standard.LiveLocationMessage;
import it.auties.whatsapp.model.message.standard.ReactionMessage;
import it.auties.whatsapp.model.poll.PollAdditionalMetadata;
import it.auties.whatsapp.model.poll.PollUpdate;
import it.auties.whatsapp.model.sync.PhotoChange;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Objects.requireNonNullElseGet;

/**
 * A model class that holds the information related to a {@link Message}.
 */
@ProtobufMessage(name = "WebMessageInfo")
public final class ChatMessageInfo implements MessageInfo<ChatMessageInfo>, MessageStatusInfo<ChatMessageInfo> {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    private final ChatMessageKey key;
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private MessageContainer message;
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    private final long timestampSeconds;
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    private MessageStatus status;
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    private final Jid senderJid;
    @ProtobufProperty(index = 6, type = ProtobufType.UINT64)
    private final long messageC2STimestamp;
    @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
    private boolean ignore;
    @ProtobufProperty(index = 17, type = ProtobufType.BOOL)
    private boolean starred;
    @ProtobufProperty(index = 18, type = ProtobufType.BOOL)
    private final boolean broadcast;
    @ProtobufProperty(index = 19, type = ProtobufType.STRING)
    private final String pushName;
    @ProtobufProperty(index = 20, type = ProtobufType.BYTES)
    private final byte[] mediaCiphertextSha256;
    @ProtobufProperty(index = 21, type = ProtobufType.BOOL)
    private final boolean multicast;
    @ProtobufProperty(index = 22, type = ProtobufType.BOOL)
    private final boolean urlText;
    @ProtobufProperty(index = 23, type = ProtobufType.BOOL)
    private final boolean urlNumber;
    @ProtobufProperty(index = 24, type = ProtobufType.ENUM)
    private final StubType stubType;
    @ProtobufProperty(index = 25, type = ProtobufType.BOOL)
    private final boolean clearMedia;
    @ProtobufProperty(index = 26, type = ProtobufType.STRING)
    private final List<String> stubParameters;
    @ProtobufProperty(index = 27, type = ProtobufType.UINT32)
    private final int duration;
    @ProtobufProperty(index = 28, type = ProtobufType.STRING)
    private final List<String> labels;
    @ProtobufProperty(index = 29, type = ProtobufType.MESSAGE)
    private final PaymentInfo paymentInfo;
    @ProtobufProperty(index = 30, type = ProtobufType.MESSAGE)
    private final LiveLocationMessage finalLiveLocation;
    @ProtobufProperty(index = 31, type = ProtobufType.MESSAGE)
    private final PaymentInfo quotedPaymentInfo;
    @ProtobufProperty(index = 32, type = ProtobufType.UINT64)
    private final long ephemeralStartTimestamp;
    @ProtobufProperty(index = 33, type = ProtobufType.UINT32)
    private final int ephemeralDuration;
    @ProtobufProperty(index = 34, type = ProtobufType.BOOL)
    private final boolean enableEphemeral;
    @ProtobufProperty(index = 35, type = ProtobufType.BOOL)
    private final boolean ephemeralOutOfSync;
    @ProtobufProperty(index = 36, type = ProtobufType.ENUM)
    private final BusinessPrivacyStatus businessPrivacyStatus;
    @ProtobufProperty(index = 37, type = ProtobufType.STRING)
    private final String businessVerifiedName;
    @ProtobufProperty(index = 38, type = ProtobufType.MESSAGE)
    private final MediaData mediaData;
    @ProtobufProperty(index = 39, type = ProtobufType.MESSAGE)
    private final PhotoChange photoChange;
    @ProtobufProperty(index = 40, type = ProtobufType.MESSAGE)
    private final MessageReceipt receipt;
    @ProtobufProperty(index = 41, type = ProtobufType.MESSAGE)
    private final List<ReactionMessage> reactions;
    @ProtobufProperty(index = 42, type = ProtobufType.MESSAGE)
    private final MediaData quotedStickerData;
    @ProtobufProperty(index = 43, type = ProtobufType.BYTES)
    private final byte[] futureProofData;
    @ProtobufProperty(index = 44, type = ProtobufType.MESSAGE)
    private final PublicServiceAnnouncementStatus psaStatus;
    @ProtobufProperty(index = 45, type = ProtobufType.MESSAGE)
    private final List<PollUpdate> pollUpdates;
    @ProtobufProperty(index = 46, type = ProtobufType.MESSAGE)
    private PollAdditionalMetadata pollAdditionalMetadata;
    @ProtobufProperty(index = 47, type = ProtobufType.STRING)
    private final String agentId;
    @ProtobufProperty(index = 48, type = ProtobufType.BOOL)
    private final boolean statusAlreadyViewed;
    @ProtobufProperty(index = 49, type = ProtobufType.BYTES)
    private byte[] messageSecret;
    @ProtobufProperty(index = 50, type = ProtobufType.MESSAGE)
    private final KeepInChat keepInChat;
    @ProtobufProperty(index = 51, type = ProtobufType.STRING)
    private final Jid originalSender;
    @ProtobufProperty(index = 52, type = ProtobufType.UINT64)
    private long revokeTimestampSeconds;

    @JsonBackReference
    private Chat chat;

    private Contact sender;

    public ChatMessageInfo(ChatMessageKey key, MessageContainer message, long timestampSeconds, MessageStatus status, Jid senderJid, long messageC2STimestamp, boolean ignore, boolean starred, boolean broadcast, String pushName, byte[] mediaCiphertextSha256, boolean multicast, boolean urlText, boolean urlNumber, StubType stubType, boolean clearMedia, List<String> stubParameters, int duration, List<String> labels, PaymentInfo paymentInfo, LiveLocationMessage finalLiveLocation, PaymentInfo quotedPaymentInfo, long ephemeralStartTimestamp, int ephemeralDuration, boolean enableEphemeral, boolean ephemeralOutOfSync, BusinessPrivacyStatus businessPrivacyStatus, String businessVerifiedName, MediaData mediaData, PhotoChange photoChange, MessageReceipt receipt, List<ReactionMessage> reactions, MediaData quotedStickerData, byte[] futureProofData, PublicServiceAnnouncementStatus psaStatus, List<PollUpdate> pollUpdates, PollAdditionalMetadata pollAdditionalMetadata, String agentId, boolean statusAlreadyViewed, byte[] messageSecret, KeepInChat keepInChat, Jid originalSender, long revokeTimestampSeconds) {
        this.key = key;
        this.message = Objects.requireNonNullElseGet(message, MessageContainer::empty);
        this.timestampSeconds = timestampSeconds;
        this.status = status;
        this.senderJid = senderJid;
        this.messageC2STimestamp = messageC2STimestamp;
        this.ignore = ignore;
        this.starred = starred;
        this.broadcast = broadcast;
        this.pushName = pushName;
        this.mediaCiphertextSha256 = mediaCiphertextSha256;
        this.multicast = multicast;
        this.urlText = urlText;
        this.urlNumber = urlNumber;
        this.stubType = stubType;
        this.clearMedia = clearMedia;
        this.stubParameters = stubParameters;
        this.duration = duration;
        this.labels = labels;
        this.paymentInfo = paymentInfo;
        this.finalLiveLocation = finalLiveLocation;
        this.quotedPaymentInfo = quotedPaymentInfo;
        this.ephemeralStartTimestamp = ephemeralStartTimestamp;
        this.ephemeralDuration = ephemeralDuration;
        this.enableEphemeral = enableEphemeral;
        this.ephemeralOutOfSync = ephemeralOutOfSync;
        this.businessPrivacyStatus = businessPrivacyStatus;
        this.businessVerifiedName = businessVerifiedName;
        this.mediaData = mediaData;
        this.photoChange = photoChange;
        this.receipt = Objects.requireNonNullElseGet(receipt, MessageReceipt::new);
        this.reactions = reactions;
        this.quotedStickerData = quotedStickerData;
        this.futureProofData = futureProofData;
        this.psaStatus = psaStatus;
        this.pollUpdates = pollUpdates;
        this.pollAdditionalMetadata = pollAdditionalMetadata;
        this.agentId = agentId;
        this.statusAlreadyViewed = statusAlreadyViewed;
        this.messageSecret = messageSecret;
        this.keepInChat = keepInChat;
        this.originalSender = originalSender;
        this.revokeTimestampSeconds = revokeTimestampSeconds;
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ChatMessageInfo(ChatMessageKey key, MessageContainer message, long timestampSeconds, MessageStatus status, Jid senderJid, long messageC2STimestamp, boolean ignore, boolean starred, boolean broadcast, String pushName, byte[] mediaCiphertextSha256, boolean multicast, boolean urlText, boolean urlNumber, StubType stubType, boolean clearMedia, List<String> stubParameters, int duration, List<String> labels, PaymentInfo paymentInfo, LiveLocationMessage finalLiveLocation, PaymentInfo quotedPaymentInfo, long ephemeralStartTimestamp, int ephemeralDuration, boolean enableEphemeral, boolean ephemeralOutOfSync, BusinessPrivacyStatus businessPrivacyStatus, String businessVerifiedName, MediaData mediaData, PhotoChange photoChange, MessageReceipt receipt, List<ReactionMessage> reactions, MediaData quotedStickerData, byte[] futureProofData, PublicServiceAnnouncementStatus psaStatus, List<PollUpdate> pollUpdates, PollAdditionalMetadata pollAdditionalMetadata, String agentId, boolean statusAlreadyViewed, byte[] messageSecret, KeepInChat keepInChat, Jid originalSender, long revokeTimestampSeconds, Chat chat, Contact sender) {
        this.key = key;
        this.message = message;
        this.timestampSeconds = timestampSeconds;
        this.status = status;
        this.senderJid = senderJid;
        this.messageC2STimestamp = messageC2STimestamp;
        this.ignore = ignore;
        this.starred = starred;
        this.broadcast = broadcast;
        this.pushName = pushName;
        this.mediaCiphertextSha256 = mediaCiphertextSha256;
        this.multicast = multicast;
        this.urlText = urlText;
        this.urlNumber = urlNumber;
        this.stubType = stubType;
        this.clearMedia = clearMedia;
        this.stubParameters = stubParameters;
        this.duration = duration;
        this.labels = labels;
        this.paymentInfo = paymentInfo;
        this.finalLiveLocation = finalLiveLocation;
        this.quotedPaymentInfo = quotedPaymentInfo;
        this.ephemeralStartTimestamp = ephemeralStartTimestamp;
        this.ephemeralDuration = ephemeralDuration;
        this.enableEphemeral = enableEphemeral;
        this.ephemeralOutOfSync = ephemeralOutOfSync;
        this.businessPrivacyStatus = businessPrivacyStatus;
        this.businessVerifiedName = businessVerifiedName;
        this.mediaData = mediaData;
        this.photoChange = photoChange;
        this.receipt = receipt;
        this.reactions = reactions;
        this.quotedStickerData = quotedStickerData;
        this.futureProofData = futureProofData;
        this.psaStatus = psaStatus;
        this.pollUpdates = pollUpdates;
        this.pollAdditionalMetadata = pollAdditionalMetadata;
        this.agentId = agentId;
        this.statusAlreadyViewed = statusAlreadyViewed;
        this.messageSecret = messageSecret;
        this.keepInChat = keepInChat;
        this.originalSender = originalSender;
        this.revokeTimestampSeconds = revokeTimestampSeconds;
        this.chat = chat;
        this.sender = sender;
    }

    /**
     * Determines whether the message was sent by you or by someone else
     *
     * @return a boolean
     */
    public boolean fromMe() {
        return key.fromMe();
    }

    /**
     * Returns the name of the chat where this message is or its pretty jid
     *
     * @return a non-null String
     */
    public String chatName() {
        if (chat != null) {
            return chat.name();
        }

        return chatJid().user();
    }

    /**
     * Returns the jid of the contact or group that sent the message.
     *
     * @return a non-null ContactJid
     */
    public Jid chatJid() {
        return key.chatJid();
    }

    /**
     * Returns the name of the person that sent this message or its pretty jid
     *
     * @return a non-null String
     */
    public String senderName() {
        return sender().map(Contact::name).orElseGet(senderJid()::user);
    }

    /**
     * Returns the message quoted by this message
     *
     * @return a non-empty optional {@link ChatMessageInfo} if this message quotes a message in memory
     */
    public Optional<QuotedMessageInfo> quotedMessage() {
        return Optional.of(message)
                .flatMap(MessageContainer::contentWithContext)
                .flatMap(ContextualMessage::contextInfo)
                .flatMap(QuotedMessageInfo::of);
    }

    /**
     * Returns the timestampSeconds for this message
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    /**
     * Returns the timestampSeconds for this message
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> revokeTimestamp() {
        return Clock.parseSeconds(revokeTimestampSeconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChatMessageInfo that &&
                timestampSeconds == that.timestampSeconds &&
                messageC2STimestamp == that.messageC2STimestamp &&
                ignore == that.ignore &&
                starred == that.starred &&
                broadcast == that.broadcast &&
                multicast == that.multicast &&
                urlText == that.urlText &&
                urlNumber == that.urlNumber &&
                clearMedia == that.clearMedia &&
                duration == that.duration &&
                ephemeralStartTimestamp == that.ephemeralStartTimestamp &&
                ephemeralDuration == that.ephemeralDuration &&
                enableEphemeral == that.enableEphemeral &&
                ephemeralOutOfSync == that.ephemeralOutOfSync &&
                statusAlreadyViewed == that.statusAlreadyViewed &&
                revokeTimestampSeconds == that.revokeTimestampSeconds &&
                Objects.equals(key, that.key) &&
                Objects.equals(message, that.message) &&
                status == that.status &&
                Objects.equals(senderJid, that.senderJid) &&
                Objects.equals(pushName, that.pushName) &&
                Objects.deepEquals(mediaCiphertextSha256, that.mediaCiphertextSha256) &&
                stubType == that.stubType &&
                Objects.equals(stubParameters, that.stubParameters) &&
                Objects.equals(labels, that.labels) &&
                Objects.equals(paymentInfo, that.paymentInfo) &&
                Objects.equals(finalLiveLocation, that.finalLiveLocation) &&
                Objects.equals(quotedPaymentInfo, that.quotedPaymentInfo) &&
                businessPrivacyStatus == that.businessPrivacyStatus &&
                Objects.equals(businessVerifiedName, that.businessVerifiedName) &&
                Objects.equals(mediaData, that.mediaData) &&
                Objects.equals(photoChange, that.photoChange) &&
                Objects.equals(receipt, that.receipt) &&
                Objects.equals(reactions, that.reactions) &&
                Objects.equals(quotedStickerData, that.quotedStickerData) &&
                Objects.deepEquals(futureProofData, that.futureProofData) &&
                Objects.equals(psaStatus, that.psaStatus) &&
                Objects.equals(pollUpdates, that.pollUpdates) &&
                Objects.equals(pollAdditionalMetadata, that.pollAdditionalMetadata) &&
                Objects.equals(agentId, that.agentId) &&
                Objects.deepEquals(messageSecret, that.messageSecret) &&
                Objects.equals(keepInChat, that.keepInChat) &&
                Objects.equals(originalSender, that.originalSender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, message, timestampSeconds, status, senderJid, messageC2STimestamp, ignore, starred, broadcast, pushName, Arrays.hashCode(mediaCiphertextSha256), multicast, urlText, urlNumber, stubType, clearMedia, stubParameters, duration, labels, paymentInfo, finalLiveLocation, quotedPaymentInfo, ephemeralStartTimestamp, ephemeralDuration, enableEphemeral, ephemeralOutOfSync, businessPrivacyStatus, businessVerifiedName, mediaData, photoChange, receipt, reactions, quotedStickerData, Arrays.hashCode(futureProofData), psaStatus, pollUpdates, pollAdditionalMetadata, agentId, statusAlreadyViewed, Arrays.hashCode(messageSecret), keepInChat, originalSender, revokeTimestampSeconds);
    }

    /**
     * Returns the id of the message
     *
     * @return a non-null String
     */
    public String id() {
        return key.id();
    }

    /**
     * Returns the jid of the sender
     *
     * @return a non-null ContactJid
     */
    public Jid senderJid() {
        return requireNonNullElseGet(senderJid, () -> key.senderJid().orElseGet(key::chatJid));
    }

    @Override
    public Jid parentJid() {
        return chatJid();
    }

    public ChatMessageKey key() {
        return key;
    }

    @Override
    public MessageContainer message() {
        return message;
    }

    @Override
    public ChatMessageInfo setMessage(MessageContainer message) {
        this.message = message;
        return this;
    }

    public OptionalLong timestampSeconds() {
        return Clock.parseTimestamp(timestampSeconds);
    }

    @Override
    public MessageStatus status() {
        return status;
    }

    public long messageC2STimestamp() {
        return messageC2STimestamp;
    }

    public boolean ignore() {
        return ignore;
    }

    public ChatMessageInfo setIgnore(boolean ignore) {
        this.ignore = ignore;
        return this;
    }

    public boolean starred() {
        return starred;
    }

    public boolean broadcast() {
        return broadcast;
    }

    public Optional<String> pushName() {
        return Optional.ofNullable(pushName);
    }

    public Optional<byte[]> mediaCiphertextSha256() {
        return Optional.ofNullable(mediaCiphertextSha256);
    }

    public boolean multicast() {
        return multicast;
    }

    public boolean urlText() {
        return urlText;
    }

    public boolean urlNumber() {
        return urlNumber;
    }

    public Optional<StubType> stubType() {
        return Optional.ofNullable(stubType);
    }

    public boolean clearMedia() {
        return clearMedia;
    }

    public List<String> stubParameters() {
        return stubParameters;
    }

    public int duration() {
        return duration;
    }

    public List<String> labels() {
        return labels;
    }

    public Optional<PaymentInfo> paymentInfo() {
        return Optional.ofNullable(paymentInfo);
    }

    public Optional<LiveLocationMessage> finalLiveLocation() {
        return Optional.ofNullable(finalLiveLocation);
    }

    public Optional<PaymentInfo> quotedPaymentInfo() {
        return Optional.ofNullable(quotedPaymentInfo);
    }

    public long ephemeralStartTimestamp() {
        return ephemeralStartTimestamp;
    }

    public int ephemeralDuration() {
        return ephemeralDuration;
    }

    public boolean enableEphemeral() {
        return enableEphemeral;
    }

    public boolean ephemeralOutOfSync() {
        return ephemeralOutOfSync;
    }

    public Optional<BusinessPrivacyStatus> businessPrivacyStatus() {
        return Optional.ofNullable(businessPrivacyStatus);
    }

    public Optional<String> businessVerifiedName() {
        return Optional.ofNullable(businessVerifiedName);
    }

    public Optional<MediaData> mediaData() {
        return Optional.ofNullable(mediaData);
    }

    public Optional<PhotoChange> photoChange() {
        return Optional.ofNullable(photoChange);
    }

    public MessageReceipt receipt() {
        return receipt;
    }

    public List<ReactionMessage> reactions() {
        return reactions;
    }

    public Optional<MediaData> quotedStickerData() {
        return Optional.ofNullable(quotedStickerData);
    }

    public byte[] futureProofData() {
        return futureProofData;
    }

    public Optional<PublicServiceAnnouncementStatus> psaStatus() {
        return Optional.ofNullable(psaStatus);
    }

    public List<PollUpdate> pollUpdates() {
        return pollUpdates;
    }

    public Optional<PollAdditionalMetadata> pollAdditionalMetadata() {
        return Optional.ofNullable(pollAdditionalMetadata);
    }

    public ChatMessageInfo setPollAdditionalMetadata(PollAdditionalMetadata pollAdditionalMetadata) {
        this.pollAdditionalMetadata = pollAdditionalMetadata;
        return this;
    }

    public Optional<String> agentId() {
        return Optional.ofNullable(agentId);
    }

    public boolean statusAlreadyViewed() {
        return statusAlreadyViewed;
    }

    public Optional<byte[]> messageSecret() {
        return Optional.ofNullable(messageSecret);
    }

    public ChatMessageInfo setMessageSecret(byte[] messageSecret) {
        this.messageSecret = messageSecret;
        return this;
    }

    public Optional<KeepInChat> keepInChat() {
        return Optional.ofNullable(keepInChat);
    }

    public Optional<Jid> originalSender() {
        return Optional.ofNullable(originalSender);
    }

    public long revokeTimestampSeconds() {
        return revokeTimestampSeconds;
    }

    public Optional<Chat> chat() {
        return Optional.ofNullable(chat);
    }

    public ChatMessageInfo setChat(Chat chat) {
        this.chat = chat;
        return this;
    }

    public Optional<Contact> sender() {
        return Optional.ofNullable(sender);
    }

    public ChatMessageInfo setSender(Contact sender) {
        this.sender = sender;
        return this;
    }

    @Override
    public ChatMessageInfo setStatus(MessageStatus status) {
        this.status = status;
        return this;
    }

    public ChatMessageInfo setStarred(boolean starred) {
        this.starred = starred;
        return this;
    }

    public ChatMessageInfo setRevokeTimestampSeconds(long revokeTimestampSeconds) {
        this.revokeTimestampSeconds = revokeTimestampSeconds;
        return this;
    }


    /**
     * The constants of this enumerated type describe the various types of server message that a {@link ChatMessageInfo} can describe
     */
    @ProtobufEnum
    public enum StubType {
        UNKNOWN(0, List.of("unknown")),
        REVOKE(1, List.of("revoked")),
        CIPHERTEXT(2, List.of("ciphertext")),
        FUTUREPROOF(3, List.of("phone")),
        NON_VERIFIED_TRANSITION(4, List.of("non_verified_transition")),
        UNVERIFIED_TRANSITION(5, List.of("unverified_transition")),
        VERIFIED_TRANSITION(6, List.of("verified_transition")),
        VERIFIED_LOW_UNKNOWN(7, List.of("verified_low_unknown")),
        VERIFIED_HIGH(8, List.of("verified_high")),
        VERIFIED_INITIAL_UNKNOWN(9, List.of("verified_initial_unknown")),
        VERIFIED_INITIAL_LOW(10, List.of("verified_initial_low")),
        VERIFIED_INITIAL_HIGH(11, List.of("verified_initial_high")),
        VERIFIED_TRANSITION_ANY_TO_NONE(12, List.of("verified_transition_any_to_none")),
        VERIFIED_TRANSITION_ANY_TO_HIGH(13, List.of("verified_transition_any_to_high")),
        VERIFIED_TRANSITION_HIGH_TO_LOW(14, List.of("verified_transition_high_to_low")),
        VERIFIED_TRANSITION_HIGH_TO_UNKNOWN(15, List.of("verified_transition_high_to_unknown")),
        VERIFIED_TRANSITION_UNKNOWN_TO_LOW(16, List.of("verified_transition_unknown_to_low")),
        VERIFIED_TRANSITION_LOW_TO_UNKNOWN(17, List.of("verified_transition_low_to_unknown")),
        VERIFIED_TRANSITION_NONE_TO_LOW(18, List.of("verified_transition_none_to_low")),
        VERIFIED_TRANSITION_NONE_TO_UNKNOWN(19, List.of("verified_transition_none_to_unknown")),
        GROUP_CREATE(20, List.of("create")),
        GROUP_CHANGE_SUBJECT(21, List.of("subject")),
        GROUP_CHANGE_ICON(22, List.of("picture")),
        GROUP_CHANGE_INVITE_LINK(23, List.of("revoke_invite")),
        GROUP_CHANGE_DESCRIPTION(24, List.of("description")),
        GROUP_CHANGE_RESTRICT(25, List.of("restrict", "locked", "unlocked")),
        GROUP_CHANGE_ANNOUNCE(26, List.of("announce", "announcement", "not_announcement")),
        GROUP_PARTICIPANT_ADD(27, List.of("add")),
        GROUP_PARTICIPANT_REMOVE(28, List.of("remove")),
        GROUP_PARTICIPANT_PROMOTE(29, List.of("promote")),
        GROUP_PARTICIPANT_DEMOTE(30, List.of("demote")),
        GROUP_PARTICIPANT_INVITE(31, List.of("invite")),
        GROUP_PARTICIPANT_LEAVE(32, List.of("leave")),
        GROUP_PARTICIPANT_CHANGE_NUMBER(33, List.of("modify")),
        BROADCAST_CREATE(34, List.of("create")),
        BROADCAST_ADD(35, List.of("add")),
        BROADCAST_REMOVE(36, List.of("remove")),
        GENERIC_NOTIFICATION(37, List.of("notification")),
        E2E_IDENTITY_CHANGED(38, List.of("identity")),
        E2E_ENCRYPTED(39, List.of("encrypt")),
        CALL_MISSED_VOICE(40, List.of("miss")),
        CALL_MISSED_VIDEO(41, List.of("miss_video")),
        INDIVIDUAL_CHANGE_NUMBER(42, List.of("change_number")),
        GROUP_DELETE(43, List.of("delete")),
        GROUP_ANNOUNCE_MODE_MESSAGE_BOUNCE(44, List.of("announce_msg_bounce")),
        CALL_MISSED_GROUP_VOICE(45, List.of("miss_group")),
        CALL_MISSED_GROUP_VIDEO(46, List.of("miss_group_video")),
        PAYMENT_CIPHERTEXT(47, List.of("ciphertext")),
        PAYMENT_FUTUREPROOF(48, List.of("futureproof")),
        PAYMENT_TRANSACTION_STATUS_UPDATE_FAILED(49, List.of("payment_transaction_status_update_failed")),
        PAYMENT_TRANSACTION_STATUS_UPDATE_REFUNDED(50, List.of("payment_transaction_status_update_refunded")),
        PAYMENT_TRANSACTION_STATUS_UPDATE_REFUND_FAILED(51, List.of("payment_transaction_status_update_refund_failed")),
        PAYMENT_TRANSACTION_STATUS_RECEIVER_PENDING_SETUP(52, List.of("payment_transaction_status_receiver_pending_setup")),
        PAYMENT_TRANSACTION_STATUS_RECEIVER_SUCCESS_AFTER_HICCUP(53, List.of("payment_transaction_status_receiver_success_after_hiccup")),
        PAYMENT_ACTION_ACCOUNT_SETUP_REMINDER(54, List.of("payment_action_account_setup_reminder")),
        PAYMENT_ACTION_SEND_PAYMENT_REMINDER(55, List.of("payment_action_send_payment_reminder")),
        PAYMENT_ACTION_SEND_PAYMENT_INVITATION(56, List.of("payment_action_send_payment_invitation")),
        PAYMENT_ACTION_REQUEST_DECLINED(57, List.of("payment_action_request_declined")),
        PAYMENT_ACTION_REQUEST_EXPIRED(58, List.of("payment_action_request_expired")),
        PAYMENT_ACTION_REQUEST_CANCELLED(59, List.of("payment_transaction_request_cancelled")),
        BIZ_VERIFIED_TRANSITION_TOP_TO_BOTTOM(60, List.of("biz_verified_transition_top_to_bottom")),
        BIZ_VERIFIED_TRANSITION_BOTTOM_TO_TOP(61, List.of("biz_verified_transition_bottom_to_top")),
        BIZ_INTRO_TOP(62, List.of("biz_intro_top")),
        BIZ_INTRO_BOTTOM(63, List.of("biz_intro_bottom")),
        BIZ_NAME_CHANGE(64, List.of("biz_name_change")),
        BIZ_MOVE_TO_CONSUMER_APP(65, List.of("biz_move_to_consumer_app")),
        BIZ_TWO_TIER_MIGRATION_TOP(66, List.of("biz_two_tier_migration_top")),
        BIZ_TWO_TIER_MIGRATION_BOTTOM(67, List.of("biz_two_tier_migration_bottom")),
        OVERSIZED(68, List.of("oversized")),
        GROUP_CHANGE_NO_FREQUENTLY_FORWARDED(69, List.of("frequently_forwarded_ok", "no_frequently_forwarded")),
        GROUP_V4_ADD_INVITE_SENT(70, List.of("v4_add_invite_sent")),
        GROUP_PARTICIPANT_ADD_REQUEST_JOIN(71, List.of("v4_add_invite_join")),
        CHANGE_EPHEMERAL_SETTING(72, List.of("ephemeral", "not_ephemeral")),
        E2E_DEVICE_CHANGED(73, List.of("device")),
        VIEWED_ONCE(74, List.of()),
        E2E_ENCRYPTED_NOW(75, List.of("encrypt_now")),
        BLUE_MSG_BSP_FB_TO_BSP_PREMISE(76, List.of("blue_msg_bsp_fb_to_bsp_premise")),
        BLUE_MSG_BSP_FB_TO_SELF_FB(77, List.of("blue_msg_bsp_fb_to_self_fb")),
        BLUE_MSG_BSP_FB_TO_SELF_PREMISE(78, List.of("blue_msg_bsp_fb_to_self_premise")),
        BLUE_MSG_BSP_FB_UNVERIFIED(79, List.of("blue_msg_bsp_fb_unverified")),
        BLUE_MSG_BSP_FB_UNVERIFIED_TO_SELF_PREMISE_VERIFIED(80, List.of("blue_msg_bsp_fb_unverified_to_self_premise_verified")),
        BLUE_MSG_BSP_FB_VERIFIED(81, List.of("blue_msg_bsp_fb_verified")),
        BLUE_MSG_BSP_FB_VERIFIED_TO_SELF_PREMISE_UNVERIFIED(82, List.of("blue_msg_bsp_fb_verified_to_self_premise_unverified")),
        BLUE_MSG_BSP_PREMISE_TO_SELF_PREMISE(83, List.of("blue_msg_bsp_premise_to_self_premise")),
        BLUE_MSG_BSP_PREMISE_UNVERIFIED(84, List.of("blue_msg_bsp_premise_unverified")),
        BLUE_MSG_BSP_PREMISE_UNVERIFIED_TO_SELF_PREMISE_VERIFIED(85, List.of("blue_msg_bsp_premise_unverified_to_self_premise_verified")),
        BLUE_MSG_BSP_PREMISE_VERIFIED(86, List.of("blue_msg_bsp_premise_verified")),
        BLUE_MSG_BSP_PREMISE_VERIFIED_TO_SELF_PREMISE_UNVERIFIED(87, List.of("blue_msg_bsp_premise_verified_to_self_premise_unverified")),
        BLUE_MSG_CONSUMER_TO_BSP_FB_UNVERIFIED(88, List.of("blue_msg_consumer_to_bsp_fb_unverified")),
        BLUE_MSG_CONSUMER_TO_BSP_PREMISE_UNVERIFIED(89, List.of("blue_msg_consumer_to_bsp_premise_unverified")),
        BLUE_MSG_CONSUMER_TO_SELF_FB_UNVERIFIED(90, List.of("blue_msg_consumer_to_self_fb_unverified")),
        BLUE_MSG_CONSUMER_TO_SELF_PREMISE_UNVERIFIED(91, List.of("blue_msg_consumer_to_self_premise_unverified")),
        BLUE_MSG_SELF_FB_TO_BSP_PREMISE(92, List.of("blue_msg_self_fb_to_bsp_premise")),
        BLUE_MSG_SELF_FB_TO_SELF_PREMISE(93, List.of("blue_msg_self_fb_to_self_premise")),
        BLUE_MSG_SELF_FB_UNVERIFIED(94, List.of("blue_msg_self_fb_unverified")),
        BLUE_MSG_SELF_FB_UNVERIFIED_TO_SELF_PREMISE_VERIFIED(95, List.of("blue_msg_self_fb_unverified_to_self_premise_verified")),
        BLUE_MSG_SELF_FB_VERIFIED(96, List.of("blue_msg_self_fb_verified")),
        BLUE_MSG_SELF_FB_VERIFIED_TO_SELF_PREMISE_UNVERIFIED(97, List.of("blue_msg_self_fb_verified_to_self_premise_unverified")),
        BLUE_MSG_SELF_PREMISE_TO_BSP_PREMISE(98, List.of("blue_msg_self_premise_to_bsp_premise")),
        BLUE_MSG_SELF_PREMISE_UNVERIFIED(99, List.of("blue_msg_self_premise_unverified")),
        BLUE_MSG_SELF_PREMISE_VERIFIED(100, List.of("blue_msg_self_premise_verified")),
        BLUE_MSG_TO_BSP_FB(101, List.of("blue_msg_to_bsp_fb")),
        BLUE_MSG_TO_CONSUMER(102, List.of("blue_msg_to_consumer")),
        BLUE_MSG_TO_SELF_FB(103, List.of("blue_msg_to_self_fb")),
        BLUE_MSG_UNVERIFIED_TO_BSP_FB_VERIFIED(104, List.of("blue_msg_unverified_to_bsp_fb_verified")),
        BLUE_MSG_UNVERIFIED_TO_BSP_PREMISE_VERIFIED(105, List.of("blue_msg_unverified_to_bsp_premise_verified")),
        BLUE_MSG_UNVERIFIED_TO_SELF_FB_VERIFIED(106, List.of("blue_msg_unverified_to_self_fb_verified")),
        BLUE_MSG_UNVERIFIED_TO_VERIFIED(107, List.of("blue_msg_unverified_to_verified")),
        BLUE_MSG_VERIFIED_TO_BSP_FB_UNVERIFIED(108, List.of("blue_msg_verified_to_bsp_fb_unverified")),
        BLUE_MSG_VERIFIED_TO_BSP_PREMISE_UNVERIFIED(109, List.of("blue_msg_verified_to_bsp_premise_unverified")),
        BLUE_MSG_VERIFIED_TO_SELF_FB_UNVERIFIED(110, List.of("blue_msg_verified_to_self_fb_unverified")),
        BLUE_MSG_VERIFIED_TO_UNVERIFIED(111, List.of("blue_msg_verified_to_unverified")),
        BLUE_MSG_BSP_FB_UNVERIFIED_TO_BSP_PREMISE_VERIFIED(112, List.of("blue_msg_bsp_fb_unverified_to_bsp_premise_verified")),
        BLUE_MSG_BSP_FB_UNVERIFIED_TO_SELF_FB_VERIFIED(113, List.of("blue_msg_bsp_fb_unverified_to_self_fb_verified")),
        BLUE_MSG_BSP_FB_VERIFIED_TO_BSP_PREMISE_UNVERIFIED(114, List.of("blue_msg_bsp_fb_verified_to_bsp_premise_unverified")),
        BLUE_MSG_BSP_FB_VERIFIED_TO_SELF_FB_UNVERIFIED(115, List.of("blue_msg_bsp_fb_verified_to_self_fb_unverified")),
        BLUE_MSG_SELF_FB_UNVERIFIED_TO_BSP_PREMISE_VERIFIED(116, List.of("blue_msg_self_fb_unverified_to_bsp_premise_verified")),
        BLUE_MSG_SELF_FB_VERIFIED_TO_BSP_PREMISE_UNVERIFIED(117, List.of("blue_msg_self_fb_verified_to_bsp_premise_unverified")),
        E2E_IDENTITY_UNAVAILABLE(118, List.of("e2e_identity_unavailable")),
        GROUP_CREATING(119, List.of()),
        GROUP_CREATE_FAILED(120, List.of()),
        GROUP_BOUNCED(121, List.of()),
        BLOCK_CONTACT(122, List.of("block_contact")),
        EPHEMERAL_SETTING_NOT_APPLIED(123, List.of()),
        SYNC_FAILED(124, List.of()),
        SYNCING(125, List.of()),
        BIZ_PRIVACY_MODE_INIT_FB(126, List.of("biz_privacy_mode_init_fb")),
        BIZ_PRIVACY_MODE_INIT_BSP(127, List.of("biz_privacy_mode_init_bsp")),
        BIZ_PRIVACY_MODE_TO_FB(128, List.of("biz_privacy_mode_to_fb")),
        BIZ_PRIVACY_MODE_TO_BSP(129, List.of("biz_privacy_mode_to_bsp")),
        DISAPPEARING_MODE(130, List.of("disappearing_mode")),
        E2E_DEVICE_FETCH_FAILED(131, List.of()),
        ADMIN_REVOKE(132, List.of("admin")),
        GROUP_INVITE_LINK_GROWTH_LOCKED(133, List.of("growth_locked", "growth_unlocked")),
        COMMUNITY_LINK_PARENT_GROUP(134, List.of("parent_group_link")),
        COMMUNITY_LINK_SIBLING_GROUP(135, List.of("sibling_group_link")),
        COMMUNITY_LINK_SUB_GROUP(136, List.of("sub_group_link", "link")),
        COMMUNITY_UNLINK_PARENT_GROUP(137, List.of("parent_group_unlink")),
        COMMUNITY_UNLINK_SIBLING_GROUP(138, List.of("sibling_group_unlink")),
        COMMUNITY_UNLINK_SUB_GROUP(139, List.of("sub_group_unlink", "unlink")),
        GROUP_PARTICIPANT_ACCEPT(140, List.of()),
        GROUP_PARTICIPANT_LINKED_GROUP_JOIN(141, List.of("linked_group_join")),
        COMMUNITY_CREATE(142, List.of("community_create")),
        EPHEMERAL_KEEP_IN_CHAT(143, List.of("ephemeral_keep_in_chat")),
        GROUP_MEMBERSHIP_JOIN_APPROVAL_REQUEST(144, List.of("membership_approval_request")),
        GROUP_MEMBERSHIP_JOIN_APPROVAL_MODE(145, List.of("membership_approval_mode")),
        INTEGRITY_UNLINK_PARENT_GROUP(146, List.of("integrity_parent_group_unlink")),
        COMMUNITY_PARTICIPANT_PROMOTE(147, List.of("linked_group_promote")),
        COMMUNITY_PARTICIPANT_DEMOTE(148, List.of("linked_group_demote")),
        COMMUNITY_PARENT_GROUP_DELETED(149, List.of("delete_parent_group")),
        COMMUNITY_LINK_PARENT_GROUP_MEMBERSHIP_APPROVAL(150, List.of("parent_group_link_membership_approval")),
        GROUP_PARTICIPANT_JOINED_GROUP_AND_PARENT_GROUP(151, List.of("auto_add")),
        MASKED_THREAD_CREATED(152, List.of("masked_thread_created")),
        MASKED_THREAD_UNMASKED(153, List.of()),
        BIZ_CHAT_ASSIGNMENT(154, List.of("chat_assignment")),
        CHAT_PSA(155, List.of("e2e_notification")),
        CHAT_POLL_CREATION_MESSAGE(156, List.of()),
        CAG_MASKED_THREAD_CREATED(157, List.of("cag_masked_thread_created")),
        COMMUNITY_PARENT_GROUP_SUBJECT_CHANGED(158, List.of("subject")),
        CAG_INVITE_AUTO_ADD(159, List.of("invite_auto_add")),
        BIZ_CHAT_ASSIGNMENT_UNASSIGN(160, List.of("chat_assignment_unassign")),
        CAG_INVITE_AUTO_JOINED(161, List.of("invite_auto_add"));

        final int index;
        private final List<String> symbols;

        StubType(@ProtobufEnumIndex int index, List<String> symbols) {
            this.index = index;
            this.symbols = symbols;
        }

        public int index() {
            return index;
        }

        public List<String> symbols() {
            return symbols;
        }

        public static Optional<StubType> of(String symbol) {
            return Arrays.stream(values()).filter(entry -> entry.symbols().contains(symbol)).findFirst();
        }
    }
}
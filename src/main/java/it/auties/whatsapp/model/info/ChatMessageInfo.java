package it.auties.whatsapp.model.info;

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
public final class ChatMessageInfo implements MessageInfo, MessageStatusInfo { // TODO: Check me
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ChatMessageKey key;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    MessageContainer message;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    final long timestampSeconds;

    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    MessageStatus status;

    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final Jid senderJid;

    @ProtobufProperty(index = 6, type = ProtobufType.UINT64)
    final long messageC2STimestamp;

    @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
    boolean ignore;

    @ProtobufProperty(index = 17, type = ProtobufType.BOOL)
    boolean starred;

    @ProtobufProperty(index = 18, type = ProtobufType.BOOL)
    final boolean broadcast;

    @ProtobufProperty(index = 19, type = ProtobufType.STRING)
    final String pushName;

    @ProtobufProperty(index = 20, type = ProtobufType.BYTES)
    final byte[] mediaCiphertextSha256;

    @ProtobufProperty(index = 21, type = ProtobufType.BOOL)
    final boolean multicast;

    @ProtobufProperty(index = 22, type = ProtobufType.BOOL)
    final boolean urlText;

    @ProtobufProperty(index = 23, type = ProtobufType.BOOL)
    final boolean urlNumber;

    @ProtobufProperty(index = 24, type = ProtobufType.ENUM)
    final ChatMessageStubType stubType;

    @ProtobufProperty(index = 25, type = ProtobufType.BOOL)
    final boolean clearMedia;

    @ProtobufProperty(index = 26, type = ProtobufType.STRING)
    final List<String> stubParameters;

    @ProtobufProperty(index = 27, type = ProtobufType.UINT32)
    final int duration;

    @ProtobufProperty(index = 28, type = ProtobufType.STRING)
    final List<String> labels;

    @ProtobufProperty(index = 29, type = ProtobufType.MESSAGE)
    final PaymentInfo paymentInfo;

    @ProtobufProperty(index = 30, type = ProtobufType.MESSAGE)
    final LiveLocationMessage finalLiveLocation;

    @ProtobufProperty(index = 31, type = ProtobufType.MESSAGE)
    final PaymentInfo quotedPaymentInfo;

    @ProtobufProperty(index = 32, type = ProtobufType.UINT64)
    final long ephemeralStartTimestamp;

    @ProtobufProperty(index = 33, type = ProtobufType.UINT32)
    final int ephemeralDuration;

    @ProtobufProperty(index = 34, type = ProtobufType.BOOL)
    final boolean enableEphemeral;

    @ProtobufProperty(index = 35, type = ProtobufType.BOOL)
    final boolean ephemeralOutOfSync;

    @ProtobufProperty(index = 36, type = ProtobufType.ENUM)
    final BusinessPrivacyStatus businessPrivacyStatus;

    @ProtobufProperty(index = 37, type = ProtobufType.STRING)
    final String businessVerifiedName;

    @ProtobufProperty(index = 38, type = ProtobufType.MESSAGE)
    final MediaData mediaData;

    @ProtobufProperty(index = 39, type = ProtobufType.MESSAGE)
    final PhotoChange photoChange;

    @ProtobufProperty(index = 40, type = ProtobufType.MESSAGE)
    final MessageReceipt receipt;

    @ProtobufProperty(index = 41, type = ProtobufType.MESSAGE)
    final List<ReactionMessage> reactions;

    @ProtobufProperty(index = 42, type = ProtobufType.MESSAGE)
    final MediaData quotedStickerData;

    @ProtobufProperty(index = 43, type = ProtobufType.BYTES)
    final byte[] futureProofData;

    @ProtobufProperty(index = 44, type = ProtobufType.MESSAGE)
    final PublicServiceAnnouncementStatus psaStatus;

    @ProtobufProperty(index = 45, type = ProtobufType.MESSAGE)
    final List<PollUpdate> pollUpdates;

    @ProtobufProperty(index = 46, type = ProtobufType.MESSAGE)
    PollAdditionalMetadata pollAdditionalMetadata;

    @ProtobufProperty(index = 47, type = ProtobufType.STRING)
    final String agentId;

    @ProtobufProperty(index = 48, type = ProtobufType.BOOL)
    final boolean statusAlreadyViewed;

    @ProtobufProperty(index = 49, type = ProtobufType.BYTES)
    byte[] messageSecret;

    @ProtobufProperty(index = 50, type = ProtobufType.MESSAGE)
    final KeepInChat keepInChat;

    @ProtobufProperty(index = 51, type = ProtobufType.STRING)
    final Jid originalSender;

    @ProtobufProperty(index = 52, type = ProtobufType.UINT64)
    long revokeTimestampSeconds;

    private Chat chat;

    private Contact sender;

    ChatMessageInfo(ChatMessageKey key, MessageContainer message, long timestampSeconds, MessageStatus status, Jid senderJid, long messageC2STimestamp, boolean ignore, boolean starred, boolean broadcast, String pushName, byte[] mediaCiphertextSha256, boolean multicast, boolean urlText, boolean urlNumber, ChatMessageStubType stubType, boolean clearMedia, List<String> stubParameters, int duration, List<String> labels, PaymentInfo paymentInfo, LiveLocationMessage finalLiveLocation, PaymentInfo quotedPaymentInfo, long ephemeralStartTimestamp, int ephemeralDuration, boolean enableEphemeral, boolean ephemeralOutOfSync, BusinessPrivacyStatus businessPrivacyStatus, String businessVerifiedName, MediaData mediaData, PhotoChange photoChange, MessageReceipt receipt, List<ReactionMessage> reactions, MediaData quotedStickerData, byte[] futureProofData, PublicServiceAnnouncementStatus psaStatus, List<PollUpdate> pollUpdates, PollAdditionalMetadata pollAdditionalMetadata, String agentId, boolean statusAlreadyViewed, byte[] messageSecret, KeepInChat keepInChat, Jid originalSender, long revokeTimestampSeconds) {
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
    public void setMessage(MessageContainer message) {
        this.message = message;
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

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
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

    public Optional<ChatMessageStubType> stubType() {
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

    public void setPollAdditionalMetadata(PollAdditionalMetadata pollAdditionalMetadata) {
        this.pollAdditionalMetadata = pollAdditionalMetadata;
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

    public void setMessageSecret(byte[] messageSecret) {
        this.messageSecret = messageSecret;
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

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public Optional<Contact> sender() {
        return Optional.ofNullable(sender);
    }

    public void setSender(Contact sender) {
        this.sender = sender;
    }

    @Override
    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    public void setRevokeTimestampSeconds(long revokeTimestampSeconds) {
        this.revokeTimestampSeconds = revokeTimestampSeconds;
    }
}
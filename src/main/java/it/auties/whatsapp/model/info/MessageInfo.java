package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.annotation.JsonBackReference;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.business.BusinessPrivacyStatus;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.media.MediaData;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.standard.LiveLocationMessage;
import it.auties.whatsapp.model.message.standard.ReactionMessage;
import it.auties.whatsapp.model.poll.PollAdditionalMetadata;
import it.auties.whatsapp.model.poll.PollUpdate;
import it.auties.whatsapp.model.sync.PhotoChange;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Json;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNullElseGet;

/**
 * A model class that holds the information related to a {@link Message}.
 */
public final class MessageInfo implements Info, MessageMetadataProvider, ProtobufMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
    private final @NonNull MessageKey key;
    @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
    private @NonNull MessageContainer message;
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    private final long timestampSeconds;
    @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
    private @NonNull MessageStatus status;
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    private final ContactJid senderJid;
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
    @ProtobufProperty(index = 24, type = ProtobufType.OBJECT)
    private final StubType stubType;
    @ProtobufProperty(index = 25, type = ProtobufType.BOOL)
    private final boolean clearMedia;
    @ProtobufProperty(index = 26, type = ProtobufType.STRING, repeated = true)
    private final List<String> stubParameters;
    @ProtobufProperty(index = 27, type = ProtobufType.UINT32)
    private final int duration;
    @ProtobufProperty(index = 28, type = ProtobufType.STRING, repeated = true)
    private final List<String> labels;
    @ProtobufProperty(index = 29, type = ProtobufType.OBJECT)
    private final PaymentInfo paymentInfo;
    @ProtobufProperty(index = 30, type = ProtobufType.OBJECT)
    private final LiveLocationMessage finalLiveLocation;
    @ProtobufProperty(index = 31, type = ProtobufType.OBJECT)
    private final PaymentInfo quotedPaymentInfo;
    @ProtobufProperty(index = 32, type = ProtobufType.UINT64)
    private final long ephemeralStartTimestamp;
    @ProtobufProperty(index = 33, type = ProtobufType.UINT32)
    private final int ephemeralDuration;
    @ProtobufProperty(index = 34, type = ProtobufType.BOOL)
    private final boolean enableEphemeral;
    @ProtobufProperty(index = 35, type = ProtobufType.BOOL)
    private final boolean ephemeralOutOfSync;
    @ProtobufProperty(index = 36, type = ProtobufType.OBJECT)
    private final BusinessPrivacyStatus businessPrivacyStatus;
    @ProtobufProperty(index = 37, type = ProtobufType.STRING)
    private final String businessVerifiedName;
    @ProtobufProperty(index = 38, type = ProtobufType.OBJECT)
    private final MediaData mediaData;
    @ProtobufProperty(index = 39, type = ProtobufType.OBJECT)
    private final PhotoChange photoChange;
    @ProtobufProperty(index = 40, type = ProtobufType.OBJECT)
    private final @NonNull MessageReceipt receipt;
    @ProtobufProperty(index = 41, type = ProtobufType.OBJECT, repeated = true)
    private final List<ReactionMessage> reactions;
    @ProtobufProperty(index = 42, type = ProtobufType.OBJECT)
    private final MediaData quotedStickerData;
    @ProtobufProperty(index = 43, type = ProtobufType.BYTES)
    private final byte[] futureProofData;
    @ProtobufProperty(index = 44, type = ProtobufType.OBJECT)
    private final PublicServiceAnnouncementStatus psaStatus;
    @ProtobufProperty(index = 45, type = ProtobufType.OBJECT, repeated = true)
    private final List<PollUpdate> pollUpdates;
    @ProtobufProperty(index = 46, type = ProtobufType.OBJECT)
    private PollAdditionalMetadata pollAdditionalMetadata;
    @ProtobufProperty(index = 47, type = ProtobufType.STRING)
    private final String agentId;
    @ProtobufProperty(index = 48, type = ProtobufType.BOOL)
    private final boolean statusAlreadyViewed;
    @ProtobufProperty(index = 49, type = ProtobufType.BYTES)
    private byte[] messageSecret;
    @ProtobufProperty(index = 50, type = ProtobufType.OBJECT)
    private final KeepInChat keepInChat;
    @ProtobufProperty(index = 51, type = ProtobufType.STRING)
    private final ContactJid originalSender;
    @ProtobufProperty(index = 52, type = ProtobufType.UINT64)
    private long revokeTimestampSeconds;

    @JsonBackReference
    @Nullable
    private Chat chat;

    @Nullable
    private Contact sender;

    public MessageInfo(@NonNull MessageKey key, @NonNull MessageContainer message, long timestampSeconds, @NonNull MessageStatus status, ContactJid senderJid, long messageC2STimestamp, boolean ignore, boolean starred, boolean broadcast, String pushName, byte[] mediaCiphertextSha256, boolean multicast, boolean urlText, boolean urlNumber, StubType stubType, boolean clearMedia, List<String> stubParameters, int duration, List<String> labels, PaymentInfo paymentInfo, LiveLocationMessage finalLiveLocation, PaymentInfo quotedPaymentInfo, long ephemeralStartTimestamp, int ephemeralDuration, boolean enableEphemeral, boolean ephemeralOutOfSync, BusinessPrivacyStatus businessPrivacyStatus, String businessVerifiedName, MediaData mediaData, PhotoChange photoChange, @NonNull MessageReceipt receipt, List<ReactionMessage> reactions, MediaData quotedStickerData, byte[] futureProofData, PublicServiceAnnouncementStatus psaStatus, List<PollUpdate> pollUpdates, PollAdditionalMetadata pollAdditionalMetadata, String agentId, boolean statusAlreadyViewed, byte[] messageSecret, KeepInChat keepInChat, ContactJid originalSender, long revokeTimestampSeconds, @Nullable Chat chat, @Nullable Contact sender) {
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


    public MessageInfo(@NonNull MessageKey key, @NonNull MessageContainer message, long timestampSeconds, @NonNull MessageStatus status, ContactJid senderJid, long messageC2STimestamp, boolean ignore, boolean starred, boolean broadcast, String pushName, byte[] mediaCiphertextSha256, boolean multicast, boolean urlText, boolean urlNumber, StubType stubType, boolean clearMedia, List<String> stubParameters, int duration, List<String> labels, PaymentInfo paymentInfo, LiveLocationMessage finalLiveLocation, PaymentInfo quotedPaymentInfo, long ephemeralStartTimestamp, int ephemeralDuration, boolean enableEphemeral, boolean ephemeralOutOfSync, BusinessPrivacyStatus businessPrivacyStatus, String businessVerifiedName, MediaData mediaData, PhotoChange photoChange, @NonNull MessageReceipt receipt, List<ReactionMessage> reactions, MediaData quotedStickerData, byte[] futureProofData, PublicServiceAnnouncementStatus psaStatus, List<PollUpdate> pollUpdates, PollAdditionalMetadata pollAdditionalMetadata, String agentId, boolean statusAlreadyViewed, byte[] messageSecret, KeepInChat keepInChat, ContactJid originalSender, long revokeTimestampSeconds) {
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
        if(chat != null) {
            return chat.name();
        }

        return chatJid().user();
    }

    /**
     * Returns the jid of the contact or group that sent the message.
     *
     * @return a non-null ContactJid
     */
    public ContactJid chatJid() {
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
     * @return a non-empty optional {@link MessageInfo} if this message quotes a message in memory
     */
    public Optional<QuotedMessage> quotedMessage() {
        return Optional.of(message)
                .flatMap(MessageContainer::contentWithContext)
                .flatMap(ContextualMessage::contextInfo)
                .flatMap(QuotedMessage::of);
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

    /**
     * Converts this message to a json. Useful when debugging.
     *
     * @return a non-null string
     */
    public String toJson() {
        return Json.writeValueAsString(this, true);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id());
    }

    public boolean equals(Object object) {
        return object instanceof MessageInfo that
                && Objects.equals(this.id(), that.id())
                && Objects.equals(this.stubType, that.stubType);
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
    @Override
    public ContactJid senderJid() {
        return requireNonNullElseGet(senderJid, () -> key.senderJid().orElseGet(key::chatJid));
    }

    public @NonNull MessageKey key() {
        return key;
    }

    @Override
    public @NonNull MessageContainer message() {
        return message;
    }

    public MessageInfo setMessage(MessageContainer message) {
        this.message = message;
        return this;
    }

    public long timestampSeconds() {
        return timestampSeconds;
    }

    public @NonNull MessageStatus status() {
        return status;
    }

    public long messageC2STimestamp() {
        return messageC2STimestamp;
    }

    public boolean ignore() {
        return ignore;
    }

    public MessageInfo setIgnore(boolean ignore) {
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

    public MessageInfo setPollAdditionalMetadata(PollAdditionalMetadata pollAdditionalMetadata) {
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

    public MessageInfo setMessageSecret(byte[] messageSecret) {
        this.messageSecret = messageSecret;
        return this;
    }

    public Optional<KeepInChat> keepInChat() {
        return Optional.ofNullable(keepInChat);
    }

    public Optional<ContactJid> originalSender() {
        return Optional.ofNullable(originalSender);
    }

    public long revokeTimestampSeconds() {
        return revokeTimestampSeconds;
    }

    @Override
    public Optional<Chat> chat() {
        return Optional.ofNullable(chat);
    }

    public MessageInfo setChat(Chat chat) {
        this.chat = chat;
        return this;
    }

    @Override
    public Optional<Contact> sender() {
        return Optional.ofNullable(sender);
    }

    public MessageInfo setSender(Contact sender) {
        this.sender = sender;
        return this;
    }

    public MessageInfo setStatus(MessageStatus status) {
        this.status = status;
        return this;
    }

    public MessageInfo setStarred(boolean starred) {
        this.starred = starred;
        return this;
    }

    public MessageInfo setRevokeTimestampSeconds(long revokeTimestampSeconds) {
        this.revokeTimestampSeconds = revokeTimestampSeconds;
        return this;
    }
}
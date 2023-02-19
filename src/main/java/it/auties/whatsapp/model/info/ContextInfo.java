package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.annotation.JsonBackReference;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatDisappear;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.model.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that holds the information related to a {@link ContextualMessage}.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
@ToString(exclude = "quotedMessageChat")
public final class ContextInfo implements Info {
    /**
     * The jid of the message that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 1, type = STRING)
    @Setter(AccessLevel.NONE)
    private String quotedMessageId;

    /**
     * The jid of the contact that sent the message that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 2, type = STRING, implementation = ContactJid.class)
    @Setter(AccessLevel.NONE)
    private ContactJid quotedMessageSenderJid;

    /**
     * The contact that sent the message that this ContextualMessage quotes
     */
    private Contact quotedMessageSender;

    /**
     * The message container that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = MessageContainer.class)
    @Setter(AccessLevel.NONE)
    private MessageContainer quotedMessage;

    /**
     * The jid of the contact that sent the message that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 4, type = STRING, implementation = ContactJid.class)
    @Setter(AccessLevel.NONE)
    private ContactJid quotedMessageChatJid;

    /**
     * The contact that sent the message that this ContextualMessage quotes
     */
    @JsonBackReference
    private Chat quotedMessageChat;

    /**
     * A list of the contacts' jids mentioned in this ContextualMessage
     */
    @ProtobufProperty(index = 15, type = STRING, repeated = true, implementation = ContactJid.class)
    private List<ContactJid> mentions;

    /**
     * Conversation source
     */
    @ProtobufProperty(index = 18, type = STRING)
    private String conversionSource;

    /**
     * Conversation data
     */
    @ProtobufProperty(index = 19, type = BYTES)
    private byte[] conversionData;

    /**
     * Conversation delay in endTimeStamp
     */
    @ProtobufProperty(index = 20, type = UINT32)
    private int conversionDelaySeconds;

    /**
     * Forwarding score
     */
    @ProtobufProperty(index = 21, type = UINT32)
    private int forwardingScore;

    /**
     * Whether this ContextualMessage is forwarded
     */
    @ProtobufProperty(index = 22, type = BOOL)
    private boolean forwarded;

    /**
     * The ad that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 23, type = MESSAGE, implementation = AdReplyInfo.class)
    private AdReplyInfo quotedAd;

    /**
     * Placeholder key
     */
    @ProtobufProperty(index = 24, type = MESSAGE, implementation = MessageKey.class)
    private MessageKey placeholderKey;

    /**
     * The expiration in seconds for this ContextualMessage. Only valid if the chat where this message
     * was sent is ephemeral.
     */
    @ProtobufProperty(index = 25, type = UINT32)
    private int ephemeralExpiration;

    /**
     * The timestamp, that is the seconds in seconds since {@link java.time.Instant#EPOCH}, of the
     * last modification to the ephemeral settings for the chat where this ContextualMessage was
     * sent.
     */
    @ProtobufProperty(index = 26, type = INT64)
    private long ephemeralSettingTimestamp;

    /**
     * Ephemeral shared secret
     */
    @ProtobufProperty(index = 27, type = BYTES)
    private byte[] ephemeralSharedSecret;

    /**
     * External ad reply
     */
    @ProtobufProperty(index = 28, type = MESSAGE, implementation = ExternalAdReplyInfo.class)
    private ExternalAdReplyInfo externalAdReply;

    /**
     * Entry point conversion source
     */
    @ProtobufProperty(index = 29, type = STRING)
    private String entryPointConversionSource;

    /**
     * Entry point conversion app
     */
    @ProtobufProperty(index = 30, type = STRING)
    private String entryPointConversionApp;

    /**
     * Entry point conversion delay in endTimeStamp
     */
    @ProtobufProperty(index = 31, type = UINT32)
    private int entryPointConversionDelaySeconds;

    /**
     * Disappearing mode
     */
    @ProtobufProperty(index = 32, type = MESSAGE, implementation = ChatDisappear.class)
    private ChatDisappear disappearingMode;

    /**
     * Action link
     */
    @ProtobufProperty(index = 33, type = STRING)
    private ActionLink actionLink;

    /**
     * Group subject
     */
    @ProtobufProperty(index = 34, type = STRING)
    private String groupSubject;

    /**
     * Parent group
     */
    @ProtobufProperty(index = 35, type = STRING, implementation = ContactJid.class)
    private ContactJid parentGroup;

    /**
     * Trust banner type
     */
    @ProtobufProperty(index = 37, name = "trustBannerType", type = STRING)
    private String trustBannerType;

    /**
     * Trust banner action
     */
    @ProtobufProperty(index = 38, name = "trustBannerAction", type = UINT32)
    private Integer trustBannerAction;

    private ContextInfo(@NonNull MessageMetadataProvider quotedMessage) {
        this.quotedMessageId = quotedMessage.id();
        this.quotedMessageSenderJid = quotedMessage.sender().map(Contact::jid).orElse(null);
        this.quotedMessageSender = quotedMessage.sender().orElse(null);
        this.quotedMessageChatJid = quotedMessage.chat().jid();
        this.quotedMessageChat = quotedMessage.chat();
        this.quotedMessage = quotedMessage.message();
    }

    /**
     * Constructs a ContextInfo from a quoted message
     *
     * @param quotedMessage the message to quote
     * @return a non-null context info
     */
    public static ContextInfo of(@NonNull MessageMetadataProvider quotedMessage) {
        return new ContextInfo(quotedMessage);
    }

    /**
     * Returns the sender of the quoted message
     *
     * @return an optional
     */
    public Optional<Contact> quotedMessageSender() {
        return Optional.ofNullable(quotedMessageSender);
    }

    /**
     * Returns the chat jid of the quoted message
     *
     * @return an optional
     */
    public Optional<ContactJid> quotedMessageChatJid() {
        return Optional.ofNullable(quotedMessageChatJid).or(this::quotedMessageSenderJid);
    }

    /**
     * Returns the jid of the sender of the quoted message
     *
     * @return an optional
     */
    public Optional<ContactJid> quotedMessageSenderJid() {
        return Optional.ofNullable(quotedMessageSenderJid);
    }

    /**
     * Returns whether this context info has information about a quoted message
     *
     * @return a boolean
     */
    public boolean hasQuotedMessage() {
        return quotedMessageId().isPresent() && quotedMessage().isPresent() && quotedMessageChat().isPresent();
    }

    /**
     * Returns the id of the quoted message
     *
     * @return an optional
     */
    public Optional<String> quotedMessageId() {
        return Optional.ofNullable(quotedMessageId);
    }

    /**
     * Returns the quoted message
     *
     * @return an optional
     */
    public Optional<MessageContainer> quotedMessage() {
        return Optional.ofNullable(quotedMessage);
    }

    /**
     * Returns the chat of the quoted message
     *
     * @return an optional
     */
    public Optional<Chat> quotedMessageChat() {
        return Optional.ofNullable(quotedMessageChat);
    }
}
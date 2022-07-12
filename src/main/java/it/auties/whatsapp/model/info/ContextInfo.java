package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.annotation.JsonBackReference;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatDisappear;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.model.ActionLink;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.payment.PaymentOrderMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Optional;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that holds the information related to a {@link ContextualMessage}.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder(builderMethodName = "newContextInfo")
@Accessors(fluent = true)
public sealed class ContextInfo implements Info permits PaymentOrderMessage {
    /**
     * The jid of the message that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String quotedMessageId;

    /**
     * The jid of the contact that sent the message that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 2, type = STRING, concreteType = ContactJid.class, requiresConversion = true)
    @Setter(AccessLevel.NONE)
    private ContactJid quotedMessageSenderJid;

    /**
     * The contact that sent the message that this ContextualMessage quotes
     */
    private Contact quotedMessageSender;

    /**
     * The message container that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = MessageContainer.class)
    @Setter(AccessLevel.NONE)
    private MessageContainer quotedMessageContainer;

    /**
     * The message that this ContextualMessage quotes
     */
    private MessageInfo quotedMessage;

    /**
     * The jid of the contact that sent the message that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 4, type = STRING, concreteType = ContactJid.class, requiresConversion = true)
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
    @ProtobufProperty(index = 15, type = STRING, repeated = true, concreteType = ContactJid.class, requiresConversion = true)
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
    @ProtobufProperty(index = 22, type = BOOLEAN)
    private boolean forwarded;

    /**
     * The ad that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 23, type = MESSAGE, concreteType = AdReplyInfo.class)
    private AdReplyInfo quotedAd;

    /**
     * Placeholder key
     */
    @ProtobufProperty(index = 24, type = MESSAGE, concreteType = MessageKey.class)
    private MessageKey placeholderKey;

    /**
     * The expiration in endTimeStamp for this ContextualMessage.
     * Only valid if the chat where this message was sent is ephemeral.
     */
    @ProtobufProperty(index = 25, type = UINT32)
    private int ephemeralExpiration;

    /**
     * The timestamp, that is the endTimeStamp in endTimeStamp since {@link java.time.Instant#EPOCH}, of the last modification to the ephemeral settings
     * for the chat where this ContextualMessage was sent.
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
    @ProtobufProperty(index = 28, type = MESSAGE, concreteType = ExternalAdReplyInfo.class)
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
    @ProtobufProperty(index = 32, type = MESSAGE, concreteType = ChatDisappear.class)
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
    @ProtobufProperty(index = 35, type = STRING, concreteType = ContactJid.class, requiresConversion = true)
    private ContactJid parentGroup;

    private ContextInfo(@NonNull MessageInfo quotedMessage) {
        this.quotedMessageId = quotedMessage.key()
                .id();
        this.quotedMessageSenderJid = quotedMessage.senderJid();
        this.quotedMessageSender = quotedMessage.sender()
                .orElse(null);
        this.quotedMessageChatJid = quotedMessage.chatJid();
        this.quotedMessageChat = quotedMessage.chat()
                .orElse(null);
        this.quotedMessage = quotedMessage;
        this.quotedMessageContainer = quotedMessage.message();
    }

    /**
     * Constructs a ContextInfo from a quoted message
     *
     * @param quotedMessage the message to quote
     * @return a non-null context info
     */
    public static ContextInfo of(@NonNull MessageInfo quotedMessage) {
        return new ContextInfo(quotedMessage);
    }

    /**
     * Returns the quoted message info
     *
     * @return an optional
     */
    public Optional<MessageInfo> quotedMessage() {
        return Optional.ofNullable(quotedMessage);
    }

    /**
     * Returns the quoted message
     *
     * @return an optional
     */
    public Optional<MessageContainer> quotedMessageContainer() {
        return Optional.ofNullable(quotedMessage != null ?
                quotedMessage.message() :
                quotedMessageContainer);
    }
}

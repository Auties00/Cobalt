package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.base.ButtonActionLink;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatDisappear;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.MessageContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A model class that holds the information related to a {@link it.auties.whatsapp.model.message.model.ContextualMessage}.
 */
@ProtobufMessage(name = "ContextInfo")
public final class ContextInfo implements Info {
    /**
     * The jid of the message that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String quotedMessageId;

    /**
     * The jid of the contact that sent the message that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final Jid quotedMessageSenderJid;

    /**
     * The message container that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private final MessageContainer quotedMessage;

    /**
     * The jid of the contact that sent the message that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    private final Jid quotedMessageChatJid;

    /**
     * A list of the contacts' jids mentioned in this ContextualMessage
     */
    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    private final List<Jid> mentions;

    /**
     * Conversation source
     */
    @ProtobufProperty(index = 18, type = ProtobufType.STRING)
    private final String conversionSource;

    /**
     * Conversation data
     */
    @ProtobufProperty(index = 19, type = ProtobufType.BYTES)
    private final byte[] conversionData;

    /**
     * Conversation delay in endTimeStamp
     */
    @ProtobufProperty(index = 20, type = ProtobufType.UINT32)
    private final int conversionDelaySeconds;

    /**
     * Forwarding score
     */
    @ProtobufProperty(index = 21, type = ProtobufType.UINT32)
    private final int forwardingScore;

    /**
     * Whether this ContextualMessage is forwarded
     */
    @ProtobufProperty(index = 22, type = ProtobufType.BOOL)
    private final boolean forwarded;

    /**
     * The ad that this ContextualMessage quotes
     */
    @ProtobufProperty(index = 23, type = ProtobufType.MESSAGE)
    private final AdReplyInfo quotedAd;

    /**
     * Placeholder key
     */
    @ProtobufProperty(index = 24, type = ProtobufType.MESSAGE)
    private final ChatMessageKey placeholderKey;

    /**
     * The expiration in seconds for this ContextualMessage. Only valid if the chat where this message
     * was sent is ephemeral.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.UINT32)
    private int ephemeralExpiration;
    /**
     * The timestampSeconds, that is the seconds in seconds since {@link java.time.Instant#EPOCH}, of the
     * last modification to the ephemeral settings for the chat where this ContextualMessage was
     * sent.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.INT64)
    private long ephemeralSettingTimestamp;

    /**
     * Ephemeral shared secret
     */
    @ProtobufProperty(index = 27, type = ProtobufType.BYTES)
    private final byte[] ephemeralSharedSecret;

    /**
     * External ad reply
     */
    @ProtobufProperty(index = 28, type = ProtobufType.MESSAGE)
    private final ExternalAdReplyInfo externalAdReply;

    /**
     * Entry point conversion source
     */
    @ProtobufProperty(index = 29, type = ProtobufType.STRING)
    private final String entryPointConversionSource;

    /**
     * Entry point conversion app
     */
    @ProtobufProperty(index = 30, type = ProtobufType.STRING)
    private final String entryPointConversionApp;

    /**
     * Entry point conversion delay in endTimeStamp
     */
    @ProtobufProperty(index = 31, type = ProtobufType.UINT32)
    private final int entryPointConversionDelaySeconds;

    /**
     * Disappearing mode
     */
    @ProtobufProperty(index = 32, type = ProtobufType.MESSAGE)
    private final ChatDisappear disappearingMode;

    /**
     * Action link
     */
    @ProtobufProperty(index = 33, type = ProtobufType.MESSAGE)
    private final ButtonActionLink actionLink;

    /**
     * Group subject
     */
    @ProtobufProperty(index = 34, type = ProtobufType.STRING)
    private final String groupSubject;

    /**
     * Parent group
     */
    @ProtobufProperty(index = 35, type = ProtobufType.STRING)
    private final Jid parentGroup;

    /**
     * Trust banner type
     */
    @ProtobufProperty(index = 37, type = ProtobufType.STRING)
    private final String trustBannerType;

    /**
     * Trust banner action
     */
    @ProtobufProperty(index = 38, type = ProtobufType.UINT32)
    private final int trustBannerAction;

    /**
     * The contact that sent the message that this ContextualMessage quotes
     */
    private Contact quotedMessageSender;

    /**
     * The contact that sent the message that this ContextualMessage quotes
     */
    @JsonBackReference
    private Chat quotedMessageChat;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ContextInfo(String quotedMessageId, Jid quotedMessageSenderJid, MessageContainer quotedMessage, Jid quotedMessageChatJid, List<Jid> mentions, String conversionSource, byte[] conversionData, int conversionDelaySeconds, int forwardingScore, boolean forwarded, AdReplyInfo quotedAd, ChatMessageKey placeholderKey, int ephemeralExpiration, long ephemeralSettingTimestamp, byte[] ephemeralSharedSecret, ExternalAdReplyInfo externalAdReply, String entryPointConversionSource, String entryPointConversionApp, int entryPointConversionDelaySeconds, ChatDisappear disappearingMode, ButtonActionLink actionLink, String groupSubject, Jid parentGroup, String trustBannerType, int trustBannerAction) {
        this.quotedMessageId = quotedMessageId;
        this.quotedMessageSenderJid = quotedMessageSenderJid;
        this.quotedMessage = quotedMessage;
        this.quotedMessageChatJid = quotedMessageChatJid;
        this.mentions = mentions;
        this.conversionSource = conversionSource;
        this.conversionData = conversionData;
        this.conversionDelaySeconds = conversionDelaySeconds;
        this.forwardingScore = forwardingScore;
        this.forwarded = forwarded;
        this.quotedAd = quotedAd;
        this.placeholderKey = placeholderKey;
        this.ephemeralExpiration = ephemeralExpiration;
        this.ephemeralSettingTimestamp = ephemeralSettingTimestamp;
        this.ephemeralSharedSecret = ephemeralSharedSecret;
        this.externalAdReply = externalAdReply;
        this.entryPointConversionSource = entryPointConversionSource;
        this.entryPointConversionApp = entryPointConversionApp;
        this.entryPointConversionDelaySeconds = entryPointConversionDelaySeconds;
        this.disappearingMode = disappearingMode;
        this.actionLink = actionLink;
        this.groupSubject = groupSubject;
        this.parentGroup = parentGroup;
        this.trustBannerType = trustBannerType;
        this.trustBannerAction = trustBannerAction;
    }

    public static ContextInfo of(MessageInfo<?> quotedMessage) {
        return new ContextInfoBuilder()
                .quotedMessageId(quotedMessage.id())
                .quotedMessageSenderJid(quotedMessage.senderJid())
                .quotedMessage(quotedMessage.message())
                .quotedMessageChatJid(quotedMessage.parentJid())
                .mentions(new ArrayList<>())
                .build();
    }


    public static ContextInfo of(ContextInfo contextInfo, MessageInfo<?> quotedMessage) {
        return contextInfo == null ? of(quotedMessage) : new ContextInfoBuilder()
                .quotedMessageId(quotedMessage.id())
                .quotedMessageSenderJid(quotedMessage.senderJid())
                .quotedMessage(quotedMessage.message())
                .quotedMessageChatJid(quotedMessage.parentJid())
                .mentions(new ArrayList<>())
                .conversionSource(contextInfo.conversionSource)
                .conversionData(contextInfo.conversionData)
                .conversionDelaySeconds(contextInfo.conversionDelaySeconds)
                .forwardingScore(contextInfo.forwardingScore)
                .forwarded(contextInfo.forwarded)
                .quotedAd(contextInfo.quotedAd)
                .placeholderKey(contextInfo.placeholderKey)
                .ephemeralExpiration(contextInfo.ephemeralExpiration)
                .ephemeralSettingTimestamp(contextInfo.ephemeralSettingTimestamp)
                .ephemeralSharedSecret(contextInfo.ephemeralSharedSecret)
                .externalAdReply(contextInfo.externalAdReply)
                .entryPointConversionSource(contextInfo.entryPointConversionSource)
                .entryPointConversionApp(contextInfo.entryPointConversionApp)
                .entryPointConversionDelaySeconds(contextInfo.entryPointConversionDelaySeconds)
                .disappearingMode(contextInfo.disappearingMode)
                .actionLink(contextInfo.actionLink)
                .groupSubject(contextInfo.groupSubject)
                .parentGroup(contextInfo.parentGroup)
                .trustBannerType(contextInfo.trustBannerType)
                .trustBannerAction(contextInfo.trustBannerAction)
                .build();

    }

    public static ContextInfo empty() {
        return new ContextInfoBuilder()
                .mentions(new ArrayList<>())
                .build();
    }

    /**
     * Returns the sender of the quoted message
     *
     * @return an optional
     */
    public Optional<Contact> quotedMessageSender() {
        return Optional.ofNullable(quotedMessageSender);
    }

    public ContextInfo setQuotedMessageSender(Contact quotedMessageSender) {
        this.quotedMessageSender = quotedMessageSender;
        return this;
    }

    /**
     * Returns the chat jid of the quoted message
     *
     * @return an optional
     */
    public Optional<Jid> quotedMessageChatJid() {
        return Optional.ofNullable(quotedMessageChatJid).or(this::quotedMessageSenderJid);
    }

    /**
     * Returns the jid of the sender of the quoted message
     *
     * @return an optional
     */
    public Optional<Jid> quotedMessageSenderJid() {
        return Optional.ofNullable(quotedMessageSenderJid);
    }

    /**
     * Returns whether this context info has information about a quoted message
     *
     * @return a boolean
     */
    public boolean hasQuotedMessage() {
        return quotedMessageId().isPresent()
                && quotedMessage().isPresent()
                && quotedMessageChat().isPresent();
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


    public ContextInfo setQuotedMessageChat(Chat quotedMessageChat) {
        this.quotedMessageChat = quotedMessageChat;
        return this;
    }

    public List<Jid> mentions() {
        return mentions;
    }

    public Optional<String> conversionSource() {
        return Optional.ofNullable(conversionSource);
    }

    public Optional<byte[]> conversionData() {
        return Optional.ofNullable(conversionData);
    }

    public int conversionDelaySeconds() {
        return conversionDelaySeconds;
    }

    public int forwardingScore() {
        return forwardingScore;
    }

    public boolean forwarded() {
        return forwarded;
    }

    public Optional<AdReplyInfo> quotedAd() {
        return Optional.ofNullable(quotedAd);
    }

    public Optional<ChatMessageKey> placeholderKey() {
        return Optional.ofNullable(placeholderKey);
    }

    public int ephemeralExpiration() {
        return ephemeralExpiration;
    }

    public ContextInfo setEphemeralExpiration(int ephemeralExpiration) {
        this.ephemeralExpiration = ephemeralExpiration;
        return this;
    }

    public long ephemeralSettingTimestamp() {
        return ephemeralSettingTimestamp;
    }

    public ContextInfo setEphemeralSettingTimestamp(long ephemeralSettingTimestamp) {
        this.ephemeralSettingTimestamp = ephemeralSettingTimestamp;
        return this;
    }

    public Optional<byte[]> ephemeralSharedSecret() {
        return Optional.ofNullable(ephemeralSharedSecret);
    }

    public Optional<ExternalAdReplyInfo> externalAdReply() {
        return Optional.ofNullable(externalAdReply);
    }

    public Optional<String> entryPointConversionSource() {
        return Optional.ofNullable(entryPointConversionSource);
    }

    public Optional<String> entryPointConversionApp() {
        return Optional.ofNullable(entryPointConversionApp);
    }

    public int entryPointConversionDelaySeconds() {
        return entryPointConversionDelaySeconds;
    }

    public Optional<ChatDisappear> disappearingMode() {
        return Optional.ofNullable(disappearingMode);
    }

    public Optional<ButtonActionLink> actionLink() {
        return Optional.ofNullable(actionLink);
    }

    public Optional<String> groupSubject() {
        return Optional.ofNullable(groupSubject);
    }

    public Optional<Jid> parentGroup() {
        return Optional.ofNullable(parentGroup);
    }

    public Optional<String> trustBannerType() {
        return Optional.ofNullable(trustBannerType);
    }

    public int trustBannerAction() {
        return trustBannerAction;
    }
}
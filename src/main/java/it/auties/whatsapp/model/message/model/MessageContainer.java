package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.info.CallInfo;
import it.auties.whatsapp.model.info.DeviceContextInfo;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.payment.*;
import it.auties.whatsapp.model.message.server.*;
import it.auties.whatsapp.model.message.standard.*;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A container for all types of messages known currently to WhatsappWeb.
 * <p>
 * Only one of these properties should be populated, however it's not certain as Whatsapp's Protobuf
 * doesn't use a oneof instruction as it would be logical to in said case. This may imply that in
 * some particular and rare cases more than one property can be populated.
 * <p>
 * There are several categories of messages:
 * <ul>
 * <li>Server messages</li>
 * <li>Button messages</li>
 * <li>Product messages</li>
 * <li>Payment messages</li>
 * <li>Standard messages</li>
 * </ul>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("Message")
public class MessageContainer implements ProtobufMessage {
    /**
     * An empty message
     */
    private static final EmptyMessage EMPTY_MESSAGE = new EmptyMessage();

    @ProtobufProperty(index = 1, type = STRING)
    private String textWithNoContextMessage;

    /**
     * Sender key distribution message
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = SenderKeyDistributionMessage.class)
    private SenderKeyDistributionMessage senderKeyDistributionMessage;

    /**
     * Image message
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ImageMessage.class)
    private ImageMessage imageMessage;

    /**
     * Contact message
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = ContactMessage.class)
    private ContactMessage contactMessage;

    /**
     * Location message
     */
    @ProtobufProperty(index = 5, type = MESSAGE, implementation = LocationMessage.class)
    private LocationMessage locationMessage;

    /**
     * Text message
     */
    @ProtobufProperty(index = 6, type = MESSAGE, implementation = TextMessage.class)
    private TextMessage textMessage;

    /**
     * Document message
     */
    @ProtobufProperty(index = 7, type = MESSAGE, implementation = DocumentMessage.class)
    private DocumentMessage documentMessage;

    /**
     * Audio message
     */
    @ProtobufProperty(index = 8, type = MESSAGE, implementation = AudioMessage.class)
    private AudioMessage audioMessage;

    /**
     * Video message
     */
    @ProtobufProperty(index = 9, type = MESSAGE, implementation = VideoMessage.class)
    private VideoMessage videoMessage;

    /**
     * Call message
     */
    @ProtobufProperty(index = 10, type = MESSAGE, implementation = CallInfo.class)
    private CallInfo call;

    /**
     * Sever message
     */
    @ProtobufProperty(index = 12, type = MESSAGE, implementation = ProtocolMessage.class)
    private ProtocolMessage protocolMessage;

    /**
     * Contact array message
     */
    @ProtobufProperty(index = 13, type = MESSAGE, implementation = ContactsArrayMessage.class)
    private ContactsArrayMessage contactsArrayMessage;

    /**
     * Highly structured message
     */
    @ProtobufProperty(index = 14, type = MESSAGE, implementation = HighlyStructuredMessage.class)
    private HighlyStructuredMessage highlyStructuredMessage;

    /**
     * Send payment message
     */
    @ProtobufProperty(index = 16, type = MESSAGE, implementation = SendPaymentMessage.class)
    private SendPaymentMessage sendPaymentMessage;

    /**
     * Live location message
     */
    @ProtobufProperty(index = 18, type = MESSAGE, implementation = LiveLocationMessage.class)
    private LiveLocationMessage liveLocationMessage;

    /**
     * Request payment message
     */
    @ProtobufProperty(index = 22, type = MESSAGE, implementation = RequestPaymentMessage.class)
    private RequestPaymentMessage requestPaymentMessage;

    /**
     * Decline payment request message
     */
    @ProtobufProperty(index = 23, type = MESSAGE, implementation = DeclinePaymentRequestMessage.class)
    private DeclinePaymentRequestMessage declinePaymentRequestMessage;

    /**
     * Cancel payment request message
     */
    @ProtobufProperty(index = 24, type = MESSAGE, implementation = CancelPaymentRequestMessage.class)
    private CancelPaymentRequestMessage cancelPaymentRequestMessage;

    /**
     * Template message
     */
    @ProtobufProperty(index = 25, type = MESSAGE, implementation = TemplateMessage.class)
    private TemplateMessage templateMessage;

    /**
     * Sticker message
     */
    @ProtobufProperty(index = 26, type = MESSAGE, implementation = StickerMessage.class)
    private StickerMessage stickerMessage;

    /**
     * Group invite message
     */
    @ProtobufProperty(index = 28, type = MESSAGE, implementation = GroupInviteMessage.class)
    private GroupInviteMessage groupInviteMessage;

    /**
     * Template button reply message
     */
    @ProtobufProperty(index = 29, type = MESSAGE, implementation = TemplateReplyMessage.class)
    private TemplateReplyMessage templateReplyMessage;

    /**
     * Product message
     */
    @ProtobufProperty(index = 30, type = MESSAGE, implementation = ProductMessage.class)
    private ProductMessage productMessage;

    /**
     * Device sent message
     */
    @ProtobufProperty(index = 31, type = MESSAGE, implementation = DeviceSentMessage.class)
    private DeviceSentMessage deviceSentMessage;

    /**
     * Device dataSync message
     */
    @ProtobufProperty(index = 32, type = MESSAGE, implementation = DeviceSyncMessage.class)
    private DeviceSyncMessage deviceSyncMessage;

    /**
     * List message
     */
    @ProtobufProperty(index = 36, type = MESSAGE, implementation = ListMessage.class)
    private ListMessage listMessage;

    /**
     * View once message
     */
    @ProtobufProperty(index = 37, type = MESSAGE, implementation = FutureMessageContainer.class)
    private FutureMessageContainer viewOnceMessage;

    /**
     * Order message
     */
    @ProtobufProperty(index = 38, type = MESSAGE, implementation = PaymentOrderMessage.class)
    private PaymentOrderMessage orderMessage;

    /**
     * List response message
     */
    @ProtobufProperty(index = 39, type = MESSAGE, implementation = ListResponseMessage.class)
    private ListResponseMessage listResponseMessage;

    /**
     * Ephemeral message
     */
    @ProtobufProperty(index = 40, type = MESSAGE, implementation = FutureMessageContainer.class)
    private FutureMessageContainer ephemeralMessage;

    /**
     * Invoice message
     */
    @ProtobufProperty(index = 41, type = MESSAGE, implementation = PaymentInvoiceMessage.class)
    private PaymentInvoiceMessage invoiceMessage;

    /**
     * Buttons message
     */
    @ProtobufProperty(index = 42, type = MESSAGE, implementation = ButtonsMessage.class)
    private ButtonsMessage buttonsMessage;

    /**
     * Buttons response message
     */
    @ProtobufProperty(index = 43, type = MESSAGE, implementation = ButtonsResponseMessage.class)
    private ButtonsResponseMessage buttonsResponseMessage;

    /**
     * Payment invite message
     */
    @ProtobufProperty(index = 44, type = MESSAGE, implementation = PaymentInviteMessage.class)
    private PaymentInviteMessage paymentInviteMessage;

    /**
     * Interactive message
     */
    @ProtobufProperty(index = 45, type = MESSAGE, implementation = InteractiveMessage.class)
    private InteractiveMessage interactiveMessage;

    /**
     * Reaction message
     */
    @ProtobufProperty(index = 46, type = MESSAGE, implementation = ReactionMessage.class)
    private ReactionMessage reactionMessage;

    /**
     * Sticker sync message
     */
    @ProtobufProperty(index = 47, type = MESSAGE, implementation = StickerSyncRMRMessage.class)
    private StickerSyncRMRMessage stickerSyncMessage;

    /**
     * Interactive response
     */
    @ProtobufProperty(index = 48, name = "interactiveResponseMessage", type = MESSAGE)
    private InteractiveResponseMessage interactiveResponseMessage;

    /**
     * Poll creation
     */
    @ProtobufProperty(index = 49, name = "pollCreationMessage", type = MESSAGE)
    private PollCreationMessage pollCreationMessage;

    /**
     * Poll update
     */
    @ProtobufProperty(index = 50, name = "pollUpdateMessage", type = MESSAGE)
    private PollUpdateMessage pollUpdateMessage;

    /**
     * Keep in chat
     */
    @ProtobufProperty(index = 51, name = "keepInChatMessage", type = MESSAGE)
    private KeepInChatMessage keepInChatMessage;

    /**
     * Document with caption
     */
    @ProtobufProperty(index = 53, name = "documentWithCaptionMessage", type = MESSAGE)
    private FutureMessageContainer documentWithCaptionMessage;

    /**
     * Request phone number
     */
    @ProtobufProperty(index = 54, name = "requestPhoneNumberMessage", type = MESSAGE)
    private RequestPhoneNumberMessage requestPhoneNumberMessage;

    /**
     * View once v2
     */
    @ProtobufProperty(index = 55, name = "viewOnceMessageV2", type = MESSAGE)
    private FutureMessageContainer viewOnceV2Message;

    /**
     * Encrypted reaction
     */
    @ProtobufProperty(index = 56, name = "encReactionMessage", type = MESSAGE)
    private EncryptedReactionMessage encryptedReactionMessage;

    /**
     * Edited
     */
    @ProtobufProperty(index = 58, name = "editedMessage", type = MESSAGE)
    private FutureMessageContainer editedMessage;

    /**
     * View once v2 extension
     */
    @ProtobufProperty(index = 59, name = "viewOnceMessageV2Extension", type = MESSAGE)
    private FutureMessageContainer viewOnceV2ExtensionMessage;

    /**
     * Message context info
     */
    @ProtobufProperty(index = 35, type = MESSAGE, implementation = DeviceContextInfo.class)
    @Getter
    @Default
    private DeviceContextInfo deviceInfo = DeviceContextInfo.of();

    /**
     * Constructs a new MessageContainerBuilder from a message of any type
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     * @return a non-null builder
     */
    public static <T extends Message> MessageContainerBuilder builder(@NonNull T message) {
        var builder = MessageContainer.builder();
        switch(message) {
            case EmptyMessage ignored -> {}
            case SenderKeyDistributionMessage senderKeyDistribution -> builder.senderKeyDistributionMessage(senderKeyDistribution);
            case ImageMessage image -> builder.imageMessage(image);
            case ContactMessage contact -> builder.contactMessage(contact);
            case LocationMessage location -> builder.locationMessage(location);
            case TextMessage text -> builder.textMessage(text);
            case DocumentMessage document -> builder.documentMessage(document);
            case AudioMessage audio -> builder.audioMessage(audio);
            case VideoMessage video -> builder.videoMessage(video);
            case ProtocolMessage protocol -> builder.protocolMessage(protocol);
            case ContactsArrayMessage contactsArray -> builder.contactsArrayMessage(contactsArray);
            case HighlyStructuredMessage highlyStructured -> builder.highlyStructuredMessage(highlyStructured);
            case SendPaymentMessage sendPayment -> builder.sendPaymentMessage(sendPayment);
            case LiveLocationMessage liveLocation -> builder.liveLocationMessage(liveLocation);
            case RequestPaymentMessage requestPayment -> builder.requestPaymentMessage(requestPayment);
            case DeclinePaymentRequestMessage declinePaymentRequest -> builder.declinePaymentRequestMessage(declinePaymentRequest);
            case CancelPaymentRequestMessage cancelPaymentRequest -> builder.cancelPaymentRequestMessage(cancelPaymentRequest);
            case it.auties.whatsapp.model.message.button.TemplateMessage template -> builder.templateMessage(template);
            case StickerMessage sticker -> builder.stickerMessage(sticker);
            case GroupInviteMessage groupInvite -> builder.groupInviteMessage(groupInvite);
            case TemplateReplyMessage templateButtonReply -> builder.templateReplyMessage(templateButtonReply);
            case ProductMessage product -> builder.productMessage(product);
            case DeviceSyncMessage deviceSync -> builder.deviceSyncMessage(deviceSync);
            case ListMessage buttonsList -> builder.listMessage(buttonsList);
            case PaymentOrderMessage order -> builder.orderMessage(order);
            case ListResponseMessage listResponse -> builder.listResponseMessage(listResponse);
            case PaymentInvoiceMessage invoice -> builder.invoiceMessage(invoice);
            case ButtonsMessage buttons -> builder.buttonsMessage(buttons);
            case ButtonsResponseMessage buttonsResponse -> builder.buttonsResponseMessage(buttonsResponse);
            case PaymentInviteMessage paymentInvite -> builder.paymentInviteMessage(paymentInvite);
            case InteractiveMessage interactive -> builder.interactiveMessage(interactive);
            case ReactionMessage reaction -> builder.reactionMessage(reaction);
            case StickerSyncRMRMessage stickerSync -> builder.stickerSyncMessage(stickerSync);
            case DeviceSentMessage deviceSent -> builder.deviceSentMessage(deviceSent);
            case InteractiveResponseMessage interactiveResponseMessage ->
                    builder.interactiveResponseMessage(interactiveResponseMessage);
            case PollCreationMessage pollCreationMessage -> builder.pollCreationMessage(pollCreationMessage);
            case PollUpdateMessage pollUpdateMessage -> builder.pollUpdateMessage(pollUpdateMessage);
            case KeepInChatMessage keepInChatMessage -> builder.keepInChatMessage(keepInChatMessage);
            case RequestPhoneNumberMessage requestPhoneNumberMessage ->
                    builder.requestPhoneNumberMessage(requestPhoneNumberMessage);
            case EncryptedReactionMessage encReactionMessage -> builder.encryptedReactionMessage(encReactionMessage);
            default -> throw new IllegalStateException("Unsupported message: " + message);
        }
        return builder;
    }

    /**
     * Constructs a new MessageContainer from a text message
     *
     * @param message the text message with no context
     */
    public static MessageContainer of(@NonNull String message) {
        return MessageContainer.builder().textMessage(TextMessage.of(message)).build();
    }

    /**
     * Constructs a new MessageContainerBuilder
     *
     * @return a non-null builder
     */
    public static MessageContainerBuilder builder() {
        return new MessageContainerBuilder();
    }

    /**
     * Constructs a new MessageContainer from a message of any type
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     * @return a non-null container
     */
    public static <T extends Message> MessageContainer of(@NonNull T message) {
        return builder(message).build();
    }

    /**
     * Constructs a new MessageContainer from a message of any type that can only be seen once
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofViewOnce(@NonNull T message) {
        return MessageContainer.builder().viewOnceMessage(FutureMessageContainer.of(message)).build();
    }

    /**
     * Constructs a new MessageContainer from a message of any type that can only be seen once(version
     * v2)
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofViewOnceV2(@NonNull T message) {
        return MessageContainer.builder().viewOnceV2Message(FutureMessageContainer.of(message)).build();
    }

    /**
     * Constructs a new MessageContainer from a message of any type marking it as ephemeral
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofEphemeral(@NonNull T message) {
        return MessageContainer.builder().ephemeralMessage(FutureMessageContainer.of(message)).build();
    }

    /**
     * Constructs a new MessageContainer from an edited message
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofEditedMessage(@NonNull T message) {
        return MessageContainer.builder().editedMessage(FutureMessageContainer.of(message)).build();
    }

    /**
     * Constructs a new MessageContainer from a document with caption message
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofDocumentWithCaption(@NonNull T message) {
        return MessageContainer.builder().documentWithCaptionMessage(FutureMessageContainer.of(message)).build();
    }

    /**
     * Returns the first populated message inside this container. If no message is found,
     * {@link EmptyMessage} is returned
     *
     * @return a non-null message
     */
    public Message content() {
        if (this.textWithNoContextMessage != null) {
            return TextMessage.of(textWithNoContextMessage);
        }
        if (this.senderKeyDistributionMessage != null) {
            return senderKeyDistributionMessage;
        }
        if (this.imageMessage != null) {
            return imageMessage;
        }
        if (this.contactMessage != null) {
            return contactMessage;
        }
        if (this.locationMessage != null) {
            return locationMessage;
        }
        if (this.textMessage != null) {
            return textMessage;
        }
        if (this.documentMessage != null) {
            return documentMessage;
        }
        if (this.audioMessage != null) {
            return audioMessage;
        }
        if (this.videoMessage != null) {
            return videoMessage;
        }
        if (this.protocolMessage != null) {
            return protocolMessage;
        }
        if (this.contactsArrayMessage != null) {
            return contactsArrayMessage;
        }
        if (this.highlyStructuredMessage != null) {
            return highlyStructuredMessage;
        }
        if (this.sendPaymentMessage != null) {
            return sendPaymentMessage;
        }
        if (this.liveLocationMessage != null) {
            return liveLocationMessage;
        }
        if (this.requestPaymentMessage != null) {
            return requestPaymentMessage;
        }
        if (this.declinePaymentRequestMessage != null) {
            return declinePaymentRequestMessage;
        }
        if (this.cancelPaymentRequestMessage != null) {
            return cancelPaymentRequestMessage;
        }
        if (this.templateMessage != null) {
            return templateMessage;
        }
        if (this.stickerMessage != null) {
            return stickerMessage;
        }
        if (this.groupInviteMessage != null) {
            return groupInviteMessage;
        }
        if (this.templateReplyMessage != null) {
            return templateReplyMessage;
        }
        if (this.productMessage != null) {
            return productMessage;
        }
        if (this.deviceSentMessage != null) {
            return deviceSentMessage.message().content();
        }
        if (this.deviceSyncMessage != null) {
            return deviceSyncMessage;
        }
        if (this.listMessage != null) {
            return listMessage;
        }
        if (this.viewOnceMessage != null) {
            return viewOnceMessage.unbox();
        }
        if (this.orderMessage != null) {
            return orderMessage;
        }
        if (this.listResponseMessage != null) {
            return listResponseMessage;
        }
        if (this.ephemeralMessage != null) {
            return ephemeralMessage.unbox();
        }
        if (this.invoiceMessage != null) {
            return invoiceMessage;
        }
        if (this.buttonsMessage != null) {
            return buttonsMessage;
        }
        if (this.buttonsResponseMessage != null) {
            return buttonsResponseMessage;
        }
        if (this.paymentInviteMessage != null) {
            return paymentInviteMessage;
        }
        if (interactiveMessage != null) {
            return interactiveMessage;
        }
        if (reactionMessage != null) {
            return reactionMessage;
        }
        if (stickerSyncMessage != null) {
            return stickerSyncMessage;
        }
        if (interactiveResponseMessage != null) {
            return interactiveResponseMessage;
        }
        if (pollCreationMessage != null) {
            return pollCreationMessage;
        }
        if (pollUpdateMessage != null) {
            return pollUpdateMessage;
        }
        if (keepInChatMessage != null) {
            return keepInChatMessage;
        }
        if (documentWithCaptionMessage != null) {
            return documentWithCaptionMessage.unbox();
        }
        if (requestPhoneNumberMessage != null) {
            return requestPhoneNumberMessage;
        }
        if (viewOnceV2Message != null) {
            return viewOnceV2Message.unbox();
        }
        if (encryptedReactionMessage != null) {
            return encryptedReactionMessage;
        }
        if (editedMessage != null) {
            return editedMessage.unbox();
        }
        if (viewOnceV2ExtensionMessage != null) {
            return viewOnceV2ExtensionMessage.unbox();
        }
        return EMPTY_MESSAGE;
    }

    /**
     * Returns the first populated contextual message inside this container
     *
     * @return a non-null Optional ContextualMessage
     */
    public Optional<ContextualMessage> contentWithContext() {
        if (this.imageMessage != null) {
            return Optional.of(imageMessage);
        }
        if (this.contactMessage != null) {
            return Optional.of(contactMessage);
        }
        if (this.locationMessage != null) {
            return Optional.of(locationMessage);
        }
        if (this.textMessage != null) {
            return Optional.of(textMessage);
        }
        if (this.documentMessage != null) {
            return Optional.of(documentMessage);
        }
        if (this.audioMessage != null) {
            return Optional.of(audioMessage);
        }
        if (this.videoMessage != null) {
            return Optional.of(videoMessage);
        }
        if (this.contactsArrayMessage != null) {
            return Optional.of(contactsArrayMessage);
        }
        if (this.liveLocationMessage != null) {
            return Optional.of(liveLocationMessage);
        }
        if (this.templateMessage != null) {
            return Optional.of(templateMessage);
        }
        if (this.stickerMessage != null) {
            return Optional.of(stickerMessage);
        }
        if (this.groupInviteMessage != null) {
            return Optional.of(groupInviteMessage);
        }
        if (this.templateReplyMessage != null) {
            return Optional.of(templateReplyMessage);
        }
        if (this.productMessage != null) {
            return Optional.of(productMessage);
        }
        if (this.listMessage != null) {
            return Optional.of(listMessage);
        }
        if (this.invoiceMessage != null) {
            return Optional.of(invoiceMessage);
        }
        if (this.buttonsMessage != null) {
            return Optional.of(buttonsMessage);
        }
        if (this.buttonsResponseMessage != null) {
            return Optional.of(buttonsResponseMessage);
        }
        if (this.viewOnceMessage != null && viewOnceMessage.unbox() instanceof ContextualMessage contextualEphemeral) {
            return Optional.of(contextualEphemeral);
        }
        if (this.ephemeralMessage != null && ephemeralMessage.unbox() instanceof ContextualMessage contextualEphemeral) {
            return Optional.of(contextualEphemeral);
        }
        if (interactiveResponseMessage != null) {
            return Optional.of(interactiveResponseMessage);
        }
        if (pollCreationMessage != null) {
            return Optional.of(pollCreationMessage);
        }
        if (documentWithCaptionMessage != null && documentWithCaptionMessage.unbox() instanceof ContextualMessage contextualEphemeral) {
            return Optional.of(contextualEphemeral);
        }
        if (requestPhoneNumberMessage != null) {
            return Optional.of(requestPhoneNumberMessage);
        }
        if (viewOnceV2Message != null && viewOnceV2Message.unbox() instanceof ContextualMessage contextualEphemeral) {
            return Optional.of(contextualEphemeral);
        }
        if (editedMessage != null && editedMessage.unbox() instanceof ContextualMessage contextualEphemeral) {
            return Optional.of(contextualEphemeral);
        }
        if (viewOnceV2ExtensionMessage != null && viewOnceV2ExtensionMessage.unbox() instanceof ContextualMessage contextualEphemeral) {
            return Optional.of(contextualEphemeral);
        }
        return Optional.empty();
    }

    /**
     * Checks whether the message that this container wraps matches the provided type
     *
     * @param type the non-null type to check against
     * @return a boolean
     */
    public boolean hasType(@NonNull MessageType type) {
        return content().type() == type;
    }

    /**
     * Checks whether the message that this container wraps matches the provided category
     *
     * @param category the non-null category to check against
     * @return a boolean
     */
    public boolean hasCategory(@NonNull MessageCategory category) {
        return content().category() == category;
    }

    /**
     * Returns the type of the message
     *
     * @return a non-null type
     */
    public MessageType type() {
        return ephemeralMessage != null ? MessageType.EPHEMERAL : viewOnceMessage != null || viewOnceV2Message != null || viewOnceV2ExtensionMessage != null ? MessageType.VIEW_ONCE : content().type();
    }

    /**
     * Returns the deep type of the message unwrapping ephemeral and view once messages
     *
     * @return a non-null type
     */
    public MessageType deepType() {
        return content().type();
    }

    /**
     * Returns the category of the message
     *
     * @return a non-null category
     */
    public MessageCategory category() {
        return content().category();
    }

    /**
     * Returns the call wrapped by this message, if any is present
     *
     * @return a non-null optional
     */
    public Optional<CallInfo> call() {
        return Optional.ofNullable(call);
    }

    /**
     * Converts this message to an ephemeral message
     *
     * @return a non-null message container
     */
    public MessageContainer toEphemeral() {
        return type() == MessageType.EPHEMERAL ? this : MessageContainer.builder()
                .ephemeralMessage(FutureMessageContainer.of(content()))
                .call(call)
                .deviceInfo(deviceInfo)
                .build();
    }

    /**
     * Converts this message to a view once message
     *
     * @return a non-null message container
     */
    public MessageContainer toViewOnce() {
        return type() == MessageType.VIEW_ONCE ? this : MessageContainer.builder()
                .viewOnceMessage(FutureMessageContainer.of(content()))
                .call(call)
                .deviceInfo(deviceInfo)
                .build();
    }

    /**
     * Returns an unboxed message where are all future-proof messages(i.e. ephemeral and view once)
     * have been unboxed
     *
     * @return a non-null message container
     */
    public MessageContainer unbox() {
        if (deviceSentMessage != null) {
            return deviceSentMessage.message();
        }
        if (viewOnceMessage != null) {
            return viewOnceMessage.content();
        }
        if (ephemeralMessage != null) {
            return ephemeralMessage.content();
        }
        if (documentWithCaptionMessage != null) {
            return documentWithCaptionMessage.content();
        }
        if (viewOnceV2Message != null) {
            return viewOnceV2Message.content();
        }
        if (editedMessage != null) {
            return editedMessage.content();
        }
        if (viewOnceV2ExtensionMessage != null) {
            return viewOnceV2ExtensionMessage.content();
        }
        return MessageContainer.of(content())
                .toBuilder()
                .call(call)
                .deviceInfo(deviceInfo)
                .build();
    }

    /**
     * Returns whether this container is empty
     *
     * @return a boolean
     */
    public boolean isEmpty() {
        return hasCategory(MessageCategory.EMPTY);
    }

    /**
     * Converts this container into a String
     *
     * @return a non-null String
     */
    @Override
    public String toString() {
        return Objects.toString(content());
    }

    /**
     * Returns an optional string with no context message.
     *
     * @return An Optional containing a String representing the message with no context. It can be empty if there is no message.
     */
    public Optional<String> textWithNoContextMessage() {
        return Optional.ofNullable(textWithNoContextMessage);
    }

    /**
     * Returns an optional SenderKeyDistributionMessage.
     *
     * @return An Optional containing a SenderKeyDistributionMessage. It can be empty if there is no message.
     */
    public Optional<SenderKeyDistributionMessage> senderKeyDistributionMessage() {
        return Optional.ofNullable(senderKeyDistributionMessage);
    }

    /**
     * Returns an optional ImageMessage.
     *
     * @return An Optional containing an ImageMessage. It can be empty if there is no message.
     */
    public Optional<ImageMessage> imageMessage() {
        return Optional.ofNullable(imageMessage);
    }

    /**
     * Returns an optional ContactMessage.
     *
     * @return An Optional containing a ContactMessage. It can be empty if there is no message.
     */
    public Optional<ContactMessage> contactMessage() {
        return Optional.ofNullable(contactMessage);
    }

    /**
     * Returns an optional LocationMessage.
     *
     * @return An Optional containing a LocationMessage. It can be empty if there is no message.
     */
    public Optional<LocationMessage> locationMessage() {
        return Optional.ofNullable(locationMessage);
    }

    /**
     * Returns an optional TextMessage.
     *
     * @return An Optional containing a TextMessage. It can be empty if there is no message.
     */
    public Optional<TextMessage> textMessage() {
        return Optional.ofNullable(textMessage);
    }

    /**
     * Returns an optional DocumentMessage.
     *
     * @return An Optional containing a DocumentMessage. It can be empty if there is no message.
     */
    public Optional<DocumentMessage> documentMessage() {
        return Optional.ofNullable(documentMessage);
    }

    /**
     * Returns an optional AudioMessage.
     *
     * @return An Optional containing an AudioMessage. It can be empty if there is no message.
     */
    public Optional<AudioMessage> audioMessage() {
        return Optional.ofNullable(audioMessage);
    }

    /**
     * Returns an optional VideoMessage.
     *
     * @return An Optional containing a VideoMessage. It can be empty if there is no message.
     */
    public Optional<VideoMessage> videoMessage() {
        return Optional.ofNullable(videoMessage);
    }

    /**
     * Returns an optional ProtocolMessage.
     *
     * @return An Optional containing a ProtocolMessage. It can be empty if there is no message.
     */
    public Optional<ProtocolMessage> protocolMessage() {
        return Optional.ofNullable(protocolMessage);
    }

    /**
     * Returns an optional ContactsArrayMessage.
     *
     * @return An Optional containing a ContactsArrayMessage. It can be empty if there is no message.
     */
    public Optional<ContactsArrayMessage> contactsArrayMessage() {
        return Optional.ofNullable(contactsArrayMessage);
    }

    /**
     * Returns an optional HighlyStructuredMessage.
     *
     * @return An Optional containing a HighlyStructuredMessage. It can be empty if there is no message.
     */
    public Optional<HighlyStructuredMessage> highlyStructuredMessage() {
        return Optional.ofNullable(highlyStructuredMessage);
    }


    /**
     * Returns an {@link Optional} containing the {@link SendPaymentMessage} associated with this message,
     * or an empty Optional if the message does not contain a send payment message.
     *
     * @return an Optional containing the payment message associated with this message, or an empty Optional
     */
    public Optional<SendPaymentMessage> sendPaymentMessage() {
        return Optional.ofNullable(sendPaymentMessage);
    }

    /**
     * Returns an optional instance of LiveLocationMessage.
     *
     * @return an optional instance of LiveLocationMessage, which may be null.
     */
    public Optional<LiveLocationMessage> liveLocationMessage() {
        return Optional.ofNullable(liveLocationMessage);
    }

    /**
     * Returns an optional instance of RequestPaymentMessage.
     *
     * @return an optional instance of RequestPaymentMessage, which may be null.
     */
    public Optional<RequestPaymentMessage> requestPaymentMessage() {
        return Optional.ofNullable(requestPaymentMessage);
    }

    /**
     * Returns an optional instance of DeclinePaymentRequestMessage.
     *
     * @return an optional instance of DeclinePaymentRequestMessage, which may be null.
     */
    public Optional<DeclinePaymentRequestMessage> declinePaymentRequestMessage() {
        return Optional.ofNullable(declinePaymentRequestMessage);
    }

    /**
     * Returns an optional instance of CancelPaymentRequestMessage.
     *
     * @return an optional instance of CancelPaymentRequestMessage, which may be null.
     */
    public Optional<CancelPaymentRequestMessage> cancelPaymentRequestMessage() {
        return Optional.ofNullable(cancelPaymentRequestMessage);
    }

    /**
     * Returns an optional instance of TemplateMessage.
     *
     * @return an optional instance of TemplateMessage, which may be null.
     */
    public Optional<TemplateMessage> templateMessage() {
        return Optional.ofNullable(templateMessage);
    }

    /**
     * Returns an optional instance of StickerMessage.
     *
     * @return an optional instance of StickerMessage, which may be null.
     */
    public Optional<StickerMessage> stickerMessage() {
        return Optional.ofNullable(stickerMessage);
    }

    /**
     * Returns an optional instance of GroupInviteMessage.
     *
     * @return an optional instance of GroupInviteMessage, which may be null.
     */
    public Optional<GroupInviteMessage> groupInviteMessage() {
        return Optional.ofNullable(groupInviteMessage);
    }

    /**
     * Returns an optional instance of TemplateReplyMessage.
     *
     * @return an optional instance of TemplateReplyMessage, which may be null.
     */
    public Optional<TemplateReplyMessage> templateReplyMessage() {
        return Optional.ofNullable(templateReplyMessage);
    }

    /**
     * Returns an optional instance of ProductMessage.
     *
     * @return an optional instance of ProductMessage, which may be null.
     */
    public Optional<ProductMessage> productMessage() {
        return Optional.ofNullable(productMessage);
    }

    /**
     * Returns an optional instance of DeviceSentMessage.
     *
     * @return an optional instance of DeviceSentMessage, which may be null.
     */
    public Optional<DeviceSentMessage> deviceSentMessage() {
        return Optional.ofNullable(deviceSentMessage);
    }

    /**
     * Returns an optional instance of DeviceSyncMessage.
     *
     * @return an optional instance of DeviceSyncMessage, which may be null.
     */
    public Optional<DeviceSyncMessage> deviceSyncMessage() {
        return Optional.ofNullable(deviceSyncMessage);
    }

    /**
     * Returns an optional instance of ListMessage.
     *
     * @return an optional instance of ListMessage, which may be null.
     */
    public Optional<ListMessage> listMessage() {
        return Optional.ofNullable(listMessage);
    }

    /**
     * Returns an optional instance of FutureMessageContainer with a view once message.
     *
     * @return an optional instance of FutureMessageContainer with a view once message, which may be null.
     */
    public Optional<FutureMessageContainer> viewOnceMessage() {
        return Optional.ofNullable(viewOnceMessage);
    }

    /**
     * Returns an {@link Optional} containing the {@link PaymentOrderMessage} associated with this message,
     * or an empty Optional if the message does not contain a payment order.
     *
     * @return an Optional containing the PaymentOrderMessage associated with this message, or an empty Optional
     */
    public Optional<PaymentOrderMessage> orderMessage() {
        return Optional.ofNullable(orderMessage);
    }

    /**
     * Returns an {@link Optional} containing the {@link ListResponseMessage} associated with this message,
     * or an empty Optional if the message does not contain a list response.
     *
     * @return an Optional containing the ListResponseMessage associated with this message, or an empty Optional
     */
    public Optional<ListResponseMessage> listResponseMessage() {
        return Optional.ofNullable(listResponseMessage);
    }

    /**
     * Returns an {@link Optional} containing the {@link FutureMessageContainer} associated with this message,
     * or an empty Optional if the message does not contain an ephemeral message.
     *
     * @return an Optional containing the ephemeral message associated with this message, or an empty Optional
     */
    public Optional<FutureMessageContainer> ephemeralMessage() {
        return Optional.ofNullable(ephemeralMessage);
    }

    /**
     * Returns an {@link Optional} containing the {@link PaymentInvoiceMessage} associated with this message,
     * or an empty Optional if the message does not contain a payment invoice.
     *
     * @return an Optional containing the PaymentInvoiceMessage associated with this message, or an empty Optional
     */
    public Optional<PaymentInvoiceMessage> invoiceMessage() {
        return Optional.ofNullable(invoiceMessage);
    }

    /**
     * Returns an {@link Optional} containing the {@link ButtonsMessage} associated with this message,
     * or an empty Optional if the message does not contain a buttons message.
     *
     * @return an Optional containing the ButtonsMessage associated with this message, or an empty Optional
     */
    public Optional<ButtonsMessage> buttonsMessage() {
        return Optional.ofNullable(buttonsMessage);
    }

    /**
     * Returns an {@link Optional} containing the {@link ButtonsResponseMessage} associated with this message,
     * or an empty Optional if the message does not contain a buttons response message.
     *
     * @return an Optional containing the ButtonsResponseMessage associated with this message, or an empty Optional
     */
    public Optional<ButtonsResponseMessage> buttonsResponseMessage() {
        return Optional.ofNullable(buttonsResponseMessage);
    }

    /**
     * Returns an {@link Optional} containing the {@link PaymentInviteMessage} associated with this message,
     * or an empty Optional if the message does not contain a payment invite message.
     *
     * @return an Optional containing the PaymentInviteMessage associated with this message, or an empty Optional
     */
    public Optional<PaymentInviteMessage> paymentInviteMessage() {
        return Optional.ofNullable(paymentInviteMessage);
    }

    /**
     * Returns an {@link Optional} containing the {@link InteractiveMessage} associated with this message,
     * or an empty Optional if the message does not contain an interactive message.
     *
     * @return an Optional containing the InteractiveMessage associated with this message, or an empty Optional
     */
    public Optional<InteractiveMessage> interactiveMessage() {
        return Optional.ofNullable(interactiveMessage);
    }

    /**
     * Returns an {@link Optional} containing the {@link ReactionMessage} associated with this message,
     * or an empty Optional if the message does not contain a reaction message.
     *
     * @return an Optional containing the ReactionMessage associated with this message, or an empty Optional
     */
    public Optional<ReactionMessage> reactionMessage() {
        return Optional.ofNullable(reactionMessage);
    }

    /**
     * Returns an Optional containing the StickerSyncRMRMessage, if present.
     *
     * @return an Optional containing the StickerSyncRMRMessage, or an empty Optional if it is null.
     */
    public Optional<StickerSyncRMRMessage> stickerSyncMessage() {
        return Optional.ofNullable(stickerSyncMessage);
    }

    /**
     * Returns an Optional containing the InteractiveResponseMessage, if present.
     *
     * @return an Optional containing the InteractiveResponseMessage, or an empty Optional if it is null.
     */
    public Optional<InteractiveResponseMessage> interactiveResponseMessage() {
        return Optional.ofNullable(interactiveResponseMessage);
    }

    /**
     * Returns an Optional containing the PollCreationMessage, if present.
     *
     * @return an Optional containing the PollCreationMessage, or an empty Optional if it is null.
     */
    public Optional<PollCreationMessage> pollCreationMessage() {
        return Optional.ofNullable(pollCreationMessage);
    }

    /**
     * Returns an Optional containing the PollUpdateMessage, if present.
     *
     * @return an Optional containing the PollUpdateMessage, or an empty Optional if it is null.
     */
    public Optional<PollUpdateMessage> pollUpdateMessage() {
        return Optional.ofNullable(pollUpdateMessage);
    }

    /**
     * Returns an Optional containing the KeepInChatMessage, if present.
     *
     * @return an Optional containing the KeepInChatMessage, or an empty Optional if it is null.
     */
    public Optional<KeepInChatMessage> keepInChatMessage() {
        return Optional.ofNullable(keepInChatMessage);
    }

    /**
     * Returns an Optional containing the FutureMessageContainer with document and caption, if present.
     *
     * @return an Optional containing the FutureMessageContainer with document and caption, or an empty Optional if it is null.
     */
    public Optional<FutureMessageContainer> documentWithCaptionMessage() {
        return Optional.ofNullable(documentWithCaptionMessage);
    }

    /**
     * Returns an Optional containing the RequestPhoneNumberMessage, if present.
     *
     * @return an Optional containing the RequestPhoneNumberMessage, or an empty Optional if it is null.
     */
    public Optional<RequestPhoneNumberMessage> requestPhoneNumberMessage() {
        return Optional.ofNullable(requestPhoneNumberMessage);
    }

    /**
     * Returns an Optional containing the FutureMessageContainer with view-once content, if present.
     *
     * @return an Optional containing the FutureMessageContainer with view-once content, or an empty Optional if it is null.
     */
    public Optional<FutureMessageContainer> viewOnceV2Message() {
        return Optional.ofNullable(viewOnceV2Message);
    }

    /**
     * Returns an Optional containing the EncryptedReactionMessage, if present.
     *
     * @return an Optional containing the EncryptedReactionMessage, or an empty Optional if it is null.
     */
    public Optional<EncryptedReactionMessage> encryptedReactionMessage() {
        return Optional.ofNullable(encryptedReactionMessage);
    }

    /**
     * Returns an Optional containing the FutureMessageContainer with edited message content, if present.
     *
     * @return an Optional containing the FutureMessageContainer with edited message content, or an empty Optional if it is null.
     */
    public Optional<FutureMessageContainer> editedMessage() {
        return Optional.ofNullable(editedMessage);
    }

    /**
     * Returns an Optional containing the FutureMessageContainer with view-once extension content, if present.
     *
     * @return an Optional containing the FutureMessageContainer with view-once extension content, or an empty Optional if it is null.
     */
    public Optional<FutureMessageContainer> viewOnceV2ExtensionMessage() {
        return Optional.ofNullable(viewOnceV2ExtensionMessage);
    }
}
package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredMessage;
import it.auties.whatsapp.model.info.DeviceContextInfo;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.payment.*;
import it.auties.whatsapp.model.message.server.*;
import it.auties.whatsapp.model.message.standard.*;

import java.util.Objects;
import java.util.Optional;

/**
 * A container for all types of messages known currently to WhatsappWeb.
 * <p>
 * Only one of these properties should be populated, however it's not certain as Whatsapp's Protobuf
 * doesn't use a one of instruction as it would be logical to in said case. This may imply that in
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
@ProtobufMessageName("Message")
public record MessageContainer(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Optional<String> textWithNoContextMessage,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<SenderKeyDistributionMessage> senderKeyDistributionMessage,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Optional<ImageMessage> imageMessage,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<ContactMessage> contactMessage,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        Optional<LocationMessage> locationMessage,
        @ProtobufProperty(index = 6, type = ProtobufType.OBJECT)
        Optional<TextMessage> textMessage,
        @ProtobufProperty(index = 7, type = ProtobufType.OBJECT)
        Optional<DocumentMessage> documentMessage,
        @ProtobufProperty(index = 8, type = ProtobufType.OBJECT)
        Optional<AudioMessage> audioMessage,
        @ProtobufProperty(index = 9, type = ProtobufType.OBJECT)
        Optional<VideoOrGifMessage> videoMessage,
        @ProtobufProperty(index = 10, type = ProtobufType.OBJECT)
        Optional<CallMessage> callMessage,
        @ProtobufProperty(index = 12, type = ProtobufType.OBJECT)
        Optional<ProtocolMessage> protocolMessage,
        @ProtobufProperty(index = 13, type = ProtobufType.OBJECT)
        Optional<ContactsMessage> contactsArrayMessage,
        @ProtobufProperty(index = 14, type = ProtobufType.OBJECT)
        Optional<HighlyStructuredMessage> highlyStructuredMessage,
        @ProtobufProperty(index = 16, type = ProtobufType.OBJECT)
        Optional<SendPaymentMessage> sendPaymentMessage,
        @ProtobufProperty(index = 18, type = ProtobufType.OBJECT)
        Optional<LiveLocationMessage> liveLocationMessage,
        @ProtobufProperty(index = 22, type = ProtobufType.OBJECT)
        Optional<RequestPaymentMessage> requestPaymentMessage,
        @ProtobufProperty(index = 23, type = ProtobufType.OBJECT)
        Optional<DeclinePaymentRequestMessage> declinePaymentRequestMessage,
        @ProtobufProperty(index = 24, type = ProtobufType.OBJECT)
        Optional<CancelPaymentRequestMessage> cancelPaymentRequestMessage,
        @ProtobufProperty(index = 25, type = ProtobufType.OBJECT)
        Optional<TemplateMessage> templateMessage,
        @ProtobufProperty(index = 26, type = ProtobufType.OBJECT)
        Optional<StickerMessage> stickerMessage,
        @ProtobufProperty(index = 28, type = ProtobufType.OBJECT)
        Optional<GroupInviteMessage> groupInviteMessage,
        @ProtobufProperty(index = 29, type = ProtobufType.OBJECT)
        Optional<TemplateReplyMessage> templateReplyMessage,
        @ProtobufProperty(index = 30, type = ProtobufType.OBJECT)
        Optional<ProductMessage> productMessage,
        @ProtobufProperty(index = 31, type = ProtobufType.OBJECT)
        Optional<DeviceSentMessage> deviceSentMessage,
        @ProtobufProperty(index = 32, type = ProtobufType.OBJECT)
        Optional<DeviceSyncMessage> deviceSyncMessage,
        @ProtobufProperty(index = 36, type = ProtobufType.OBJECT)
        Optional<ListMessage> listMessage,
        @ProtobufProperty(index = 37, type = ProtobufType.OBJECT)
        Optional<FutureMessageContainer> viewOnceMessage,
        @ProtobufProperty(index = 38, type = ProtobufType.OBJECT)
        Optional<PaymentOrderMessage> orderMessage,
        @ProtobufProperty(index = 39, type = ProtobufType.OBJECT)
        Optional<ListResponseMessage> listResponseMessage,
        @ProtobufProperty(index = 40, type = ProtobufType.OBJECT)
        Optional<FutureMessageContainer> ephemeralMessage,
        @ProtobufProperty(index = 41, type = ProtobufType.OBJECT)
        Optional<PaymentInvoiceMessage> invoiceMessage,
        @ProtobufProperty(index = 42, type = ProtobufType.OBJECT)
        Optional<ButtonsMessage> buttonsMessage,
        @ProtobufProperty(index = 43, type = ProtobufType.OBJECT)
        Optional<ButtonsResponseMessage> buttonsResponseMessage,
        @ProtobufProperty(index = 44, type = ProtobufType.OBJECT)
        Optional<PaymentInviteMessage> paymentInviteMessage,
        @ProtobufProperty(index = 45, type = ProtobufType.OBJECT)
        Optional<InteractiveMessage> interactiveMessage,
        @ProtobufProperty(index = 46, type = ProtobufType.OBJECT)
        Optional<ReactionMessage> reactionMessage,
        @ProtobufProperty(index = 47, type = ProtobufType.OBJECT)
        Optional<StickerSyncRMRMessage> stickerSyncMessage,
        @ProtobufProperty(index = 48, type = ProtobufType.OBJECT)
        Optional<InteractiveResponseMessage> interactiveResponseMessage,
        @ProtobufProperty(index = 49, type = ProtobufType.OBJECT)
        Optional<PollCreationMessage> pollCreationMessage,
        @ProtobufProperty(index = 50, type = ProtobufType.OBJECT)
        Optional<PollUpdateMessage> pollUpdateMessage,
        @ProtobufProperty(index = 51, type = ProtobufType.OBJECT)
        Optional<KeepInChatMessage> keepInChatMessage,
        @ProtobufProperty(index = 53, type = ProtobufType.OBJECT)
        Optional<FutureMessageContainer> documentWithCaptionMessage,
        @ProtobufProperty(index = 54, type = ProtobufType.OBJECT)
        Optional<RequestPhoneNumberMessage> requestPhoneNumberMessage,
        @ProtobufProperty(index = 55, type = ProtobufType.OBJECT)
        Optional<FutureMessageContainer> viewOnceV2Message,
        @ProtobufProperty(index = 56, type = ProtobufType.OBJECT)
        Optional<EncryptedReactionMessage> encryptedReactionMessage,
        @ProtobufProperty(index = 58, type = ProtobufType.OBJECT)
        Optional<FutureMessageContainer> editedMessage,
        @ProtobufProperty(index = 59, type = ProtobufType.OBJECT)
        Optional<FutureMessageContainer> viewOnceV2ExtensionMessage,
        @ProtobufProperty(index = 78, type = ProtobufType.OBJECT)
        Optional<NewsletterAdminInviteMessage> newsletterAdminInviteMessage,
        @ProtobufProperty(index = 35, type = ProtobufType.OBJECT)
        Optional<DeviceContextInfo> deviceInfo
) implements ProtobufMessage {
    /**
     * An empty message
     */
    private static final EmptyMessage EMPTY_MESSAGE = new EmptyMessage();

    /**
     * Returns an empty message container
     *
     * @return a non-null container
     */
    public static MessageContainer empty() {
        return new MessageContainerBuilder().build();
    }

    /**
     * Constructs a new MessageContainer from a message of any type
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     * @return a non-null container
     */
    public static <T extends Message> MessageContainer of(T message) {
        return ofBuilder(message).build();
    }

    /**
     * Constructs a new MessageContainerBuilder from a message of any type
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     * @return a non-null builder
     */
    public static <T extends Message> MessageContainerBuilder ofBuilder(T message) {
        var builder = new MessageContainerBuilder();
        switch (message) {
            case SenderKeyDistributionMessage senderKeyDistribution ->
                    builder.senderKeyDistributionMessage(Optional.of(senderKeyDistribution));
            case ImageMessage image -> builder.imageMessage(Optional.of(image));
            case ContactMessage contact -> builder.contactMessage(Optional.of(contact));
            case LocationMessage location -> builder.locationMessage(Optional.of(location));
            case TextMessage text -> builder.textMessage(Optional.of(text));
            case DocumentMessage document -> builder.documentMessage(Optional.of(document));
            case AudioMessage audio -> builder.audioMessage(Optional.of(audio));
            case VideoOrGifMessage video -> builder.videoMessage(Optional.of(video));
            case ProtocolMessage protocol -> builder.protocolMessage(Optional.of(protocol));
            case ContactsMessage contactsArray -> builder.contactsArrayMessage(Optional.of(contactsArray));
            case HighlyStructuredMessage highlyStructured ->
                    builder.highlyStructuredMessage(Optional.of(highlyStructured));
            case SendPaymentMessage sendPayment -> builder.sendPaymentMessage(Optional.of(sendPayment));
            case LiveLocationMessage liveLocation -> builder.liveLocationMessage(Optional.of(liveLocation));
            case RequestPaymentMessage requestPayment -> builder.requestPaymentMessage(Optional.of(requestPayment));
            case DeclinePaymentRequestMessage declinePaymentRequest ->
                    builder.declinePaymentRequestMessage(Optional.of(declinePaymentRequest));
            case CancelPaymentRequestMessage cancelPaymentRequest ->
                    builder.cancelPaymentRequestMessage(Optional.of(cancelPaymentRequest));
            case TemplateMessage template -> builder.templateMessage(Optional.of(template));
            case StickerMessage sticker -> builder.stickerMessage(Optional.of(sticker));
            case GroupInviteMessage groupInvite -> builder.groupInviteMessage(Optional.of(groupInvite));
            case TemplateReplyMessage templateButtonReply ->
                    builder.templateReplyMessage(Optional.of(templateButtonReply));
            case ProductMessage product -> builder.productMessage(Optional.of(product));
            case DeviceSyncMessage deviceSync -> builder.deviceSyncMessage(Optional.of(deviceSync));
            case ListMessage buttonsList -> builder.listMessage(Optional.of(buttonsList));
            case PaymentOrderMessage order -> builder.orderMessage(Optional.of(order));
            case ListResponseMessage listResponse -> builder.listResponseMessage(Optional.of(listResponse));
            case PaymentInvoiceMessage invoice -> builder.invoiceMessage(Optional.of(invoice));
            case ButtonsMessage buttons -> builder.buttonsMessage(Optional.of(buttons));
            case ButtonsResponseMessage buttonsResponse -> builder.buttonsResponseMessage(Optional.of(buttonsResponse));
            case PaymentInviteMessage paymentInvite -> builder.paymentInviteMessage(Optional.of(paymentInvite));
            case InteractiveMessage interactive -> builder.interactiveMessage(Optional.of(interactive));
            case ReactionMessage reaction -> builder.reactionMessage(Optional.of(reaction));
            case StickerSyncRMRMessage stickerSync -> builder.stickerSyncMessage(Optional.of(stickerSync));
            case DeviceSentMessage deviceSent -> builder.deviceSentMessage(Optional.of(deviceSent));
            case InteractiveResponseMessage interactiveResponseMessage ->
                    builder.interactiveResponseMessage(Optional.of(interactiveResponseMessage));
            case PollCreationMessage pollCreationMessage ->
                    builder.pollCreationMessage(Optional.of(pollCreationMessage));
            case PollUpdateMessage pollUpdateMessage -> builder.pollUpdateMessage(Optional.of(pollUpdateMessage));
            case KeepInChatMessage keepInChatMessage -> builder.keepInChatMessage(Optional.of(keepInChatMessage));
            case RequestPhoneNumberMessage requestPhoneNumberMessage ->
                    builder.requestPhoneNumberMessage(Optional.of(requestPhoneNumberMessage));
            case EncryptedReactionMessage encReactionMessage ->
                    builder.encryptedReactionMessage(Optional.of(encReactionMessage));
            case CallMessage callMessage -> builder.callMessage(Optional.of(callMessage));
            case NewsletterAdminInviteMessage newsletterAdminInviteMessage -> builder.newsletterAdminInviteMessage(newsletterAdminInviteMessage);
            default -> {}
        }
        return builder;
    }

    /**
     * Constructs a new MessageContainer from a text message
     *
     * @param message the text message with no context
     */
    public static MessageContainer of(String message) {
        return new MessageContainerBuilder()
                .textMessage(TextMessage.of(message))
                .build();
    }

    /**
     * Constructs a new MessageContainer from a message of any type that can only be seen once
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofViewOnce(T message) {
        return new MessageContainerBuilder()
                .viewOnceMessage(FutureMessageContainer.of(message))
                .build();
    }

    /**
     * Constructs a new MessageContainer from a message of any type that can only be seen once(version
     * v2)
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofViewOnceV2(T message) {
        return new MessageContainerBuilder()
                .viewOnceV2Message(FutureMessageContainer.of(message))
                .build();
    }

    /**
     * Constructs a new MessageContainer from a message of any type marking it as ephemeral
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofEphemeral(T message) {
        return new MessageContainerBuilder()
                .ephemeralMessage(FutureMessageContainer.of(message))
                .build();
    }

    /**
     * Constructs a new MessageContainer from an edited message
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofEditedMessage(T message) {
        return new MessageContainerBuilder()
                .editedMessage(FutureMessageContainer.of(message))
                .build();
    }

    /**
     * Constructs a new MessageContainer from a document with caption message
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofDocumentWithCaption(T message) {
        return new MessageContainerBuilder()
                .documentWithCaptionMessage(FutureMessageContainer.of(message))
                .build();
    }

    /**
     * Returns the first populated message inside this container. If no message is found,
     * {@link EmptyMessage} is returned
     *
     * @return a non-null message
     */
    public Message content() {
        if (this.textWithNoContextMessage.isPresent()) {
            return TextMessage.of(textWithNoContextMessage.get());
        }
        if (this.senderKeyDistributionMessage.isPresent()) {
            return senderKeyDistributionMessage.get();
        }
        if (this.imageMessage.isPresent()) {
            return imageMessage.get();
        }
        if (this.contactMessage.isPresent()) {
            return contactMessage.get();
        }
        if (this.locationMessage.isPresent()) {
            return locationMessage.get();
        }
        if (this.textMessage.isPresent()) {
            return textMessage.get();
        }
        if (this.documentMessage.isPresent()) {
            return documentMessage.get();
        }
        if (this.audioMessage.isPresent()) {
            return audioMessage.get();
        }
        if (this.videoMessage.isPresent()) {
            return videoMessage.get();
        }
        if (this.protocolMessage.isPresent()) {
            return protocolMessage.get();
        }
        if (this.contactsArrayMessage.isPresent()) {
            return contactsArrayMessage.get();
        }
        if (this.highlyStructuredMessage.isPresent()) {
            return highlyStructuredMessage.get();
        }
        if (this.sendPaymentMessage.isPresent()) {
            return sendPaymentMessage.get();
        }
        if (this.liveLocationMessage.isPresent()) {
            return liveLocationMessage.get();
        }
        if (this.requestPaymentMessage.isPresent()) {
            return requestPaymentMessage.get();
        }
        if (this.declinePaymentRequestMessage.isPresent()) {
            return declinePaymentRequestMessage.get();
        }
        if (this.cancelPaymentRequestMessage.isPresent()) {
            return cancelPaymentRequestMessage.get();
        }
        if (this.templateMessage.isPresent()) {
            return templateMessage.get();
        }
        if (this.stickerMessage.isPresent()) {
            return stickerMessage.get();
        }
        if (this.groupInviteMessage.isPresent()) {
            return groupInviteMessage.get();
        }
        if (this.templateReplyMessage.isPresent()) {
            return templateReplyMessage.get();
        }
        if (this.productMessage.isPresent()) {
            return productMessage.get();
        }
        if (this.deviceSentMessage.isPresent()) {
            return deviceSentMessage.get().message().content();
        }
        if (this.deviceSyncMessage.isPresent()) {
            return deviceSyncMessage.get();
        }
        if (this.listMessage.isPresent()) {
            return listMessage.get();
        }
        if (this.viewOnceMessage.isPresent()) {
            return viewOnceMessage.get().unbox();
        }
        if (this.orderMessage.isPresent()) {
            return orderMessage.get();
        }
        if (this.listResponseMessage.isPresent()) {
            return listResponseMessage.get();
        }
        if (this.ephemeralMessage.isPresent()) {
            return ephemeralMessage.get().unbox();
        }
        if (this.invoiceMessage.isPresent()) {
            return invoiceMessage.get();
        }
        if (this.buttonsMessage.isPresent()) {
            return buttonsMessage.get();
        }
        if (this.buttonsResponseMessage.isPresent()) {
            return buttonsResponseMessage.get();
        }
        if (this.paymentInviteMessage.isPresent()) {
            return paymentInviteMessage.get();
        }
        if (interactiveMessage.isPresent()) {
            return interactiveMessage.get();
        }
        if (reactionMessage.isPresent()) {
            return reactionMessage.get();
        }
        if (stickerSyncMessage.isPresent()) {
            return stickerSyncMessage.get();
        }
        if (interactiveResponseMessage.isPresent()) {
            return interactiveResponseMessage.get();
        }
        if (pollCreationMessage.isPresent()) {
            return pollCreationMessage.get();
        }
        if (pollUpdateMessage.isPresent()) {
            return pollUpdateMessage.get();
        }
        if (keepInChatMessage.isPresent()) {
            return keepInChatMessage.get();
        }
        if (documentWithCaptionMessage.isPresent()) {
            return documentWithCaptionMessage.get().unbox();
        }
        if (requestPhoneNumberMessage.isPresent()) {
            return requestPhoneNumberMessage.get();
        }
        if (viewOnceV2Message.isPresent()) {
            return viewOnceV2Message.get().unbox();
        }
        if (encryptedReactionMessage.isPresent()) {
            return encryptedReactionMessage.get();
        }
        if (editedMessage.isPresent()) {
            return editedMessage.get().unbox();
        }
        if (viewOnceV2ExtensionMessage.isPresent()) {
            return viewOnceV2ExtensionMessage.get().unbox();
        }
        if (callMessage.isPresent()) {
            return callMessage.get();
        }
        if(newsletterAdminInviteMessage.isPresent()) {
            return newsletterAdminInviteMessage.get();
        }
        return EMPTY_MESSAGE;
    }

    /**
     * Returns the first populated contextual message inside this container
     *
     * @return a non-null Optional ContextualMessage
     */
    public Optional<ContextualMessage<?>> contentWithContext() {
        return Optional.of(content())
                .filter(entry -> entry instanceof ContextualMessage)
                .map(entry -> (ContextualMessage<?>) entry);
    }

    /**
     * Checks whether the message that this container wraps matches the provided type
     *
     * @param type the non-null type to check against
     * @return a boolean
     */
    public boolean hasType(MessageType type) {
        return content().type() == type;
    }

    /**
     * Checks whether the message that this container wraps matches the provided category
     *
     * @param category the non-null category to check against
     * @return a boolean
     */
    public boolean hasCategory(MessageCategory category) {
        return content().category() == category;
    }

    /**
     * Returns the type of the message
     *
     * @return a non-null type
     */
    public MessageType type() {
        if(textWithNoContextMessage.isPresent()) {
            return MessageType.TEXT;
        }

        if (ephemeralMessage.isPresent()) {
            return MessageType.EPHEMERAL;
        }

        if (viewOnceMessage.isPresent() || viewOnceV2Message.isPresent() || viewOnceV2ExtensionMessage.isPresent()) {
            return MessageType.VIEW_ONCE;
        }

        if (editedMessage.isPresent()) {
            return MessageType.EDITED;
        }

        return content().type();
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
     * Converts this message to an ephemeral message
     *
     * @return a non-null message container
     */
    public MessageContainer toEphemeral() {
        if (type() == MessageType.EPHEMERAL) {
            return this;
        }

        return new MessageContainerBuilder()
                .ephemeralMessage(FutureMessageContainer.of(content()))
                .deviceInfo(deviceInfo)
                .build();
    }

    /**
     * Converts this message to a view once message
     *
     * @return a non-null message container
     */
    public MessageContainer toViewOnce() {
        if (type() == MessageType.VIEW_ONCE) {
            return this;
        }

        return new MessageContainerBuilder()
                .viewOnceMessage(FutureMessageContainer.of(content()))
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
        if (deviceSentMessage.isPresent()) {
            return deviceSentMessage.get().message();
        }

        if (viewOnceMessage.isPresent()) {
            return viewOnceMessage.get().content();
        }

        if (ephemeralMessage.isPresent()) {
            return ephemeralMessage.get().content();
        }

        if (documentWithCaptionMessage.isPresent()) {
            return documentWithCaptionMessage.get().content();
        }

        if (viewOnceV2Message.isPresent()) {
            return viewOnceV2Message.get().content();
        }

        if (editedMessage.isPresent()) {
            return editedMessage.get().content();
        }

        if (viewOnceV2ExtensionMessage.isPresent()) {
            return viewOnceV2ExtensionMessage.get().content();
        }

        return this;
    }

    /**
     * Returns a copy of this container with a different device info
     *
     * @return a non-null message container
     */
    public MessageContainer withDeviceInfo(DeviceContextInfo deviceInfo) {
        if (deviceSentMessage.isPresent()) {
            return ofBuilder(deviceSentMessage.get())
                    .deviceInfo(deviceInfo)
                    .build();
        }

        if (viewOnceMessage.isPresent()) {
            return new MessageContainerBuilder()
                    .viewOnceMessage(viewOnceMessage.get())
                    .deviceInfo(deviceInfo)
                    .build();
        }

        if (ephemeralMessage.isPresent()) {
            return new MessageContainerBuilder()
                    .ephemeralMessage(ephemeralMessage.get())
                    .deviceInfo(deviceInfo)
                    .build();
        }

        if (documentWithCaptionMessage.isPresent()) {
            return new MessageContainerBuilder()
                    .documentWithCaptionMessage(documentWithCaptionMessage.get())
                    .deviceInfo(deviceInfo)
                    .build();
        }

        if (viewOnceV2Message.isPresent()) {
            return new MessageContainerBuilder()
                    .viewOnceV2Message(viewOnceV2Message.get())
                    .deviceInfo(deviceInfo)
                    .build();
        }

        if (editedMessage.isPresent()) {
            return new MessageContainerBuilder()
                    .editedMessage(editedMessage.get())
                    .deviceInfo(deviceInfo)
                    .build();
        }

        if (viewOnceV2ExtensionMessage.isPresent()) {
            return new MessageContainerBuilder()
                    .viewOnceV2ExtensionMessage(viewOnceV2ExtensionMessage.get())
                    .deviceInfo(deviceInfo)
                    .build();
        }

        return ofBuilder(content())
                .deviceInfo(deviceInfo)
                .build();
    }

    /**
     * Returns whether this container is empty
     *
     * @return a boolean
     */
    public boolean isEmpty() {
        return hasType(MessageType.EMPTY);
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
}
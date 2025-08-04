package it.auties.whatsapp.model.message.model;

import com.alibaba.fastjson2.JSONObject;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
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
 * Only one of these properties is populated usually, but it is possible to have multiple after a message retry for example
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
@ProtobufMessage(name = "Message")
public final class MessageContainer {
    private static final EmptyMessage EMPTY_MESSAGE = new EmptyMessage();

    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String textWithNoContextMessage;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final SenderKeyDistributionMessage senderKeyDistributionMessage;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final ImageMessage imageMessage;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final ContactMessage contactMessage;

    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final LocationMessage locationMessage;

    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final TextMessage textMessage;

    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final DocumentMessage documentMessage;

    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final AudioMessage audioMessage;

    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    final VideoOrGifMessage videoMessage;

    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    final CallMessage callMessage;

    @ProtobufProperty(index = 12, type = ProtobufType.MESSAGE)
    final ProtocolMessage protocolMessage;

    @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
    final ContactsMessage contactsArrayMessage;

    @ProtobufProperty(index = 14, type = ProtobufType.MESSAGE)
    final HighlyStructuredMessage highlyStructuredMessage;

    @ProtobufProperty(index = 16, type = ProtobufType.MESSAGE)
    final SendPaymentMessage sendPaymentMessage;

    @ProtobufProperty(index = 18, type = ProtobufType.MESSAGE)
    final LiveLocationMessage liveLocationMessage;

    @ProtobufProperty(index = 22, type = ProtobufType.MESSAGE)
    final RequestPaymentMessage requestPaymentMessage;

    @ProtobufProperty(index = 23, type = ProtobufType.MESSAGE)
    final DeclinePaymentRequestMessage declinePaymentRequestMessage;

    @ProtobufProperty(index = 24, type = ProtobufType.MESSAGE)
    final CancelPaymentRequestMessage cancelPaymentRequestMessage;

    @ProtobufProperty(index = 25, type = ProtobufType.MESSAGE)
    final TemplateMessage templateMessage;

    @ProtobufProperty(index = 26, type = ProtobufType.MESSAGE)
    final StickerMessage stickerMessage;

    @ProtobufProperty(index = 28, type = ProtobufType.MESSAGE)
    final GroupInviteMessage groupInviteMessage;

    @ProtobufProperty(index = 29, type = ProtobufType.MESSAGE)
    final TemplateReplyMessage templateReplyMessage;

    @ProtobufProperty(index = 30, type = ProtobufType.MESSAGE)
    final ProductMessage productMessage;

    @ProtobufProperty(index = 31, type = ProtobufType.MESSAGE)
    final DeviceSentMessage deviceSentMessage;

    @ProtobufProperty(index = 32, type = ProtobufType.MESSAGE)
    final DeviceSyncMessage deviceSyncMessage;

    @ProtobufProperty(index = 36, type = ProtobufType.MESSAGE)
    final ListMessage listMessage;

    @ProtobufProperty(index = 37, type = ProtobufType.MESSAGE)
    final FutureMessageContainer viewOnceMessage;

    @ProtobufProperty(index = 38, type = ProtobufType.MESSAGE)
    final PaymentOrderMessage orderMessage;

    @ProtobufProperty(index = 39, type = ProtobufType.MESSAGE)
    final ListResponseMessage listResponseMessage;

    @ProtobufProperty(index = 40, type = ProtobufType.MESSAGE)
    final FutureMessageContainer ephemeralMessage;

    @ProtobufProperty(index = 41, type = ProtobufType.MESSAGE)
    final PaymentInvoiceMessage invoiceMessage;

    @ProtobufProperty(index = 42, type = ProtobufType.MESSAGE)
    final ButtonsMessage buttonsMessage;

    @ProtobufProperty(index = 43, type = ProtobufType.MESSAGE)
    final ButtonsResponseMessage buttonsResponseMessage;

    @ProtobufProperty(index = 44, type = ProtobufType.MESSAGE)
    final PaymentInviteMessage paymentInviteMessage;

    @ProtobufProperty(index = 45, type = ProtobufType.MESSAGE)
    final InteractiveMessage interactiveMessage;

    @ProtobufProperty(index = 46, type = ProtobufType.MESSAGE)
    final ReactionMessage reactionMessage;

    @ProtobufProperty(index = 47, type = ProtobufType.MESSAGE)
    final StickerSyncRMRMessage stickerSyncMessage;

    @ProtobufProperty(index = 48, type = ProtobufType.MESSAGE)
    final InteractiveResponseMessage interactiveResponseMessage;

    @ProtobufProperty(index = 49, type = ProtobufType.MESSAGE)
    final PollCreationMessage pollCreationMessage;

    @ProtobufProperty(index = 50, type = ProtobufType.MESSAGE)
    final PollUpdateMessage pollUpdateMessage;

    @ProtobufProperty(index = 51, type = ProtobufType.MESSAGE)
    final KeepInChatMessage keepInChatMessage;

    @ProtobufProperty(index = 53, type = ProtobufType.MESSAGE)
    final FutureMessageContainer documentWithCaptionMessage;

    @ProtobufProperty(index = 54, type = ProtobufType.MESSAGE)
    final RequestPhoneNumberMessage requestPhoneNumberMessage;

    @ProtobufProperty(index = 55, type = ProtobufType.MESSAGE)
    final FutureMessageContainer viewOnceV2Message;

    @ProtobufProperty(index = 56, type = ProtobufType.MESSAGE)
    final EncryptedReactionMessage encryptedReactionMessage;

    @ProtobufProperty(index = 58, type = ProtobufType.MESSAGE)
    final FutureMessageContainer editedMessage;

    @ProtobufProperty(index = 59, type = ProtobufType.MESSAGE)
    final FutureMessageContainer viewOnceV2ExtensionMessage;

    @ProtobufProperty(index = 78, type = ProtobufType.MESSAGE)
    final NewsletterAdminInviteMessage newsletterAdminInviteMessage;

    @ProtobufProperty(index = 35, type = ProtobufType.MESSAGE)
    final DeviceContextInfo deviceInfo;

    MessageContainer(String textWithNoContextMessage, SenderKeyDistributionMessage senderKeyDistributionMessage, ImageMessage imageMessage, ContactMessage contactMessage, LocationMessage locationMessage, TextMessage textMessage, DocumentMessage documentMessage, AudioMessage audioMessage, VideoOrGifMessage videoMessage, CallMessage callMessage, ProtocolMessage protocolMessage, ContactsMessage contactsArrayMessage, HighlyStructuredMessage highlyStructuredMessage, SendPaymentMessage sendPaymentMessage, LiveLocationMessage liveLocationMessage, RequestPaymentMessage requestPaymentMessage, DeclinePaymentRequestMessage declinePaymentRequestMessage, CancelPaymentRequestMessage cancelPaymentRequestMessage, TemplateMessage templateMessage, StickerMessage stickerMessage, GroupInviteMessage groupInviteMessage, TemplateReplyMessage templateReplyMessage, ProductMessage productMessage, DeviceSentMessage deviceSentMessage, DeviceSyncMessage deviceSyncMessage, ListMessage listMessage, FutureMessageContainer viewOnceMessage, PaymentOrderMessage orderMessage, ListResponseMessage listResponseMessage, FutureMessageContainer ephemeralMessage, PaymentInvoiceMessage invoiceMessage, ButtonsMessage buttonsMessage, ButtonsResponseMessage buttonsResponseMessage, PaymentInviteMessage paymentInviteMessage, InteractiveMessage interactiveMessage, ReactionMessage reactionMessage, StickerSyncRMRMessage stickerSyncMessage, InteractiveResponseMessage interactiveResponseMessage, PollCreationMessage pollCreationMessage, PollUpdateMessage pollUpdateMessage, KeepInChatMessage keepInChatMessage, FutureMessageContainer documentWithCaptionMessage, RequestPhoneNumberMessage requestPhoneNumberMessage, FutureMessageContainer viewOnceV2Message, EncryptedReactionMessage encryptedReactionMessage, FutureMessageContainer editedMessage, FutureMessageContainer viewOnceV2ExtensionMessage, NewsletterAdminInviteMessage newsletterAdminInviteMessage, DeviceContextInfo deviceInfo) {
        this.textWithNoContextMessage = textWithNoContextMessage;
        this.senderKeyDistributionMessage = senderKeyDistributionMessage;
        this.imageMessage = imageMessage;
        this.contactMessage = contactMessage;
        this.locationMessage = locationMessage;
        this.textMessage = textMessage;
        this.documentMessage = documentMessage;
        this.audioMessage = audioMessage;
        this.videoMessage = videoMessage;
        this.callMessage = callMessage;
        this.protocolMessage = protocolMessage;
        this.contactsArrayMessage = contactsArrayMessage;
        this.highlyStructuredMessage = highlyStructuredMessage;
        this.sendPaymentMessage = sendPaymentMessage;
        this.liveLocationMessage = liveLocationMessage;
        this.requestPaymentMessage = requestPaymentMessage;
        this.declinePaymentRequestMessage = declinePaymentRequestMessage;
        this.cancelPaymentRequestMessage = cancelPaymentRequestMessage;
        this.templateMessage = templateMessage;
        this.stickerMessage = stickerMessage;
        this.groupInviteMessage = groupInviteMessage;
        this.templateReplyMessage = templateReplyMessage;
        this.productMessage = productMessage;
        this.deviceSentMessage = deviceSentMessage;
        this.deviceSyncMessage = deviceSyncMessage;
        this.listMessage = listMessage;
        this.viewOnceMessage = viewOnceMessage;
        this.orderMessage = orderMessage;
        this.listResponseMessage = listResponseMessage;
        this.ephemeralMessage = ephemeralMessage;
        this.invoiceMessage = invoiceMessage;
        this.buttonsMessage = buttonsMessage;
        this.buttonsResponseMessage = buttonsResponseMessage;
        this.paymentInviteMessage = paymentInviteMessage;
        this.interactiveMessage = interactiveMessage;
        this.reactionMessage = reactionMessage;
        this.stickerSyncMessage = stickerSyncMessage;
        this.interactiveResponseMessage = interactiveResponseMessage;
        this.pollCreationMessage = pollCreationMessage;
        this.pollUpdateMessage = pollUpdateMessage;
        this.keepInChatMessage = keepInChatMessage;
        this.documentWithCaptionMessage = documentWithCaptionMessage;
        this.requestPhoneNumberMessage = requestPhoneNumberMessage;
        this.viewOnceV2Message = viewOnceV2Message;
        this.encryptedReactionMessage = encryptedReactionMessage;
        this.editedMessage = editedMessage;
        this.viewOnceV2ExtensionMessage = viewOnceV2ExtensionMessage;
        this.newsletterAdminInviteMessage = newsletterAdminInviteMessage;
        this.deviceInfo = deviceInfo;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MessageContainer that
                && Objects.equals(textWithNoContextMessage, that.textWithNoContextMessage)
                && Objects.equals(senderKeyDistributionMessage, that.senderKeyDistributionMessage)
                && Objects.equals(imageMessage, that.imageMessage)
                && Objects.equals(contactMessage, that.contactMessage)
                && Objects.equals(locationMessage, that.locationMessage)
                && Objects.equals(textMessage, that.textMessage)
                && Objects.equals(documentMessage, that.documentMessage)
                && Objects.equals(audioMessage, that.audioMessage)
                && Objects.equals(videoMessage, that.videoMessage)
                && Objects.equals(callMessage, that.callMessage)
                && Objects.equals(protocolMessage, that.protocolMessage)
                && Objects.equals(contactsArrayMessage, that.contactsArrayMessage)
                && Objects.equals(highlyStructuredMessage, that.highlyStructuredMessage)
                && Objects.equals(sendPaymentMessage, that.sendPaymentMessage)
                && Objects.equals(liveLocationMessage, that.liveLocationMessage)
                && Objects.equals(requestPaymentMessage, that.requestPaymentMessage)
                && Objects.equals(declinePaymentRequestMessage, that.declinePaymentRequestMessage)
                && Objects.equals(cancelPaymentRequestMessage, that.cancelPaymentRequestMessage)
                && Objects.equals(templateMessage, that.templateMessage)
                && Objects.equals(stickerMessage, that.stickerMessage)
                && Objects.equals(groupInviteMessage, that.groupInviteMessage)
                && Objects.equals(templateReplyMessage, that.templateReplyMessage)
                && Objects.equals(productMessage, that.productMessage)
                && Objects.equals(deviceSentMessage, that.deviceSentMessage)
                && Objects.equals(deviceSyncMessage, that.deviceSyncMessage)
                && Objects.equals(listMessage, that.listMessage)
                && Objects.equals(viewOnceMessage, that.viewOnceMessage)
                && Objects.equals(orderMessage, that.orderMessage)
                && Objects.equals(listResponseMessage, that.listResponseMessage)
                && Objects.equals(ephemeralMessage, that.ephemeralMessage)
                && Objects.equals(invoiceMessage, that.invoiceMessage)
                && Objects.equals(buttonsMessage, that.buttonsMessage)
                && Objects.equals(buttonsResponseMessage, that.buttonsResponseMessage)
                && Objects.equals(paymentInviteMessage, that.paymentInviteMessage)
                && Objects.equals(interactiveMessage, that.interactiveMessage)
                && Objects.equals(reactionMessage, that.reactionMessage)
                && Objects.equals(stickerSyncMessage, that.stickerSyncMessage)
                && Objects.equals(interactiveResponseMessage, that.interactiveResponseMessage)
                && Objects.equals(pollCreationMessage, that.pollCreationMessage)
                && Objects.equals(pollUpdateMessage, that.pollUpdateMessage)
                && Objects.equals(keepInChatMessage, that.keepInChatMessage)
                && Objects.equals(documentWithCaptionMessage, that.documentWithCaptionMessage)
                && Objects.equals(requestPhoneNumberMessage, that.requestPhoneNumberMessage)
                && Objects.equals(viewOnceV2Message, that.viewOnceV2Message)
                && Objects.equals(encryptedReactionMessage, that.encryptedReactionMessage)
                && Objects.equals(editedMessage, that.editedMessage)
                && Objects.equals(viewOnceV2ExtensionMessage, that.viewOnceV2ExtensionMessage)
                && Objects.equals(newsletterAdminInviteMessage, that.newsletterAdminInviteMessage)
                && Objects.equals(deviceInfo, that.deviceInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                textWithNoContextMessage,
                senderKeyDistributionMessage,
                imageMessage,
                contactMessage,
                locationMessage,
                textMessage,
                documentMessage,
                audioMessage,
                videoMessage,
                callMessage,
                protocolMessage,
                contactsArrayMessage,
                highlyStructuredMessage,
                sendPaymentMessage,
                liveLocationMessage,
                requestPaymentMessage,
                declinePaymentRequestMessage,
                cancelPaymentRequestMessage,
                templateMessage,
                stickerMessage,
                groupInviteMessage,
                templateReplyMessage,
                productMessage,
                deviceSentMessage,
                deviceSyncMessage,
                listMessage,
                viewOnceMessage,
                orderMessage,
                listResponseMessage,
                ephemeralMessage,
                invoiceMessage,
                buttonsMessage,
                buttonsResponseMessage,
                paymentInviteMessage,
                interactiveMessage,
                reactionMessage,
                stickerSyncMessage,
                interactiveResponseMessage,
                pollCreationMessage,
                pollUpdateMessage,
                keepInChatMessage,
                documentWithCaptionMessage,
                requestPhoneNumberMessage,
                viewOnceV2Message,
                encryptedReactionMessage,
                editedMessage,
                viewOnceV2ExtensionMessage,
                newsletterAdminInviteMessage,
                deviceInfo
        );
    }

    /**
     * Returns an empty message container
     *
     * @return a non-null container
     */
    public static MessageContainer empty() {
        return new MessageContainerBuilder().build();
    }

    public static Optional<MessageContainer> ofJson(JSONObject jsonObject) {
        return Optional.empty();
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
                    builder.senderKeyDistributionMessage(senderKeyDistribution);
            case ImageMessage image -> builder.imageMessage(image);
            case ContactMessage contact -> builder.contactMessage(contact);
            case LocationMessage location -> builder.locationMessage(location);
            case TextMessage text -> builder.textMessage(text);
            case DocumentMessage document -> builder.documentMessage(document);
            case AudioMessage audio -> builder.audioMessage(audio);
            case VideoOrGifMessage video -> builder.videoMessage(video);
            case ProtocolMessage protocol -> builder.protocolMessage(protocol);
            case ContactsMessage contactsArray -> builder.contactsArrayMessage(contactsArray);
            case HighlyStructuredMessage highlyStructured ->
                    builder.highlyStructuredMessage(highlyStructured);
            case SendPaymentMessage sendPayment -> builder.sendPaymentMessage(sendPayment);
            case LiveLocationMessage liveLocation -> builder.liveLocationMessage(liveLocation);
            case RequestPaymentMessage requestPayment -> builder.requestPaymentMessage(requestPayment);
            case DeclinePaymentRequestMessage declinePaymentRequest ->
                    builder.declinePaymentRequestMessage(declinePaymentRequest);
            case CancelPaymentRequestMessage cancelPaymentRequest ->
                    builder.cancelPaymentRequestMessage(cancelPaymentRequest);
            case TemplateMessage template -> builder.templateMessage(template);
            case StickerMessage sticker -> builder.stickerMessage(sticker);
            case GroupInviteMessage groupInvite -> builder.groupInviteMessage(groupInvite);
            case TemplateReplyMessage templateButtonReply ->
                    builder.templateReplyMessage(templateButtonReply);
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
            case PollCreationMessage pollCreationMessage ->
                    builder.pollCreationMessage(pollCreationMessage);
            case PollUpdateMessage pollUpdateMessage -> builder.pollUpdateMessage(pollUpdateMessage);
            case KeepInChatMessage keepInChatMessage -> builder.keepInChatMessage(keepInChatMessage);
            case RequestPhoneNumberMessage requestPhoneNumberMessage ->
                    builder.requestPhoneNumberMessage(requestPhoneNumberMessage);
            case EncryptedReactionMessage encReactionMessage ->
                    builder.encryptedReactionMessage(encReactionMessage);
            case CallMessage callMessage -> builder.callMessage(callMessage);
            case NewsletterAdminInviteMessage newsletterAdminInviteMessage ->
                    builder.newsletterAdminInviteMessage(newsletterAdminInviteMessage);
            default -> {
            }
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
        var futureMessageContainer = new FutureMessageContainerBuilder()
                .content(MessageContainer.of(message))
                .build();
        return new MessageContainerBuilder()
                .viewOnceMessage(futureMessageContainer)
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
        var futureMessageContainer = new FutureMessageContainerBuilder()
                .content(MessageContainer.of(message))
                .build();
        return new MessageContainerBuilder()
                .viewOnceV2Message(futureMessageContainer)
                .build();
    }

    /**
     * Constructs a new MessageContainer from a message of any type marking it as ephemeral
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofEphemeral(T message) {
        var futureMessageContainer = new FutureMessageContainerBuilder()
                .content(MessageContainer.of(message))
                .build();
        return new MessageContainerBuilder()
                .ephemeralMessage(futureMessageContainer)
                .build();
    }

    /**
     * Constructs a new MessageContainer from an edited message
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofEditedMessage(T message) {
        var futureMessageContainer = new FutureMessageContainerBuilder()
                .content(MessageContainer.of(message))
                .build();
        return new MessageContainerBuilder()
                .editedMessage(futureMessageContainer)
                .build();
    }

    /**
     * Constructs a new MessageContainer from a document with caption message
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofDocumentWithCaption(T message) {
        var futureMessageContainer = new FutureMessageContainerBuilder()
                .content(MessageContainer.of(message))
                .build();
        return new MessageContainerBuilder()
                .documentWithCaptionMessage(futureMessageContainer)
                .build();
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
            return viewOnceMessage.content().content();
        }
        if (this.orderMessage != null) {
            return orderMessage;
        }
        if (this.listResponseMessage != null) {
            return listResponseMessage;
        }
        if (this.ephemeralMessage != null) {
            return ephemeralMessage.content().content();
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
            return documentWithCaptionMessage.content().content();
        }
        if (requestPhoneNumberMessage != null) {
            return requestPhoneNumberMessage;
        }
        if (viewOnceV2Message != null) {
            return viewOnceV2Message.content.content();
        }
        if (encryptedReactionMessage != null) {
            return encryptedReactionMessage;
        }
        if (editedMessage != null) {
            return editedMessage.content().content();
        }
        if (viewOnceV2ExtensionMessage != null) {
            return viewOnceV2ExtensionMessage.content().content();
        }
        if (callMessage != null) {
            return callMessage;
        }
        if (newsletterAdminInviteMessage != null) {
            return newsletterAdminInviteMessage;
        }
        // This needs to be last
        if (this.senderKeyDistributionMessage != null) {
            return senderKeyDistributionMessage;
        }
        return EMPTY_MESSAGE;
    }

    /**
     * Returns the first populated contextual message inside this container
     *
     * @return a non-null Optional ContextualMessage
     */
    public Optional<ContextualMessage> contentWithContext() {
        return Optional.of(content())
                .filter(entry -> entry instanceof ContextualMessage)
                .map(entry -> (ContextualMessage) entry);
    }

    /**
     * Checks whether the message that this container wraps matches the provided type
     *
     * @param type the non-null type to check against
     * @return a boolean
     */
    public boolean hasType(Message.Type type) {
        return content().type() == type;
    }

    /**
     * Checks whether the message that this container wraps matches the provided category
     *
     * @param category the non-null category to check against
     * @return a boolean
     */
    public boolean hasCategory(Message.Category category) {
        return content().category() == category;
    }

    /**
     * Returns the type of the message
     *
     * @return a non-null type
     */
    public Message.Type type() {
        if (textWithNoContextMessage != null) {
            return Message.Type.TEXT;
        }

        if (ephemeralMessage != null) {
            return Message.Type.EPHEMERAL;
        }

        if (viewOnceMessage != null || viewOnceV2Message != null || viewOnceV2ExtensionMessage != null) {
            return Message.Type.VIEW_ONCE;
        }

        if (editedMessage != null) {
            return Message.Type.EDITED;
        }

        return content().type();
    }

    /**
     * Returns the deep type of the message unwrapping ephemeral and view once messages
     *
     * @return a non-null type
     */
    public Message.Type deepType() {
        return content().type();
    }

    /**
     * Returns the category of the message
     *
     * @return a non-null category
     */
    public Message.Category category() {
        return content().category();
    }

    /**
     * Converts this message to an ephemeral message
     *
     * @return a non-null message container
     */
    public MessageContainer toEphemeral() {
        if (type() == Message.Type.EPHEMERAL) {
            return this;
        }

        var futureMessageContainer = new FutureMessageContainerBuilder()
                .content(MessageContainer.of(content()))
                .build();
        return new MessageContainerBuilder()
                .ephemeralMessage(futureMessageContainer)
                .deviceInfo(deviceInfo)
                .build();
    }

    /**
     * Converts this message to a view once message
     *
     * @return a non-null message container
     */
    public MessageContainer toViewOnce() {
        if (type() == Message.Type.VIEW_ONCE) {
            return this;
        }

        var futureMessageContainer = new FutureMessageContainerBuilder()
                .content(MessageContainer.of(content()))
                .build();
        return new MessageContainerBuilder()
                .viewOnceMessage(futureMessageContainer)
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

        return this;
    }

    /**
     * Returns a copy of this container with a different device info
     *
     * @return a non-null message container
     */
    public MessageContainer withDeviceInfo(DeviceContextInfo deviceInfo) {
        if (deviceSentMessage != null) {
            return ofBuilder(deviceSentMessage)
                    .deviceInfo(deviceInfo)
                    .build();
        }

        if (viewOnceMessage != null) {
            return new MessageContainerBuilder()
                    .viewOnceMessage(viewOnceMessage)
                    .deviceInfo(deviceInfo)
                    .build();
        }

        if (ephemeralMessage != null) {
            return new MessageContainerBuilder()
                    .ephemeralMessage(ephemeralMessage)
                    .deviceInfo(deviceInfo)
                    .build();
        }

        if (documentWithCaptionMessage != null) {
            return new MessageContainerBuilder()
                    .documentWithCaptionMessage(documentWithCaptionMessage)
                    .deviceInfo(deviceInfo)
                    .build();
        }

        if (viewOnceV2Message != null) {
            return new MessageContainerBuilder()
                    .viewOnceV2Message(viewOnceV2Message)
                    .deviceInfo(deviceInfo)
                    .build();
        }

        if (editedMessage != null) {
            return new MessageContainerBuilder()
                    .editedMessage(editedMessage)
                    .deviceInfo(deviceInfo)
                    .build();
        }

        if (viewOnceV2ExtensionMessage != null) {
            return new MessageContainerBuilder()
                    .viewOnceV2ExtensionMessage(viewOnceV2ExtensionMessage)
                    .deviceInfo(deviceInfo)
                    .build();
        }

        return ofBuilder(content())
                .deviceInfo(deviceInfo)
                .build();
    }

    public Optional<DeviceContextInfo> deviceInfo() {
        return Optional.ofNullable(deviceInfo);
    }

    public Optional<SenderKeyDistributionMessage> senderKeyDistributionMessage() {
        return Optional.ofNullable(senderKeyDistributionMessage);
    }

    /**
     * Returns whether this container is empty
     *
     * @return a boolean
     */
    public boolean isEmpty() {
        return hasType(Message.Type.EMPTY);
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
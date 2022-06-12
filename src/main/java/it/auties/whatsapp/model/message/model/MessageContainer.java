package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.info.CallInfo;
import it.auties.whatsapp.model.info.MessageContextInfo;
import it.auties.whatsapp.model.message.business.InteractiveMessage;
import it.auties.whatsapp.model.message.business.ProductMessage;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.device.DeviceSentMessage;
import it.auties.whatsapp.model.message.device.DeviceSyncMessage;
import it.auties.whatsapp.model.message.payment.*;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.model.message.server.StickerSyncRMRMessage;
import it.auties.whatsapp.model.message.standard.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.Optional;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A container for all types of messages known currently to WhatsappWeb.
 * <p>
 * Only one of these properties should be populated, however it's not certain as Whatsapp's Protobuf doesn't use a oneof instruction as it would be logical to in said case.
 * This may imply that in some particular and rare cases more than one property can be populated.
 * <p>
 * There are several categories of messages:
 * <ul>
 *     <li>Server messages</li>
 *     <li>Device messages</li>
 *     <li>Button messages</li>
 *     <li>Product messages</li>
 *     <li>Payment messages</li>
 *     <li>Standard messages</li>
 * </ul>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "newMessageContainer", buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public class MessageContainer implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING)
    private String textWithNoContext;

    /**
     * Sender key distribution message
     */
    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = SenderKeyDistributionMessage.class)
    private SenderKeyDistributionMessage senderKeyDistribution;

    /**
     * Image message
     */
    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ImageMessage.class)
    private ImageMessage image;

    /**
     * Contact message
     */
    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = ContactMessage.class)
    private ContactMessage contact;

    /**
     * Location message
     */
    @ProtobufProperty(index = 5, type = MESSAGE, concreteType = LocationMessage.class)
    private LocationMessage location;

    /**
     * Text message
     */
    @ProtobufProperty(index = 6, type = MESSAGE, concreteType = TextMessage.class)
    private TextMessage text;

    /**
     * Document message
     */
    @ProtobufProperty(index = 7, type = MESSAGE, concreteType = DocumentMessage.class)
    private DocumentMessage document;

    /**
     * Audio message
     */
    @ProtobufProperty(index = 8, type = MESSAGE, concreteType = AudioMessage.class)
    private AudioMessage audio;

    /**
     * Video message
     */
    @ProtobufProperty(index = 9, type = MESSAGE, concreteType = VideoMessage.class)
    private VideoMessage video;

    /**
     * Call message
     */
    @ProtobufProperty(index = 10, type = MESSAGE, concreteType = CallInfo.class)
    private CallInfo call;

    /**
     * Sever message
     */
    @ProtobufProperty(index = 12, type = MESSAGE, concreteType = ProtocolMessage.class)
    private ProtocolMessage protocol;

    /**
     * Contact array message
     */
    @ProtobufProperty(index = 13, type = MESSAGE, concreteType = ContactsArrayMessage.class)
    private ContactsArrayMessage contactsArray;

    /**
     * Highly structured message
     */
    @ProtobufProperty(index = 14, type = MESSAGE, concreteType = ButtonStructureMessage.class)
    private ButtonStructureMessage highlyStructured;

    /**
     * Send payment message
     */
    @ProtobufProperty(index = 16, type = MESSAGE, concreteType = SendPaymentMessage.class)
    private SendPaymentMessage sendPayment;

    /**
     * Live location message
     */
    @ProtobufProperty(index = 18, type = MESSAGE, concreteType = LiveLocationMessage.class)
    private LiveLocationMessage liveLocation;

    /**
     * Request payment message
     */
    @ProtobufProperty(index = 22, type = MESSAGE, concreteType = RequestPaymentMessage.class)
    private RequestPaymentMessage requestPayment;

    /**
     * Decline payment request message
     */
    @ProtobufProperty(index = 23, type = MESSAGE, concreteType = DeclinePaymentRequestMessage.class)
    private DeclinePaymentRequestMessage declinePaymentRequest;

    /**
     * Cancel payment request message
     */
    @ProtobufProperty(index = 24, type = MESSAGE, concreteType = CancelPaymentRequestMessage.class)
    private CancelPaymentRequestMessage cancelPaymentRequest;

    /**
     * Template message
     */
    @ProtobufProperty(index = 25, type = MESSAGE, concreteType = ButtonTemplateMessage.class)
    private ButtonTemplateMessage template;

    /**
     * Sticker message
     */
    @ProtobufProperty(index = 26, type = MESSAGE, concreteType = StickerMessage.class)
    private StickerMessage sticker;

    /**
     * Group invite message
     */
    @ProtobufProperty(index = 28, type = MESSAGE, concreteType = GroupInviteMessage.class)
    private GroupInviteMessage groupInvite;

    /**
     * Template button reply message
     */
    @ProtobufProperty(index = 29, type = MESSAGE, concreteType = ButtonTemplateReplyMessage.class)
    private ButtonTemplateReplyMessage templateButtonReply;

    /**
     * Product message
     */
    @ProtobufProperty(index = 30, type = MESSAGE, concreteType = ProductMessage.class)
    private ProductMessage product;

    /**
     * Device sent message
     */
    @ProtobufProperty(index = 31, type = MESSAGE, concreteType = DeviceSentMessage.class)
    private DeviceSentMessage deviceSent;

    /**
     * Device dataSync message
     */
    @ProtobufProperty(index = 32, type = MESSAGE, concreteType = DeviceSyncMessage.class)
    private DeviceSyncMessage deviceSync;

    /**
     * List message
     */
    @ProtobufProperty(index = 36, type = MESSAGE, concreteType = ButtonListMessage.class)
    private ButtonListMessage buttonList;

    /**
     * View once message
     */
    @ProtobufProperty(index = 37, type = MESSAGE, concreteType = FutureMessageContainer.class)
    private FutureMessageContainer viewOnce;

    /**
     * Order message
     */
    @ProtobufProperty(index = 38, type = MESSAGE, concreteType = PaymentOrderMessage.class)
    private PaymentOrderMessage order;

    /**
     * List response message
     */
    @ProtobufProperty(index = 39, type = MESSAGE, concreteType = ButtonListResponseMessage.class)
    private ButtonListResponseMessage buttonListResponse;

    /**
     * Ephemeral message
     */
    @ProtobufProperty(index = 40, type = MESSAGE, concreteType = FutureMessageContainer.class)
    private FutureMessageContainer ephemeral;

    /**
     * Invoice message
     */
    @ProtobufProperty(index = 41, type = MESSAGE, concreteType = PaymentInvoiceMessage.class)
    private PaymentInvoiceMessage invoice;

    /**
     * Buttons message
     */
    @ProtobufProperty(index = 42, type = MESSAGE, concreteType = ButtonsMessage.class)
    private ButtonsMessage buttons;

    /**
     * Buttons response message
     */
    @ProtobufProperty(index = 43, type = MESSAGE, concreteType = ButtonsResponseMessage.class)
    private ButtonsResponseMessage buttonsResponse;

    /**
     * Payment invite message
     */
    @ProtobufProperty(index = 44, type = MESSAGE, concreteType = PaymentInviteMessage.class)
    private PaymentInviteMessage paymentInvite;

    /**
     * Interactive message
     */
    @ProtobufProperty(index = 45, type = MESSAGE, concreteType = InteractiveMessage.class)
    private InteractiveMessage interactive;

    /**
     * Reaction message
     */
    @ProtobufProperty(index = 46, type = MESSAGE, concreteType = ReactionMessage.class)
    private ReactionMessage reaction;

    /**
     * Sticker sync message
     */
    @ProtobufProperty(index = 47, type = MESSAGE, concreteType = StickerSyncRMRMessage.class)
    private StickerSyncRMRMessage stickerSync;

    /**
     * Message context info
     */
    @ProtobufProperty(index = 35, type = MESSAGE, concreteType = MessageContextInfo.class)
    @Getter
    private MessageContextInfo messageContextInfo;

    /**
     * Constructs a new MessageContainer from a message of any type
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    @SuppressWarnings("PatternVariableHidesField")
    public <T extends Message> MessageContainer(@NonNull T message) {
        switch (message) {
            case SenderKeyDistributionMessage senderKeyDistribution ->
                    this.senderKeyDistribution = senderKeyDistribution;
            case ImageMessage image -> this.image = image;
            case ContactMessage contact -> this.contact = contact;
            case LocationMessage location -> this.location = location;
            case TextMessage text -> this.text = text;
            case DocumentMessage document -> this.document = document;
            case AudioMessage audio -> this.audio = audio;
            case VideoMessage video -> this.video = video;
            case ProtocolMessage protocol -> this.protocol = protocol;
            case ContactsArrayMessage contactsArray -> this.contactsArray = contactsArray;
            case ButtonStructureMessage highlyStructured -> this.highlyStructured = highlyStructured;
            case SendPaymentMessage sendPayment -> this.sendPayment = sendPayment;
            case LiveLocationMessage liveLocation -> this.liveLocation = liveLocation;
            case RequestPaymentMessage requestPayment -> this.requestPayment = requestPayment;
            case DeclinePaymentRequestMessage declinePaymentRequest ->
                    this.declinePaymentRequest = declinePaymentRequest;
            case CancelPaymentRequestMessage cancelPaymentRequest -> this.cancelPaymentRequest = cancelPaymentRequest;
            case ButtonTemplateMessage template -> this.template = template;
            case StickerMessage sticker -> this.sticker = sticker;
            case GroupInviteMessage groupInvite -> this.groupInvite = groupInvite;
            case ButtonTemplateReplyMessage templateButtonReply -> this.templateButtonReply = templateButtonReply;
            case ProductMessage product -> this.product = product;
            case DeviceSentMessage deviceSent -> this.deviceSent = deviceSent;
            case DeviceSyncMessage deviceSync -> this.deviceSync = deviceSync;
            case ButtonListMessage buttonsList -> this.buttonList = buttonsList;
            case PaymentOrderMessage order -> this.order = order;
            case ButtonListResponseMessage listResponse -> this.buttonListResponse = listResponse;
            case PaymentInvoiceMessage invoice -> this.invoice = invoice;
            case ButtonsMessage buttons -> this.buttons = buttons;
            case ButtonsResponseMessage buttonsResponse -> this.buttonsResponse = buttonsResponse;
            case PaymentInviteMessage paymentInvite -> this.paymentInvite = paymentInvite;
            default -> throw new IllegalStateException("Unsupported message: " + message);
        }
    }

    /**
     * Constructs a new MessageContainer from a message of any type
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer of(@NonNull T message) {
        return new MessageContainer(message);
    }

    /**
     * Constructs a new MessageContainer from a text message with no context
     *
     * @param message the text message with no context
     */
    public static MessageContainer of(@NonNull String message) {
        return MessageContainer.newMessageContainer()
                .textWithNoContext(message)
                .create();
    }

    /**
     * Constructs a new MessageContainer from a message of any type that can only be seen once
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofViewOnce(@NonNull T message) {
        return MessageContainer.newMessageContainer()
                .viewOnce(FutureMessageContainer.of(MessageContainer.of(message)))
                .create();
    }

    /**
     * Constructs a new MessageContainer from a message of any type marking it as ephemeral
     *
     * @param message the message that the new container should wrap
     * @param <T>     the type of the message
     */
    public static <T extends Message> MessageContainer ofEphemeral(@NonNull T message) {
        return MessageContainer.newMessageContainer()
                .ephemeral(FutureMessageContainer.of(MessageContainer.of(message)))
                .create();
    }

    /**
     * Returns the first populated message inside this container
     *
     * @return a nullable Message
     */
    public Message content() {
        if (this.textWithNoContext != null)
            return TextMessage.of(textWithNoContext);
        if (this.senderKeyDistribution != null)
            return senderKeyDistribution;
        if (this.image != null)
            return image;
        if (this.contact != null)
            return contact;
        if (this.location != null)
            return location;
        if (this.text != null)
            return text;
        if (this.document != null)
            return document;
        if (this.audio != null)
            return audio;
        if (this.video != null)
            return video;
        if (this.protocol != null)
            return protocol;
        if (this.contactsArray != null)
            return contactsArray;
        if (this.highlyStructured != null)
            return highlyStructured;
        if (this.sendPayment != null)
            return sendPayment;
        if (this.liveLocation != null)
            return liveLocation;
        if (this.requestPayment != null)
            return requestPayment;
        if (this.declinePaymentRequest != null)
            return declinePaymentRequest;
        if (this.cancelPaymentRequest != null)
            return cancelPaymentRequest;
        if (this.template != null)
            return template;
        if (this.sticker != null)
            return sticker;
        if (this.groupInvite != null)
            return groupInvite;
        if (this.templateButtonReply != null)
            return templateButtonReply;
        if (this.product != null)
            return product;
        if (this.deviceSent != null)
            return deviceSent;
        if (this.deviceSync != null)
            return deviceSync;
        if (this.buttonList != null)
            return buttonList;
        if (this.viewOnce != null)
            return viewOnce.unbox();
        if (this.order != null)
            return order;
        if (this.buttonListResponse != null)
            return buttonListResponse;
        if (this.ephemeral != null)
            return ephemeral.unbox();
        if (this.invoice != null)
            return invoice;
        if (this.buttons != null)
            return buttons;
        if (this.buttonsResponse != null)
            return buttonsResponse;
        if (this.paymentInvite != null)
            return paymentInvite;
        return null;
    }

    /**
     * Returns the first populated contextual message inside this container
     *
     * @return a non-null Optional ContextualMessage
     */
    public Optional<ContextualMessage> contentWithContext() {
        if (this.image != null)
            return Optional.of(image);
        if (this.contact != null)
            return Optional.of(contact);
        if (this.location != null)
            return Optional.of(location);
        if (this.text != null)
            return Optional.of(text);
        if (this.document != null)
            return Optional.of(document);
        if (this.audio != null)
            return Optional.of(audio);
        if (this.video != null)
            return Optional.of(video);
        if (this.contactsArray != null)
            return Optional.of(contactsArray);
        if (this.liveLocation != null)
            return Optional.of(liveLocation);
        if (this.template != null)
            return Optional.of(template);
        if (this.sticker != null)
            return Optional.of(sticker);
        if (this.groupInvite != null)
            return Optional.of(groupInvite);
        if (this.templateButtonReply != null)
            return Optional.of(templateButtonReply);
        if (this.product != null)
            return Optional.of(product);
        if (this.buttonList != null)
            return Optional.of(buttonList);
        if (this.invoice != null)
            return Optional.of(invoice);
        if (this.buttons != null)
            return Optional.of(buttons);
        if (this.buttonsResponse != null)
            return Optional.of(buttonsResponse);
        return Optional.empty();
    }

    /**
     * Returns the type of message that this object wraps
     *
     * @return a non-null enumerated type
     */
    public MessageContainer.ContentType type() {
        return switch (content()) {
            case null -> ContentType.EMPTY;
            case DeviceMessage ignored -> ContentType.DEVICE;
            case PaymentMessage ignored -> ContentType.PAYMENT;
            case ServerMessage ignored -> ContentType.SERVER;
            case ButtonMessage ignored -> ContentType.BUTTON;
            case ProductMessage ignored -> ContentType.PRODUCT;
            default -> ContentType.STANDARD;
        };
    }

    /**
     * Returns whether this container is empty
     *
     * @return true if this container contains no message
     */
    public boolean isEmpty() {
        return type() == ContentType.EMPTY;
    }

    /**
     * Returns whether this container contains a standard message
     *
     * @return true if this container contains a standard message
     */
    public boolean isStandard() {
        return type() == ContentType.STANDARD;
    }

    /**
     * Returns whether this container contains a sever message
     *
     * @return true if this container contains a sever message
     */
    public boolean isServer() {
        return type() == ContentType.SERVER;
    }

    /**
     * Returns whether this container contains a device message
     *
     * @return true if this container contains a device message
     */
    public boolean isDevice() {
        return type() == ContentType.DEVICE;
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
     * Converts this container into a String
     *
     * @return a non-null String
     */
    @Override
    public String toString() {
        return Objects.toString(content());
    }

    /**
     * The constants of this enumerated type describe the various types of messages that a {@link MessageContainer} can wrap
     */
    public enum ContentType {
        /**
         * Server message
         */
        EMPTY,

        /**
         * Server message
         */
        SERVER,

        /**
         * PAYMENT message
         */
        PAYMENT,

        /**
         * Device message
         */
        DEVICE,

        /**
         * Button message
         */
        BUTTON,

        /**
         * Product message
         */
        PRODUCT,

        /**
         * Standard message
         */
        STANDARD
    }
}

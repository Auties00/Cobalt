package it.auties.whatsapp.protobuf.message.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSetter;
import it.auties.protobuf.annotation.ProtobufType;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.info.CallInfo;
import it.auties.whatsapp.protobuf.message.button.*;
import it.auties.whatsapp.protobuf.message.device.DeviceSentMessage;
import it.auties.whatsapp.protobuf.message.device.DeviceSyncMessage;
import it.auties.whatsapp.protobuf.message.payment.*;
import it.auties.whatsapp.protobuf.message.server.ProtocolMessage;
import it.auties.whatsapp.protobuf.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.protobuf.message.standard.*;
import it.auties.whatsapp.protobuf.signal.message.SignalDistributionMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.Objects;
import java.util.Optional;

/**
 * A container for all types of messages known currently to WhatsappWeb.
 *
 * Only one of these properties should be populated, however it's not certain as Whatsapp's Protobuf doesn't use a oneof instruction as it would be logical to in said case.
 * This may imply that in some particular and rare cases more than one property can be populated.
 *
 * There are several categories of messages:
 * <ul>
 *     <li>Server messages</li>
 *     <li>Device messages</li>
 *     <li>Button messages</li>
 *     <li>Product messages</li>
 *     <li>Payment messages</li>
 *     <li>Standard messages</li>
 * </ul>
 *
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(fluent = true)
public class MessageContainer { // TODO: Find a way to refactor this while keeping compatibility with Whatsapp
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String textWithNoContext;

  @JsonSetter("1")
  private void mapToMessage(String textWithNoContext){
    this.textWithNoContext = textWithNoContext;
    this.text = new TextMessage(textWithNoContext);
  }
  
  /**
   * Sender key distribution message
   */
  @JsonProperty("2")
  @JsonPropertyDescription("distribution")
  private SenderKeyDistributionMessage senderKeyDistribution;
  
  /**
   * Image message
   */
  @JsonProperty("3")
  @JsonPropertyDescription("image")
  private ImageMessage image;
  
  /**
   * Contact message
   */
  @JsonProperty("4")
  @JsonPropertyDescription("contact")
  private ContactMessage contact;

  /**
   * Location message
   */
  @JsonProperty("5")
  @JsonPropertyDescription("location")
  private LocationMessage location;

  /**
   * Text message
   */
  @JsonProperty("6")
  @JsonPropertyDescription("text")
  private TextMessage text;

  /**
   * Document message
   */
  @JsonProperty("7")
  @JsonPropertyDescription("document")
  private DocumentMessage document;
  
  /**
   * Audio message
   */
  @JsonProperty("8")
  @JsonPropertyDescription("audio")
  private AudioMessage audio;

  /**
   * Video message
   */
  @JsonProperty("9")
  @JsonPropertyDescription("video")
  private VideoMessage video;
  
  /**
   * Call message
   */
  @JsonProperty("10")
  @JsonPropertyDescription("call")
  private CallInfo call;

  /**
   * Sever message
   */
  @JsonProperty("12")
  @JsonPropertyDescription("protocol")
  private ProtocolMessage protocol;
  
  /**
   * Contact array message
   */
  @JsonProperty("13")
  @JsonPropertyDescription("contacts")
  private ContactsArrayMessage contactsArray;

  /**
   * Highly structured message
   */
  @JsonProperty("14")
  @JsonPropertyDescription("highlyStructured")
  private StructuredButtonMessage highlyStructured;
  
  /**
   * Fast ratchet key sender key distribution message
   */
  @JsonProperty("15")
  @JsonPropertyDescription("fastRatchetKeySenderKeyDistribution")
  private SignalDistributionMessage fastRatchetKeySenderKeyDistribution;
  
  /**
   * Send payment message
   */
  @JsonProperty("16")
  @JsonPropertyDescription("sendPayment")
  private SendPaymentMessage sendPayment;

  /**
   * Live location message
   */
  @JsonProperty("18")
  @JsonPropertyDescription("liveLocation")
  private LiveLocationMessage liveLocation;
  
  /**
   * Request payment message
   */
  @JsonProperty("22")
  @JsonPropertyDescription("requestPayment")
  private RequestPaymentMessage requestPayment;

  /**
   * Decline payment request message
   */
  @JsonProperty("23")
  @JsonPropertyDescription("declinePaymentRequest")
  private DeclinePaymentRequestMessage declinePaymentRequest;
  
  /**
   * Cancel payment request message
   */
  @JsonProperty("24")
  @JsonPropertyDescription("cancelPaymentRequest")
  private CancelPaymentRequestMessage cancelPaymentRequest;
  
  /**
   * Template message
   */
  @JsonProperty("25")
  @JsonPropertyDescription("template")
  private TemplateMessage template;

  /**
   * Sticker message
   */
  @JsonProperty("26")
  @JsonPropertyDescription("sticker")
  private StickerMessage sticker;

  /**
   * Group invite message
   */
  @JsonProperty("28")
  @JsonPropertyDescription("groupInvite")
  private GroupInviteMessage groupInvite;

  /**
   * Template button reply message
   */
  @JsonProperty("29")
  @JsonPropertyDescription("templateButtonReply")
  private TemplateButtonReplyMessage templateButtonReply;
  
  /**
   * Product message
   */
  @JsonProperty("30")
  @JsonPropertyDescription("product")
  private ProductMessage product;

  /**
   * Device sent message
   */
  @JsonProperty("31")
  @JsonPropertyDescription("deviceSent")
  private DeviceSentMessage deviceSent;
  
  /**
   * Device dataSync message
   */
  @JsonProperty("32")
  @JsonPropertyDescription("deviceSync")
  private DeviceSyncMessage deviceSync;

  /**
   * List message
   */
  @JsonProperty("36")
  @JsonPropertyDescription("buttonsList")
  private ListMessage buttonsList;

  /**
   * View once message
   */
  @JsonProperty("37")
  @JsonPropertyDescription("viewOnce")
  @ProtobufType(MessageContainer.class)
  private Message viewOnce;

  @JsonSetter("37")
  private void mapViewOnce(MessageContainer container){
    if(container == null){
      return;
    }

    this.viewOnce = container.content();
  }

  /**
   * Order message
   */
  @JsonProperty("38")
  @JsonPropertyDescription("order")
  private PaymentOrderMessage order;

  /**
   * List response message
   */
  @JsonProperty("39")
  @JsonPropertyDescription("listResponse")
  private ListResponseMessage listResponse;

  /**
   * Ephemeral message
   */
  @JsonProperty("40")
  @JsonPropertyDescription("ephemeral")
  private Message ephemeral;

  @JsonSetter("40")
  private void mapEphemeral(MessageContainer container){
    if(container == null){
      return;
    }

    this.ephemeral = container.content();
  }

  /**
   * Invoice message
   */
  @JsonProperty("41")
  @JsonPropertyDescription("invoice")
  private PaymentInvoiceMessage invoice;

  /**
   * Buttons message
   */
  @JsonProperty("42")
  @JsonPropertyDescription("buttons")
  private ButtonsMessage buttons;

  /**
   * Buttons response message
   */
  @JsonProperty("43")
  @JsonPropertyDescription("buttonsResponse")
  private ButtonsResponseMessage buttonsResponse;

  /**
   * Payment invite message
   */
  @JsonProperty("44")
  @JsonPropertyDescription("paymentInvite")
  private PaymentInviteMessage paymentInvite;

  // Unsupported for now: MessageContextInfo(35), InteractiveMessage(45), ReactionMessage(46), StickerSyncRMRMessage(47)

  /**
   * Constructs a new MessageContainer from a message of any type
   *
   * @param message the message that the new container should wrap
   * @param <T> the type of the message
   */
  public static <T extends Message> MessageContainer of(@NonNull T message){
    return new MessageContainer(message);
  }

  /**
   * Constructs a new MessageContainer from a message of any type that can only be seen once
   *
   * @param message the message that the new container should wrap
   * @param <T> the type of the message
   */
  public static <T extends Message> MessageContainer ofViewOnce(@NonNull T message){
    return MessageContainer.builder()
            .viewOnce(message)
            .build();
  }

  /**
   * Constructs a new MessageContainer from a message of any type marking it as ephemeral
   *
   * @param message the message that the new container should wrap
   * @param <T> the type of the message
   */
  public static <T extends Message> MessageContainer ofEphemeral(@NonNull T message){
    return MessageContainer.builder()
            .ephemeral(message)
            .build();
  }

  /**
   * Constructs a new MessageContainer from a message of any type
   * 
   * @param message the message that the new container should wrap
   * @param <T> the type of the message
   */
  public <T extends Message> MessageContainer(@NonNull T message){
    switch (message) {
      case SenderKeyDistributionMessage senderKeyDistribution -> this.senderKeyDistribution = senderKeyDistribution;
      case ImageMessage image -> this.image = image;
      case ContactMessage contact -> this.contact = contact;
      case LocationMessage location -> this.location = location;
      case TextMessage extendedText -> this.text = extendedText;
      case DocumentMessage document -> this.document = document;
      case AudioMessage audio -> this.audio = audio;
      case VideoMessage video -> this.video = video;
      case ProtocolMessage protocol -> this.protocol = protocol;
      case ContactsArrayMessage contactsArray -> this.contactsArray = contactsArray;
      case StructuredButtonMessage highlyStructured -> this.highlyStructured = highlyStructured;
      case SendPaymentMessage sendPayment -> this.sendPayment = sendPayment;
      case LiveLocationMessage liveLocation -> this.liveLocation = liveLocation;
      case RequestPaymentMessage requestPayment -> this.requestPayment = requestPayment;
      case DeclinePaymentRequestMessage declinePaymentRequest -> this.declinePaymentRequest = declinePaymentRequest;
      case CancelPaymentRequestMessage cancelPaymentRequest -> this.cancelPaymentRequest = cancelPaymentRequest;
      case TemplateMessage template -> this.template = template;
      case StickerMessage sticker -> this.sticker = sticker;
      case GroupInviteMessage groupInvite -> this.groupInvite = groupInvite;
      case TemplateButtonReplyMessage templateButtonReply -> this.templateButtonReply = templateButtonReply;
      case ProductMessage product -> this.product = product;
      case DeviceSentMessage deviceSent -> this.deviceSent = deviceSent;
      case DeviceSyncMessage deviceSync -> this.deviceSync = deviceSync;
      case ListMessage buttonsList -> this.buttonsList = buttonsList;
      case PaymentOrderMessage order -> this.order = order;
      case ListResponseMessage listResponse -> this.listResponse = listResponse;
      case PaymentInvoiceMessage invoice -> this.invoice = invoice;
      case ButtonsMessage buttons -> this.buttons = buttons;
      case ButtonsResponseMessage buttonsResponse -> this.buttonsResponse = buttonsResponse;
      case PaymentInviteMessage paymentInvite -> this.paymentInvite = paymentInvite;
      default -> throw new IllegalStateException("Unsupported message: " + message);
    }
  }

  /**
   * Returns the first populated message inside this container
   *
   * @return a nullable Message
   */
  @JsonIgnore
  public Message content(){
    if(this.senderKeyDistribution != null) return senderKeyDistribution;
    if(this.image != null) return image;
    if(this.contact != null) return contact;
    if(this.location != null) return location;
    if(this.text != null) return text;
    if(this.document != null) return document;
    if(this.audio != null) return audio;
    if(this.video != null) return video;
    if(this.protocol != null) return protocol;
    if(this.contactsArray != null) return contactsArray;
    if(this.highlyStructured != null) return highlyStructured;
    if(this.sendPayment != null) return sendPayment;
    if(this.liveLocation != null) return liveLocation;
    if(this.requestPayment != null) return requestPayment;
    if(this.declinePaymentRequest != null) return declinePaymentRequest;
    if(this.cancelPaymentRequest != null) return cancelPaymentRequest;
    if(this.template != null) return template;
    if(this.sticker != null) return sticker;
    if(this.groupInvite != null) return groupInvite;
    if(this.templateButtonReply != null) return templateButtonReply;
    if(this.product != null) return product;
    if(this.deviceSent != null) return deviceSent;
    if(this.deviceSync != null) return deviceSync;
    if(buttonsList != null) return buttonsList;
    if(order != null) return order;
    if(listResponse != null) return listResponse;
    if(invoice != null) return invoice;
    if(buttons != null) return buttons;
    if(buttonsResponse != null) return buttonsResponse;
    if(paymentInvite != null) return paymentInvite;
    return null;
  }

  /**
   * Returns the first populated contextual message inside this container
   *
   * @return a non-null Optional ContextualMessage
   */
  @JsonIgnore
  public Optional<ContextualMessage> contentWithContext(){
    if(this.image != null) return Optional.of(image);
    if(this.contact != null) return Optional.of(contact);
    if(this.location != null) return Optional.of(location);
    if(this.text != null) return Optional.of(text);
    if(this.document != null) return Optional.of(document);
    if(this.audio != null) return Optional.of(audio);
    if(this.video != null) return Optional.of(video);
    if(this.contactsArray != null) return Optional.of(contactsArray);
    if(this.liveLocation != null) return Optional.of(liveLocation);
    if(this.template != null) return Optional.of(template);
    if(this.sticker != null) return Optional.of(sticker);
    if(this.groupInvite != null) return Optional.of(groupInvite);
    if(this.templateButtonReply != null) return Optional.of(templateButtonReply);
    if(this.product != null) return Optional.of(product);
    if(buttonsList != null) return Optional.of(buttonsList);
    if(invoice != null) return Optional.of(invoice);
    if(buttons != null) return Optional.of(buttons);
    if(buttonsResponse != null) return Optional.of(buttonsResponse);
    return Optional.empty();
  }

  /**
   * Returns the type of message that this object wraps
   *
   * @return a non-null enumerated type
   */
  @JsonIgnore
  public @NonNull MessageContainer.ContentType type(){
    return switch (content()){
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
  @JsonIgnore
  public boolean isEmpty(){
    return type() == ContentType.EMPTY;
  }

  /**
   * Returns whether this container contains a standard message
   *
   * @return true if this container contains a standard message
   */
  @JsonIgnore
  public boolean isStandard(){
    return type() == ContentType.STANDARD;
  }

  /**
   * Returns whether this container contains a sever message
   *
   * @return true if this container contains a sever message
   */
  @JsonIgnore
  public boolean isServer(){
    return type() == ContentType.SERVER;
  }

  /**
   * Returns whether this container contains a device message
   *
   * @return true if this container contains a device message
   */
  @JsonIgnore
  public boolean isDevice(){
    return type() == ContentType.DEVICE;
  }

  /**
   * Returns the call wrapped by this message, if any is present
   *
   * @return a non-null optional
   */
  @JsonIgnore
  public Optional<CallInfo> call() {
    return Optional.ofNullable(call);
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

package it.auties.whatsapp.protobuf.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
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

import java.util.NoSuchElementException;
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
public class MessageContainer { // Not how I would design it, Whatsapp's choice obviously
  // Just a linker
  @JsonSetter("1")
  private void fromText(String textMessageWithNoContext){
    this.text = new TextMessage(textMessageWithNoContext);
  }
  
  /**
   * Sender key distribution message
   */
  @JsonProperty("2")
  private SenderKeyDistributionMessage senderKeyDistribution;
  
  /**
   * Image message
   */
  @JsonProperty("3")
  private ImageMessage image;
  
  /**
   * Contact message
   */
  @JsonProperty("4")
  private ContactMessage contact;

  /**
   * Location message
   */
  @JsonProperty("5")
  private LocationMessage location;

  /**
   * Text message
   */
  @JsonProperty("6")
  private TextMessage text;

  /**
   * Document message
   */
  @JsonProperty("7")
  private DocumentMessage document;
  
  /**
   * Audio message
   */
  @JsonProperty("8")
  private AudioMessage audio;

  /**
   * Video message
   */
  @JsonProperty("9")
  private VideoMessage video;
  
  /**
   * Call message
   */
  @JsonProperty("10")
  private CallInfo call;

  /**
   * Sever message
   */
  @JsonProperty("12")
  private ProtocolMessage protocol;
  
  /**
   * Contact array message
   */
  @JsonProperty("13")
  private ContactsArrayMessage contactsArray;

  /**
   * Highly structured message
   */
  @JsonProperty("14")
  private StructuredButtonMessage highlyStructured;
  
  /**
   * Fast ratchet key sender key distribution message
   */
  @JsonProperty("15")
  private SignalDistributionMessage fastRatchetKeySenderKeyDistribution;
  
  /**
   * Send payment message
   */
  @JsonProperty("16")
  private SendPaymentMessage sendPayment;

  /**
   * Live location message
   */
  @JsonProperty("18")
  private LiveLocationMessage liveLocation;
  
  /**
   * Request payment message
   */
  @JsonProperty("22")
  private RequestPaymentMessage requestPayment;

  /**
   * Decline payment request message
   */
  @JsonProperty("23")
  private DeclinePaymentRequestMessage declinePaymentRequest;
  
  /**
   * Cancel payment request message
   */
  @JsonProperty("24")
  private CancelPaymentRequestMessage cancelPaymentRequest;
  
  /**
   * Template message
   */
  @JsonProperty("25")
  private TemplateMessage template;

  /**
   * Sticker message
   */
  @JsonProperty("26")
  private StickerMessage sticker;

  /**
   * Group invite message
   */
  @JsonProperty("28")
  private GroupInviteMessage groupInvite;

  /**
   * Template button reply message
   */
  @JsonProperty("29")
  private TemplateButtonReplyMessage templateButtonReply;
  
  /**
   * Product message
   */
  @JsonProperty("30")
  private ProductMessage product;

  /**
   * Device sent message
   */
  @JsonProperty("31")
  private DeviceSentMessage deviceSent;
  
  /**
   * Device sync message
   */
  @JsonProperty("32")
  private DeviceSyncMessage deviceSync;

  /**
   * List message
   */
  @JsonProperty("36")
  private ListMessage buttonsList;

  /**
   * View once message
   */
  @JsonProperty("37")
  private Message viewOnce;

  /**
   * Order message
   */
  @JsonProperty("38")
  private PaymentOrderMessage order;

  /**
   * List response message
   */
  @JsonProperty("39")
  private ListResponseMessage listResponse;

  /**
   * Ephemeral message
   */
  @JsonProperty("40")
  private Message ephemeral;

  /**
   * Invoice message
   */
  @JsonProperty("41")
  private PaymentInvoiceMessage invoice;

  /**
   * Buttons message
   */
  @JsonProperty("42")
  private ButtonsMessage buttons;

  /**
   * Buttons response message
   */
  @JsonProperty("43")
  private ButtonsResponseMessage buttonsResponse;

  /**
   * Payment invite message
   */
  @JsonProperty("44")
  private PaymentInviteMessage paymentInvite;

  // Unsupported for now: MessageContextInfo(35), InteractiveMessage(45), ReactionMessage(46), StickerSyncRMRMessage(47)

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
   * Constructs a new MessageContainer from a simple text message
   *
   * @param text the text message that the new container should wrap
   */
  @JsonCreator
  public MessageContainer(String text){
    this.text = new TextMessage(text);
  }

  /**
   * Returns the first populated message inside this container
   *
   * @return a non-null Message
   */
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
    throw new NoSuchElementException("MessageContainer has no content!");
  }

  /**
   * Returns the first populated contextual message inside this container
   *
   * @return a non-null Optional ContextualMessage
   */
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
  public @NonNull MessageContainer.ContentType type(){
    return switch (content()){
      case DeviceMessage ignored -> ContentType.DEVICE;
      case PaymentMessage ignored -> ContentType.PAYMENT;
      case ServerMessage ignored -> ContentType.SERVER;
      case ButtonMessage ignored -> ContentType.BUTTON;
      case ProductMessage ignored -> ContentType.PRODUCT;
      default -> ContentType.STANDARD;
    };
  }

  /**
   * Returns whether this container contains a standard message
   *
   * @return true if this container contains a standard message
   */
  public boolean isStandard(){
    return type() == ContentType.STANDARD;
  }

  /**
   * Returns whether this container contains a sever message
   *
   * @return true if this container contains a sever message
   */
  public boolean isServer(){
    return type() == ContentType.SERVER;
  }

  /**
   * Returns whether this container contains a device message
   *
   * @return true if this container contains a device message
   */
  public boolean isDevice(){
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
   * The constants of this enumerated type describe the various types of messages that a {@link MessageContainer} can wrap
   */
  public enum ContentType {
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

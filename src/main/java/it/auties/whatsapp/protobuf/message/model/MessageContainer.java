package it.auties.whatsapp.protobuf.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.message.business.*;
import it.auties.whatsapp.protobuf.message.device.DeviceSentMessage;
import it.auties.whatsapp.protobuf.message.device.DeviceSyncMessage;
import it.auties.whatsapp.protobuf.message.server.ProtocolMessage;
import it.auties.whatsapp.protobuf.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.protobuf.message.standard.*;
import it.auties.whatsapp.protobuf.unknown.Call;
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
 *     <li>Server messages: {@link MessageContainer#protocolMessage} is populated</li>
 *     <li>Device messages: {@link MessageContainer#deviceSentMessage} or {@link MessageContainer#deviceSyncMessage} are populated</li>
 *     <li>Whatsapp Business messages: {@link MessageContainer#templateButtonReplyMessage}, {@link MessageContainer#templateMessage}, {@link MessageContainer#highlyStructuredMessage}, {@link MessageContainer#productMessage} or any payment message are populated</li>
 *     <li>Standard messages: Any other property may be populated, though only one at the time. These messages may only be sent by contacts.</li>
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
  /**
   * Sever message
   */
  @JsonProperty(value = "12")
  private ProtocolMessage protocolMessage;

  /**
   * Device sync message
   */
  @JsonProperty(value = "32")
  private DeviceSyncMessage deviceSyncMessage;

  /**
   * Device sent message
   */
  @JsonProperty(value = "31")
  private DeviceSentMessage deviceSentMessage;

  /**
   * Fast ratchet key sender key distribution message
   */
  @JsonProperty(value = "15")
  private SenderKeyDistributionMessage fastRatchetKeySenderKeyDistributionMessage;

  /**
   * Sender key distribution message
   */
  @JsonProperty(value = "2")
  private SenderKeyDistributionMessage senderKeyDistributionMessage;

  /**
   * Template button reply message
   */
  @JsonProperty(value = "29")
  private TemplateButtonReplyMessage templateButtonReplyMessage;

  /**
   * Template message
   */
  @JsonProperty(value = "25")
  private TemplateMessage templateMessage;

  /**
   * Highly structured message
   */
  @JsonProperty(value = "14")
  private HighlyStructuredMessage highlyStructuredMessage;

  /**
   * Product message
   */
  @JsonProperty(value = "30")
  private ProductMessage productMessage;

  /**
   * Group invite message
   */
  @JsonProperty(value = "28")
  private GroupInviteMessage groupInviteMessage;

  /**
   * Sticker message
   */
  @JsonProperty(value = "26")
  private StickerMessage stickerMessage;

  /**
   * Cancel payment request message
   */
  @JsonProperty(value = "24")
  private CancelPaymentRequestMessage cancelPaymentRequestMessage;

  /**
   * Decline payment request message
   */
  @JsonProperty(value = "23")
  private DeclinePaymentRequestMessage declinePaymentRequestMessage;

  /**
   * Request payment message
   */
  @JsonProperty(value = "22")
  private RequestPaymentMessage requestPaymentMessage;

  /**
   * Send payment message
   */
  @JsonProperty(value = "16")
  private SendPaymentMessage sendPaymentMessage;

  /**
   * Live location message
   */
  @JsonProperty(value = "18")
  private LiveLocationMessage liveLocationMessage;


  /**
   * Contact array message
   */
  @JsonProperty(value = "13")
  private ContactsArrayMessage contactsArrayMessage;

  /**
   * Call message
   */
  @JsonProperty(value = "10")
  private Call call;

  /**
   * Video message
   */
  @JsonProperty(value = "9")
  private VideoMessage videoMessage;

  /**
   * Audio message
   */
  @JsonProperty(value = "8")
  private AudioMessage audioMessage;

  /**
   * Document message
   */
  @JsonProperty(value = "7")
  private DocumentMessage documentMessage;

  /**
   * Text message
   */
  @JsonProperty(value = "6")
  private TextMessage textMessage;

  /**
   * Location message
   */
  @JsonProperty(value = "5")
  private LocationMessage locationMessage;

  /**
   * Contact message
   */
  @JsonProperty(value = "4")
  private ContactMessage contactMessage;

  /**
   * Image message
   */
  @JsonProperty(value = "3")
  private ImageMessage imageMessage;

  // Just a linker
  @JsonProperty(value = "1")
  private void fromText(String textMessageWithNoContext){
    this.textMessage = new TextMessage(textMessageWithNoContext);
  }

  /**
   * Constructs a new MessageContainer from a message of any type
   * 
   * @param message the message that the new container should wrap
   * @param <T> the type of the message
   */
  public <T extends Message> MessageContainer(@NonNull T message){
    switch (message) {
      case SenderKeyDistributionMessage senderKeyDistributionMessage -> this.senderKeyDistributionMessage = senderKeyDistributionMessage;
      case ImageMessage imageMessage -> this.imageMessage = imageMessage;
      case ContactMessage contactMessage -> this.contactMessage = contactMessage;
      case LocationMessage locationMessage -> this.locationMessage = locationMessage;
      case TextMessage extendedTextMessage -> this.textMessage = extendedTextMessage;
      case DocumentMessage documentMessage -> this.documentMessage = documentMessage;
      case AudioMessage audioMessage -> this.audioMessage = audioMessage;
      case VideoMessage videoMessage -> this.videoMessage = videoMessage;
      case ProtocolMessage protocolMessage -> this.protocolMessage = protocolMessage;
      case ContactsArrayMessage contactsArrayMessage -> this.contactsArrayMessage = contactsArrayMessage;
      case HighlyStructuredMessage highlyStructuredMessage -> this.highlyStructuredMessage = highlyStructuredMessage;
      case SendPaymentMessage sendPaymentMessage -> this.sendPaymentMessage = sendPaymentMessage;
      case LiveLocationMessage liveLocationMessage -> this.liveLocationMessage = liveLocationMessage;
      case RequestPaymentMessage requestPaymentMessage -> this.requestPaymentMessage = requestPaymentMessage;
      case DeclinePaymentRequestMessage declinePaymentRequestMessage -> this.declinePaymentRequestMessage = declinePaymentRequestMessage;
      case CancelPaymentRequestMessage cancelPaymentRequestMessage -> this.cancelPaymentRequestMessage = cancelPaymentRequestMessage;
      case TemplateMessage templateMessage -> this.templateMessage = templateMessage;
      case StickerMessage stickerMessage -> this.stickerMessage = stickerMessage;
      case GroupInviteMessage groupInviteMessage -> this.groupInviteMessage = groupInviteMessage;
      case TemplateButtonReplyMessage templateButtonReplyMessage -> this.templateButtonReplyMessage = templateButtonReplyMessage;
      case ProductMessage productMessage -> this.productMessage = productMessage;
      case DeviceSentMessage deviceSentMessage -> this.deviceSentMessage = deviceSentMessage;
      case DeviceSyncMessage deviceSyncMessage -> this.deviceSyncMessage = deviceSyncMessage;
      default -> throw new IllegalStateException("Unsupported message: " + message);
    }
  }

  /**
   * Constructs a new MessageContainer from a simple text message
   *
   * @param textMessage the text message that the new container should wrap
   */
  @JsonCreator
  public MessageContainer(String textMessage){
    this.textMessage = new TextMessage(textMessage);
  }

  /**
   * Returns the first populated message inside this container
   *
   * @return a non-null Message
   */
  public Message content(){
    if(this.senderKeyDistributionMessage != null) return senderKeyDistributionMessage;
    if(this.imageMessage != null) return imageMessage;
    if(this.contactMessage != null) return contactMessage;
    if(this.locationMessage != null) return locationMessage;
    if(this.textMessage  != null) return textMessage;
    if(this.documentMessage != null) return documentMessage;
    if(this.audioMessage != null) return audioMessage;
    if(this.videoMessage != null) return videoMessage;
    if(this.protocolMessage != null) return protocolMessage;
    if(this.contactsArrayMessage != null) return contactsArrayMessage;
    if(this.highlyStructuredMessage != null) return highlyStructuredMessage;
    if(this.sendPaymentMessage != null) return sendPaymentMessage;
    if(this.liveLocationMessage != null) return liveLocationMessage;
    if(this.requestPaymentMessage != null) return requestPaymentMessage;
    if(this.declinePaymentRequestMessage != null) return declinePaymentRequestMessage;
    if(this.cancelPaymentRequestMessage != null) return cancelPaymentRequestMessage;
    if(this.templateMessage != null) return templateMessage;
    if(this.stickerMessage != null) return stickerMessage;
    if(this.groupInviteMessage != null) return groupInviteMessage;
    if(this.templateButtonReplyMessage != null) return templateButtonReplyMessage;
    if(this.productMessage != null) return productMessage;
    if(this.deviceSentMessage != null) return deviceSentMessage;
    if(this.deviceSyncMessage != null) return deviceSyncMessage;
    throw new NoSuchElementException("MessageContainer has no content!");
  }

  /**
   * Returns the first populated contextual message inside this container
   *
   * @return a non-null Optional ContextualMessage
   */
  public Optional<ContextualMessage> contentWithContext(){
    if(this.imageMessage != null) return Optional.of(imageMessage);
    if(this.contactMessage != null) return Optional.of(contactMessage);
    if(this.locationMessage != null) return Optional.of(locationMessage);
    if(this.textMessage  != null) return Optional.of(textMessage);
    if(this.documentMessage != null) return Optional.of(documentMessage);
    if(this.audioMessage != null) return Optional.of(audioMessage);
    if(this.videoMessage != null) return Optional.of(videoMessage);
    if(this.contactsArrayMessage != null) return Optional.of(contactsArrayMessage);
    if(this.liveLocationMessage != null) return Optional.of(liveLocationMessage);
    if(this.templateMessage != null) return Optional.of(templateMessage);
    if(this.stickerMessage != null) return Optional.of(stickerMessage);
    if(this.groupInviteMessage != null) return Optional.of(groupInviteMessage);
    if(this.templateButtonReplyMessage != null) return Optional.of(templateButtonReplyMessage);
    if(this.productMessage != null) return Optional.of(productMessage);
    return Optional.empty();
  }

  /**
   * Returns the type of message that this object wraps
   *
   * @return a non-null enumerated type
   */
  public @NonNull MessageContainerContentType type(){
    return switch (content()){
      case ServerMessage ignored -> MessageContainerContentType.SERVER;
      case DeviceMessage ignored -> MessageContainerContentType.DEVICE;
      case BusinessMessage ignored -> MessageContainerContentType.BUSINESS;
      default -> MessageContainerContentType.STANDARD;
    };
  }

  /**
   * Returns whether this container contains a standard message
   *
   * @return true if this container contains a standard message
   */
  public boolean isStandard(){
    return type() == MessageContainerContentType.STANDARD;
  }

  /**
   * Returns whether this container contains a sever message
   *
   * @return true if this container contains a sever message
   */
  public boolean isServer(){
    return type() == MessageContainerContentType.SERVER;
  }

  /**
   * Returns whether this container contains a business message
   *
   * @return true if this container contains a business message
   */
  public boolean isBusiness(){
    return type() == MessageContainerContentType.SERVER;
  }

  /**
   * Returns whether this container contains a device message
   *
   * @return true if this container contains a device message
   */
  public boolean isDevice(){
    return type() == MessageContainerContentType.DEVICE;
  }

  /**
   * The constants of this enumerated type describe the various types of messages that a {@link MessageContainer} can wrap
   */
  public enum MessageContainerContentType {
    /**
     * Server message
     */
    SERVER,

    /**
     * Device message
     */
    DEVICE,

    /**
     * Business message
     */
    BUSINESS,

    /**
     * Standard message
     */
    STANDARD,
  }
}

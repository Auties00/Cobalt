package it.auties.whatsapp4j.protobuf.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.protobuf.message.business.*;
import it.auties.whatsapp4j.protobuf.message.device.DeviceSentMessage;
import it.auties.whatsapp4j.protobuf.message.device.DeviceSyncMessage;
import it.auties.whatsapp4j.protobuf.message.security.SenderKeyDistributionMessage;
import it.auties.whatsapp4j.protobuf.message.server.ProtocolMessage;
import it.auties.whatsapp4j.protobuf.message.standard.*;
import it.auties.whatsapp4j.protobuf.model.Call;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import lombok.*;
import lombok.experimental.Accessors;

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
 *     <li>Security messages(Signal's standard): {@link MessageContainer#senderKeyDistributionMessage} or {@link MessageContainer#fastRatchetKeySenderKeyDistributionMessage} are populated. These messages follow the <a href="https://github.com/signalapp/libsignal-protocol-c">Signal Protocol</a></li>
 *     <li>Whatsapp Business messages: {@link MessageContainer#templateButtonReplyMessage}, {@link MessageContainer#templateMessage}, {@link MessageContainer#highlyStructuredMessage}, {@link MessageContainer#productMessage} or any payment message are populated</li>
 *     <li>Standard messages: Any other property may be populated, though only one at the time. These messages may only be sent by contacts.</li>
 * </ul>
 *
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class MessageContainer {
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

  /**
   * This property should NOT be used.
   * The Protobuf used by whatsapp has two fields for text messages: one with context and one without.
   * From a developer perspective though, it's kind of hell to use so I joined them into one.
   */
  @JsonProperty(value = "1")
  private String rawText;

  /**
   * Json accessor to delegate the property rawText to textMessage
   *
   * @param rawText the text to delegate
   */
  @JsonProperty(value = "1")
  private void rawText(String rawText){
    this.textMessage = new TextMessage(rawText);
  }

  /**
   * This accessor should not be used and is provided only as a string utility.
   *
   * @return the raw text
   */
  private String rawText(){
    return rawText;
  }

  /**
   * Constructs a new MessageContainer from a message of any type
   * 
   * @param message the message that the new container should wrap
   * @param <T> the type of the message
   */
  // When Java 17 comes out this will be simplified to an elegant switch statement, for now this is what we've got
  public <T extends Message> MessageContainer(@NonNull T message){
    if(message instanceof SenderKeyDistributionMessage senderKeyDistributionMessage) this.senderKeyDistributionMessage = senderKeyDistributionMessage;
    if(message instanceof ImageMessage imageMessage) this.imageMessage = imageMessage;
    if(message instanceof ContactMessage contactMessage) this.contactMessage = contactMessage;
    if(message instanceof LocationMessage locationMessage) this.locationMessage = locationMessage;
    if(message instanceof TextMessage extendedTextMessage) this.textMessage = extendedTextMessage;
    if(message instanceof DocumentMessage documentMessage) this.documentMessage = documentMessage;
    if(message instanceof AudioMessage audioMessage) this.audioMessage = audioMessage;
    if(message instanceof VideoMessage videoMessage) this.videoMessage = videoMessage;
    if(message instanceof ProtocolMessage protocolMessage) this.protocolMessage = protocolMessage;
    if(message instanceof ContactsArrayMessage contactsArrayMessage) this.contactsArrayMessage = contactsArrayMessage;
    if(message instanceof HighlyStructuredMessage highlyStructuredMessage) this.highlyStructuredMessage = highlyStructuredMessage;
    if(message instanceof SendPaymentMessage sendPaymentMessage) this.sendPaymentMessage = sendPaymentMessage;
    if(message instanceof LiveLocationMessage liveLocationMessage) this.liveLocationMessage = liveLocationMessage;
    if(message instanceof RequestPaymentMessage requestPaymentMessage) this.requestPaymentMessage = requestPaymentMessage;
    if(message instanceof DeclinePaymentRequestMessage declinePaymentRequestMessage) this.declinePaymentRequestMessage = declinePaymentRequestMessage;
    if(message instanceof CancelPaymentRequestMessage cancelPaymentRequestMessage) this.cancelPaymentRequestMessage = cancelPaymentRequestMessage;
    if(message instanceof TemplateMessage templateMessage) this.templateMessage = templateMessage;
    if(message instanceof StickerMessage stickerMessage) this.stickerMessage = stickerMessage;
    if(message instanceof GroupInviteMessage groupInviteMessage) this.groupInviteMessage = groupInviteMessage;
    if(message instanceof TemplateButtonReplyMessage templateButtonReplyMessage) this.templateButtonReplyMessage = templateButtonReplyMessage;
    if(message instanceof ProductMessage productMessage) this.productMessage = productMessage;
    if(message instanceof DeviceSentMessage deviceSentMessage) this.deviceSentMessage = deviceSentMessage;
    if(message instanceof DeviceSyncMessage deviceSyncMessage) this.deviceSyncMessage = deviceSyncMessage;
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
   * @return a non null Optional Message
   */
  public Optional<Message> populatedMessage(){
    if(this.senderKeyDistributionMessage != null) return Optional.of(senderKeyDistributionMessage);
    if(this.imageMessage != null) return Optional.of(imageMessage);
    if(this.contactMessage != null) return Optional.of( contactMessage);
    if(this.locationMessage != null) return Optional.of(locationMessage);
    if(this.textMessage  != null) return Optional.of(textMessage);
    if(this.documentMessage != null) return Optional.of(documentMessage);
    if(this.audioMessage != null) return Optional.of( audioMessage);
    if(this.videoMessage != null) return Optional.of(videoMessage);
    if(this.protocolMessage != null) return Optional.of(protocolMessage);
    if(this.contactsArrayMessage != null) return Optional.of(contactsArrayMessage);
    if(this.highlyStructuredMessage != null) return Optional.of(highlyStructuredMessage);
    if(this.sendPaymentMessage != null) return Optional.of(sendPaymentMessage);
    if(this.liveLocationMessage != null) return Optional.of(liveLocationMessage);
    if(this.requestPaymentMessage != null) return Optional.of(requestPaymentMessage);
    if(this.declinePaymentRequestMessage != null) return Optional.of(declinePaymentRequestMessage);
    if(this.cancelPaymentRequestMessage != null) return Optional.of(cancelPaymentRequestMessage);
    if(this.templateMessage != null) return Optional.of(templateMessage);
    if(this.stickerMessage != null) return Optional.of(stickerMessage);
    if(this.groupInviteMessage != null) return Optional.of(groupInviteMessage);
    if(this.templateButtonReplyMessage != null) return Optional.of(templateButtonReplyMessage);
    if(this.productMessage != null) return Optional.of(productMessage);
    if(this.deviceSentMessage != null) return Optional.of(deviceSentMessage);
    if(this.deviceSyncMessage != null) return Optional.of(deviceSyncMessage);
    return Optional.empty();
  }

  /**
   * Returns the first populated contextual message inside this container
   *
   * @return a non null Optional ContextualMessage
   */
  public Optional<ContextualMessage> populatedContextualMessage(){
    if(this.imageMessage != null) return Optional.of(imageMessage);
    if(this.contactMessage != null) return Optional.of( contactMessage);
    if(this.locationMessage != null) return Optional.of(locationMessage);
    if(this.textMessage  != null) return Optional.of(textMessage);
    if(this.documentMessage != null) return Optional.of(documentMessage);
    if(this.audioMessage != null) return Optional.of( audioMessage);
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
   * @return a non null enumerated type
   */
  public @NonNull MessageContainerContentType type(){
    var message = populatedMessage().orElse(null);
    if(message == null){
      return MessageContainerContentType.EMPTY;
    }

    if(message instanceof SecurityMessage){
      return MessageContainerContentType.SECURITY;
    }

    if(message instanceof ServerMessage){
      return MessageContainerContentType.SERVER;
    }

    if(message instanceof DeviceMessage){
      return MessageContainerContentType.DEVICE;
    }

    if(message instanceof BusinessMessage){
      return MessageContainerContentType.BUSINESS;
    }

    return MessageContainerContentType.STANDARD;
  }

  /**
   * Returns whether this container contains a standard message
   *
   * @return true if this container contains a standard message
   */
  public boolean isStandardMessage(){
    return type() == MessageContainerContentType.STANDARD;
  }

  /**
   * Returns whether this container contains a sever message
   *
   * @return true if this container contains a sever message
   */
  public boolean isServerMessage(){
    return type() == MessageContainerContentType.SERVER;
  }

  /**
   * The constants of this enumerated type describe the various types of messages that a {@link MessageContainer} can wrap
   */
  enum MessageContainerContentType {
    /**
     * Server message
     */
    SERVER,

    /**
     * Device message
     */
    DEVICE,

    /**
     * Security message
     */
    SECURITY,

    /**
     * Business message
     */
    BUSINESS,

    /**
     * Standard message
     */
    STANDARD,

    /**
     * No message
     */
    EMPTY
  }
}

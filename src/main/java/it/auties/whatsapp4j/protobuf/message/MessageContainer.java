package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.model.Call;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

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
 *     <li>Template messages: {@link MessageContainer#templateButtonReplyMessage}, {@link MessageContainer#templateMessage} or {@link MessageContainer#highlyStructuredMessage} are populated. These messages are used when communicating with a WhatsappBusiness account.</li>
 *     <li>User messages: Any other property may be populated, though only one at the time(probably)</li>
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
   * Text message with context.
   * For text messages with no context refer to {@link MessageContainer#textMessage}
   */
  @JsonProperty(value = "6")
  private ExtendedTextMessage extendedTextMessage;

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
   * Text message with no context.
   * For text messages with context refer to {@link MessageContainer#extendedTextMessage}
   */
  @JsonProperty(value = "1")
  private String textMessage;

  /**
   * Constructs a new MessageContainer from a message of any type
   * 
   * @param message the message that the new container should wrap
   * @param <T> the type of the message
   */
  // When Java 17 comes out this will be simplified to an elegant switch statement, for now this is what we've got
  public <T extends Message> MessageContainer(@NotNull T message){
    if(message instanceof SenderKeyDistributionMessage senderKeyDistributionMessage) this.senderKeyDistributionMessage = senderKeyDistributionMessage;
    if(message instanceof ImageMessage imageMessage) this.imageMessage = imageMessage;
    if(message instanceof ContactMessage contactMessage) this.contactMessage = contactMessage;
    if(message instanceof LocationMessage locationMessage) this.locationMessage = locationMessage;
    if(message instanceof ExtendedTextMessage extendedTextMessage) this.extendedTextMessage = extendedTextMessage;
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
  public MessageContainer(String textMessage){
    this.textMessage = textMessage;
  }
}

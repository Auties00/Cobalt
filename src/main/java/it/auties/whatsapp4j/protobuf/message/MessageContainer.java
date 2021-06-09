package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.model.Call;
import lombok.*;
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
}

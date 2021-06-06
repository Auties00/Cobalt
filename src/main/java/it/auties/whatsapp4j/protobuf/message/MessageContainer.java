package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.model.Call;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class MessageContainer {
  @JsonProperty(value = "32")
  private DeviceSyncMessage deviceSyncMessage;

  @JsonProperty(value = "31")
  private DeviceSentMessage deviceSentMessage;

  @JsonProperty(value = "30")
  private ProductMessage productMessage;

  @JsonProperty(value = "29")
  private TemplateButtonReplyMessage templateButtonReplyMessage;

  @JsonProperty(value = "28")
  private GroupInviteMessage groupInviteMessage;

  @JsonProperty(value = "26")
  private StickerMessage stickerMessage;

  @JsonProperty(value = "25")
  private TemplateMessage templateMessage;

  @JsonProperty(value = "24")
  private CancelPaymentRequestMessage cancelPaymentRequestMessage;

  @JsonProperty(value = "23")
  private DeclinePaymentRequestMessage declinePaymentRequestMessage;

  @JsonProperty(value = "22")
  private RequestPaymentMessage requestPaymentMessage;

  @JsonProperty(value = "18")
  private LiveLocationMessage liveLocationMessage;

  @JsonProperty(value = "16")
  private SendPaymentMessage sendPaymentMessage;

  @JsonProperty(value = "15")
  private SenderKeyDistributionMessage fastRatchetKeySenderKeyDistributionMessage;

  @JsonProperty(value = "14")
  private HighlyStructuredMessage highlyStructuredMessage;

  @JsonProperty(value = "13")
  private ContactsArrayMessage contactsArrayMessage;

  @JsonProperty(value = "12")
  private ProtocolMessage protocolMessage;

  @JsonProperty(value = "10")
  private Call call;

  @JsonProperty(value = "9")
  private VideoMessage videoMessage;

  @JsonProperty(value = "8")
  private AudioMessage audioMessage;

  @JsonProperty(value = "7")
  private DocumentMessage documentMessage;

  @JsonProperty(value = "6")
  private ExtendedTextMessage extendedTextMessage;

  @JsonProperty(value = "5")
  private LocationMessage locationMessage;

  @JsonProperty(value = "4")
  private ContactMessage contactMessage;

  @JsonProperty(value = "3")
  private ImageMessage imageMessage;

  @JsonProperty(value = "2")
  private SenderKeyDistributionMessage senderKeyDistributionMessage;

  @JsonProperty(value = "1")
  private String conversation;
}

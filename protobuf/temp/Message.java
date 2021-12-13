package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.message.server.SenderKeyDistributionMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Message {
  @JsonProperty(value = "44")
  @JsonPropertyDescription("PaymentInviteMessage")
  private PaymentInviteMessage paymentInviteMessage;

  @JsonProperty(value = "43")
  @JsonPropertyDescription("ButtonsResponseMessage")
  private ButtonsResponseMessage buttonsResponseMessage;

  @JsonProperty(value = "42")
  @JsonPropertyDescription("ButtonsMessage")
  private ButtonsMessage buttonsMessage;

  @JsonProperty(value = "41")
  @JsonPropertyDescription("InvoiceMessage")
  private InvoiceMessage invoiceMessage;

  @JsonProperty(value = "40")
  @JsonPropertyDescription("FutureProofMessage")
  private FutureProofMessage ephemeralMessage;

  @JsonProperty(value = "39")
  @JsonPropertyDescription("ListResponseMessage")
  private ListResponseMessage listResponseMessage;

  @JsonProperty(value = "38")
  @JsonPropertyDescription("OrderMessage")
  private OrderMessage orderMessage;

  @JsonProperty(value = "37")
  @JsonPropertyDescription("FutureProofMessage")
  private FutureProofMessage viewOnceMessage;

  @JsonProperty(value = "36")
  @JsonPropertyDescription("ListMessage")
  private ListMessage listMessage;

  @JsonProperty(value = "35")
  @JsonPropertyDescription("MessageContextInfo")
  private MessageContextInfo messageContextInfo;

  @JsonProperty(value = "31")
  @JsonPropertyDescription("DeviceSentMessage")
  private DeviceSentMessage deviceSentMessage;

  @JsonProperty(value = "30")
  @JsonPropertyDescription("ProductMessage")
  private ProductMessage productMessage;

  @JsonProperty(value = "29")
  @JsonPropertyDescription("TemplateButtonReplyMessage")
  private TemplateButtonReplyMessage templateButtonReplyMessage;

  @JsonProperty(value = "28")
  @JsonPropertyDescription("GroupInviteMessage")
  private GroupInviteMessage groupInviteMessage;

  @JsonProperty(value = "26")
  @JsonPropertyDescription("StickerMessage")
  private StickerMessage stickerMessage;

  @JsonProperty(value = "25")
  @JsonPropertyDescription("TemplateMessage")
  private TemplateMessage templateMessage;

  @JsonProperty(value = "24")
  @JsonPropertyDescription("CancelPaymentRequestMessage")
  private CancelPaymentRequestMessage cancelPaymentRequestMessage;

  @JsonProperty(value = "23")
  @JsonPropertyDescription("DeclinePaymentRequestMessage")
  private DeclinePaymentRequestMessage declinePaymentRequestMessage;

  @JsonProperty(value = "22")
  @JsonPropertyDescription("RequestPaymentMessage")
  private RequestPaymentMessage requestPaymentMessage;

  @JsonProperty(value = "18")
  @JsonPropertyDescription("LiveLocationMessage")
  private LiveLocationMessage liveLocationMessage;

  @JsonProperty(value = "16")
  @JsonPropertyDescription("SendPaymentMessage")
  private SendPaymentMessage sendPaymentMessage;

  @JsonProperty(value = "15")
  @JsonPropertyDescription("SenderKeyDistributionMessage")
  private SenderKeyDistributionMessage fastRatchetKeySenderKeyDistributionMessage;

  @JsonProperty(value = "14")
  @JsonPropertyDescription("HighlyStructuredMessage")
  private HighlyStructuredMessage highlyStructuredMessage;

  @JsonProperty(value = "13")
  @JsonPropertyDescription("ContactsArrayMessage")
  private ContactsArrayMessage contactsArrayMessage;

  @JsonProperty(value = "12")
  @JsonPropertyDescription("ProtocolMessage")
  private ProtocolMessage protocolMessage;

  @JsonProperty(value = "11")
  @JsonPropertyDescription("Chat")
  private Chat chat;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("Call")
  private Call call;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("VideoMessage")
  private VideoMessage videoMessage;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("AudioMessage")
  private AudioMessage audioMessage;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("DocumentMessage")
  private DocumentMessage documentMessage;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("ExtendedTextMessage")
  private ExtendedTextMessage extendedTextMessage;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("LocationMessage")
  private LocationMessage locationMessage;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("ContactMessage")
  private ContactMessage contactMessage;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("ImageMessage")
  private ImageMessage imageMessage;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("SenderKeyDistributionMessage")
  private SenderKeyDistributionMessage senderKeyDistributionMessage;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String conversation;
}

package it.auties.whatsapp.protobuf.beta;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Message {

  @JsonProperty(value = "44", required = false)
  @JsonPropertyDescription("PaymentInviteMessage")
  private PaymentInviteMessage paymentInviteMessage;

  @JsonProperty(value = "43", required = false)
  @JsonPropertyDescription("ButtonsResponseMessage")
  private ButtonsResponseMessage buttonsResponseMessage;

  @JsonProperty(value = "42", required = false)
  @JsonPropertyDescription("ButtonsMessage")
  private ButtonsMessage buttonsMessage;

  @JsonProperty(value = "41", required = false)
  @JsonPropertyDescription("InvoiceMessage")
  private InvoiceMessage invoiceMessage;

  @JsonProperty(value = "40", required = false)
  @JsonPropertyDescription("FutureProofMessage")
  private FutureProofMessage ephemeralMessage;

  @JsonProperty(value = "39", required = false)
  @JsonPropertyDescription("ListResponseMessage")
  private ListResponseMessage listResponseMessage;

  @JsonProperty(value = "38", required = false)
  @JsonPropertyDescription("OrderMessage")
  private OrderMessage orderMessage;

  @JsonProperty(value = "37", required = false)
  @JsonPropertyDescription("FutureProofMessage")
  private FutureProofMessage viewOnceMessage;

  @JsonProperty(value = "36", required = false)
  @JsonPropertyDescription("ListMessage")
  private ListMessage listMessage;

  @JsonProperty(value = "35", required = false)
  @JsonPropertyDescription("MessageContextInfo")
  private MessageContextInfo messageContextInfo;

  @JsonProperty(value = "31", required = false)
  @JsonPropertyDescription("DeviceSentMessage")
  private DeviceSentMessage deviceSentMessage;

  @JsonProperty(value = "30", required = false)
  @JsonPropertyDescription("ProductMessage")
  private ProductMessage productMessage;

  @JsonProperty(value = "29", required = false)
  @JsonPropertyDescription("TemplateButtonReplyMessage")
  private TemplateButtonReplyMessage templateButtonReplyMessage;

  @JsonProperty(value = "28", required = false)
  @JsonPropertyDescription("GroupInviteMessage")
  private GroupInviteMessage groupInviteMessage;

  @JsonProperty(value = "26", required = false)
  @JsonPropertyDescription("StickerMessage")
  private StickerMessage stickerMessage;

  @JsonProperty(value = "25", required = false)
  @JsonPropertyDescription("TemplateMessage")
  private TemplateMessage templateMessage;

  @JsonProperty(value = "24", required = false)
  @JsonPropertyDescription("CancelPaymentRequestMessage")
  private CancelPaymentRequestMessage cancelPaymentRequestMessage;

  @JsonProperty(value = "23", required = false)
  @JsonPropertyDescription("DeclinePaymentRequestMessage")
  private DeclinePaymentRequestMessage declinePaymentRequestMessage;

  @JsonProperty(value = "22", required = false)
  @JsonPropertyDescription("RequestPaymentMessage")
  private RequestPaymentMessage requestPaymentMessage;

  @JsonProperty(value = "18", required = false)
  @JsonPropertyDescription("LiveLocationMessage")
  private LiveLocationMessage liveLocationMessage;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("SendPaymentMessage")
  private SendPaymentMessage sendPaymentMessage;

  @JsonProperty(value = "15", required = false)
  @JsonPropertyDescription("SenderKeyDistributionMessage")
  private SenderKeyDistributionMessage fastRatchetKeySenderKeyDistributionMessage;

  @JsonProperty(value = "14", required = false)
  @JsonPropertyDescription("HighlyStructuredMessage")
  private HighlyStructuredMessage highlyStructuredMessage;

  @JsonProperty(value = "13", required = false)
  @JsonPropertyDescription("ContactsArrayMessage")
  private ContactsArrayMessage contactsArrayMessage;

  @JsonProperty(value = "12", required = false)
  @JsonPropertyDescription("ProtocolMessage")
  private ProtocolMessage protocolMessage;

  @JsonProperty(value = "11", required = false)
  @JsonPropertyDescription("Chat")
  private Chat chat;

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("Call")
  private Call call;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("VideoMessage")
  private VideoMessage videoMessage;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("AudioMessage")
  private AudioMessage audioMessage;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("DocumentMessage")
  private DocumentMessage documentMessage;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("ExtendedTextMessage")
  private ExtendedTextMessage extendedTextMessage;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("LocationMessage")
  private LocationMessage locationMessage;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("ContactMessage")
  private ContactMessage contactMessage;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("ImageMessage")
  private ImageMessage imageMessage;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("SenderKeyDistributionMessage")
  private SenderKeyDistributionMessage senderKeyDistributionMessage;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String conversation;
}

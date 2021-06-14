package it.auties.whatsapp4j.protobuf.message.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.message.model.MessageKey;
import it.auties.whatsapp4j.protobuf.message.model.PaymentMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage sent by a WhatsappBusiness account to decline a {@link RequestPaymentMessage}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor(staticName = "newDeclinePaymentRequestMessage")
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class DeclinePaymentRequestMessage implements PaymentMessage {
  /**
   * The key of the original {@link RequestPaymentMessage} that this message cancels
   */
  @JsonProperty(value = "1")
  private MessageKey key;
}

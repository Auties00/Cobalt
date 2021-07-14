package it.auties.whatsapp4j.protobuf.message.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.protobuf.message.model.MessageKey;
import it.auties.whatsapp4j.protobuf.message.model.PaymentMessage;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage that cancels a {@link RequestPaymentMessage} in a WhatsappBusiness chat.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor(staticName = "newCancelPaymentMessage")
@NoArgsConstructor
@Data
@Accessors(fluent = true)
public final class CancelPaymentRequestMessage implements PaymentMessage {
  /**
   * The key of the original {@link RequestPaymentMessage} that this message cancels
   */
  @JsonProperty(value = "1")
  private MessageKey key;
}

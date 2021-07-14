package it.auties.whatsapp4j.protobuf.message.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.protobuf.message.model.MessageContainer;
import it.auties.whatsapp4j.protobuf.message.model.MessageKey;
import it.auties.whatsapp4j.protobuf.message.model.PaymentMessage;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage sent by a WhatsappBusiness account to confirm a {@link RequestPaymentMessage}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newSendPaymentMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class SendPaymentMessage implements PaymentMessage {
  /**
   * The key of the original {@link RequestPaymentMessage} that this message confirms
   */
  @JsonProperty(value = "3")
  private MessageKey requestMessageKey;

  /**
   * The caption message, that is the message below the payment confirmation
   */
  @JsonProperty(value = "2")
  private MessageContainer noteMessage;
}

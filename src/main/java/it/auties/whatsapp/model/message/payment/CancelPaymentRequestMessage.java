package it.auties.whatsapp.model.message.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.CallInfo;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

/**
 * A model class that represents a WhatsappMessage that cancels a {@link RequestPaymentMessage}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor(staticName = "newCancelPaymentMessage")
@NoArgsConstructor
@Builder(builderMethodName = "newCancelPaymentMessage", buildMethodName = "create")
@Jacksonized
@Data
@Accessors(fluent = true)
public final class CancelPaymentRequestMessage implements PaymentMessage {
  /**
   * The key of the original {@link RequestPaymentMessage} that this message cancels
   */
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = MessageKey.class)
  private MessageKey key;
}

package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage that refers to a declined payment.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class DeclinePaymentRequestMessage implements Message {
  /**
   * No information is available regarding this field.
   * Considering that this field's parent class, {@link DeclinePaymentRequestMessage}, is used in {@link MessageContainer} which already provides a {@link MessageKey} field,
   * it makes no sense to provide another instance of the same object here.
   * Perhaps this field is used to identify the key of the the payment message that this message cancels.
   */
  @JsonProperty(value = "1")
  private MessageKey key;
}

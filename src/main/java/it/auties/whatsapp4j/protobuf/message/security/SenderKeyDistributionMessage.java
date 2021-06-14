package it.auties.whatsapp4j.protobuf.message.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.message.model.SecurityMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage sent by WhatsappWeb for security purposes.
 * Whatsapp follows the Signal Standard, for more information about this message visit <a href="https://archive.kaidan.im/libsignal-protocol-c-docs/html/struct___textsecure_____sender_key_distribution_message.html">their documentation</a>
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newSenderKeyDistributionMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class SenderKeyDistributionMessage implements SecurityMessage {
  /**
   * Axolotl sender key distribution message
   */
  @JsonProperty(value = "2")
  private byte[] axolotlSenderKeyDistributionMessage;

  /**
   * Group id
   */
  @JsonProperty(value = "1")
  private String groupId;
}

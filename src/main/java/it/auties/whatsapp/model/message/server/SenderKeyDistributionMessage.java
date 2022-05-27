package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.message.model.ServerMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BYTES;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that represents a WhatsappMessage sent by WhatsappWeb for security purposes.
 * Whatsapp follows the Signal Standard, for more information about this message visit <a href="https://archive.kaidan.im/libsignal-protocol-c-docs/html/struct___textsecure_____sender_key_distribution_message.html">their documentation</a>
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newSenderKeyDistributionMessage", buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class SenderKeyDistributionMessage implements ServerMessage {
  /**
   * The jid of the sender
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String groupId;

  /**
   * The sender key
   */
  @ProtobufProperty(index = 2, type = BYTES)
  private byte[] data;
}

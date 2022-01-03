package it.auties.whatsapp.protobuf.message.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.message.model.ServerMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

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
@Accessors(fluent = true)
public final class SenderKeyDistributionMessage implements ServerMessage {
  /**
   * The id of the sender
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String groupId;

  /**
   * The sender key
   */
  @JsonProperty("2")
  @JsonPropertyDescription("bytes")
  private byte[] data;

  /**
   * Constructs a new {@link SenderKeyDistributionMessage} using an encoded message
   *
   * @param groupId the group id
   * @param encodedMessage the encoded message
   */
  @JsonCreator
  public SenderKeyDistributionMessage(@JsonProperty("1") String groupId, @JsonProperty("2") String encodedMessage){
    this.groupId = groupId;
    this.data = encodedMessage.getBytes(StandardCharsets.UTF_8);
  }
}

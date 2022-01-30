package it.auties.whatsapp.protobuf.message.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.util.WhatsappUtils;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;

/**
 * A container for unique identifiers and metadata linked to a {@link Message} and contained in {@link MessageInfo}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@NoArgsConstructor
@Data
@Accessors(fluent = true)
@Builder(builderMethodName = "newMessageKey", buildMethodName = "create")
public class MessageKey {
  /**
   * The jid of the contact or group that sent the message.
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  @NonNull
  private ContactJid chatJid;

  /**
   * Determines whether the message was sent by you or by someone else
   */
  @JsonProperty("2")
  @JsonPropertyDescription("bool")
  private boolean fromMe;

  /**
   * The id of the message
   */
  @JsonProperty("3")
  @JsonPropertyDescription("string")
  @NonNull
  @Default
  private String id = WhatsappUtils.randomId();
}

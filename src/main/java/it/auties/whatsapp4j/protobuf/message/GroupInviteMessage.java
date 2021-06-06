package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a whatsapp group invite inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class GroupInviteMessage implements ContextualMessage {
  @JsonProperty(value = "7")
  private ContextInfo contextInfo;

  @JsonProperty(value = "6")
  private String caption;

  @JsonProperty(value = "5")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "4")
  private String groupName;

  @JsonProperty(value = "3")
  private long inviteExpiration;

  @JsonProperty(value = "2")
  private String inviteCode;

  @JsonProperty(value = "1")
  private String groupJid;
}

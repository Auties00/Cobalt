package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a whatsapp group invite inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor(staticName = "newGroupInviteMessage")
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newGroupInviteMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class GroupInviteMessage extends ContextualMessage {
  /**
   * The context info of this message
   */
  @JsonProperty(value = "7")
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info

  /**
   * The caption of this invite
   */
  @JsonProperty(value = "6")
  private String caption;

  /**
   * The thumbnail of the group that this invite regards encoded as jpeg in an array of bytes
   */
  @JsonProperty(value = "5")
  private byte[] jpegThumbnail;

  /**
   * The name of the group that this invite regards
   */
  @JsonProperty(value = "4")
  private String groupName;

  /**
   * The expiration of this invite in milliseconds since {@link java.time.Instant#EPOCH}
   */
  @JsonProperty(value = "3")
  private long inviteExpiration;

  /**
   * The invite code of this message
   */
  @JsonProperty(value = "2")
  private String inviteCode;

  /**
   * The jid of the group that this invite regards
   */
  @JsonProperty(value = "1")
  private String groupJid;
}

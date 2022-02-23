package it.auties.whatsapp.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a whatsapp group invite inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor(staticName = "newGroupInviteMessage")
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newGroupInviteMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class GroupInviteMessage extends ContextualMessage {
  /**
   * The jid of the group that this invite regards
   */
  @JsonProperty("1")
  @JsonPropertyDescription("group")
  private ContactJid groupId;

  /**
   * The invite code of this message
   */
  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String code;

  /**
   * The expiration of this invite in milliseconds since {@link java.time.Instant#EPOCH}
   */
  @JsonProperty("3")
  @JsonPropertyDescription("uint64")
  private long expiration;
  
  /**
   * The name of the group that this invite regards
   */
  @JsonProperty("4")
  @JsonPropertyDescription("string")
  private String groupName;

  /**
   * The thumbnail of the group that this invite regards encoded as jpeg in an array of bytes
   */
  @JsonProperty("5")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnail;

  /**
   * The caption of this invite
   */
  @JsonProperty("6")
  @JsonPropertyDescription("string")
  private String caption;
  
  /**
   * The context info of this message
   */
  @JsonProperty("7")
  @JsonPropertyDescription("context")
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info
}

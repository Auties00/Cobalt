package it.auties.whatsapp4j.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.message.model.ContextualMessage;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a contact inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(buildMethodName = "create")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
public final class ContactMessage extends ContextualMessage {
  /**
   * The info about the contact that this message wraps encoded as a vcard
   */
  @JsonProperty(value = "16")
  private String vcard;

  /**
   * The name of the contact that this message wraps
   */
  @JsonProperty(value = "1")
  private String displayName;

  /**
   * Constructs a new builder to create a ContactMessage.
   * The result can be later sent using {@link WhatsappAPI#sendMessage(it.auties.whatsapp4j.protobuf.info.MessageInfo)}
   *
   * @param displayName the display name of the contact that the new message wraps
   * @param vcard       the info about the contact that the new message wraps encoded as a vcard
   * @param contextInfo the context info that the new message wraps
   *
   * @return a non null new message
   */
  @Builder(builderClassName = "NewContactMessageBuilder", builderMethodName = "newContactMessage", buildMethodName = "create")
  private static ContactMessage builder(String displayName, String vcard, ContextInfo contextInfo) {
    return ContactMessage.builder()
            .vcard(vcard)
            .displayName(displayName)
            .contextInfo(contextInfo)
            .create();
  }

  private static ContactMessageBuilder<?, ?> builder() {
    return new ContactMessageBuilderImpl();
  }
}

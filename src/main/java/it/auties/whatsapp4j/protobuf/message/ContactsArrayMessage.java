package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a list of contacts inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newContactsArrayMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class ContactsArrayMessage extends ContextualMessage {
  /**
   * A list of {@link ContactMessage} that this message wraps
   */
  @JsonProperty(value = "2")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ContactMessage> contacts;

  /**
   * The name of the contact the first contact that this message wraps
   */
  @JsonProperty(value = "1")
  private String displayName;
}

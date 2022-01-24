package it.auties.whatsapp.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a list of contacts inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newContactsArrayMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class ContactsArrayMessage extends ContextualMessage {
  /**
   * The name of the contact the first contact that this message wraps
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String name;

  /**
   * A list of {@link ContactMessage} that this message wraps
   */
  @JsonProperty("2")
  @JsonPropertyDescription("contact")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ContactMessage> contacts;
}

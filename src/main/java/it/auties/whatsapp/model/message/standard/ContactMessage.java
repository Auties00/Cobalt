package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a contact inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(builderMethodName = "newContactMessage", buildMethodName = "create")
@Jacksonized
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
public final class ContactMessage extends ContextualMessage {
  /**
   * The name of the contact that this message wraps
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String name;

  /**
   * The info about the contact that this message wraps encoded as a vcard
   */
  @ProtobufProperty(index = 16, type = STRING)
  private String vcard;
}

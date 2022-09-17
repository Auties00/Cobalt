package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactCard;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that represents a message holding a contact inside
 */
@AllArgsConstructor(staticName = "newContactMessage")
@NoArgsConstructor
@SuperBuilder(builderMethodName = "newContactMessageBuilder")
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
    @ProtobufProperty(index = 16, type = STRING, concreteType = ContactCard.class, requiresConversion = true)
    private ContactCard vcard;

    @Override
    public MessageType type() {
        return MessageType.CONTACT;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}

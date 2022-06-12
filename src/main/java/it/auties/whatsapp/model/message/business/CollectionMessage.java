package it.auties.whatsapp.model.message.business;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.model.BusinessMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.INT32;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that represents a message holding a product inside
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class CollectionMessage implements BusinessMessage {
    @ProtobufProperty(index = 1, type = STRING, concreteType = ContactJid.class, requiresConversion = true)
    private ContactJid contact;

    @ProtobufProperty(index = 2, type = STRING)
    private String id;

    @ProtobufProperty(index = 3, type = INT32)
    private int messageVersion;
}

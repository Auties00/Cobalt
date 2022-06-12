package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that represents a message holding a list of contacts inside
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newContactsArrayMessage", buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class ContactsArrayMessage extends ContextualMessage {
    /**
     * The name of the contact the first contact that this message wraps
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String name;

    /**
     * A list of {@link ContactMessage} that this message wraps
     */
    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ContactMessage.class, repeated = true)
    private List<ContactMessage> contacts;

    public static abstract class ContactsArrayMessageBuilder<C extends ContactsArrayMessage, B extends ContactsArrayMessageBuilder<C, B>>
            extends ContextualMessageBuilder<C, B> {
        public B contacts(List<ContactMessage> contacts) {
            if (this.contacts == null)
                this.contacts = new ArrayList<>();
            this.contacts.addAll(contacts);
            return self();
        }
    }
}

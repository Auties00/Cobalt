package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model clas that represents a new contact push name
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class ContactAction implements Action {
    /**
     * The full name of the contact
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String fullName;

    /**
     * The first name of the contact
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String firstName;

    /**
     * The lid jid
     */
    @ProtobufProperty(index = 3, name = "lidJid", type = STRING)
    private String lidJid;

    /**
     * Returns the name of this contact
     *
     * @return a non-null String
     */
    public String name() {
        return Objects.requireNonNullElse(fullName, firstName);
    }

    /**
     * Returns the full name of this contact
     *
     * @return an optional
     */
    public Optional<String> fullName() {
        return Optional.ofNullable(fullName);
    }

    /**
     * Returns the first name of this contact
     *
     * @return an optional
     */
    public Optional<String> firstName() {
        return Optional.ofNullable(firstName);
    }

    /**
     * Returns the lid jid of this contact
     *
     * @return an optional
     */
    public Optional<ContactJid> lidJid() {
        return Optional.ofNullable(lidJid).map(ContactJid::of);
    }

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "contact";
    }
}
package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model clas that represents a new contact push name
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(access = AccessLevel.PROTECTED)
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
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "contact";
    }
}

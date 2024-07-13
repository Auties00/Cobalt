package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;

import java.util.Optional;

/**
 * A model clas that represents a new contact push name
 */
@ProtobufMessage(name = "SyncActionValue.ContactAction")
public record ContactAction(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Optional<String> fullName,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        Optional<String> firstName,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        Optional<String> lidJid
) implements Action {
    /**
     * Returns the name of this contact
     *
     * @return an optional
     */
    public Optional<String> name() {
        return fullName.or(this::firstName);
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

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int actionVersion() {
        return 2;
    }

    /**
     * The type of this action
     *
     * @return a non-null string
     */
    @Override
    public PatchType actionType() {
        return null;
    }
}
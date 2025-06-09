package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Objects;
import java.util.Optional;

/**
 * A model clas that represents a new contact push name
 */
@ProtobufMessage(name = "SyncActionValue.ContactAction")
public final class ContactAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String fullName;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String firstName;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final Jid lid;

    ContactAction(String fullName, String firstName, Jid lid) {
        this.fullName = fullName;
        this.firstName = firstName;
        this.lid = lid;
    }

    @Override
    public String indexName() {
        return "contact";
    }

    @Override
    public int actionVersion() {
        return 2;
    }

    public Optional<String> name() {
        return Optional.ofNullable(fullName)
                .or(this::firstName);
    }

    public Optional<String> fullName() {
        return Optional.ofNullable(fullName);
    }

    public Optional<String> firstName() {
        return Optional.ofNullable(firstName);
    }

    public Optional<Jid> lid() {
        return Optional.ofNullable(lid);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ContactAction that
                && Objects.equals(fullName, that.fullName)
                && Objects.equals(firstName, that.firstName)
                && Objects.equals(lid, that.lid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName, firstName, lid);
    }

    @Override
    public String toString() {
        return "ContactAction[" +
                "fullName=" + fullName + ", " +
                "firstName=" + firstName + ", " +
                "lidJid=" + lid + ']';
    }
}
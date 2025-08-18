package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model class that contains the main Whatsapp version being used
 */
@ProtobufMessage(name = "SyncActionValue.PrimaryVersionAction")
public final class PrimaryVersionAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String version;

    PrimaryVersionAction(String version) {
        this.version = version;
    }

    @Override
    public String indexName() {
        return "primary_version";
    }

    @Override
    public int actionVersion() {
        return 7;
    }

    public Optional<String> version() {
        return Optional.ofNullable(version);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PrimaryVersionAction that
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(version);
    }

    @Override
    public String toString() {
        return "PrimaryVersionAction[" +
                "version=" + version + ']';
    }
}

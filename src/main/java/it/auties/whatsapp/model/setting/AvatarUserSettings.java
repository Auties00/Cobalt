package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

public record AvatarUserSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String facebookId,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String password
) implements Setting {
    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }
}
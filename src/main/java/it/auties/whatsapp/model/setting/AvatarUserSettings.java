package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessageName("AvatarUserSettings")
public record AvatarUserSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String facebookId,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String password
) implements Setting {
    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }
}
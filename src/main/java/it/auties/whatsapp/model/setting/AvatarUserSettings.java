package it.auties.whatsapp.model.setting;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("AvatarUserSettings")
public final class AvatarUserSettings implements Setting {
    @ProtobufProperty(index = 1, name = "fbid", type = ProtobufType.STRING)
    private String facebookId;

    @ProtobufProperty(index = 2, name = "password", type = ProtobufType.STRING)
    private String password;

    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }
}
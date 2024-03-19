package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessageName("SyncActionValue.PushNameSetting")
public record PushNameSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String name
) implements Setting {
    @Override
    public String indexName() {
        return "setting_pushName";
    }
}

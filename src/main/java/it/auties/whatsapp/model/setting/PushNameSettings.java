package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "SyncActionValue.PushNameSetting")
public record PushNameSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String name
) implements Setting {
    @Override
    public int settingVersion() {
        return 1;
    }

    @Override
    public String indexName() {
        return "setting_pushName";
    }
}

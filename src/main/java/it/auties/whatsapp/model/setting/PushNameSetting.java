package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

public record PushNameSetting(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String name
) implements Setting {
    @Override
    public String indexName() {
        return "setting_pushName";
    }
}

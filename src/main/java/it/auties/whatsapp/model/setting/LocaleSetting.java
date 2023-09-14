package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

public record LocaleSetting(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String locale
) implements Setting {
    @Override
    public String indexName() {
        return "setting_locale";
    }
}

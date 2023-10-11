package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessageName("LocaleSetting")
public record LocaleSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String locale
) implements Setting {
    @Override
    public String indexName() {
        return "setting_locale";
    }
}

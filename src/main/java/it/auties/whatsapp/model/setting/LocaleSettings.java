package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "LocaleSetting")
public record LocaleSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String locale
) implements Setting {
    @Override
    public int settingVersion() {
        return 3;
    }

    @Override
    public String indexName() {
        return "setting_locale";
    }
}

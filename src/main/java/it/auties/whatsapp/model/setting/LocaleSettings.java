package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage(name = "LocaleSetting")
public final class LocaleSettings implements Setting {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String locale;

    LocaleSettings(String locale) {
        this.locale = Objects.requireNonNull(locale, "locale cannot be null");
    }

    public String locale() {
        return locale;
    }

    @Override
    public int settingVersion() {
        return 3;
    }

    @Override
    public String indexName() {
        return "setting_locale";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LocaleSettings that
                && Objects.equals(locale, that.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locale);
    }

    @Override
    public String toString() {
        return "LocaleSettings[" +
                "locale=" + locale +
                ']';
    }
}
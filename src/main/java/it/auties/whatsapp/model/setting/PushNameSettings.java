package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage(name = "SyncActionValue.PushNameSetting")
public final class PushNameSettings implements Setting {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    PushNameSettings(String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    public String name() {
        return name;
    }

    @Override
    public int settingVersion() {
        return 1;
    }

    @Override
    public String indexName() {
        return "setting_pushName";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PushNameSettings that
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "PushNameSettings[" +
                "name=" + name +
                ']';
    }
}
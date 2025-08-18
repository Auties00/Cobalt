package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

@ProtobufMessage(name = "AvatarUserSetting")
public record EphemeralSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.SFIXED32)
        int duration,
        @ProtobufProperty(index = 2, type = ProtobufType.SFIXED64)
        long timestampSeconds
) implements Setting {
    /**
     * Returns when this setting was toggled
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    @Override
    public int settingVersion() {
        return -1;
    }

    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }
}

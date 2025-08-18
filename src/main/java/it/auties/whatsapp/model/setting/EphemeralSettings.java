package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@ProtobufMessage(name = "AvatarUserSetting")
public final class EphemeralSettings implements Setting {
    @ProtobufProperty(index = 1, type = ProtobufType.SFIXED32)
    final int duration;

    @ProtobufProperty(index = 2, type = ProtobufType.SFIXED64)
    final long timestampSeconds;

    EphemeralSettings(int duration, long timestampSeconds) {
        this.duration = duration;
        this.timestampSeconds = timestampSeconds;
    }

    /**
     * Returns when this setting was toggled
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    public int duration() {
        return duration;
    }

    public long timestampSeconds() {
        return timestampSeconds;
    }

    @Override
    public int settingVersion() {
        return -1;
    }

    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EphemeralSettings that
                && Objects.equals(duration, that.duration)
                && Objects.equals(timestampSeconds, that.timestampSeconds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration, timestampSeconds);
    }

    @Override
    public String toString() {
        return "EphemeralSettings[" +
                "duration=" + duration + ", " +
                "timestampSeconds=" + timestampSeconds + ']';
    }
}
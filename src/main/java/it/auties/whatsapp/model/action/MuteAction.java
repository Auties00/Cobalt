package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model clas that represents a new mute status for a chat
 */
@ProtobufMessage(name = "SyncActionValue.MuteAction")
public final class MuteAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean muted;

    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    final long muteEndTimestampSeconds;

    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean autoMuted;

    MuteAction(boolean muted, long muteEndTimestampSeconds, boolean autoMuted) {
        this.muted = muted;
        this.muteEndTimestampSeconds = muteEndTimestampSeconds;
        this.autoMuted = autoMuted;
    }

    @Override
    public String indexName() {
        return "mute";
    }

    @Override
    public int actionVersion() {
        return 2;
    }

    public boolean muted() {
        return muted;
    }

    public long muteEndTimestampSeconds() {
        return muteEndTimestampSeconds;
    }

    public Optional<ZonedDateTime> muteEndTimestamp() {
        return Clock.parseSeconds(muteEndTimestampSeconds);
    }

    public boolean autoMuted() {
        return autoMuted;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MuteAction that
                && muted == that.muted
                && muteEndTimestampSeconds == that.muteEndTimestampSeconds
                && autoMuted == that.autoMuted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(muted, muteEndTimestampSeconds, autoMuted);
    }

    @Override
    public String toString() {
        return "MuteAction[" +
                "muted=" + muted + ", " +
                "muteEndTimestampSeconds=" + muteEndTimestampSeconds + ", " +
                "autoMuted=" + autoMuted + ']';
    }
}
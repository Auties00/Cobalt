package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.util.Clock;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.INT64;

/**
 * A model clas that represents a new mute status for a chat
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder(access = AccessLevel.PROTECTED)
@Jacksonized
@Accessors(fluent = true)
public final class MuteAction implements Action {
    /**
     * Whether this action marks the chat as muted
     */
    @ProtobufProperty(index = 1, type = BOOL)
    private boolean muted;

    /**
     * The timestamp of the of this mute
     */
    @ProtobufProperty(index = 2, type = INT64)
    private Long muteEndTimestampInSeconds;

    /**
     * Auto mute
     */
    @ProtobufProperty(index = 3, name = "autoMuted", type = BOOL)
    private boolean autoMuted;

    /**
     * Returns when the mute ends
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> muteEnd() {
        return Clock.parseSeconds(muteEndTimestampInSeconds);
    }

    /**
     * Returns when the mute ends in seconds
     *
     * @return a long
     */
    public long muteEndTimestampInSeconds() {
        return muteEndTimestampInSeconds == null ? 0 : muteEndTimestampInSeconds;
    }

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "mute";
    }
}
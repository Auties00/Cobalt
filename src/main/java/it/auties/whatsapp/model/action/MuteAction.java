package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A model clas that represents a new mute status for a chat
 */
@ProtobufMessage(name = "SyncActionValue.MuteAction")
public record MuteAction(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean muted,
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        OptionalLong muteEndTimestampSeconds,
        @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
        boolean autoMuted
) implements Action {
    /**
     * Returns when the mute ends
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> muteEndTimestamp() {
        return Clock.parseSeconds(muteEndTimestampSeconds.orElse(0));
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

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int actionVersion() {
        return 2;
    }

    /**
     * The type of this action
     *
     * @return a non-null string
     */
    @Override
    public PatchType actionType() {
        return null;
    }
}
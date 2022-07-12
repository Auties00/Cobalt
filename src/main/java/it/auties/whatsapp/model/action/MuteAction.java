package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BOOLEAN;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.INT64;

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
    @ProtobufProperty(index = 1, type = BOOLEAN)
    private boolean muted;

    /**
     * The timestamp of the of this mute
     */
    @ProtobufProperty(index = 2, type = INT64)
    private Long muteEndTimestamp;

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

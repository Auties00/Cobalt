package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BOOL;

/**
 * A model clas that represents a new pin status for a chat
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class PinAction implements Action {
    /**
     * Whether this action marks the chat as pinned
     */
    @ProtobufProperty(index = 1, type = BOOL)
    private boolean pinned;

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "pin_v1";
    }
}

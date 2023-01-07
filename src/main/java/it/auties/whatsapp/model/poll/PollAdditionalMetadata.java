package it.auties.whatsapp.model.poll;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that represents additional metadata about a {@link it.auties.whatsapp.model.message.standard.PollCreationMessage}
 * Not currently used, so it's package private
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PollAdditionalMetadata")
class PollAdditionalMetadata
        implements ProtobufMessage {
    /**
     * Whether the poll was invalidated
     */
    @ProtobufProperty(index = 1, name = "pollInvalidated", type = ProtobufType.BOOL)
    private boolean pollInvalidated;
}
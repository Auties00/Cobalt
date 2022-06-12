package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.sync.ActionMessageRangeSync;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class DeleteChatAction implements Action {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = ActionMessageRangeSync.class)
    private ActionMessageRangeSync messageRange;

    @Override
    public String indexName() {
        return "deleteChat";
    }
}

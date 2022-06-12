package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class LabelEditAction implements Action {
    @ProtobufProperty(index = 1, type = STRING)
    private String name;

    @ProtobufProperty(index = 2, type = INT32)
    private int color;

    @ProtobufProperty(index = 3, type = INT32)
    private int predefinedId;

    @ProtobufProperty(index = 4, type = BOOLEAN)
    private boolean deleted;

    @Override
    public String indexName() {
        return "label_edit";
    }
}

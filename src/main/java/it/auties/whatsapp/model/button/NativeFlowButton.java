package it.auties.whatsapp.model.button;

import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class NativeFlowButton {
    @ProtobufProperty(index = 1, type = STRING)
    private String name;

    @ProtobufProperty(index = 2, type = STRING)
    private String buttonParamsJson;
}

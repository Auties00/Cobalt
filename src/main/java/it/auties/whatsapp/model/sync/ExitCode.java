package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;
import static it.auties.protobuf.base.ProtobufType.UINT64;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ExitCode implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = UINT64)
    private Long code;

    @ProtobufProperty(index = 2, type = STRING)
    private String text;
}

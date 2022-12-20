package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BYTES;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ClientFinish
        implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] staticText;

    @ProtobufProperty(index = 2, type = BYTES)
    private byte[] payload;
}

package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BYTES;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ClientHello implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] ephemeral;

    @ProtobufProperty(index = 2, type = BYTES)
    private byte[] staticText;

    @ProtobufProperty(index = 3, type = BYTES)
    private byte[] payload;

    public ClientHello(byte @NonNull [] ephemeral) {
        this.ephemeral = ephemeral;
    }
}

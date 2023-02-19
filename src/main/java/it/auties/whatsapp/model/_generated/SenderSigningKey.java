package it.auties.whatsapp.model._generated;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BYTES;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("SenderSigningKey")
public class SenderSigningKey implements ProtobufMessage {
    @ProtobufProperty(index = 1, name = "_public", type = BYTES)
    private byte[] _public;

    @ProtobufProperty(index = 2, name = "_private", type = BYTES)
    private byte[] _private;
}
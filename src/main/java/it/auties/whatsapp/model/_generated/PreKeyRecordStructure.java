package it.auties.whatsapp.model._generated;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.UINT32;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("PreKeyRecordStructure")
public class PreKeyRecordStructure implements ProtobufMessage {
    @ProtobufProperty(index = 1, name = "id", type = UINT32)
    private Integer id;

    @ProtobufProperty(index = 2, name = "publicKey", type = BYTES)
    private byte[] publicKey;

    @ProtobufProperty(index = 3, name = "privateKey", type = BYTES)
    private byte[] privateKey;
}
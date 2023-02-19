package it.auties.whatsapp.model._generated;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("PendingPreKey")
public class PendingPreKey implements ProtobufMessage {
    @ProtobufProperty(index = 1, name = "preKeyId", type = UINT32)
    private Integer preKeyId;

    @ProtobufProperty(index = 3, name = "signedPreKeyId", type = INT32)
    private Integer signedPreKeyId;

    @ProtobufProperty(index = 2, name = "baseKey", type = BYTES)
    private byte[] baseKey;
}
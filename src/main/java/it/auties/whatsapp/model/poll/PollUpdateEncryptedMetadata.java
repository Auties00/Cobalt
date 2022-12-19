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

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PollEncValue")
public class PollUpdateEncryptedMetadata implements ProtobufMessage {
    @ProtobufProperty(index = 1, name = "encPayload", type = ProtobufType.BYTES)
    private byte[] payload;

    @ProtobufProperty(index = 2, name = "encIv", type = ProtobufType.BYTES)
    private byte[] iv;
}

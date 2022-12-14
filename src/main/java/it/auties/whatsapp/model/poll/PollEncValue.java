package it.auties.whatsapp.model.poll;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("PollEncValue")
public class PollEncValue implements ProtobufMessage {
    @ProtobufProperty(index = 1, name = "encPayload", type = ProtobufType.BYTES)
    private byte[] encPayload;

    @ProtobufProperty(index = 2, name = "encIv", type = ProtobufType.BYTES)
    private byte[] encIv;
}

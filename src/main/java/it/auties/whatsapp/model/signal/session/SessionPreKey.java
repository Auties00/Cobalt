package it.auties.whatsapp.model.signal.session;


import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage
public record SessionPreKey(
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        Integer preKeyId,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] baseKey,
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        int signedKeyId
) {

}
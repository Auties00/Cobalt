package it.auties.whatsapp.model.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;


@ProtobufMessage(name = "PaymentBackground.MediaData")
public record PaymentMediaData(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] mediaKey,
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        long mediaKeyTimestamp,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] mediaSha256,
        @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
        byte[] mediaEncryptedSha256,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String mediaDirectPath
) {

}

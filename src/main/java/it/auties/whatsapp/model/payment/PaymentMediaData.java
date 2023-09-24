package it.auties.whatsapp.model.payment;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;


@ProtobufMessageName("PaymentBackground.MediaData")
public record PaymentMediaData(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte @NonNull [] mediaKey,
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        long mediaKeyTimestamp,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte @NonNull [] mediaSha256,
        @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
        byte @NonNull [] mediaEncryptedSha256,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        @NonNull
        String mediaDirectPath
) implements ProtobufMessage {

}

package it.auties.whatsapp.model.payment;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

@ProtobufMessage(name = "PaymentBackground")
public record PaymentBackground(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String id,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
        long mediaSize,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
        int width,
        @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
        int height,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String mimetype,
        @ProtobufProperty(index = 6, type = ProtobufType.FIXED32)
        int placeholderArgb,
        @ProtobufProperty(index = 7, type = ProtobufType.FIXED32)
        int textArgb,
        @ProtobufProperty(index = 8, type = ProtobufType.FIXED32)
        int subtextArgb,
        @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
        Optional<PaymentMediaData> mediaData,
        @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
        PaymentBackgroundType type
) {

    @ProtobufEnum
    public enum PaymentBackgroundType {
        UNKNOWN(0),
        DEFAULT(1);

        final int index;

        PaymentBackgroundType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}
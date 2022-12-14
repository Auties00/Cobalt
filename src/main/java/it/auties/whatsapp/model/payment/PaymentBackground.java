package it.auties.whatsapp.model.payment;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.*;
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class PaymentBackground implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING)
    private String id;

    @ProtobufProperty(index = 2, type = UINT64)
    private long mediaSize;

    @ProtobufProperty(index = 3, type = UINT32)
    private int width;

    @ProtobufProperty(index = 4, type = UINT32)
    private int height;

    @ProtobufProperty(index = 5, type = STRING)
    private String mimetype;

    @ProtobufProperty(index = 6, type = FIXED32)
    private int placeholderArgb;

    @ProtobufProperty(index = 7, type = FIXED32)
    private int textArgb;

    @ProtobufProperty(index = 8, type = FIXED32)
    private int subtextArgb;

    @ProtobufProperty(index = 9, type = MESSAGE, implementation = PaymentMediaData.class)
    private PaymentMediaData mediaData;

    @ProtobufProperty(index = 10, type = MESSAGE, implementation = PaymentBackground.PaymentBackgroundType.class)
    private PaymentBackgroundType type;

    @AllArgsConstructor
    @Accessors(fluent = true)
    @ProtobufName("Type")
    public enum PaymentBackgroundType {

        UNKNOWN(0),
        DEFAULT(1);
        @Getter
        private final int index;

        public static PaymentBackgroundType of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }
}
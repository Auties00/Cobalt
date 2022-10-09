package it.auties.whatsapp.model.payment;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class PaymentMediaData implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] mediaKey;

    @ProtobufProperty(index = 2, type = INT64)
    private long mediaKeyTimestamp;

    @ProtobufProperty(index = 3, type = BYTES)
    private byte[] mediaSha256;

    @ProtobufProperty(index = 4, type = BYTES)
    private byte[] mediaEncryptedSha256;

    @ProtobufProperty(index = 5, type = STRING)
    private String mediaDirectPath;
}

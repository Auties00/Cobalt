package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

import static it.auties.protobuf.base.ProtobufType.*;

@AllArgsConstructor
@NoArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class DeviceListMetadata implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] senderKeyHash;

    @ProtobufProperty(index = 2, type = UINT64)
    private Long senderTimestamp;

    @ProtobufProperty(index = 3, type = UINT32, repeated = true, packed = true)
    private List<Integer> senderKeyIndexes;

    @ProtobufProperty(index = 8, type = BYTES)
    private byte[] recipientKeyHash;

    @ProtobufProperty(index = 9, type = UINT64)
    private Long recipientTimestamp;

    @ProtobufProperty(index = 10, type = UINT32, repeated = true, packed = true)
    private List<Integer> recipientKeyIndexes;
}

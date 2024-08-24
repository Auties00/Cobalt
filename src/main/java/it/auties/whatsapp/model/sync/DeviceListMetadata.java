package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import java.util.List;

import static it.auties.protobuf.model.ProtobufType.*;

@ProtobufMessage(name = "DeviceListMetadata")
public record DeviceListMetadata(@ProtobufProperty(index = 1, type = BYTES) byte[] senderKeyHash,
                                 @ProtobufProperty(index = 2, type = UINT64) Long senderTimestamp,
                                 @ProtobufProperty(index = 3, type = UINT32, packed = true) List<Integer> senderKeyIndexes,
                                 @ProtobufProperty(index = 8, type = BYTES) byte[] recipientKeyHash,
                                 @ProtobufProperty(index = 9, type = UINT64) Long recipientTimestamp,
                                 @ProtobufProperty(index = 10, type = UINT32, packed = true) List<Integer> recipientKeyIndexes) {
}

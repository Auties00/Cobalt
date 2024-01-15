package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessageName("ADVDeviceIdentity")
public record DeviceIdentity(@ProtobufProperty(index = 1, type = ProtobufType.UINT32)
                             int rawId,
                             @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
                             long timestamp,
                             @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
                             int keyIndex) implements ProtobufMessage {
}

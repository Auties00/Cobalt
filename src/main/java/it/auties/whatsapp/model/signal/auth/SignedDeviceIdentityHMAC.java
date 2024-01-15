package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessageName("ADVSignedDeviceIdentityHMAC")
public record SignedDeviceIdentityHMAC(@ProtobufProperty(index = 1, type = ProtobufType.BYTES) byte[] details,
                                       @ProtobufProperty(index = 2, type = ProtobufType.BYTES) byte[] hmac) implements ProtobufMessage {
}

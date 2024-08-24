package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "ADVSignedDeviceIdentity")
public record SignedDeviceIdentity(@ProtobufProperty(index = 1, type = ProtobufType.BYTES) byte[] details,
                                   @ProtobufProperty(index = 2, type = ProtobufType.BYTES) byte[] accountSignatureKey,
                                   @ProtobufProperty(index = 3, type = ProtobufType.BYTES) byte[] accountSignature,
                                   @ProtobufProperty(index = 4, type = ProtobufType.BYTES) byte[] deviceSignature) {
}

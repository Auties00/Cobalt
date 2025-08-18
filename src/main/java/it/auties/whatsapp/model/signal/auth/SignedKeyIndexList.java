package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "ADVSignedKeyIndexList")
public record SignedKeyIndexList(@ProtobufProperty(index = 1, type = ProtobufType.BYTES) byte[] details,
                                 @ProtobufProperty(index = 2, type = ProtobufType.BYTES) byte[] accountSignature) {
}

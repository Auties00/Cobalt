package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents a business certificate
 */
@ProtobufMessageName("VerifiedNameCertificate")
public record BusinessVerifiedNameCertificate(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] encodedDetails,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] signature,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] serverSignature
) implements ProtobufMessage {
    public BusinessVerifiedNameDetails details() {
        return BusinessVerifiedNameDetailsSpec.decode(encodedDetails);
    }
}

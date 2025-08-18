package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that holds a payload about a business account.
 */
@ProtobufMessage(name = "BizAccountPayload")
public record BusinessAccountPayload(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        BusinessVerifiedNameCertificate certificate,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] info
) {

}
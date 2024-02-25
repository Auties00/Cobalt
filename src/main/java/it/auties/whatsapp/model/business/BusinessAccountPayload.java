package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that holds a payload about a business account.
 */
@ProtobufMessageName("BizAccountPayload")
public record BusinessAccountPayload(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        BusinessVerifiedNameCertificate certificate,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] info
) implements ProtobufMessage {

}
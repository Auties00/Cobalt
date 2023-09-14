package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.BusinessAccountInfo;

/**
 * A model class that holds a payload about a business account.
 */
public record BusinessAccountPayload(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        BusinessVerifiedNameCertificate certificate,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        BusinessAccountInfo info
) implements ProtobufMessage {

}
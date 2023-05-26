package it.auties.whatsapp.model.business;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.info.BusinessAccountInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

/**
 * A model class that holds a payload about a business account.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("BizAccountPayload")
public class BusinessAccountPayload implements ProtobufMessage {
    /**
     * The certificate of this account
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = BusinessVerifiedNameCertificate.class)
    private BusinessVerifiedNameCertificate certificate;

    /**
     * The info about this account
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = BusinessAccountInfo.class)
    private BusinessAccountInfo info;
}
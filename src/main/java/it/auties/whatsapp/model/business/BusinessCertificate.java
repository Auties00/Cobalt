package it.auties.whatsapp.model.business;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BYTES;

/**
 * A model class that represents a business certificate
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class BusinessCertificate implements ProtobufMessage {
    /**
     * The details of this certificate
     */
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] details;

    /**
     * The signature of this certificate
     */
    @ProtobufProperty(index = 2, type = BYTES)
    private byte[] signature;

    /**
     * The server signature of this certificate
     */
    @ProtobufProperty(index = 3, type = BYTES)
    private byte[] serverSignature;
}

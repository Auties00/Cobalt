package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a business certificate
 */
@ProtobufMessage(name = "VerifiedNameCertificate")
public final class BusinessVerifiedNameCertificate {
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    final byte[] encodedDetails;
    
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    final byte[] signature;
    
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final byte[] serverSignature;

    BusinessVerifiedNameCertificate(byte[] encodedDetails, byte[] signature, byte[] serverSignature) {
        this.encodedDetails = encodedDetails;
        this.signature = signature;
        this.serverSignature = serverSignature;
    }

    public Optional<byte[]> encodedDetails() {
        return Optional.ofNullable(encodedDetails);
    }

    public Optional<byte[]> signature() {
        return Optional.ofNullable(signature);
    }

    public Optional<byte[]> serverSignature() {
        return Optional.ofNullable(serverSignature);
    }

    public BusinessVerifiedNameDetails details() {
        return BusinessVerifiedNameDetailsSpec.decode(encodedDetails);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessVerifiedNameCertificate that
                && Arrays.equals(encodedDetails, that.encodedDetails)
                && Arrays.equals(signature, that.signature)
                && Arrays.equals(serverSignature, that.serverSignature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(encodedDetails), Arrays.hashCode(signature), Arrays.hashCode(serverSignature));
    }

    @Override
    public String toString() {
        return "BusinessVerifiedNameCertificate[" +
                "encodedDetails=" + Arrays.toString(encodedDetails) +
                ", signature=" + Arrays.toString(signature) +
                ", serverSignature=" + Arrays.toString(serverSignature) +
                ']';
    }
}

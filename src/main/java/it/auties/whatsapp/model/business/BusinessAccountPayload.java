package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that holds a payload about a business account.
 */
@ProtobufMessage(name = "BizAccountPayload")
public final class BusinessAccountPayload {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final BusinessVerifiedNameCertificate certificate;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    final byte[] info;

    BusinessAccountPayload(BusinessVerifiedNameCertificate certificate, byte[] info) {
        this.certificate = certificate;
        this.info = info;
    }

    public Optional<BusinessVerifiedNameCertificate> certificate() {
        return Optional.ofNullable(certificate);
    }

    public Optional<byte[]> info() {
        return Optional.ofNullable(info);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessAccountPayload that
                && Objects.equals(certificate, that.certificate)
                && Arrays.equals(info, that.info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(certificate, Arrays.hashCode(info));
    }

    @Override
    public String toString() {
        return "BusinessAccountPayload{" +
                "certificate=" + certificate +
                ", info=" + Arrays.toString(info) +
                '}';
    }
}
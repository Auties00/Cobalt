package com.github.auties00.cobalt.wire.linked.federated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Reply payload of the federated-identity ("Waffle") enterprise-customer
 * generation RPC.
 *
 * <p>This RPC is the final step of the federated-identity → enterprise
 * enrolment flow. After the user has consented to a specific disclosure (in
 * the disclosure id, version, language, and locale carried by the request),
 * the Meta bridge mints a "WhatsApp Enterprise Authenticated Customer"
 * (WAEntAC) record that ties the linked Meta identity to an enterprise
 * customer entry. Subsequent enterprise surfaces - hosted business account,
 * catalog, and so on - address the linked enterprise via the bootstrapped
 * record this reply confirms.
 *
 * <p>The reply body is opaque to the WhatsApp server: the relay returns it
 * inside a {@link FederatedRsaEncryption} envelope sealed against the
 * symmetric key the client embedded in the request.
 */
@ProtobufMessage(name = "FederatedEnterpriseCustomer")
public final class FederatedEnterpriseCustomer {
    /**
     * RSA-2048 envelope carrying the bootstrapped enterprise-customer record.
     * Always present alongside a successful response.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    FederatedRsaEncryption encryption;

    /**
     * Constructs a new {@code FederatedEnterpriseCustomer} reply.
     *
     * @param encryption the relay-returned encryption envelope, or
     *                   {@code null} when absent
     */
    FederatedEnterpriseCustomer(FederatedRsaEncryption encryption) {
        this.encryption = encryption;
    }

    /**
     * Returns the relay-returned encryption envelope carrying the
     * enterprise-customer record.
     *
     * @return an {@link Optional} containing the envelope, or empty
     */
    public Optional<FederatedRsaEncryption> encryption() {
        return Optional.ofNullable(encryption);
    }

    /**
     * Replaces the encryption envelope.
     *
     * @param encryption the new envelope, or {@code null} to clear
     */
    public void setEncryption(FederatedRsaEncryption encryption) {
        this.encryption = encryption;
    }
}

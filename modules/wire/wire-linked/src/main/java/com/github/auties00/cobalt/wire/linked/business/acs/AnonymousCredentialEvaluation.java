package com.github.auties00.cobalt.wire.linked.business.acs;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Server-evaluated blinded WhatsApp anonymous credential ready for the
 * client to unblind into a signed token.
 *
 * <p>When the WhatsApp anonymous-credential service issues credentials,
 * the server returns one evaluation per blinded token submitted by the
 * client. The evaluation is an opaque payload that the client unblinds
 * and parses into a signed token using the corresponding
 * {@link AnonymousCredentialProof}.
 *
 * <p>This model wraps that opaque payload.
 */
@ProtobufMessage(name = "AnonymousCredentialEvaluation")
public final class AnonymousCredentialEvaluation {
    /**
     * Opaque server-evaluation payload, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String data;

    /**
     * Constructs a new {@code AnonymousCredentialEvaluation}.
     *
     * @param data the opaque server-evaluation payload, or {@code null}
     *             when the server omitted it
     */
    AnonymousCredentialEvaluation(String data) {
        this.data = data;
    }

    /**
     * Returns the opaque server-evaluation payload.
     *
     * @return an {@code Optional} carrying the payload, or empty when the
     *         server omitted it
     */
    public Optional<String> data() {
        return Optional.ofNullable(data);
    }
}

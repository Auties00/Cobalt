package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One capability a WhatsApp Business AI agent can offer, paired with whether
 * it is currently available to the account.
 *
 * <p>The WhatsApp Business AI agent (the merchant's auto-reply assistant)
 * surfaces a set of capabilities (for example answering from knowledge,
 * capturing leads, or recommending products). Not every capability is
 * unlocked for every account; each one is reported together with an
 * availability status so a caller can discover what the assistant may do
 * before configuring it.
 */
@ProtobufMessage(name = "BusinessAiAbility")
public final class BusinessAiAbility {
    /**
     * Server-defined marker naming the capability. The full value set is not
     * recoverable from the WhatsApp client, so the raw marker is exposed as a
     * string. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String type;

    /**
     * Server-defined marker reporting whether the capability is available to
     * the account. The full value set is not recoverable from the WhatsApp
     * client, so the raw marker is exposed as a string. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String status;

    /**
     * Constructs a new {@code BusinessAiAbility}. Either argument may be
     * {@code null} when the server omitted the corresponding field.
     *
     * @param type   the capability marker, or {@code null}
     * @param status the availability marker, or {@code null}
     */
    BusinessAiAbility(String type, String status) {
        this.type = type;
        this.status = status;
    }

    /**
     * Returns the marker naming the capability.
     *
     * @return the capability marker, or empty when the server omitted it
     */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the marker reporting whether the capability is available.
     *
     * @return the availability marker, or empty when the server omitted it
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }
}

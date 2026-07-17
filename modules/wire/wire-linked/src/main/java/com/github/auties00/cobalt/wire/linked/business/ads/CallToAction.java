package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Call-to-action button of a Click-to-WhatsApp ad creative.
 *
 * <p>An ad creative may carry a single tappable button. This model describes it: the
 * {@link #type() button type} (for example {@code "WHATSAPP_MESSAGE"}, {@code "LEARN_MORE"}) and the
 * {@link #value() destination payload} the button opens.
 */
@ProtobufMessage(name = "CallToAction")
public final class CallToAction {
    /**
     * Server-defined button-type token. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String type;

    /**
     * Destination payload the button opens. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final CallToActionValue value;

    /**
     * Constructs a new {@code CallToAction}. Every argument may be {@code null} to leave the
     * corresponding field unset.
     *
     * @param type  the button-type token, or {@code null}
     * @param value the destination payload, or {@code null}
     */
    CallToAction(String type, CallToActionValue value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Returns the button-type token.
     *
     * @return an {@link Optional} carrying the button type, or empty when unset
     */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the destination payload the button opens.
     *
     * @return an {@link Optional} carrying the destination payload, or empty when unset
     */
    public Optional<CallToActionValue> value() {
        return Optional.ofNullable(value);
    }
}

package com.github.auties00.cobalt.wire.linked.business.waa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Acknowledgement that the caller's stored account-type preference for the
 * WhatsApp Ads sign-in flow was cleared.
 *
 * <p>The WhatsApp Ads sign-in flow caches the merchant's chosen account type
 * and Facebook ad page after the first sign-in so the picker is skipped on
 * subsequent visits. The "switch account type" action clears that cached
 * preference so the next sign-in re-prompts. This model is the server's
 * acknowledgement of that clear.
 *
 * <p>The acknowledgement is exposed as a raw string because the server scalar's
 * encoding is not recoverable from the WhatsApp client.
 */
@ProtobufMessage(name = "WhatsAppAdsAccountTypeReset")
public final class WhatsAppAdsAccountTypeReset {
    /**
     * Server acknowledgement marker. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String acknowledgement;

    /**
     * Constructs a new {@code WhatsAppAdsAccountTypeReset}.
     *
     * @param acknowledgement the server acknowledgement marker, or {@code null}
     */
    WhatsAppAdsAccountTypeReset(String acknowledgement) {
        this.acknowledgement = acknowledgement;
    }

    /**
     * Returns the server acknowledgement marker.
     *
     * @return the acknowledgement marker, or empty when the server omitted it
     */
    public Optional<String> acknowledgement() {
        return Optional.ofNullable(acknowledgement);
    }
}

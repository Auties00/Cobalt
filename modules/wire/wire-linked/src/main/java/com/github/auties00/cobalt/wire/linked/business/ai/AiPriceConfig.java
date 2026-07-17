package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Price-sharing configuration attached to a WhatsApp Business AI agent rule.
 *
 * <p>A rule may control whether and how the auto-reply assistant volunteers
 * product prices in a conversation. This model carries that single setting,
 * expressed as the price-sharing mode the merchant selected.
 */
@ProtobufMessage(name = "AiPriceConfig")
public final class AiPriceConfig {
    /**
     * Mode controlling whether and how the assistant shares product prices.
     * Empty when the merchant left it unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String priceSharing;

    /**
     * Constructs a new {@code AiPriceConfig}. The {@code priceSharing} may be
     * {@code null} to leave the mode unset.
     *
     * @param priceSharing the price-sharing mode, or {@code null}
     */
    AiPriceConfig(String priceSharing) {
        this.priceSharing = priceSharing;
    }

    /**
     * Returns the mode controlling whether and how the assistant shares
     * product prices.
     *
     * @return an {@link Optional} carrying the price-sharing mode, or empty
     *         when unset
     */
    public Optional<String> priceSharing() {
        return Optional.ofNullable(priceSharing);
    }
}

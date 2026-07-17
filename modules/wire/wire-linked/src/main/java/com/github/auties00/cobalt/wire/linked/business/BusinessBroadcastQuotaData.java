package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Lookup context for the WhatsApp Business broadcast marketing-message quota query.
 *
 * <p>The quota query carries a small terms-of-service acknowledgement describing whether the merchant
 * has accepted the marketing-message terms. This model holds that single acknowledgement flag; the
 * transport nests it under a {@code tos} object as {@code {tos: {did_accept: ...}}}.
 */
@ProtobufMessage(name = "BusinessBroadcastQuotaData")
public final class BusinessBroadcastQuotaData {
    /**
     * Whether the merchant has accepted the marketing-message terms of service. Serialized as the
     * {@code did_accept} field of the nested {@code tos} object.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean didAccept;

    /**
     * Constructs a new {@code BusinessBroadcastQuotaData}.
     *
     * @param didAccept whether the marketing-message terms have been accepted
     */
    public BusinessBroadcastQuotaData(boolean didAccept) {
        this.didAccept = didAccept;
    }

    /**
     * Returns whether the merchant has accepted the marketing-message terms of service.
     *
     * @return {@code true} when the terms have been accepted, {@code false} otherwise
     */
    public boolean didAccept() {
        return didAccept;
    }
}

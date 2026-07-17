package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Four-way state surface carried by the
 * {@code <marketing_messages status/>} eligibility projection.
 *
 * <p>WhatsApp Business marketing-messages campaigns are gated behind a
 * server-driven enrolment funnel. The relay surfaces the current funnel
 * state so the WhatsApp Business client can decide whether to enable the
 * campaign-creation surface, surface a warning banner, hide the surface,
 * or temporarily pause it.
 */
@ProtobufEnum
public enum BusinessMarketingMessagesStatus {
    /**
     * The marketing-messages feature is not available for this account.
     */
    FAIL(0),

    /**
     * The marketing-messages feature is temporarily paused.
     */
    PAUSED(1),

    /**
     * The marketing-messages feature is available with no caveats.
     */
    SUCCESS(2),

    /**
     * The marketing-messages feature is available but the relay is
     * surfacing a non-fatal warning (e.g. quota close to exhaustion).
     */
    WARNING(3);

    /**
     * The protobuf wire-format index associated with this status value.
     */
    final int index;

    /**
     * Constructs a new {@code BusinessMarketingMessagesStatus} with the
     * supplied protobuf index.
     *
     * @param index the protobuf wire-format index
     */
    BusinessMarketingMessagesStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the protobuf wire-format index associated with this value.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }
}

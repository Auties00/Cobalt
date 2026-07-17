package com.github.auties00.cobalt.wire.linked.business.ai;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * One customer lead captured by a WhatsApp Business AI agent's lead-capture
 * flow.
 *
 * <p>When a customer completes a lead-capture flow, the details they supplied
 * are recorded as a lead. This model is one such recorded lead: it carries the
 * captured {@link #customerInfo()}, the WhatsApp addresses of the customer who
 * submitted it ({@link #phoneNumber()} and {@link #lid()}), the
 * {@link #capturedAt()} instant, and whether the merchant has already
 * {@link #seen()} it in the lead inbox.
 */
@ProtobufMessage(name = "BusinessAiCapturedLead")
public final class BusinessAiCapturedLead {
    /**
     * Server-issued identifier of this captured lead. This is the handle used
     * to refer to the lead; it is not a WhatsApp address. Empty when the
     * server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Whether the merchant has already seen this lead in the lead inbox. A
     * lead the merchant has not yet seen contributes to the flow's unseen-lead
     * badge.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean seen;

    /**
     * Details the customer supplied when completing the flow. Empty when the
     * server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String customerInfo;

    /**
     * Phone-number address of the customer who submitted the lead. Empty when
     * the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final Jid phoneNumber;

    /**
     * LID address of the customer who submitted the lead. Empty when the
     * server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final Jid lid;

    /**
     * Instant the lead was captured. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant capturedAt;

    /**
     * Constructs a new {@code BusinessAiCapturedLead}. Every reference
     * argument may be {@code null} when the server omitted the corresponding
     * field.
     *
     * @param id           the lead identifier, or {@code null}
     * @param seen         whether the lead has been seen
     * @param customerInfo the captured customer details, or {@code null}
     * @param phoneNumber  the customer's phone-number address, or {@code null}
     * @param lid          the customer's LID address, or {@code null}
     * @param capturedAt   the capture instant, or {@code null}
     */
    BusinessAiCapturedLead(String id, boolean seen, String customerInfo, Jid phoneNumber, Jid lid, Instant capturedAt) {
        this.id = id;
        this.seen = seen;
        this.customerInfo = customerInfo;
        this.phoneNumber = phoneNumber;
        this.lid = lid;
        this.capturedAt = capturedAt;
    }

    /**
     * Returns the server-issued identifier of this captured lead.
     *
     * @return the lead id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns whether the merchant has already seen this lead.
     *
     * @return {@code true} when the lead has been seen, {@code false} otherwise
     */
    public boolean seen() {
        return seen;
    }

    /**
     * Returns the details the customer supplied when completing the flow.
     *
     * @return the captured customer details, or empty when the server omitted
     *         them
     */
    public Optional<String> customerInfo() {
        return Optional.ofNullable(customerInfo);
    }

    /**
     * Returns the phone-number address of the customer who submitted the lead.
     *
     * @return the customer's phone-number address, or empty when the server
     *         omitted it
     */
    public Optional<Jid> phoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    /**
     * Returns the LID address of the customer who submitted the lead.
     *
     * @return the customer's LID address, or empty when the server omitted it
     */
    public Optional<Jid> lid() {
        return Optional.ofNullable(lid);
    }

    /**
     * Returns the instant the lead was captured.
     *
     * @return the capture instant, or empty when the server omitted it
     */
    public Optional<Instant> capturedAt() {
        return Optional.ofNullable(capturedAt);
    }
}

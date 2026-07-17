package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The confirmation screen a merchant sees after a WhatsApp Business
 * advertisement goes live.
 *
 * <p>Once a "Click-to-WhatsApp" ad (a paid promotion that opens a chat with the
 * business when tapped) is published, the server returns the data the success
 * modal shows: which advertising account was billed and how the chosen payment
 * method is presented. This model gathers those confirmation fields.
 *
 * <p>{@link #billableAccountId()} is the advertising account that was billed;
 * {@link #paymentLabel()} is the human-readable description of the payment
 * method used; and {@link #paymentLogos()} lists the icons shown alongside that
 * description.
 */
@ProtobufMessage(name = "BusinessAdSuccessScreen")
public final class BusinessAdSuccessScreen {
    /**
     * Identifier of the advertising account that was billed. A numeric
     * advertising identifier, not a WhatsApp address. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String billableAccountId;

    /**
     * Human-readable description of the payment method used. Empty when the
     * server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String paymentLabel;

    /**
     * Icons shown alongside the payment description, as resolved locations, in
     * the order the server returned them. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<URI> paymentLogos;

    /**
     * Constructs a new {@code BusinessAdSuccessScreen}. A {@code null}
     * {@code paymentLogos} is coerced to an empty list, and the other reference
     * arguments may be {@code null} when the server omitted them.
     *
     * @param billableAccountId the billed advertising-account identifier, or {@code null}
     * @param paymentLabel      the payment-method description, or {@code null}
     * @param paymentLogos      the payment-method icon locations; {@code null} treated as empty
     */
    BusinessAdSuccessScreen(String billableAccountId, String paymentLabel, List<URI> paymentLogos) {
        this.billableAccountId = billableAccountId;
        this.paymentLabel = paymentLabel;
        this.paymentLogos = paymentLogos == null ? List.of() : paymentLogos;
    }

    /**
     * Returns the identifier of the advertising account that was billed.
     *
     * @return the billed advertising-account id, or empty when the server
     *         omitted it
     */
    public Optional<String> billableAccountId() {
        return Optional.ofNullable(billableAccountId);
    }

    /**
     * Returns the human-readable description of the payment method used.
     *
     * @return the payment-method description, or empty when the server omitted it
     */
    public Optional<String> paymentLabel() {
        return Optional.ofNullable(paymentLabel);
    }

    /**
     * Returns the icons shown alongside the payment description.
     *
     * @return an unmodifiable view of the payment-method icon locations; never
     *         {@code null}, possibly empty
     */
    public List<URI> paymentLogos() {
        return Collections.unmodifiableList(paymentLogos);
    }
}

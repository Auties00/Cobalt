package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Server-computed projection of how a WhatsApp Business advertisement is
 * expected to perform across a range of daily budgets.
 *
 * <p>Before a merchant publishes a "Click-to-WhatsApp" ad (the paid
 * promotion that opens a chat with the business when tapped), the
 * advertising surface plots a curve relating spend to expected outcomes.
 * Each point on the curve fixes one bid amount and reports the expected
 * spend, reach, impressions, and actions at that bid; the curve as a
 * whole lets the merchant pick a budget by trading off cost against
 * outcome.
 *
 * <p>This model is that curve: an ordered list of
 * {@link AdBudgetEstimatePoint} samples taken across the budget range.
 */
@ProtobufMessage(name = "AdBudgetEstimate")
public final class AdBudgetEstimate {
    /**
     * Curve samples relating budget to expected outcomes, in the order
     * the server returned them. Never {@code null}, possibly empty when
     * the server returned no curve.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<AdBudgetEstimatePoint> curve;

    /**
     * Constructs a new {@code AdBudgetEstimate}. A {@code null}
     * {@code curve} is coerced to an empty list.
     *
     * @param curve the curve samples; {@code null} treated as empty
     */
    AdBudgetEstimate(List<AdBudgetEstimatePoint> curve) {
        this.curve = curve == null ? List.of() : curve;
    }

    /**
     * Returns the curve samples relating budget to expected outcomes.
     *
     * @return an unmodifiable view of the curve samples; never
     *         {@code null}, possibly empty
     */
    public List<AdBudgetEstimatePoint> curve() {
        return Collections.unmodifiableList(curve);
    }
}

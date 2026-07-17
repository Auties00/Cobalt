package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalDouble;

/**
 * One point on the daily-outcome curve a WhatsApp Business advertisement
 * is projected to deliver at a particular budget level.
 *
 * <p>Before a merchant publishes a "Click-to-WhatsApp" ad (the paid
 * promotion that opens a chat with the business when tapped), the
 * advertising surface plots a curve relating spend to expected outcomes:
 * a higher daily budget reaches more people and drives more actions, with
 * diminishing returns as the budget grows. Each point on that curve fixes
 * one bid amount and reports what the merchant should expect to spend
 * there, how many people the ad would reach, how many impressions and
 * actions it would drive, and the lower and upper bounds the server
 * brackets the reach and action estimates with.
 *
 * <p>The values are server-computed estimates and may be fractional, so
 * they are exposed as {@code OptionalDouble}.
 */
@ProtobufMessage(name = "AdBudgetEstimatePoint")
public final class AdBudgetEstimatePoint {
    /**
     * Estimated number of actions the ad drives at this curve point, or
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.DOUBLE)
    final Double actions;

    /**
     * Lower bound of the estimated actions, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
    final Double actionsLowerBound;

    /**
     * Upper bound of the estimated actions, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.DOUBLE)
    final Double actionsUpperBound;

    /**
     * Bid amount fixed for this curve point, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.DOUBLE)
    final Double bid;

    /**
     * Estimated number of impressions the ad drives at this curve point,
     * or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.DOUBLE)
    final Double impressions;

    /**
     * Estimated reach at this curve point, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.DOUBLE)
    final Double reach;

    /**
     * Lower bound of the estimated reach, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.DOUBLE)
    final Double reachLowerBound;

    /**
     * Upper bound of the estimated reach, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.DOUBLE)
    final Double reachUpperBound;

    /**
     * Spend amount expected at this curve point, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.DOUBLE)
    final Double spend;

    /**
     * Constructs a new {@code AdBudgetEstimatePoint}. Any reference
     * argument may be {@code null} when the server omitted the
     * corresponding field.
     *
     * @param actions           the estimated number of actions, or
     *                          {@code null}
     * @param actionsLowerBound the lower bound of the estimated actions,
     *                          or {@code null}
     * @param actionsUpperBound the upper bound of the estimated actions,
     *                          or {@code null}
     * @param bid               the bid amount, or {@code null}
     * @param impressions       the estimated number of impressions, or
     *                          {@code null}
     * @param reach             the estimated reach, or {@code null}
     * @param reachLowerBound   the lower bound of the estimated reach, or
     *                          {@code null}
     * @param reachUpperBound   the upper bound of the estimated reach, or
     *                          {@code null}
     * @param spend             the expected spend amount, or {@code null}
     */
    AdBudgetEstimatePoint(Double actions, Double actionsLowerBound, Double actionsUpperBound, Double bid,
                          Double impressions, Double reach, Double reachLowerBound, Double reachUpperBound,
                          Double spend) {
        this.actions = actions;
        this.actionsLowerBound = actionsLowerBound;
        this.actionsUpperBound = actionsUpperBound;
        this.bid = bid;
        this.impressions = impressions;
        this.reach = reach;
        this.reachLowerBound = reachLowerBound;
        this.reachUpperBound = reachUpperBound;
        this.spend = spend;
    }

    /**
     * Returns the estimated number of actions the ad drives at this curve
     * point.
     *
     * @return an {@code OptionalDouble} carrying the estimate, or empty
     *         when the server omitted it
     */
    public OptionalDouble actions() {
        return actions != null ? OptionalDouble.of(actions) : OptionalDouble.empty();
    }

    /**
     * Returns the lower bound of the estimated actions.
     *
     * @return an {@code OptionalDouble} carrying the lower bound, or empty
     *         when the server omitted it
     */
    public OptionalDouble actionsLowerBound() {
        return actionsLowerBound != null ? OptionalDouble.of(actionsLowerBound) : OptionalDouble.empty();
    }

    /**
     * Returns the upper bound of the estimated actions.
     *
     * @return an {@code OptionalDouble} carrying the upper bound, or empty
     *         when the server omitted it
     */
    public OptionalDouble actionsUpperBound() {
        return actionsUpperBound != null ? OptionalDouble.of(actionsUpperBound) : OptionalDouble.empty();
    }

    /**
     * Returns the bid amount fixed for this curve point.
     *
     * @return an {@code OptionalDouble} carrying the bid, or empty when
     *         the server omitted it
     */
    public OptionalDouble bid() {
        return bid != null ? OptionalDouble.of(bid) : OptionalDouble.empty();
    }

    /**
     * Returns the estimated number of impressions the ad drives at this
     * curve point.
     *
     * @return an {@code OptionalDouble} carrying the estimate, or empty
     *         when the server omitted it
     */
    public OptionalDouble impressions() {
        return impressions != null ? OptionalDouble.of(impressions) : OptionalDouble.empty();
    }

    /**
     * Returns the estimated reach at this curve point.
     *
     * @return an {@code OptionalDouble} carrying the estimate, or empty
     *         when the server omitted it
     */
    public OptionalDouble reach() {
        return reach != null ? OptionalDouble.of(reach) : OptionalDouble.empty();
    }

    /**
     * Returns the lower bound of the estimated reach.
     *
     * @return an {@code OptionalDouble} carrying the lower bound, or empty
     *         when the server omitted it
     */
    public OptionalDouble reachLowerBound() {
        return reachLowerBound != null ? OptionalDouble.of(reachLowerBound) : OptionalDouble.empty();
    }

    /**
     * Returns the upper bound of the estimated reach.
     *
     * @return an {@code OptionalDouble} carrying the upper bound, or empty
     *         when the server omitted it
     */
    public OptionalDouble reachUpperBound() {
        return reachUpperBound != null ? OptionalDouble.of(reachUpperBound) : OptionalDouble.empty();
    }

    /**
     * Returns the expected spend amount at this curve point.
     *
     * @return an {@code OptionalDouble} carrying the spend, or empty when
     *         the server omitted it
     */
    public OptionalDouble spend() {
        return spend != null ? OptionalDouble.of(spend) : OptionalDouble.empty();
    }
}

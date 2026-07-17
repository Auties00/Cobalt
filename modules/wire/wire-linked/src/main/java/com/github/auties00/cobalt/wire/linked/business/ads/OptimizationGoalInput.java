package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Optimisation goal selecting what a Click-to-WhatsApp ad's delivery is optimised for.
 *
 * <p>When estimating reach or configuring an ad, the caller picks what the ad should optimise for (for
 * example link clicks or conversations). This model carries that single choice as the
 * {@link #optimizationGoal() optimisation goal} token.
 */
@ProtobufMessage(name = "OptimizationGoalInput")
public final class OptimizationGoalInput {
    /**
     * Server-defined optimisation-goal token. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String optimizationGoal;

    /**
     * Constructs a new {@code OptimizationGoalInput}. The {@code optimizationGoal} may be {@code null}
     * to leave it unset.
     *
     * @param optimizationGoal the optimisation-goal token, or {@code null}
     */
    OptimizationGoalInput(String optimizationGoal) {
        this.optimizationGoal = optimizationGoal;
    }

    /**
     * Returns the optimisation-goal token.
     *
     * @return an {@link Optional} carrying the goal, or empty when unset
     */
    public Optional<String> optimizationGoal() {
        return Optional.ofNullable(optimizationGoal);
    }
}

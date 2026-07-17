package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The budget choices offered for a WhatsApp Business advertisement.
 *
 * <p>When a merchant sets how much to spend on a "Click-to-WhatsApp" ad (a paid
 * promotion that opens a chat with the business when tapped), the ad-creation
 * surface presents a set of pre-computed spend amounts to pick from and
 * enforces a floor so the budget is never set too low to deliver. This model
 * carries those choices.
 *
 * <p>{@link #currentBudget()} is the amount currently selected for the ad,
 * {@link #amountOptions()} lists the offered spend amounts, and
 * {@link #minimumDailyBudget()} is the lowest daily spend the server allows.
 * All amounts are expressed in the billing currency's minor units as strings to
 * preserve the server's exact precision.
 */
@ProtobufMessage(name = "BusinessAdBudgetOptions")
public final class BusinessAdBudgetOptions {
    /**
     * Amount currently selected for the ad, in the billing currency's minor
     * units, as a string. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String currentBudget;

    /**
     * Offered spend amounts, each in the billing currency's minor units as a
     * string, in the order the server returned them. Never {@code null},
     * possibly empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> amountOptions;

    /**
     * Lowest daily spend the server allows, in the billing currency's minor
     * units, as a string. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String minimumDailyBudget;

    /**
     * Constructs a new {@code BusinessAdBudgetOptions}. A {@code null}
     * {@code amountOptions} is coerced to an empty list, and the scalar
     * reference arguments may be {@code null} when the server omitted them.
     *
     * @param currentBudget      the currently selected amount, or {@code null}
     * @param amountOptions      the offered amounts; {@code null} treated as empty
     * @param minimumDailyBudget the minimum daily amount, or {@code null}
     */
    BusinessAdBudgetOptions(String currentBudget, List<String> amountOptions, String minimumDailyBudget) {
        this.currentBudget = currentBudget;
        this.amountOptions = amountOptions == null ? List.of() : amountOptions;
        this.minimumDailyBudget = minimumDailyBudget;
    }

    /**
     * Returns the amount currently selected for the ad.
     *
     * @return the current budget amount, or empty when the server omitted it
     */
    public Optional<String> currentBudget() {
        return Optional.ofNullable(currentBudget);
    }

    /**
     * Returns the offered spend amounts.
     *
     * @return an unmodifiable view of the offered amounts; never {@code null},
     *         possibly empty
     */
    public List<String> amountOptions() {
        return Collections.unmodifiableList(amountOptions);
    }

    /**
     * Returns the lowest daily spend the server allows.
     *
     * @return the minimum daily amount, or empty when the server omitted it
     */
    public Optional<String> minimumDailyBudget() {
        return Optional.ofNullable(minimumDailyBudget);
    }
}

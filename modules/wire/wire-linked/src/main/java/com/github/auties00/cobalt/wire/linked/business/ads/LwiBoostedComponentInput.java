package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Input describing a boosted Click-to-WhatsApp component to create or edit.
 *
 * <p>A boosted component ties a creative to its budget, schedule, audience, placements, and welcome
 * experience. This model carries the whole boost definition: the ad-group creatives, the goal,
 * objective and optimisation options, the budget, duration and currency, the ad account and audience
 * selection, the placement spec, the messenger welcome message, and the serialized targeting spec
 * string. It is shared by the create-draft, edit-draft, and create-boosted-component operations.
 */
@ProtobufMessage(name = "LwiBoostedComponentInput")
public final class LwiBoostedComponentInput {
    /**
     * Ad-group creative specifications, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<AdGroupSpec> adgroupSpecs;

    /**
     * Click-to-WhatsApp lightweight-instant goal token. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String adsLwiGoal;

    /**
     * Chosen audience option token. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String audienceOption;

    /**
     * Total budget in the account currency's minor units, or unset when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    final Long budget;

    /**
     * Budget-type token (for example daily or lifetime). Empty when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String budgetType;

    /**
     * ISO 4217 currency code the budget is expressed in. Empty when unset.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String currency;

    /**
     * Digital Services Act beneficiary name. Empty when unset.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String dsaBeneficiary;

    /**
     * Digital Services Act payor name. Empty when unset.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String dsaPayor;

    /**
     * Campaign duration in days, or unset when the server omitted it.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT32)
    final Integer durationInDays;

    /**
     * Legacy Facebook ad-account identifier the boost is funded from. A numeric string, not a WhatsApp
     * address. Empty when unset.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    final String legacyAdAccountId;

    /**
     * Welcome-message configuration for the opening conversation. Empty when unset.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.MESSAGE)
    final MessengerWelcomeMessage messengerWelcomeMessage;

    /**
     * Campaign objective token. Empty when unset.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    final String objective;

    /**
     * Placement configuration. Empty when unset.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
    final PlacementSpec placementSpec;

    /**
     * Saved-audience identifier the boost targets. Empty when unset.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    final String savedAudienceId;

    /**
     * Serialized targeting spec string. Empty when unset.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    final String targetingSpecString;

    /**
     * Constructs a new {@code LwiBoostedComponentInput}. A {@code null} {@code adgroupSpecs} is coerced
     * to an empty list; every other argument may be {@code null} to leave the corresponding field
     * unset.
     *
     * @param adgroupSpecs            the ad-group creatives; {@code null} treated as empty
     * @param adsLwiGoal              the lightweight-instant goal token, or {@code null}
     * @param audienceOption          the audience option token, or {@code null}
     * @param budget                  the total budget in minor units, or {@code null}
     * @param budgetType              the budget-type token, or {@code null}
     * @param currency                the currency code, or {@code null}
     * @param dsaBeneficiary          the Digital Services Act beneficiary, or {@code null}
     * @param dsaPayor                the Digital Services Act payor, or {@code null}
     * @param durationInDays          the campaign duration in days, or {@code null}
     * @param legacyAdAccountId       the legacy ad-account identifier, or {@code null}
     * @param messengerWelcomeMessage the welcome-message configuration, or {@code null}
     * @param objective               the campaign objective token, or {@code null}
     * @param placementSpec           the placement configuration, or {@code null}
     * @param savedAudienceId         the saved-audience identifier, or {@code null}
     * @param targetingSpecString     the serialized targeting spec string, or {@code null}
     */
    LwiBoostedComponentInput(List<AdGroupSpec> adgroupSpecs, String adsLwiGoal, String audienceOption,
                             Long budget, String budgetType, String currency, String dsaBeneficiary,
                             String dsaPayor, Integer durationInDays, String legacyAdAccountId,
                             MessengerWelcomeMessage messengerWelcomeMessage, String objective,
                             PlacementSpec placementSpec, String savedAudienceId, String targetingSpecString) {
        this.adgroupSpecs = adgroupSpecs == null ? List.of() : List.copyOf(adgroupSpecs);
        this.adsLwiGoal = adsLwiGoal;
        this.audienceOption = audienceOption;
        this.budget = budget;
        this.budgetType = budgetType;
        this.currency = currency;
        this.dsaBeneficiary = dsaBeneficiary;
        this.dsaPayor = dsaPayor;
        this.durationInDays = durationInDays;
        this.legacyAdAccountId = legacyAdAccountId;
        this.messengerWelcomeMessage = messengerWelcomeMessage;
        this.objective = objective;
        this.placementSpec = placementSpec;
        this.savedAudienceId = savedAudienceId;
        this.targetingSpecString = targetingSpecString;
    }

    /**
     * Returns the ad-group creative specifications.
     *
     * @return an unmodifiable view of the ad-group creatives; never {@code null}, possibly empty
     */
    public List<AdGroupSpec> adgroupSpecs() {
        return adgroupSpecs;
    }

    /**
     * Returns the Click-to-WhatsApp lightweight-instant goal token.
     *
     * @return an {@link Optional} carrying the goal, or empty when unset
     */
    public Optional<String> adsLwiGoal() {
        return Optional.ofNullable(adsLwiGoal);
    }

    /**
     * Returns the chosen audience option token.
     *
     * @return an {@link Optional} carrying the audience option, or empty when unset
     */
    public Optional<String> audienceOption() {
        return Optional.ofNullable(audienceOption);
    }

    /**
     * Returns the total budget in the account currency's minor units.
     *
     * @return an {@link OptionalLong} carrying the budget, or empty when unset
     */
    public OptionalLong budget() {
        return budget == null ? OptionalLong.empty() : OptionalLong.of(budget);
    }

    /**
     * Returns the budget-type token.
     *
     * @return an {@link Optional} carrying the budget type, or empty when unset
     */
    public Optional<String> budgetType() {
        return Optional.ofNullable(budgetType);
    }

    /**
     * Returns the currency code the budget is expressed in.
     *
     * @return an {@link Optional} carrying the currency, or empty when unset
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns the Digital Services Act beneficiary name.
     *
     * @return an {@link Optional} carrying the beneficiary, or empty when unset
     */
    public Optional<String> dsaBeneficiary() {
        return Optional.ofNullable(dsaBeneficiary);
    }

    /**
     * Returns the Digital Services Act payor name.
     *
     * @return an {@link Optional} carrying the payor, or empty when unset
     */
    public Optional<String> dsaPayor() {
        return Optional.ofNullable(dsaPayor);
    }

    /**
     * Returns the campaign duration in days.
     *
     * @return an {@link OptionalInt} carrying the duration, or empty when unset
     */
    public OptionalInt durationInDays() {
        return durationInDays == null ? OptionalInt.empty() : OptionalInt.of(durationInDays);
    }

    /**
     * Returns the legacy Facebook ad-account identifier the boost is funded from.
     *
     * @return an {@link Optional} carrying the ad-account identifier, or empty when unset
     */
    public Optional<String> legacyAdAccountId() {
        return Optional.ofNullable(legacyAdAccountId);
    }

    /**
     * Returns the welcome-message configuration for the opening conversation.
     *
     * @return an {@link Optional} carrying the welcome message, or empty when unset
     */
    public Optional<MessengerWelcomeMessage> messengerWelcomeMessage() {
        return Optional.ofNullable(messengerWelcomeMessage);
    }

    /**
     * Returns the campaign objective token.
     *
     * @return an {@link Optional} carrying the objective, or empty when unset
     */
    public Optional<String> objective() {
        return Optional.ofNullable(objective);
    }

    /**
     * Returns the placement configuration.
     *
     * @return an {@link Optional} carrying the placement spec, or empty when unset
     */
    public Optional<PlacementSpec> placementSpec() {
        return Optional.ofNullable(placementSpec);
    }

    /**
     * Returns the saved-audience identifier the boost targets.
     *
     * @return an {@link Optional} carrying the saved-audience identifier, or empty when unset
     */
    public Optional<String> savedAudienceId() {
        return Optional.ofNullable(savedAudienceId);
    }

    /**
     * Returns the serialized targeting spec string.
     *
     * @return an {@link Optional} carrying the targeting spec string, or empty when unset
     */
    public Optional<String> targetingSpecString() {
        return Optional.ofNullable(targetingSpecString);
    }
}

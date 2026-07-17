package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Input model for the audience-picker query of the Click-to-WhatsApp ad
 * creation flow.
 *
 * <p>A Click-to-WhatsApp ad is a paid promotion that opens a chat with the
 * business when tapped. While building one the merchant picks the audience
 * from a list of suggestions tailored to the chosen goal, budget, and run
 * duration, plus the merchant's reusable saved audiences. This input
 * carries the parameters that select which suggestions the server returns.
 *
 * <p>The {@link #input() boosted-component input} identifies the boost being
 * targeted, carried as an {@link LwiBoostedComponentInput}. The
 * {@link #objective() campaign objective},
 * {@link #budgetMicros() budget}, {@link #budgetType() budget cadence},
 * {@link #durationSeconds() run duration}, {@link #adAccountId() ad
 * account}, and {@link #savedAudienceCount() saved-audience page size}
 * narrow the response further; every field may be left unset so the server
 * applies its default.
 */
@ProtobufMessage(name = "BusinessAdAudienceSectionQuery")
public final class BusinessAdAudienceSectionQuery {
    /**
     * Boosted-component input identifying which boost the audience
     * suggestions are computed for. Unset omits the reference.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final LwiBoostedComponentInput input;

    /**
     * Campaign objective the audience suggestions are tailored for.
     * Unset lets the server apply its default.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String objective;

    /**
     * Selected budget amount, expressed in micros of the billing currency
     * (one micro is one millionth of a currency unit). The wire value is
     * an unitless integer; the unit follows Meta's standard ad-budget
     * convention. Unset omits the variable.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final Long budgetMicros;

    /**
     * Budget cadence selected for the campaign. The recognised values are
     * defined by the server and are not modelled as an enum. Unset omits
     * the variable.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String budgetType;

    /**
     * Run duration of the campaign in seconds. Unset omits the variable.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64)
    final Long durationSeconds;

    /**
     * Advertising-account identifier the audience suggestions are scoped
     * to. Unset omits the variable.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String adAccountId;

    /**
     * Page size requested for the merchant's saved audiences. Unset omits
     * the variable.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT32)
    final Integer savedAudienceCount;

    /**
     * Constructs a new {@code BusinessAdAudienceSectionQuery}. Every
     * argument may be {@code null} to omit the corresponding variable from
     * the request.
     *
     * @param input              the boosted-component input, or {@code null}
     * @param objective          the campaign objective, or {@code null}
     * @param budgetMicros       the budget in micros, or {@code null}
     * @param budgetType         the budget cadence, or {@code null}
     * @param durationSeconds    the run duration in seconds, or {@code null}
     * @param adAccountId        the advertising-account identifier, or
     *                           {@code null}
     * @param savedAudienceCount the saved-audience page size, or
     *                           {@code null}
     */
    public BusinessAdAudienceSectionQuery(LwiBoostedComponentInput input, String objective, Long budgetMicros,
                                          String budgetType, Long durationSeconds, String adAccountId,
                                          Integer savedAudienceCount) {
        this.input = input;
        this.objective = objective;
        this.budgetMicros = budgetMicros;
        this.budgetType = budgetType;
        this.durationSeconds = durationSeconds;
        this.adAccountId = adAccountId;
        this.savedAudienceCount = savedAudienceCount;
    }

    /**
     * Returns the boosted-component input.
     *
     * @return an {@link Optional} carrying the input, or empty when unset
     */
    public Optional<LwiBoostedComponentInput> input() {
        return Optional.ofNullable(input);
    }

    /**
     * Returns the campaign objective.
     *
     * @return an {@link Optional} carrying the objective, or empty when
     *         unset
     */
    public Optional<String> objective() {
        return Optional.ofNullable(objective);
    }

    /**
     * Returns the budget amount in micros.
     *
     * @return an {@link OptionalLong} carrying the budget, or empty when
     *         unset
     */
    public OptionalLong budgetMicros() {
        return budgetMicros == null ? OptionalLong.empty() : OptionalLong.of(budgetMicros);
    }

    /**
     * Returns the budget cadence.
     *
     * @return an {@link Optional} carrying the cadence, or empty when
     *         unset
     */
    public Optional<String> budgetType() {
        return Optional.ofNullable(budgetType);
    }

    /**
     * Returns the run duration in seconds.
     *
     * @return an {@link OptionalLong} carrying the duration, or empty when
     *         unset
     */
    public OptionalLong durationSeconds() {
        return durationSeconds == null ? OptionalLong.empty() : OptionalLong.of(durationSeconds);
    }

    /**
     * Returns the advertising-account identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> adAccountId() {
        return Optional.ofNullable(adAccountId);
    }

    /**
     * Returns the saved-audience page size.
     *
     * @return an {@link OptionalInt} carrying the page size, or empty when
     *         unset
     */
    public OptionalInt savedAudienceCount() {
        return savedAudienceCount == null ? OptionalInt.empty() : OptionalInt.of(savedAudienceCount);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessAdAudienceSectionQuery) obj;
        return Objects.equals(input, that.input)
                && Objects.equals(objective, that.objective)
                && Objects.equals(budgetMicros, that.budgetMicros)
                && Objects.equals(budgetType, that.budgetType)
                && Objects.equals(durationSeconds, that.durationSeconds)
                && Objects.equals(adAccountId, that.adAccountId)
                && Objects.equals(savedAudienceCount, that.savedAudienceCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, objective, budgetMicros, budgetType, durationSeconds,
                adAccountId, savedAudienceCount);
    }

    @Override
    public String toString() {
        return "BusinessAdAudienceSectionQuery[" +
                "input=" + input + ", " +
                "objective=" + objective + ", " +
                "budgetMicros=" + budgetMicros + ", " +
                "budgetType=" + budgetType + ", " +
                "durationSeconds=" + durationSeconds + ", " +
                "adAccountId=" + adAccountId + ", " +
                "savedAudienceCount=" + savedAudienceCount + ']';
    }
}

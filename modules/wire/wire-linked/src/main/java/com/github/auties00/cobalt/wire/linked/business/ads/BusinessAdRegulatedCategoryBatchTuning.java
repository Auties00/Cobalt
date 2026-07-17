package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for the multi-category regulated-targeting adjustment of the
 * Click-to-WhatsApp ad creation flow.
 *
 * <p>A Click-to-WhatsApp ad is a paid promotion that opens a chat with the
 * business when tapped. When the audience targeting falls under several
 * regulated categories at once, plus the special-ad-category restrictions
 * applicable to certain countries, the server rewrites the targeting into
 * a compliant form in a single round trip. This input carries the
 * parameters the server uses to compute that batched rewrite.
 *
 * <p>The {@link #targetSpec() targeting}, {@link #regulatedCategories()
 * regulated categories}, {@link #specialAdCategoryCountries()
 * special-ad-category countries}, and {@link #tuningOptions() tuning
 * options} parameterise the rewrite. The {@link #adAccountId() ad account}
 * scopes the adjustment.
 */
@ProtobufMessage(name = "BusinessAdRegulatedCategoryBatchTuning")
public final class BusinessAdRegulatedCategoryBatchTuning {
    /**
     * Advertising-account identifier the targeting belongs to. Unset omits
     * the variable.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String adAccountId;

    /**
     * Audience targeting specification to adjust. Unset omits the variable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final TargetingSpec targetSpec;

    /**
     * Regulated categories that apply, in the order they are sent. Never
     * {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final List<SpecialAdCategory> regulatedCategories;

    /**
     * Countries whose special-ad-category rules apply, in the order they are
     * sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final List<String> specialAdCategoryCountries;

    /**
     * Tuning options controlling how the spec is rewritten. Unset omits the
     * variable.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final TuningOptions tuningOptions;

    /**
     * Constructs a new {@code BusinessAdRegulatedCategoryBatchTuning}. A
     * {@code null} list argument is coerced to an empty list; every other
     * argument may be {@code null} to omit the corresponding variable from
     * the request.
     *
     * @param adAccountId                the advertising-account identifier, or {@code null}
     * @param targetSpec                 the audience targeting, or {@code null}
     * @param regulatedCategories        the regulated categories; {@code null} treated as empty
     * @param specialAdCategoryCountries the special-ad-category countries; {@code null} treated as empty
     * @param tuningOptions              the tuning options, or {@code null}
     */
    public BusinessAdRegulatedCategoryBatchTuning(String adAccountId, TargetingSpec targetSpec,
                                                  List<SpecialAdCategory> regulatedCategories,
                                                  List<String> specialAdCategoryCountries,
                                                  TuningOptions tuningOptions) {
        this.adAccountId = adAccountId;
        this.targetSpec = targetSpec;
        this.regulatedCategories = regulatedCategories == null ? List.of() : List.copyOf(regulatedCategories);
        this.specialAdCategoryCountries = specialAdCategoryCountries == null ? List.of() : List.copyOf(specialAdCategoryCountries);
        this.tuningOptions = tuningOptions;
    }

    /**
     * Returns the advertising-account identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when unset
     */
    public Optional<String> adAccountId() {
        return Optional.ofNullable(adAccountId);
    }

    /**
     * Returns the audience targeting specification.
     *
     * @return an {@link Optional} carrying the targeting spec, or empty when unset
     */
    public Optional<TargetingSpec> targetSpec() {
        return Optional.ofNullable(targetSpec);
    }

    /**
     * Returns the regulated categories that apply.
     *
     * @return an unmodifiable view of the regulated categories; never {@code null}, possibly empty
     */
    public List<SpecialAdCategory> regulatedCategories() {
        return regulatedCategories;
    }

    /**
     * Returns the countries whose special-ad-category rules apply.
     *
     * @return an unmodifiable view of the countries; never {@code null}, possibly empty
     */
    public List<String> specialAdCategoryCountries() {
        return specialAdCategoryCountries;
    }

    /**
     * Returns the tuning options controlling how the spec is rewritten.
     *
     * @return an {@link Optional} carrying the tuning options, or empty when unset
     */
    public Optional<TuningOptions> tuningOptions() {
        return Optional.ofNullable(tuningOptions);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessAdRegulatedCategoryBatchTuning) obj;
        return Objects.equals(adAccountId, that.adAccountId)
                && Objects.equals(targetSpec, that.targetSpec)
                && Objects.equals(regulatedCategories, that.regulatedCategories)
                && Objects.equals(specialAdCategoryCountries, that.specialAdCategoryCountries)
                && Objects.equals(tuningOptions, that.tuningOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adAccountId, targetSpec, regulatedCategories,
                specialAdCategoryCountries, tuningOptions);
    }

    @Override
    public String toString() {
        return "BusinessAdRegulatedCategoryBatchTuning[" +
                "adAccountId=" + adAccountId + ", " +
                "targetSpec=" + targetSpec + ", " +
                "regulatedCategories=" + regulatedCategories + ", " +
                "specialAdCategoryCountries=" + specialAdCategoryCountries + ", " +
                "tuningOptions=" + tuningOptions + ']';
    }
}

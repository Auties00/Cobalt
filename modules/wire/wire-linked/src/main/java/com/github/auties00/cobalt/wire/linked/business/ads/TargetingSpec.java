package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Targeting specification constraining the audience of a Click-to-WhatsApp ad.
 *
 * <p>A targeting spec is the Facebook targeting object: it narrows the audience by age, gender,
 * location, detailed-targeting groups, and automation. This model carries the keys WhatsApp Web's
 * targeting builder populates: the {@link #ageMin() minimum} and {@link #ageMax() maximum} age (or the
 * {@link #ageRange() age range} the Advantage-plus toggle writes instead), the {@link #genders()
 * genders}, the {@link #geoLocations() geographic constraint}, the {@link #flexibleSpec() flexible
 * detailed-targeting groups}, and the {@link #targetingAutomation() automation settings}.
 */
@ProtobufMessage(name = "TargetingSpec")
public final class TargetingSpec {
    /**
     * Minimum audience age, or unset when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final Integer ageMin;

    /**
     * Maximum audience age, or unset when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    final Integer ageMax;

    /**
     * Age range as a two-element {@code [min, max]} list the Advantage-plus toggle writes in place of
     * the discrete minimum and maximum, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final List<Integer> ageRange;

    /**
     * Gender codes the ad targets ({@code 1} for men, {@code 2} for women), in the order they are
     * sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final List<Integer> genders;

    /**
     * Geographic constraint of the targeting. Empty when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final TargetingGeoLocations geoLocations;

    /**
     * Flexible detailed-targeting groups, in the order they are sent. Never {@code null}, possibly
     * empty.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final List<TargetingFlexibleSpec> flexibleSpec;

    /**
     * Automation settings of the targeting. Empty when unset.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final TargetingAutomation targetingAutomation;

    /**
     * Constructs a new {@code TargetingSpec}. A {@code null} list argument is coerced to an empty list;
     * every other argument may be {@code null} to leave the corresponding field unset.
     *
     * @param ageMin              the minimum audience age, or {@code null}
     * @param ageMax              the maximum audience age, or {@code null}
     * @param ageRange            the {@code [min, max]} age range; {@code null} treated as empty
     * @param genders             the gender codes; {@code null} treated as empty
     * @param geoLocations        the geographic constraint, or {@code null}
     * @param flexibleSpec        the flexible detailed-targeting groups; {@code null} treated as empty
     * @param targetingAutomation the automation settings, or {@code null}
     */
    TargetingSpec(Integer ageMin, Integer ageMax, List<Integer> ageRange, List<Integer> genders,
                  TargetingGeoLocations geoLocations, List<TargetingFlexibleSpec> flexibleSpec,
                  TargetingAutomation targetingAutomation) {
        this.ageMin = ageMin;
        this.ageMax = ageMax;
        this.ageRange = ageRange == null ? List.of() : List.copyOf(ageRange);
        this.genders = genders == null ? List.of() : List.copyOf(genders);
        this.geoLocations = geoLocations;
        this.flexibleSpec = flexibleSpec == null ? List.of() : List.copyOf(flexibleSpec);
        this.targetingAutomation = targetingAutomation;
    }

    /**
     * Returns the minimum audience age.
     *
     * @return an {@link OptionalInt} carrying the minimum age, or empty when unset
     */
    public OptionalInt ageMin() {
        return ageMin == null ? OptionalInt.empty() : OptionalInt.of(ageMin);
    }

    /**
     * Returns the maximum audience age.
     *
     * @return an {@link OptionalInt} carrying the maximum age, or empty when unset
     */
    public OptionalInt ageMax() {
        return ageMax == null ? OptionalInt.empty() : OptionalInt.of(ageMax);
    }

    /**
     * Returns the age range as a two-element {@code [min, max]} list.
     *
     * @return an unmodifiable view of the age range; never {@code null}, possibly empty
     */
    public List<Integer> ageRange() {
        return ageRange;
    }

    /**
     * Returns the gender codes the ad targets.
     *
     * @return an unmodifiable view of the gender codes; never {@code null}, possibly empty
     */
    public List<Integer> genders() {
        return genders;
    }

    /**
     * Returns the geographic constraint of the targeting.
     *
     * @return an {@link Optional} carrying the geographic constraint, or empty when unset
     */
    public Optional<TargetingGeoLocations> geoLocations() {
        return Optional.ofNullable(geoLocations);
    }

    /**
     * Returns the flexible detailed-targeting groups.
     *
     * @return an unmodifiable view of the flexible detailed-targeting groups; never {@code null},
     *         possibly empty
     */
    public List<TargetingFlexibleSpec> flexibleSpec() {
        return flexibleSpec;
    }

    /**
     * Returns the automation settings of the targeting.
     *
     * @return an {@link Optional} carrying the automation settings, or empty when unset
     */
    public Optional<TargetingAutomation> targetingAutomation() {
        return Optional.ofNullable(targetingAutomation);
    }
}

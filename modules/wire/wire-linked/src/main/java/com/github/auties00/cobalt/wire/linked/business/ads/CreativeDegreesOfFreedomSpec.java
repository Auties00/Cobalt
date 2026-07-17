package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Optional;

/**
 * Advantage-plus creative-optimisation settings of a Click-to-WhatsApp ad group's creative.
 *
 * <p>When automatic creative optimisation is enabled, the ad group's creative carries a
 * degrees-of-freedom spec describing which transformations the platform may apply. This model carries
 * the {@link #degreesOfFreedomType() enrolment type} and the image, stories, and text
 * {@link #imageTransformationTypes() transformation} type lists WhatsApp Web populates.
 */
@ProtobufMessage(name = "CreativeDegreesOfFreedomSpec")
public final class CreativeDegreesOfFreedomSpec {
    /**
     * Enrolment type of the creative optimisation (for example {@code "USER_ENROLLED_LWI_ACO"}). Empty
     * when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String degreesOfFreedomType;

    /**
     * Image transformation types the platform may apply, in the order they are sent. Never {@code null},
     * possibly empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> imageTransformationTypes;

    /**
     * Stories transformation types the platform may apply, in the order they are sent. Never
     * {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> storiesTransformationTypes;

    /**
     * Text transformation types the platform may apply, in the order they are sent. Never {@code null},
     * possibly empty.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final List<String> textTransformationTypes;

    /**
     * Creative-features spec describing the automated creative enhancements the platform may apply. Empty
     * when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final CreativeFeaturesSpec creativeFeaturesSpec;

    /**
     * Constructs a new {@code CreativeDegreesOfFreedomSpec}. A {@code null} list argument is coerced to
     * an empty list; {@code degreesOfFreedomType} and {@code creativeFeaturesSpec} may be {@code null} to
     * leave them unset.
     *
     * @param degreesOfFreedomType       the enrolment type, or {@code null}
     * @param imageTransformationTypes   the image transformation types; {@code null} treated as empty
     * @param storiesTransformationTypes the stories transformation types; {@code null} treated as empty
     * @param textTransformationTypes    the text transformation types; {@code null} treated as empty
     * @param creativeFeaturesSpec       the creative-features spec, or {@code null}
     */
    CreativeDegreesOfFreedomSpec(String degreesOfFreedomType, List<String> imageTransformationTypes,
                                 List<String> storiesTransformationTypes, List<String> textTransformationTypes,
                                 CreativeFeaturesSpec creativeFeaturesSpec) {
        this.degreesOfFreedomType = degreesOfFreedomType;
        this.imageTransformationTypes = imageTransformationTypes == null ? List.of() : List.copyOf(imageTransformationTypes);
        this.storiesTransformationTypes = storiesTransformationTypes == null ? List.of() : List.copyOf(storiesTransformationTypes);
        this.textTransformationTypes = textTransformationTypes == null ? List.of() : List.copyOf(textTransformationTypes);
        this.creativeFeaturesSpec = creativeFeaturesSpec;
    }

    /**
     * Returns the enrolment type of the creative optimisation.
     *
     * @return an {@link Optional} carrying the enrolment type, or empty when unset
     */
    public Optional<String> degreesOfFreedomType() {
        return Optional.ofNullable(degreesOfFreedomType);
    }

    /**
     * Returns the image transformation types the platform may apply.
     *
     * @return an unmodifiable view of the image transformation types; never {@code null}, possibly empty
     */
    public List<String> imageTransformationTypes() {
        return imageTransformationTypes;
    }

    /**
     * Returns the stories transformation types the platform may apply.
     *
     * @return an unmodifiable view of the stories transformation types; never {@code null}, possibly empty
     */
    public List<String> storiesTransformationTypes() {
        return storiesTransformationTypes;
    }

    /**
     * Returns the text transformation types the platform may apply.
     *
     * @return an unmodifiable view of the text transformation types; never {@code null}, possibly empty
     */
    public List<String> textTransformationTypes() {
        return textTransformationTypes;
    }

    /**
     * Returns the creative-features spec describing the automated creative enhancements.
     *
     * @return an {@link Optional} carrying the creative-features spec, or empty when unset
     */
    public Optional<CreativeFeaturesSpec> creativeFeaturesSpec() {
        return Optional.ofNullable(creativeFeaturesSpec);
    }
}

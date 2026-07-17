package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for the plain-language targeting-description query of the
 * Click-to-WhatsApp ad creation flow.
 *
 * <p>A Click-to-WhatsApp ad is a paid promotion that opens a chat with the
 * business when tapped. After choosing an audience the merchant reviews
 * who the ad will reach as a list of readable sentences (for example
 * "people aged 18 to 34 in Italy interested in football"). This input
 * carries the parameters the server uses to render those sentences.
 *
 * <p>The {@link #targetingSpecString() targeting specification} is the
 * serialized audience targeting to describe; its structure is defined by
 * the server and is carried verbatim as a string. The
 * {@link #adAccountId() ad account}, {@link #audienceOption() audience
 * option}, and {@link #locationOnly() location-only flag} narrow the
 * description; any of them may be left unset.
 */
@ProtobufMessage(name = "BusinessAdTargetingSentencesQuery")
public final class BusinessAdTargetingSentencesQuery {
    /**
     * Advertising-account identifier the description is scoped to. Unset
     * omits the variable.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String adAccountId;

    /**
     * Selected audience option the description is computed for. The
     * recognised values are defined by the server. Unset omits the
     * variable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String audienceOption;

    /**
     * Whether the description should cover only the location portion of
     * the targeting. {@code true} restricts the rendered sentences to
     * location facets. Unset omits the variable.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final Boolean locationOnly;

    /**
     * Serialized targeting specification to describe. Its structure is
     * defined by the server and is carried verbatim. Unset omits the
     * variable.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String targetingSpecString;

    /**
     * Constructs a new {@code BusinessAdTargetingSentencesQuery}. Every
     * argument may be {@code null} to omit the corresponding variable from
     * the request.
     *
     * @param adAccountId         the advertising-account identifier, or
     *                            {@code null}
     * @param audienceOption      the audience option, or {@code null}
     * @param locationOnly        whether to describe only the location
     *                            portion, or {@code null}
     * @param targetingSpecString the serialized targeting, or {@code null}
     */
    public BusinessAdTargetingSentencesQuery(String adAccountId, String audienceOption,
                                             Boolean locationOnly, String targetingSpecString) {
        this.adAccountId = adAccountId;
        this.audienceOption = audienceOption;
        this.locationOnly = locationOnly;
        this.targetingSpecString = targetingSpecString;
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
     * Returns the audience option.
     *
     * @return an {@link Optional} carrying the option, or empty when unset
     */
    public Optional<String> audienceOption() {
        return Optional.ofNullable(audienceOption);
    }

    /**
     * Returns whether the description should cover only the location
     * portion. The accessor mirrors the wire-level tri-state ({@code true},
     * {@code false}, unset).
     *
     * @return the {@code Boolean} value, or {@code null} when unset
     */
    public Boolean locationOnly() {
        return locationOnly;
    }

    /**
     * Returns the serialized targeting specification.
     *
     * @return an {@link Optional} carrying the specification, or empty
     *         when unset
     */
    public Optional<String> targetingSpecString() {
        return Optional.ofNullable(targetingSpecString);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessAdTargetingSentencesQuery) obj;
        return Objects.equals(adAccountId, that.adAccountId)
                && Objects.equals(audienceOption, that.audienceOption)
                && Objects.equals(locationOnly, that.locationOnly)
                && Objects.equals(targetingSpecString, that.targetingSpecString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adAccountId, audienceOption, locationOnly, targetingSpecString);
    }

    @Override
    public String toString() {
        return "BusinessAdTargetingSentencesQuery[" +
                "adAccountId=" + adAccountId + ", " +
                "audienceOption=" + audienceOption + ", " +
                "locationOnly=" + locationOnly + ", " +
                "targetingSpecString=" + targetingSpecString + ']';
    }
}

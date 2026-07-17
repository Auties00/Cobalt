package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A chosen audience option for a Click-to-WhatsApp ad reach estimate.
 *
 * <p>When estimating an ad's daily reach, the caller supplies the audience the estimate is computed
 * for. This model carries that audience: the {@link #audienceOption() option} and
 * {@link #audienceKey() key} identifying it, its {@link #name() display name}, whether it is
 * {@link #clientEditable() client-editable} and {@link #subjectToDsa() subject to the Digital Services
 * Act}, and its {@link #targetSpecStringWithoutPlacements() targeting spec string} with placements
 * excluded.
 */
@ProtobufMessage(name = "AdsLwiAudience")
public final class AdsLwiAudience {
    /**
     * Option token identifying the chosen audience. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String audienceOption;

    /**
     * Key identifying the chosen audience. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String audienceKey;

    /**
     * Display name of the audience. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String name;

    /**
     * Whether the audience may be edited by the client.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean clientEditable;

    /**
     * Whether the audience is subject to Digital Services Act restrictions.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final boolean subjectToDsa;

    /**
     * Serialized targeting spec string for the audience with placements excluded. Empty when unset.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String targetSpecStringWithoutPlacements;

    /**
     * Constructs a new {@code AdsLwiAudience}. Every string argument may be {@code null} to leave the
     * corresponding field unset.
     *
     * @param audienceOption                    the audience option token, or {@code null}
     * @param audienceKey                       the audience key, or {@code null}
     * @param name                              the display name, or {@code null}
     * @param clientEditable                    whether the audience is client-editable
     * @param subjectToDsa                      whether the audience is subject to the Digital Services Act
     * @param targetSpecStringWithoutPlacements the targeting spec string without placements, or {@code null}
     */
    AdsLwiAudience(String audienceOption, String audienceKey, String name, boolean clientEditable,
                   boolean subjectToDsa, String targetSpecStringWithoutPlacements) {
        this.audienceOption = audienceOption;
        this.audienceKey = audienceKey;
        this.name = name;
        this.clientEditable = clientEditable;
        this.subjectToDsa = subjectToDsa;
        this.targetSpecStringWithoutPlacements = targetSpecStringWithoutPlacements;
    }

    /**
     * Returns the option token identifying the chosen audience.
     *
     * @return an {@link Optional} carrying the option, or empty when unset
     */
    public Optional<String> audienceOption() {
        return Optional.ofNullable(audienceOption);
    }

    /**
     * Returns the key identifying the chosen audience.
     *
     * @return an {@link Optional} carrying the key, or empty when unset
     */
    public Optional<String> audienceKey() {
        return Optional.ofNullable(audienceKey);
    }

    /**
     * Returns the display name of the audience.
     *
     * @return an {@link Optional} carrying the name, or empty when unset
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns whether the audience may be edited by the client.
     *
     * @return {@code true} when the audience is client-editable, {@code false} otherwise
     */
    public boolean clientEditable() {
        return clientEditable;
    }

    /**
     * Returns whether the audience is subject to Digital Services Act restrictions.
     *
     * @return {@code true} when the audience is subject to the Digital Services Act, {@code false} otherwise
     */
    public boolean subjectToDsa() {
        return subjectToDsa;
    }

    /**
     * Returns the serialized targeting spec string for the audience with placements excluded.
     *
     * @return an {@link Optional} carrying the targeting spec string, or empty when unset
     */
    public Optional<String> targetSpecStringWithoutPlacements() {
        return Optional.ofNullable(targetSpecStringWithoutPlacements);
    }
}

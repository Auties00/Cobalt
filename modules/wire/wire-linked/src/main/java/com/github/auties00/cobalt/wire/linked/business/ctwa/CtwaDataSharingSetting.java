package com.github.auties00.cobalt.wire.linked.business.ctwa;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Optional;

/**
 * Tri-state of the global Small-Business click-to-WhatsApp (CTWA) data-sharing setting.
 *
 * <p>A WhatsApp Business account carries a single account-wide switch deciding whether CTWA
 * conversion signals derived from ad-originated conversations may be shared back to Meta. The
 * switch is fetched through the business-settings privacy query, where it rides the
 * {@code <smb_data_sharing_with_meta_consent>} grandchild of the privacy IQ as one of the three
 * tri-state wire literals {@code "true"}, {@code "false"} and {@code "notset"}. The setting gates
 * whether the conversion-signal pipeline emits: an explicit opt-in ({@link #ENABLED}) lets signals
 * flow, an explicit opt-out ({@link #DISABLED}) blocks them outright, and a never-prompted state
 * ({@link #NOT_SET}) suppresses event sharing while the consent dialog has yet to be answered.
 *
 * <p>Callers that have never fetched the setting treat an absent value as {@link #NOT_SET}.
 */
@ProtobufEnum
public enum CtwaDataSharingSetting {
    /**
     * The account has never been prompted to make a choice; maps to the wire literal
     * {@code "notset"}. Event sharing stays suppressed until the consent dialog is answered.
     */
    NOT_SET(0),

    /**
     * The account has explicitly declined to share CTWA data with Meta; maps to the wire literal
     * {@code "false"}.
     */
    DISABLED(1),

    /**
     * The account has explicitly granted consent to share CTWA data with Meta; maps to the wire
     * literal {@code "true"}.
     */
    ENABLED(2);

    /**
     * The protobuf-encoded index used to persist this enum.
     */
    final int index;

    /**
     * Constructs a CTWA data-sharing-setting variant bound to its protobuf index.
     *
     * @param index the protobuf index
     */
    CtwaDataSharingSetting(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the protobuf-encoded index.
     *
     * @return the protobuf index
     */
    public int index() {
        return index;
    }

    /**
     * Resolves the CTWA data-sharing-setting variant for a business-settings privacy wire literal.
     *
     * <p>The {@code "true"} literal maps to {@link #ENABLED}, the {@code "false"} literal to
     * {@link #DISABLED}, and the {@code "notset"} literal to {@link #NOT_SET}. Any other or
     * {@code null} literal yields an empty result so the caller can fall back to {@link #NOT_SET}.
     *
     * @param wireValue the privacy wire literal, possibly {@code null}
     * @return an {@link Optional} carrying the matching variant, or empty when the literal is
     *         {@code null} or unrecognised
     */
    public static Optional<CtwaDataSharingSetting> ofWire(String wireValue) {
        return switch (wireValue) {
            case "true" -> Optional.of(ENABLED);
            case "false" -> Optional.of(DISABLED);
            case "notset" -> Optional.of(NOT_SET);
            case null, default -> Optional.empty();
        };
    }
}

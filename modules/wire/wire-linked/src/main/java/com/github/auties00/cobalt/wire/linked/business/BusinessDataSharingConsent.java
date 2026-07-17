package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Optional;

/**
 * Tri-state of the SMB-data-sharing-with-Meta consent value persisted by
 * WhatsApp Business accounts.
 *
 * <p>Through the WhatsApp Business privacy settings, a small-business owner
 * can decide whether to share aggregated business data with Meta for
 * cross-product analytics. The decision is persisted on the relay as a
 * tri-state attribute on the {@code <smb_data_sharing_with_meta_consent>}
 * grandchild of the privacy IQ: an explicit opt-in
 * ({@link #TRUE}), an explicit opt-out ({@link #FALSE}), or a
 * never-prompted state ({@link #NOT_SET}) that surfaces the consent dialog
 * the next time the user opens the relevant settings panel.
 */
@ProtobufEnum
public enum BusinessDataSharingConsent {
    /**
     * The user has explicitly granted consent to share SMB data with Meta.
     * Wire value {@code "true"}.
     */
    TRUE(0, "true"),

    /**
     * The user has explicitly declined to share SMB data with Meta. Wire
     * value {@code "false"}.
     */
    FALSE(1, "false"),

    /**
     * The user has not yet been prompted to make a choice. The client
     * surfaces the consent dialog whenever this value is observed. Wire
     * value {@code "notset"}.
     */
    NOT_SET(2, "notset");

    /**
     * The protobuf wire-format index associated with this consent value.
     */
    final int index;

    /**
     * The literal value carried by the {@code value} attribute of the
     * {@code <smb_data_sharing_with_meta_consent>} child.
     */
    final String wireValue;

    /**
     * Constructs a new {@code BusinessDataSharingConsent} with the supplied
     * protobuf index and wire-side string.
     *
     * @param index     the protobuf wire-format index
     * @param wireValue the literal wire string; never {@code null}
     */
    BusinessDataSharingConsent(@ProtobufEnumIndex int index, String wireValue) {
        this.index = index;
        this.wireValue = wireValue;
    }

    /**
     * Returns the protobuf wire-format index associated with this value.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the wire-side string carried on the
     * {@code <smb_data_sharing_with_meta_consent>} child.
     *
     * @return the wire string; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a {@code BusinessDataSharingConsent} from the wire literal.
     *
     * @param wireValue the wire literal, possibly {@code null}
     * @return an {@link Optional} containing the matching constant, or empty
     *         when the literal is {@code null} or unrecognised
     */
    public static Optional<BusinessDataSharingConsent> ofWire(String wireValue) {
        if (wireValue == null) {
            return Optional.empty();
        }
        for (var value : values()) {
            if (value.wireValue.equals(wireValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}

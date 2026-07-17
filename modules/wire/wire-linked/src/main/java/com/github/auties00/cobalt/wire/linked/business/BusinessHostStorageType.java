package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Optional;

/**
 * Identifies where a WhatsApp account's data is hosted and processed, as
 * carried by the privacy-mode triplet on the {@code <biz>} envelope of inbound
 * messages and notifications and as cached on the verified-name certificate.
 *
 * <p>This is the message-level {@code host_storage} encoding (WhatsApp's
 * {@code HostStorageEnumType}), and is deliberately distinct from the protobuf
 * {@link BusinessIdentityHostStorageType}: here {@link #ON_PREMISE} is the wire
 * value {@code 1} and {@link #FACEBOOK} is {@code 2}, whereas the
 * {@code BizIdentityInfo.HostStorageType} encoding numbers the same concepts
 * {@code 0} and {@code 1}. A {@link #FACEBOOK} value marks a Meta-hosted
 * (cross-Meta interoperable) account; it is the signal the country and
 * Terms-of-Service inbound-message gating checks read from a contact's privacy
 * mode.
 */
@ProtobufEnum
public enum BusinessHostStorageType {
    /**
     * Data is hosted and processed on the business's own infrastructure
     * (on-premises deployment of the WhatsApp Business API); wire value
     * {@code 1}.
     */
    ON_PREMISE(1),

    /**
     * Data is hosted and processed on Meta-managed infrastructure (Cloud API
     * deployment), marking a cross-Meta interoperable account; wire value
     * {@code 2}.
     */
    FACEBOOK(2);

    /**
     * The protobuf wire index for this constant.
     */
    final int index;

    /**
     * Constructs a constant bound to the given protobuf wire index.
     *
     * @param index the protobuf wire index
     */
    BusinessHostStorageType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the constant matching the given wire index, if any.
     *
     * <p>This resolves the raw {@code host_storage} attribute parsed from a
     * {@code <biz>} stanza into the typed constant; an unrecognised index
     * yields an empty result rather than throwing.
     *
     * @param index the wire index to resolve
     * @return an {@link Optional} carrying the matching constant, or empty when
     *         no constant has the given index
     */
    public static Optional<BusinessHostStorageType> ofIndex(int index) {
        return Arrays.stream(values())
                .filter(value -> value.index == index)
                .findFirst();
    }

    /**
     * Returns the protobuf wire index of this constant.
     *
     * @return the protobuf index
     */
    public int index() {
        return index;
    }
}

package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Identifies where a business's data is hosted and processed within a
 * {@link BusinessIdentityInfo} privacy-mode triplet, distinguishing
 * on-premises deployments from Meta-managed Cloud API deployments.
 *
 * <p>This is the {@code BizIdentityInfo.HostStorageType} protobuf encoding, and
 * is deliberately distinct from the message-level {@link BusinessHostStorageType}:
 * here {@link #ON_PREMISE} is the wire value {@code 0} and {@link #FACEBOOK} is
 * {@code 1}, whereas the message-level {@code host_storage} attribute numbers
 * the same concepts {@code 1} and {@code 2}.
 */
@ProtobufEnum(name = "BizIdentityInfo.HostStorageType")
public enum BusinessIdentityHostStorageType {
    /**
     * Data is hosted and processed on the business's own infrastructure
     * (on-premises deployment of the WhatsApp Business API); wire value
     * {@code 0}.
     */
    ON_PREMISE(0),

    /**
     * Data is hosted and processed on Meta-managed infrastructure (Cloud API
     * deployment); wire value {@code 1}.
     */
    FACEBOOK(1);

    /**
     * The protobuf wire index for this constant.
     */
    final int index;

    /**
     * Constructs a constant bound to the given protobuf wire index.
     *
     * @param index the protobuf wire index
     */
    BusinessIdentityHostStorageType(@ProtobufEnumIndex int index) {
        this.index = index;
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

package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of business privacy
 */
public enum BusinessPrivacyStatus implements ProtobufEnum {
    /**
     * End-to-end encryption
     */
    E2EE(0),
    /**
     * Bsp encryption
     */
    BSP(1),
    /**
     * Facebook encryption
     */
    FACEBOOK(2),
    /**
     * Facebook and bsp encryption
     */
    BSP_AND_FB(3);

    final int index;

    BusinessPrivacyStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
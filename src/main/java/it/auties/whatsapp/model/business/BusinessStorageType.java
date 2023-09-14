package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of hosting that a Whatsapp
 * business account can use
 */
public enum BusinessStorageType implements ProtobufEnum {
    /**
     * Hosted on a private server ("On-Premise")
     */
    SELF_HOSTED(0),
    /**
     * Hosted by facebook
     */
    FACEBOOK(1);

    final int index;

    BusinessStorageType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
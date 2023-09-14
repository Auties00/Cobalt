package it.auties.whatsapp.model.interactive;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of surfaces that a
 * {@link InteractiveShop} can have
 */
public enum InteractiveShopSurfaceType implements ProtobufEnum {
    /**
     * Unknown
     */
    UNKNOWN_SURFACE(0),
    /**
     * Facebook
     */
    FACEBOOK(1),
    /**
     * Instagram
     */
    INSTAGRAM(2),
    /**
     * Whatsapp
     */
    WHATSAPP(3);

    final int index;

    InteractiveShopSurfaceType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}

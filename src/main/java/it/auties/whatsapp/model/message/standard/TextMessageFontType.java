package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various types of fonts that a
 * {@link TextMessage} supports. Not all clients currently display all fonts correctly.
 */
public enum TextMessageFontType implements ProtobufEnum {
    /**
     * Sans Serif
     */
    SANS_SERIF(0),
    /**
     * Serif
     */
    SERIF(1),
    /**
     * Norican Regular
     */
    NORICAN_REGULAR(2),
    /**
     * Brydan Write
     */
    BRYNDAN_WRITE(3),
    /**
     * Bebasnue Regular
     */
    BEBASNEUE_REGULAR(4),
    /**
     * Oswald Heavy
     */
    OSWALD_HEAVY(5);

    final int index;

    TextMessageFontType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }
}

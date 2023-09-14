package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various sources from where a gif can come
 * from
 */
public enum VideoMessageAttribution implements ProtobufEnum {
    /**
     * No source was specified
     */
    NONE(0),
    /**
     * Giphy
     */
    GIPHY(1),
    /**
     * Tenor
     */
    TENOR(2);

    final int index;

    VideoMessageAttribution(int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }
}

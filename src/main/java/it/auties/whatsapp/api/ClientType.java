package it.auties.whatsapp.api;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The constants of this enumerated type describe the various types of API that can be used to make
 * {@link Whatsapp} work
 */
@ProtobufEnum
public enum ClientType {
    /**
     * A standalone client that requires the QR code to be scanned by its companion on log-in Reversed
     * from <a href="https://web.whatsapp.com">Whatsapp Web Client</a>
     */
    WEB(0),
    /**
     * A standalone client that requires an SMS code sent to the companion's phone number on log-in
     * Reversed from <a href="https://github.com/tgalal/yowsup/issues/2910">KaiOS Mobile App</a>
     */
    MOBILE(1);

    final int index;

    ClientType(@ProtobufEnumIndex int index) {
        this.index = index;
    }
}

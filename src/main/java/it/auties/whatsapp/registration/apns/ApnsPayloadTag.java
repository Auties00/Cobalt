package it.auties.whatsapp.registration.apns;

import java.util.Arrays;

public enum ApnsPayloadTag {
    KEEP_ALIVE_SEND(0x0c),
    KEEP_ALIVE_ACK(0x0d),
    CONNECT(7),
    READY(8),
    FILTER(9),
    NOTIFICATION(0xa),
    ACK(0xb),
    STATE(0x14),
    PUB_SUB(0x1d),
    PUB_SUB_RESPONSE(0x20),
    NO_STORAGE(0xe),
    GET_TOKEN(0x11),
    TOKEN_RESPONSE(0x12);

    private final int value;

    ApnsPayloadTag(int value) {
        this.value = value;
    }

    static ApnsPayloadTag of(int value) {
        return Arrays.stream(values())
                .filter(entry -> entry.value() == value)
                .findFirst()
                .orElseThrow();
    }

    int value() {
        return value;
    }
}

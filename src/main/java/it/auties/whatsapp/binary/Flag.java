package it.auties.whatsapp.binary;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Accessors(fluent = true)
public enum Flag {
    AVAILABLE((byte) 160),
    IGNORE((byte) (1 << 7)),
    ACKNOWLEDGE((byte) (1 << 6)),
    UNAVAILABLE((byte) (1 << 4)),
    EXPIRES((byte) (1 << 3)),
    COMPOSING((byte) (1 << 2)),
    RECORDING((byte) (1 << 2)),
    PAUSED((byte) (1 << 2));

    @Getter
    private final byte data;
}

package it.auties.whatsapp4j.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Accessors(fluent = true)
public enum UserPresence {
    AVAILABLE("available", Flag.AVAILABLE),
    UNAVAILABLE("unavailable", Flag.UNAVAILABLE),
    COMPOSING("composing", Flag.COMPOSING),
    RECORDING("recording", Flag.RECORDING),
    PAUSED("paused", Flag.PAUSED);

    @Getter
    private final String content;
    @Getter
    private final byte data;
}

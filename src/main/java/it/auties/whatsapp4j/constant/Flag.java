package it.auties.whatsapp4j.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Flag {
    public final byte AVAILABLE = (byte) 160;
    public final byte IGNORE = (byte) (1 << 7);
    public final byte ACKNOLEDGE = (byte) (1 << 6);
    public final byte UNAVAILABLE = (byte) (1 << 4);
    public final byte EXPIRES = (byte) (1 << 3);
    public final byte COMPOSING = (byte) (1 << 2);
    public final byte RECORDING = (byte) (1 << 2);
    public final byte PAUSED = (byte) (1 << 2);
}

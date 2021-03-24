package it.auties.whatsapp4j.binary;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various tags used by an encrypted {@link BinaryArray}.
 * These tags were extracted from JS code found at https://web.whatsapp.com/.
 * It is important to remember that these are unsigned ints, not bytes.
 * For this reason when comparing a byte with one of these tags it is important to convert said byte to an unsigned int using {@link Byte#toUnsignedInt(byte)}.
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum BinaryTag {
    LIST_EMPTY(0),
    STREAM_END(2),
    DICTIONARY_0(236),
    DICTIONARY_1(237),
    DICTIONARY_2(238),
    DICTIONARY_3(239),
    LIST_8(248),
    LIST_16(249),
    JID_PAIR(250),
    HEX_8(251),
    BINARY_8(252),
    BINARY_20(253),
    BINARY_32(254),
    NIBBLE_8(255),
    SINGLE_BYTE_MAX (256),
    PACKED_MAX(254);

    @Getter
    private final int data;

    /**
     * Returns the {@link BinaryTag} whose content matches {@code data}
     *
     * @param data the data to search
     * @throws IllegalArgumentException if no {@link BinaryTag} matches {@code data}
     * @return the matching {@link BinaryTag}
     */
    public static @NonNull BinaryTag forData(int data){
        return Arrays.stream(values()).filter(entry -> entry.data() == data).findAny().orElseThrow(() -> new IllegalArgumentException("Tag#forData: cannot convert %s to any tag".formatted(data)));
    }
}

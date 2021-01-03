package it.auties.whatsapp4j.constant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

@AllArgsConstructor
@Accessors(fluent = true)
public enum Tag {
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

    public static Tag forName(@NotNull String tag){
        return Arrays.stream(values()).filter(entry -> entry.name().equals(tag)).findAny().orElseThrow();
    }

    public static Tag forData(int data){
        System.out.println(data);
        return Arrays.stream(values()).filter(entry -> entry.data() == data).findAny().orElseThrow();
    }
}

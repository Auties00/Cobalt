package it.auties.whatsapp.binary;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Accessors(fluent = true)
public enum Tag {
  UNKNOWN(-1),
  LIST_EMPTY(0),
  STREAM_END(2),
  DICTIONARY_0(236),
  DICTIONARY_1(237),
  DICTIONARY_2(238),
  DICTIONARY_3(239),
  COMPANION_JID(247),
  LIST_8(248),
  LIST_16(249),
  JID_PAIR(250),
  HEX_8(251),
  BINARY_8(252),
  BINARY_20(253),
  BINARY_32(254),
  NIBBLE_8(255),
  SINGLE_BYTE_MAX(256),
  PACKED_MAX(254);

  @Getter
  private final int data;

  public static Tag of(int data) {
    return Arrays.stream(values())
        .filter(entry -> entry.data() == data)
        .findAny()
        .orElse(UNKNOWN);
  }

  public boolean contentEquals(int number) {
    return number == this.data();
  }
}

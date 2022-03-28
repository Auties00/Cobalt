package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.bytes.Bytes;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class MutationSync implements ParsableMutation {
  @JsonProperty("1")
  @JsonPropertyDescription("SyncdMutationSyncdOperation")
  private Operation operation;

  @JsonProperty("2")
  @JsonPropertyDescription("SyncdRecord")
  private RecordSync record;

  @Override
  public byte[] id() {
    return record.id();
  }

  @Override
  public Bytes valueBlob() {
    return record.valueBlob();
  }

  @Override
  public Bytes indexBlob() {
    return record.indexBlob();
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum Operation {
    SET(0, (byte) 0x01),
    REMOVE(1, (byte) 0x02);

    @Getter
    private final int index;

    @Getter
    private final byte value;

    @JsonCreator
    public static Operation forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}

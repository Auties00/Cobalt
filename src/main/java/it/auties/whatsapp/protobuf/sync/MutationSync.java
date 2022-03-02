package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.buffer.ByteBuffer;
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
  public ByteBuffer valueBlob() {
    return record.valueBlob();
  }

  @Override
  public ByteBuffer indexBlob() {
    return record.indexBlob();
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum Operation {
    SET(0),
    REMOVE(1);

    @Getter
    private final int index;

    @JsonCreator
    public static Operation forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}

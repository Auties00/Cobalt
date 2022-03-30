package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class RecordSync {
  @JsonProperty("1")
  @JsonPropertyDescription("SyncdIndex")
  private IndexSync index;

  @JsonProperty("2")
  @JsonPropertyDescription("SyncdValue")
  private ValueSync value;

  @JsonProperty("3")
  @JsonPropertyDescription("KeyId")
  private KeyId keyId;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum Operation {
      SET(0, (byte) 0x01),
      REMOVE(1, (byte) 0x02);

      @Getter
      private final int index;

      @Getter
      private final byte value;
  }
}

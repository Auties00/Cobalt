package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("SyncdRecord")
public final class RecordSync implements ProtobufMessage, Syncable {
  @ProtobufProperty(index = 1, type = MESSAGE, implementation = IndexSync.class)
  private IndexSync index;

  @ProtobufProperty(index = 2, type = MESSAGE, implementation = ValueSync.class)
  private ValueSync value;

  @ProtobufProperty(index = 3, type = MESSAGE, implementation = KeyId.class)
  private KeyId keyId;

  @Override
  public Operation operation() {
    return Operation.SET;
  }

  @Override
  public RecordSync record() {
    return this;
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  @ProtobufName("SyncdOperation")
  public enum Operation implements ProtobufMessage {

    SET(0, ((byte) (0x1))),
    REMOVE(1, ((byte) (0x2)));
    @Getter
    private final int index;

    @Getter
    private final byte content;

    @JsonCreator
    public static Operation of(int index) {
      return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst()
          .orElse(null);
    }
  }
}
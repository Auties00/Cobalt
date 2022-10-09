package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class RecordSync implements ProtobufMessage, Syncable {
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = IndexSync.class)
    private IndexSync index;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ValueSync.class)
    private ValueSync value;

    @ProtobufProperty(index = 3, type = MESSAGE, implementation = KeyId.class)
    private KeyId keyId;

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum Operation implements ProtobufMessage {
        SET(0, (byte) 0x01),
        REMOVE(1, (byte) 0x02);

        @Getter
        private final int index;

        @Getter
        private final byte content;

        @JsonCreator
        public static Operation forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Override
    public Operation operation() {
        return Operation.SET;
    }

    @Override
    public RecordSync record() {
        return this;
    }
}

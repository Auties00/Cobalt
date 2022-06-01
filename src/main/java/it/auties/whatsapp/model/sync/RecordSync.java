package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class RecordSync implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = IndexSync.class)
    private IndexSync index;

    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ValueSync.class)
    private ValueSync value;

    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = KeyId.class)
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
}

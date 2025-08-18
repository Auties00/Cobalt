package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "SyncdRecord")
public record RecordSync(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        IndexSync index,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        ValueSync value,
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        KeyId keyId
) implements Syncable {
    @Override
    public Operation operation() {
        return Operation.SET;
    }

    @Override
    public RecordSync record() {
        return this;
    }

    @ProtobufEnum
    public enum Operation {
        SET(0, ((byte) (0x1))),
        REMOVE(1, ((byte) (0x2)));

        final int index;
        private final byte content;

        Operation(@ProtobufEnumIndex int index, byte content) {
            this.index = index;
            this.content = content;
        }

        public int index() {
            return index;
        }

        public byte content() {
            return content;
        }
    }
}
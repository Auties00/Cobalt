package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessageName("SyncdRecord")
public record RecordSync(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        IndexSync index,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        ValueSync value,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        KeyId keyId
) implements ProtobufMessage, Syncable {
    @Override
    public Operation operation() {
        return Operation.SET;
    }

    @Override
    public RecordSync record() {
        return this;
    }

    public enum Operation implements ProtobufEnum {
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
package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;

import java.util.Optional;

import static it.auties.protobuf.model.ProtobufType.OBJECT;
import static it.auties.protobuf.model.ProtobufType.STRING;

@ProtobufMessageName("MediaRetryNotification")
public record MediaRetryNotification(@ProtobufProperty(index = 1, type = STRING) String stanzaId,
                                     @ProtobufProperty(index = 2, type = STRING) Optional<String> directPath,
                                     @ProtobufProperty(index = 3, type = OBJECT) MediaRetryNotificationResultType result) implements ProtobufMessage {

    public enum MediaRetryNotificationResultType implements ProtobufEnum {

        GENERAL_ERROR(0),
        SUCCESS(1),
        NOT_FOUND(2),
        DECRYPTION_ERROR(3);

        MediaRetryNotificationResultType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }
}

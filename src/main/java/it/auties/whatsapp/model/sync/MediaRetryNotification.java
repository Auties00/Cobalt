package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import java.util.Optional;

import static it.auties.protobuf.model.ProtobufType.ENUM;
import static it.auties.protobuf.model.ProtobufType.STRING;

@ProtobufMessage(name = "MediaRetryNotification")
public record MediaRetryNotification(@ProtobufProperty(index = 1, type = STRING) String stanzaId,
                                     @ProtobufProperty(index = 2, type = STRING) Optional<String> directPath,
                                     @ProtobufProperty(index = 3, type = ENUM) MediaRetryNotificationResultType result) {

    @ProtobufEnum
    public enum MediaRetryNotificationResultType {
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

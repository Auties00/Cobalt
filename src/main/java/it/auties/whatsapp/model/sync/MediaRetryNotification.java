package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class MediaRetryNotification implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING)
    private String messageId;

    @ProtobufProperty(index = 2, type = STRING)
    private String directPath;

    @ProtobufProperty(index = 3, type = MESSAGE, implementation = MediaRetryNotification.Result.class)
    private Result result;

    @AllArgsConstructor
    @Accessors(fluent = true)
    @ProtobufName("ResultType")
    public enum Result implements ProtobufMessage {
        GENERAL_ERROR(0),
        SUCCESS(1),
        NOT_FOUND(2),
        DECRYPTION_ERROR(3);
        
        @Getter
        private final int index;

        @JsonCreator
        public static Result of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }
}
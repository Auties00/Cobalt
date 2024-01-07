package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.util.*;

public record NewsletterReactionSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        Type value,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        List<String> blockedCodes,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
        OptionalLong enabledTimestampSeconds
) implements ProtobufMessage {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public NewsletterReactionSettings(Type value, @JsonProperty("blocked_codes") List<String> blockedCodes, @JsonProperty("enabled_ts_sec") Long enabledTimestampSeconds) {
        this(
                value,
                Objects.requireNonNullElseGet(blockedCodes, ArrayList::new),
                Clock.parseTimestamp(enabledTimestampSeconds)
        );
    }

    public enum Type implements ProtobufEnum {
        UNKNOWN(0),
        ALL(1),
        BASIC(2),
        NONE(3),
        BLOCKLIST(4);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }

        public static Type of(String name) {
            return Arrays.stream(values())
                    .filter(entry -> entry.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(UNKNOWN);
        }
    }
}

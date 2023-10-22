package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.util.Clock;

import java.util.*;

public record NewsletterReactionSettings(Type value, List<String> blockedCodes, OptionalLong enabledTimestampSeconds) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public NewsletterReactionSettings(Type value, @JsonProperty("blocked_codes") List<String> blockedCodes, @JsonProperty("enabled_ts_sec") Long enabledTimestampSeconds) {
        this(
                value,
                Objects.requireNonNullElseGet(blockedCodes, ArrayList::new),
                Clock.parseTimestamp(enabledTimestampSeconds)
        );
    }

    public enum Type {
        UNKNOWN,
        ALL,
        BASIC,
        NONE,
        BLOCKLIST;

        public static Type of(String name) {
            return Arrays.stream(values())
                    .filter(entry -> entry.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(UNKNOWN);
        }
    }
}

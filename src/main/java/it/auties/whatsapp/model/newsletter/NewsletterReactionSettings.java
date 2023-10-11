package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

public record NewsletterReactionSettings(Type value, @JsonProperty("blocked_codes") List<String> blockedCodes,
                                         @JsonProperty("enabled_ts_sec") long enabledTimestampSeconds) {
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

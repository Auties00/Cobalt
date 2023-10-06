package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NewsletterReactionSettings(String value, @JsonProperty("blocked_codes") List<String> blockedCodes,
                                         @JsonProperty("enabled_ts_sec") long enabledTimestampSeconds) {

}

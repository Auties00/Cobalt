package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

public record NewsletterDescription(String id, String text, @JsonProperty("update_time") long updateTimeSeconds) {
    public Optional<ZonedDateTime> updateTime() {
        return Clock.parseSeconds(updateTimeSeconds);
    }
}

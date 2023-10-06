package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalInt;

public record NewsletterMetadata(
        NewsletterName name,
        NewsletterDescription description,
        Optional<NewsletterPicture> picture,
        Optional<String> handle,
        Optional<NewsletterSettings> settings,
        String invite,
        @JsonProperty("subscribers_count") OptionalInt subscribers,
        String verification,
        @JsonProperty("creation_time") long creationTimestampSeconds
) {
    public Optional<ZonedDateTime> creationTimestamp() {
        return Clock.parseSeconds(creationTimestampSeconds);
    }
}

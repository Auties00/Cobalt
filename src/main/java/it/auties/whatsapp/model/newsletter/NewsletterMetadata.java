package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record NewsletterMetadata(
        NewsletterName name,
        NewsletterDescription description,
        Optional<NewsletterPicture> picture,
        Optional<String> handle,
        Optional<NewsletterSettings> settings,
        String invite,
        OptionalLong subscribers,
        boolean verification,
        OptionalLong creationTimestampSeconds
) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    NewsletterMetadata(
            NewsletterName name,
            NewsletterDescription description,
            NewsletterPicture picture,
            String handle,
            NewsletterSettings settings,
            String invite,
            @JsonProperty("subscribers_count")
            Long subscribers,
            String verification,
            @JsonProperty("creation_time")
            Long creationTimestampSeconds
    ) {
        this(
                name,
                description,
                Optional.ofNullable(picture),
                Optional.ofNullable(handle),
                Optional.ofNullable(settings),
                invite,
                subscribers == null ? OptionalLong.empty() : OptionalLong.of(subscribers),
                Objects.equals(verification, "VERIFIED"),
                creationTimestampSeconds == null ? OptionalLong.empty() : OptionalLong.of(creationTimestampSeconds)
        );
    }

    public Optional<ZonedDateTime> creationTimestamp() {
        return Clock.parseSeconds(creationTimestampSeconds.orElse(0L));
    }
}

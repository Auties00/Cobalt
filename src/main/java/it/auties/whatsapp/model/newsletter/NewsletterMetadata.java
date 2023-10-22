package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalLong;

public record NewsletterMetadata(
        Optional<NewsletterName> name,
        Optional<NewsletterDescription> description,
        Optional<NewsletterPicture> picture,
        Optional<String> handle,
        Optional<NewsletterSettings> settings,
        Optional<String> invite,
        OptionalLong subscribers,
        Optional<Boolean> verification,
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
                Optional.ofNullable(name),
                Optional.ofNullable(description),
                Optional.ofNullable(picture),
                Optional.ofNullable(handle),
                Optional.ofNullable(settings),
                Optional.ofNullable(invite),
                subscribers == null ? OptionalLong.empty() : OptionalLong.of(subscribers),
                verification == null ? Optional.empty() : Optional.of(verification.equals("VERIFIED")),
                creationTimestampSeconds == null ? OptionalLong.empty() : OptionalLong.of(creationTimestampSeconds)
        );
    }

    public Optional<ZonedDateTime> creationTimestamp() {
        return Clock.parseSeconds(creationTimestampSeconds.orElse(0L));
    }
}

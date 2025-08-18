package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalLong;

@ProtobufMessage
public record NewsletterMetadata(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        Optional<NewsletterName> name,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        Optional<NewsletterDescription> description,
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        Optional<NewsletterPicture> picture,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        Optional<String> handle,
        @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
        Optional<NewsletterSettings> settings,
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        Optional<String> invite,
        @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
        Optional<Boolean> verification,
        @ProtobufProperty(index = 8, type = ProtobufType.UINT64)
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
                verification == null ? Optional.empty() : Optional.of(verification.equals("VERIFIED")),
                creationTimestampSeconds == null ? OptionalLong.empty() : OptionalLong.of(creationTimestampSeconds)
        );
    }

    public Optional<ZonedDateTime> creationTimestamp() {
        return Clock.parseSeconds(creationTimestampSeconds.orElse(0L));
    }
}

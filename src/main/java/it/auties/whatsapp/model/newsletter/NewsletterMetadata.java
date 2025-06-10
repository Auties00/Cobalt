package it.auties.whatsapp.model.newsletter;

import io.avaje.jsonb.Json;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

@ProtobufMessage
public final class NewsletterMetadata {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final NewsletterName name;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final NewsletterDescription description;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final NewsletterPicture picture;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String handle;

    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final NewsletterSettings settings;

    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String invite;

    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    @Json.Property("verification")
    final String verification;

    @ProtobufProperty(index = 8, type = ProtobufType.UINT64)
    @Json.Ignore
    final long creationTimestampSeconds;

    NewsletterMetadata(NewsletterName name, NewsletterDescription description, NewsletterPicture picture, String handle, NewsletterSettings settings, String invite, String verification, long creationTimestampSeconds) {
        this.name = name;
        this.description = description;
        this.picture = picture;
        this.handle = handle;
        this.settings = settings;
        this.invite = invite;
        this.verification = verification;
        this.creationTimestampSeconds = creationTimestampSeconds;
    }

    public NewsletterMetadata(NewsletterName name, NewsletterDescription description, NewsletterPicture picture, String handle, NewsletterSettings settings, String invite, boolean verification, long creationTimestampSeconds) {
        this.name = name;
        this.description = description;
        this.picture = picture;
        this.handle = handle;
        this.settings = settings;
        this.invite = invite;
        this.verification = verification ? "ON" : "OFF";
        this.creationTimestampSeconds = creationTimestampSeconds;
    }

    @Json.Property("creation_time")
    public OptionalLong creationTimestampSeconds() {
        return Clock.parseTimestamp(creationTimestampSeconds);
    }

    public Optional<ZonedDateTime> creationTimestamp() {
        return Clock.parseSeconds(creationTimestampSeconds);
    }

    @Json.Property("name")
    public Optional<NewsletterName> name() {
        return Optional.ofNullable(name);
    }

    @Json.Property("description")
    public Optional<NewsletterDescription> description() {
        return Optional.ofNullable(description);
    }

    @Json.Property("picture")
    public Optional<NewsletterPicture> picture() {
        return Optional.ofNullable(picture);
    }

    @Json.Property("handle")
    public Optional<String> handle() {
        return Optional.ofNullable(handle);
    }

    @Json.Property("settings")
    public Optional<NewsletterSettings> settings() {
        return Optional.ofNullable(settings);
    }

    @Json.Property("invite")
    public Optional<String> invite() {
        return Optional.ofNullable(invite);
    }

    public boolean verification() {
        return Objects.equals(verification, "VERIFIED");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterMetadata that
                && creationTimestampSeconds == that.creationTimestampSeconds &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(picture, that.picture) &&
                Objects.equals(handle, that.handle) &&
                Objects.equals(settings, that.settings) &&
                Objects.equals(invite, that.invite) &&
                Objects.equals(verification, that.verification);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, picture, handle, settings, invite, verification, creationTimestampSeconds);
    }

    @Override
    public String toString() {
        return "NewsletterMetadata[" +
                "name=" + name + ", " +
                "description=" + description + ", " +
                "picture=" + picture + ", " +
                "handle=" + handle + ", " +
                "settings=" + settings + ", " +
                "invite=" + invite + ", " +
                "verification=" + verification + ", " +
                "creationTimestampSeconds=" + creationTimestampSeconds + ']';
    }

}

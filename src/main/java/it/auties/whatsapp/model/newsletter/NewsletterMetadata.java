package it.auties.whatsapp.model.newsletter;

import com.alibaba.fastjson2.JSONObject;
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
    final NewsletterVerification verification;

    @ProtobufProperty(index = 8, type = ProtobufType.UINT64)
    final long creationTimestampSeconds;

    NewsletterMetadata(NewsletterName name, NewsletterDescription description, NewsletterPicture picture, String handle, NewsletterSettings settings, String invite, NewsletterVerification verification, long creationTimestampSeconds) {
        this.name = name;
        this.description = description;
        this.picture = picture;
        this.handle = handle;
        this.settings = settings;
        this.invite = invite;
        this.verification = verification;
        this.creationTimestampSeconds = creationTimestampSeconds;
    }

    public static Optional<NewsletterMetadata> ofJson(JSONObject jsonObject) {
        if(jsonObject == null) {
            return Optional.empty();
        }

        var name = NewsletterName.ofJson(jsonObject.getJSONObject("name"))
                .orElse(null);
        var description = NewsletterDescription.ofJson(jsonObject.getJSONObject("description"))
                .orElse(null);
        var picture = NewsletterPicture.ofJson(jsonObject.getJSONObject("picture"))
                .orElse(null);
        var handle = jsonObject.getString("handle");
        var settings = NewsletterSettings.ofJson(jsonObject.getJSONObject("settings"))
                .orElse(null);
        var invite = jsonObject.getString("invite");
        var verification = NewsletterVerification.ofJson(jsonObject.getString("verification"))
                .orElse(null);
        var creationTimestampSeconds = jsonObject.getLongValue("creation_timestamp", 0);
        var result = new NewsletterMetadata(name, description, picture, handle, settings, invite, verification, creationTimestampSeconds);
        return Optional.of(result);
    }

    public OptionalLong creationTimestampSeconds() {
        return Clock.parseTimestamp(creationTimestampSeconds);
    }

    public Optional<ZonedDateTime> creationTimestamp() {
        return Clock.parseSeconds(creationTimestampSeconds);
    }

    public Optional<NewsletterName> name() {
        return Optional.ofNullable(name);
    }

    public Optional<NewsletterDescription> description() {
        return Optional.ofNullable(description);
    }

    public Optional<NewsletterPicture> picture() {
        return Optional.ofNullable(picture);
    }

    public Optional<String> handle() {
        return Optional.ofNullable(handle);
    }

    public Optional<NewsletterSettings> settings() {
        return Optional.ofNullable(settings);
    }

    public Optional<String> invite() {
        return Optional.ofNullable(invite);
    }

    public Optional<NewsletterVerification> verification() {
        return Optional.ofNullable(verification);
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

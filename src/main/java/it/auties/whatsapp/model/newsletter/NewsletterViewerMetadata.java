package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Map;
import java.util.Objects;

@ProtobufMessage
public final class NewsletterViewerMetadata {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    private boolean mute;
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    private NewsletterViewerRole role;

    public NewsletterViewerMetadata(boolean mute, NewsletterViewerRole role) {
        this.mute = mute;
        this.role = role;
    }

    @JsonCreator
    NewsletterViewerMetadata(Map<String, ?> json) {
        this.mute = switch (json.get("mute")) {
            case Boolean bool -> bool;
            case String string -> Objects.equals(string, "ON");
            default -> false;
        };
        this.role = switch (json.get("role")) {
            case String string -> NewsletterViewerRole.of(string);
            case Integer index -> NewsletterViewerRole.of(index);
            default -> NewsletterViewerRole.UNKNOWN;
        };
    }

    public boolean mute() {
        return mute;
    }

    public NewsletterViewerRole role() {
        return role;
    }

    public NewsletterViewerMetadata setMute(boolean mute) {
        this.mute = mute;
        return this;
    }

    public NewsletterViewerMetadata setRole(NewsletterViewerRole role) {
        this.role = role;
        return this;
    }

    @Override
    public String toString() {
        return "NewsletterViewerMetadata{" +
                "mute=" + mute +
                ", role=" + role +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterViewerMetadata that
                && mute == that.mute
                && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mute, role);
    }
}

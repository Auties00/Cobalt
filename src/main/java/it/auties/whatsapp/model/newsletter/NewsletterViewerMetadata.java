package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.Map;
import java.util.Objects;

public final class NewsletterViewerMetadata implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    private boolean mute;
    @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
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
    public int hashCode() {
        return Objects.hash(mute, role);
    }
}

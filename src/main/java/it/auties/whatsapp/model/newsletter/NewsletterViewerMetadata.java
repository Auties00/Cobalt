package it.auties.whatsapp.model.newsletter;

import io.avaje.jsonb.Json;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Json
@ProtobufMessage
public final class NewsletterViewerMetadata {
    private static final Map<String, NewsletterViewerRole> PRETTY_NAME_TO_ROLE = Arrays.stream(NewsletterViewerRole.values())
            .collect(Collectors.toMap(entry -> entry.name().toLowerCase(), role -> role));

    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    boolean mute;

    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    NewsletterViewerRole role;

    public NewsletterViewerMetadata(boolean mute, NewsletterViewerRole role) {
        this.mute = mute;
        this.role = Objects.requireNonNullElse(role, NewsletterViewerRole.UNKNOWN);
    }

    @Json.Creator
    NewsletterViewerMetadata(@Json.Unmapped Map<String, Object> json) {
        this.mute = switch (json.get("mute")) {
            case Boolean bool -> bool;
            case String string -> string.equals("ON");
            default -> false;
        };
        this.role = switch (json.get("role")) {
            case String string -> PRETTY_NAME_TO_ROLE.getOrDefault(string.toLowerCase(), NewsletterViewerRole.UNKNOWN);
            case Integer index -> {
                var values = NewsletterViewerRole.values();
                yield index >= values.length ? NewsletterViewerRole.UNKNOWN : values[index];
            }
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

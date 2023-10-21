package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;
import java.util.Objects;

public final class NewsletterViewerMetadata {
    private boolean mute;
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
    public int hashCode() {
        return Objects.hash(mute, role);
    }
}

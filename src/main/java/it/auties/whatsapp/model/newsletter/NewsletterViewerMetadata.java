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
    NewsletterViewerMetadata(Map<String, String> json) {
        this(Objects.equals(json.get("mute"), "ON"), NewsletterViewerRole.of(json.get("role")));
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

package it.auties.whatsapp.model.newsletter;

import com.alibaba.fastjson2.JSONObject;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

@ProtobufMessage
public final class NewsletterViewerMetadata {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    boolean mute;

    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    NewsletterViewerRole role;

    public NewsletterViewerMetadata(boolean mute, NewsletterViewerRole role) {
        this.mute = mute;
        this.role = Objects.requireNonNullElse(role, NewsletterViewerRole.UNKNOWN);
    }

    public static Optional<NewsletterViewerMetadata> ofJson(JSONObject object) {
        var mute = switch (object.get("mute")) {
            case Boolean bool -> bool;
            case String string -> string.equals("ON");
            case null, default -> false;
        };
        var role = switch (object.get("role")) {
            case String string -> NewsletterViewerRole.of(string);
            case Integer index -> NewsletterViewerRole.of(index);
            default -> NewsletterViewerRole.UNKNOWN;
        };
        var result = new NewsletterViewerMetadata(mute, role);
        return Optional.of(result);
    }

    public boolean mute() {
        return mute;
    }

    public NewsletterViewerRole role() {
        return role;
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }

    public void setRole(NewsletterViewerRole role) {
        this.role = role;
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

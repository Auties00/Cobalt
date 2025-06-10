package it.auties.whatsapp.model.newsletter;

import io.avaje.jsonb.Json;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage
@Json
public final class NewsletterPicture {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    @Json.Property("id")
    final String id;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    @Json.Property("type")
    final String type;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    @Json.Property("direct_path")
    final String directPath;

    NewsletterPicture(String id, String type, String directPath) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.directPath = Objects.requireNonNull(directPath, "directPath cannot be null");
    }

    public String id() {
        return id;
    }

    public String type() {
        return type;
    }

    public String directPath() {
        return directPath;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterPicture that
                && Objects.equals(id, that.id)
                && Objects.equals(type, that.type)
                && Objects.equals(directPath, that.directPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, directPath);
    }

    @Override
    public String toString() {
        return "NewsletterPicture[" +
                "id=" + id +
                ", type=" + type +
                ", directPath=" + directPath +
                ']';
    }
}
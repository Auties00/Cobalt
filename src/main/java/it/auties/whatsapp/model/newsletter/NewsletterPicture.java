package it.auties.whatsapp.model.newsletter;

import com.alibaba.fastjson2.JSONObject;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

@ProtobufMessage
public final class NewsletterPicture {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String type;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String directPath;

    NewsletterPicture(String id, String type, String directPath) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.directPath = Objects.requireNonNull(directPath, "directPath cannot be null");
    }

    public static Optional<NewsletterPicture> ofJson(JSONObject jsonObject) {
        if(jsonObject == null) {
            return Optional.empty();
        }

        var id = jsonObject.getString("id");
        if(id == null) {
            return Optional.empty();
        }

        var directPath = jsonObject.getString("direct_path");
        if(directPath == null) {
            return Optional.empty();
        }

        var type = Objects.requireNonNullElse(jsonObject.getString("type"), "default");
        var result = new NewsletterPicture(id, type, directPath);
        return Optional.of(result);
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
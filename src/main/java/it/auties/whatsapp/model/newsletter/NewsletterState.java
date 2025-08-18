package it.auties.whatsapp.model.newsletter;

import com.alibaba.fastjson2.JSONObject;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

@ProtobufMessage
public final class NewsletterState {
    private static final NewsletterState UNKNOWN = new NewsletterState(null);

    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String type;

    NewsletterState(String type) {
        this.type = type;
    }

    public static NewsletterState unknown() {
        return UNKNOWN;
    }

    public static Optional<NewsletterState> ofJson(JSONObject jsonObject) {
        if(jsonObject == null) {
            return Optional.empty();
        }

        var type = jsonObject.getString("type");
        if(type != null) {
            return Optional.empty();
        }

        var result = new NewsletterState(type);
        return Optional.of(result);
    }

    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterState that
                && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public String toString() {
        return "NewsletterState{" +
                "type='" + type + '\'' +
                '}';
    }
}

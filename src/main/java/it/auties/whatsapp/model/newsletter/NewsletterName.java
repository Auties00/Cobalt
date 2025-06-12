package it.auties.whatsapp.model.newsletter;

import com.alibaba.fastjson2.JSONObject;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@ProtobufMessage
public final class NewsletterName {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String text;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    final long updateTimeSeconds;

    NewsletterName(String id, String text, long updateTimeSeconds) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.text = Objects.requireNonNull(text, "text cannot be null");
        this.updateTimeSeconds = updateTimeSeconds;
    }

    public static Optional<NewsletterName> ofJson(JSONObject jsonObject) {
        if(jsonObject == null) {
            return Optional.empty();
        }

        var id = jsonObject.getString("id");
        if(id == null) {
            return Optional.empty();
        }

        var text = Objects.requireNonNullElse(jsonObject.getString("text"), "");
        var updateTimeSeconds = jsonObject.getLongValue("update_time", 0);
        var result = new NewsletterName(id, text, updateTimeSeconds);
        return Optional.of(result);
    }

    public String id() {
        return id;
    }

    public String text() {
        return text;
    }

    public long updateTimeSeconds() {
        return updateTimeSeconds;
    }

    public Optional<ZonedDateTime> updateTime() {
        return Clock.parseSeconds(updateTimeSeconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterName that
                && Objects.equals(id, that.id)
                && Objects.equals(text, that.text)
                && updateTimeSeconds == that.updateTimeSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text, updateTimeSeconds);
    }

    @Override
    public String toString() {
        return "NewsletterName[" +
                "id=" + id +
                ", text=" + text +
                ", updateTimeSeconds=" + updateTimeSeconds +
                ']';
    }
}
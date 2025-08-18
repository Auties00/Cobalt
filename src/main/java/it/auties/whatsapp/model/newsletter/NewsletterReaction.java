package it.auties.whatsapp.model.newsletter;

import com.alibaba.fastjson2.JSONObject;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

@ProtobufMessage
public final class NewsletterReaction {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String content;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    long count;

    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    boolean fromMe;

    public NewsletterReaction(String content, long count, boolean fromMe) {
        this.content = Objects.requireNonNull(content, "content cannot be null");
        this.count = count;
        this.fromMe = fromMe;
    }

    public static Optional<NewsletterReaction> ofJson(JSONObject jsonObject) {
        if(jsonObject == null) {
            return Optional.empty();
        }

        var content = jsonObject.getString("content");
        if(content == null) {
            return Optional.empty();
        }

        var count = jsonObject.getLongValue("count", 0);
        var fromMe = jsonObject.getBooleanValue("fromMe", false);
        var result = new NewsletterReaction(content, count, fromMe);
        return Optional.of(result);
    }

    public String content() {
        return content;
    }

    public long count() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public boolean fromMe() {
        return fromMe;
    }

    public void setFromMe(boolean fromMe) {
        this.fromMe = fromMe;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NewsletterReaction
                that && Objects.equals(this.content(), that.content());
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "NewsletterReaction[" +
                "content=" + content + ", " +
                "count=" + count + ", " +
                "fromMe=" + fromMe + ']';
    }
}
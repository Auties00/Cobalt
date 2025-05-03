package it.auties.whatsapp.model.newsletter;

import io.avaje.jsonb.Json;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage
@Json
public final class NewsletterReaction {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    @Json.Property("content")
    final String content;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    @Json.Property("count")
    long count;

    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    @Json.Property("fromMe")
    boolean fromMe;

    public NewsletterReaction(String content, long count, boolean fromMe) {
        this.content = Objects.requireNonNull(content, "content cannot be null");
        this.count = count;
        this.fromMe = fromMe;
    }

    public String content() {
        return content;
    }

    public long count() {
        return count;
    }

    public NewsletterReaction setCount(long count) {
        this.count = count;
        return this;
    }

    public boolean fromMe() {
        return fromMe;
    }

    public NewsletterReaction setFromMe(boolean fromMe) {
        this.fromMe = fromMe;
        return this;
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
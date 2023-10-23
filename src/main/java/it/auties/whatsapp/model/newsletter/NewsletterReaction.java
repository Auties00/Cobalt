package it.auties.whatsapp.model.newsletter;

import java.util.Objects;

public final class NewsletterReaction {
    private final String content;
    private long count;
    private boolean fromMe;

    public NewsletterReaction(String content, long count, boolean fromMe) {
        this.content = content;
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
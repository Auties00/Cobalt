package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Represents an aggregated reaction tally for a particular emoji on a
 * newsletter message.
 *
 * <p>Because newsletters are broadcast, individual reactors are never
 * exposed to other subscribers; only the total count per emoji is
 * delivered. The {@linkplain #fromMe() fromMe} flag remains meaningful so
 * that the local UI can reflect whether the current session contributed to
 * the tally.
 */
@ProtobufMessage
public final class NewsletterReaction {
    /**
     * The emoji content that identifies which reaction this tally refers
     * to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String content;

    /**
     * The total number of subscribers who have posted this reaction.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    long count;

    /**
     * Whether the current session contributed to this tally.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    boolean fromMe;

    /**
     * Constructs a new {@code NewsletterReaction} with the supplied emoji
     * content, total count, and local-participation flag.
     *
     * @param content the emoji content, must not be {@code null}
     * @param count   the total reaction count
     * @param fromMe  {@code true} when the current session has posted this
     *                reaction
     * @throws NullPointerException if {@code content} is {@code null}
     */
    public NewsletterReaction(String content, long count, boolean fromMe) {
        this.content = Objects.requireNonNull(content, "content cannot be null");
        this.count = count;
        this.fromMe = fromMe;
    }

    /**
     * Returns the emoji content this tally refers to.
     *
     * @return the emoji content, never {@code null}
     */
    public String content() {
        return content;
    }

    /**
     * Sets the emoji content this tally refers to.
     *
     * @param content the new emoji content, must not be {@code null}
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns the total number of subscribers who have posted this
     * reaction.
     *
     * @return the reaction count
     */
    public long count() {
        return count;
    }

    /**
     * Sets the total reaction count.
     *
     * @param count the new reaction count
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     * Returns whether the current session has posted this reaction.
     *
     * @return {@code true} when the local user contributed to this tally
     */
    public boolean fromMe() {
        return fromMe;
    }

    /**
     * Sets whether the current session has posted this reaction.
     *
     * @param fromMe the new local-participation flag
     */
    public void setFromMe(boolean fromMe) {
        this.fromMe = fromMe;
    }

    /**
     * Returns whether this reaction equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterReaction}
     *         whose fields are all equal to this one's
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterReaction that
                            && count == that.count
                            && fromMe == that.fromMe
                            && Objects.equals(content, that.content);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this reaction
     */
    @Override
    public int hashCode() {
        return Objects.hash(content, count, fromMe);
    }
}

package com.github.auties00.cobalt.wire.stanza.smax.support;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Objects;
import java.util.Optional;

/**
 * Describes one offending newsletter message embedded as a {@code <message>} child of a
 * {@link SmaxNewsletterReportRequest} payload.
 *
 * <p>An entry is built either from the minimal scalar fields {@code (messageFrom, t, id)} or from
 * a fully pre-built {@link Stanza}; {@link SmaxNewsletterReportRequest} enumerates one entry per
 * offending message harvested from the local cache.
 *
 * @implNote
 * This implementation models the minimal-attribute shape ({@code from}, {@code t}, {@code id});
 * the optional {@link #raw} field lets callers bypass scalar reconstruction when a richer stanza is
 * already available.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutSpamMessageMixin")
public final class SmaxNewsletterReportMessageEntry {
    /**
     * Holds the sender JID of the offending message, routed into {@code <message from="..."/>}.
     */
    private final Jid messageFrom;

    /**
     * Holds the message timestamp in Unix seconds, routed into {@code <message t="..."/>}.
     */
    private final long messageTimestamp;

    /**
     * Holds the message stanza id, routed into {@code <message id="..."/>}.
     */
    private final String messageId;

    /**
     * Holds the optional pre-built {@code <message>} stanza.
     *
     * <p>When set, {@link #toNode()} returns this stanza verbatim and ignores the scalar fields.
     */
    private final Stanza raw;

    /**
     * Constructs an entry from the minimal scalar fields.
     *
     * <p>Equivalent to {@code this(messageFrom, messageTimestamp, messageId, null)}.
     *
     * @param messageFrom      the sender JID; never {@code null}
     * @param messageTimestamp the timestamp in Unix seconds
     * @param messageId        the stanza id; never {@code null}
     * @throws NullPointerException if {@code messageFrom} or {@code messageId} is {@code null}
     */
    public SmaxNewsletterReportMessageEntry(Jid messageFrom, long messageTimestamp, String messageId) {
        this(messageFrom, messageTimestamp, messageId, null);
    }

    /**
     * Constructs an entry that prefers a pre-built stanza over the scalar fields.
     *
     * <p>The scalar fields are retained for {@link #equals(Object)}, {@link #hashCode()} and
     * {@link #toString()} even when {@code raw} is supplied.
     *
     * @param messageFrom      the sender JID; never {@code null}
     * @param messageTimestamp the timestamp in Unix seconds
     * @param messageId        the stanza id; never {@code null}
     * @param raw              the optional pre-built stanza; may be {@code null}
     * @throws NullPointerException if {@code messageFrom} or {@code messageId} is {@code null}
     */
    public SmaxNewsletterReportMessageEntry(Jid messageFrom, long messageTimestamp, String messageId, Stanza raw) {
        this.messageFrom = Objects.requireNonNull(messageFrom, "messageFrom cannot be null");
        this.messageTimestamp = messageTimestamp;
        this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
        this.raw = raw;
    }

    /**
     * Returns the sender JID routed into {@code <message from>}.
     *
     * @return the JID; never {@code null}
     */
    public Jid messageFrom() {
        return messageFrom;
    }

    /**
     * Returns the message timestamp in Unix seconds routed into {@code <message t>}.
     *
     * @return the timestamp in Unix seconds
     */
    public long messageTimestamp() {
        return messageTimestamp;
    }

    /**
     * Returns the message stanza id routed into {@code <message id>}.
     *
     * @return the id; never {@code null}
     */
    public String messageId() {
        return messageId;
    }

    /**
     * Returns the optional pre-built {@code <message>} stanza.
     *
     * <p>Empty when {@link #toNode()} reconstructs the envelope from the scalar fields.
     *
     * @return an {@link Optional} carrying the stanza, or empty when omitted
     */
    public Optional<Stanza> raw() {
        return Optional.ofNullable(raw);
    }

    /**
     * Builds the {@code <message>} child suitable for embedding into a
     * {@link SmaxNewsletterReportRequest} payload.
     *
     * <p>Returns the pre-built {@link #raw} stanza when set; otherwise reconstructs the minimal
     * {@code (from, t, id)} envelope.
     *
     * @return the built stanza; never {@code null}
     */
    public Stanza toStanza() {
        if (raw != null) {
            return raw;
        }
        return new StanzaBuilder()
                .description("message")
                .attribute("from", messageFrom)
                .attribute("t", messageTimestamp)
                .attribute("id", messageId)
                .build();
    }

    /**
     * Compares this entry to another for value equality across all fields.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal {@link SmaxNewsletterReportMessageEntry}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxNewsletterReportMessageEntry) obj;
        return this.messageTimestamp == that.messageTimestamp
                && Objects.equals(this.messageFrom, that.messageFrom)
                && Objects.equals(this.messageId, that.messageId)
                && Objects.equals(this.raw, that.raw);
    }

    /**
     * Returns a hash code derived from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(messageFrom, messageTimestamp, messageId, raw);
    }

    /**
     * Returns a debug string listing the scalar fields.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxNewsletterReportMessageEntry[messageFrom=" + messageFrom
                + ", messageTimestamp=" + messageTimestamp
                + ", messageId=" + messageId + ']';
    }
}

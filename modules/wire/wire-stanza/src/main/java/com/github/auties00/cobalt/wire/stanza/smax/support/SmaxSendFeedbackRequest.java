package com.github.auties00.cobalt.wire.stanza.smax.support;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Submits feedback labels for a previously rated support-bot message as an outbound
 * {@code smax_id=138} IQ to the {@code fb:thrift_iq} relay.
 *
 * <p>The labels enumerate the kinds of feedback the user selected (one to ten entries) and are
 * paired with the rated message id. The relay's reply is parsed by
 * {@link SmaxSendFeedbackResponse}.
 *
 * @implNote
 * This implementation flattens the WA Web mixin chain into a single {@link StanzaBuilder} that pins
 * {@code xmlns="fb:thrift_iq"}, {@code smax_id=138}, {@code to=Jid.userServer()} and
 * {@code type="set"}; the one-to-ten cap is enforced in the constructor.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutSupportMessageFeedbackSendFeedbackRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutSupportMessageFeedbackHackBaseIQSetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSupportMessageFeedbackBaseIQSetRequestMixin")
public final class SmaxSendFeedbackRequest implements SmaxStanza.Request {
    /**
     * Holds the optional {@code from} JID forwarded into the IQ envelope.
     *
     * <p>{@code null} lets the relay infer the sender from the connection.
     */
    private final Jid iqFrom;

    /**
     * Holds the id of the rated message, routed into {@code <message id="..."/>}.
     */
    private final String messageId;

    /**
     * Holds the non-empty list of feedback-kind labels (one to ten entries), each materialised as
     * a {@code <feedback kind="..."/>} child under {@code <feedback_list>}.
     */
    private final List<String> feedbackKinds;

    /**
     * Constructs a feedback request.
     *
     * <p>Supply {@code iqFrom == null} to let the relay infer the sender from the connection.
     *
     * @param iqFrom        the optional sender JID; may be {@code null}
     * @param messageId     the rated message id; never {@code null}
     * @param feedbackKinds the feedback labels (1..10 entries); never {@code null}
     * @throws NullPointerException     if {@code messageId} or {@code feedbackKinds} is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code feedbackKinds} is empty or has more than ten
     *                                  entries
     */
    public SmaxSendFeedbackRequest(Jid iqFrom, String messageId, List<String> feedbackKinds) {
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(feedbackKinds, "feedbackKinds cannot be null");
        if (feedbackKinds.isEmpty() || feedbackKinds.size() > 10) {
            throw new IllegalArgumentException("feedbackKinds must contain 1..10 entries");
        }
        this.iqFrom = iqFrom;
        this.messageId = messageId;
        this.feedbackKinds = List.copyOf(feedbackKinds);
    }

    /**
     * Returns the optional {@code from} JID forwarded into the IQ envelope.
     *
     * <p>Empty when the relay is left to infer the sender from the connection.
     *
     * @return an {@link Optional} carrying the sender JID, or empty when omitted
     */
    public Optional<Jid> iqFrom() {
        return Optional.ofNullable(iqFrom);
    }

    /**
     * Returns the rated message id routed into {@code <message id>}.
     *
     * @return the message id; never {@code null}
     */
    public String messageId() {
        return messageId;
    }

    /**
     * Returns the feedback-kind labels materialised as {@code <feedback kind>} children.
     *
     * <p>The list is unmodifiable and contains one to ten entries.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<String> feedbackKinds() {
        return feedbackKinds;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Emits the {@code <iq>} envelope carrying the {@code <message>} child and the
     * {@code <feedback_list>} of per-kind {@code <feedback>} children.
     *
     * @implNote
     * This implementation materialises each feedback kind as a separate
     * {@code <feedback kind="..."/>} stanza and forwards {@link #iqFrom} into the envelope's
     * {@code from} attribute through
     * {@link StanzaBuilder#attribute(String, com.github.auties00.cobalt.wire.core.jid.JidProvider)},
     * which accepts a {@code null} JID.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutSupportMessageFeedbackSendFeedbackRequest",
            exports = "makeSendFeedbackRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var messageNode = new StanzaBuilder()
                .description("message")
                .attribute("id", messageId)
                .build();
        var feedbackNodes = feedbackKinds.stream()
                .map(kind -> new StanzaBuilder()
                        .description("feedback")
                        .attribute("kind", kind)
                        .build())
                .toList();
        var feedbackListNode = new StanzaBuilder()
                .description("feedback_list")
                .content(feedbackNodes)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "fb:thrift_iq")
                .attribute("smax_id", 138)
                .attribute("from", iqFrom)
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .content(List.of(messageNode, feedbackListNode));
    }

    /**
     * Compares this request to another for value equality across all fields.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal {@link SmaxSendFeedbackRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxSendFeedbackRequest) obj;
        return Objects.equals(this.iqFrom, that.iqFrom)
                && Objects.equals(this.messageId, that.messageId)
                && Objects.equals(this.feedbackKinds, that.feedbackKinds);
    }

    /**
     * Returns a hash code derived from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(iqFrom, messageId, feedbackKinds);
    }

    /**
     * Returns a debug string listing every field.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxSendFeedbackRequest[iqFrom=" + iqFrom
                + ", messageId=" + messageId
                + ", feedbackKinds=" + feedbackKinds + ']';
    }
}

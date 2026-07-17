package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * The outbound stanza that records a per-contact biz-feedback
 * preference on the relay.
 *
 * <p>Writes user reactions to biz-message interactions (block, unblock or report, plus an optional
 * free-form annotation).
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBizMsgUserFeedbackUpdatePreferenceRequest")
public final class SmaxUpdatePreferenceRequest implements SmaxStanza.Request {
    /**
     * The feedback action keyword routed into the {@code action}
     * attribute.
     */
    private final String action;

    /**
     * The contact JID the feedback applies to.
     */
    private final Jid jid;

    /**
     * The optional free-form feedback annotation; {@code null}
     * omits the {@code feedback} attribute.
     */
    private final String feedback;

    /**
     * Constructs a new request without a free-form annotation.
     *
     * <p>The default form used when the surface records only a keyword action (block, unblock or
     * report) without a user-supplied note.
     *
     * @param action the feedback action; never {@code null}
     * @param jid    the target contact JID; never {@code null}
     * @throws NullPointerException if either argument is
     *                              {@code null}
     */
    public SmaxUpdatePreferenceRequest(String action, Jid jid) {
        this(action, jid, null);
    }

    /**
     * Constructs a new request, optionally carrying a free-form
     * feedback annotation.
     *
     * <p>Forwards the user's keyword and free-form note verbatim. The {@code action} value is
     * opaque and is treated as an arbitrary string by the relay.
     *
     * @param action   the feedback action; never {@code null}
     * @param jid      the target contact JID; never {@code null}
     * @param feedback the optional free-form annotation; may be
     *                 {@code null}
     * @throws NullPointerException if {@code action} or {@code jid}
     *                              is {@code null}
     */
    public SmaxUpdatePreferenceRequest(String action, Jid jid, String feedback) {
        this.action = Objects.requireNonNull(action, "action cannot be null");
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.feedback = feedback;
    }

    /**
     * Returns the feedback action keyword.
     *
     * <p>Surfaces as the {@code action} attribute on the outbound {@code <user_feedback>} child.
     *
     * @return the action; never {@code null}
     */
    public String action() {
        return action;
    }

    /**
     * Returns the target contact JID.
     *
     * <p>Surfaces as the {@code jid} attribute on the outbound {@code <user_feedback>} child.
     *
     * @return the JID; never {@code null}
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Returns the optional free-form feedback annotation.
     *
     * <p>Returns {@link Optional#empty()} when the request was built via the two-argument
     * constructor.
     *
     * @return an {@link Optional} carrying the annotation
     */
    public Optional<String> feedback() {
        return Optional.ofNullable(feedback);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation stamps {@code xmlns="w:biz:msg_feedback"}, {@code type="set"},
     * {@code to="s.whatsapp.net"} and emits a single {@code <user_feedback>} child carrying the
     * {@code action} and {@code jid} pair plus the optional {@code feedback} annotation. The IQ
     * {@code id} is assigned by the dispatcher.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBizMsgUserFeedbackUpdatePreferenceRequest",
            exports = "makeUpdatePreferenceRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var feedbackBuilder = new StanzaBuilder()
                .description("user_feedback")
                .attribute("action", action)
                .attribute("jid", jid);
        if (feedback != null) {
            feedbackBuilder.attribute("feedback", feedback);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz:msg_feedback")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(feedbackBuilder.build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxUpdatePreferenceRequest) obj;
        return Objects.equals(this.action, that.action)
                && Objects.equals(this.jid, that.jid)
                && Objects.equals(this.feedback, that.feedback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(action, jid, feedback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "SmaxUpdatePreferenceRequest[action=" + action
                + ", jid=" + jid
                + ", feedback=" + feedback + ']';
    }
}

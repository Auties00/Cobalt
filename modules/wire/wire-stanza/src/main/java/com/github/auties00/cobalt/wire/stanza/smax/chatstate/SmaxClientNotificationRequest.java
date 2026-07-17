package com.github.auties00.cobalt.wire.stanza.smax.chatstate;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Represents the outbound
 * {@code <chatstate to="..."><composing|paused/></chatstate>} stanza that
 * broadcasts the local user's typing or paused state to a peer or group.
 *
 * <p>This stanza surfaces the typing, voice-note recording, and paused
 * indicators. It is fire-and-forget on the wire; the relay does not
 * acknowledge it.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutChatstateClientNotificationRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutChatstateStateTypes")
public final class SmaxClientNotificationRequest implements SmaxStanza.Request {
    /**
     * Holds the chat JID receiving the indicator.
     *
     * <p>For 1:1 chats this is the peer user JID; for group chats this is the
     * group JID, and the relay fans the indicator out to active participants.
     */
    private final Jid chatstateTo;

    /**
     * Holds the state-type payload, either a
     * {@link SmaxClientNotificationComposing} or a
     * {@link SmaxClientNotificationPaused}.
     */
    private final SmaxClientNotificationStateType stateType;

    /**
     * Constructs a client-notification request.
     *
     * @param chatstateTo the chat JID; never {@code null}
     * @param stateType   the state-type payload; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SmaxClientNotificationRequest(Jid chatstateTo, SmaxClientNotificationStateType stateType) {
        this.chatstateTo = Objects.requireNonNull(chatstateTo, "chatstateTo cannot be null");
        this.stateType = Objects.requireNonNull(stateType, "stateType cannot be null");
    }

    /**
     * Returns the chat JID receiving the indicator.
     *
     * @return the chat JID; never {@code null}
     */
    public Jid chatstateTo() {
        return chatstateTo;
    }

    /**
     * Returns the state-type payload.
     *
     * @return the state-type; never {@code null}
     */
    public SmaxClientNotificationStateType stateType() {
        return stateType;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Emits a {@code <chatstate to="...">} envelope and delegates the
     * inner child to {@link SmaxClientNotificationStateType#toStanza()}.
     *
     * @implNote
     * This implementation does not emit the WA Web optional internal-test
     * mixin child, which is a dev-only debug payload absent from production
     * stanzas.
     *
     * @return a {@link StanzaBuilder} carrying the
     *         {@code <chatstate><composing|paused/></chatstate>} stanza
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutChatstateClientNotificationRequest",
            exports = "makeClientNotificationRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var stateChild = stateType.toStanza().build();
        return new StanzaBuilder()
                .description("chatstate")
                .attribute("to", chatstateTo)
                .content(stateChild);
    }

    /**
     * Returns whether the given object is a {@link SmaxClientNotificationRequest}
     * with an equal {@link #chatstateTo()} and {@link #stateType()}.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when both fields match
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxClientNotificationRequest) obj;
        return Objects.equals(this.chatstateTo, that.chatstateTo)
                && Objects.equals(this.stateType, that.stateType);
    }

    /**
     * Returns a hash code derived from the chat JID and the state-type
     * payload.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(chatstateTo, stateType);
    }

    /**
     * Returns a debug-friendly textual representation of this request.
     *
     * @return the textual representation
     */
    @Override
    public String toString() {
        return "SmaxClientNotificationRequest[chatstateTo=" + chatstateTo
                + ", stateType=" + stateType + ']';
    }
}

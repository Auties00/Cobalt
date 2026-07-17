package com.github.auties00.cobalt.wire.stanza.smax.chatstate;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

/**
 * Represents the outbound "paused" state-type carried by a
 * {@link SmaxClientNotificationRequest}.
 *
 * <p>This payload backs the "user stopped typing" indicator; it clears the
 * typing dot from the peer's chat UI and carries no payload of its own.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutChatstatePausedMixin")
public final class SmaxClientNotificationPaused implements SmaxClientNotificationStateType {
    /**
     * Constructs the empty paused state-type.
     */
    public SmaxClientNotificationPaused() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Emits a bare {@code <paused/>} child with no attributes.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <paused/>} child
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutChatstatePausedMixin",
            exports = "mergePausedMixin", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        return new StanzaBuilder()
                .description("paused");
    }

    /**
     * Returns whether the given object is also a
     * {@link SmaxClientNotificationPaused}.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when the candidate has the same runtime type
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj != null && obj.getClass() == this.getClass();
    }

    /**
     * Returns the constant hash code shared by every paused state-type.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return SmaxClientNotificationPaused.class.hashCode();
    }

    /**
     * Returns a debug-friendly textual representation of this state-type.
     *
     * @return the textual representation
     */
    @Override
    public String toString() {
        return "SmaxClientNotificationPaused[]";
    }
}

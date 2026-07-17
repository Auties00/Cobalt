package com.github.auties00.cobalt.wire.stanza.smax.chatstate;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Objects;

/**
 * Represents the outbound "composing" state-type carried by a
 * {@link SmaxClientNotificationRequest}.
 *
 * <p>This payload backs both the "user is typing" indicator and the "user is
 * recording a voice note" indicator. The two surfaces differ only by whether
 * {@link #hasComposingMediaAudio()} is set: an unset marker produces the
 * typing indicator, while a set marker produces the voice-note recording
 * indicator.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutChatstateComposingMixin")
public final class SmaxClientNotificationComposing implements SmaxClientNotificationStateType {
    /**
     * Holds whether the {@code media="audio"} marker is emitted.
     *
     * <p>A {@code true} value indicates a voice-note recording in progress
     * (the recording indicator in the chat UI); a {@code false} value
     * indicates plain text typing.
     */
    private final boolean hasComposingMediaAudio;

    /**
     * Constructs a composing state-type.
     *
     * @param hasComposingMediaAudio whether to emit the {@code media="audio"} marker
     */
    public SmaxClientNotificationComposing(boolean hasComposingMediaAudio) {
        this.hasComposingMediaAudio = hasComposingMediaAudio;
    }

    /**
     * Returns whether the {@code media="audio"} marker is emitted.
     *
     * @return {@code true} when audio recording, {@code false} when typing text
     */
    public boolean hasComposingMediaAudio() {
        return hasComposingMediaAudio;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Emits a {@code <composing>} child, attaching {@code media="audio"}
     * only when {@link #hasComposingMediaAudio()} is {@code true}.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <composing>} child
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutChatstateComposingMixin",
            exports = "mergeComposingMixin", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        return new StanzaBuilder()
                .description("composing")
                .attribute("media", "audio", hasComposingMediaAudio);
    }

    /**
     * Returns whether the given object is a {@link SmaxClientNotificationComposing}
     * with an equal {@link #hasComposingMediaAudio()} marker.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when the audio markers match
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxClientNotificationComposing) obj;
        return this.hasComposingMediaAudio == that.hasComposingMediaAudio;
    }

    /**
     * Returns a hash code derived from the {@link #hasComposingMediaAudio()}
     * marker.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(hasComposingMediaAudio);
    }

    /**
     * Returns a debug-friendly textual representation of this state-type.
     *
     * @return the textual representation
     */
    @Override
    public String toString() {
        return "SmaxClientNotificationComposing[hasComposingMediaAudio="
                + hasComposingMediaAudio + ']';
    }
}

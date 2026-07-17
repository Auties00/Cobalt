package com.github.auties00.cobalt.wire.stanza.smax.chatstate;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the sealed disjunction over the two inbound chat-state
 * indicators: the peer is typing ({@link Composing}) or has stopped
 * ({@link Paused}).
 *
 * <p>A consumer derives a UI-facing status from this disjunction: a
 * {@link Paused} maps to a paused status, a {@link Composing} with an empty
 * {@link Composing#composingMedia()} maps to a typing status, and a
 * {@link Composing} whose {@link Composing#composingMedia()} is {@code "audio"}
 * maps to a voice-note recording status.
 */
@WhatsAppWebModule(moduleName = "WASmaxInChatstateStateTypes")
public sealed interface SmaxServerNotificationStateType permits SmaxServerNotificationStateType.Composing, SmaxServerNotificationStateType.Paused {

    /**
     * Parses an inbound stanza into the first matching variant.
     *
     * <p>Parsing tries {@link Composing} first and falls back to
     * {@link Paused}, returning the first successful parse and
     * {@link Optional#empty()} when neither matches.
     *
     * @param stanza the inbound {@code <chatstate/>} stanza
     * @return an {@link Optional} carrying the parsed variant, or empty on no-match
     */
    @WhatsAppWebExport(moduleName = "WASmaxInChatstateStateTypes",
            exports = "parseStateTypes", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxServerNotificationStateType> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var composing = Composing.of(stanza);
        if (composing.isPresent()) {
            return composing;
        }
        return Paused.of(stanza);
    }

    /**
     * Represents the variant indicating the peer is currently typing or
     * recording audio.
     *
     * <p>The presence and value of {@link #composingMedia()} distinguishes
     * text typing (empty) from voice-note recording ({@code "audio"}).
     */
    @WhatsAppWebModule(moduleName = "WASmaxInChatstateComposingMixin")
    final class Composing implements SmaxServerNotificationStateType {
        /**
         * Holds the optional {@code media} attribute on the {@code <composing>}
         * child, either {@code "audio"} for voice-note recording or
         * {@code null} for plain text typing.
         */
        private final String composingMedia;

        /**
         * Constructs a {@link Composing} variant.
         *
         * @param composingMedia the optional {@code media} attribute; may be {@code null}
         */
        public Composing(String composingMedia) {
            this.composingMedia = composingMedia;
        }

        /**
         * Returns the optional {@code media} attribute.
         *
         * @return an {@link Optional} carrying {@code "audio"} when the peer
         *         is recording a voice note, or empty when typing text
         */
        public Optional<String> composingMedia() {
            return Optional.ofNullable(composingMedia);
        }

        /**
         * Parses an inbound stanza into a {@link Composing} variant.
         *
         * <p>Parsing asserts the {@code <chatstate>} tag, requires an inner
         * {@code <composing>} child, and accepts only an absent {@code media}
         * attribute or one whose value is the literal {@code "audio"}. Any
         * other shape yields {@link Optional#empty()}.
         *
         * @param stanza the inbound chatstate stanza
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInChatstateComposingMixin",
                exports = "parseComposingMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Composing> of(Stanza stanza) {
            if (!stanza.hasDescription("chatstate")) {
                return Optional.empty();
            }
            var composingChild = stanza.getChild("composing").orElse(null);
            if (composingChild == null) {
                return Optional.empty();
            }
            var media = composingChild.getAttributeAsString("media").orElse(null);
            if (media != null && !"audio".equals(media)) {
                return Optional.empty();
            }
            return Optional.of(new Composing(media));
        }

        /**
         * Returns whether the given object is a {@link Composing} with an
         * equal {@link #composingMedia()} attribute.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when the media attributes match
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Composing) obj;
            return Objects.equals(this.composingMedia, that.composingMedia);
        }

        /**
         * Returns a hash code derived from the {@link #composingMedia()}
         * attribute.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(composingMedia);
        }

        /**
         * Returns a debug-friendly textual representation of this variant.
         *
         * @return the textual representation
         */
        @Override
        public String toString() {
            return "SmaxServerNotificationStateType.Composing[composingMedia="
                    + composingMedia + ']';
        }
    }

    /**
     * Represents the variant indicating the peer has stopped typing.
     *
     * <p>This variant clears the typing dot from the chat UI and carries no
     * payload of its own.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInChatstatePausedMixin")
    final class Paused implements SmaxServerNotificationStateType {
        /**
         * Constructs the empty {@link Paused} variant.
         */
        public Paused() {
        }

        /**
         * Parses an inbound stanza into a {@link Paused} variant.
         *
         * <p>Parsing asserts the {@code <chatstate>} tag and requires the
         * inner {@code <paused>} child to be present, yielding
         * {@link Optional#empty()} otherwise.
         *
         * @param stanza the inbound chatstate stanza
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInChatstatePausedMixin",
                exports = "parsePausedMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Paused> of(Stanza stanza) {
            if (!stanza.hasDescription("chatstate")) {
                return Optional.empty();
            }
            if (stanza.getChild("paused").isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Paused());
        }

        /**
         * Returns whether the given object is also a {@link Paused} variant.
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
         * Returns the constant hash code shared by every paused variant.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Paused.class.hashCode();
        }

        /**
         * Returns a debug-friendly textual representation of this variant.
         *
         * @return the textual representation
         */
        @Override
        public String toString() {
            return "SmaxServerNotificationStateType.Paused[]";
        }
    }
}

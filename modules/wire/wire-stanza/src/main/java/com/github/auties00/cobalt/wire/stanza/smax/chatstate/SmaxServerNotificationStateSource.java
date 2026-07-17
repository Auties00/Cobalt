package com.github.auties00.cobalt.wire.stanza.smax.chatstate;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the sealed disjunction over the two state-source mixins
 * identifying the origin of an inbound {@code <chatstate>} event: a 1:1 peer
 * ({@link FromUser}) or a group participant ({@link FromGroup}).
 *
 * <p>A consumer branches on {@link #of(Stanza)}: a {@link FromUser} result
 * routes the indicator to the 1:1 handler, while a {@link FromGroup} result
 * routes it to the group handler with the participant attached.
 */
@WhatsAppWebModule(moduleName = "WASmaxInChatstateStateSource")
public sealed interface SmaxServerNotificationStateSource permits SmaxServerNotificationStateSource.FromUser, SmaxServerNotificationStateSource.FromGroup {

    /**
     * Parses an inbound stanza into the first matching variant.
     *
     * <p>Parsing tries {@link FromUser} first and falls back to
     * {@link FromGroup}, returning the first successful parse and
     * {@link Optional#empty()} when neither matches.
     *
     * @param stanza the inbound {@code <chatstate/>} stanza
     * @return an {@link Optional} carrying the parsed variant, or empty on no-match
     */
    @WhatsAppWebExport(moduleName = "WASmaxInChatstateStateSource",
            exports = "parseStateSource", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxServerNotificationStateSource> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var fromUser = FromUser.of(stanza);
        if (fromUser.isPresent()) {
            return fromUser;
        }
        return FromGroup.of(stanza);
    }

    /**
     * Reports whether the given JID's server is one of the user-JID server
     * domains.
     *
     * <p>The admit set comprises the {@code s.whatsapp.net}, {@code interop},
     * {@code msgr}, {@code lid}, and {@code bot} server domains, matching the
     * domains accepted by the WhatsApp Web user-JID validator.
     *
     * @param jid the JID to test; never {@code null}
     * @return {@code true} when {@link Jid#server()} is one of the user-JID
     *         server domains, {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAJids",
            exports = "validateUserJid", adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isUserJidServer(Jid jid) {
        var server = jid.server().toString();
        return "s.whatsapp.net".equals(server)
                || "interop".equals(server)
                || "msgr".equals(server)
                || "lid".equals(server)
                || "bot".equals(server);
    }

    /**
     * Represents the variant raised by a 1:1 peer.
     *
     * <p>This variant identifies the chat-state event as originating from a
     * direct conversation; the {@link #from()} JID is the peer's user JID.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInChatstateFromUserMixin")
    final class FromUser implements SmaxServerNotificationStateSource {
        /**
         * Holds the user JID raising the event.
         */
        private final Jid from;

        /**
         * Constructs a {@link FromUser} variant.
         *
         * @param from the user JID; never {@code null}
         * @throws NullPointerException if {@code from} is {@code null}
         */
        public FromUser(Jid from) {
            this.from = Objects.requireNonNull(from, "from cannot be null");
        }

        /**
         * Returns the user JID raising the event.
         *
         * @return the JID; never {@code null}
         */
        public Jid from() {
            return from;
        }

        /**
         * Parses an inbound stanza into a {@link FromUser} variant.
         *
         * <p>Parsing asserts the {@code <chatstate>} tag and requires the
         * {@code from} attribute to resolve to a user-JID server domain
         * admitted by {@link #isUserJidServer(Jid)}, yielding
         * {@link Optional#empty()} on any mismatch.
         *
         * @param stanza the inbound chatstate stanza
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInChatstateFromUserMixin",
                exports = "parseFromUserMixin", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<FromUser> of(Stanza stanza) {
            if (!stanza.hasDescription("chatstate")) {
                return Optional.empty();
            }
            var from = stanza.getAttributeAsJid("from").orElse(null);
            if (from == null) {
                return Optional.empty();
            }
            if (!isUserJidServer(from)) {
                return Optional.empty();
            }
            return Optional.of(new FromUser(from));
        }

        /**
         * Returns whether the given object is a {@link FromUser} with an
         * equal {@link #from()} JID.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when the JIDs match
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (FromUser) obj;
            return Objects.equals(this.from, that.from);
        }

        /**
         * Returns a hash code derived from the {@link #from()} JID.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(from);
        }

        /**
         * Returns a debug-friendly textual representation of this variant.
         *
         * @return the textual representation
         */
        @Override
        public String toString() {
            return "SmaxServerNotificationStateSource.FromUser[from=" + from + ']';
        }
    }

    /**
     * Represents the variant raised by a participant inside a group chat.
     *
     * <p>This variant identifies the chat-state event as originating from a
     * group conversation; the {@link #from()} JID is the group JID and
     * {@link #participant()} identifies which group member raised the event.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInChatstateFromGroupMixin")
    final class FromGroup implements SmaxServerNotificationStateSource {
        /**
         * Holds the group JID hosting the chat.
         */
        private final Jid from;

        /**
         * Holds the participant JID raising the event.
         */
        private final Jid participant;

        /**
         * Holds the optional phone-number-form participant JID.
         *
         * <p>The relay supplies this when it can attach the PN-form identity
         * for a participant whose primary identifier is a LID; it stays
         * {@code null} for participants without an associated PN.
         */
        private final Jid participantPn;

        /**
         * Constructs a {@link FromGroup} variant.
         *
         * @param from          the group JID; never {@code null}
         * @param participant   the participant JID; never {@code null}
         * @param participantPn the optional pn-form participant JID; may be {@code null}
         * @throws NullPointerException if {@code from} or {@code participant} is {@code null}
         */
        public FromGroup(Jid from, Jid participant, Jid participantPn) {
            this.from = Objects.requireNonNull(from, "from cannot be null");
            this.participant = Objects.requireNonNull(participant, "participant cannot be null");
            this.participantPn = participantPn;
        }

        /**
         * Returns the group JID hosting the chat.
         *
         * @return the JID; never {@code null}
         */
        public Jid from() {
            return from;
        }

        /**
         * Returns the participant JID raising the event.
         *
         * @return the JID; never {@code null}
         */
        public Jid participant() {
            return participant;
        }

        /**
         * Returns the optional pn-form participant JID.
         *
         * @return an {@link Optional} carrying the JID, or empty when omitted
         */
        public Optional<Jid> participantPn() {
            return Optional.ofNullable(participantPn);
        }

        /**
         * Parses an inbound stanza into a {@link FromGroup} variant.
         *
         * <p>Parsing asserts the {@code <chatstate>} tag, requires the
         * {@code from} attribute to resolve to a {@code g.us} group JID, and
         * requires the {@code participant} attribute to resolve to a user-JID
         * server domain admitted by {@link #isUserJidServer(Jid)}. The
         * optional {@code participant_pn} attribute is held against the same
         * admit set when present. Any mismatch yields {@link Optional#empty()}.
         *
         * @implNote
         * This implementation treats an absent {@code participant_pn} as
         * success-with-{@code null} but rejects a present
         * {@code participant_pn} that fails {@link #isUserJidServer(Jid)}.
         *
         * @param stanza the inbound chatstate stanza
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInChatstateFromGroupMixin",
                exports = "parseFromGroupMixin", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<FromGroup> of(Stanza stanza) {
            if (!stanza.hasDescription("chatstate")) {
                return Optional.empty();
            }
            var from = stanza.getAttributeAsJid("from").orElse(null);
            if (from == null || !"g.us".equals(from.server().toString())) {
                return Optional.empty();
            }
            var participant = stanza.getAttributeAsJid("participant").orElse(null);
            if (participant == null) {
                return Optional.empty();
            }
            if (!isUserJidServer(participant)) {
                return Optional.empty();
            }
            Jid participantPn = null;
            if (stanza.getAttribute("participant_pn").isPresent()) {
                participantPn = stanza.getAttributeAsJid("participant_pn").orElse(null);
                if (participantPn == null || !isUserJidServer(participantPn)) {
                    return Optional.empty();
                }
            }
            return Optional.of(new FromGroup(from, participant, participantPn));
        }

        /**
         * Returns whether the given object is a {@link FromGroup} with an
         * equal group JID, participant, and participant PN.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when all three fields match
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (FromGroup) obj;
            return Objects.equals(this.from, that.from)
                    && Objects.equals(this.participant, that.participant)
                    && Objects.equals(this.participantPn, that.participantPn);
        }

        /**
         * Returns a hash code derived from the group JID, participant, and
         * participant PN.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(from, participant, participantPn);
        }

        /**
         * Returns a debug-friendly textual representation of this variant.
         *
         * @return the textual representation
         */
        @Override
        public String toString() {
            return "SmaxServerNotificationStateSource.FromGroup[from=" + from
                    + ", participant=" + participant
                    + ", participantPn=" + participantPn + ']';
        }
    }
}

package com.github.auties00.cobalt.calls.signaling.session;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the {@code <call_summary>} subtree a peer or the relay attaches to an inbound
 * {@code <terminate>} to describe the call that just ended.
 *
 * <p>The summary carries the ended call's identifier and creator, its total duration, the media the
 * call carried, and one {@link Participant} entry per party. Each participant is a {@code <user>} node
 * for a real account or a {@code <bot>} node for an automated agent; both carry the party's device
 * {@code jid}, its phone-number {@code user_pn}, and its final {@code state} literal.
 *
 * <p>The summary is informational: the engine surfaces it for call history rather than acting on it, so
 * {@link #of(Stanza)} parses best effort. A missing attribute yields a {@code null} (or {@code 0}
 * duration) component rather than a rejected summary, and a child whose tag is neither {@code <user>} nor
 * {@code <bot>} is skipped. Instances are immutable; the participant list is copied on construction.
 *
 * @param callId       the ended call's identifier, or {@code null} when the summary carried none
 * @param callCreator  the ended call's creator device JID, or {@code null} when absent
 * @param callDuration the call's total duration in the wire {@code call_duration} units, or {@code 0}
 *                     when absent
 * @param media        the media literal the call carried, or {@code null} when absent
 * @param participants the per party summary entries in wire order; never {@code null}, possibly empty
 */
public record CallSummary(String callId, Jid callCreator, long callDuration, String media,
                          List<Participant> participants) {
    /**
     * The element description (tag name) of a {@code <call_summary>} subtree.
     */
    private static final String ELEMENT = "call_summary";

    /**
     * The child element description of a real account participant entry.
     */
    private static final String USER_ELEMENT = "user";

    /**
     * The child element description of an automated agent (bot) participant entry.
     */
    private static final String BOT_ELEMENT = "bot";

    /**
     * The attribute naming the ended call's identifier on a {@code <call_summary>}.
     */
    private static final String CALL_ID_ATTRIBUTE = "call-id";

    /**
     * The attribute naming the ended call's creator device JID on a {@code <call_summary>}.
     */
    private static final String CALL_CREATOR_ATTRIBUTE = "call-creator";

    /**
     * The attribute naming the ended call's total duration on a {@code <call_summary>}.
     */
    private static final String CALL_DURATION_ATTRIBUTE = "call_duration";

    /**
     * The attribute naming the media the ended call carried on a {@code <call_summary>}.
     */
    private static final String MEDIA_ATTRIBUTE = "media";

    /**
     * The attribute naming a participant's device JID on a {@code <user>} or {@code <bot>} entry.
     */
    private static final String JID_ATTRIBUTE = "jid";

    /**
     * The attribute naming a participant's phone-number JID on a {@code <user>} or {@code <bot>} entry.
     */
    private static final String USER_PN_ATTRIBUTE = "user_pn";

    /**
     * The attribute naming a participant's final state literal on a {@code <user>} or {@code <bot>} entry.
     */
    private static final String STATE_ATTRIBUTE = "state";

    /**
     * Canonicalizes the record, copying the participant list.
     *
     * @throws NullPointerException if {@code participants} is {@code null} or contains a {@code null}
     *                              element
     */
    public CallSummary {
        Objects.requireNonNull(participants, "participants cannot be null");
        participants = List.copyOf(participants);
    }

    /**
     * Parses a {@code <call_summary>} subtree into a typed summary.
     *
     * <p>Reads the summary's {@code call-id}, {@code call-creator}, {@code call_duration}, and
     * {@code media} attributes, then each {@code <user>} and {@code <bot>} child into a
     * {@link Participant}. The parse is best effort: an absent attribute yields a {@code null} (or
     * {@code 0} duration) component, and any child that is neither a {@code <user>} nor a {@code <bot>}
     * is skipped.
     *
     * @param stanza the {@code <call_summary>} subtree, or {@code null}
     * @return the parsed summary, or {@link Optional#empty()} when {@code stanza} is {@code null} or is
     *         not a {@code <call_summary>} element
     */
    public static Optional<CallSummary> of(Stanza stanza) {
        if (stanza == null || !ELEMENT.equals(stanza.description())) {
            return Optional.empty();
        }
        var participants = new ArrayList<Participant>();
        stanza.streamChildren().forEach(child -> {
            var bot = BOT_ELEMENT.equals(child.description());
            if (bot || USER_ELEMENT.equals(child.description())) {
                participants.add(new Participant(
                        child.getAttributeAsJid(JID_ATTRIBUTE, null),
                        child.getAttributeAsJid(USER_PN_ATTRIBUTE, null),
                        child.getAttributeAsString(STATE_ATTRIBUTE, null),
                        bot));
            }
        });
        return Optional.of(new CallSummary(
                stanza.getAttributeAsString(CALL_ID_ATTRIBUTE, null),
                stanza.getAttributeAsJid(CALL_CREATOR_ATTRIBUTE, null),
                stanza.getAttributeAsLong(CALL_DURATION_ATTRIBUTE, 0),
                stanza.getAttributeAsString(MEDIA_ATTRIBUTE, null),
                participants));
    }

    /**
     * Renders this summary back into a {@code <call_summary>} subtree.
     *
     * <p>Emits the summary attributes, omitting an absent creator or media, and one child per
     * {@link Participant}. This is the inverse of {@link #of(Stanza)} for the components it models.
     *
     * @return the {@code <call_summary>} stanza
     */
    public Stanza toStanza() {
        var builder = new StanzaBuilder()
                .description(ELEMENT)
                .attribute(CALL_ID_ATTRIBUTE, callId)
                .attribute(CALL_CREATOR_ATTRIBUTE, callCreator, callCreator != null)
                .attribute(CALL_DURATION_ATTRIBUTE, callDuration)
                .attribute(MEDIA_ATTRIBUTE, media);
        if (!participants.isEmpty()) {
            var children = new ArrayList<Stanza>(participants.size());
            for (var participant : participants) {
                children.add(participant.toStanza());
            }
            builder.content(children);
        }
        return builder.build();
    }

    /**
     * Models one party's entry in a {@link CallSummary}.
     *
     * @param jid    the party's device JID, or {@code null} when absent
     * @param userPn the party's phone-number JID, or {@code null} when absent
     * @param state  the party's final state literal, or {@code null} when absent
     * @param bot    {@code true} when the entry was a {@code <bot>} node, {@code false} for a {@code <user>}
     */
    public record Participant(Jid jid, Jid userPn, String state, boolean bot) {
        /**
         * Renders this participant back into its {@code <user>} or {@code <bot>} entry.
         *
         * @return the participant entry stanza
         */
        private Stanza toStanza() {
            return new StanzaBuilder()
                    .description(bot ? BOT_ELEMENT : USER_ELEMENT)
                    .attribute(JID_ATTRIBUTE, jid, jid != null)
                    .attribute(USER_PN_ATTRIBUTE, userPn, userPn != null)
                    .attribute(STATE_ATTRIBUTE, state)
                    .build();
        }
    }
}

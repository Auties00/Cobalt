package com.github.auties00.cobalt.wire.stanza.smax.support;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reports a WhatsApp group as an outbound {@code spam} IQ, inlining the offending messages and
 * calls under {@code <spam_list>}.
 *
 * <p>A minimal report requires only {@link Builder#spamListJid(Jid)} and
 * {@link Builder#spamListSpamFlow(String)}; the remaining optional fields (source, subject,
 * is-known-chat, FRX) refine the attribution. The relay's verdict is parsed by
 * {@link SmaxGroupReportResponse}.
 *
 * @implNote
 * This implementation flattens the WA Web mixin chain into a single {@link StanzaBuilder} that pins
 * {@code xmlns="spam"}, {@code to=JidServer.user()} and {@code type="set"}. Optional attributes
 * and children are emitted only when their builder fields are non-{@code null} or non-empty.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutSpamGroupReportRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamBaseReportMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamBaseIQSetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamEntitySubjectMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamFRXMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamIsKnownChatMixin")
public final class SmaxGroupReportRequest implements SmaxStanza.Request {
    /**
     * Holds the group JID being reported, routed into {@code <spam_list jid="..."/>}.
     */
    private final Jid spamListJid;

    /**
     * Holds the spam-flow identifier routed into {@code <spam_list spam_flow="..."/>}, naming the
     * user-facing surface that issued the report.
     */
    private final String spamListSpamFlow;

    /**
     * Holds the optional adder JID (the user who added the reporter to the group), routed into
     * {@code <spam_list source="..."/>} when present.
     */
    private final Jid spamListSource;

    /**
     * Holds the optional group subject string routed into {@code <spam_list subject="..."/>} when
     * present.
     */
    private final String spamListSubject;

    /**
     * Holds the optional {@code is_known_chat} marker routed into
     * {@code <spam_list is_known_chat="..."/>} when present, indicating prior history with the
     * group.
     */
    private final String spamListIsKnownChat;

    /**
     * Holds the optional pre-built {@code <frx>} (free-form reporting extensions) child appended
     * verbatim under the IQ envelope when present.
     */
    private final Stanza frxChild;

    /**
     * Holds the pre-built {@code <message>} children harvested from the offending chat (WA Web
     * caps this at 210), appended in insertion order under {@code <spam_list>}; never
     * {@code null}.
     */
    private final List<Stanza> messageChildren;

    /**
     * Holds the pre-built {@code <call>} children harvested from the offending chat (WA Web caps
     * this at 5), appended in insertion order under {@code <spam_list>}; never {@code null}.
     */
    private final List<Stanza> callChildren;

    /**
     * Constructs a request from the assembled {@link Builder} state.
     *
     * @param builder the source builder; never {@code null}
     */
    private SmaxGroupReportRequest(Builder builder) {
        this.spamListJid = Objects.requireNonNull(builder.spamListJid, "spamListJid cannot be null");
        this.spamListSpamFlow = Objects.requireNonNull(builder.spamListSpamFlow,
                "spamListSpamFlow cannot be null");
        this.spamListSource = builder.spamListSource;
        this.spamListSubject = builder.spamListSubject;
        this.spamListIsKnownChat = builder.spamListIsKnownChat;
        this.frxChild = builder.frxChild;
        this.messageChildren = List.copyOf(builder.messageChildren);
        this.callChildren = List.copyOf(builder.callChildren);
    }

    /**
     * Returns a new {@link Builder}, the canonical entry point for assembling a group spam
     * report.
     *
     * @return a new builder; never {@code null}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the group JID being reported, the {@code <spam_list jid>} value.
     *
     * @return the JID; never {@code null}
     */
    public Jid spamListJid() {
        return spamListJid;
    }

    /**
     * Returns the spam-flow identifier, the {@code <spam_list spam_flow>} value.
     *
     * @return the spam-flow; never {@code null}
     */
    public String spamListSpamFlow() {
        return spamListSpamFlow;
    }

    /**
     * Returns the optional adder JID, the {@code <spam_list source>} value.
     *
     * @return an {@link Optional} carrying the JID, or empty when omitted
     */
    public Optional<Jid> spamListSource() {
        return Optional.ofNullable(spamListSource);
    }

    /**
     * Returns the optional group subject, the {@code <spam_list subject>} value.
     *
     * @return an {@link Optional} carrying the subject, or empty when omitted
     */
    public Optional<String> spamListSubject() {
        return Optional.ofNullable(spamListSubject);
    }

    /**
     * Returns the optional {@code is_known_chat} marker, the {@code <spam_list is_known_chat>}
     * value.
     *
     * @return an {@link Optional} carrying the marker, or empty when omitted
     */
    public Optional<String> spamListIsKnownChat() {
        return Optional.ofNullable(spamListIsKnownChat);
    }

    /**
     * Returns the optional pre-built {@code <frx>} child appended to the IQ.
     *
     * @return an {@link Optional} carrying the stanza, or empty when omitted
     */
    public Optional<Stanza> frxChild() {
        return Optional.ofNullable(frxChild);
    }

    /**
     * Returns the offending-message children captured from the chat at report time.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<Stanza> messageChildren() {
        return messageChildren;
    }

    /**
     * Returns the offending-call children captured from the chat at report time.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<Stanza> callChildren() {
        return callChildren;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Assembles the {@code <spam_list>} child (attributes plus message and call children) and
     * attaches the FRX child to the outer {@code <iq>} when present.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutSpamGroupReportRequest",
            exports = "makeGroupReportRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var spamListBuilder = new StanzaBuilder()
                .description("spam_list")
                .attribute("jid", spamListJid)
                .attribute("spam_flow", spamListSpamFlow);
        if (spamListSource != null) {
            spamListBuilder.attribute("source", spamListSource);
        }
        if (spamListSubject != null) {
            spamListBuilder.attribute("subject", spamListSubject);
        }
        if (spamListIsKnownChat != null) {
            spamListBuilder.attribute("is_known_chat", spamListIsKnownChat);
        }
        var spamListChildren = new ArrayList<Stanza>(messageChildren.size() + callChildren.size());
        spamListChildren.addAll(messageChildren);
        spamListChildren.addAll(callChildren);
        if (!spamListChildren.isEmpty()) {
            spamListBuilder.content(spamListChildren);
        }
        var iqChildren = new ArrayList<Stanza>();
        iqChildren.add(spamListBuilder.build());
        if (frxChild != null) {
            iqChildren.add(frxChild);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "spam")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(iqChildren);
    }

    /**
     * Compares this request to another for value equality across all fields.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal {@link SmaxGroupReportRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupReportRequest) obj;
        return Objects.equals(this.spamListJid, that.spamListJid)
                && Objects.equals(this.spamListSpamFlow, that.spamListSpamFlow)
                && Objects.equals(this.spamListSource, that.spamListSource)
                && Objects.equals(this.spamListSubject, that.spamListSubject)
                && Objects.equals(this.spamListIsKnownChat, that.spamListIsKnownChat)
                && Objects.equals(this.frxChild, that.frxChild)
                && Objects.equals(this.messageChildren, that.messageChildren)
                && Objects.equals(this.callChildren, that.callChildren);
    }

    /**
     * Returns a hash code derived from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(spamListJid, spamListSpamFlow, spamListSource, spamListSubject,
                spamListIsKnownChat, frxChild, messageChildren, callChildren);
    }

    /**
     * Returns a debug string listing the scalar fields and the child counts.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxGroupReportRequest[spamListJid=" + spamListJid
                + ", spamListSpamFlow=" + spamListSpamFlow
                + ", spamListSource=" + spamListSource
                + ", spamListSubject=" + spamListSubject
                + ", spamListIsKnownChat=" + spamListIsKnownChat
                + ", messageChildren=" + messageChildren.size()
                + ", callChildren=" + callChildren.size() + ']';
    }

    /**
     * Assembles a {@link SmaxGroupReportRequest} from the WA Web mixin inputs.
     *
     * <p>The required {@link #spamListJid} and {@link #spamListSpamFlow} are validated at setter
     * time; the remaining fields are optional. Obtain an instance via
     * {@link SmaxGroupReportRequest#builder()}.
     *
     * @implNote
     * This implementation collects the optional mixin payloads into private fields and produces an
     * immutable copy from {@link #build()}.
     */
    public static final class Builder {
        /**
         * Holds the required group JID, set via {@link #spamListJid(Jid)}.
         */
        private Jid spamListJid;

        /**
         * Holds the required spam-flow string, set via {@link #spamListSpamFlow(String)}.
         */
        private String spamListSpamFlow;

        /**
         * Holds the optional adder JID, set via {@link #spamListSource(Jid)}.
         */
        private Jid spamListSource;

        /**
         * Holds the optional group subject, set via {@link #spamListSubject(String)}.
         */
        private String spamListSubject;

        /**
         * Holds the optional {@code is_known_chat} marker, set via
         * {@link #spamListIsKnownChat(String)}.
         */
        private String spamListIsKnownChat;

        /**
         * Holds the optional pre-built FRX child stanza, set via {@link #frxChild(Stanza)}.
         */
        private Stanza frxChild;

        /**
         * Accumulates the {@code <message>} children built from the ids passed to
         * {@link #reportedMessageIds(List)}.
         */
        private final List<Stanza> messageChildren = new ArrayList<>();

        /**
         * Accumulates the {@code <call>} children appended via {@link #addCallChild(Stanza)}.
         */
        private final List<Stanza> callChildren = new ArrayList<>();

        /**
         * Constructs an empty builder.
         */
        public Builder() {
        }

        /**
         * Sets the target group JID, naming the group being reported through
         * {@code <spam_list jid>}.
         *
         * @param spamListJid the JID; never {@code null}
         * @return this builder
         * @throws NullPointerException if {@code spamListJid} is {@code null}
         */
        public Builder spamListJid(Jid spamListJid) {
            this.spamListJid = Objects.requireNonNull(spamListJid, "spamListJid cannot be null");
            return this;
        }

        /**
         * Sets the spam-flow identifier, naming the user-facing surface that triggered the report
         * through {@code <spam_list spam_flow>}.
         *
         * @param spamListSpamFlow the spam-flow; never {@code null}
         * @return this builder
         * @throws NullPointerException if {@code spamListSpamFlow} is {@code null}
         */
        public Builder spamListSpamFlow(String spamListSpamFlow) {
            this.spamListSpamFlow = Objects.requireNonNull(spamListSpamFlow,
                    "spamListSpamFlow cannot be null");
            return this;
        }

        /**
         * Sets the optional adder JID, identifying the user who added the reporter to the group
         * through {@code <spam_list source>}.
         *
         * @param spamListSource the JID; may be {@code null}
         * @return this builder
         */
        public Builder spamListSource(Jid spamListSource) {
            this.spamListSource = spamListSource;
            return this;
        }

        /**
         * Sets the optional group subject surfaced for attribution through
         * {@code <spam_list subject>}.
         *
         * @param spamListSubject the subject; may be {@code null}
         * @return this builder
         */
        public Builder spamListSubject(String spamListSubject) {
            this.spamListSubject = spamListSubject;
            return this;
        }

        /**
         * Sets the optional {@code is_known_chat} marker flagging prior history with the group
         * through {@code <spam_list is_known_chat>}.
         *
         * @param spamListIsKnownChat the marker; may be {@code null}
         * @return this builder
         */
        public Builder spamListIsKnownChat(String spamListIsKnownChat) {
            this.spamListIsKnownChat = spamListIsKnownChat;
            return this;
        }

        /**
         * Sets the optional pre-built {@code <frx>} child appended verbatim under the outer
         * {@code <iq>}.
         *
         * <p>The caller is responsible for building the tagset, context and parameters per the
         * WA Web FRX schema.
         *
         * @param frxChild the FRX stanza; may be {@code null}
         * @return this builder
         */
        public Builder frxChild(Stanza frxChild) {
            this.frxChild = frxChild;
            return this;
        }

        /**
         * Appends one {@code <message id="..."/>} child per reported message id to the spam-list
         * payload, building the stanza children internally so callers pass the raw ids captured from
         * the offending chat rather than pre-built stanzas.
         *
         * @implNote
         * WA Web caps the count at 210; this implementation does not enforce the cap and lets the
         * relay reject oversize lists.
         *
         * @param messageIds the reported message ids in report order; never {@code null}, entries
         *                   never {@code null}
         * @return this builder
         * @throws NullPointerException if {@code messageIds} or any entry is {@code null}
         */
        public Builder reportedMessageIds(List<String> messageIds) {
            Objects.requireNonNull(messageIds, "messageIds cannot be null");
            for (var messageId : messageIds) {
                Objects.requireNonNull(messageId, "messageIds entries cannot be null");
                messageChildren.add(new StanzaBuilder()
                        .description("message")
                        .attribute("id", messageId)
                        .build());
            }
            return this;
        }

        /**
         * Appends a pre-built {@code <call>} child to the spam-list payload.
         *
         * @implNote
         * WA Web caps the count at 5; this implementation does not enforce the cap and lets the
         * relay reject oversize lists.
         *
         * @param callStanza the stanza; never {@code null}
         * @return this builder
         * @throws NullPointerException if {@code callStanza} is {@code null}
         */
        public Builder addCallChild(Stanza callStanza) {
            Objects.requireNonNull(callStanza, "callStanza cannot be null");
            callChildren.add(callStanza);
            return this;
        }

        /**
         * Builds an immutable {@link SmaxGroupReportRequest} from the accumulated state.
         *
         * @return a new {@link SmaxGroupReportRequest}; never {@code null}
         * @throws NullPointerException if any required field was not set
         */
        public SmaxGroupReportRequest build() {
            return new SmaxGroupReportRequest(this);
        }
    }
}

package com.github.auties00.cobalt.wire.stanza.smax.support;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Reports an offending status (chat-status) message as an outbound {@code spam} IQ.
 *
 * <p>A report requires the status owner, sender, timestamp and id; the recipient, is-known-chat
 * marker, business-opt-out, business-report and FRX fields are optional. The relay's verdict is
 * parsed by {@link SmaxStatusReportResponse}. Obtain an instance via {@link #builder()}.
 *
 * @implNote
 * This implementation flattens the WA Web mixin chain into a single {@link StanzaBuilder} that pins
 * {@code xmlns="spam"}, {@code to=JidServer.user()} and {@code type="set"}. The biz-opt-out and
 * biz-report children attach to {@code <spam_list>}; only FRX attaches to the outer {@code <iq>}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutSpamStatusReportRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamBaseReportMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamBaseIQSetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamMessageRecipientMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamFRXMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamIsKnownChatMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamBizOptOutMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamBizReportMixin")
public final class SmaxStatusReportRequest implements SmaxStanza.Request {
    /**
     * Holds the status-owner JID routed into {@code <spam_list jid="..."/>}.
     */
    private final Jid spamListJid;

    /**
     * Holds the spam-flow identifier routed into {@code <spam_list spam_flow="..."/>}, naming the
     * user-facing surface that issued the report.
     */
    private final String spamListSpamFlow;

    /**
     * Holds the sender JID of the offending status message, routed into
     * {@code <message from="..."/>}.
     */
    private final Jid messageFrom;

    /**
     * Holds the status message timestamp in Unix seconds, routed into {@code <message t="..."/>}.
     */
    private final long messageTimestamp;

    /**
     * Holds the status message stanza id, routed into {@code <message id="..."/>}.
     */
    private final String messageId;

    /**
     * Holds the optional recipient JID routed into {@code <message to="..."/>}, surfacing the user
     * the status was originally sent to when reporting from a one-to-one timeline.
     */
    private final Jid messageTo;

    /**
     * Holds the optional {@code is_known_chat} marker routed into
     * {@code <spam_list is_known_chat="..."/>}.
     */
    private final String spamListIsKnownChat;

    /**
     * Holds the optional pre-built {@code <biz_opt_out>} child appended under {@code <spam_list>}
     * when present.
     */
    private final Stanza bizOptOutChild;

    /**
     * Holds the optional pre-built {@code <biz_report>} child appended under {@code <spam_list>}
     * when present.
     */
    private final Stanza bizReportChild;

    /**
     * Holds the optional pre-built {@code <frx>} child appended under the outer {@code <iq>} when
     * present.
     */
    private final Stanza frxChild;

    /**
     * Holds the optional pre-built {@code <message>} child.
     *
     * <p>When set, {@link #toStanza()} embeds this stanza verbatim instead of synthesising the
     * {@code (from, t, id, to)} envelope from the scalar fields.
     */
    private final Stanza messageChild;

    /**
     * Constructs a request from the assembled {@link Builder} state.
     *
     * @param builder the source builder; never {@code null}
     */
    private SmaxStatusReportRequest(Builder builder) {
        this.spamListJid = Objects.requireNonNull(builder.spamListJid, "spamListJid cannot be null");
        this.spamListSpamFlow = Objects.requireNonNull(builder.spamListSpamFlow,
                "spamListSpamFlow cannot be null");
        this.messageFrom = Objects.requireNonNull(builder.messageFrom, "messageFrom cannot be null");
        this.messageTimestamp = builder.messageTimestamp;
        this.messageId = Objects.requireNonNull(builder.messageId, "messageId cannot be null");
        this.messageTo = builder.messageTo;
        this.spamListIsKnownChat = builder.spamListIsKnownChat;
        this.bizOptOutChild = builder.bizOptOutChild;
        this.bizReportChild = builder.bizReportChild;
        this.frxChild = builder.frxChild;
        this.messageChild = builder.messageChild;
    }

    /**
     * Returns a new {@link Builder}, the canonical entry point for assembling a status spam
     * report.
     *
     * @return a new builder; never {@code null}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the status-owner JID, the {@code <spam_list jid>} value.
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
     * Returns the sender JID of the offending status message, the {@code <message from>} value.
     *
     * @return the JID; never {@code null}
     */
    public Jid messageFrom() {
        return messageFrom;
    }

    /**
     * Returns the status message timestamp in Unix seconds, the {@code <message t>} value.
     *
     * @return the timestamp
     */
    public long messageTimestamp() {
        return messageTimestamp;
    }

    /**
     * Returns the status message stanza id, the {@code <message id>} value.
     *
     * @return the id; never {@code null}
     */
    public String messageId() {
        return messageId;
    }

    /**
     * Returns the optional recipient JID, the {@code <message to>} value.
     *
     * <p>Empty when the status was reported from a surface other than a one-to-one timeline.
     *
     * @return an {@link Optional} carrying the JID, or empty when omitted
     */
    public Optional<Jid> messageTo() {
        return Optional.ofNullable(messageTo);
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
     * Returns the optional pre-built {@code <biz_opt_out>} child appended under
     * {@code <spam_list>}.
     *
     * @return an {@link Optional} carrying the stanza, or empty when omitted
     */
    public Optional<Stanza> bizOptOutChild() {
        return Optional.ofNullable(bizOptOutChild);
    }

    /**
     * Returns the optional pre-built {@code <biz_report>} child appended under
     * {@code <spam_list>}.
     *
     * @return an {@link Optional} carrying the stanza, or empty when omitted
     */
    public Optional<Stanza> bizReportChild() {
        return Optional.ofNullable(bizReportChild);
    }

    /**
     * Returns the optional pre-built {@code <frx>} child appended under the outer {@code <iq>}.
     *
     * @return an {@link Optional} carrying the stanza, or empty when omitted
     */
    public Optional<Stanza> frxChild() {
        return Optional.ofNullable(frxChild);
    }

    /**
     * Returns the optional pre-built {@code <message>} child.
     *
     * <p>Empty when {@link #toStanza()} synthesises the envelope from the scalar fields.
     *
     * @return an {@link Optional} carrying the stanza, or empty when omitted
     */
    public Optional<Stanza> messageChild() {
        return Optional.ofNullable(messageChild);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds the {@code <message>} child, attaches it under {@code <spam_list>} together with
     * the optional biz-opt-out and biz-report children, and appends the optional FRX child to the
     * outer {@code <iq>}.
     *
     * @implNote
     * This implementation embeds {@link #messageChild} verbatim when set, otherwise synthesises a
     * {@code <message>} envelope from the {@code (from, t, id, to)} scalar fields.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutSpamStatusReportRequest",
            exports = "makeStatusReportRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        Stanza messageStanza;
        if (messageChild != null) {
            messageStanza = messageChild;
        } else {
            var messageBuilder = new StanzaBuilder()
                    .description("message")
                    .attribute("from", messageFrom)
                    .attribute("t", messageTimestamp)
                    .attribute("id", messageId);
            if (messageTo != null) {
                messageBuilder.attribute("to", messageTo);
            }
            messageStanza = messageBuilder.build();
        }
        var spamListBuilder = new StanzaBuilder()
                .description("spam_list")
                .attribute("jid", spamListJid)
                .attribute("spam_flow", spamListSpamFlow);
        if (spamListIsKnownChat != null) {
            spamListBuilder.attribute("is_known_chat", spamListIsKnownChat);
        }
        spamListBuilder.content(messageStanza);
        if (bizOptOutChild != null) {
            spamListBuilder.content(bizOptOutChild);
        }
        if (bizReportChild != null) {
            spamListBuilder.content(bizReportChild);
        }
        var iqBuilder = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "spam")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(spamListBuilder.build());
        if (frxChild != null) {
            iqBuilder.content(frxChild);
        }
        return iqBuilder;
    }

    /**
     * Compares this request to another for value equality across all fields.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal {@link SmaxStatusReportRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxStatusReportRequest) obj;
        return this.messageTimestamp == that.messageTimestamp
                && Objects.equals(this.spamListJid, that.spamListJid)
                && Objects.equals(this.spamListSpamFlow, that.spamListSpamFlow)
                && Objects.equals(this.messageFrom, that.messageFrom)
                && Objects.equals(this.messageId, that.messageId)
                && Objects.equals(this.messageTo, that.messageTo)
                && Objects.equals(this.spamListIsKnownChat, that.spamListIsKnownChat)
                && Objects.equals(this.bizOptOutChild, that.bizOptOutChild)
                && Objects.equals(this.bizReportChild, that.bizReportChild)
                && Objects.equals(this.frxChild, that.frxChild)
                && Objects.equals(this.messageChild, that.messageChild);
    }

    /**
     * Returns a hash code derived from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(spamListJid, spamListSpamFlow, messageFrom, messageTimestamp, messageId,
                messageTo, spamListIsKnownChat, bizOptOutChild, bizReportChild, frxChild, messageChild);
    }

    /**
     * Returns a debug string listing the scalar fields.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxStatusReportRequest[spamListJid=" + spamListJid
                + ", spamListSpamFlow=" + spamListSpamFlow
                + ", messageFrom=" + messageFrom
                + ", messageTimestamp=" + messageTimestamp
                + ", messageId=" + messageId
                + ", messageTo=" + messageTo + ']';
    }

    /**
     * Assembles a {@link SmaxStatusReportRequest} from the WA Web mixin inputs.
     *
     * <p>The required {@link #spamListJid}, {@link #spamListSpamFlow}, {@link #messageFrom} and
     * {@link #messageId} are validated at setter time; the remaining fields are optional. Obtain
     * an instance via {@link SmaxStatusReportRequest#builder()}.
     *
     * @implNote
     * This implementation collects the optional mixin payloads into private fields and produces an
     * immutable copy from {@link #build()}.
     */
    public static final class Builder {
        /**
         * Holds the required status-owner JID, set via {@link #spamListJid(Jid)}.
         */
        private Jid spamListJid;

        /**
         * Holds the required spam-flow string, set via {@link #spamListSpamFlow(String)}.
         */
        private String spamListSpamFlow;

        /**
         * Holds the required sender JID, set via {@link #messageFrom(Jid)}.
         */
        private Jid messageFrom;

        /**
         * Holds the status message timestamp in Unix seconds, set via
         * {@link #messageTimestamp(long)}; defaults to {@code 0}.
         */
        private long messageTimestamp;

        /**
         * Holds the required status message stanza id, set via {@link #messageId(String)}.
         */
        private String messageId;

        /**
         * Holds the optional recipient JID, set via {@link #messageTo(Jid)}.
         */
        private Jid messageTo;

        /**
         * Holds the optional {@code is_known_chat} marker, set via
         * {@link #spamListIsKnownChat(String)}.
         */
        private String spamListIsKnownChat;

        /**
         * Holds the optional pre-built {@code <biz_opt_out>} child, set via
         * {@link #bizOptOutChild(Stanza)}.
         */
        private Stanza bizOptOutChild;

        /**
         * Holds the optional pre-built {@code <biz_report>} child, set via
         * {@link #bizReportChild(Stanza)}.
         */
        private Stanza bizReportChild;

        /**
         * Holds the optional pre-built {@code <frx>} child, set via {@link #frxChild(Stanza)}.
         */
        private Stanza frxChild;

        /**
         * Holds the optional pre-built {@code <message>} child, set via
         * {@link #messageChild(Stanza)}; {@code null} when {@link #toStanza()} should synthesise the
         * envelope from the scalar fields.
         */
        private Stanza messageChild;

        /**
         * Constructs an empty builder.
         */
        public Builder() {
        }

        /**
         * Sets the status-owner JID, naming the status owner through {@code <spam_list jid>}.
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
         * Sets the sender JID of the offending status, routed into {@code <message from>}.
         *
         * @param messageFrom the JID; never {@code null}
         * @return this builder
         * @throws NullPointerException if {@code messageFrom} is {@code null}
         */
        public Builder messageFrom(Jid messageFrom) {
            this.messageFrom = Objects.requireNonNull(messageFrom, "messageFrom cannot be null");
            return this;
        }

        /**
         * Sets the status message timestamp in Unix seconds, routed into {@code <message t>}.
         *
         * @param messageTimestamp the timestamp
         * @return this builder
         */
        public Builder messageTimestamp(long messageTimestamp) {
            this.messageTimestamp = messageTimestamp;
            return this;
        }

        /**
         * Sets the status message stanza id, routed into {@code <message id>}.
         *
         * @param messageId the id; never {@code null}
         * @return this builder
         * @throws NullPointerException if {@code messageId} is {@code null}
         */
        public Builder messageId(String messageId) {
            this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
            return this;
        }

        /**
         * Sets the optional recipient JID routed into {@code <message to>} when set, surfacing the
         * user the status was originally sent to.
         *
         * @param messageTo the JID; may be {@code null}
         * @return this builder
         */
        public Builder messageTo(Jid messageTo) {
            this.messageTo = messageTo;
            return this;
        }

        /**
         * Sets the optional {@code is_known_chat} marker routed into
         * {@code <spam_list is_known_chat>} when set.
         *
         * @param spamListIsKnownChat the marker; may be {@code null}
         * @return this builder
         */
        public Builder spamListIsKnownChat(String spamListIsKnownChat) {
            this.spamListIsKnownChat = spamListIsKnownChat;
            return this;
        }

        /**
         * Sets the optional pre-built {@code <biz_opt_out>} child routed under {@code <spam_list>}
         * when set, meaningful only for reports against business statuses.
         *
         * @param bizOptOutChild the stanza; may be {@code null}
         * @return this builder
         */
        public Builder bizOptOutChild(Stanza bizOptOutChild) {
            this.bizOptOutChild = bizOptOutChild;
            return this;
        }

        /**
         * Sets the optional pre-built {@code <biz_report>} child routed under {@code <spam_list>}
         * when set, meaningful only for reports against business statuses.
         *
         * @param bizReportChild the stanza; may be {@code null}
         * @return this builder
         */
        public Builder bizReportChild(Stanza bizReportChild) {
            this.bizReportChild = bizReportChild;
            return this;
        }

        /**
         * Sets the optional pre-built {@code <frx>} child routed under the outer {@code <iq>}
         * when set.
         *
         * @param frxChild the stanza; may be {@code null}
         * @return this builder
         */
        public Builder frxChild(Stanza frxChild) {
            this.frxChild = frxChild;
            return this;
        }

        /**
         * Sets the optional pre-built {@code <message>} child.
         *
         * <p>When set, {@link #toStanza()} embeds the supplied stanza verbatim instead of synthesising
         * the envelope from the scalar fields.
         *
         * @param messageChild the stanza; may be {@code null}
         * @return this builder
         */
        public Builder messageChild(Stanza messageChild) {
            this.messageChild = messageChild;
            return this;
        }

        /**
         * Builds an immutable {@link SmaxStatusReportRequest} from the accumulated state.
         *
         * @return a new {@link SmaxStatusReportRequest}; never {@code null}
         * @throws NullPointerException if any required field was not set
         */
        public SmaxStatusReportRequest build() {
            return new SmaxStatusReportRequest(this);
        }
    }
}

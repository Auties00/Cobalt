package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the outbound {@code <iq type="set" xmlns="w:g2">} stanza that approves, rejects, or cancels pending
 * sub-group suggestions against a community parent group.
 *
 * <p>This request backs the community-admin approve, reject, and cancel actions on the sub-group suggestion
 * queue. The relay processes all three sub-actions in one request and returns per-suggestion echo rows in the
 * matching {@link SmaxGroupsSubGroupSuggestionsActionResponse.Success}. Each sub-action list is capped at 1000
 * entries, and at least one of the three lists must be non-empty.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsSubGroupSuggestionsActionRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsSubGroupSuggestionsActionRequest implements SmaxStanza.Request {
    /**
     * The parent (community) group {@link Jid} that anchors the suggestion queue.
     */
    private final Jid parentGroupJid;

    /**
     * The suggestions to approve. Each entry must carry both a {@code creator} and a {@code jid} attribute.
     */
    private final List<CreatorSuggestion> approve;

    /**
     * The suggestions to reject. Each entry must carry both a {@code creator} and a {@code jid} attribute.
     */
    private final List<CreatorSuggestion> reject;

    /**
     * The suggestions to cancel. Each entry carries only the {@code jid} attribute.
     */
    private final List<JidSuggestion> cancel;

    /**
     * Constructs a sub-group-suggestions-action request.
     *
     * <p>The relay caps each of the three sub-action lists at 1000 entries, so a caller batching wider
     * suggestion-queue mutations should split the work across multiple requests. The three lists are copied so
     * post-construction mutation of the caller's lists has no effect on the request.
     *
     * @param parentGroupJid the parent community {@link Jid}
     * @param approve        the approve list
     * @param reject         the reject list
     * @param cancel         the cancel list
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException when every list is empty
     */
    public SmaxGroupsSubGroupSuggestionsActionRequest(Jid parentGroupJid,
                   List<CreatorSuggestion> approve,
                   List<CreatorSuggestion> reject,
                   List<JidSuggestion> cancel) {
        Objects.requireNonNull(parentGroupJid, "parentGroupJid cannot be null");
        Objects.requireNonNull(approve, "approve cannot be null");
        Objects.requireNonNull(reject, "reject cannot be null");
        Objects.requireNonNull(cancel, "cancel cannot be null");
        if (approve.isEmpty() && reject.isEmpty() && cancel.isEmpty()) {
            throw new IllegalArgumentException("at least one of approve/reject/cancel must be non-empty");
        }
        this.parentGroupJid = parentGroupJid;
        this.approve = List.copyOf(approve);
        this.reject = List.copyOf(reject);
        this.cancel = List.copyOf(cancel);
    }

    /**
     * Returns the parent (community) group {@link Jid}.
     *
     * <p>The value routes verbatim into the IQ's {@code to} attribute.
     *
     * @return the parent group {@link Jid}; never {@code null}
     */
    public Jid parentGroupJid() {
        return parentGroupJid;
    }

    /**
     * Returns the approve list.
     *
     * @return an unmodifiable list of {@link CreatorSuggestion} entries; never {@code null}
     */
    public List<CreatorSuggestion> approve() {
        return approve;
    }

    /**
     * Returns the reject list.
     *
     * @return an unmodifiable list of {@link CreatorSuggestion} entries; never {@code null}
     */
    public List<CreatorSuggestion> reject() {
        return reject;
    }

    /**
     * Returns the cancel list.
     *
     * @return an unmodifiable list of {@link JidSuggestion} entries; never {@code null}
     */
    public List<JidSuggestion> cancel() {
        return cancel;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * <p>The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<parentGroupJid>" type="set">
     *         <sub_group_suggestions_action>
     *             <approve>
     *                 <sub_group_suggestion creator="..." jid="..." creator_pn="..."/>
     *                 ...
     *             </approve>
     *             <reject>
     *                 <sub_group_suggestion creator="..." jid="..." creator_pn="..."/>
     *                 ...
     *             </reject>
     *             <cancel>
     *                 <sub_group_suggestion jid="..."/>
     *                 ...
     *             </cancel>
     *         </sub_group_suggestions_action>
     *     </iq>
     * }
     * A sub-action child is omitted when the corresponding caller list is empty.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <sub_group_suggestions_action>}
     *         payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsSubGroupSuggestionsActionRequest",
            exports = "makeSubGroupSuggestionsActionRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var actionChildren = new ArrayList<Stanza>();
        if (!approve.isEmpty()) {
            var entries = new ArrayList<Stanza>();
            for (var entry : approve) {
                entries.add(entry.toStanza());
            }
            var approveNode = new StanzaBuilder()
                    .description("approve")
                    .content(entries)
                    .build();
            actionChildren.add(approveNode);
        }
        if (!reject.isEmpty()) {
            var entries = new ArrayList<Stanza>();
            for (var entry : reject) {
                entries.add(entry.toStanza());
            }
            var rejectNode = new StanzaBuilder()
                    .description("reject")
                    .content(entries)
                    .build();
            actionChildren.add(rejectNode);
        }
        if (!cancel.isEmpty()) {
            var entries = new ArrayList<Stanza>();
            for (var entry : cancel) {
                entries.add(entry.toStanza());
            }
            var cancelNode = new StanzaBuilder()
                    .description("cancel")
                    .content(entries)
                    .build();
            actionChildren.add(cancelNode);
        }
        var actionNode = new StanzaBuilder()
                .description("sub_group_suggestions_action")
                .content(actionChildren)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", parentGroupJid)
                .attribute("type", "set")
                .content(actionNode);
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsSubGroupSuggestionsActionRequest} with
     *         identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsSubGroupSuggestionsActionRequest) obj;
        return Objects.equals(this.parentGroupJid, that.parentGroupJid)
                && Objects.equals(this.approve, that.approve)
                && Objects.equals(this.reject, that.reject)
                && Objects.equals(this.cancel, that.cancel);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(parentGroupJid, approve, reject, cancel);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsSubGroupSuggestionsActionRequest[parentGroupJid=" + parentGroupJid
                + ", approve=" + approve
                + ", reject=" + reject
                + ", cancel=" + cancel + ']';
    }

    /**
     * Represents a suggestion entry carrying the {@code creator}/{@code jid} pair used by the approve and reject
     * lists.
     *
     * <p>The {@code creator} attribute carries the {@link Jid} of the user who originally proposed the
     * suggestion; the optional {@code creator_pn} carries the corresponding phone-number JID echo when the
     * suggestion was filed under the LID addressing mode.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutGroupsSubGroupSuggestionMixin")
    public static final class CreatorSuggestion {
        /**
         * The {@link Jid} of the user who proposed the suggestion.
         */
        private final Jid creator;

        /**
         * The proposed sub-group {@link Jid}.
         */
        private final Jid jid;

        /**
         * The optional creator phone-number {@link Jid}.
         */
        private final Jid creatorPn;

        /**
         * Constructs a creator-suggestion entry.
         *
         * @param creator   the creator {@link Jid}
         * @param jid       the sub-group {@link Jid}
         * @param creatorPn the optional creator phone-number {@link Jid}; may be {@code null}
         * @throws NullPointerException if {@code creator} or {@code jid} is {@code null}
         */
        public CreatorSuggestion(Jid creator, Jid jid, Jid creatorPn) {
            this.creator = Objects.requireNonNull(creator, "creator cannot be null");
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
            this.creatorPn = creatorPn;
        }

        /**
         * Returns the creator {@link Jid}.
         *
         * @return the creator {@link Jid}; never {@code null}
         */
        public Jid creator() {
            return creator;
        }

        /**
         * Returns the sub-group {@link Jid}.
         *
         * @return the sub-group {@link Jid}; never {@code null}
         */
        public Jid jid() {
            return jid;
        }

        /**
         * Returns the optional creator phone-number {@link Jid}.
         *
         * @return an {@link Optional} carrying the phone-number {@link Jid}, or empty when omitted
         */
        public Optional<Jid> creatorPn() {
            return Optional.ofNullable(creatorPn);
        }

        /**
         * Materialises the {@code <sub_group_suggestion/>} child carrying this entry.
         *
         * @return the materialised {@link Stanza}
         */
        public Stanza toStanza() {
            var builder = new StanzaBuilder()
                    .description("sub_group_suggestion")
                    .attribute("creator", creator)
                    .attribute("jid", jid);
            if (creatorPn != null) {
                builder.attribute("creator_pn", creatorPn);
            }
            return builder.build();
        }

        /**
         * Compares this entry to {@code obj} for value equality across every field.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link CreatorSuggestion} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (CreatorSuggestion) obj;
            return Objects.equals(this.creator, that.creator)
                    && Objects.equals(this.jid, that.jid)
                    && Objects.equals(this.creatorPn, that.creatorPn);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(creator, jid, creatorPn);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsSubGroupSuggestionsActionRequest.CreatorSuggestion[creator=" + creator
                    + ", jid=" + jid
                    + ", creatorPn=" + creatorPn + ']';
        }
    }

    /**
     * Represents a suggestion entry carrying only the {@code jid} used by the cancel list.
     *
     * <p>The cancelling caller is implicit; the relay enforces ownership server-side, so cancel entries omit the
     * {@code creator} attribute.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutGroupsSubGroupSuggestionWithoutCreatorMixin")
    public static final class JidSuggestion {
        /**
         * The proposed sub-group {@link Jid} to cancel.
         */
        private final Jid jid;

        /**
         * Constructs a jid-only suggestion entry.
         *
         * @param jid the sub-group {@link Jid}
         * @throws NullPointerException if {@code jid} is {@code null}
         */
        public JidSuggestion(Jid jid) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        }

        /**
         * Returns the sub-group {@link Jid}.
         *
         * @return the sub-group {@link Jid}; never {@code null}
         */
        public Jid jid() {
            return jid;
        }

        /**
         * Materialises the {@code <sub_group_suggestion jid="..."/>} child carrying this entry.
         *
         * @return the materialised {@link Stanza}
         */
        public Stanza toStanza() {
            return new StanzaBuilder()
                    .description("sub_group_suggestion")
                    .attribute("jid", jid)
                    .build();
        }

        /**
         * Compares this entry to {@code obj} for value equality on {@link #jid()}.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link JidSuggestion} with the same {@link Jid}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (JidSuggestion) obj;
            return Objects.equals(this.jid, that.jid);
        }

        /**
         * Returns a hash derived from {@link #jid()}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(jid);
        }

        /**
         * Returns a debug string carrying {@link #jid()}.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsSubGroupSuggestionsActionRequest.JidSuggestion[jid=" + jid + ']';
        }
    }
}

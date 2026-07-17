package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Posts a new sub-group suggestion inside a community as an {@code <iq type="set" xmlns="w:g2">} stanza.
 *
 * <p>The suggestion either proposes a fresh sub-group to spin up or names one or more existing groups to absorb;
 * the body is a single {@code <sub_group_suggestion/>} child whose shape is decided by the {@link #suggestion()}
 * oneof. Callers pair this request with {@link SmaxGroupsCreateSubGroupSuggestionResponse} to read the relay's
 * verdict.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsCreateSubGroupSuggestionRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsCreateSubGroupSuggestionRequest implements SmaxStanza.Request {
    /**
     * Holds the parent community {@link Jid} routed verbatim into the IQ envelope's {@code to} attribute.
     */
    private final Jid parentGroupJid;

    /**
     * Holds the suggestion-body oneof choosing between
     * {@link SmaxGroupsCreateSubGroupSuggestionSuggestion.NewGroup} and
     * {@link SmaxGroupsCreateSubGroupSuggestionSuggestion.ExistingGroups}.
     */
    private final SmaxGroupsCreateSubGroupSuggestionSuggestion suggestion;

    /**
     * Constructs a request targeting the given community with the supplied suggestion body.
     *
     * <p>The request envelope is identical for both suggestion variants; the caller selects the
     * {@link SmaxGroupsCreateSubGroupSuggestionSuggestion} variant that matches the user-facing action.
     *
     * @param parentGroupJid the parent community {@link Jid}; never {@code null}
     * @param suggestion     the suggestion body; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SmaxGroupsCreateSubGroupSuggestionRequest(Jid parentGroupJid, SmaxGroupsCreateSubGroupSuggestionSuggestion suggestion) {
        this.parentGroupJid = Objects.requireNonNull(parentGroupJid, "parentGroupJid cannot be null");
        this.suggestion = Objects.requireNonNull(suggestion, "suggestion cannot be null");
    }

    /**
     * Returns the parent community {@link Jid} targeted by this request.
     *
     * <p>The returned value is the one rendered into the IQ envelope's {@code to} attribute.
     *
     * @return the parent group {@link Jid}; never {@code null}
     */
    public Jid parentGroupJid() {
        return parentGroupJid;
    }

    /**
     * Returns the suggestion-body oneof.
     *
     * <p>Callers discriminate between the new-group and existing-groups branches with {@code instanceof}.
     *
     * @return the suggestion; never {@code null}
     */
    public SmaxGroupsCreateSubGroupSuggestionSuggestion suggestion() {
        return suggestion;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation builds a single {@code <sub_group_suggestion/>} child, lets
     * {@link SmaxGroupsCreateSubGroupSuggestionSuggestion#contributeTo(StanzaBuilder)} stamp the branch-specific
     * attributes and children, then wraps it in the {@code <iq xmlns="w:g2" type="set">} envelope addressed to
     * {@link #parentGroupJid()}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsCreateSubGroupSuggestionRequest",
            exports = "makeCreateSubGroupSuggestionRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var suggestionBuilder = new StanzaBuilder()
                .description("sub_group_suggestion");
        suggestion.contributeTo(suggestionBuilder);
        var suggestionNode = suggestionBuilder.build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", parentGroupJid)
                .attribute("type", "set")
                .content(suggestionNode);
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsCreateSubGroupSuggestionRequest} with
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
        var that = (SmaxGroupsCreateSubGroupSuggestionRequest) obj;
        return Objects.equals(this.parentGroupJid, that.parentGroupJid)
                && Objects.equals(this.suggestion, that.suggestion);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(parentGroupJid, suggestion);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsCreateSubGroupSuggestionRequest[parentGroupJid=" + parentGroupJid
                + ", suggestion=" + suggestion + ']';
    }
}

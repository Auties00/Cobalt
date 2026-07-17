package com.github.auties00.cobalt.wire.stanza.iq.status;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.privacy.StatusPrivacyMode;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Models the outbound legacy-IQ stanza that sets the calling user's status-privacy setting.
 *
 * <p>Dispatching this request as an {@link IqStanza.Request} produces an
 * {@code <iq xmlns="status" type="set">} envelope addressed to {@link JidServer#user()} and wrapping a
 * single {@code <privacy list="...">} child whose {@code <user jid="...">} grandchildren carry the
 * paired JID list. The relay acknowledges the change with one of the
 * {@link IqSetStatusPrivacyResponse} variants. This request is the write counterpart of
 * {@link IqQueryStatusPrivacyRequest}.
 */
@WhatsAppWebModule(moduleName = "WAWebStatusSetAndSyncPrivacy")
public final class IqSetStatusPrivacyRequest implements IqStanza.Request {
    /**
     * Holds the audience selector serialised into the {@code list} attribute of the
     * {@code <privacy>} child.
     */
    private final StatusPrivacyMode mode;

    /**
     * Holds the JIDs fanned out as {@code <user jid="...">} grandchildren, in wire order.
     *
     * <p>{@code null} entries are skipped when the stanza is built. Never {@code null}.
     */
    private final List<Jid> jids;

    /**
     * Constructs a set-status-privacy request bound to the given mode and JID list.
     *
     * @param mode the audience selector; never {@code null}
     * @param jids the paired JID list; never {@code null}
     * @throws NullPointerException if {@code mode} or {@code jids} is {@code null}
     */
    public IqSetStatusPrivacyRequest(StatusPrivacyMode mode, List<Jid> jids) {
        this.mode = Objects.requireNonNull(mode, "mode cannot be null");
        Objects.requireNonNull(jids, "jids cannot be null");
        this.jids = List.copyOf(jids);
    }

    /**
     * Returns the audience selector bound to this request.
     *
     * @return the mode; never {@code null}
     */
    public StatusPrivacyMode mode() {
        return mode;
    }

    /**
     * Returns the JIDs bound to this request.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<Jid> jids() {
        return jids;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code <iq xmlns="status" type="set">} envelope addressed to
     * {@link JidServer#user()} wrapping a single {@code <privacy list="...">} child whose
     * {@code list} attribute is the wire token for the bound {@link #mode()} and whose
     * {@code <user jid="...">} grandchildren carry the bound {@link #jids()} in order (skipping any
     * {@code null} entry). The IQ {@code id} attribute is assigned by the dispatch layer.
     *
     * @implNote This implementation maps {@link StatusPrivacyMode#CONTACTS} to {@code "contacts"},
     * {@link StatusPrivacyMode#WHITELIST} to {@code "contact_whitelist"}, and
     * {@link StatusPrivacyMode#CONTACTS_EXCEPT} to {@code "contact_blacklist"}.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the {@code <privacy>}
     *         payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStatusSetAndSyncPrivacy", exports = "setAndSyncStatusPrivacy",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var listAttr = switch (mode) {
            case CONTACTS -> "contacts";
            case WHITELIST -> "contact_whitelist";
            case CONTACTS_EXCEPT -> "contact_blacklist";
        };
        var userChildren = new ArrayList<Stanza>(jids.size());
        for (var jid : jids) {
            if (jid == null) {
                continue;
            }
            userChildren.add(new StanzaBuilder()
                    .description("user")
                    .attribute("jid", jid)
                    .build());
        }
        var privacyNode = new StanzaBuilder()
                .description("privacy")
                .attribute("list", listAttr)
                .content(userChildren)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "status")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(privacyNode);
    }

    /**
     * Compares this request with another object for value equality on the mode and JID list.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is an {@link IqSetStatusPrivacyRequest} carrying an equal
     *         mode and JID list, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSetStatusPrivacyRequest) obj;
        return this.mode == that.mode
                && Objects.equals(this.jids, that.jids);
    }

    /**
     * Returns a hash code derived from the mode and JID list.
     *
     * @return the field-derived hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(mode, jids);
    }

    /**
     * Returns a debug string rendering the mode and JID list.
     *
     * @return a string representation of this request
     */
    @Override
    public String toString() {
        return "IqSetStatusPrivacyRequest[mode=" + mode + ", jids=" + jids + ']';
    }
}

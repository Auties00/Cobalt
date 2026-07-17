package com.github.auties00.cobalt.wire.stanza.smax.abprops;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Builds the outbound {@code <iq xmlns="abt" type="get" to="s.whatsapp.net">} stanza that fetches
 * the group-scoped AB-props experiment-config bundle.
 *
 * <p>This request fetches the per-group experiment overrides that layer on top of the user-scoped
 * bundle fetched by {@link SmaxAbPropsGetExperimentConfigRequest}. It is dispatched once per group
 * the caller wants to sync, typically when a group becomes active. The {@code groupJid} is required
 * and is routed into the {@code <props group/>} attribute; an optional {@code propsHash} lets the
 * relay short-circuit the reply to a delta when its current bundle for that group already matches.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutAbPropsGetGroupExperimentConfigRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutAbPropsBaseIQGetRequestMixin")
public final class SmaxAbPropsGetGroupExperimentConfigRequest implements SmaxStanza.Request {
    /**
     * Holds the group JID whose experiment configuration is requested.
     *
     * <p>Serialised verbatim into the {@code <props group/>} attribute; never {@code null}.
     */
    private final Jid groupJid;

    /**
     * Holds the cached bundle hash echoed back to the relay, or {@code null} when none is known.
     *
     * <p>When present, this hash is serialised into the {@code <props hash/>} attribute so the relay
     * can reply with a delta when its current bundle for the group already matches.
     */
    private final String propsHash;

    /**
     * Constructs a conditional request for the given group carrying the supplied cached hash.
     *
     * <p>Supplying a non-{@code null} hash lets the relay short-circuit the reply to a delta when its
     * current bundle for the group already matches.
     *
     * @param groupJid  the target group JID; never {@code null}
     * @param propsHash the cached props hash, or {@code null} when none is known
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    public SmaxAbPropsGetGroupExperimentConfigRequest(Jid groupJid, String propsHash) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        this.propsHash = propsHash;
    }

    /**
     * Constructs an unconditional request for the given group carrying no cached hash.
     *
     * <p>Used on the first fetch for a group, when no cached bundle exists.
     *
     * @param groupJid the target group JID; never {@code null}
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    public SmaxAbPropsGetGroupExperimentConfigRequest(Jid groupJid) {
        this(groupJid, null);
    }

    /**
     * Returns the target group JID.
     *
     * @return the group JID; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns the cached props hash, when one was supplied.
     *
     * <p>Empty on the first fetch for the group.
     *
     * @return an {@link Optional} carrying the hash, or empty when none was supplied
     */
    public Optional<String> propsHash() {
        return Optional.ofNullable(propsHash);
    }

    /**
     * Builds the outbound {@code <iq xmlns="abt" type="get">} stanza wrapping the {@code <props/>}
     * payload.
     *
     * <p>The {@code <props/>} child always carries the {@code group} attribute and, when set, the
     * cached {@code hash} attribute that lets the relay short-circuit to a delta.
     *
     * @implSpec
     * The builder is returned unbuilt so the dispatch path can stamp a fresh {@code id} attribute
     * before flushing the stanza.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <props/>} payload; never
     *         {@code null}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutAbPropsGetGroupExperimentConfigRequest",
            exports = "makeGetGroupExperimentConfigRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var propsBuilder = new StanzaBuilder()
                .description("props")
                .attribute("group", groupJid);
        if (propsHash != null) {
            propsBuilder.attribute("hash", propsHash);
        }
        var propsNode = propsBuilder.build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "abt")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(propsNode);
    }

    /**
     * Compares this request with another for value equality over the group JID and cached hash.
     *
     * @param obj the object to compare against, may be {@code null}
     * @return {@code true} when {@code obj} is a {@code SmaxAbPropsGetGroupExperimentConfigRequest}
     *         with equal {@code groupJid} and {@code propsHash}; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxAbPropsGetGroupExperimentConfigRequest) obj;
        return Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.propsHash, that.propsHash);
    }

    /**
     * Returns a hash code derived from the group JID and cached hash.
     *
     * @return the combined hash of {@code groupJid} and {@code propsHash}
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid, propsHash);
    }

    /**
     * Returns a debug representation listing the group JID and cached hash.
     *
     * @return a string of the form {@code SmaxAbPropsGetGroupExperimentConfigRequest[groupJid=..., propsHash=...]}
     */
    @Override
    public String toString() {
        return "SmaxAbPropsGetGroupExperimentConfigRequest[groupJid=" + groupJid
                + ", propsHash=" + propsHash + ']';
    }
}

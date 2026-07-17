package com.github.auties00.cobalt.wire.stanza.smax.abprops;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Builds the outbound {@code <iq xmlns="abt" type="get" to="s.whatsapp.net">} stanza that fetches
 * the AB-props experiment-config bundle.
 *
 * <p>This request fetches the user-scoped experiment configuration bundle that gates WhatsApp's
 * feature flags. It is dispatched on session bootstrap and again whenever the relay pushes a
 * {@code <notification type="abprops">} bump, and the matching
 * {@link SmaxAbPropsGetExperimentConfigResponse} populates the local AB-prop store the runtime
 * feature gates read from. Either {@code propsHash} or {@code propsRefreshId} may be carried so the
 * relay can short-circuit the reply to a delta when its current bundle already matches; both are
 * absent on the first fetch of a session, which forces a full bundle reply.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutAbPropsGetExperimentConfigRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutAbPropsBaseIQGetRequestMixin")
public final class SmaxAbPropsGetExperimentConfigRequest implements SmaxStanza.Request {
    /**
     * Holds the cached bundle hash echoed back to the relay, or {@code null} when none is known.
     *
     * <p>When present, this hash is serialised into the {@code <props hash/>} attribute so the relay
     * can compare it against its current bundle and reply with a delta instead of the full payload.
     */
    private final String propsHash;

    /**
     * Holds the cached refresh id echoed back to the relay, or {@code null} when none is known.
     *
     * <p>When present, this id is serialised into the {@code <props refresh_id/>} attribute so the
     * relay can correlate this fetch with a prior server-pushed {@code <notification type="abprops">}
     * bump.
     */
    private final Integer propsRefreshId;

    /**
     * Constructs a conditional request carrying the supplied cached identifiers.
     *
     * <p>Both arguments are nullable; supplying either lets the relay short-circuit the reply to a
     * delta when its current bundle already matches. Supplying neither is equivalent to
     * {@link #SmaxAbPropsGetExperimentConfigRequest()}.
     *
     * @param propsHash      the cached props hash, or {@code null} when none is known
     * @param propsRefreshId the cached refresh id, or {@code null} when none is known
     */
    public SmaxAbPropsGetExperimentConfigRequest(String propsHash, Integer propsRefreshId) {
        this.propsHash = propsHash;
        this.propsRefreshId = propsRefreshId;
    }

    /**
     * Constructs an unconditional request carrying no cached identifiers.
     *
     * <p>Used on the first fetch of a session, when no cached bundle exists and the relay must
     * return the full props bundle.
     */
    public SmaxAbPropsGetExperimentConfigRequest() {
        this(null, null);
    }

    /**
     * Returns the cached props hash, when one was supplied.
     *
     * <p>Empty on the first fetch of a session.
     *
     * @return an {@link Optional} carrying the hash, or empty when none was supplied
     */
    public Optional<String> propsHash() {
        return Optional.ofNullable(propsHash);
    }

    /**
     * Returns the cached refresh id, when one was supplied.
     *
     * <p>Empty when the client never received a prior refresh-id bump.
     *
     * @return an {@link Optional} carrying the refresh id, or empty when none was supplied
     */
    public Optional<Integer> propsRefreshId() {
        return Optional.ofNullable(propsRefreshId);
    }

    /**
     * Builds the outbound {@code <iq xmlns="abt" type="get">} stanza wrapping the {@code <props/>}
     * payload.
     *
     * <p>The {@code <props/>} child always carries {@code protocol="1"} and, when set, the cached
     * {@code hash} and {@code refresh_id} attributes that let the relay short-circuit to a delta.
     *
     * @implSpec
     * The builder is returned unbuilt so the dispatch path can stamp a fresh {@code id} attribute
     * before flushing the stanza.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <props/>} payload; never
     *         {@code null}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutAbPropsGetExperimentConfigRequest",
            exports = "makeGetExperimentConfigRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var propsBuilder = new StanzaBuilder()
                .description("props")
                .attribute("protocol", "1");
        if (propsHash != null) {
            propsBuilder.attribute("hash", propsHash);
        }
        if (propsRefreshId != null) {
            propsBuilder.attribute("refresh_id", propsRefreshId);
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
     * Compares this request with another for value equality over its cached identifiers.
     *
     * @param obj the object to compare against, may be {@code null}
     * @return {@code true} when {@code obj} is a {@code SmaxAbPropsGetExperimentConfigRequest} with
     *         equal {@code propsHash} and {@code propsRefreshId}; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxAbPropsGetExperimentConfigRequest) obj;
        return Objects.equals(this.propsHash, that.propsHash)
                && Objects.equals(this.propsRefreshId, that.propsRefreshId);
    }

    /**
     * Returns a hash code derived from the cached identifiers.
     *
     * @return the combined hash of {@code propsHash} and {@code propsRefreshId}
     */
    @Override
    public int hashCode() {
        return Objects.hash(propsHash, propsRefreshId);
    }

    /**
     * Returns a debug representation listing the cached identifiers.
     *
     * @return a string of the form {@code SmaxAbPropsGetExperimentConfigRequest[propsHash=..., propsRefreshId=...]}
     */
    @Override
    public String toString() {
        return "SmaxAbPropsGetExperimentConfigRequest[propsHash=" + propsHash
                + ", propsRefreshId=" + propsRefreshId + ']';
    }
}

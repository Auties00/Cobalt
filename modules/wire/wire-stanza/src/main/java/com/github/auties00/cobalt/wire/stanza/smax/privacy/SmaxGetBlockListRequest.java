package com.github.auties00.cobalt.wire.stanza.smax.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Builds the outbound {@code <iq xmlns="blocklist" type="get">} stanza that fetches the user's blocked-contacts list.
 *
 * <p>This stanza backs the Settings blocked-contacts surface. When the caller supplies the digest of the locally
 * cached blocklist, the relay can short-circuit to a {@link SmaxGetBlockListResponse.SuccessWithMatch} reply when
 * that cache is still authoritative; otherwise it returns the full list. The reply is parsed by
 * {@link SmaxGetBlockListResponse}.
 *
 * @implNote This implementation models the optional {@code <item dhash/>} child as a single nullable field; the
 * LID-versus-PN addressing dispatch lives entirely on the response side, since the request envelope is identical
 * on both wires.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBlocklistsGetBlockListRequest")
public final class SmaxGetBlockListRequest implements SmaxStanza.Request {
    /**
     * The cached digest of the local blocklist, or {@code null} to request a fresh full list.
     *
     * <p>A {@code null} value forces the relay to return a full mismatch body rather than a cache-match
     * short-circuit.
     */
    private final String itemDhash;

    /**
     * Constructs a get-blocklist request.
     *
     * <p>Pass {@code null} for {@code itemDhash} on first boot or whenever the local cache has been invalidated;
     * pass the previously stored digest to opt into the relay's cache-match short-circuit.
     *
     * @param itemDhash the cached blocklist digest, or {@code null} to request a full list
     */
    public SmaxGetBlockListRequest(String itemDhash) {
        this.itemDhash = itemDhash;
    }

    /**
     * Returns the cached digest when set.
     *
     * <p>A present value means the request was issued in cache-match mode; an empty value means a full re-fetch
     * was requested.
     *
     * @return an {@link Optional} carrying the digest, or empty when no cached digest was supplied
     */
    public Optional<String> itemDhash() {
        return Optional.ofNullable(itemDhash);
    }

    /**
     * Builds the outbound {@code <iq>} stanza ready for dispatch.
     *
     * <p>The returned {@link StanzaBuilder} addresses {@code s.whatsapp.net} with {@code xmlns="blocklist"} and
     * {@code type="get"}; the {@code id} attribute is filled in downstream by the central client dispatcher.
     * When {@link #itemDhash()} is present, the builder also attaches a {@code <item dhash="..."/>} child so the
     * relay can compare against the cached digest.
     *
     * @implNote This implementation flattens the separate WA Web request and request-item factories into a single
     * method, since Cobalt centralises id generation on the client and the inner item factory has no other call
     * sites.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsGetBlockListRequest",
            exports = "makeGetBlockListRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsGetBlockListRequest",
            exports = "makeGetBlockListRequestItem", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var iqBuilder = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "blocklist")
                .attribute("to", JidServer.user())
                .attribute("type", "get");
        if (itemDhash != null) {
            var itemNode = new StanzaBuilder()
                    .description("item")
                    .attribute("dhash", itemDhash)
                    .build();
            iqBuilder.content(itemNode);
        }
        return iqBuilder;
    }

    /**
     * Compares this request with another for equality by cached digest.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxGetBlockListRequest} with an equal digest
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGetBlockListRequest) obj;
        return Objects.equals(this.itemDhash, that.itemDhash);
    }

    /**
     * Returns a hash code derived from the cached digest.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(itemDhash);
    }

    /**
     * Returns a debug representation carrying the cached digest.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxGetBlockListRequest[itemDhash=" + itemDhash + ']';
    }
}

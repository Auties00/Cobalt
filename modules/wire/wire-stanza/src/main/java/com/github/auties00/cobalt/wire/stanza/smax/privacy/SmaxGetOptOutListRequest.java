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
 * Builds the outbound {@code <iq xmlns="optoutlist" type="get">} stanza that fetches the marketing-message opt-out list.
 *
 * <p>This stanza backs the marketing-messages opt-out surface. The caller may seed the request with the digest of
 * the locally cached list to enable the relay's cache-match short-circuit, and may scope the result to a single
 * marketing category. The reply is parsed by {@link SmaxGetOptOutListResponse}.
 *
 * @implNote This implementation flattens the separate WA Web request and request-item factories into a single
 * method; both attributes are optional and emitted only when the corresponding field is set.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBlocklistsGetOptOutListRequest")
public final class SmaxGetOptOutListRequest implements SmaxStanza.Request {
    /**
     * The cached digest of the opt-out list, or {@code null} to request a full list.
     *
     * <p>Pass {@code null} on first boot or after a cache wipe.
     */
    private final String itemDhash;

    /**
     * The marketing-message category scoping the query, or {@code null} for an unscoped fetch.
     *
     * <p>One of the user-controls category constants; consumers typically pass the active category surfaced by
     * the UI.
     */
    private final String iqCategory;

    /**
     * Constructs an opt-out-list request.
     *
     * <p>Pass {@code null} for either argument to omit the corresponding wire attribute and fall back to the
     * relay's defaults of full list and all categories.
     *
     * @param itemDhash  the cached digest; may be {@code null}
     * @param iqCategory the category filter; may be {@code null}
     */
    public SmaxGetOptOutListRequest(String itemDhash, String iqCategory) {
        this.itemDhash = itemDhash;
        this.iqCategory = iqCategory;
    }

    /**
     * Returns the cached digest when set.
     *
     * @return an {@link Optional} carrying the digest, or empty when no cached digest was supplied
     */
    public Optional<String> itemDhash() {
        return Optional.ofNullable(itemDhash);
    }

    /**
     * Returns the category filter when set.
     *
     * @return an {@link Optional} carrying the category, or empty when no category was supplied
     */
    public Optional<String> iqCategory() {
        return Optional.ofNullable(iqCategory);
    }

    /**
     * Builds the outbound {@code <iq>} stanza ready for dispatch.
     *
     * <p>The returned {@link StanzaBuilder} addresses {@code s.whatsapp.net} with {@code xmlns="optoutlist"} and
     * {@code type="get"}; the {@code id} attribute is filled in downstream by the central client dispatcher. The
     * {@code category} attribute is added only when {@link #iqCategory()} is present; the
     * {@code <item dhash="..."/>} child is added only when {@link #itemDhash()} is present.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsGetOptOutListRequest",
            exports = "makeGetOptOutListRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsGetOptOutListRequest",
            exports = "makeGetOptOutListRequestItem", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var iqBuilder = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "optoutlist")
                .attribute("to", JidServer.user())
                .attribute("type", "get");
        if (iqCategory != null) {
            iqBuilder.attribute("category", iqCategory);
        }
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
     * Compares this request with another for equality by cached digest and category filter.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal {@link SmaxGetOptOutListRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGetOptOutListRequest) obj;
        return Objects.equals(this.itemDhash, that.itemDhash)
                && Objects.equals(this.iqCategory, that.iqCategory);
    }

    /**
     * Returns a hash code derived from the cached digest and category filter.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(itemDhash, iqCategory);
    }

    /**
     * Returns a debug representation carrying the cached digest and category filter.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxGetOptOutListRequest[itemDhash=" + itemDhash
                + ", iqCategory=" + iqCategory + ']';
    }
}

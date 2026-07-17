package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Objects;

/**
 * Models the typed outbound {@code <iq xmlns="fb:thrift_iq" type="get">} stanza that runs a {@code profile_typeahead} {@code catkit} business-category lookup.
 *
 * <p>The SMB profile-editor category picker uses this request to look up category suggestions as
 * the merchant types. The matching {@link IqQueryBusinessCategoriesResponse} returns the typed
 * category list plus a synthetic {@code not_a_biz} sentinel that the picker uses to model the "this
 * is not a business" opt-out row. An empty query requests the unfiltered category list.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryBusinessCategoriesJob")
public final class IqQueryBusinessCategoriesRequest implements IqStanza.Request {
    /**
     * Holds the free-text typeahead query routed verbatim into the {@code <query/>} child content.
     *
     * <p>An empty string requests the unfiltered category list.
     */
    private final String query;

    /**
     * Constructs a typed request over the given typeahead string.
     *
     * <p>An empty string requests the unfiltered category list.
     *
     * @param query the typeahead query; never {@code null}
     * @throws NullPointerException if {@code query} is {@code null}
     */
    public IqQueryBusinessCategoriesRequest(String query) {
        this.query = Objects.requireNonNull(query, "query cannot be null");
    }

    /**
     * Returns the typeahead query the stanza carries.
     *
     * @return the query; never {@code null}
     */
    public String query() {
        return query;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation materialises the WAP envelope produced by {@code WAWebQueryBusinessCategoriesJob.queryBusinessCategories}:
     * a {@code <request op="profile_typeahead" type="catkit" v="1"/>} wrapper carrying a
     * {@code <query/>} child with the verbatim typeahead string.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQueryBusinessCategoriesJob",
            exports = "queryBusinessCategories", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var queryNode = new StanzaBuilder()
                .description("query")
                .content(query)
                .build();
        var requestNode = new StanzaBuilder()
                .description("request")
                .attribute("op", "profile_typeahead")
                .attribute("type", "catkit")
                .attribute("v", "1")
                .content(queryNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "fb:thrift_iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(requestNode);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqQueryBusinessCategoriesRequest) obj;
        return Objects.equals(this.query, that.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query);
    }

    @Override
    public String toString() {
        return "IqQueryBusinessCategoriesRequest[query=" + query + ']';
    }
}

package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;

/**
 * Builds the {@code <iq xmlns="fb:thrift_iq" type="set">} stanza that toggles the cart-enabled flag
 * in the current merchant's commerce settings.
 *
 * <p>The stanza is sent from the catalog-management commerce-settings surface; flipping the flag
 * controls whether the merchant's catalog grid shows the "add to cart" affordance, and the relay
 * echoes the post-mutation value back inside the response.
 *
 * @implNote
 * This implementation models the legacy WAP-IQ path only; WA Web routes the same call through the
 * Relay GraphQL endpoint when the {@code graphQLForCommerceSettingsEnabled} gating flag is on,
 * falling back to the WAP-IQ payload on graphql-error and recovery-required paths, but Cobalt keeps
 * the WAP-IQ payload as the single transport.
 */
@WhatsAppWebModule(moduleName = "WAWebBusinessProfileJob")
public final class IqUpdateCartEnabledRequest implements IqStanza.Request {
    /**
     * Holds the desired cart-enabled flag stamped into the {@code enabled} attribute of the
     * {@code <cart/>} grandchild.
     */
    private final boolean cartEnabled;

    /**
     * Constructs a request from the desired cart-enabled state.
     *
     * <p>Pass {@code true} to enable the cart affordance on the catalog grid, {@code false} to
     * disable it.
     *
     * @param cartEnabled the desired state
     */
    public IqUpdateCartEnabledRequest(boolean cartEnabled) {
        this.cartEnabled = cartEnabled;
    }

    /**
     * Returns the desired cart-enabled flag the stanza stamps.
     *
     * <p>The relay routes the value verbatim into the {@code enabled} attribute of the resulting
     * {@code <cart/>} grandchild.
     *
     * @return the flag
     */
    public boolean cartEnabled() {
        return cartEnabled;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation materialises a {@code <cart enabled/>} grandchild wrapped in a
     * {@code <commerce_settings/>} envelope and an {@code fb:thrift_iq set} IQ frame routed to the
     * WhatsApp service.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBusinessProfileJob",
            exports = "updateCartEnabled", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var cartNode = new StanzaBuilder()
                .description("cart")
                .attribute("enabled", String.valueOf(cartEnabled))
                .build();
        var commerceSettingsNode = new StanzaBuilder()
                .description("commerce_settings")
                .content(cartNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "fb:thrift_iq")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(commerceSettingsNode);
    }

    /**
     * Compares this request with another for value equality on the cart-enabled flag.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal request
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqUpdateCartEnabledRequest) obj;
        return this.cartEnabled == that.cartEnabled;
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Boolean.hashCode(cartEnabled);
    }

    /**
     * Returns a diagnostic string naming the cart-enabled flag.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "IqUpdateCartEnabledRequest[cartEnabled=" + cartEnabled + ']';
    }
}

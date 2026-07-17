package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The typed outbound {@code <iq xmlns="fb:thrift_iq" type="get">} stanza that refreshes a buyer-side cart against a
 * merchant catalog.
 *
 * <p>The request revalidates a cart against the merchant's live catalog before checkout; the matching
 * {@link IqBizRefreshCartResponse} echoes the canonical price for each line plus the cart-wide totals, flags entries
 * that have changed price, gone out of stock or been removed, and surfaces the per-line cap the buyer must respect.
 *
 * @implNote
 * This implementation targets the deprecated WAP path; the WA Web job's modern entry routes through a GraphQL
 * refresh-cart job. Cobalt ships only the WAP envelope shape carried by {@code op="refresh"} cart stanzas.
 */
@WhatsAppWebModule(moduleName = "WAWebBizRefreshCartJob")
public final class IqBizRefreshCartRequest implements IqStanza.Request {
    /**
     * The merchant catalog JID that the cart is being refreshed against, emitted as the {@code biz_jid} attribute of
     * the {@code <cart/>} envelope.
     */
    private final Jid businessJid;

    /**
     * The cart line product identifiers, emitted as {@code <product><id/></product>} children of the {@code <cart/>}
     * envelope.
     */
    private final List<String> productIds;

    /**
     * The requested thumbnail width in pixels emitted as the {@code <width/>} child content of
     * {@code <image_dimensions/>}.
     */
    private final int width;

    /**
     * The requested thumbnail height in pixels emitted as the {@code <height/>} child content of
     * {@code <image_dimensions/>}.
     */
    private final int height;

    /**
     * The optional direct-connection encrypted info blob emitted as the {@code <direct_connection_encrypted_info/>}
     * child content.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * Constructs a typed request from a merchant target and the cart line identifiers.
     *
     * <p>The list must contain at least one entry because the relay rejects empty cart refreshes. Pass {@code null} for
     * {@code directConnectionEncryptedInfo} when the direct-merchant routing path is not in use.
     *
     * @param businessJid                   the merchant catalog {@link Jid}; never {@code null}
     * @param productIds                    the cart line identifiers; never {@code null} and must be non-empty
     * @param width                         the requested thumbnail width in pixels
     * @param height                        the requested thumbnail height in pixels
     * @param directConnectionEncryptedInfo the direct-connection routing blob; may be {@code null}
     * @throws NullPointerException     if {@code businessJid} or {@code productIds} is {@code null}
     * @throws IllegalArgumentException when {@code productIds} is empty
     */
    public IqBizRefreshCartRequest(Jid businessJid,
                   List<String> productIds,
                   int width,
                   int height,
                   String directConnectionEncryptedInfo) {
        this.businessJid = Objects.requireNonNull(businessJid, "businessJid cannot be null");
        Objects.requireNonNull(productIds, "productIds cannot be null");
        if (productIds.isEmpty()) {
            throw new IllegalArgumentException("productIds cannot be empty");
        }
        this.productIds = List.copyOf(productIds);
        this.width = width;
        this.height = height;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
    }

    /**
     * Returns the merchant catalog {@link Jid} that the cart refresh names.
     *
     * @return the {@link Jid}; never {@code null}
     */
    public Jid businessJid() {
        return businessJid;
    }

    /**
     * Returns the cart line product identifiers that the cart names.
     *
     * <p>The list is non-empty and ordered as supplied to the constructor.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<String> productIds() {
        return productIds;
    }

    /**
     * Returns the requested thumbnail width that the relay uses when sizing per-line thumbnails in the success reply.
     *
     * @return the width in pixels
     */
    public int width() {
        return width;
    }

    /**
     * Returns the requested thumbnail height that the relay uses when sizing per-line thumbnails in the success reply.
     *
     * @return the height in pixels
     */
    public int height() {
        return height;
    }

    /**
     * Returns the optional direct-connection routing blob.
     *
     * <p>The blob lets the relay forward the refresh directly to the merchant when direct-connection routing is enabled;
     * the value is absent when classical relay routing applies.
     *
     * @return an {@link Optional} carrying the blob
     */
    public Optional<String> directConnectionEncryptedInfo() {
        return Optional.ofNullable(directConnectionEncryptedInfo);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits a {@code <cart op="refresh" biz_jid/>} wrapper carrying the
     * {@code <product><id/></product>} entries, the {@code <image_dimensions>} pair and, when supplied, the
     * {@code <direct_connection_encrypted_info/>} child.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBizRefreshCartJob",
            exports = "refreshCart", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var children = new ArrayList<Stanza>();
        for (var id : productIds) {
            var idNode = new StanzaBuilder()
                    .description("id")
                    .content(id)
                    .build();
            children.add(new StanzaBuilder()
                    .description("product")
                    .content(idNode)
                    .build());
        }
        var widthNode = new StanzaBuilder()
                .description("width")
                .content(String.valueOf(width))
                .build();
        var heightNode = new StanzaBuilder()
                .description("height")
                .content(String.valueOf(height))
                .build();
        children.add(new StanzaBuilder()
                .description("image_dimensions")
                .content(List.of(widthNode, heightNode))
                .build());
        if (directConnectionEncryptedInfo != null) {
            children.add(new StanzaBuilder()
                    .description("direct_connection_encrypted_info")
                    .content(directConnectionEncryptedInfo)
                    .build());
        }
        var cartNode = new StanzaBuilder()
                .description("cart")
                .attribute("op", "refresh")
                .attribute("biz_jid", businessJid)
                .content(children)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "fb:thrift_iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(cartNode);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqBizRefreshCartRequest) obj;
        return this.width == that.width
                && this.height == that.height
                && Objects.equals(this.businessJid, that.businessJid)
                && Objects.equals(this.productIds, that.productIds)
                && Objects.equals(this.directConnectionEncryptedInfo, that.directConnectionEncryptedInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessJid, productIds, width, height, directConnectionEncryptedInfo);
    }

    @Override
    public String toString() {
        return "IqBizRefreshCartRequest[businessJid=" + businessJid
                + ", productIds=" + productIds + ']';
    }
}

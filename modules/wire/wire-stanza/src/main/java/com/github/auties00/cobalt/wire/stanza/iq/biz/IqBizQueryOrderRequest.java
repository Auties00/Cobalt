package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The typed outbound {@code <iq xmlns="fb:thrift_iq" type="get">} stanza that fetches the typed detail of a single
 * business order.
 *
 * <p>The request materialises the order-details surface for a buyer who taps a merchant order receipt; the matching
 * {@link IqBizQueryOrderResponse} carries the line items, subtotal, tax and total, alongside thumbnails sized to the
 * requested {@code width} and {@code height}. The {@code token} is the merchant-supplied authentication token attached
 * to the order; the optional direct-connection blob lets the relay route the query directly to the merchant when that
 * path is enabled.
 *
 * @implNote
 * This implementation targets the deprecated WAP path; the WA Web job routes through a GraphQL query first when the
 * GraphQL-for-order-info flag is set and only falls back to this stanza shape when the GraphQL path is disabled. Cobalt
 * ships only the wire-level WAP envelope.
 *
 * @deprecated superseded by the GraphQL order-details read ({@code LiveLinkedWhatsAppClient.queryOrder}
 * via {@code BizQueryOrderJobWhatsAppGraphQlRequest}); WA Web no longer emits the {@code fb:thrift_iq}
 * order shape.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WAWebBizQueryOrderJob")
public final class IqBizQueryOrderRequest implements IqStanza.Request {
    /**
     * The order identifier routed verbatim into the {@code <order id/>} attribute.
     */
    private final String orderId;

    /**
     * The requested thumbnail width in pixels, emitted as the {@code <width/>} child content.
     */
    private final int width;

    /**
     * The requested thumbnail height in pixels, emitted as the {@code <height/>} child content.
     */
    private final int height;

    /**
     * The merchant-issued order authentication token emitted as the {@code <token/>} child content.
     */
    private final String token;

    /**
     * The optional direct-connection encrypted info blob emitted as the {@code <direct_connection_encrypted_info/>}
     * child content.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * Constructs a typed request from an order id, a thumbnail size and a merchant token.
     *
     * <p>The order id and the token are mandatory because the relay rejects stanzas without an order target and without
     * an authentication token. Pass {@code null} for {@code directConnectionEncryptedInfo} when the direct-merchant
     * routing path is not in use.
     *
     * @param orderId                       the order identifier; never {@code null}
     * @param width                         the requested thumbnail width in pixels
     * @param height                        the requested thumbnail height in pixels
     * @param token                         the merchant-issued authentication token; never {@code null}
     * @param directConnectionEncryptedInfo the direct-connection routing blob; may be {@code null}
     * @throws NullPointerException if {@code orderId} or {@code token} is {@code null}
     */
    public IqBizQueryOrderRequest(String orderId, int width, int height, String token,
                   String directConnectionEncryptedInfo) {
        this.orderId = Objects.requireNonNull(orderId, "orderId cannot be null");
        this.width = width;
        this.height = height;
        this.token = Objects.requireNonNull(token, "token cannot be null");
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
    }

    /**
     * Returns the order identifier that the stanza names.
     *
     * <p>The value is the same opaque id returned by the merchant order pipeline and consumed by
     * {@link IqBizQueryOrderResponse.Success}.
     *
     * @return the order identifier; never {@code null}
     */
    public String orderId() {
        return orderId;
    }

    /**
     * Returns the requested thumbnail width that the relay uses when sizing product thumbnails in the success reply.
     *
     * @return the width in pixels
     */
    public int width() {
        return width;
    }

    /**
     * Returns the requested thumbnail height that the relay uses when sizing product thumbnails in the success reply.
     *
     * @return the height in pixels
     */
    public int height() {
        return height;
    }

    /**
     * Returns the merchant-issued authentication token that the relay verifies before serving the order detail.
     *
     * <p>The value is opaque and unique to the merchant-order pair.
     *
     * @return the token; never {@code null}
     */
    public String token() {
        return token;
    }

    /**
     * Returns the optional direct-connection routing blob.
     *
     * <p>The blob lets the relay forward the query directly to the merchant when direct-connection routing is enabled;
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
     * This implementation emits an {@code <order op="get" id/>} wrapper carrying the {@code <image_dimensions>} pair,
     * the {@code <token/>} child and, when supplied, the {@code <direct_connection_encrypted_info/>} child.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBizQueryOrderJob",
            exports = "queryOrder", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var widthNode = new StanzaBuilder()
                .description("width")
                .content(String.valueOf(width))
                .build();
        var heightNode = new StanzaBuilder()
                .description("height")
                .content(String.valueOf(height))
                .build();
        var imageDimensionsNode = new StanzaBuilder()
                .description("image_dimensions")
                .content(List.of(widthNode, heightNode))
                .build();
        var tokenNode = new StanzaBuilder()
                .description("token")
                .content(token)
                .build();
        var orderChildren = new ArrayList<Stanza>();
        orderChildren.add(imageDimensionsNode);
        orderChildren.add(tokenNode);
        if (directConnectionEncryptedInfo != null) {
            orderChildren.add(new StanzaBuilder()
                    .description("direct_connection_encrypted_info")
                    .content(directConnectionEncryptedInfo)
                    .build());
        }
        var orderNode = new StanzaBuilder()
                .description("order")
                .attribute("op", "get")
                .attribute("id", orderId)
                .content(orderChildren)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "fb:thrift_iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(orderNode);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqBizQueryOrderRequest) obj;
        return this.width == that.width
                && this.height == that.height
                && Objects.equals(this.orderId, that.orderId)
                && Objects.equals(this.token, that.token)
                && Objects.equals(this.directConnectionEncryptedInfo, that.directConnectionEncryptedInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, width, height, token, directConnectionEncryptedInfo);
    }

    @Override
    public String toString() {
        return "IqBizQueryOrderRequest[orderId=" + orderId + ", width=" + width
                + ", height=" + height + ']';
    }
}

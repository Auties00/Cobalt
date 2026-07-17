package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The typed sealed family of inbound reply variants produced by the relay in response to an
 * {@link IqBizRefreshCartRequest}.
 *
 * <p>The family covers the three documented outcomes of a cart refresh: {@link Success} carries the per-line price,
 * the cart-wide totals and the cap available, {@link ClientError} surfaces a relay validation rejection, and
 * {@link ServerError} reports a transport or backend failure. {@link #of(Stanza, Stanza)} projects the raw {@link Stanza}
 * into the right variant before the caller switches over it.
 */
@WhatsAppWebModule(moduleName = "WAWebBizRefreshCartJob")
public sealed interface IqBizRefreshCartResponse extends IqStanza.Response
        permits IqBizRefreshCartResponse.Success, IqBizRefreshCartResponse.ClientError, IqBizRefreshCartResponse.ServerError {

    /**
     * Tries each {@link IqBizRefreshCartResponse} variant in priority order and returns the first match.
     *
     * <p>The success path is tried first, then the client-error envelope, then the server-error envelope. The result
     * is empty only when none of the three documented shapes apply.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant
     * @throws NullPointerException if either argument is {@code null}
     */
    static Optional<? extends IqBizRefreshCartResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * The {@code Success} reply variant carrying the typed cart envelope.
     *
     * <p>This variant carries the cart-wide totals block and the per-line {@link Product} entries from which the
     * cart-revalidate surface is rendered before the buyer advances to checkout.
     */
    final class Success implements IqBizRefreshCartResponse {
        /**
         * The cart totals block carrying the pre-tax subtotal, the grand total, the currency code and the status marker.
         *
         * <p>The block models the {@code <price/>} child of the {@code <cart/>} envelope, rendered as the totals row of
         * the cart-revalidate surface.
         */
        public static final class Price {
            /**
             * The pre-tax subtotal string lifted from {@code <price><subtotal/></price>}, when supplied.
             */
            private final String subtotal;

            /**
             * The grand total string lifted from {@code <price><total/></price>}, when supplied.
             */
            private final String total;

            /**
             * The currency code (ISO 4217) lifted from {@code <price><currency/></price>}, when supplied.
             */
            private final String currency;

            /**
             * The price-status marker lifted from {@code <price><price_status/></price>}, when supplied.
             */
            private final String priceStatus;

            /**
             * Constructs a typed price block by projecting the {@code <price/>} child of a {@code <cart/>} envelope.
             *
             * <p>Pass {@code null} for any field that the wire shape omitted.
             *
             * @param subtotal    the pre-tax subtotal string; may be {@code null}
             * @param total       the grand total string; may be {@code null}
             * @param currency    the currency code; may be {@code null}
             * @param priceStatus the status marker; may be {@code null}
             */
            public Price(String subtotal, String total, String currency, String priceStatus) {
                this.subtotal = subtotal;
                this.total = total;
                this.currency = currency;
                this.priceStatus = priceStatus;
            }

            /**
             * Returns the pre-tax subtotal string for the subtotal row of the cart totals block.
             *
             * @return an {@link Optional} carrying the subtotal
             */
            public Optional<String> subtotal() {
                return Optional.ofNullable(subtotal);
            }

            /**
             * Returns the grand total string for the headline total of the cart-revalidate surface.
             *
             * @return an {@link Optional} carrying the total
             */
            public Optional<String> total() {
                return Optional.ofNullable(total);
            }

            /**
             * Returns the currency code (ISO 4217) that frames the subtotal and total strings.
             *
             * @return an {@link Optional} carrying the code
             */
            public Optional<String> currency() {
                return Optional.ofNullable(currency);
            }

            /**
             * Returns the per-cart price-status marker that the relay returns alongside the totals.
             *
             * <p>The value is typically {@code "OK"} when the cart resolves cleanly.
             *
             * @return an {@link Optional} carrying the marker
             */
            public Optional<String> priceStatus() {
                return Optional.ofNullable(priceStatus);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Price) obj;
                return Objects.equals(this.subtotal, that.subtotal)
                        && Objects.equals(this.total, that.total)
                        && Objects.equals(this.currency, that.currency)
                        && Objects.equals(this.priceStatus, that.priceStatus);
            }

            @Override
            public int hashCode() {
                return Objects.hash(subtotal, total, currency, priceStatus);
            }

            @Override
            public String toString() {
                return "IqBizRefreshCartResponse.Success.Price[subtotal=" + subtotal
                        + ", total=" + total + ", currency=" + currency
                        + ", priceStatus=" + priceStatus + ']';
            }
        }

        /**
         * One typed cart line carrying the product identity, the canonical price, the cart cap, the optional thumbnail,
         * the optional sale-price block and the per-line status marker.
         *
         * <p>The status marker lets the UI flag entries that have been removed or are no longer purchasable.
         */
        public static final class Product {
            /**
             * The product identifier within the merchant catalog.
             */
            private final String id;

            /**
             * The product display name, when supplied.
             */
            private final String name;

            /**
             * The unit price string, when supplied.
             */
            private final String price;

            /**
             * The line currency code (ISO 4217), when supplied.
             */
            private final String currency;

            /**
             * The maximum cart quantity that the relay allows for this line.
             */
            private final int maxAvailable;

            /**
             * The catalog thumbnail identifier, when supplied.
             */
            private final String thumbnailId;

            /**
             * The catalog thumbnail URL, when supplied.
             */
            private final String thumbnailUrl;

            /**
             * The optional sale-price block carrying a discounted price and an optional sale window.
             */
            private final SalePrice salePrice;

            /**
             * The per-line status marker (for example {@code "INVALID_PRODUCT"}, {@code "MISSING"}), when supplied.
             */
            private final String status;

            /**
             * Constructs a typed cart line by projecting a {@code <product/>} child of the {@code <cart/>} envelope.
             *
             * <p>Pass {@code null} for any optional field that the wire shape omitted. The {@code maxAvailable} cap
             * defaults to {@code 99} when the relay does not stamp a per-line limit.
             *
             * @param id           the product identifier; never {@code null}
             * @param name         the display name; may be {@code null}
             * @param price        the unit price string; may be {@code null}
             * @param currency     the currency code; may be {@code null}
             * @param maxAvailable the cart cap
             * @param thumbnailId  the thumbnail identifier; may be {@code null}
             * @param thumbnailUrl the thumbnail URL; may be {@code null}
             * @param salePrice    the sale-price block; may be {@code null}
             * @param status       the status marker; may be {@code null}
             * @throws NullPointerException if {@code id} is {@code null}
             */
            public Product(String id,
                           String name,
                           String price,
                           String currency,
                           int maxAvailable,
                           String thumbnailId,
                           String thumbnailUrl,
                           SalePrice salePrice,
                           String status) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.name = name;
                this.price = price;
                this.currency = currency;
                this.maxAvailable = maxAvailable;
                this.thumbnailId = thumbnailId;
                this.thumbnailUrl = thumbnailUrl;
                this.salePrice = salePrice;
                this.status = status;
            }

            /**
             * Returns the merchant-catalog identifier of the product the cart line refers to.
             *
             * @return the identifier; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the cart-line display name.
             *
             * <p>The value is absent when the relay omitted the {@code <name/>} child.
             *
             * @return an {@link Optional} carrying the name
             */
            public Optional<String> name() {
                return Optional.ofNullable(name);
            }

            /**
             * Returns the unit price string for the per-line price column.
             *
             * <p>The value is absent when the relay did not stamp a per-line price.
             *
             * @return an {@link Optional} carrying the price
             */
            public Optional<String> price() {
                return Optional.ofNullable(price);
            }

            /**
             * Returns the line currency code (ISO 4217) when it diverges from the cart-wide currency.
             *
             * <p>The value is absent when the relay omitted the {@code <currency/>} child.
             *
             * @return an {@link Optional} carrying the code
             */
            public Optional<String> currency() {
                return Optional.ofNullable(currency);
            }

            /**
             * Returns the maximum cart quantity that the relay allows for this line.
             *
             * <p>The cap clamps the buyer's quantity selector before the cart is re-submitted.
             *
             * @return the cap
             */
            public int maxAvailable() {
                return maxAvailable;
            }

            /**
             * Returns the catalog thumbnail identifier for downloading the image bytes separately.
             *
             * <p>The value is absent when the cart line references a product without a thumbnail.
             *
             * @return an {@link Optional} carrying the identifier
             */
            public Optional<String> thumbnailId() {
                return Optional.ofNullable(thumbnailId);
            }

            /**
             * Returns the catalog thumbnail URL for loading the image directly over HTTP.
             *
             * @return an {@link Optional} carrying the URL
             */
            public Optional<String> thumbnailUrl() {
                return Optional.ofNullable(thumbnailUrl);
            }

            /**
             * Returns the sale-price block carrying the discounted price and optional sale window.
             *
             * <p>The value is present only when the merchant has a promo running on this line.
             *
             * @return an {@link Optional} carrying the block
             */
            public Optional<SalePrice> salePrice() {
                return Optional.ofNullable(salePrice);
            }

            /**
             * Returns the per-line status marker flagging a cart line as no longer purchasable.
             *
             * <p>Typical values are {@code "INVALID_PRODUCT"} or {@code "MISSING"}.
             *
             * @return an {@link Optional} carrying the marker
             */
            public Optional<String> status() {
                return Optional.ofNullable(status);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Product) obj;
                return this.maxAvailable == that.maxAvailable
                        && Objects.equals(this.id, that.id)
                        && Objects.equals(this.name, that.name)
                        && Objects.equals(this.price, that.price)
                        && Objects.equals(this.currency, that.currency)
                        && Objects.equals(this.thumbnailId, that.thumbnailId)
                        && Objects.equals(this.thumbnailUrl, that.thumbnailUrl)
                        && Objects.equals(this.salePrice, that.salePrice)
                        && Objects.equals(this.status, that.status);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, name, price, currency, maxAvailable,
                        thumbnailId, thumbnailUrl, salePrice, status);
            }

            @Override
            public String toString() {
                return "IqBizRefreshCartResponse.Success.Product[id=" + id
                        + ", name=" + name + ", price=" + price
                        + ", currency=" + currency + ", maxAvailable=" + maxAvailable + ']';
            }
        }

        /**
         * The sale-price sub-block carrying a discounted price and an optional sale window.
         *
         * <p>The block models the {@code <sale_price/>} child of a cart {@code <product/>}; the strike-through price and
         * a sale window are rendered when both endpoints are stamped.
         */
        public static final class SalePrice {
            /**
             * The discounted price string lifted from {@code <sale_price><price/></sale_price>}.
             */
            private final String price;

            /**
             * The optional sale start date string lifted from {@code <sale_price><start_date/></sale_price>}.
             */
            private final String startDate;

            /**
             * The optional sale end date string lifted from {@code <sale_price><end_date/></sale_price>}.
             */
            private final String endDate;

            /**
             * Constructs a typed sale-price block by projecting a {@code <sale_price/>} child.
             *
             * <p>Pass {@code null} for either endpoint when the wire shape omitted the sale window.
             *
             * @param price     the discounted price; never {@code null}
             * @param startDate the sale start date; may be {@code null}
             * @param endDate   the sale end date; may be {@code null}
             * @throws NullPointerException if {@code price} is {@code null}
             */
            public SalePrice(String price, String startDate, String endDate) {
                this.price = Objects.requireNonNull(price, "price cannot be null");
                this.startDate = startDate;
                this.endDate = endDate;
            }

            /**
             * Returns the discounted price string rendered alongside the strike-through {@link Product#price()}.
             *
             * @return the price; never {@code null}
             */
            public String price() {
                return price;
            }

            /**
             * Returns the sale window's start endpoint when the merchant stamped one on the line.
             *
             * @return an {@link Optional} carrying the date
             */
            public Optional<String> startDate() {
                return Optional.ofNullable(startDate);
            }

            /**
             * Returns the sale window's end endpoint when the merchant stamped one on the line.
             *
             * @return an {@link Optional} carrying the date
             */
            public Optional<String> endDate() {
                return Optional.ofNullable(endDate);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (SalePrice) obj;
                return Objects.equals(this.price, that.price)
                        && Objects.equals(this.startDate, that.startDate)
                        && Objects.equals(this.endDate, that.endDate);
            }

            @Override
            public int hashCode() {
                return Objects.hash(price, startDate, endDate);
            }

            @Override
            public String toString() {
                return "IqBizRefreshCartResponse.Success.SalePrice[price=" + price
                        + ", startDate=" + startDate + ", endDate=" + endDate + ']';
            }
        }

        /**
         * The cart totals block carrying subtotal, total, currency and status marker.
         */
        private final Price price;

        /**
         * The cart lines in wire order.
         */
        private final List<Product> products;

        /**
         * Constructs a typed success reply by projecting a {@code <cart/>} child into the typed model.
         *
         * @param price    the totals block; never {@code null}
         * @param products the cart lines; never {@code null}
         * @throws NullPointerException if either argument is {@code null}
         */
        public Success(Price price, List<Product> products) {
            this.price = Objects.requireNonNull(price, "price cannot be null");
            Objects.requireNonNull(products, "products cannot be null");
            this.products = List.copyOf(products);
        }

        /**
         * Returns the cart totals block containing subtotal, total, currency and status marker.
         *
         * @return the block; never {@code null}
         */
        public Price price() {
            return price;
        }

        /**
         * Returns the typed cart lines in wire order.
         *
         * <p>The list is empty when the relay returned a cart envelope without any product rows.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<Product> products() {
            return products;
        }

        /**
         * Tries to parse a {@link Success} variant from the inbound stanza.
         *
         * <p>The result is empty when the stanza does not carry a {@code result} envelope matching the original request.
         *
         * @implNote
         * This implementation returns an empty success envelope when the {@code <cart/>} child is absent, reads the
         * totals from the nested {@code <price/>} block, and projects each {@code <product/>} child into a
         * {@link Product} carrying its optional {@code <media><image>} sub-block (mapping {@code request_image_url} to
         * {@link Product#thumbnailUrl()}) and {@code <sale_price/>} sub-block. The {@code max_available} cap defaults to
         * {@code 99} when the wire shape omits it.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebBizRefreshCartJob",
                exports = "refreshCartResponse", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var cartNode = stanza.getChild("cart").orElse(null);
            if (cartNode == null) {
                return Optional.of(new Success(new Price(null, null, null, null),
                        Collections.emptyList()));
            }
            String subtotal = null;
            String total = null;
            String currency = null;
            String priceStatus = null;
            var priceNode = cartNode.getChild("price").orElse(null);
            if (priceNode != null) {
                subtotal = priceNode.getChild("subtotal")
                        .flatMap(Stanza::toContentString).orElse(null);
                total = priceNode.getChild("total")
                        .flatMap(Stanza::toContentString).orElse(null);
                currency = priceNode.getChild("currency")
                        .flatMap(Stanza::toContentString).orElse(null);
                priceStatus = priceNode.getChild("price_status")
                        .flatMap(Stanza::toContentString).orElse(null);
            }
            var price = new Price(subtotal, total, currency, priceStatus);
            var products = new ArrayList<Product>();
            for (var productNode : cartNode.getChildren("product")) {
                var idNode = productNode.getChild("id").orElse(null);
                if (idNode == null) {
                    continue;
                }
                var id = idNode.toContentString().orElse(null);
                if (id == null) {
                    continue;
                }
                var name = productNode.getChild("name")
                        .flatMap(Stanza::toContentString).orElse(null);
                var pprice = productNode.getChild("price")
                        .flatMap(Stanza::toContentString).orElse(null);
                var pcurrency = productNode.getChild("currency")
                        .flatMap(Stanza::toContentString).orElse(null);
                var maxAvailable = 99;
                var maxAvailableContent = productNode.getChild("max_available")
                        .flatMap(Stanza::toContentString).orElse(null);
                if (maxAvailableContent != null) {
                    try {
                        maxAvailable = Integer.parseInt(maxAvailableContent);
                    } catch (NumberFormatException _) {
                    }
                }
                String thumbId = null;
                String thumbUrl = null;
                productNode.getChild("media").ifPresent(media ->
                        media.getChild("image").orElse(null));
                var imageNode = productNode.getChild("media")
                        .flatMap(media -> media.getChild("image")).orElse(null);
                if (imageNode != null) {
                    thumbId = imageNode.getChild("id")
                            .flatMap(Stanza::toContentString).orElse(null);
                    thumbUrl = imageNode.getChild("request_image_url")
                            .flatMap(Stanza::toContentString).orElse(null);
                }
                SalePrice salePrice = null;
                var salePriceNode = productNode.getChild("sale_price").orElse(null);
                if (salePriceNode != null) {
                    var sp = salePriceNode.getChild("price")
                            .flatMap(Stanza::toContentString).orElse("");
                    var startDate = salePriceNode.getChild("start_date")
                            .flatMap(Stanza::toContentString).orElse(null);
                    var endDate = salePriceNode.getChild("end_date")
                            .flatMap(Stanza::toContentString).orElse(null);
                    salePrice = new SalePrice(sp, startDate, endDate);
                }
                var status = productNode.getChild("status")
                        .flatMap(Stanza::toContentString).orElse(null);
                products.add(new Product(id, name, pprice, pcurrency, maxAvailable,
                        thumbId, thumbUrl, salePrice, status));
            }
            return Optional.of(new Success(price, products));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.price, that.price)
                    && Objects.equals(this.products, that.products);
        }

        @Override
        public int hashCode() {
            return Objects.hash(price, products);
        }

        @Override
        public String toString() {
            return "IqBizRefreshCartResponse.Success[price=" + price
                    + ", products=" + products + ']';
        }
    }

    /**
     * The {@code ClientError} reply variant surfacing a client-side rejection.
     *
     * <p>This variant reports a refused cart refresh; a typical example is a relay validation failure surfaced as a
     * SMAX error envelope.
     */
    final class ClientError implements IqBizRefreshCartResponse {
        /**
         * The numeric error code lifted from the SMAX error envelope.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text lifted from the SMAX error envelope.
         */
        private final String errorText;

        /**
         * Constructs a typed client-error reply from a parsed error envelope.
         *
         * <p>Pass {@code null} for {@code errorText} when the wire shape omitted the text field.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable error text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the SMAX error code that the relay used to classify the failure.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the relay-supplied error explanation when present.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the inbound stanza.
         *
         * <p>The result is empty when the stanza does not carry a client-error envelope matching the original request.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the client-error schema
         */
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        @Override
        public String toString() {
            return "IqBizRefreshCartResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The {@code ServerError} reply variant surfacing a server-side failure.
     *
     * <p>This variant reports a backend failure that did not produce a typed cart; a typical example is a 500-class
     * status returned when the backend produced an unparseable shape.
     */
    final class ServerError implements IqBizRefreshCartResponse {
        /**
         * The numeric error code lifted from the SMAX error envelope.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text lifted from the SMAX error envelope.
         */
        private final String errorText;

        /**
         * Constructs a typed server-error reply from a parsed error envelope.
         *
         * <p>Pass {@code null} for {@code errorText} when the wire shape omitted the text field.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable error text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the SMAX error code that the relay used to classify the failure.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the relay-supplied error explanation when present.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the inbound stanza.
         *
         * <p>The result is empty when the stanza does not carry a server-error envelope matching the original request.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the server-error schema
         */
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        @Override
        public String toString() {
            return "IqBizRefreshCartResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

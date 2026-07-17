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
 * Models the inbound reply the relay produces in response to an {@link IqQueryProductListCatalogRequest}.
 *
 * <p>The sealed family is pattern-matched to drive the catalog grid: {@link Success} carries the
 * typed product entries (each a full body or a synthetic invalid marker), {@link ClientError}
 * surfaces a rejected request and {@link ServerError} surfaces a transient internal failure.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryProductListCatalogJob")
public sealed interface IqQueryProductListCatalogResponse extends IqStanza.Response
        permits IqQueryProductListCatalogResponse.Success, IqQueryProductListCatalogResponse.ClientError, IqQueryProductListCatalogResponse.ServerError {

    /**
     * Tries each variant in priority order until one matches.
     *
     * <p>The order is {@link Success}, then {@link ClientError}, then {@link ServerError}; call this
     * on every IQ stanza tagged with the {@code <product_list/>} payload.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant
     * @throws NullPointerException if either argument is {@code null}
     */
    static Optional<? extends IqQueryProductListCatalogResponse> of(Stanza stanza, Stanza request) {
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
     * Models the {@code Success} variant, which carries the typed product list.
     *
     * <p>Each {@link Product} entry returned by {@link #products()} is either a fully populated body
     * or the synthetic {@link Product#invalid(String) invalid marker} for ids the relay flagged as
     * no longer fetchable.
     */
    final class Success implements IqQueryProductListCatalogResponse {
        /**
         * Carries one typed product entry decoded from a {@code <product/>} child of the
         * {@code <product_list/>} payload.
         *
         * <p>An invalid entry leaves every field other than its id unset, so test {@link #invalid()}
         * before reading the remaining fields.
         */
        public static final class Product {
            /**
             * Holds the opaque product id.
             */
            private final String id;

            /**
             * Holds whether this entry is the synthetic {@code "INVALID_PRODUCT"} marker; when
             * {@code true} the remaining fields are unset and only {@code id} is meaningful.
             */
            private final boolean invalid;

            /**
             * Holds the optional product name.
             */
            private final String name;

            /**
             * Holds the optional description body.
             */
            private final String description;

            /**
             * Holds the optional canonical URL.
             */
            private final String url;

            /**
             * Holds the optional retailer-side identifier.
             */
            private final String retailerId;

            /**
             * Holds the optional availability marker.
             */
            private final String availability;

            /**
             * Holds the maximum cart quantity.
             */
            private final int maxAvailable;

            /**
             * Holds the optional currency code.
             */
            private final String currency;

            /**
             * Holds the optional price (string-encoded major-units integer).
             */
            private final String price;

            /**
             * Holds whether the product is hidden from the catalog grid.
             */
            private final boolean hidden;

            /**
             * Holds whether the product is sanctioned by Meta enforcement.
             */
            private final boolean sanctioned;

            /**
             * Holds whether the product is part of a featured set ({@code belongs_to=true}).
             */
            private final boolean checkmark;

            /**
             * Holds the optional whatsapp-side approval status (e.g. {@code "APPROVED"},
             * {@code "PENDING"}).
             */
            private final String whatsappStatus;

            /**
             * Holds whether the product can be appealed from a rejection.
             */
            private final boolean canAppeal;

            /**
             * Holds the decoded image entries, in wire order.
             */
            private final List<Image> images;

            /**
             * Holds the decoded video entries, in wire order.
             */
            private final List<Video> videos;

            /**
             * Holds the optional sale-price block.
             */
            private final SalePrice salePrice;

            /**
             * Holds the optional compliance-info block.
             */
            private final ComplianceInfo complianceInfo;

            /**
             * Holds the optional signed shimmed URL.
             */
            private final String signedShimmedUrl;

            /**
             * Holds the optional compliance category marker (e.g. {@code "Default"}).
             */
            private final String complianceCategory;

            /**
             * Constructs the synthetic invalid-product marker for the given id.
             *
             * <p>The relay echoes a {@code <product><status>INVALID_PRODUCT</status></product>} child
             * for ids it can no longer fetch; the resulting entry carries only the id and the invalid
             * flag, every other field is unset.
             *
             * @param id the product id; never {@code null}
             * @return an invalid product entry; never {@code null}
             * @throws NullPointerException if {@code id} is {@code null}
             */
            public static Product invalid(String id) {
                Objects.requireNonNull(id, "id cannot be null");
                return new Product(id, true, null, null, null, null, null, 0, null, null,
                        false, false, false, null, false,
                        Collections.emptyList(), Collections.emptyList(),
                        null, null, null, null);
            }

            /**
             * Constructs a fully populated product entry from the wire-decoded field set.
             *
             * <p>The field-by-field optional shape matches the wire echo so the catalog grid can
             * render each entry without further normalisation; called from the response parser.
             *
             * @param id                 see {@link #id()}
             * @param invalid            see {@link #invalid()}
             * @param name               see {@link #name()}
             * @param description        see {@link #description()}
             * @param url                see {@link #url()}
             * @param retailerId         see {@link #retailerId()}
             * @param availability       see {@link #availability()}
             * @param maxAvailable       see {@link #maxAvailable()}
             * @param currency           see {@link #currency()}
             * @param price              see {@link #price()}
             * @param hidden             see {@link #hidden()}
             * @param sanctioned         see {@link #sanctioned()}
             * @param checkmark          see {@link #checkmark()}
             * @param whatsappStatus     see {@link #whatsappStatus()}
             * @param canAppeal          see {@link #canAppeal()}
             * @param images             see {@link #images()}
             * @param videos             see {@link #videos()}
             * @param salePrice          see {@link #salePrice()}
             * @param complianceInfo     see {@link #complianceInfo()}
             * @param signedShimmedUrl   see {@link #signedShimmedUrl()}
             * @param complianceCategory see {@link #complianceCategory()}
             * @throws NullPointerException if {@code id}, {@code images}, or {@code videos} is
             *                              {@code null}
             */
            public Product(String id,
                           boolean invalid,
                           String name,
                           String description,
                           String url,
                           String retailerId,
                           String availability,
                           int maxAvailable,
                           String currency,
                           String price,
                           boolean hidden,
                           boolean sanctioned,
                           boolean checkmark,
                           String whatsappStatus,
                           boolean canAppeal,
                           List<Image> images,
                           List<Video> videos,
                           SalePrice salePrice,
                           ComplianceInfo complianceInfo,
                           String signedShimmedUrl,
                           String complianceCategory) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.invalid = invalid;
                this.name = name;
                this.description = description;
                this.url = url;
                this.retailerId = retailerId;
                this.availability = availability;
                this.maxAvailable = maxAvailable;
                this.currency = currency;
                this.price = price;
                this.hidden = hidden;
                this.sanctioned = sanctioned;
                this.checkmark = checkmark;
                this.whatsappStatus = whatsappStatus;
                this.canAppeal = canAppeal;
                Objects.requireNonNull(images, "images cannot be null");
                this.images = List.copyOf(images);
                Objects.requireNonNull(videos, "videos cannot be null");
                this.videos = List.copyOf(videos);
                this.salePrice = salePrice;
                this.complianceInfo = complianceInfo;
                this.signedShimmedUrl = signedShimmedUrl;
                this.complianceCategory = complianceCategory;
            }

            /**
             * Returns the opaque product id, used as the catalog-grid key.
             *
             * <p>The relay echoes the same id for both valid and invalid entries.
             *
             * @return the id; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the invalid-product flag that gates access to the remaining fields.
             *
             * <p>When {@code true} only {@link #id()} is meaningful and the catalog grid should
             * render a "product no longer available" placeholder.
             *
             * @return {@code true} when the relay marked this entry as invalid
             */
            public boolean invalid() {
                return invalid;
            }

            /**
             * Returns the product display name driving the catalog-card title.
             *
             * @return an {@link Optional} carrying the name
             */
            public Optional<String> name() {
                return Optional.ofNullable(name);
            }

            /**
             * Returns the description body driving the catalog-card body.
             *
             * @return an {@link Optional} carrying the description
             */
            public Optional<String> description() {
                return Optional.ofNullable(description);
            }

            /**
             * Returns the canonical product URL backing the "open in browser" affordance.
             *
             * @return an {@link Optional} carrying the URL
             */
            public Optional<String> url() {
                return Optional.ofNullable(url);
            }

            /**
             * Returns the retailer-side identifier that maps the entry back to the merchant's
             * external commerce platform.
             *
             * @return an {@link Optional} carrying the id
             */
            public Optional<String> retailerId() {
                return Optional.ofNullable(retailerId);
            }

            /**
             * Returns the availability marker driving the catalog-card stock badge (e.g.
             * {@code "in stock"}, {@code "out of stock"}).
             *
             * @return an {@link Optional} carrying the marker
             */
            public Optional<String> availability() {
                return Optional.ofNullable(availability);
            }

            /**
             * Returns the maximum cart quantity that caps the quantity selector.
             *
             * <p>The value defaults to 99 when the relay omits the field.
             *
             * @return the cap
             */
            public int maxAvailable() {
                return maxAvailable;
            }

            /**
             * Returns the ISO 4217 currency code, formatted with {@link #price()} into the
             * catalog-card price string.
             *
             * @return an {@link Optional} carrying the code
             */
            public Optional<String> currency() {
                return Optional.ofNullable(currency);
            }

            /**
             * Returns the price string driving the catalog-card price label.
             *
             * <p>The relay encodes the price as a major-units integer string (e.g. {@code "1999"} for
             * 19.99 in the {@link #currency()} unit).
             *
             * @return an {@link Optional} carrying the price string
             */
            public Optional<String> price() {
                return Optional.ofNullable(price);
            }

            /**
             * Returns the hidden flag.
             *
             * <p>Hidden entries are returned by id-list fetches but not by collection listings.
             *
             * @return {@code true} when hidden
             */
            public boolean hidden() {
                return hidden;
            }

            /**
             * Returns the sanctioned flag.
             *
             * <p>Sanctioned products are flagged by Meta enforcement, hidden from the catalog grid and
             * cannot be checked out.
             *
             * @return {@code true} when sanctioned
             */
            public boolean sanctioned() {
                return sanctioned;
            }

            /**
             * Returns the featured-set flag, decoded from the {@code <belongs_to/>} grandchild.
             *
             * <p>It drives the "featured" checkmark badge on the catalog grid.
             *
             * @return {@code true} when featured
             */
            public boolean checkmark() {
                return checkmark;
            }

            /**
             * Returns the whatsapp-side approval status driving the review-state badge (e.g.
             * {@code "APPROVED"}, {@code "PENDING"}, {@code "REJECTED"}).
             *
             * @return an {@link Optional} carrying the status
             */
            public Optional<String> whatsappStatus() {
                return Optional.ofNullable(whatsappStatus);
            }

            /**
             * Returns the can-appeal flag that enables the appeal CTA for rejected products.
             *
             * @return {@code true} when the product can be appealed
             */
            public boolean canAppeal() {
                return canAppeal;
            }

            /**
             * Returns the image list driving the catalog-card image carousel, in merchant-supplied
             * upload order.
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<Image> images() {
                return images;
            }

            /**
             * Returns the video list driving the catalog-card video carousel, in merchant-supplied
             * upload order.
             *
             * @return an unmodifiable list; never {@code null}
             */
            public List<Video> videos() {
                return videos;
            }

            /**
             * Returns the sale-price block driving the catalog-card "sale price" label.
             *
             * <p>An empty optional means the product is not on sale.
             *
             * @return an {@link Optional} carrying the block
             */
            public Optional<SalePrice> salePrice() {
                return Optional.ofNullable(salePrice);
            }

            /**
             * Returns the compliance-info block backing the regulatory disclosure under the catalog
             * card (country of origin, importer name and address).
             *
             * @return an {@link Optional} carrying the block
             */
            public Optional<ComplianceInfo> complianceInfo() {
                return Optional.ofNullable(complianceInfo);
            }

            /**
             * Returns the signed shimmed URL used when ad-shimlinking is enabled.
             *
             * <p>The URL is a signed proxy that surfaces ad-click telemetry before redirecting to
             * {@link #url()}.
             *
             * @return an {@link Optional} carrying the URL
             */
            public Optional<String> signedShimmedUrl() {
                return Optional.ofNullable(signedShimmedUrl);
            }

            /**
             * Returns the compliance-category marker (e.g. {@code "Default"},
             * {@code "BodyEnhancement"}) that gates which disclosure fields the catalog card displays.
             *
             * @return an {@link Optional} carrying the marker
             */
            public Optional<String> complianceCategory() {
                return Optional.ofNullable(complianceCategory);
            }

            /**
             * Compares this product with another for value equality across every decoded field.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is an equal product
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Product) obj;
                return this.invalid == that.invalid
                        && this.maxAvailable == that.maxAvailable
                        && this.hidden == that.hidden
                        && this.sanctioned == that.sanctioned
                        && this.checkmark == that.checkmark
                        && this.canAppeal == that.canAppeal
                        && Objects.equals(this.id, that.id)
                        && Objects.equals(this.name, that.name)
                        && Objects.equals(this.description, that.description)
                        && Objects.equals(this.url, that.url)
                        && Objects.equals(this.retailerId, that.retailerId)
                        && Objects.equals(this.availability, that.availability)
                        && Objects.equals(this.currency, that.currency)
                        && Objects.equals(this.price, that.price)
                        && Objects.equals(this.whatsappStatus, that.whatsappStatus)
                        && Objects.equals(this.images, that.images)
                        && Objects.equals(this.videos, that.videos)
                        && Objects.equals(this.salePrice, that.salePrice)
                        && Objects.equals(this.complianceInfo, that.complianceInfo)
                        && Objects.equals(this.signedShimmedUrl, that.signedShimmedUrl)
                        && Objects.equals(this.complianceCategory, that.complianceCategory);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                var h = Objects.hash(id, invalid, name, description, url, retailerId,
                        availability, maxAvailable, currency, price, hidden, sanctioned,
                        checkmark, whatsappStatus, canAppeal);
                return 31 * h + Objects.hash(images, videos, salePrice, complianceInfo,
                        signedShimmedUrl, complianceCategory);
            }

            /**
             * Returns a diagnostic string naming the id, invalid flag, name, price and currency.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "IqQueryProductListCatalogResponse.Success.Product[id=" + id
                        + ", invalid=" + invalid + ", name=" + name + ", price=" + price
                        + ", currency=" + currency + ']';
            }
        }

        /**
         * Carries one image entry on a {@link Product}: id, requested-resolution URL, and
         * original-resolution URL.
         *
         * <p>Both URLs are signed and short-lived; the requested-resolution URL backs the
         * catalog-card thumbnail and the full URL backs the product-detail surface.
         */
        public static final class Image {
            /**
             * Holds the image id.
             */
            private final String id;

            /**
             * Holds the requested-resolution URL (per-request resized).
             */
            private final String requestedUrl;

            /**
             * Holds the original-resolution URL.
             */
            private final String fullUrl;

            /**
             * Constructs an image from the {@code <media><image/>} grandchild fields; called from the
             * response parser.
             *
             * @param id           the id; never {@code null}
             * @param requestedUrl the requested URL; never {@code null}
             * @param fullUrl      the full URL; never {@code null}
             * @throws NullPointerException if any argument is {@code null}
             */
            public Image(String id, String requestedUrl, String fullUrl) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.requestedUrl = Objects.requireNonNull(requestedUrl, "requestedUrl cannot be null");
                this.fullUrl = Objects.requireNonNull(fullUrl, "fullUrl cannot be null");
            }

            /**
             * Returns the image id, a stable key for the catalog-card carousel.
             *
             * @return the id; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the requested-resolution URL backing the catalog-card thumbnail.
             *
             * <p>The URL is signed at the resolution the caller requested via
             * {@link IqQueryProductListCatalogRequest#width()} and
             * {@link IqQueryProductListCatalogRequest#height()}.
             *
             * @return the URL; never {@code null}
             */
            public String requestedUrl() {
                return requestedUrl;
            }

            /**
             * Returns the original-resolution URL backing the product-detail surface.
             *
             * @return the URL; never {@code null}
             */
            public String fullUrl() {
                return fullUrl;
            }

            /**
             * Compares this image with another for value equality on the id and both URLs.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is an equal image
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Image) obj;
                return Objects.equals(this.id, that.id)
                        && Objects.equals(this.requestedUrl, that.requestedUrl)
                        && Objects.equals(this.fullUrl, that.fullUrl);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(id, requestedUrl, fullUrl);
            }

            /**
             * Returns a diagnostic string naming the id and requested URL.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "IqQueryProductListCatalogResponse.Success.Image[id=" + id
                        + ", requestedUrl=" + requestedUrl + ']';
            }
        }

        /**
         * Carries one video entry on a {@link Product}: id, original-resolution URL, and poster-frame
         * thumbnail URL.
         *
         * <p>Both URLs are signed and short-lived; the original URL backs the in-line video player on
         * the product-detail surface and the thumbnail URL backs the carousel poster frame.
         */
        public static final class Video {
            /**
             * Holds the video id.
             */
            private final String id;

            /**
             * Holds the original-resolution video URL.
             */
            private final String originalVideoUrl;

            /**
             * Holds the thumbnail URL.
             */
            private final String thumbnailUrl;

            /**
             * Constructs a video from the {@code <media><video/>} grandchild fields; called from the
             * response parser.
             *
             * @param id               the id; never {@code null}
             * @param originalVideoUrl the video URL; never {@code null}
             * @param thumbnailUrl     the thumbnail URL; never {@code null}
             * @throws NullPointerException if any argument is {@code null}
             */
            public Video(String id, String originalVideoUrl, String thumbnailUrl) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.originalVideoUrl = Objects.requireNonNull(
                        originalVideoUrl, "originalVideoUrl cannot be null");
                this.thumbnailUrl = Objects.requireNonNull(thumbnailUrl, "thumbnailUrl cannot be null");
            }

            /**
             * Returns the video id, a stable key for the catalog-card carousel.
             *
             * @return the id; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the original-resolution video URL backing the in-line video player on the
             * product-detail surface.
             *
             * @return the URL; never {@code null}
             */
            public String originalVideoUrl() {
                return originalVideoUrl;
            }

            /**
             * Returns the poster-frame thumbnail URL shown in the carousel before playback starts.
             *
             * @return the URL; never {@code null}
             */
            public String thumbnailUrl() {
                return thumbnailUrl;
            }

            /**
             * Compares this video with another for value equality on the id and both URLs.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is an equal video
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Video) obj;
                return Objects.equals(this.id, that.id)
                        && Objects.equals(this.originalVideoUrl, that.originalVideoUrl)
                        && Objects.equals(this.thumbnailUrl, that.thumbnailUrl);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(id, originalVideoUrl, thumbnailUrl);
            }

            /**
             * Returns a diagnostic string naming the id and original video URL.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "IqQueryProductListCatalogResponse.Success.Video[id=" + id
                        + ", originalVideoUrl=" + originalVideoUrl + ']';
            }
        }

        /**
         * Carries one sale-price block on a {@link Product}: discounted price plus optional sale date
         * window.
         *
         * <p>The block backs the "sale price" label on the catalog card; the start and end dates are
         * optional and the card surfaces an indefinite sale when both are absent.
         */
        public static final class SalePrice {
            /**
             * Holds the sale-price string (same major-units encoding as {@link Product#price()}).
             */
            private final String price;

            /**
             * Holds the optional sale start date in {@code YYYY-MM-DD'T'HH:MM:SS}.
             */
            private final String startDate;

            /**
             * Holds the optional sale end date in {@code YYYY-MM-DD'T'HH:MM:SS}.
             */
            private final String endDate;

            /**
             * Constructs a block from the price and the independently-optional date window; called
             * from the response parser.
             *
             * @param price     the price; never {@code null}
             * @param startDate the start date; may be {@code null}
             * @param endDate   the end date; may be {@code null}
             * @throws NullPointerException if {@code price} is {@code null}
             */
            public SalePrice(String price, String startDate, String endDate) {
                this.price = Objects.requireNonNull(price, "price cannot be null");
                this.startDate = startDate;
                this.endDate = endDate;
            }

            /**
             * Returns the sale price driving the catalog-card "sale price" label.
             *
             * <p>The encoding matches {@link Product#price()}.
             *
             * @return the price; never {@code null}
             */
            public String price() {
                return price;
            }

            /**
             * Returns the sale start date.
             *
             * <p>An empty optional means the sale has no defined start.
             *
             * @return an {@link Optional} carrying the date
             */
            public Optional<String> startDate() {
                return Optional.ofNullable(startDate);
            }

            /**
             * Returns the sale end date.
             *
             * <p>An empty optional means the sale has no defined end.
             *
             * @return an {@link Optional} carrying the date
             */
            public Optional<String> endDate() {
                return Optional.ofNullable(endDate);
            }

            /**
             * Compares this block with another for value equality on the price and date window.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is an equal block
             */
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

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(price, startDate, endDate);
            }

            /**
             * Returns a diagnostic string naming the price and date window.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "IqQueryProductListCatalogResponse.Success.SalePrice[price=" + price
                        + ", startDate=" + startDate + ", endDate=" + endDate + ']';
            }
        }

        /**
         * Carries one compliance-info block on a {@link Product}: country of origin plus optional
         * importer details.
         *
         * <p>The block backs the regulatory disclosure rendered under the catalog card, required for
         * products imported into markets such as India where the importer name and address must be
         * visible to the buyer.
         */
        public static final class ComplianceInfo {
            /**
             * Holds the ISO 3166-1 alpha-2 country code of origin.
             */
            private final String countryCodeOrigin;

            /**
             * Holds the optional importer legal name.
             */
            private final String importerName;

            /**
             * Holds the optional importer address block.
             */
            private final ImporterAddress importerAddress;

            /**
             * Constructs a compliance-info block from the country code and the independently-optional
             * importer fields; called from the response parser.
             *
             * @param countryCodeOrigin the country code; never {@code null}
             * @param importerName      the importer name; may be {@code null}
             * @param importerAddress   the importer address; may be {@code null}
             * @throws NullPointerException if {@code countryCodeOrigin} is {@code null}
             */
            public ComplianceInfo(String countryCodeOrigin, String importerName,
                                  ImporterAddress importerAddress) {
                this.countryCodeOrigin = Objects.requireNonNull(
                        countryCodeOrigin, "countryCodeOrigin cannot be null");
                this.importerName = importerName;
                this.importerAddress = importerAddress;
            }

            /**
             * Returns the country of origin code rendered on the "Country of origin" disclosure line.
             *
             * @return the code; never {@code null}
             */
            public String countryCodeOrigin() {
                return countryCodeOrigin;
            }

            /**
             * Returns the importer legal name rendered on the "Importer" disclosure line.
             *
             * <p>An empty optional means the relay omitted the field.
             *
             * @return an {@link Optional} carrying the name
             */
            public Optional<String> importerName() {
                return Optional.ofNullable(importerName);
            }

            /**
             * Returns the importer address rendered on the disclosure address lines.
             *
             * <p>An empty optional means the relay omitted the block.
             *
             * @return an {@link Optional} carrying the address
             */
            public Optional<ImporterAddress> importerAddress() {
                return Optional.ofNullable(importerAddress);
            }

            /**
             * Compares this block with another for value equality on the country code and importer
             * fields.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is an equal block
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (ComplianceInfo) obj;
                return Objects.equals(this.countryCodeOrigin, that.countryCodeOrigin)
                        && Objects.equals(this.importerName, that.importerName)
                        && Objects.equals(this.importerAddress, that.importerAddress);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(countryCodeOrigin, importerName, importerAddress);
            }

            /**
             * Returns a diagnostic string naming the country code and importer name.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "IqQueryProductListCatalogResponse.Success.ComplianceInfo[countryCodeOrigin="
                        + countryCodeOrigin + ", importerName=" + importerName + ']';
            }
        }

        /**
         * Carries the importer address block on a {@link ComplianceInfo}: street, city, region,
         * postcode and country.
         *
         * <p>The block backs the importer address lines of the regulatory disclosure; street1, city
         * and country code are mandatory while the remaining lines are optional.
         */
        public static final class ImporterAddress {
            /**
             * Holds the street line 1.
             */
            private final String street1;

            /**
             * Holds the optional street line 2.
             */
            private final String street2;

            /**
             * Holds the optional postal code.
             */
            private final String postalCode;

            /**
             * Holds the city.
             */
            private final String city;

            /**
             * Holds the optional state, region or province.
             */
            private final String region;

            /**
             * Holds the ISO 3166-1 alpha-2 country code.
             */
            private final String countryCode;

            /**
             * Constructs an address from the wire-decoded fields; called from the response parser.
             *
             * <p>The wire schema requires street1, city and country code to be present.
             *
             * @param street1     the line-1 street; never {@code null}
             * @param street2     the line-2 street; may be {@code null}
             * @param postalCode  the postal code; may be {@code null}
             * @param city        the city; never {@code null}
             * @param region      the region; may be {@code null}
             * @param countryCode the country code; never {@code null}
             * @throws NullPointerException if {@code street1}, {@code city} or {@code countryCode} is
             *                              {@code null}
             */
            public ImporterAddress(String street1, String street2, String postalCode,
                                   String city, String region, String countryCode) {
                this.street1 = Objects.requireNonNull(street1, "street1 cannot be null");
                this.street2 = street2;
                this.postalCode = postalCode;
                this.city = Objects.requireNonNull(city, "city cannot be null");
                this.region = region;
                this.countryCode = Objects.requireNonNull(countryCode, "countryCode cannot be null");
            }

            /**
             * Returns the line-1 street rendered as the first street line of the importer address.
             *
             * @return the street; never {@code null}
             */
            public String street1() {
                return street1;
            }

            /**
             * Returns the optional line-2 street.
             *
             * <p>An empty optional means the relay omitted the field.
             *
             * @return an {@link Optional} carrying the street
             */
            public Optional<String> street2() {
                return Optional.ofNullable(street2);
            }

            /**
             * Returns the optional postal code.
             *
             * <p>An empty optional means the relay omitted the field.
             *
             * @return an {@link Optional} carrying the code
             */
            public Optional<String> postalCode() {
                return Optional.ofNullable(postalCode);
            }

            /**
             * Returns the city rendered on the importer address.
             *
             * @return the city; never {@code null}
             */
            public String city() {
                return city;
            }

            /**
             * Returns the optional state, region or province.
             *
             * <p>An empty optional means the relay omitted the field.
             *
             * @return an {@link Optional} carrying the region
             */
            public Optional<String> region() {
                return Optional.ofNullable(region);
            }

            /**
             * Returns the ISO 3166-1 alpha-2 country code rendered on the importer address.
             *
             * @return the code; never {@code null}
             */
            public String countryCode() {
                return countryCode;
            }

            /**
             * Compares this address with another for value equality across every line.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is an equal address
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (ImporterAddress) obj;
                return Objects.equals(this.street1, that.street1)
                        && Objects.equals(this.street2, that.street2)
                        && Objects.equals(this.postalCode, that.postalCode)
                        && Objects.equals(this.city, that.city)
                        && Objects.equals(this.region, that.region)
                        && Objects.equals(this.countryCode, that.countryCode);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(street1, street2, postalCode, city, region, countryCode);
            }

            /**
             * Returns a diagnostic string naming street1, city and country code.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "IqQueryProductListCatalogResponse.Success.ImporterAddress[street1=" + street1
                        + ", city=" + city + ", countryCode=" + countryCode + ']';
            }
        }

        /**
         * Holds the decoded products in wire order.
         */
        private final List<Product> products;

        /**
         * Constructs a success reply from the decoded products; called from {@link #of(Stanza, Stanza)}.
         *
         * <p>The supplied list is defensively copied so the caller may mutate the source freely after
         * construction.
         *
         * @param products the product list; never {@code null}
         * @throws NullPointerException if {@code products} is {@code null}
         */
        public Success(List<Product> products) {
            Objects.requireNonNull(products, "products cannot be null");
            this.products = List.copyOf(products);
        }

        /**
         * Returns the decoded products driving the catalog grid.
         *
         * <p>The order matches the wire order, which matches the caller-supplied id order on the
         * {@link IqQueryProductListCatalogRequest}.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<Product> products() {
            return products;
        }

        /**
         * Tries to parse a {@link Success} variant from the stanza.
         *
         * <p>The method validates the {@code <iq type="result">} envelope, iterates over every
         * {@code <product/>} child of the {@code <product_list/>} payload and dispatches to
         * {@link #parseProduct(Stanza, String)} for non-invalid entries; called from
         * {@link #of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation detects the synthetic invalid-product marker by checking the
         * {@code <status/>} grandchild for the literal {@code "INVALID_PRODUCT"} body.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebQueryProductListCatalogJob",
                exports = "QueryProductListCatalog", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var productListNode = stanza.getChild("product_list").orElse(null);
            if (productListNode == null) {
                return Optional.of(new Success(Collections.emptyList()));
            }
            var products = new ArrayList<Product>();
            for (var productNode : productListNode.getChildren("product")) {
                var idNode = productNode.getChild("id").orElse(null);
                if (idNode == null) {
                    continue;
                }
                var id = idNode.toContentString().orElse(null);
                if (id == null) {
                    continue;
                }
                var statusNode = productNode.getChild("status").orElse(null);
                if (statusNode != null
                        && "INVALID_PRODUCT".equals(statusNode.toContentString().orElse(""))) {
                    products.add(Product.invalid(id));
                    continue;
                }
                products.add(parseProduct(productNode, id));
            }
            return Optional.of(new Success(products));
        }

        /**
         * Decodes a single {@code <product/>} child into the typed {@link Product} projection.
         *
         * <p>The helper reads every supported grandchild and attribute of the product stanza and
         * assembles a fully populated entry for {@link #of(Stanza, Stanza)}.
         *
         * @implNote
         * The {@code max_available} field is read from either the attribute or the grandchild (the
         * relay uses both shapes across snapshots) and the cap defaults to 99 when both are absent or
         * unparseable.
         *
         * @param productStanza the {@code <product/>} child
         * @param id          the parsed product id
         * @return the decoded product entry; never {@code null}
         */
        private static Product parseProduct(Stanza productStanza, String id) {
            var name = productStanza.getChild("name")
                    .flatMap(Stanza::toContentString).orElse(null);
            var description = productStanza.getChild("description")
                    .flatMap(Stanza::toContentString).orElse(null);
            var url = productStanza.getChild("url")
                    .flatMap(Stanza::toContentString).orElse(null);
            var retailerId = productStanza.getChild("retailer_id")
                    .flatMap(Stanza::toContentString).orElse(null);
            var availability = productStanza.getAttributeAsString("availability").orElse(null);
            var currency = productStanza.getChild("currency")
                    .flatMap(Stanza::toContentString).orElse(null);
            var price = productStanza.getChild("price")
                    .flatMap(Stanza::toContentString).orElse(null);
            var hidden = productStanza.getAttributeAsString("is_hidden")
                    .map("true"::equals).orElse(false);
            var sanctioned = productStanza.getAttributeAsString("is_sanctioned")
                    .map("true"::equals).orElse(false);
            var checkmark = productStanza.getChild("belongs_to")
                    .flatMap(Stanza::toContentString)
                    .map("true"::equals).orElse(false);
            var complianceCategory = productStanza.getAttributeAsString("compliance_category").orElse(null);
            var maxAvailable = 99;
            var maxAvailableAttr = productStanza.getAttributeAsString("max_available").orElse(null);
            if (maxAvailableAttr != null) {
                try {
                    maxAvailable = Integer.parseInt(maxAvailableAttr);
                } catch (NumberFormatException _) {
                }
            }
            var maxAvailableChild = productStanza.getChild("max_available")
                    .flatMap(Stanza::toContentString).orElse(null);
            if (maxAvailableChild != null) {
                try {
                    maxAvailable = Integer.parseInt(maxAvailableChild);
                } catch (NumberFormatException _) {
                }
            }
            String whatsappStatus = null;
            var canAppeal = false;
            var statusInfoNode = productStanza.getChild("status_info").orElse(null);
            if (statusInfoNode != null) {
                whatsappStatus = statusInfoNode.getChild("status")
                        .flatMap(Stanza::toContentString).orElse(null);
                canAppeal = statusInfoNode.getChild("can_appeal")
                        .flatMap(Stanza::toContentString)
                        .map("true"::equals).orElse(false);
            }
            var images = new ArrayList<Image>();
            var videos = new ArrayList<Video>();
            productStanza.getChild("media").ifPresent(media -> {
                for (var img : media.getChildren("image")) {
                    var imgId = img.getChild("id")
                            .flatMap(Stanza::toContentString).orElse("");
                    var requested = img.getChild("request_image_url")
                            .flatMap(Stanza::toContentString).orElse("");
                    var full = img.getChild("original_image_url")
                            .flatMap(Stanza::toContentString).orElse("");
                    images.add(new Image(imgId, requested, full));
                }
                for (var vid : media.getChildren("video")) {
                    var vidId = vid.getChild("id")
                            .flatMap(Stanza::toContentString).orElse("");
                    var original = vid.getChild("original_video_url")
                            .flatMap(Stanza::toContentString).orElse("");
                    var thumb = vid.getChild("thumbnail_url")
                            .flatMap(Stanza::toContentString).orElse("");
                    videos.add(new Video(vidId, original, thumb));
                }
            });
            SalePrice salePrice = null;
            var salePriceNode = productStanza.getChild("sale_price").orElse(null);
            if (salePriceNode != null) {
                var sp = salePriceNode.getChild("price")
                        .flatMap(Stanza::toContentString).orElse("");
                var start = salePriceNode.getChild("start_date")
                        .flatMap(Stanza::toContentString).orElse(null);
                var end = salePriceNode.getChild("end_date")
                        .flatMap(Stanza::toContentString).orElse(null);
                salePrice = new SalePrice(sp, start, end);
            }
            ComplianceInfo complianceInfo = null;
            var complianceInfoNode = productStanza.getChild("compliance_info").orElse(null);
            if (complianceInfoNode != null) {
                var country = complianceInfoNode.getChild("country_code_origin")
                        .flatMap(Stanza::toContentString).orElse("");
                var importerName = complianceInfoNode.getChild("importer_name")
                        .flatMap(Stanza::toContentString).orElse(null);
                ImporterAddress importerAddress = null;
                var importerAddressNode = complianceInfoNode.getChild("importer_address").orElse(null);
                if (importerAddressNode != null) {
                    var street1 = importerAddressNode.getChild("street1")
                            .flatMap(Stanza::toContentString).orElse("");
                    var street2 = importerAddressNode.getChild("street2")
                            .flatMap(Stanza::toContentString).orElse(null);
                    var postal = importerAddressNode.getChild("postal_code")
                            .flatMap(Stanza::toContentString).orElse(null);
                    var city = importerAddressNode.getChild("city")
                            .flatMap(Stanza::toContentString).orElse("");
                    var region = importerAddressNode.getChild("region")
                            .flatMap(Stanza::toContentString).orElse(null);
                    var ccode = importerAddressNode.getChild("country_code")
                            .flatMap(Stanza::toContentString).orElse("");
                    importerAddress = new ImporterAddress(street1, street2, postal, city, region, ccode);
                }
                complianceInfo = new ComplianceInfo(country, importerName, importerAddress);
            }
            var signedShimmedUrl = productStanza.getChild("shimmed_url")
                    .flatMap(Stanza::toContentString)
                    .filter(s -> !s.isEmpty()).orElse(null);
            return new Product(id, false, name, description, url, retailerId, availability,
                    maxAvailable, currency, price, hidden, sanctioned, checkmark,
                    whatsappStatus, canAppeal, images, videos, salePrice, complianceInfo,
                    signedShimmedUrl, complianceCategory);
        }

        /**
         * Compares this variant with another for value equality on the products.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal success
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.products, that.products);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(products);
        }

        /**
         * Returns a diagnostic string naming the products.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqQueryProductListCatalogResponse.Success[products=" + products + ']';
        }
    }

    /**
     * Models the {@code ClientError} variant, emitted when the relay rejects the request as
     * malformed or referencing an unknown merchant or catalog.
     *
     * <p>Surface it as a user-facing 4xx-class error on the catalog grid.
     */
    final class ClientError implements IqQueryProductListCatalogResponse {
        /**
         * Holds the numeric error code echoed by the {@code <error/>} child.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the {@code <error/>} child.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the relay's {@code <error/>} envelope; called from
         * {@link #of(Stanza, Stanza)}.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code, used to dispatch on the relay-side rejection reason.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, when supplied.
         *
         * <p>The text is server-localised and not stable across snapshots, so it is suitable for
         * logging only.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the stanza.
         *
         * <p>The method delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} to
         * extract the (code, text) envelope; called from {@link #of(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the client-error schema
         */
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant with another for value equality on the code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal client error
         */
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

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a diagnostic string naming the error code and text.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqQueryProductListCatalogResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the {@code ServerError} variant, emitted when the relay returns a transient
     * internal-failure status while processing the request.
     *
     * <p>The relay returns this shape when the catalog backend is temporarily unavailable; use it to
     * drive a backoff-and-retry path on the catalog grid.
     */
    final class ServerError implements IqQueryProductListCatalogResponse {
        /**
         * Holds the numeric error code echoed by the {@code <error/>} child.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the {@code <error/>} child.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the relay's {@code <error/>} envelope; called from
         * {@link #of(Stanza, Stanza)}.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code; a 5xx-class value is the canonical retry trigger.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, when supplied.
         *
         * <p>The text is server-localised and not stable across snapshots, so it is suitable for
         * logging only.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the stanza.
         *
         * <p>The method delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} to
         * extract the (code, text) envelope; called from {@link #of(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the server-error schema
         */
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant with another for value equality on the code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal server error
         */
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

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a diagnostic string naming the error code and text.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqQueryProductListCatalogResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

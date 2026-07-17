package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Parses the graph.whatsapp.com GraphQL response of the single-product query built by
 * {@link QueryCatalogProductWhatsAppGraphQlRequest}.
 *
 * <p>Exposes the linked chain {@code xwa_product_catalog_get_product -> product_catalog -> product},
 * projecting the single {@link Product} onto the full catalog-product model: identifiers, core
 * metadata, status, sale price, media, compliance info and variant info.
 *
 * @see QueryCatalogProductWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebQueryCatalogProductQuery")
public final class QueryCatalogProductWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed product, or {@code null} when the graph.whatsapp.com endpoint omitted any link in the
     * {@code xwa_product_catalog_get_product -> product_catalog -> product} chain.
     */
    private final Product product;

    /**
     * Constructs a response wrapping the parsed product.
     *
     * <p>Reserved for the static parser.
     *
     * @param product the parsed product, or {@code null} when the graph.whatsapp.com endpoint omitted it
     */
    private QueryCatalogProductWhatsAppGraphQlResponse(Product product) {
        this.product = product;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked chain {@code xwa_product_catalog_get_product -> product_catalog -> product};
     * the returned {@link Optional} is empty when {@code data} is {@code null}, and the wrapped
     * {@link #product()} is empty when any intermediate link is absent.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<QueryCatalogProductWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var success = data.getJSONObject("xwa_product_catalog_get_product");
        var productCatalog = success == null ? null : success.getJSONObject("product_catalog");
        var product = productCatalog == null ? null : Product.of(productCatalog.getJSONObject("product")).orElse(null);
        return Optional.of(new QueryCatalogProductWhatsAppGraphQlResponse(product));
    }

    /**
     * Returns the parsed product.
     *
     * @return the parsed {@link Product}, or empty when the graph.whatsapp.com endpoint omitted any link in the response
     *         chain
     */
    public Optional<Product> product() {
        return Optional.ofNullable(product);
    }

    /**
     * Enumerates the closed set of {@code product_availability} values the graph.whatsapp.com catalog endpoint returns.
     *
     * <p>The constants mirror the wire literals WhatsApp Web maps onto its in-stock model in
     * {@code WAWebBizParseProductGraphql}; any value outside this set, including a missing field,
     * collapses to {@link #UNKNOWN}.
     */
    public enum ProductAvailability {
        /**
         * The product is in stock.
         */
        IN_STOCK,

        /**
         * The product is out of stock.
         */
        OUT_OF_STOCK,

        /**
         * The product is available for a different postcode than the one queried.
         */
        AVAILABLE_FOR_ANOTHER_POSTCODE,

        /**
         * The graph.whatsapp.com endpoint returned no availability or a value outside the known set.
         */
        UNKNOWN;

        /**
         * Resolves a wire {@code product_availability} literal to its constant, defaulting unknown or
         * {@code null} values to {@link #UNKNOWN}.
         *
         * @param wire the wire literal, may be {@code null}
         * @return the matching constant, or {@link #UNKNOWN} when {@code wire} is {@code null} or
         *         outside the known set
         */
        static ProductAvailability of(String wire) {
            if (wire == null) {
                return UNKNOWN;
            }
            return switch (wire) {
                case "IN_STOCK" -> IN_STOCK;
                case "OUT_OF_STOCK" -> OUT_OF_STOCK;
                case "AVAILABLE_FOR_ANOTHER_POSTCODE" -> AVAILABLE_FOR_ANOTHER_POSTCODE;
                default -> UNKNOWN;
            };
        }
    }

    /**
     * Wraps a single {@code XWACatalogProduct} returned by the graph.whatsapp.com catalog endpoint.
     *
     * <p>Carries the product's identifiers, core metadata and the linked status, sale-price, media,
     * compliance and variant sub-objects. Numeric-valued fields that WhatsApp Web serializes as
     * strings ({@code max_available}, {@code price}) are kept as strings; {@code is_hidden},
     * {@code belongs_to} and {@code status_info.can_appeal} are stringified-boolean wire enums kept as
     * strings.
     */
    public static final class Product {
        /**
         * Holds the product id.
         */
        private final String id;

        /**
         * Holds the merchant-assigned retailer id.
         */
        private final String retailerId;

        /**
         * Holds the {@code is_hidden} wire enum literal ({@code ISHIDDEN_TRUE}/{@code ISHIDDEN_FALSE}).
         */
        private final String isHidden;

        /**
         * Holds whether the product is sanctioned.
         */
        private final Boolean isSanctioned;

        /**
         * Holds the parsed product-availability verdict.
         */
        private final ProductAvailability productAvailability;

        /**
         * Holds the maximum available quantity, serialized as a numeric string on the wire.
         */
        private final String maxAvailable;

        /**
         * Holds the product name.
         */
        private final String name;

        /**
         * Holds the product description.
         */
        private final String description;

        /**
         * Holds the product URL.
         */
        private final String url;

        /**
         * Holds the shimmed (signed) product URL.
         */
        private final String shimmedUrl;

        /**
         * Holds the ISO 4217 currency code.
         */
        private final String currency;

        /**
         * Holds the price in the smallest currency unit, serialized as a numeric string on the wire.
         */
        private final String price;

        /**
         * Holds the linked status-info sub-object, or {@code null} when absent.
         */
        private final StatusInfo statusInfo;

        /**
         * Holds the linked sale-price sub-object, or {@code null} when absent.
         */
        private final SalePrice salePrice;

        /**
         * Holds the linked media sub-object, or {@code null} when absent.
         */
        private final Media media;

        /**
         * Holds the {@code belongs_to} stringified-boolean wire enum ({@code "true"}/{@code "false"}).
         */
        private final String belongsTo;

        /**
         * Holds the {@code compliance_category} wire enum literal.
         */
        private final String complianceCategory;

        /**
         * Holds the linked compliance-info sub-object, or {@code null} when absent.
         */
        private final ComplianceInfo complianceInfo;

        /**
         * Holds the linked variant-info sub-object, or {@code null} when absent.
         */
        private final VariantInfo variantInfo;

        /**
         * Constructs a product wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param id                  the product id
         * @param retailerId          the merchant-assigned retailer id
         * @param isHidden            the {@code is_hidden} wire enum literal
         * @param isSanctioned        whether the product is sanctioned
         * @param productAvailability the parsed product-availability verdict
         * @param maxAvailable        the maximum available quantity as a numeric string
         * @param name                the product name
         * @param description         the product description
         * @param url                 the product URL
         * @param shimmedUrl          the shimmed product URL
         * @param currency            the ISO 4217 currency code
         * @param price               the price as a numeric string
         * @param statusInfo          the linked status-info sub-object, or {@code null}
         * @param salePrice           the linked sale-price sub-object, or {@code null}
         * @param media               the linked media sub-object, or {@code null}
         * @param belongsTo           the {@code belongs_to} stringified-boolean wire enum
         * @param complianceCategory  the {@code compliance_category} wire enum literal
         * @param complianceInfo      the linked compliance-info sub-object, or {@code null}
         * @param variantInfo         the linked variant-info sub-object, or {@code null}
         */
        private Product(String id, String retailerId, String isHidden, Boolean isSanctioned,
                        ProductAvailability productAvailability, String maxAvailable, String name, String description,
                        String url, String shimmedUrl, String currency, String price, StatusInfo statusInfo,
                        SalePrice salePrice, Media media, String belongsTo, String complianceCategory,
                        ComplianceInfo complianceInfo, VariantInfo variantInfo) {
            this.id = id;
            this.retailerId = retailerId;
            this.isHidden = isHidden;
            this.isSanctioned = isSanctioned;
            this.productAvailability = productAvailability;
            this.maxAvailable = maxAvailable;
            this.name = name;
            this.description = description;
            this.url = url;
            this.shimmedUrl = shimmedUrl;
            this.currency = currency;
            this.price = price;
            this.statusInfo = statusInfo;
            this.salePrice = salePrice;
            this.media = media;
            this.belongsTo = belongsTo;
            this.complianceCategory = complianceCategory;
            this.complianceInfo = complianceInfo;
            this.variantInfo = variantInfo;
        }

        /**
         * Returns the product id.
         *
         * @return the product id, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the merchant-assigned retailer id.
         *
         * @return the retailer id, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> retailerId() {
            return Optional.ofNullable(retailerId);
        }

        /**
         * Returns the {@code is_hidden} wire enum literal.
         *
         * <p>WhatsApp Web treats the product as hidden when this equals {@code "ISHIDDEN_TRUE"}.
         *
         * @return the {@code is_hidden} literal, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> isHidden() {
            return Optional.ofNullable(isHidden);
        }

        /**
         * Returns whether the product is sanctioned.
         *
         * @return {@code true} when the graph.whatsapp.com endpoint flagged the product as sanctioned, {@code false}
         *         otherwise or when the field was omitted
         */
        public boolean isSanctioned() {
            return isSanctioned != null && isSanctioned;
        }

        /**
         * Returns the parsed product-availability verdict.
         *
         * @return the availability verdict, {@link ProductAvailability#UNKNOWN} when the graph.whatsapp.com endpoint omitted
         *         the field
         */
        public ProductAvailability productAvailability() {
            return productAvailability;
        }

        /**
         * Returns the maximum available quantity.
         *
         * @return the maximum available quantity as a numeric string, or empty when the graph.whatsapp.com endpoint omitted
         *         the field
         */
        public Optional<String> maxAvailable() {
            return Optional.ofNullable(maxAvailable);
        }

        /**
         * Returns the product name.
         *
         * @return the product name, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> name() {
            return Optional.ofNullable(name);
        }

        /**
         * Returns the product description.
         *
         * @return the product description, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> description() {
            return Optional.ofNullable(description);
        }

        /**
         * Returns the product URL.
         *
         * @return the product URL, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> url() {
            return Optional.ofNullable(url);
        }

        /**
         * Returns the shimmed (signed) product URL.
         *
         * @return the shimmed URL, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> shimmedUrl() {
            return Optional.ofNullable(shimmedUrl);
        }

        /**
         * Returns the ISO 4217 currency code.
         *
         * @return the currency code, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> currency() {
            return Optional.ofNullable(currency);
        }

        /**
         * Returns the price in the smallest currency unit.
         *
         * @return the price as a numeric string, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> price() {
            return Optional.ofNullable(price);
        }

        /**
         * Returns the linked status-info sub-object.
         *
         * @return the parsed {@link StatusInfo}, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<StatusInfo> statusInfo() {
            return Optional.ofNullable(statusInfo);
        }

        /**
         * Returns the linked sale-price sub-object.
         *
         * @return the parsed {@link SalePrice}, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<SalePrice> salePrice() {
            return Optional.ofNullable(salePrice);
        }

        /**
         * Returns the linked media sub-object.
         *
         * @return the parsed {@link Media}, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<Media> media() {
            return Optional.ofNullable(media);
        }

        /**
         * Returns the {@code belongs_to} stringified-boolean wire enum.
         *
         * <p>WhatsApp Web treats the product as belonging to the checkmark collection when this equals
         * {@code "true"}.
         *
         * @return the {@code belongs_to} literal, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> belongsTo() {
            return Optional.ofNullable(belongsTo);
        }

        /**
         * Returns the {@code compliance_category} wire enum literal.
         *
         * @return the compliance-category literal, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> complianceCategory() {
            return Optional.ofNullable(complianceCategory);
        }

        /**
         * Returns the linked compliance-info sub-object.
         *
         * @return the parsed {@link ComplianceInfo}, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<ComplianceInfo> complianceInfo() {
            return Optional.ofNullable(complianceInfo);
        }

        /**
         * Returns the linked variant-info sub-object.
         *
         * @return the parsed {@link VariantInfo}, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<VariantInfo> variantInfo() {
            return Optional.ofNullable(variantInfo);
        }

        /**
         * Parses a {@link Product} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link Product}, or empty when {@code obj} is {@code null}
         */
        static Optional<Product> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var statusInfo = StatusInfo.of(obj.getJSONObject("status_info")).orElse(null);
            var salePrice = SalePrice.of(obj.getJSONObject("sale_price")).orElse(null);
            var media = Media.of(obj.getJSONObject("media")).orElse(null);
            var complianceInfo = ComplianceInfo.of(obj.getJSONObject("compliance_info")).orElse(null);
            var variantInfo = VariantInfo.of(obj.getJSONObject("variant_info")).orElse(null);
            return Optional.of(new Product(
                    obj.getString("id"),
                    obj.getString("retailer_id"),
                    obj.getString("is_hidden"),
                    obj.getBoolean("is_sanctioned"),
                    ProductAvailability.of(obj.getString("product_availability")),
                    obj.getString("max_available"),
                    obj.getString("name"),
                    obj.getString("description"),
                    obj.getString("url"),
                    obj.getString("shimmed_url"),
                    obj.getString("currency"),
                    obj.getString("price"),
                    statusInfo,
                    salePrice,
                    media,
                    obj.getString("belongs_to"),
                    obj.getString("compliance_category"),
                    complianceInfo,
                    variantInfo));
        }

        /**
         * Parses a list of {@link Product} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<Product> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Product>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Wraps the {@code status_info} sub-object of type {@code XWAProductCatalogProductStatusInfo}.
     *
     * <p>Carries the product's review status and whether a rejected product can be appealed.
     */
    public static final class StatusInfo {
        /**
         * Holds the {@code can_appeal} stringified-boolean wire enum ({@code "true"}/{@code "false"}).
         */
        private final String canAppeal;

        /**
         * Holds the review status literal.
         */
        private final String status;

        /**
         * Constructs a status-info wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param canAppeal the {@code can_appeal} stringified-boolean wire enum
         * @param status    the review status literal
         */
        private StatusInfo(String canAppeal, String status) {
            this.canAppeal = canAppeal;
            this.status = status;
        }

        /**
         * Returns the {@code can_appeal} stringified-boolean wire enum.
         *
         * <p>WhatsApp Web treats a rejected product as appealable when this equals {@code "true"}.
         *
         * @return the {@code can_appeal} literal, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> canAppeal() {
            return Optional.ofNullable(canAppeal);
        }

        /**
         * Returns the review status literal.
         *
         * <p>WhatsApp Web's {@code asProductReviewType} recognises {@code APPROVED}, {@code PENDING}
         * and {@code REJECTED} and defaults a missing value to {@code APPROVED}; the field is kept as
         * a string because the graph.whatsapp.com endpoint consumes it leniently rather than enforcing the closed set.
         *
         * @return the review status literal, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> status() {
            return Optional.ofNullable(status);
        }

        /**
         * Parses a {@link StatusInfo} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link StatusInfo}, or empty when {@code obj} is {@code null}
         */
        static Optional<StatusInfo> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new StatusInfo(obj.getString("can_appeal"), obj.getString("status")));
        }
    }

    /**
     * Wraps the {@code sale_price} sub-object of type {@code XWAProductCatalogProductSalePrice}.
     *
     * <p>Carries the discounted price and the ISO 8601 start and end dates bounding the sale window.
     */
    public static final class SalePrice {
        /**
         * Holds the sale price in the smallest currency unit, serialized as a numeric string.
         */
        private final String price;

        /**
         * Holds the sale start date as an ISO 8601 string.
         */
        private final String startDate;

        /**
         * Holds the sale end date as an ISO 8601 string.
         */
        private final String endDate;

        /**
         * Constructs a sale-price wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param price     the sale price as a numeric string
         * @param startDate the sale start date as an ISO 8601 string
         * @param endDate   the sale end date as an ISO 8601 string
         */
        private SalePrice(String price, String startDate, String endDate) {
            this.price = price;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        /**
         * Returns the sale price.
         *
         * @return the sale price as a numeric string, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> price() {
            return Optional.ofNullable(price);
        }

        /**
         * Returns the sale start date.
         *
         * @return the start date as an ISO 8601 string, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> startDate() {
            return Optional.ofNullable(startDate);
        }

        /**
         * Returns the sale end date.
         *
         * @return the end date as an ISO 8601 string, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> endDate() {
            return Optional.ofNullable(endDate);
        }

        /**
         * Parses a {@link SalePrice} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link SalePrice}, or empty when {@code obj} is {@code null}
         */
        static Optional<SalePrice> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new SalePrice(obj.getString("price"), obj.getString("start_date"), obj.getString("end_date")));
        }
    }

    /**
     * Wraps the {@code media} sub-object of type {@code XWAProductCatalogProductMedia}.
     *
     * <p>Carries the product's image and video galleries.
     */
    public static final class Media {
        /**
         * Holds the product images.
         */
        private final List<Image> images;

        /**
         * Holds the product videos.
         */
        private final List<Video> videos;

        /**
         * Constructs a media wrapper from its parsed galleries.
         *
         * <p>Reserved for the static parser.
         *
         * @param images the product images
         * @param videos the product videos
         */
        private Media(List<Image> images, List<Video> videos) {
            this.images = images;
            this.videos = videos;
        }

        /**
         * Returns the product images.
         *
         * @return the product images, empty when the graph.whatsapp.com endpoint returned none
         */
        public List<Image> images() {
            return images;
        }

        /**
         * Returns the product videos.
         *
         * @return the product videos, empty when the graph.whatsapp.com endpoint returned none
         */
        public List<Video> videos() {
            return videos;
        }

        /**
         * Parses a {@link Media} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link Media}, or empty when {@code obj} is {@code null}
         */
        static Optional<Media> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new Media(Image.ofArray(obj.getJSONArray("images")), Video.ofArray(obj.getJSONArray("videos"))));
        }
    }

    /**
     * Wraps an image of type {@code XWAProductCatalogProductMediaImage}.
     *
     * <p>Carries the image id and its original and requested-size CDN URLs, plus the optional original
     * dimensions selected only in the variant-thumbnail context.
     */
    public static final class Image {
        /**
         * Holds the image id.
         */
        private final String id;

        /**
         * Holds the optional original dimensions, present only in the variant-thumbnail context.
         */
        private final OriginalDimensions originalDimensions;

        /**
         * Holds the original-size image CDN URL.
         */
        private final String originalImageUrl;

        /**
         * Holds the requested-size image CDN URL.
         */
        private final String requestImageUrl;

        /**
         * Constructs an image wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param id                 the image id
         * @param originalDimensions the optional original dimensions, or {@code null}
         * @param originalImageUrl   the original-size image CDN URL
         * @param requestImageUrl    the requested-size image CDN URL
         */
        private Image(String id, OriginalDimensions originalDimensions, String originalImageUrl, String requestImageUrl) {
            this.id = id;
            this.originalDimensions = originalDimensions;
            this.originalImageUrl = originalImageUrl;
            this.requestImageUrl = requestImageUrl;
        }

        /**
         * Returns the image id.
         *
         * @return the image id, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the optional original dimensions.
         *
         * @return the parsed {@link OriginalDimensions}, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<OriginalDimensions> originalDimensions() {
            return Optional.ofNullable(originalDimensions);
        }

        /**
         * Returns the original-size image CDN URL.
         *
         * @return the original image URL, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> originalImageUrl() {
            return Optional.ofNullable(originalImageUrl);
        }

        /**
         * Returns the requested-size image CDN URL.
         *
         * @return the requested image URL, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> requestImageUrl() {
            return Optional.ofNullable(requestImageUrl);
        }

        /**
         * Parses an {@link Image} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link Image}, or empty when {@code obj} is {@code null}
         */
        static Optional<Image> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var originalDimensions = OriginalDimensions.of(obj.getJSONObject("original_dimensions")).orElse(null);
            return Optional.of(new Image(obj.getString("id"), originalDimensions,
                    obj.getString("original_image_url"), obj.getString("request_image_url")));
        }

        /**
         * Parses a list of {@link Image} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<Image> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Image>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Wraps the {@code original_dimensions} sub-object of type
     * {@code XWAProductCatalogProductMediaImageOriginalDimensions}.
     *
     * <p>Carries the original pixel height and width of a variant thumbnail image.
     */
    public static final class OriginalDimensions {
        /**
         * Holds the original height in pixels, or {@code null} when absent.
         */
        private final Integer height;

        /**
         * Holds the original width in pixels, or {@code null} when absent.
         */
        private final Integer width;

        /**
         * Constructs an original-dimensions wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param height the original height in pixels, or {@code null}
         * @param width  the original width in pixels, or {@code null}
         */
        private OriginalDimensions(Integer height, Integer width) {
            this.height = height;
            this.width = width;
        }

        /**
         * Returns the original height in pixels.
         *
         * @return the height, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public OptionalInt height() {
            return height == null ? OptionalInt.empty() : OptionalInt.of(height);
        }

        /**
         * Returns the original width in pixels.
         *
         * @return the width, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public OptionalInt width() {
            return width == null ? OptionalInt.empty() : OptionalInt.of(width);
        }

        /**
         * Parses an {@link OriginalDimensions} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link OriginalDimensions}, or empty when {@code obj} is {@code null}
         */
        static Optional<OriginalDimensions> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new OriginalDimensions(obj.getInteger("height"), obj.getInteger("width")));
        }
    }

    /**
     * Wraps a video of type {@code XWAProductCatalogProductMediaVideo}.
     *
     * <p>Carries the video id and its original and thumbnail CDN URLs.
     */
    public static final class Video {
        /**
         * Holds the video id.
         */
        private final String id;

        /**
         * Holds the original video CDN URL.
         */
        private final String originalVideoUrl;

        /**
         * Holds the thumbnail CDN URL.
         */
        private final String thumbnailUrl;

        /**
         * Constructs a video wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param id               the video id
         * @param originalVideoUrl the original video CDN URL
         * @param thumbnailUrl     the thumbnail CDN URL
         */
        private Video(String id, String originalVideoUrl, String thumbnailUrl) {
            this.id = id;
            this.originalVideoUrl = originalVideoUrl;
            this.thumbnailUrl = thumbnailUrl;
        }

        /**
         * Returns the video id.
         *
         * @return the video id, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the original video CDN URL.
         *
         * @return the original video URL, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> originalVideoUrl() {
            return Optional.ofNullable(originalVideoUrl);
        }

        /**
         * Returns the thumbnail CDN URL.
         *
         * @return the thumbnail URL, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> thumbnailUrl() {
            return Optional.ofNullable(thumbnailUrl);
        }

        /**
         * Parses a {@link Video} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link Video}, or empty when {@code obj} is {@code null}
         */
        static Optional<Video> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new Video(obj.getString("id"), obj.getString("original_video_url"), obj.getString("thumbnail_url")));
        }

        /**
         * Parses a list of {@link Video} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<Video> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Video>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Wraps the {@code compliance_info} sub-object of type
     * {@code XWAProductCatalogProductComplianceInfo}.
     *
     * <p>Carries the product's country-of-origin compliance fields and the importer's address.
     */
    public static final class ComplianceInfo {
        /**
         * Holds the ISO country code of origin.
         */
        private final String countryCodeOrigin;

        /**
         * Holds the importer name.
         */
        private final String importerName;

        /**
         * Holds the linked importer-address sub-object, or {@code null} when absent.
         */
        private final ImporterAddress importerAddress;

        /**
         * Constructs a compliance-info wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param countryCodeOrigin the ISO country code of origin
         * @param importerName      the importer name
         * @param importerAddress   the linked importer-address sub-object, or {@code null}
         */
        private ComplianceInfo(String countryCodeOrigin, String importerName, ImporterAddress importerAddress) {
            this.countryCodeOrigin = countryCodeOrigin;
            this.importerName = importerName;
            this.importerAddress = importerAddress;
        }

        /**
         * Returns the ISO country code of origin.
         *
         * @return the country code of origin, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> countryCodeOrigin() {
            return Optional.ofNullable(countryCodeOrigin);
        }

        /**
         * Returns the importer name.
         *
         * @return the importer name, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> importerName() {
            return Optional.ofNullable(importerName);
        }

        /**
         * Returns the linked importer-address sub-object.
         *
         * @return the parsed {@link ImporterAddress}, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<ImporterAddress> importerAddress() {
            return Optional.ofNullable(importerAddress);
        }

        /**
         * Parses a {@link ComplianceInfo} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link ComplianceInfo}, or empty when {@code obj} is {@code null}
         */
        static Optional<ComplianceInfo> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var importerAddress = ImporterAddress.of(obj.getJSONObject("importer_address")).orElse(null);
            return Optional.of(new ComplianceInfo(obj.getString("country_code_origin"), obj.getString("importer_name"), importerAddress));
        }
    }

    /**
     * Wraps the {@code importer_address} sub-object of type
     * {@code XWAProductCatalogProductComplianceInfoImporterAddress}.
     *
     * <p>Carries the postal address of the product's importer.
     */
    public static final class ImporterAddress {
        /**
         * Holds the first street line.
         */
        private final String street1;

        /**
         * Holds the second street line.
         */
        private final String street2;

        /**
         * Holds the postal code.
         */
        private final String postalCode;

        /**
         * Holds the city.
         */
        private final String city;

        /**
         * Holds the region.
         */
        private final String region;

        /**
         * Holds the ISO country code.
         */
        private final String countryCode;

        /**
         * Constructs an importer-address wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param street1     the first street line
         * @param street2     the second street line
         * @param postalCode  the postal code
         * @param city        the city
         * @param region      the region
         * @param countryCode the ISO country code
         */
        private ImporterAddress(String street1, String street2, String postalCode, String city, String region, String countryCode) {
            this.street1 = street1;
            this.street2 = street2;
            this.postalCode = postalCode;
            this.city = city;
            this.region = region;
            this.countryCode = countryCode;
        }

        /**
         * Returns the first street line.
         *
         * @return the first street line, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> street1() {
            return Optional.ofNullable(street1);
        }

        /**
         * Returns the second street line.
         *
         * @return the second street line, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> street2() {
            return Optional.ofNullable(street2);
        }

        /**
         * Returns the postal code.
         *
         * @return the postal code, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> postalCode() {
            return Optional.ofNullable(postalCode);
        }

        /**
         * Returns the city.
         *
         * @return the city, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> city() {
            return Optional.ofNullable(city);
        }

        /**
         * Returns the region.
         *
         * @return the region, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> region() {
            return Optional.ofNullable(region);
        }

        /**
         * Returns the ISO country code.
         *
         * @return the country code, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> countryCode() {
            return Optional.ofNullable(countryCode);
        }

        /**
         * Parses an {@link ImporterAddress} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link ImporterAddress}, or empty when {@code obj} is {@code null}
         */
        static Optional<ImporterAddress> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new ImporterAddress(obj.getString("street1"), obj.getString("street2"),
                    obj.getString("postal_code"), obj.getString("city"), obj.getString("region"), obj.getString("country_code")));
        }
    }

    /**
     * Wraps the {@code variant_info} sub-object of type {@code XWACatalogProductVariantInfo}.
     *
     * <p>Carries the listing details, the per-variant availability, the variant types and the variant
     * properties of a multi-variant product.
     */
    public static final class VariantInfo {
        /**
         * Holds the linked listing-details sub-object, or {@code null} when absent.
         */
        private final ListingDetails listingDetails;

        /**
         * Holds the linked availability sub-object, or {@code null} when absent.
         */
        private final Availability availability;

        /**
         * Holds the variant types.
         */
        private final List<VariantType> types;

        /**
         * Holds the variant properties.
         */
        private final List<VariantProperty> variantProperties;

        /**
         * Constructs a variant-info wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param listingDetails    the linked listing-details sub-object, or {@code null}
         * @param availability      the linked availability sub-object, or {@code null}
         * @param types             the variant types
         * @param variantProperties the variant properties
         */
        private VariantInfo(ListingDetails listingDetails, Availability availability,
                            List<VariantType> types, List<VariantProperty> variantProperties) {
            this.listingDetails = listingDetails;
            this.availability = availability;
            this.types = types;
            this.variantProperties = variantProperties;
        }

        /**
         * Returns the linked listing-details sub-object.
         *
         * @return the parsed {@link ListingDetails}, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<ListingDetails> listingDetails() {
            return Optional.ofNullable(listingDetails);
        }

        /**
         * Returns the linked availability sub-object.
         *
         * @return the parsed {@link Availability}, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<Availability> availability() {
            return Optional.ofNullable(availability);
        }

        /**
         * Returns the variant types.
         *
         * @return the variant types, empty when the graph.whatsapp.com endpoint returned none
         */
        public List<VariantType> types() {
            return types;
        }

        /**
         * Returns the variant properties.
         *
         * @return the variant properties, empty when the graph.whatsapp.com endpoint returned none
         */
        public List<VariantProperty> variantProperties() {
            return variantProperties;
        }

        /**
         * Parses a {@link VariantInfo} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link VariantInfo}, or empty when {@code obj} is {@code null}
         */
        static Optional<VariantInfo> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var listingDetails = ListingDetails.of(obj.getJSONObject("listing_details")).orElse(null);
            var availability = Availability.of(obj.getJSONObject("availability")).orElse(null);
            return Optional.of(new VariantInfo(listingDetails, availability,
                    VariantType.ofArray(obj.getJSONArray("types")), VariantProperty.ofArray(obj.getJSONArray("variant_properties"))));
        }
    }

    /**
     * Wraps the {@code listing_details} sub-object of type {@code XWACatalogVariantListingDetails}.
     *
     * <p>Carries the variant-aware description and the price summary shown on the listing card.
     */
    public static final class ListingDetails {
        /**
         * Holds the variant-aware description.
         */
        private final String description;

        /**
         * Holds the {@code multi_price} listing price summary literal.
         */
        private final String multiPrice;

        /**
         * Holds the lowest variant price, serialized as a numeric string on the wire.
         */
        private final String lowestPrice;

        /**
         * Constructs a listing-details wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param description the variant-aware description
         * @param multiPrice  the listing price summary literal
         * @param lowestPrice the lowest variant price as a numeric string
         */
        private ListingDetails(String description, String multiPrice, String lowestPrice) {
            this.description = description;
            this.multiPrice = multiPrice;
            this.lowestPrice = lowestPrice;
        }

        /**
         * Returns the variant-aware description.
         *
         * @return the description, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> description() {
            return Optional.ofNullable(description);
        }

        /**
         * Returns the {@code multi_price} listing price summary literal.
         *
         * <p>Kept as a string because WhatsApp Web passes this field through verbatim, so its closed
         * value set cannot be confirmed from the bundle.
         *
         * @return the multi-price literal, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> multiPrice() {
            return Optional.ofNullable(multiPrice);
        }

        /**
         * Returns the lowest variant price.
         *
         * @return the lowest price as a numeric string, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> lowestPrice() {
            return Optional.ofNullable(lowestPrice);
        }

        /**
         * Parses a {@link ListingDetails} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link ListingDetails}, or empty when {@code obj} is {@code null}
         */
        static Optional<ListingDetails> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new ListingDetails(obj.getString("description"), obj.getString("multi_price"), obj.getString("lowest_price")));
        }
    }

    /**
     * Wraps the {@code availability} sub-object of type {@code XWACatalogVariantAvailability}.
     *
     * <p>Carries the per-variant availability listings.
     */
    public static final class Availability {
        /**
         * Holds the per-variant availability listings.
         */
        private final List<AvailabilityListing> listing;

        /**
         * Constructs an availability wrapper from its parsed listings.
         *
         * <p>Reserved for the static parser.
         *
         * @param listing the per-variant availability listings
         */
        private Availability(List<AvailabilityListing> listing) {
            this.listing = listing;
        }

        /**
         * Returns the per-variant availability listings.
         *
         * @return the listings, empty when the graph.whatsapp.com endpoint returned none
         */
        public List<AvailabilityListing> listing() {
            return listing;
        }

        /**
         * Parses an {@link Availability} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link Availability}, or empty when {@code obj} is {@code null}
         */
        static Optional<Availability> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new Availability(AvailabilityListing.ofArray(obj.getJSONArray("listing"))));
        }
    }

    /**
     * Wraps an availability listing of type {@code XWAVariantAvailabilityListing}.
     *
     * <p>Carries whether a specific variant combination is available, the option set that defines it,
     * and the resolved product id.
     */
    public static final class AvailabilityListing {
        /**
         * Holds whether this variant combination is available.
         */
        private final Boolean isAvailable;

        /**
         * Holds the option set defining this variant combination.
         */
        private final List<AvailabilityOption> options;

        /**
         * Holds the resolved product id for this variant combination.
         */
        private final String productId;

        /**
         * Constructs an availability-listing wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param isAvailable whether this variant combination is available
         * @param options     the option set defining this variant combination
         * @param productId   the resolved product id
         */
        private AvailabilityListing(Boolean isAvailable, List<AvailabilityOption> options, String productId) {
            this.isAvailable = isAvailable;
            this.options = options;
            this.productId = productId;
        }

        /**
         * Returns whether this variant combination is available.
         *
         * @return {@code true} when the graph.whatsapp.com endpoint flagged this combination as available, {@code false}
         *         otherwise or when the field was omitted
         */
        public boolean isAvailable() {
            return isAvailable != null && isAvailable;
        }

        /**
         * Returns the option set defining this variant combination.
         *
         * @return the options, empty when the graph.whatsapp.com endpoint returned none
         */
        public List<AvailabilityOption> options() {
            return options;
        }

        /**
         * Returns the resolved product id for this variant combination.
         *
         * @return the product id, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> productId() {
            return Optional.ofNullable(productId);
        }

        /**
         * Parses an {@link AvailabilityListing} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link AvailabilityListing}, or empty when {@code obj} is {@code null}
         */
        static Optional<AvailabilityListing> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new AvailabilityListing(obj.getBoolean("is_available"),
                    AvailabilityOption.ofArray(obj.getJSONArray("options")), obj.getString("product_id")));
        }

        /**
         * Parses a list of {@link AvailabilityListing} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<AvailabilityListing> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<AvailabilityListing>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Wraps a variant availability option of type {@code XWAVariantAvailabilityListingOption}.
     *
     * <p>Carries one name/value pair of the option set that identifies a variant combination.
     */
    public static final class AvailabilityOption {
        /**
         * Holds the option name.
         */
        private final String name;

        /**
         * Holds the option value.
         */
        private final String value;

        /**
         * Constructs an availability-option wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param name  the option name
         * @param value the option value
         */
        private AvailabilityOption(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Returns the option name.
         *
         * @return the option name, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> name() {
            return Optional.ofNullable(name);
        }

        /**
         * Returns the option value.
         *
         * @return the option value, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> value() {
            return Optional.ofNullable(value);
        }

        /**
         * Parses an {@link AvailabilityOption} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link AvailabilityOption}, or empty when {@code obj} is {@code null}
         */
        static Optional<AvailabilityOption> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new AvailabilityOption(obj.getString("name"), obj.getString("value")));
        }

        /**
         * Parses a list of {@link AvailabilityOption} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<AvailabilityOption> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<AvailabilityOption>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Wraps a variant type of type {@code XWACatalogVariantTypes}.
     *
     * <p>Carries one variant dimension (for example "Color") and its selectable options.
     */
    public static final class VariantType {
        /**
         * Holds the variant type name.
         */
        private final String name;

        /**
         * Holds the selectable options for this variant type.
         */
        private final List<VariantTypeOption> options;

        /**
         * Constructs a variant-type wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param name    the variant type name
         * @param options the selectable options for this variant type
         */
        private VariantType(String name, List<VariantTypeOption> options) {
            this.name = name;
            this.options = options;
        }

        /**
         * Returns the variant type name.
         *
         * @return the variant type name, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> name() {
            return Optional.ofNullable(name);
        }

        /**
         * Returns the selectable options for this variant type.
         *
         * @return the options, empty when the graph.whatsapp.com endpoint returned none
         */
        public List<VariantTypeOption> options() {
            return options;
        }

        /**
         * Parses a {@link VariantType} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link VariantType}, or empty when {@code obj} is {@code null}
         */
        static Optional<VariantType> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new VariantType(obj.getString("name"), VariantTypeOption.ofArray(obj.getJSONArray("options"))));
        }

        /**
         * Parses a list of {@link VariantType} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<VariantType> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<VariantType>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Wraps a variant type option of type {@code XWACatalogVariantTypeOption}.
     *
     * <p>Carries one selectable option value of a variant type together with its optional thumbnail
     * image.
     */
    public static final class VariantTypeOption {
        /**
         * Holds the option value.
         */
        private final String value;

        /**
         * Holds the linked thumbnail image, or {@code null} when absent.
         */
        private final Image thumbnailMedia;

        /**
         * Constructs a variant-type-option wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param value          the option value
         * @param thumbnailMedia the linked thumbnail image, or {@code null}
         */
        private VariantTypeOption(String value, Image thumbnailMedia) {
            this.value = value;
            this.thumbnailMedia = thumbnailMedia;
        }

        /**
         * Returns the option value.
         *
         * @return the option value, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> value() {
            return Optional.ofNullable(value);
        }

        /**
         * Returns the linked thumbnail image.
         *
         * @return the parsed thumbnail {@link Image}, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<Image> thumbnailMedia() {
            return Optional.ofNullable(thumbnailMedia);
        }

        /**
         * Parses a {@link VariantTypeOption} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link VariantTypeOption}, or empty when {@code obj} is {@code null}
         */
        static Optional<VariantTypeOption> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var thumbnailMedia = Image.of(obj.getJSONObject("thumbnail_media")).orElse(null);
            return Optional.of(new VariantTypeOption(obj.getString("value"), thumbnailMedia));
        }

        /**
         * Parses a list of {@link VariantTypeOption} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<VariantTypeOption> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<VariantTypeOption>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Wraps a variant property of type {@code XWACatalogVariantProperties}.
     *
     * <p>Carries one resolved name/value pair describing the selected variant of the product.
     */
    public static final class VariantProperty {
        /**
         * Holds the property name.
         */
        private final String name;

        /**
         * Holds the property value.
         */
        private final String value;

        /**
         * Constructs a variant-property wrapper from its parsed fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param name  the property name
         * @param value the property value
         */
        private VariantProperty(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Returns the property name.
         *
         * @return the property name, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> name() {
            return Optional.ofNullable(name);
        }

        /**
         * Returns the property value.
         *
         * @return the property value, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> value() {
            return Optional.ofNullable(value);
        }

        /**
         * Parses a {@link VariantProperty} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link VariantProperty}, or empty when {@code obj} is {@code null}
         */
        static Optional<VariantProperty> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            return Optional.of(new VariantProperty(obj.getString("name"), obj.getString("value")));
        }

        /**
         * Parses a list of {@link VariantProperty} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<VariantProperty> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<VariantProperty>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }
}

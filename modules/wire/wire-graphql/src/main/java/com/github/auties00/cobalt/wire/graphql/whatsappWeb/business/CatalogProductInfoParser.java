package com.github.auties00.cobalt.wire.graphql.whatsappWeb.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessItemAvailability;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProduct;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductBuilder;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductCompliance;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductComplianceBuilder;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductImage;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductImageBuilder;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductImporterAddress;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductImporterAddressBuilder;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductSalePrice;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductSalePriceBuilder;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductVideo;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessProductVideoBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Projects the rich catalog-product JSON stanza onto Cobalt's {@link BusinessProduct} model.
 *
 * <p>The catalog-management fetch operations and the public catalog read operations all return the
 * same product shape under slightly different field names; this stateless utility centralises the
 * projection so each response decoder maps a product stanza identically without duplicating the
 * defensive null handling. Both the catalog-management {@code XFBWhatsAppCatalogProductInfo} shape
 * (with {@code availability}) and the public {@code XWACatalogProduct} shape (with
 * {@code product_availability}) are accepted; field names that diverge are read in both spellings.
 *
 * @implNote This implementation tolerates a {@code url} or image url that does not parse as a
 * {@link URI} by leaving the corresponding field {@code null}, and a non-numeric {@code max_available}
 * by leaving the cart cap at {@code 0}. The {@code is_hidden} field is read both as a boolean and as
 * the WA Web enum literal {@code "ISHIDDEN_TRUE"}; the {@code status_info.status} verdict populates
 * {@link BusinessProduct#moderationStatus()} unchanged. The variant projection, the per-variant
 * availability tree, and the original-image dimensions are not carried onto {@link BusinessProduct}
 * because the domain model does not expose them.
 */
@WhatsAppWebModule(moduleName = "WAWebBizParseProductGraphql")
public final class CatalogProductInfoParser {
    /**
     * Prevents instantiation of this stateless utility class.
     *
     * <p>The class is a namespace for the {@code static} parsing helpers and holds no per-instance
     * state.
     *
     * @throws AssertionError always
     */
    private CatalogProductInfoParser() {
        throw new AssertionError("No CatalogProductInfoParser instances for you!");
    }

    /**
     * Parses an array of catalog-product JSON nodes into a list of {@link BusinessProduct} values.
     *
     * <p>Projects the {@code products} array returned by the catalog-management and public catalog read
     * operations; entries that cannot be projected (only {@code null} entries) are discarded.
     *
     * @param array the catalog-product array, possibly {@code null}
     * @return the parsed products, never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBizParseProductGraphql", exports = "parseProductGraphQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static List<BusinessProduct> parseProducts(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        var out = new ArrayList<BusinessProduct>(array.size());
        for (var i = 0; i < array.size(); i++) {
            parseProduct(array.getJSONObject(i)).ifPresent(out::add);
        }
        return List.copyOf(out);
    }

    /**
     * Parses a single catalog-product JSON stanza into a {@link BusinessProduct}.
     *
     * <p>Reads the product's scalar attributes, the image and video media lists, the optional sale
     * price, and the optional regulatory compliance block.
     *
     * @param obj the catalog-product stanza, possibly {@code null}
     * @return the parsed product, or empty when {@code obj} is {@code null} or lacks an {@code id}
     */
    public static Optional<BusinessProduct> parseProduct(JSONObject obj) {
        if (obj == null) {
            return Optional.empty();
        }
        var id = obj.getString("id");
        if (id == null) {
            return Optional.empty();
        }
        var availabilityRaw = obj.getString("availability");
        if (availabilityRaw == null) {
            availabilityRaw = obj.getString("product_availability");
        }
        var availability = parseAvailability(availabilityRaw);
        var media = obj.getJSONObject("media");
        var images = media == null ? List.<BusinessProductImage>of() : parseImages(media.getJSONArray("images"));
        var videos = media == null ? List.<BusinessProductVideo>of() : parseVideos(media.getJSONArray("videos"));
        var salePrice = parseSalePrice(obj.getJSONObject("sale_price"));
        var compliance = parseCompliance(obj.getJSONObject("compliance_info"));
        String moderationStatus = null;
        var canAppeal = false;
        var statusInfo = obj.getJSONObject("status_info");
        if (statusInfo != null) {
            moderationStatus = statusInfo.getString("status");
            canAppeal = Boolean.TRUE.equals(statusInfo.getBoolean("can_appeal"));
        }
        return Optional.of(new BusinessProductBuilder()
                .id(id)
                .invalid(false)
                .name(obj.getString("name"))
                .description(obj.getString("description"))
                .uri(parseUri(obj.getString("url")))
                .retailerId(obj.getString("retailer_id"))
                .availability(availability)
                .maxAvailable(parseInt(obj.getString("max_available")))
                .currency(obj.getString("currency"))
                .price(obj.getString("price"))
                .hidden(parseHidden(obj))
                .sanctioned(Boolean.TRUE.equals(obj.getBoolean("is_sanctioned")))
                .checkmark(false)
                .moderationStatus(moderationStatus)
                .canAppeal(canAppeal)
                .images(images)
                .videos(videos)
                .salePrice(salePrice)
                .compliance(compliance)
                .signedShimmedUri(parseUri(obj.getString("shimmed_url")))
                .complianceCategory(obj.getString("compliance_category"))
                .build());
    }

    /**
     * Maps the raw catalog availability label onto the Cobalt {@link BusinessItemAvailability}
     * constant.
     *
     * @implNote This implementation strips the WA Web {@code PRODUCTAVAILABILITY_} prefix and
     * lower-cases the remainder with underscores rewritten as spaces, matching the pretty-printed form
     * {@link BusinessItemAvailability#ofName(String)} expects; an unrecognized label collapses to
     * {@code null}.
     *
     * @param raw the raw availability label, possibly {@code null}
     * @return the matched constant, or {@code null} when the input is absent or unknown
     */
    private static BusinessItemAvailability parseAvailability(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        var stripped = raw.startsWith("PRODUCTAVAILABILITY_")
                ? raw.substring("PRODUCTAVAILABILITY_".length())
                : raw;
        var pretty = stripped.toLowerCase().replace('_', ' ');
        return BusinessItemAvailability.ofName(pretty).orElse(null);
    }

    /**
     * Reads the hidden flag from a catalog-product stanza in either of its two encodings.
     *
     * @implNote This implementation accepts both the boolean {@code is_hidden} field of the public
     * catalog shape and the WA Web enum literal {@code "ISHIDDEN_TRUE"} of the catalog-page shape; any
     * other value reads as not hidden.
     *
     * @param obj the catalog-product stanza, never {@code null}
     * @return {@code true} when the product is hidden, {@code false} otherwise
     */
    private static boolean parseHidden(JSONObject obj) {
        var bool = obj.getBoolean("is_hidden");
        if (bool != null) {
            return bool;
        }
        return "ISHIDDEN_TRUE".equals(obj.getString("is_hidden"));
    }

    /**
     * Parses the image array of a catalog-product media stanza into {@link BusinessProductImage} values.
     *
     * @implNote This implementation skips entries that lack the required id or either url, and entries
     * whose urls do not parse as a {@link URI}, because the domain image model requires all three.
     *
     * @param array the image array, possibly {@code null}
     * @return the parsed images, never {@code null}
     */
    private static List<BusinessProductImage> parseImages(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        var out = new ArrayList<BusinessProductImage>(array.size());
        for (var i = 0; i < array.size(); i++) {
            var image = array.getJSONObject(i);
            if (image == null) {
                continue;
            }
            var id = image.getString("id");
            var requested = parseUri(image.getString("request_image_url"));
            var full = parseUri(image.getString("original_image_url"));
            if (id == null || requested == null || full == null) {
                continue;
            }
            out.add(new BusinessProductImageBuilder()
                    .id(id)
                    .requestedUri(requested)
                    .fullUri(full)
                    .build());
        }
        return List.copyOf(out);
    }

    /**
     * Parses the video array of a catalog-product media stanza into {@link BusinessProductVideo} values.
     *
     * @implNote This implementation skips entries that lack the required id or either url, and entries
     * whose urls do not parse as a {@link URI}, because the domain video model requires all three.
     *
     * @param array the video array, possibly {@code null}
     * @return the parsed videos, never {@code null}
     */
    private static List<BusinessProductVideo> parseVideos(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        var out = new ArrayList<BusinessProductVideo>(array.size());
        for (var i = 0; i < array.size(); i++) {
            var video = array.getJSONObject(i);
            if (video == null) {
                continue;
            }
            var id = video.getString("id");
            var videoUri = parseUri(video.getString("original_video_url"));
            var thumbnailUri = parseUri(video.getString("thumbnail_url"));
            if (id == null || videoUri == null || thumbnailUri == null) {
                continue;
            }
            out.add(new BusinessProductVideoBuilder()
                    .id(id)
                    .videoUri(videoUri)
                    .thumbnailUri(thumbnailUri)
                    .build());
        }
        return List.copyOf(out);
    }

    /**
     * Parses the sale-price stanza of a catalog product into a sale-price block.
     *
     * @param obj the sale-price stanza, possibly {@code null}
     * @return the parsed sale price, or {@code null} when {@code obj} is {@code null} or lacks a price
     */
    private static BusinessProductSalePrice parseSalePrice(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        var price = obj.getString("price");
        if (price == null) {
            return null;
        }
        return new BusinessProductSalePriceBuilder()
                .price(price)
                .startDate(obj.getString("start_date"))
                .endDate(obj.getString("end_date"))
                .build();
    }

    /**
     * Parses the compliance-info stanza of a catalog product into a compliance block.
     *
     * @implNote This implementation drops the compliance block when the required country-of-origin
     * code is missing, and drops the nested importer address when its required first street line, city,
     * or country code is missing, because the domain models require those fields.
     *
     * @param obj the compliance-info stanza, possibly {@code null}
     * @return the parsed compliance block, or {@code null} when {@code obj} is {@code null} or lacks a
     *         country-of-origin code
     */
    private static BusinessProductCompliance parseCompliance(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        var countryCodeOrigin = obj.getString("country_code_origin");
        if (countryCodeOrigin == null) {
            return null;
        }
        var address = obj.getJSONObject("importer_address");
        var addressModel = parseImporterAddress(address);
        return new BusinessProductComplianceBuilder()
                .countryCodeOrigin(countryCodeOrigin)
                .importerName(obj.getString("importer_name"))
                .importerAddress(addressModel)
                .build();
    }

    /**
     * Parses the importer-address stanza of a compliance block into an importer-address model.
     *
     * @param obj the importer-address stanza, possibly {@code null}
     * @return the parsed importer address, or {@code null} when {@code obj} is {@code null} or lacks a
     *         required first street line, city, or country code
     */
    private static BusinessProductImporterAddress parseImporterAddress(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        var street1 = obj.getString("street1");
        var city = obj.getString("city");
        var countryCode = obj.getString("country_code");
        if (street1 == null || city == null || countryCode == null) {
            return null;
        }
        return new BusinessProductImporterAddressBuilder()
                .street1(street1)
                .street2(obj.getString("street2"))
                .postalCode(obj.getString("postal_code"))
                .city(city)
                .region(obj.getString("region"))
                .countryCode(countryCode)
                .build();
    }

    /**
     * Parses a non-empty url string into a {@link URI}.
     *
     * @param value the url string, possibly {@code null}
     * @return the parsed uri, or {@code null} when the input is missing or unparseable
     */
    private static URI parseUri(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return URI.create(value);
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    /**
     * Parses a non-empty decimal string into an {@code int}.
     *
     * @param value the value to parse, possibly {@code null}
     * @return the parsed integer, or {@code 0} when the input is missing or unparseable
     */
    private static int parseInt(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException _) {
            return 0;
        }
    }
}

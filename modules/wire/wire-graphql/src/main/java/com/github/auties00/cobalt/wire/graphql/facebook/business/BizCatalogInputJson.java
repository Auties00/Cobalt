package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.linked.business.catalog.CatalogComplianceInfo;
import com.github.auties00.cobalt.wire.linked.business.catalog.CatalogImporterAddress;
import com.github.auties00.cobalt.wire.linked.business.catalog.CatalogMedia;
import com.github.auties00.cobalt.wire.linked.business.catalog.CatalogProductInfo;

import java.util.List;

/**
 * Serializes the WhatsApp Business catalog product-info write model to its Facebook GraphQL JSON shape.
 *
 * <p>The model type is a pure, transport-agnostic domain holder carrying camelCase fields; the
 * snake_case GraphQL keys the Meta graph endpoint expects are a transport concern that lives here. The
 * helper writes the {@code product_info} object into a caller-provided {@link JSONWriter}, omitting any
 * field whose value is absent. It is shared by the catalog add-product and edit-product operations.
 */
final class BizCatalogInputJson {
    /**
     * Prevents instantiation of this static-helper holder.
     */
    private BizCatalogInputJson() {
        throw new AssertionError();
    }

    /**
     * Writes a catalog product-info write payload as the {@code {name, description, url, retailer_id,
     * currency, price, sale_price, is_hidden, media, compliance_info, compliance_category}} object.
     *
     * @param writer      the writer to append the object to
     * @param productInfo the product-info write payload to serialize
     */
    static void writeCatalogProductInfo(JSONWriter writer, CatalogProductInfo productInfo) {
        writer.startObject();
        writeStringField(writer, "name", productInfo.name().orElse(null));
        writeStringField(writer, "description", productInfo.description().orElse(null));
        writeStringField(writer, "url", productInfo.url().orElse(null));
        writeStringField(writer, "retailer_id", productInfo.retailerId().orElse(null));
        writeStringField(writer, "currency", productInfo.currency().orElse(null));
        writeStringField(writer, "price", productInfo.price().orElse(null));
        writeStringField(writer, "sale_price", productInfo.salePrice().orElse(null));
        writer.writeName("is_hidden");
        writer.writeColon();
        writer.writeBool(productInfo.hidden());
        productInfo.media().ifPresent(media -> {
            writer.writeName("media");
            writer.writeColon();
            writeCatalogMedia(writer, media);
        });
        productInfo.complianceInfo().ifPresent(info -> {
            writer.writeName("compliance_info");
            writer.writeColon();
            writeCatalogComplianceInfo(writer, info);
        });
        writeStringField(writer, "compliance_category", productInfo.complianceCategory().orElse(null));
        writer.endObject();
    }

    /**
     * Writes the product media as the {@code {image: [{url}], video: [{url}]}} object, writing each
     * media list only when non-empty.
     *
     * @param writer the writer to append the object to
     * @param media  the product media to serialize
     */
    private static void writeCatalogMedia(JSONWriter writer, CatalogMedia media) {
        writer.startObject();
        writeUrlArrayField(writer, "image", media.image());
        writeUrlArrayField(writer, "video", media.video());
        writer.endObject();
    }

    /**
     * Writes the compliance information as the {@code {country_code_origin, importer_name,
     * importer_address}} object.
     *
     * @param writer the writer to append the object to
     * @param info   the compliance information to serialize
     */
    private static void writeCatalogComplianceInfo(JSONWriter writer, CatalogComplianceInfo info) {
        writer.startObject();
        writeStringField(writer, "country_code_origin", info.countryCodeOrigin().orElse(null));
        writeStringField(writer, "importer_name", info.importerName().orElse(null));
        info.importerAddress().ifPresent(address -> {
            writer.writeName("importer_address");
            writer.writeColon();
            writeCatalogImporterAddress(writer, address);
        });
        writer.endObject();
    }

    /**
     * Writes the importer address as the {@code {street1, street2, postal_code, city, region,
     * country_code}} object.
     *
     * @param writer  the writer to append the object to
     * @param address the importer address to serialize
     */
    private static void writeCatalogImporterAddress(JSONWriter writer, CatalogImporterAddress address) {
        writer.startObject();
        writeStringField(writer, "street1", address.street1().orElse(null));
        writeStringField(writer, "street2", address.street2().orElse(null));
        writeStringField(writer, "postal_code", address.postalCode().orElse(null));
        writeStringField(writer, "city", address.city().orElse(null));
        writeStringField(writer, "region", address.region().orElse(null));
        writeStringField(writer, "country_code", address.countryCode().orElse(null));
        writer.endObject();
    }

    /**
     * Writes a named field as a JSON array of {@code {url}} objects only when {@code urls} is non-empty.
     *
     * @param writer the writer to append to
     * @param name   the JSON key
     * @param urls   the media URLs to serialize, in order
     */
    private static void writeUrlArrayField(JSONWriter writer, String name, List<String> urls) {
        if (urls.isEmpty()) {
            return;
        }
        writer.writeName(name);
        writer.writeColon();
        writer.startArray();
        for (var i = 0; i < urls.size(); i++) {
            if (i > 0) {
                writer.writeComma();
            }
            writer.startObject();
            writer.writeName("url");
            writer.writeColon();
            writer.writeString(urls.get(i));
            writer.endObject();
        }
        writer.endArray();
    }

    /**
     * Writes a string field only when {@code value} is non-null.
     *
     * @param writer the writer to append to
     * @param name   the JSON key
     * @param value  the value, or {@code null} to omit the field
     */
    private static void writeStringField(JSONWriter writer, String name, String value) {
        if (value != null) {
            writer.writeName(name);
            writer.writeColon();
            writer.writeString(value);
        }
    }
}

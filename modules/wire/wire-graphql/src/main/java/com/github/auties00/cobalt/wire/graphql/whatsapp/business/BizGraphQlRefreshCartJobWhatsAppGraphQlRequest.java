package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the graph.whatsapp.com GraphQL query that re-fetches the current state of a WhatsApp Business shopping cart.
 *
 * <p>The single {@code request} GraphQL variable is the refresh-cart request object. WhatsApp Web's
 * {@code WAWebBizRefreshCartJob.refreshCart} fills it with a {@code cart} sub-object carrying the
 * business {@link Jid}, the list of retailer product ids in the cart, the requested image dimensions,
 * an opaque direct-connection encrypted-info token, and the {@code variant_info_fields} projection
 * selector (always {@code "variant_properties"} on WhatsApp Web). The graph.whatsapp.com endpoint returns the refreshed
 * product list and price breakdown under {@code xwa_checkout_refresh_cart}; the reply is consumed
 * through {@link BizGraphQlRefreshCartJobWhatsAppGraphQlResponse}.
 *
 * @implNote This implementation sets {@link #DOC_ID} to the literal document name
 * {@code "WAWebBizGraphQLRefreshCartJobQuery"} rather than a numeric persisted-query id: the compiled
 * {@code .graphql} module of snapshot {@code 1040120866} hardcodes {@code params.id} to that string
 * and ships no {@code _facebookRelayOperation} indirection module, so no numeric id exists to use.
 *
 * @see BizGraphQlRefreshCartJobWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizGraphQLRefreshCartJobQuery")
public final class BizGraphQlRefreshCartJobWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body. The compiled document
     * exposes its name rather than a numeric id, so the document name itself is the persisted-query
     * identifier (see the class {@code @implNote}).
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGraphQLRefreshCartJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26249779981350522";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGraphQLRefreshCartJobQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizGraphQLRefreshCartJobQuery";

    /**
     * The {@code cart.jid} field naming the business account whose cart is being refreshed, or
     * {@code null} to omit it.
     */
    private final Jid jid;

    /**
     * The {@code cart.products} retailer product ids in the cart, written as
     * {@code [{"id": <productId>}, ...]}, never {@code null}.
     */
    private final List<String> productIds;

    /**
     * The {@code cart.image_dimensions.width} requested image width, or {@code null} to omit the
     * {@code image_dimensions} object.
     */
    private final Integer imageWidth;

    /**
     * The {@code cart.image_dimensions.height} requested image height, or {@code null} to omit the
     * {@code image_dimensions} object.
     */
    private final Integer imageHeight;

    /**
     * The {@code cart.direct_connection_encrypted_info} opaque direct-connection token, or
     * {@code null} to omit it.
     *
     * <p>An opaque server-provided blob threaded through the direct-connection retry path; its
     * internal structure is not modelled.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * The {@code cart.variant_info_fields} projection selector, or {@code null} to omit it.
     *
     * <p>WhatsApp Web always sets this to {@code "variant_properties"}.
     */
    private final String variantInfoFields;

    /**
     * Constructs a refresh-cart query request describing the cart to re-fetch.
     *
     * <p>Each value populates the matching field of the {@code cart} sub-object of the {@code request}
     * GraphQL variable. A {@code null} {@code jid}, {@code directConnectionEncryptedInfo}, or
     * {@code variantInfoFields} omits that field; the {@code image_dimensions} object is omitted
     * unless both {@code imageWidth} and {@code imageHeight} are non-null; a {@code null}
     * {@code productIds} is treated as an empty list.
     *
     * @param jid                           the business account {@link Jid} whose cart is being
     *                                      refreshed, or {@code null} to omit the field
     * @param productIds                    the retailer product ids in the cart, or {@code null} for
     *                                      none
     * @param imageWidth                    the requested image width, or {@code null} to omit the
     *                                      {@code image_dimensions} object
     * @param imageHeight                   the requested image height, or {@code null} to omit the
     *                                      {@code image_dimensions} object
     * @param directConnectionEncryptedInfo the opaque direct-connection token, or {@code null} to
     *                                      omit the field
     * @param variantInfoFields             the variant-info projection selector, or {@code null} to
     *                                      omit the field
     */
    public BizGraphQlRefreshCartJobWhatsAppGraphQlRequest(Jid jid, List<String> productIds, Integer imageWidth, Integer imageHeight, String directConnectionEncryptedInfo, String variantInfoFields) {
        this.jid = jid;
        this.productIds = productIds == null ? List.of() : List.copyOf(productIds);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
        this.variantInfoFields = variantInfoFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits
     * {@code {"request": {"cart": {"jid": <jid>, "products": [{"id": <id>}...],
     * "image_dimensions": {"width": <w>, "height": <h>},
     * "direct_connection_encrypted_info": <info>, "variant_info_fields": <fields>}}}}, writing the
     * {@code jid}, {@code direct_connection_encrypted_info} and {@code variant_info_fields} fields
     * only when non-null, omitting {@code image_dimensions} unless both dimensions are present, and
     * always writing the {@code products} array (empty when no ids were supplied).
     */
    @WhatsAppWebExport(moduleName = "WAWebBizRefreshCartJob", exports = "refreshCart",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            writer.writeName("cart");
            writer.writeColon();
            writer.startObject();
            if (jid != null) {
                writer.writeName("jid");
                writer.writeColon();
                writer.writeString(jid.toString());
            }

            writer.writeName("products");
            writer.writeColon();
            writer.startArray();
            for (var productId : productIds) {
                writer.startObject();
                writer.writeName("id");
                writer.writeColon();
                writer.writeString(productId);
                writer.endObject();
            }
            writer.endArray();

            if (imageWidth != null && imageHeight != null) {
                writer.writeName("image_dimensions");
                writer.writeColon();
                writer.startObject();
                writer.writeName("width");
                writer.writeColon();
                writer.writeInt32(imageWidth);
                writer.writeName("height");
                writer.writeColon();
                writer.writeInt32(imageHeight);
                writer.endObject();
            }

            if (directConnectionEncryptedInfo != null) {
                writer.writeName("direct_connection_encrypted_info");
                writer.writeColon();
                writer.writeString(directConnectionEncryptedInfo);
            }

            if (variantInfoFields != null) {
                writer.writeName("variant_info_fields");
                writer.writeColon();
                writer.writeString(variantInfoFields);
            }
            writer.endObject();
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WhatsAppGraphQlEnvironment environment() {
        return WhatsAppGraphQlEnvironment.CATALOG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> accessToken() {
        return Optional.empty();
    }
}

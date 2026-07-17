package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogPublicKey;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogPublicKeyBuilder;

import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the get-public-key query built by
 * {@link GraphQlProductCatalogGetPublicKeyJobWhatsAppGraphQlRequest} into a {@link BusinessCatalogPublicKey}.
 *
 * <p>Reads the linked {@code xwa_product_catalog_get_public_key} field and projects its PEM
 * certificate and the linked public-key-and-signature pair onto the Cobalt domain model.
 *
 * @see GraphQlProductCatalogGetPublicKeyJobWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebGraphQLProductCatalogGetPublicKeyJobQuery")
public final class GraphQlProductCatalogGetPublicKeyJobWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed catalog public key.
     */
    private final BusinessCatalogPublicKey publicKey;

    /**
     * Constructs a response wrapping the parsed catalog public key.
     *
     * <p>Reserved for the static parser.
     *
     * @param publicKey the parsed catalog public key, or {@code null} when the graph.whatsapp.com endpoint omitted the field
     */
    private GraphQlProductCatalogGetPublicKeyJobWhatsAppGraphQlResponse(BusinessCatalogPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_product_catalog_get_public_key}, its
     * {@code public_key_certificate_pem} scalar, and its {@code public_key_with_signature} child, and
     * projects them onto a {@link BusinessCatalogPublicKey}; the returned {@link Optional} is empty when
     * {@code data} or the public-key field is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the public-key field is missing
     */
    @WhatsAppWebExport(moduleName = "WAWebGraphQLProductCatalogGetPublicKeyJob", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<GraphQlProductCatalogGetPublicKeyJobWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xwa_product_catalog_get_public_key");
        if (root == null) {
            return Optional.empty();
        }
        String publicKeyPem = null;
        String publicKeySignature = null;
        var pair = root.getJSONObject("public_key_with_signature");
        if (pair != null) {
            publicKeyPem = pair.getString("public_key_pem");
            publicKeySignature = pair.getString("public_key_signature");
        }
        var publicKey = new BusinessCatalogPublicKeyBuilder()
                .publicKeyPem(publicKeyPem)
                .publicKeySignature(publicKeySignature)
                .certificatePem(root.getString("public_key_certificate_pem"))
                .build();
        return Optional.of(new GraphQlProductCatalogGetPublicKeyJobWhatsAppGraphQlResponse(publicKey));
    }

    /**
     * Returns the parsed catalog public key.
     *
     * <p>The returned {@link BusinessCatalogPublicKey} carries the merchant's public key, its
     * signature, and the certificate it was extracted from.
     *
     * @return the parsed catalog public key, never {@code null}
     */
    public BusinessCatalogPublicKey publicKey() {
        return publicKey;
    }
}

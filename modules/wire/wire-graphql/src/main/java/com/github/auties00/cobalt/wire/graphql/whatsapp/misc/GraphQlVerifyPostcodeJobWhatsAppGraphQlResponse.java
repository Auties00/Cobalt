package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.postcode.BusinessPostcodeVerification;
import com.github.auties00.cobalt.wire.linked.business.postcode.BusinessPostcodeVerificationBuilder;
import com.github.auties00.cobalt.wire.linked.business.postcode.BusinessPostcodeVerificationResult;

import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the verify-postcode query built by
 * {@link GraphQlVerifyPostcodeJobWhatsAppGraphQlRequest} into a {@link BusinessPostcodeVerification}.
 *
 * <p>Reads the linked root {@code xwa_product_catalog_get_verify_postcode} and flattens its nested
 * {@code postcode_verification_result -> result_code / encrypted_location_name} chain onto the
 * Cobalt domain model: the postcode-verification verdict and, on success, the encrypted
 * servicing-location name.
 *
 * @see GraphQlVerifyPostcodeJobWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebGraphQLVerifyPostcodeJobQuery")
public final class GraphQlVerifyPostcodeJobWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed postcode verification.
     */
    private final BusinessPostcodeVerification verification;

    /**
     * Constructs a response wrapping the parsed postcode verification.
     *
     * <p>Reserved for the static parser.
     *
     * @param verification the parsed postcode verification, or {@code null} when the graph.whatsapp.com endpoint omitted
     *                     the field
     */
    private GraphQlVerifyPostcodeJobWhatsAppGraphQlResponse(BusinessPostcodeVerification verification) {
        this.verification = verification;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_product_catalog_get_verify_postcode} and projects it onto
     * a {@link BusinessPostcodeVerification}; the returned {@link Optional} is empty when
     * {@code data} or the verification object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the verification object is missing
     */
    public static Optional<GraphQlVerifyPostcodeJobWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa_product_catalog_get_verify_postcode");
        if (root == null) {
            return Optional.empty();
        }

        var inner = root.getJSONObject("postcode_verification_result");
        if (inner == null) {
            return Optional.empty();
        }

        var result = BusinessPostcodeVerificationResult.ofRelayResultCode(inner.getString("result_code"))
                .orElse(BusinessPostcodeVerificationResult.INVALID_POSTCODE);
        var verification = new BusinessPostcodeVerificationBuilder()
                .result(result)
                .encryptedLocationName(inner.getString("encrypted_location_name"))
                .build();
        return Optional.of(new GraphQlVerifyPostcodeJobWhatsAppGraphQlResponse(verification));
    }

    /**
     * Returns the parsed postcode verification.
     *
     * @return the parsed {@link BusinessPostcodeVerification}, never {@code null}
     */
    public BusinessPostcodeVerification verification() {
        return verification;
    }
}

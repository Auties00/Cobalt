package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogMutationResult;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogMutationResultBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the delete-product mutation built by
 * {@link BizCatalogManagementDeleteProductFacebookGraphQlRequest} into a {@link BusinessCatalogMutationResult}.
 *
 * <p>Reads the linked root {@code xfb_whatsapp_catalog_delete_product} and projects its
 * {@code deleted_count} scalar onto the shared status-only outcome: the mutation is reported successful
 * when the Meta graph endpoint removed at least one product.
 *
 * @see BizCatalogManagementDeleteProductFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementDeleteProductMutation")
public final class BizCatalogManagementDeleteProductFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed deletion outcome.
     */
    private final BusinessCatalogMutationResult result;

    /**
     * Constructs a response wrapping the parsed deletion outcome.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the parsed deletion outcome, never {@code null}
     */
    private BizCatalogManagementDeleteProductFacebookGraphQlResponse(BusinessCatalogMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xfb_whatsapp_catalog_delete_product} and projects its
     * {@code deleted_count} onto a {@link BusinessCatalogMutationResult}; the returned {@link Optional}
     * is empty when {@code data} or the root is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null} or the root is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementDeleteProductMutation", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizCatalogManagementDeleteProductFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xfb_whatsapp_catalog_delete_product");
        if (root == null) {
            return Optional.empty();
        }
        var deletedCount = root.getLong("deleted_count");
        var result = new BusinessCatalogMutationResultBuilder()
                .success(deletedCount != null && deletedCount > 0)
                .build();
        return Optional.of(new BizCatalogManagementDeleteProductFacebookGraphQlResponse(result));
    }

    /**
     * Returns the parsed deletion outcome.
     *
     * @return the parsed {@link BusinessCatalogMutationResult}, never {@code null}
     */
    public BusinessCatalogMutationResult result() {
        return result;
    }
}

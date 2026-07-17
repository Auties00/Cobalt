package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastQuota;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastQuotaBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the broadcast-quota query built by
 * {@link BizBroadcastQuotaFacebookGraphQlRequest} into a {@link BusinessBroadcastQuota}.
 *
 * <p>Reads the linked {@code xwa_smb_mm_quota} root and projects it onto the Cobalt domain model.
 * The compiled WhatsApp Web document of snapshot {@code 1040120866} selects only the
 * {@code __typename} discriminator on that root, so the model carries only the type marker.
 *
 * @see BizBroadcastQuotaFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizBroadcastQuotaQuery")
public final class BizBroadcastQuotaFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed quota.
     */
    private final BusinessBroadcastQuota quota;

    /**
     * Constructs a response wrapping the parsed quota.
     *
     * <p>Reserved for the static parser.
     *
     * @param quota the parsed quota, or {@code null} when the Meta graph endpoint omitted the field
     */
    private BizBroadcastQuotaFacebookGraphQlResponse(BusinessBroadcastQuota quota) {
        this.quota = quota;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_smb_mm_quota} and projects it onto a
     * {@link BusinessBroadcastQuota}; the returned {@link Optional} is empty when {@code data} is
     * {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizBroadcastQuotaFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var quota = readQuota(data.getJSONObject("xwa_smb_mm_quota"));
        return Optional.of(new BizBroadcastQuotaFacebookGraphQlResponse(quota));
    }

    /**
     * Returns the parsed quota.
     *
     * @return the parsed {@link BusinessBroadcastQuota}, or empty when the Meta graph endpoint omitted the field
     */
    public Optional<BusinessBroadcastQuota> quota() {
        return Optional.ofNullable(quota);
    }

    /**
     * Projects the {@code xwa_smb_mm_quota} sub-object onto a {@link BusinessBroadcastQuota}.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected quota, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastQuota readQuota(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastQuotaBuilder()
                .typename(node.getString("__typename"))
                .build();
    }
}

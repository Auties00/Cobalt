package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastTargetInfo;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastTargetInfoBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the resolve-business-info mutation built by
 * {@link BizBroadcastBusinessInfoFacebookGraphQlRequest} into a {@link BusinessBroadcastTargetInfo}.
 *
 * <p>Reads the linked {@code xwa_smb_mm_business_info} root and projects the four resolved
 * linked-account entities (Meta business, payment account, advertising account, Facebook page)
 * onto the Cobalt domain model. Each entity is unwrapped from its {@code id}-only sub-object so the
 * model carries only the four identifiers.
 *
 * @see BizBroadcastBusinessInfoFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizBroadcastBusinessInfoMutation")
public final class BizBroadcastBusinessInfoFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed target info.
     */
    private final BusinessBroadcastTargetInfo businessInfo;

    /**
     * Constructs a response wrapping the parsed target info.
     *
     * <p>Reserved for the static parser.
     *
     * @param businessInfo the parsed target info, or {@code null} when the Meta graph endpoint omitted the field
     */
    private BizBroadcastBusinessInfoFacebookGraphQlResponse(BusinessBroadcastTargetInfo businessInfo) {
        this.businessInfo = businessInfo;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_smb_mm_business_info} and projects it onto a
     * {@link BusinessBroadcastTargetInfo}; the returned {@link Optional} is empty when {@code data} is
     * {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizBroadcastBusinessInfoFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var businessInfo = readBusinessInfo(data.getJSONObject("xwa_smb_mm_business_info"));
        return Optional.of(new BizBroadcastBusinessInfoFacebookGraphQlResponse(businessInfo));
    }

    /**
     * Returns the parsed target info.
     *
     * @return the parsed {@link BusinessBroadcastTargetInfo}, or empty when the Meta graph endpoint omitted the
     *         field
     */
    public Optional<BusinessBroadcastTargetInfo> businessInfo() {
        return Optional.ofNullable(businessInfo);
    }

    /**
     * Projects the {@code xwa_smb_mm_business_info} sub-object onto a
     * {@link BusinessBroadcastTargetInfo}, unwrapping each {@code {id}} sub-object to its identifier.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the projected target info, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessBroadcastTargetInfo readBusinessInfo(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessBroadcastTargetInfoBuilder()
                .businessId(readId(node.getJSONObject("business")))
                .businessPaymentAccountId(readId(node.getJSONObject("business_payment_account")))
                .adAccountId(readId(node.getJSONObject("ad_account")))
                .pageId(readId(node.getJSONObject("page")))
                .build();
    }

    /**
     * Extracts the {@code id} scalar from an entity sub-object.
     *
     * @param node the JSON object to read, possibly {@code null}
     * @return the entity identifier, or {@code null} when {@code stanza} is {@code null}
     */
    private static String readId(JSONObject node) {
        if (node == null) {
            return null;
        }
        return node.getString("id");
    }
}

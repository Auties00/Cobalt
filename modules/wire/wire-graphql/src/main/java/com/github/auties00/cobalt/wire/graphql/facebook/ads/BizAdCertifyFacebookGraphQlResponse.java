package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResultBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the advertiser self-certification mutation built by
 * {@link BizAdCertifyFacebookGraphQlRequest} into a {@link BusinessAdMutationResult}.
 *
 * <p>Projects the linked {@code ads_integrity_self_certification_certify} field onto the shared
 * mutation-result shape. The field carries the {@code certified_user_name} the certification was
 * recorded against; its presence is taken as success and the recorded name is reported as the
 * affected id.
 *
 * @see BizAdCertifyFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCertifyMutation")
public final class BizAdCertifyFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected mutation result, or {@code null} when the relay omitted the field.
     */
    private final BusinessAdMutationResult result;

    /**
     * Constructs a response wrapping the projected mutation result.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the projected mutation result, or {@code null} when the relay omitted the field
     */
    private BizAdCertifyFacebookGraphQlResponse(BusinessAdMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code ads_integrity_self_certification_certify} field onto a {@link BusinessAdMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCertifyFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("ads_integrity_self_certification_certify");
        if (node == null) {
            return Optional.of(new BizAdCertifyFacebookGraphQlResponse(null));
        }

        var certifiedUserName = node.getString("certified_user_name");
        var result = new BusinessAdMutationResultBuilder()
                .success(true)
                .affectedIds(certifiedUserName == null ? List.of() : List.of(certifiedUserName))
                .build();
        return Optional.of(new BizAdCertifyFacebookGraphQlResponse(result));
    }

    /**
     * Returns the projected mutation result.
     *
     * @return the projected {@link BusinessAdMutationResult}, or empty when the relay omitted the field
     */
    public Optional<BusinessAdMutationResult> result() {
        return Optional.ofNullable(result);
    }
}

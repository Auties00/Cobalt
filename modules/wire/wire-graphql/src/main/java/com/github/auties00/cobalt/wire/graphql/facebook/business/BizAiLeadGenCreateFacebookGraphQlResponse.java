package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiLeadGenForm;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiLeadGenFormBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the create-lead-gen-flow mutation built by
 * {@link BizAiLeadGenCreateFacebookGraphQlRequest} into a {@link BusinessAiLeadGenForm}.
 *
 * <p>The Meta graph endpoint confirms the creation with a single {@code success} scalar under
 * {@code xfb_meta_ai_biz_agent_wa_create_lead_gen_flow} without echoing the flow's contents, so the
 * projected {@link BusinessAiLeadGenForm} is present only when the Meta graph endpoint reported success and carries
 * no fields beyond representing the created flow.
 *
 * @see BizAiLeadGenCreateFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiLeadGenCreateMutation")
public final class BizAiLeadGenCreateFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected lead-capture flow, or {@code null} when the Meta graph endpoint omitted the
     * field or reported failure.
     */
    private final BusinessAiLeadGenForm form;

    /**
     * Constructs a response wrapping the projected lead-capture flow.
     *
     * <p>Reserved for the static parser.
     *
     * @param form the projected lead-capture flow, or {@code null} when the Meta graph endpoint omitted
     *             the field or reported failure
     */
    private BizAiLeadGenCreateFacebookGraphQlResponse(BusinessAiLeadGenForm form) {
        this.form = form;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * creation outcome onto a {@link BusinessAiLeadGenForm}.
     *
     * <p>Reads the linked root {@code xfb_meta_ai_biz_agent_wa_create_lead_gen_flow}; the returned
     * {@link Optional} is empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiLeadGenCreateFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var result = data.getJSONObject("xfb_meta_ai_biz_agent_wa_create_lead_gen_flow");
        var success = result != null && Boolean.TRUE.equals(result.getBoolean("success"));
        var form = success ? new BusinessAiLeadGenFormBuilder().build() : null;
        return Optional.of(new BizAiLeadGenCreateFacebookGraphQlResponse(form));
    }

    /**
     * Returns the projected lead-capture flow created by the Meta graph endpoint.
     *
     * @return the projected {@link BusinessAiLeadGenForm}, or empty when the Meta graph endpoint reported failure or
     *         omitted the field
     */
    public Optional<BusinessAiLeadGenForm> form() {
        return Optional.ofNullable(form);
    }
}

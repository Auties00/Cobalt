package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResultBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the AI auto-reply control mutation built by
 * {@link AiAgentAutoReplyControlFacebookGraphQlRequest} into a {@link BusinessAiMutationResult}.
 *
 * <p>Reads the linked {@code xfb_whatsapp_smb_maiba_status_update} field, whose single scalar
 * {@code success} reports whether the AI control state was applied, and projects it onto the shared
 * AI-agent mutation result. WhatsApp Web treats the mutation as successful only when {@code success}
 * is exactly {@code true}.
 *
 * @see AiAgentAutoReplyControlFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebAiAgentAutoReplyControlMutation")
public final class AiAgentAutoReplyControlFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed mutation result.
     */
    private final BusinessAiMutationResult result;

    /**
     * Constructs a response wrapping the parsed mutation result.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the parsed mutation result, or {@code null} when the Meta graph endpoint omitted the field
     */
    private AiAgentAutoReplyControlFacebookGraphQlResponse(BusinessAiMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xfb_whatsapp_smb_maiba_status_update} and projects its
     * {@code success} scalar onto a {@link BusinessAiMutationResult}; the returned {@link Optional} is
     * empty when {@code data} or the status-update object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the status-update object is missing
     */
    public static Optional<AiAgentAutoReplyControlFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var statusUpdate = data.getJSONObject("xfb_whatsapp_smb_maiba_status_update");
        if (statusUpdate == null) {
            return Optional.empty();
        }

        var success = statusUpdate.getBoolean("success");
        var result = new BusinessAiMutationResultBuilder()
                .success(success != null && success)
                .build();
        return Optional.of(new AiAgentAutoReplyControlFacebookGraphQlResponse(result));
    }

    /**
     * Returns the parsed mutation result.
     *
     * @return the parsed {@link BusinessAiMutationResult}, never {@code null}
     */
    public BusinessAiMutationResult result() {
        return result;
    }
}

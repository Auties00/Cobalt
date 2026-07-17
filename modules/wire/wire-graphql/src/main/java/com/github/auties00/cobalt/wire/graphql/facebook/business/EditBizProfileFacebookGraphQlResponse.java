package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the edit-business-profile mutation built by
 * {@link EditBizProfileFacebookGraphQlRequest}.
 *
 * <p>Exposes the single scalar field {@code edit_wa_web_biz_profile}, the mutation outcome the Meta graph endpoint
 * returns for the edited business account.
 *
 * @see EditBizProfileFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebEditBizProfileMutation")
public final class EditBizProfileFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the mutation outcome returned under {@code edit_wa_web_biz_profile}.
     */
    private final String editResult;

    /**
     * Constructs a response wrapping the parsed mutation outcome.
     *
     * <p>Reserved for the static parser.
     *
     * @param editResult the mutation outcome, or {@code null} when the Meta graph endpoint omitted the field
     */
    private EditBizProfileFacebookGraphQlResponse(String editResult) {
        this.editResult = editResult;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the scalar root {@code edit_wa_web_biz_profile}; the returned {@link Optional} is
     * empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<EditBizProfileFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var editResult = data.getString("edit_wa_web_biz_profile");
        return Optional.of(new EditBizProfileFacebookGraphQlResponse(editResult));
    }

    /**
     * Returns the mutation outcome.
     *
     * @return the mutation outcome, or empty when the Meta graph endpoint omitted the field
     */
    public Optional<String> editResult() {
        return Optional.ofNullable(editResult);
    }
}

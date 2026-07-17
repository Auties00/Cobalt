package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResultBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the delete-saved-audience mutation built by
 * {@link BizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteFacebookGraphQlRequest}
 * into a {@link BusinessAdMutationResult}.
 *
 * <p>Projects the scalar {@code delete_saved_audience} result returned at the top level of the GraphQL
 * {@code data} object onto the {@link BusinessAdMutationResult} model: a non-null result is treated as
 * a successful deletion and echoed through {@link BusinessAdMutationResult#affectedIds()}, while a
 * missing result is treated as a failure.
 *
 * @implNote This implementation reads {@code delete_saved_audience} as a scalar because the compiled
 * {@code WAWebBizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteMutation.graphql}
 * document of snapshot {@code 1040120866} declares it as a typeless {@code ScalarField}, and no
 * in-bundle caller narrows the scalar to a boolean or an id; WhatsApp Web treats a non-null result as
 * a successful deletion.
 *
 * @see BizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteMutation")
public final class BizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected deletion result, or {@code null} when the relay omitted the field.
     */
    private final BusinessAdMutationResult result;

    /**
     * Constructs a response wrapping the projected deletion result.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the projected deletion result, or {@code null} when the relay omitted the field
     */
    private BizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteFacebookGraphQlResponse(BusinessAdMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the scalar
     * {@code delete_saved_audience} result onto a {@link BusinessAdMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var deleted = data.getString("delete_saved_audience");
        var result = new BusinessAdMutationResultBuilder()
                .success(deleted != null)
                .affectedIds(deleted == null ? List.of() : List.of(deleted))
                .build();
        return Optional.of(new BizAdCreationTargetingModalDeleteAudienceConfirmationModal_SavedAudienceDeleteFacebookGraphQlResponse(result));
    }

    /**
     * Returns the projected deletion result.
     *
     * @return the projected {@link BusinessAdMutationResult}, or empty when the relay omitted the field
     */
    public Optional<BusinessAdMutationResult> result() {
        return Optional.ofNullable(result);
    }
}

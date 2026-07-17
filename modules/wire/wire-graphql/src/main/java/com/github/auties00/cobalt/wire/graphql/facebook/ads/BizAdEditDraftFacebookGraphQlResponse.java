package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdDraft;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdDraftBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the edit-ad-draft mutation built by
 * {@link BizAdEditDraftFacebookGraphQlRequest} into a {@link BusinessAdDraft}.
 *
 * <p>Projects the linked {@code edit_ads_ctwa_draft} field, whose {@code id} scalar is the identifier
 * of the edited draft, onto the {@link BusinessAdDraft} model.
 *
 * @see BizAdEditDraftFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdEditDraftMutation")
public final class BizAdEditDraftFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected draft, or {@code null} when the relay omitted the field.
     */
    private final BusinessAdDraft draft;

    /**
     * Constructs a response wrapping the projected draft.
     *
     * <p>Reserved for the static parser.
     *
     * @param draft the projected draft, or {@code null} when the relay omitted the field
     */
    private BizAdEditDraftFacebookGraphQlResponse(BusinessAdDraft draft) {
        this.draft = draft;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code edit_ads_ctwa_draft} field onto a {@link BusinessAdDraft}.
     *
     * <p>Reads the linked root {@code edit_ads_ctwa_draft}; the returned {@link Optional} is empty
     * when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdEditDraftFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("edit_ads_ctwa_draft");
        if (node == null) {
            return Optional.of(new BizAdEditDraftFacebookGraphQlResponse(null));
        }

        var draft = new BusinessAdDraftBuilder()
                .id(node.getString("id"))
                .build();
        return Optional.of(new BizAdEditDraftFacebookGraphQlResponse(draft));
    }

    /**
     * Returns the projected edited draft.
     *
     * @return the projected {@link BusinessAdDraft}, or empty when the relay omitted the field
     */
    public Optional<BusinessAdDraft> draft() {
        return Optional.ofNullable(draft);
    }
}

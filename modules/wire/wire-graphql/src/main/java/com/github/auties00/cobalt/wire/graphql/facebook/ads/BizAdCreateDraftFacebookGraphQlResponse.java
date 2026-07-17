package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdDraft;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdDraftBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the create-ad-draft mutation built by
 * {@link BizAdCreateDraftFacebookGraphQlRequest} into a {@link BusinessAdDraft}.
 *
 * <p>Projects the linked {@code create_ads_ctwa_draft} field, whose {@code id} scalar is the
 * identifier of the newly created draft, onto the {@link BusinessAdDraft} model.
 *
 * @see BizAdCreateDraftFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreateDraftMutation")
public final class BizAdCreateDraftFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
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
    private BizAdCreateDraftFacebookGraphQlResponse(BusinessAdDraft draft) {
        this.draft = draft;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code create_ads_ctwa_draft} field onto a {@link BusinessAdDraft}.
     *
     * <p>Reads the linked root {@code create_ads_ctwa_draft}; the returned {@link Optional} is empty
     * when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreateDraftFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("create_ads_ctwa_draft");
        if (node == null) {
            return Optional.of(new BizAdCreateDraftFacebookGraphQlResponse(null));
        }

        var draft = new BusinessAdDraftBuilder()
                .id(node.getString("id"))
                .build();
        return Optional.of(new BizAdCreateDraftFacebookGraphQlResponse(draft));
    }

    /**
     * Returns the projected draft.
     *
     * @return the projected {@link BusinessAdDraft}, or empty when the relay omitted the field
     */
    public Optional<BusinessAdDraft> draft() {
        return Optional.ofNullable(draft);
    }
}

package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.linking.BusinessLinkedAdAccount;
import com.github.auties00.cobalt.wire.linked.business.linking.BusinessLinkedAdAccountBuilder;
import com.github.auties00.cobalt.wire.linked.business.linking.BusinessLinkedAdAccounts;
import com.github.auties00.cobalt.wire.linked.business.linking.BusinessLinkedAdAccountsBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the linked-accounts query built by
 * {@link LinkedAccountsGqlFacebookGraphQlRequest} into a {@link BusinessLinkedAdAccounts}.
 *
 * <p>Reads the linked root {@code xfb_wa_biz_linked_accounts} and flattens its nested
 * {@code linked_accounts -> fb_page / wa_ad_identity} chain onto the Cobalt domain model: the
 * linked Facebook page and the linked WhatsApp ad identity, each with its identifier and
 * click-to-WhatsApp advertising state.
 *
 * @see LinkedAccountsGqlFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebLinkedAccountsGQLQuery")
public final class LinkedAccountsGqlFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed linked-accounts view.
     */
    private final BusinessLinkedAdAccounts accounts;

    /**
     * Constructs a response wrapping the parsed linked-accounts view.
     *
     * <p>Reserved for the static parser.
     *
     * @param accounts the parsed linked-accounts view, or {@code null} when the Meta graph endpoint
     *                 omitted the field
     */
    private LinkedAccountsGqlFacebookGraphQlResponse(BusinessLinkedAdAccounts accounts) {
        this.accounts = accounts;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xfb_wa_biz_linked_accounts} and projects its nested
     * {@code linked_accounts} sub-object onto a {@link BusinessLinkedAdAccounts}; the returned
     * {@link Optional} is empty when {@code data}, the root, or the linked-accounts object is
     * missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data}, the root, or the linked-accounts
     *         object is missing
     */
    public static Optional<LinkedAccountsGqlFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xfb_wa_biz_linked_accounts");
        if (root == null) {
            return Optional.empty();
        }

        var linked = root.getJSONObject("linked_accounts");
        if (linked == null) {
            return Optional.empty();
        }

        var accounts = new BusinessLinkedAdAccountsBuilder()
                .facebookPage(parseAccount(linked.getJSONObject("fb_page")))
                .whatsAppAdIdentity(parseAccount(linked.getJSONObject("wa_ad_identity")))
                .build();
        return Optional.of(new LinkedAccountsGqlFacebookGraphQlResponse(accounts));
    }

    /**
     * Projects an {@code fb_page} or {@code wa_ad_identity} sub-object onto a
     * {@link BusinessLinkedAdAccount}.
     *
     * @param obj the JSON object to project
     * @return the projected account, or {@code null} when {@code obj} is {@code null}
     */
    private static BusinessLinkedAdAccount parseAccount(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        var adStatus = obj.getJSONObject("ad_status");
        var hasActive = adStatus == null ? null : adStatus.getBoolean("has_active_ctwa_ad");
        var hasCreated = adStatus == null ? null : adStatus.getBoolean("has_created_ad");
        return new BusinessLinkedAdAccountBuilder()
                .id(obj.getString("id"))
                .hasActiveClickToWhatsAppAd(hasActive != null && hasActive)
                .hasCreatedAd(hasCreated != null && hasCreated)
                .build();
    }

    /**
     * Returns the parsed linked-accounts view.
     *
     * @return the parsed {@link BusinessLinkedAdAccounts}, never {@code null}
     */
    public BusinessLinkedAdAccounts accounts() {
        return accounts;
    }
}

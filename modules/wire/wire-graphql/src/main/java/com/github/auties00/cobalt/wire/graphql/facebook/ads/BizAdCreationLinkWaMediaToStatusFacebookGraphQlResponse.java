package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.AdMediaRegistration;
import com.github.auties00.cobalt.wire.linked.business.ads.AdMediaRegistrationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the link-WhatsApp-media-to-native-ad mutation built by
 * {@link BizAdCreationLinkWaMediaToStatusFacebookGraphQlRequest} into a list of {@link AdMediaRegistration}.
 *
 * <p>Reads the linked root {@code xfb_ctwa_native_upload_ad_media} and projects its
 * {@code media_list} array onto the Cobalt domain model: one entry per registered medium, pairing
 * the server-assigned media identifier with its media kind.
 *
 * @see BizAdCreationLinkWaMediaToStatusFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationLinkWAMediaToStatusMutation")
public final class BizAdCreationLinkWaMediaToStatusFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed media-registration entries.
     */
    private final List<AdMediaRegistration> registrations;

    /**
     * Constructs a response wrapping the parsed media-registration entries.
     *
     * <p>Reserved for the static parser.
     *
     * @param registrations the parsed media-registration entries
     */
    private BizAdCreationLinkWaMediaToStatusFacebookGraphQlResponse(List<AdMediaRegistration> registrations) {
        this.registrations = registrations;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xfb_ctwa_native_upload_ad_media} and projects its
     * {@code media_list} array onto a list of {@link AdMediaRegistration}; the returned
     * {@link Optional} is empty when {@code data} or the registration object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the registration object is missing
     */
    public static Optional<BizAdCreationLinkWaMediaToStatusFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xfb_ctwa_native_upload_ad_media");
        if (node == null) {
            return Optional.empty();
        }

        return Optional.of(new BizAdCreationLinkWaMediaToStatusFacebookGraphQlResponse(parseRegistrations(node.getJSONArray("media_list"))));
    }

    /**
     * Projects the {@code media_list} array onto a list of {@link AdMediaRegistration}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<AdMediaRegistration> parseRegistrations(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<AdMediaRegistration>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new AdMediaRegistrationBuilder()
                    .id(obj.getString("id"))
                    .kind(obj.getString("type"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed media-registration entries.
     *
     * @return the parsed entries, empty when the Meta graph endpoint returned none
     */
    public List<AdMediaRegistration> registrations() {
        return registrations;
    }
}

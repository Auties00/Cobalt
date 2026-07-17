package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessWebsiteLink;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessWebsiteLinkBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the profile-shimlinks query built by
 * {@link BizGetProfileShimlinksWhatsAppGraphQlRequest} into a list of {@link BusinessWebsiteLink}.
 *
 * <p>Reads the plural {@code xwa_whatsapp_smb_get_profile_linkshims} root and projects each entry,
 * pairing the raw advertised website with its safe redirect URL, onto the Cobalt domain model.
 *
 * @see BizGetProfileShimlinksWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizGetProfileShimlinksQuery")
public final class BizGetProfileShimlinksWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed website links.
     */
    private final List<BusinessWebsiteLink> websites;

    /**
     * Constructs a response wrapping the parsed website links.
     *
     * <p>Reserved for the static parser.
     *
     * @param websites the parsed website links
     */
    private BizGetProfileShimlinksWhatsAppGraphQlResponse(List<BusinessWebsiteLink> websites) {
        this.websites = websites;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the plural root {@code xwa_whatsapp_smb_get_profile_linkshims} and projects each entry
     * onto a {@link BusinessWebsiteLink}; the returned {@link Optional} is empty when {@code data} is
     * {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetProfileShimlinksQuery", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizGetProfileShimlinksWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var websites = parseWebsites(data.getJSONArray("xwa_whatsapp_smb_get_profile_linkshims"));
        return Optional.of(new BizGetProfileShimlinksWhatsAppGraphQlResponse(websites));
    }

    /**
     * Projects a {@code xwa_whatsapp_smb_get_profile_linkshims} JSON array onto a list of
     * {@link BusinessWebsiteLink}.
     *
     * @param array the JSON array to parse, or {@code null}
     * @return the projected website links, empty when {@code array} is {@code null}
     */
    private static List<BusinessWebsiteLink> parseWebsites(JSONArray array) {
        if (array == null) {
            return List.of();
        }
        var result = new ArrayList<BusinessWebsiteLink>(array.size());
        for (var i = 0; i < array.size(); i++) {
            var entry = array.getJSONObject(i);
            if (entry == null) {
                continue;
            }
            result.add(new BusinessWebsiteLinkBuilder()
                    .website(entry.getString("website"))
                    .safeRedirectUrl(entry.getString("shimmed_website_url"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed website links.
     *
     * <p>Each {@link BusinessWebsiteLink} pairs a raw advertised website with the safe redirect URL to
     * navigate to instead.
     *
     * @return the parsed website links, empty when the graph.whatsapp.com endpoint returned none
     */
    public List<BusinessWebsiteLink> websites() {
        return websites;
    }
}

package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdManagementScreen;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdManagementScreenBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the ad-management root query built by
 * {@link BizAdManagementRootFacebookGraphQlRequest} into a {@link BusinessAdManagementScreen}.
 *
 * <p>Projects the embedder-useful fields of the WhatsApp Business ad-management dashboard onto the
 * {@link BusinessAdManagementScreen} model: the managed page header from the {@code page} root, the
 * latest draft identifier, the identifiers of the boosted ads on the current page, and the forward
 * pagination markers from the {@code ctwa} root.
 *
 * @see BizAdManagementRootFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdManagementRootQuery")
public final class BizAdManagementRootFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected management screen.
     */
    private final BusinessAdManagementScreen screen;

    /**
     * Constructs a response wrapping the projected management screen.
     *
     * <p>Reserved for the static parser.
     *
     * @param screen the projected management screen
     */
    private BizAdManagementRootFacebookGraphQlResponse(BusinessAdManagementScreen screen) {
        this.screen = screen;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * embedder-useful fields onto a {@link BusinessAdManagementScreen}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdManagementRootFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        String pageName = null;
        var pageVerified = false;
        var page = data.getJSONObject("page");
        if (page != null) {
            pageName = page.getString("name");
            pageVerified = Boolean.TRUE.equals(page.getBoolean("is_verified"));
        }

        String latestDraftId = null;
        List<String> boostedAdIds = List.of();
        var hasNextPage = false;
        String endCursor = null;
        var ctwa = data.getJSONObject("ctwa");
        if (ctwa != null) {
            var latestDraft = ctwa.getJSONObject("latest_wa_web_draft");
            if (latestDraft != null) {
                latestDraftId = latestDraft.getString("id");
            }

            var allUserAds = ctwa.getJSONObject("all_user_ads");
            if (allUserAds != null) {
                boostedAdIds = boostedAdIds(allUserAds.getJSONArray("edges"));
                var pageInfo = allUserAds.getJSONObject("page_info");
                if (pageInfo != null) {
                    hasNextPage = Boolean.TRUE.equals(pageInfo.getBoolean("has_next_page"));
                    endCursor = pageInfo.getString("end_cursor");
                }
            }
        }

        var screen = new BusinessAdManagementScreenBuilder()
                .pageName(pageName)
                .pageVerified(pageVerified)
                .latestDraftId(latestDraftId)
                .boostedAdIds(boostedAdIds)
                .hasNextPage(hasNextPage)
                .endCursor(endCursor)
                .build();
        return Optional.of(new BizAdManagementRootFacebookGraphQlResponse(screen));
    }

    /**
     * Projects the {@code edges} array onto the identifiers of the boosted ads, reading each edge's
     * {@code stanza.id}.
     *
     * @param edges the {@code edges} array, or {@code null}
     * @return the ad identifiers, empty when {@code edges} is {@code null}
     */
    private static List<String> boostedAdIds(JSONArray edges) {
        if (edges == null) {
            return List.of();
        }

        var result = new ArrayList<String>(edges.size());
        for (var i = 0; i < edges.size(); i++) {
            var edge = edges.getJSONObject(i);
            if (edge == null) {
                continue;
            }
            var node = edge.getJSONObject("node");
            if (node == null) {
                continue;
            }
            var id = node.getString("id");
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * Returns the projected management screen.
     *
     * @return the projected {@link BusinessAdManagementScreen}
     */
    public BusinessAdManagementScreen screen() {
        return screen;
    }
}

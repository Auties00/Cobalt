package com.github.auties00.cobalt.wire.graphql.whatsappWeb.user;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.bot.profile.BotProfessionalStatus;
import com.github.auties00.cobalt.wire.linked.bot.profile.BotProfile;
import com.github.auties00.cobalt.wire.linked.bot.profile.BotProfileBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the fetch-bot-profiles query built by
 * {@link FetchBotProfilesGqlWhatsAppWebGraphQlRequest} into a list of {@link BotProfile}.
 *
 * <p>Reads the plural root {@code xfb_fetch_genai_personas} and projects each entry onto a
 * {@link BotProfile}: the persona identifier, the bot's WhatsApp address, the meta-created flag,
 * the discovery creator name and profile URL, and the persona's latest published version
 * (display name, description, and professional-status marker).
 *
 * @see FetchBotProfilesGqlWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebFetchBotProfilesGQLQuery")
public final class FetchBotProfilesGqlWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed bot profiles.
     */
    private final List<BotProfile> profiles;

    /**
     * Constructs a response wrapping the parsed bot profiles.
     *
     * <p>Reserved for the static parser.
     *
     * @param profiles the parsed bot profiles
     */
    private FetchBotProfilesGqlWhatsAppWebGraphQlResponse(List<BotProfile> profiles) {
        this.profiles = profiles;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the plural root {@code xfb_fetch_genai_personas} and projects each entry onto a
     * {@link BotProfile}; the returned {@link Optional} is empty when {@code data} is
     * {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<FetchBotProfilesGqlWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var profiles = parseProfiles(data.getJSONArray("xfb_fetch_genai_personas"));
        return Optional.of(new FetchBotProfilesGqlWhatsAppWebGraphQlResponse(profiles));
    }

    /**
     * Projects the {@code xfb_fetch_genai_personas} array onto a list of {@link BotProfile}.
     *
     * <p>Entries missing the bot {@code jid} are dropped because {@link BotProfile} requires a
     * non-null address.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<BotProfile> parseProfiles(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BotProfile>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            var jidString = obj.getString("jid");
            if (jidString == null) {
                continue;
            }

            var creator = obj.getJSONObject("creator");
            var published = obj.getJSONObject("latest_published_version_for_viewer");
            var metaCreated = obj.getBoolean("is_meta_created");
            String name = null;
            String description = null;
            BotProfessionalStatus professional = null;
            if (published != null) {
                name = published.getString("name");
                description = published.getString("description");
                professional = BotProfessionalStatus.of(published.getString("posing_as_professional"));
            }

            var builder = new BotProfileBuilder()
                    .jid(Jid.of(jidString))
                    .personaId(obj.getString("id"))
                    .name(name)
                    .description(description)
                    .isMetaCreated(metaCreated != null && metaCreated)
                    .professionalStatus(professional);
            if (creator != null) {
                builder.creatorName(creator.getString("name"));
                builder.creatorProfileUrl(parseUri(creator.getString("profile_uri")));
            }
            result.add(builder.build());
        }
        return result;
    }

    /**
     * Parses {@code raw} into a {@link URI}, returning {@code null} on failure.
     *
     * @param raw the raw URI string
     * @return the parsed URI, or {@code null} when {@code raw} is {@code null} or unparseable
     */
    private static URI parseUri(String raw) {
        if (raw == null) {
            return null;
        }

        try {
            return new URI(raw);
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    /**
     * Returns the parsed bot profiles.
     *
     * @return the parsed bot profiles, empty when the relay returned none
     */
    public List<BotProfile> profiles() {
        return profiles;
    }
}

package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the resolve-video-URL query built by
 * {@link BizAdCreationResolveVideoUrlFacebookGraphQlRequest}.
 *
 * <p>Reads the scalar {@code playable_url} under the {@code fetch__Video} root, the video's playable
 * location, and exposes it as a {@link URI}.
 *
 * @see BizAdCreationResolveVideoUrlFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationResolveVideoURLQuery")
public final class BizAdCreationResolveVideoUrlFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the resolved playable location, or {@code null} when the relay omitted it or it was not a
     * valid URI.
     */
    private final URI playableUrl;

    /**
     * Constructs a response wrapping the resolved playable location.
     *
     * <p>Reserved for the static parser.
     *
     * @param playableUrl the resolved playable location, or {@code null} when absent
     */
    private BizAdCreationResolveVideoUrlFacebookGraphQlResponse(URI playableUrl) {
        this.playableUrl = playableUrl;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and resolves the
     * {@code playable_url} scalar under the {@code fetch__Video} root into a {@link URI}.
     *
     * <p>A scalar that is absent or not a syntactically valid URI yields an empty playable location.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationResolveVideoUrlFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var video = data.getJSONObject("fetch__Video");
        var raw = video == null ? null : video.getString("playable_url");
        return Optional.of(new BizAdCreationResolveVideoUrlFacebookGraphQlResponse(toUri(raw)));
    }

    /**
     * Parses the given string into a {@link URI}, returning {@code null} when it is absent or not
     * syntactically valid.
     *
     * @param raw the candidate URI string, or {@code null}
     * @return the parsed {@link URI}, or {@code null} when absent or invalid
     */
    private static URI toUri(String raw) {
        if (raw == null) {
            return null;
        }

        try {
            return new URI(raw);
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    /**
     * Returns the resolved playable location of the video.
     *
     * @return the resolved playable location, or empty when the relay omitted it or it was not a valid
     *         URI
     */
    public Optional<URI> playableUrl() {
        return Optional.ofNullable(playableUrl);
    }
}

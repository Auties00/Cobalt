package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the resolve-video-thumbnail-URL query built by
 * {@link BizAdCreationResolveVideoThumbnailUrlFacebookGraphQlRequest}.
 *
 * <p>Reads the {@code uri} of the {@code preferred_thumbnail.image} under the {@code fetch__Video}
 * root, the preferred thumbnail's location, and exposes it as a {@link URI}.
 *
 * @see BizAdCreationResolveVideoThumbnailUrlFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationResolveVideoThumbnailURLQuery")
public final class BizAdCreationResolveVideoThumbnailUrlFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the resolved thumbnail location, or {@code null} when the relay omitted it or it was not a
     * valid URI.
     */
    private final URI thumbnailUrl;

    /**
     * Constructs a response wrapping the resolved thumbnail location.
     *
     * <p>Reserved for the static parser.
     *
     * @param thumbnailUrl the resolved thumbnail location, or {@code null} when absent
     */
    private BizAdCreationResolveVideoThumbnailUrlFacebookGraphQlResponse(URI thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and resolves the
     * preferred thumbnail's {@code uri} under the {@code fetch__Video} root into a {@link URI}.
     *
     * <p>A location that is absent or not a syntactically valid URI yields an empty thumbnail
     * location.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationResolveVideoThumbnailUrlFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        String raw = null;
        var video = data.getJSONObject("fetch__Video");
        if (video != null) {
            var preferredThumbnail = video.getJSONObject("preferred_thumbnail");
            if (preferredThumbnail != null) {
                var image = preferredThumbnail.getJSONObject("image");
                if (image != null) {
                    raw = image.getString("uri");
                }
            }
        }
        return Optional.of(new BizAdCreationResolveVideoThumbnailUrlFacebookGraphQlResponse(toUri(raw)));
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
     * Returns the resolved location of the video's preferred thumbnail.
     *
     * @return the resolved thumbnail location, or empty when the relay omitted it or it was not a
     *         valid URI
     */
    public Optional<URI> thumbnailUrl() {
        return Optional.ofNullable(thumbnailUrl);
    }
}

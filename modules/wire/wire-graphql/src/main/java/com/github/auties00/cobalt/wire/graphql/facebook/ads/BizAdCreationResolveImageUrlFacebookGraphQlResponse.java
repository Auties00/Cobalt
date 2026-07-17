package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the resolve-image-URL query built by
 * {@link BizAdCreationResolveImageUrlFacebookGraphQlRequest}.
 *
 * <p>Reads the single scalar {@code image_url_from_hash} under the linked {@code lwi} root, the
 * displayable location the relay resolved from the uploaded image hash, and exposes it as a
 * {@link URI}.
 *
 * @see BizAdCreationResolveImageUrlFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationResolveImageURLQuery")
public final class BizAdCreationResolveImageUrlFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the resolved image location, or {@code null} when the relay omitted it or it was not a
     * valid URI.
     */
    private final URI imageUrl;

    /**
     * Constructs a response wrapping the resolved image location.
     *
     * <p>Reserved for the static parser.
     *
     * @param imageUrl the resolved image location, or {@code null} when absent
     */
    private BizAdCreationResolveImageUrlFacebookGraphQlResponse(URI imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and resolves the
     * {@code image_url_from_hash} scalar under the {@code lwi} root into a {@link URI}.
     *
     * <p>A scalar that is absent or not a syntactically valid URI yields an empty image location.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationResolveImageUrlFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var lwi = data.getJSONObject("lwi");
        var raw = lwi == null ? null : lwi.getString("image_url_from_hash");
        return Optional.of(new BizAdCreationResolveImageUrlFacebookGraphQlResponse(toUri(raw)));
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
     * Returns the resolved displayable location of the image.
     *
     * @return the resolved image location, or empty when the relay omitted it or it was not a valid
     *         URI
     */
    public Optional<URI> imageUrl() {
        return Optional.ofNullable(imageUrl);
    }
}

package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Exposes the server-unfurled link metadata and encrypted thumbnail handle returned by the
 * {@link FetchPlaintextLinkPreviewMexRequest} query.
 *
 * <p>The {@code xwa2_newsletter_link_preview} envelope carries the title, description, preview
 * type, inline thumbnail bytes and the {@link #directPath()}/{@link #hash()} pair that addresses
 * the high-quality thumbnail in the media pipeline. Every scalar is surfaced as an
 * {@link Optional}.
 *
 * @implNote This implementation does not coerce missing {@code height} and {@code width} to
 * {@code 0} and does not gate the thumbnail on the presence of both {@code direct_path} and
 * {@code hash}; both the coercions and the absence policy are left to the caller.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchPlaintextLinkPreviewJob")
public final class FetchPlaintextLinkPreviewMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the {@code description} scalar projected from the envelope.
     */
    private final String description;

    /**
     * Holds the {@code direct_path} scalar carrying the encrypted thumbnail media handle.
     */
    private final String directPath;

    /**
     * Holds the {@code hash} scalar paired with {@link #directPath} when the high-quality thumbnail
     * is available.
     */
    private final String hash;

    /**
     * Holds the {@code preview_type} scalar projected from the envelope.
     */
    private final String previewType;

    /**
     * Holds the {@code thumb_data} scalar carrying the inline (low-resolution) thumbnail bytes.
     */
    private final String thumbData;

    /**
     * Holds the {@code title} scalar projected from the envelope.
     */
    private final String title;

    /**
     * Holds the {@code height} scalar of the preview thumbnail.
     */
    private final String height;

    /**
     * Holds the {@code width} scalar of the preview thumbnail.
     */
    private final String width;

    /**
     * Constructs a new response wrapping the parsed scalar fields of the
     * {@code xwa2_newsletter_link_preview} envelope.
     *
     * <p>Instances are produced only by the {@link #of(Stanza)} parser.
     *
     * @param description  the {@code description} scalar, may be {@code null}
     * @param directPath   the {@code direct_path} scalar, may be {@code null}
     * @param hash         the {@code hash} scalar, may be {@code null}
     * @param previewType  the {@code preview_type} scalar, may be {@code null}
     * @param thumbData    the {@code thumb_data} scalar, may be {@code null}
     * @param title        the {@code title} scalar, may be {@code null}
     * @param height       the {@code height} scalar, may be {@code null}
     * @param width        the {@code width} scalar, may be {@code null}
     */
    private FetchPlaintextLinkPreviewMexResponse(String description, String directPath, String hash, String previewType, String thumbData, String title, String height, String width) {
        this.description = description;
        this.directPath = directPath;
        this.hash = hash;
        this.previewType = previewType;
        this.thumbData = thumbData;
        this.title = title;
        this.height = height;
        this.width = width;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>Reads the {@code <result>} child's byte content and routes it through the private
     * byte-level parser. Yields {@link Optional#empty()} when the stanza carries no result or when
     * the {@code data.xwa2_newsletter_link_preview} envelope is absent; callers fold the same
     * absence into a minimal local preview.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchPlaintextLinkPreviewJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<FetchPlaintextLinkPreviewMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchPlaintextLinkPreviewMexResponse::of);
    }

    /**
     * Returns the {@code description} scalar of the unfurled link preview.
     *
     * @return an {@link Optional} containing the description, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the {@code direct_path} scalar carrying the encrypted thumbnail media handle.
     *
     * <p>Paired with {@link #hash()} when feeding the thumbnail download pipeline; both fields must
     * be present for the high-quality thumbnail to be fetchable.
     *
     * @return an {@link Optional} containing the direct path, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(directPath);
    }

    /**
     * Returns the {@code hash} scalar paired with {@link #directPath()} when the high-quality
     * thumbnail is available.
     *
     * @return an {@link Optional} containing the thumbnail hash, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> hash() {
        return Optional.ofNullable(hash);
    }

    /**
     * Returns the {@code preview_type} scalar classifying the unfurled link.
     *
     * @return an {@link Optional} containing the preview type, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> previewType() {
        return Optional.ofNullable(previewType);
    }

    /**
     * Returns the {@code thumb_data} scalar carrying the inline (low-resolution) thumbnail bytes.
     *
     * @return an {@link Optional} containing the inline thumbnail data, or {@link Optional#empty()}
     *         if the relay omitted the scalar
     */
    public Optional<String> thumbData() {
        return Optional.ofNullable(thumbData);
    }

    /**
     * Returns the {@code title} scalar of the unfurled link preview.
     *
     * @return an {@link Optional} containing the title, or {@link Optional#empty()} if the relay
     *         omitted the scalar
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the {@code height} scalar of the preview thumbnail.
     *
     * @return an {@link Optional} containing the height, or {@link Optional#empty()} if the relay
     *         omitted the scalar
     */
    public Optional<String> height() {
        return Optional.ofNullable(height);
    }

    /**
     * Returns the {@code width} scalar of the preview thumbnail.
     *
     * @return an {@link Optional} containing the width, or {@link Optional#empty()} if the relay
     *         omitted the scalar
     */
    public Optional<String> width() {
        return Optional.ofNullable(width);
    }

    /**
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link FetchPlaintextLinkPreviewMexResponse}.
     *
     * <p>Routed through {@link #of(Stanza)} after the byte content of the {@code <result>} child is
     * extracted. Yields {@link Optional#empty()} when the envelope, the {@code data} branch, or the
     * {@code xwa2_newsletter_link_preview} child is absent.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         {@code data.xwa2_newsletter_link_preview} envelope is absent
     */
    private static Optional<FetchPlaintextLinkPreviewMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_link_preview");
        if (root == null) {
            return Optional.empty();
        }

        var description = root.getString("description");
        var directPath = root.getString("direct_path");
        var hash = root.getString("hash");
        var previewType = root.getString("preview_type");
        var thumbData = root.getString("thumb_data");
        var title = root.getString("title");
        var height = root.getString("height");
        var width = root.getString("width");

        return Optional.of(new FetchPlaintextLinkPreviewMexResponse(description, directPath, hash, previewType, thumbData, title, height, width));
    }
}

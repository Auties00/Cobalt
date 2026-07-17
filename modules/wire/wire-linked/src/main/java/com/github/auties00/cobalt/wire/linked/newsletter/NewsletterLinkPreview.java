package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents the server-unfurled preview of a URL pasted into a
 * newsletter compose surface.
 *
 * <p>Newsletter posts cannot use the regular client-side link-preview
 * pipeline because the recipient anonymity guarantee forbids the client
 * from fetching the target URL directly. Instead, the server unfurls
 * the URL, returns the title and description, and exposes an encrypted
 * thumbnail handle that can be downloaded through the standard media
 * pipeline. This type wraps the unfurled metadata together with the
 * media handle so callers can render the preview card without dealing
 * with the GraphQL envelope.
 */
@ProtobufMessage
public final class NewsletterLinkPreview {
    /**
     * The unfurled page title.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String title;

    /**
     * The unfurled page description.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String description;

    /**
     * The wire-level preview classification (for example {@code "high_quality"}).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String previewType;

    /**
     * The direct path of the encrypted thumbnail on the media server.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String thumbnailDirectPath;

    /**
     * The hash paired with the thumbnail direct path, used for media
     * verification.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String thumbnailHash;

    /**
     * The base64-encoded inline thumbnail bytes, used as a fallback when
     * the encrypted handle has not yet been resolved.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String thumbnailData;

    /**
     * The width of the thumbnail in pixels, when reported.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT32)
    Integer thumbnailWidth;

    /**
     * The height of the thumbnail in pixels, when reported.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT32)
    Integer thumbnailHeight;

    /**
     * Constructs a new {@code NewsletterLinkPreview}. Invoked by the
     * generated protobuf deserializer and by the converters that adapt
     * wire responses into the domain model.
     *
     * @param title               the page title, may be {@code null}
     * @param description         the page description, may be {@code null}
     * @param previewType         the preview classification, may be {@code null}
     * @param thumbnailDirectPath the thumbnail direct path, may be {@code null}
     * @param thumbnailHash       the thumbnail hash, may be {@code null}
     * @param thumbnailData       the inline thumbnail bytes, may be {@code null}
     * @param thumbnailWidth      the thumbnail width in pixels, may be {@code null}
     * @param thumbnailHeight     the thumbnail height in pixels, may be {@code null}
     */
    NewsletterLinkPreview(String title, String description, String previewType, String thumbnailDirectPath, String thumbnailHash, String thumbnailData, Integer thumbnailWidth, Integer thumbnailHeight) {
        this.title = title;
        this.description = description;
        this.previewType = previewType;
        this.thumbnailDirectPath = thumbnailDirectPath;
        this.thumbnailHash = thumbnailHash;
        this.thumbnailData = thumbnailData;
        this.thumbnailWidth = thumbnailWidth;
        this.thumbnailHeight = thumbnailHeight;
    }

    /**
     * Returns the unfurled page title.
     *
     * @return an {@link Optional} carrying the title, or empty when not
     *         reported
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the unfurled page description.
     *
     * @return an {@link Optional} carrying the description, or empty when
     *         not reported
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the wire-level preview classification.
     *
     * @return an {@link Optional} carrying the classification, or empty
     *         when not reported
     */
    public Optional<String> previewType() {
        return Optional.ofNullable(previewType);
    }

    /**
     * Returns the direct path of the encrypted thumbnail on the media
     * server.
     *
     * @return an {@link Optional} carrying the direct path, or empty when
     *         not reported
     */
    public Optional<String> thumbnailDirectPath() {
        return Optional.ofNullable(thumbnailDirectPath);
    }

    /**
     * Returns the hash paired with the thumbnail direct path.
     *
     * @return an {@link Optional} carrying the hash, or empty when not
     *         reported
     */
    public Optional<String> thumbnailHash() {
        return Optional.ofNullable(thumbnailHash);
    }

    /**
     * Returns the base64-encoded inline thumbnail bytes.
     *
     * @return an {@link Optional} carrying the inline bytes, or empty
     *         when not reported
     */
    public Optional<String> thumbnailData() {
        return Optional.ofNullable(thumbnailData);
    }

    /**
     * Returns the width of the thumbnail in pixels.
     *
     * @return an {@link OptionalInt} carrying the width, or empty when
     *         not reported
     */
    public OptionalInt thumbnailWidth() {
        return thumbnailWidth == null ? OptionalInt.empty() : OptionalInt.of(thumbnailWidth);
    }

    /**
     * Returns the height of the thumbnail in pixels.
     *
     * @return an {@link OptionalInt} carrying the height, or empty when
     *         not reported
     */
    public OptionalInt thumbnailHeight() {
        return thumbnailHeight == null ? OptionalInt.empty() : OptionalInt.of(thumbnailHeight);
    }

    /**
     * Returns whether this link preview equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a
     *         {@code NewsletterLinkPreview} carrying equal fields
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterLinkPreview that
                && Objects.equals(title, that.title)
                && Objects.equals(description, that.description)
                && Objects.equals(previewType, that.previewType)
                && Objects.equals(thumbnailDirectPath, that.thumbnailDirectPath)
                && Objects.equals(thumbnailHash, that.thumbnailHash)
                && Objects.equals(thumbnailData, that.thumbnailData)
                && Objects.equals(thumbnailWidth, that.thumbnailWidth)
                && Objects.equals(thumbnailHeight, that.thumbnailHeight);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(title, description, previewType, thumbnailDirectPath, thumbnailHash, thumbnailData, thumbnailWidth, thumbnailHeight);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return "NewsletterLinkPreview[title=" + title +
                ", previewType=" + previewType + ']';
    }
}

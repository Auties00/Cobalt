package com.github.auties00.cobalt.wire.linked.business.ctwa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Click-to-WhatsApp (CTWA) ad context attached to a business conversation.
 *
 * <p>Click-to-WhatsApp is a Meta Ads feature that lets advertisers run
 * Facebook and Instagram ads which deep-link straight into a WhatsApp
 * chat with a business. When a user taps such an ad, WhatsApp captures
 * metadata about the originating creative (source URL, headline, body
 * text, thumbnail, optional video, source app and optional automated
 * greeting fields) and binds it to the resulting conversation so the
 * business can identify which ad drove the contact and so the client can
 * render an "ad reference" header above the very first message.
 *
 * <p>Only the source URL, source identifier and source-type discriminator
 * are guaranteed to be present. Every other field is populated
 * conditionally depending on the ad creative kind, the optional inline
 * thumbnail bytes and whether the source app participates in the
 * automated greeting message (AGM) integration; absent fields surface
 * as empty {@link Optional} accessors.
 *
 * <p>The automated-greeting fields ({@linkplain #greetingMessageBody()
 * greeting body}, {@linkplain #automatedGreetingMessageShown() shown
 * flag}, {@linkplain #ctaPayload() CTA payload}, {@linkplain
 * #originalImageUrl() original image URL}) are only populated when the
 * advertiser's source app opted into the WhatsApp Marketing-Optimised
 * AGM flow.
 */
@ProtobufMessage(name = "BusinessCtwaContext")
public final class BusinessCtwaContext {
    /**
     * The destination URL of the ad creative the user tapped to start
     * this conversation. Always populated.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String sourceUrl;

    /**
     * The advertiser-side identifier of the ad creative (typically an
     * opaque Meta Ads identifier). Always populated and used to attribute
     * the conversation back to a specific campaign.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String sourceId;

    /**
     * Free-form discriminator describing the ad surface type (for
     * example which Meta surface the ad was served on). Always populated.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String sourceType;

    /**
     * Optional headline copy displayed by the ad creative, mirrored into
     * the in-chat ad reference card. Empty when the creative had no
     * headline.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String title;

    /**
     * Optional body copy displayed by the ad creative, mirrored into the
     * in-chat ad reference card. Empty when the creative had no body
     * text.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String description;

    /**
     * Optional remote URL of the ad thumbnail image. Empty when the
     * creative had no thumbnail.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String thumbnailUrl;

    /**
     * Optional inline thumbnail bytes published alongside the thumbnail
     * URL. Empty when the upstream response did not include an inline
     * preview blob.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    byte[] thumbnail;

    /**
     * Optional URL of the video creative when the ad references a video
     * asset. Empty when the ad creative is a still image or has no media
     * at all.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String mediaUrl;

    /**
     * Optional discriminator indicating whether the ad creative is a
     * still image or a video. Empty when the upstream response carried
     * no thumbnail child at all.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.ENUM)
    BusinessCtwaMediaType mediaType;

    /**
     * Optional source-app identifier (for example the Meta surface app
     * that hosted the ad). Used by the client to gate the automated
     * greeting fields below; empty when absent.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    String sourceApp;

    /**
     * Optional automated greeting body text the business has configured
     * to be shown to the user as a system-generated welcome message.
     * Populated only when the source app participates in the
     * Marketing-Optimised AGM integration.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    String greetingMessageBody;

    /**
     * Optional flag indicating whether the automated greeting message
     * was actually surfaced in the chat UI. Populated only when the
     * source app participates in the Marketing-Optimised AGM
     * integration.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    Boolean automatedGreetingMessageShown;

    /**
     * Optional opaque payload attached to the ad's call-to-action button
     * (used by the business backend to deep-link into a specific journey
     * once the user replies). Empty when the ad has no CTA payload.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    String ctaPayload;

    /**
     * Optional URL of the un-edited original image used to compose the
     * ad creative. Populated only when the source app participates in
     * the Marketing-Optimised AGM integration.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    String originalImageUrl;

    /**
     * Constructs a new {@code BusinessCtwaContext} with the given
     * field values. The first three arguments must be non-{@code null};
     * every other argument may be {@code null} when the corresponding
     * ad metadata is absent.
     *
     * @param sourceUrl                     the ad destination URL; never {@code null}
     * @param sourceId                      the ad creative identifier; never {@code null}
     * @param sourceType                    the ad surface discriminator; never {@code null}
     * @param title                         the ad headline, or {@code null}
     * @param description                   the ad body copy, or {@code null}
     * @param thumbnailUrl                  the remote thumbnail URL, or {@code null}
     * @param thumbnail                     the inline thumbnail bytes, or {@code null}
     * @param mediaUrl                      the ad video URL, or {@code null}
     * @param mediaType                     the media-type discriminator, or {@code null}
     * @param sourceApp                     the source-app identifier, or {@code null}
     * @param greetingMessageBody           the automated greeting body, or {@code null}
     * @param automatedGreetingMessageShown the AGM "shown" flag, or {@code null}
     * @param ctaPayload                    the CTA payload, or {@code null}
     * @param originalImageUrl              the original-image URL, or {@code null}
     * @throws NullPointerException if {@code sourceUrl}, {@code sourceId} or
     *                              {@code sourceType} is {@code null}
     */
    BusinessCtwaContext(String sourceUrl,
                        String sourceId,
                        String sourceType,
                        String title,
                        String description,
                        String thumbnailUrl,
                        byte[] thumbnail,
                        String mediaUrl,
                        BusinessCtwaMediaType mediaType,
                        String sourceApp,
                        String greetingMessageBody,
                        Boolean automatedGreetingMessageShown,
                        String ctaPayload,
                        String originalImageUrl) {
        this.sourceUrl = Objects.requireNonNull(sourceUrl, "sourceUrl cannot be null");
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId cannot be null");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType cannot be null");
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.thumbnail = thumbnail;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.sourceApp = sourceApp;
        this.greetingMessageBody = greetingMessageBody;
        this.automatedGreetingMessageShown = automatedGreetingMessageShown;
        this.ctaPayload = ctaPayload;
        this.originalImageUrl = originalImageUrl;
    }

    /**
     * Returns the destination URL of the ad creative the user tapped
     * to start this conversation.
     *
     * @return the source URL; never {@code null}
     */
    public String sourceUrl() {
        return sourceUrl;
    }

    /**
     * Returns the advertiser-side identifier of the ad creative used to
     * attribute the conversation back to a specific campaign.
     *
     * @return the source identifier; never {@code null}
     */
    public String sourceId() {
        return sourceId;
    }

    /**
     * Returns the free-form discriminator describing the ad surface type.
     *
     * @return the source-type discriminator; never {@code null}
     */
    public String sourceType() {
        return sourceType;
    }

    /**
     * Returns the optional headline copy displayed by the ad creative.
     *
     * @return an {@code Optional} containing the headline, or empty when
     *         the creative had no headline
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the optional body copy displayed by the ad creative.
     *
     * @return an {@code Optional} containing the body copy, or empty when
     *         the creative had no body text
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the optional remote URL of the ad thumbnail image.
     *
     * @return an {@code Optional} containing the thumbnail URL, or empty
     *         when the creative had no thumbnail
     */
    public Optional<String> thumbnailUrl() {
        return Optional.ofNullable(thumbnailUrl);
    }

    /**
     * Returns the optional inline thumbnail bytes published alongside
     * the thumbnail URL.
     *
     * @return an {@code Optional} containing the thumbnail bytes, or
     *         empty when no inline preview blob was attached
     */
    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    /**
     * Returns the optional URL of the video creative when the ad
     * references a video asset.
     *
     * @return an {@code Optional} containing the video URL, or empty
     *         when the ad has no video
     */
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    /**
     * Returns the optional discriminator indicating whether the ad
     * creative is a still image or a video.
     *
     * @return an {@code Optional} containing the media-type, or empty
     *         when the upstream response had no thumbnail child at all
     */
    public Optional<BusinessCtwaMediaType> mediaType() {
        return Optional.ofNullable(mediaType);
    }

    /**
     * Returns the optional source-app identifier used by the client to
     * gate the automated greeting fields.
     *
     * @return an {@code Optional} containing the source app, or empty
     *         when absent
     */
    public Optional<String> sourceApp() {
        return Optional.ofNullable(sourceApp);
    }

    /**
     * Returns the optional automated greeting body the business has
     * configured to be displayed as a system-generated welcome message.
     *
     * @return an {@code Optional} containing the greeting body, or empty
     *         when the source app does not participate in the AGM flow
     */
    public Optional<String> greetingMessageBody() {
        return Optional.ofNullable(greetingMessageBody);
    }

    /**
     * Returns the optional flag indicating whether the automated greeting
     * message was surfaced to the user.
     *
     * @return an {@code Optional} containing the flag, or empty when not
     *         set
     */
    public Optional<Boolean> automatedGreetingMessageShown() {
        return Optional.ofNullable(automatedGreetingMessageShown);
    }

    /**
     * Returns the optional opaque payload attached to the ad's
     * call-to-action button.
     *
     * @return an {@code Optional} containing the CTA payload, or empty
     *         when the ad has no CTA payload
     */
    public Optional<String> ctaPayload() {
        return Optional.ofNullable(ctaPayload);
    }

    /**
     * Returns the optional URL of the un-edited original image used to
     * compose the ad creative.
     *
     * @return an {@code Optional} containing the URL, or empty when the
     *         source app does not participate in the AGM flow
     */
    public Optional<String> originalImageUrl() {
        return Optional.ofNullable(originalImageUrl);
    }

    /**
     * Sets the destination URL of the ad creative the user tapped to
     * start this conversation.
     *
     * @param sourceUrl the source URL to set
     * @throws NullPointerException if {@code sourceUrl} is {@code null}
     */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = Objects.requireNonNull(sourceUrl, "sourceUrl cannot be null");
    }

    /**
     * Sets the advertiser-side identifier of the ad creative.
     *
     * @param sourceId the source identifier to set
     * @throws NullPointerException if {@code sourceId} is {@code null}
     */
    public void setSourceId(String sourceId) {
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId cannot be null");
    }

    /**
     * Sets the free-form discriminator describing the ad surface type.
     *
     * @param sourceType the source-type discriminator to set
     * @throws NullPointerException if {@code sourceType} is {@code null}
     */
    public void setSourceType(String sourceType) {
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType cannot be null");
    }

    /**
     * Sets the optional headline copy displayed by the ad creative.
     *
     * @param title the headline to set, or {@code null} to clear
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets the optional body copy displayed by the ad creative.
     *
     * @param description the body copy to set, or {@code null} to clear
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the optional remote URL of the ad thumbnail image.
     *
     * @param thumbnailUrl the thumbnail URL to set, or {@code null} to clear
     */
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * Sets the optional inline thumbnail bytes.
     *
     * @param thumbnail the thumbnail bytes to set, or {@code null} to clear
     */
    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    /**
     * Sets the optional URL of the video creative.
     *
     * @param mediaUrl the video URL to set, or {@code null} to clear
     */
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    /**
     * Sets the optional media-type discriminator indicating image vs
     * video.
     *
     * @param mediaType the media-type to set, or {@code null} to clear
     */
    public void setMediaType(BusinessCtwaMediaType mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * Sets the optional source-app identifier used to gate AGM fields.
     *
     * @param sourceApp the source-app identifier to set, or {@code null} to clear
     */
    public void setSourceApp(String sourceApp) {
        this.sourceApp = sourceApp;
    }

    /**
     * Sets the optional automated greeting body.
     *
     * @param greetingMessageBody the greeting body to set, or {@code null} to clear
     */
    public void setGreetingMessageBody(String greetingMessageBody) {
        this.greetingMessageBody = greetingMessageBody;
    }

    /**
     * Sets the optional flag indicating whether the automated greeting
     * message was surfaced to the user.
     *
     * @param automatedGreetingMessageShown the flag to set, or {@code null} to clear
     */
    public void setAutomatedGreetingMessageShown(Boolean automatedGreetingMessageShown) {
        this.automatedGreetingMessageShown = automatedGreetingMessageShown;
    }

    /**
     * Sets the optional opaque payload attached to the ad's
     * call-to-action button.
     *
     * @param ctaPayload the CTA payload to set, or {@code null} to clear
     */
    public void setCtaPayload(String ctaPayload) {
        this.ctaPayload = ctaPayload;
    }

    /**
     * Sets the optional URL of the un-edited original image used to
     * compose the ad creative.
     *
     * @param originalImageUrl the URL to set, or {@code null} to clear
     */
    public void setOriginalImageUrl(String originalImageUrl) {
        this.originalImageUrl = originalImageUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCtwaContext) obj;
        return Objects.equals(this.sourceUrl, that.sourceUrl) &&
                Objects.equals(this.sourceId, that.sourceId) &&
                Objects.equals(this.sourceType, that.sourceType) &&
                Objects.equals(this.title, that.title) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.thumbnailUrl, that.thumbnailUrl) &&
                Arrays.equals(this.thumbnail, that.thumbnail) &&
                Objects.equals(this.mediaUrl, that.mediaUrl) &&
                Objects.equals(this.mediaType, that.mediaType) &&
                Objects.equals(this.sourceApp, that.sourceApp) &&
                Objects.equals(this.greetingMessageBody, that.greetingMessageBody) &&
                Objects.equals(this.automatedGreetingMessageShown, that.automatedGreetingMessageShown) &&
                Objects.equals(this.ctaPayload, that.ctaPayload) &&
                Objects.equals(this.originalImageUrl, that.originalImageUrl);
    }

    @Override
    public int hashCode() {
        var result = Objects.hash(sourceUrl, sourceId, sourceType, title, description, thumbnailUrl,
                mediaUrl, mediaType, sourceApp, greetingMessageBody,
                automatedGreetingMessageShown, ctaPayload, originalImageUrl);
        result = 31 * result + Arrays.hashCode(thumbnail);
        return result;
    }

    @Override
    public String toString() {
        return "BusinessCtwaContext[" +
                "sourceUrl=" + sourceUrl + ", " +
                "sourceId=" + sourceId + ", " +
                "sourceType=" + sourceType + ", " +
                "title=" + title + ", " +
                "description=" + description + ", " +
                "thumbnailUrl=" + thumbnailUrl + ", " +
                "thumbnail=" + Arrays.toString(thumbnail) + ", " +
                "mediaUrl=" + mediaUrl + ", " +
                "mediaType=" + mediaType + ", " +
                "sourceApp=" + sourceApp + ", " +
                "greetingMessageBody=" + greetingMessageBody + ", " +
                "automatedGreetingMessageShown=" + automatedGreetingMessageShown + ", " +
                "ctaPayload=" + ctaPayload + ", " +
                "originalImageUrl=" + originalImageUrl + ']';
    }
}

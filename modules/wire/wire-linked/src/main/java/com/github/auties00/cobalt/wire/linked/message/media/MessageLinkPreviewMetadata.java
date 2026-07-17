package com.github.auties00.cobalt.wire.linked.message.media;

import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.linked.message.payment.PaymentLinkMetadata;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Rich metadata attached to link previews shown inside chat messages.
 *
 * <p>When a text message contains a URL, WhatsApp renders a preview card that may
 * include the destination URL, an inline video, an embedded music track, a payment
 * link and/or additional signals like the social media post type. This class groups
 * all that optional metadata into a single container.
 */
@ProtobufMessage(name = "Message.LinkPreviewMetadata")
public final class MessageLinkPreviewMetadata implements Message {
    /**
     * Payment link metadata when the previewed URL targets a payment flow.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    PaymentLinkMetadata paymentLinkMetadata;

    /**
     * Structured URL metadata attached to the preview.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    MessageURLMetadata urlMetadata;

    /**
     * Experiment identifier used for server-side A/B testing of preview rendering.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    Integer fbExperimentId;

    /**
     * Duration in seconds of the media referenced by the preview, if any.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    Integer linkMediaDuration;

    /**
     * Classification of the previewed content when it comes from a social media
     * platform.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    SocialMediaPostType socialMediaPostType;

    /**
     * Whether inline video playback for the previewed link should start muted.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    Boolean linkInlineVideoMuted;

    /**
     * URL of the video content associated with the preview.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String videoContentUrl;

    /**
     * Music metadata associated with the previewed content.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    EmbeddedMusic musicMetadata;

    /**
     * Caption shown alongside inline video content.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String videoContentCaption;


    /**
     * Constructs a new link preview metadata record.
     *
     * @param paymentLinkMetadata  payment metadata when the URL targets a payment flow
     * @param urlMetadata          structured URL metadata
     * @param fbExperimentId       experiment identifier
     * @param linkMediaDuration    media duration in seconds
     * @param socialMediaPostType  the social media post classification
     * @param linkInlineVideoMuted whether inline video is muted on autoplay
     * @param videoContentUrl      the URL of video content
     * @param musicMetadata        the music metadata
     * @param videoContentCaption  the caption for inline video
     */
    MessageLinkPreviewMetadata(PaymentLinkMetadata paymentLinkMetadata, MessageURLMetadata urlMetadata, Integer fbExperimentId, Integer linkMediaDuration, SocialMediaPostType socialMediaPostType, Boolean linkInlineVideoMuted, String videoContentUrl, EmbeddedMusic musicMetadata, String videoContentCaption) {
        this.paymentLinkMetadata = paymentLinkMetadata;
        this.urlMetadata = urlMetadata;
        this.fbExperimentId = fbExperimentId;
        this.linkMediaDuration = linkMediaDuration;
        this.socialMediaPostType = socialMediaPostType;
        this.linkInlineVideoMuted = linkInlineVideoMuted;
        this.videoContentUrl = videoContentUrl;
        this.musicMetadata = musicMetadata;
        this.videoContentCaption = videoContentCaption;
    }

    /**
     * Returns the payment link metadata.
     *
     * @return the metadata, or empty if the preview does not target a payment flow
     */
    public Optional<PaymentLinkMetadata> paymentLinkMetadata() {
        return Optional.ofNullable(paymentLinkMetadata);
    }

    /**
     * Returns the structured URL metadata.
     *
     * @return the metadata, or empty if not provided
     */
    public Optional<MessageURLMetadata> urlMetadata() {
        return Optional.ofNullable(urlMetadata);
    }

    /**
     * Returns the Facebook experiment identifier for this preview.
     *
     * @return the experiment id, or empty if unset
     */
    public OptionalInt fbExperimentId() {
        return fbExperimentId == null ? OptionalInt.empty() : OptionalInt.of(fbExperimentId);
    }

    /**
     * Returns the duration of the preview media in seconds.
     *
     * @return the duration, or empty if unknown
     */
    public OptionalInt linkMediaDuration() {
        return linkMediaDuration == null ? OptionalInt.empty() : OptionalInt.of(linkMediaDuration);
    }

    /**
     * Returns the social media post classification.
     *
     * @return the post type, or empty if not a social media post
     */
    public Optional<SocialMediaPostType> socialMediaPostType() {
        return Optional.ofNullable(socialMediaPostType);
    }

    /**
     * Returns whether inline video should start muted.
     *
     * @return {@code true} if muted, {@code false} otherwise
     */
    public boolean linkInlineVideoMuted() {
        return linkInlineVideoMuted != null && linkInlineVideoMuted;
    }

    /**
     * Returns the URL of the inline video content.
     *
     * @return the URL, or empty if none is present
     */
    public Optional<String> videoContentUrl() {
        return Optional.ofNullable(videoContentUrl);
    }

    /**
     * Returns the music metadata associated with the preview.
     *
     * @return the music metadata, or empty if none is present
     */
    public Optional<EmbeddedMusic> musicMetadata() {
        return Optional.ofNullable(musicMetadata);
    }

    /**
     * Returns the caption shown with the inline video.
     *
     * @return the caption, or empty if none is present
     */
    public Optional<String> videoContentCaption() {
        return Optional.ofNullable(videoContentCaption);
    }

    /**
     * Updates the payment link metadata.
     *
     * @param paymentLinkMetadata the new metadata, or {@code null} to clear
     */
    public void setPaymentLinkMetadata(PaymentLinkMetadata paymentLinkMetadata) {
        this.paymentLinkMetadata = paymentLinkMetadata;
    }

    /**
     * Updates the structured URL metadata.
     *
     * @param urlMetadata the new metadata, or {@code null} to clear
     */
    public void setUrlMetadata(MessageURLMetadata urlMetadata) {
        this.urlMetadata = urlMetadata;
    }

    /**
     * Updates the experiment identifier.
     *
     * @param fbExperimentId the new identifier, or {@code null} to clear
     */
    public void setFbExperimentId(Integer fbExperimentId) {
        this.fbExperimentId = fbExperimentId;
    }

    /**
     * Updates the preview media duration.
     *
     * @param linkMediaDuration the new duration in seconds, or {@code null} to clear
     */
    public void setLinkMediaDuration(Integer linkMediaDuration) {
        this.linkMediaDuration = linkMediaDuration;
    }

    /**
     * Updates the social media post classification.
     *
     * @param socialMediaPostType the new post type, or {@code null} to clear
     */
    public void setSocialMediaPostType(SocialMediaPostType socialMediaPostType) {
        this.socialMediaPostType = socialMediaPostType;
    }

    /**
     * Updates the inline video muted flag.
     *
     * @param linkInlineVideoMuted {@code true} if muted, {@code false} or {@code null} otherwise
     */
    public void setLinkInlineVideoMuted(Boolean linkInlineVideoMuted) {
        this.linkInlineVideoMuted = linkInlineVideoMuted;
    }

    /**
     * Updates the URL of the inline video content.
     *
     * @param videoContentUrl the new URL, or {@code null} to clear
     */
    public void setVideoContentUrl(String videoContentUrl) {
        this.videoContentUrl = videoContentUrl;
    }

    /**
     * Updates the music metadata.
     *
     * @param musicMetadata the new metadata, or {@code null} to clear
     */
    public void setMusicMetadata(EmbeddedMusic musicMetadata) {
        this.musicMetadata = musicMetadata;
    }

    /**
     * Updates the inline video caption.
     *
     * @param videoContentCaption the new caption, or {@code null} to clear
     */
    public void setVideoContentCaption(String videoContentCaption) {
        this.videoContentCaption = videoContentCaption;
    }

    /**
     * Classification of previews that originate from a social media platform.
     *
     * <p>The classification controls how the preview card is rendered (for example
     * as a carousel or as an inline video).
     */
    @ProtobufEnum(name = "Message.LinkPreviewMetadata.SocialMediaPostType")
    public static enum SocialMediaPostType {
        /**
         * The link is not a recognized social media post.
         */
        NONE(0),
        /**
         * The link is a short-form vertical video (reel).
         */
        REEL(1),
        /**
         * The link is a live video stream.
         */
        LIVE_VIDEO(2),
        /**
         * The link is a standard long-form video.
         */
        LONG_VIDEO(3),
        /**
         * The link is a single image post.
         */
        SINGLE_IMAGE(4),
        /**
         * The link is a multi-image carousel post.
         */
        CAROUSEL(5);

        /**
         * Constructs a new enum constant.
         *
         * @param index the protobuf wire index used to serialize this constant
         */
        SocialMediaPostType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Protobuf wire index of this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the index
         */
        public int index() {
            return this.index;
        }
    }
}

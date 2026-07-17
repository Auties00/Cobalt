package com.github.auties00.cobalt.wire.linked.bot.rendering;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Mutation data for incrementally updating a unified rich response from the AI
 * bot as it streams in.
 *
 * <p>This type is used in scenarios where the bot response is delivered
 * progressively. It carries:
 * <ul>
 * <li>{@linkplain #sideBySideMetadata() Side-by-side metadata} — for A/B testing
 *     where users compare two bot responses and select the better one.
 * <li>{@linkplain #mediaDetailsMetadataList() Media details} — high-resolution
 *     and preview variants of bot-generated media content.
 * </ul>
 *
 * <p>This type is referenced from
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata BotMetadata} as the
 * {@code unifiedResponseMutation} field (protobuf index 28).
 */
@ProtobufMessage(name = "BotUnifiedResponseMutation")
public final class BotUnifiedResponseMutation {
    /**
     * Metadata for the side-by-side A/B test comparison of bot responses.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    SideBySideMetadata sideBySideMetadata;

    /**
     * The list of media detail entries, each containing high-resolution and
     * preview variants of a bot-generated media item.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<MediaDetailsMetadata> mediaDetailsMetadataList;


    /**
     * Constructs a new {@code BotUnifiedResponseMutation} with the specified values.
     *
     * @param sideBySideMetadata        the side-by-side comparison metadata, or {@code null}
     * @param mediaDetailsMetadataList  the media details list, or {@code null}
     */
    BotUnifiedResponseMutation(SideBySideMetadata sideBySideMetadata, List<MediaDetailsMetadata> mediaDetailsMetadataList) {
        this.sideBySideMetadata = sideBySideMetadata;
        this.mediaDetailsMetadataList = mediaDetailsMetadataList;
    }

    /**
     * Returns the side-by-side A/B test comparison metadata.
     *
     * @return an {@code Optional} describing the side-by-side metadata, or an
     *         empty {@code Optional} if not set
     */
    public Optional<SideBySideMetadata> sideBySideMetadata() {
        return Optional.ofNullable(sideBySideMetadata);
    }

    /**
     * Returns an unmodifiable view of the media details list.
     *
     * @return the list of media detail entries, never {@code null}
     */
    public List<MediaDetailsMetadata> mediaDetailsMetadataList() {
        return mediaDetailsMetadataList == null ? List.of() : Collections.unmodifiableList(mediaDetailsMetadataList);
    }

    /**
     * Sets the side-by-side A/B test comparison metadata.
     *
     * @param sideBySideMetadata the new side-by-side metadata, or {@code null}
     */
    public void setSideBySideMetadata(SideBySideMetadata sideBySideMetadata) {
        this.sideBySideMetadata = sideBySideMetadata;
    }

    /**
     * Sets the list of media detail entries.
     *
     * @param mediaDetailsMetadataList the new media details list, or {@code null}
     */
    public void setMediaDetailsMetadataList(List<MediaDetailsMetadata> mediaDetailsMetadataList) {
        this.mediaDetailsMetadataList = mediaDetailsMetadataList;
    }

    /**
     * Metadata for a single media item within a unified bot response, carrying
     * both a high-resolution and a preview (thumbnail) variant.
     */
    @ProtobufMessage(name = "BotUnifiedResponseMutation.MediaDetailsMetadata")
    public static final class MediaDetailsMetadata {
        /**
         * The unique identifier for this media item within the response, for
         * example {@code "img_001"}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String id;

        /**
         * The high-resolution variant of this media item.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        BotMediaMetadata highResMedia;

        /**
         * The preview (thumbnail) variant of this media item.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        BotMediaMetadata previewMedia;


        /**
         * Constructs a new {@code MediaDetailsMetadata} with the specified values.
         *
         * @param id           the media item identifier, or {@code null}
         * @param highResMedia the high-resolution media metadata, or {@code null}
         * @param previewMedia the preview media metadata, or {@code null}
         */
        MediaDetailsMetadata(String id, BotMediaMetadata highResMedia, BotMediaMetadata previewMedia) {
            this.id = id;
            this.highResMedia = highResMedia;
            this.previewMedia = previewMedia;
        }

        /**
         * Returns the unique identifier for this media item.
         *
         * @return an {@code Optional} describing the media identifier, or an
         *         empty {@code Optional} if not set
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the high-resolution variant of this media item.
         *
         * @return an {@code Optional} describing the high-resolution metadata,
         *         or an empty {@code Optional} if not set
         */
        public Optional<BotMediaMetadata> highResMedia() {
            return Optional.ofNullable(highResMedia);
        }

        /**
         * Returns the preview (thumbnail) variant of this media item.
         *
         * @return an {@code Optional} describing the preview metadata, or an
         *         empty {@code Optional} if not set
         */
        public Optional<BotMediaMetadata> previewMedia() {
            return Optional.ofNullable(previewMedia);
        }

        /**
         * Sets the unique identifier for this media item.
         *
         * @param id the new media identifier, or {@code null}
         */
        public void setId(String id) {
            this.id = id;
    }

        /**
         * Sets the high-resolution variant of this media item.
         *
         * @param highResMedia the new high-resolution metadata, or {@code null}
         */
        public void setHighResMedia(BotMediaMetadata highResMedia) {
            this.highResMedia = highResMedia;
    }

        /**
         * Sets the preview (thumbnail) variant of this media item.
         *
         * @param previewMedia the new preview metadata, or {@code null}
         */
        public void setPreviewMedia(BotMediaMetadata previewMedia) {
            this.previewMedia = previewMedia;
    }
    }

    /**
     * Metadata for the side-by-side A/B testing feature where users compare
     * two bot responses and select the one they prefer.
     */
    @ProtobufMessage(name = "BotUnifiedResponseMutation.SideBySideMetadata")
    public static final class SideBySideMetadata {
        /**
         * The identifier of the primary response in the side-by-side
         * comparison, for example {@code "resp_primary_abc123"}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String primaryResponseId;

        /**
         * Whether the survey call-to-action prompting the user to compare
         * the two responses has been rendered in the UI.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
        Boolean surveyCallToActionHasRendered;


        /**
         * Constructs a new {@code SideBySideMetadata} with the specified values.
         *
         * @param primaryResponseId             the primary response identifier, or {@code null}
         * @param surveyCallToActionHasRendered  whether the survey CTA has been rendered, or {@code null}
         */
        SideBySideMetadata(String primaryResponseId, Boolean surveyCallToActionHasRendered) {
            this.primaryResponseId = primaryResponseId;
            this.surveyCallToActionHasRendered = surveyCallToActionHasRendered;
        }

        /**
         * Returns the identifier of the primary response in the comparison.
         *
         * @return an {@code Optional} describing the primary response identifier,
         *         or an empty {@code Optional} if not set
         */
        public Optional<String> primaryResponseId() {
            return Optional.ofNullable(primaryResponseId);
        }

        /**
         * Returns whether the survey call-to-action has been rendered.
         *
         * @return {@code true} if the survey CTA has been rendered,
         *         {@code false} otherwise
         */
        public boolean surveyCallToActionHasRendered() {
            return surveyCallToActionHasRendered != null && surveyCallToActionHasRendered;
        }

        /**
         * Sets the identifier of the primary response in the comparison.
         *
         * @param primaryResponseId the new primary response identifier, or {@code null}
         */
        public void setPrimaryResponseId(String primaryResponseId) {
            this.primaryResponseId = primaryResponseId;
    }

        /**
         * Sets whether the survey call-to-action has been rendered.
         *
         * @param surveyCallToActionHasRendered whether the survey CTA has been rendered, or {@code null}
         */
        public void setSurveyCallToActionHasRendered(Boolean surveyCallToActionHasRendered) {
            this.surveyCallToActionHasRendered = surveyCallToActionHasRendered;
    }
    }
}

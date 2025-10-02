
package it.auties.whatsapp.api;

import it.auties.linkpreview.LinkPreview;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.standard.TextMessage;

import java.util.Objects;

/**
 * A functional interface that handles link preview generation for WhatsApp messages.
 * <p>
 * This interface provides mechanisms to automatically detect URLs in messages and enrich them
 * with preview metadata such as titles, descriptions, and thumbnails.
 *
 * <p>The handler processes {@link Message} instances by:
 * <ul>
 *   <li>Scanning the message text for URLs</li>
 *   <li>Fetching preview metadata (title, description, images, videos)</li>
 *   <li>Optionally replacing detected text with canonical URLs</li>
 *   <li>Setting thumbnail images from the largest available media</li>
 *   <li>Configuring appropriate preview types (video or none)</li>
 * </ul>
 *
 * @see Message
 * @see TextMessage
 * @see LinkPreview
 */
public interface WhatsappMessagePreviewHandler {

    /**
     * Creates an enabled preview handler that processes messages to generate link previews.
     * <p>
     * This implementation:
     * <ul>
     *   <li>Uses the LinkPreview library to detect and process URLs in message text</li>
     *   <li>Extracts metadata including title, description, and media thumbnails</li>
     *   <li>Sets the largest available image as the message thumbnail</li>
     *   <li>Configures video preview type for video content, or none for other content</li>
     *   <li>Handles network errors gracefully by ignoring failed thumbnail downloads</li>
     * </ul>
     *
     * <p>Non-text messages are ignored and left unmodified.
     *
     * @param allowInference when {@code true}, allows the handler to replace detected URL text
     *                      with canonical URLs if they differ. When {@code false}, preserves
     *                      the original message text unchanged.
     * @return a non-null preview handler that processes messages for link previews
     *
     * @apiNote The handler selects thumbnails based on resolution (width × height), choosing
     *         the largest available image or video thumbnail. Network failures during thumbnail
     *         download are silently ignored to prevent message processing interruption.
     */
    static WhatsappMessagePreviewHandler enabled(boolean allowInference) {
        return message -> {
            if(!(message instanceof TextMessage textMessage)) {
                return;
            }

            var preview = LinkPreview.createPreview(textMessage.text());
            preview.ifPresent(match -> {
                var uri = match.result().toString();
                if (allowInference && !Objects.equals(match.text(), uri)) {
                    textMessage.setText(textMessage.text().replace(match.text(), uri));
                }
                textMessage.setMatchedText(uri);

                textMessage.setTitle(match.result().title());
                textMessage.setDescription(match.result().siteDescription());

                match.result()
                        .images()
                        .stream()
                        .reduce((first, second) -> first.width() * first.height() > second.width() * second.height() ? first : second)
                        .ifPresent(media -> {
                            try (var stream = media.uri().toURL().openStream()) {
                                textMessage.setThumbnail(stream.readAllBytes());
                            } catch (Throwable ignored) {

                            }
                            textMessage.setThumbnailWidth(media.width());
                            textMessage.setThumbnailHeight(media.height());
                        });

                match.result()
                        .videos()
                        .stream()
                        .reduce((first, second) -> first.width() * first.height() > second.width() * second.height() ? first : second)
                        .ifPresentOrElse(
                                media -> {
                                    textMessage.setCanonicalUrl(media.uri().toString());
                                    textMessage.setThumbnailWidth(media.width());
                                    textMessage.setThumbnailHeight(media.height());
                                    textMessage.setPreviewType(TextMessage.PreviewType.VIDEO);
                                },
                                () -> {
                                    textMessage.setCanonicalUrl(match.result().uri().toString());
                                    textMessage.setPreviewType(TextMessage.PreviewType.NONE);
                                }
                        );
            });
        };
    }

    /**
     * Creates a disabled preview handler that performs no processing on messages.
     * <p>
     * This is a no-op implementation that leaves messages unchanged, effectively
     * disabling link preview generation. Use this when you want to handle link previews
     * manually or disable the feature entirely.
     *
     * @return a non-null preview handler that performs no operations
     */
    static WhatsappMessagePreviewHandler disable() {
        return _ -> {};
    }

    /**
     * Processes a message to add link preview attributes.
     * <p>
     * Implementations should examine the message for URLs and populate relevant
     * preview fields such as title, description, thumbnail, and canonical URL.
     * The method should handle different message types appropriately, typically
     * focusing on text-based messages that support link previews.
     *
     * <p>The method should handle network failures gracefully and avoid throwing exceptions
     * that could interrupt message processing.
     *
     * <p>Common processing steps include:
     * <ul>
     *   <li>Checking if the message type supports link previews</li>
     *   <li>Detecting URLs in the message content</li>
     *   <li>Fetching metadata from detected URLs</li>
     *   <li>Setting preview fields on the message object</li>
     *   <li>Downloading and setting thumbnail images</li>
     *   <li>Configuring appropriate preview types</li>
     * </ul>
     *
     * @param message the message to process for link previews, must not be null.
     *               The message object may be modified in-place with preview data
     *               if it supports link previews (e.g., {@link TextMessage}).
     */
    void attribute(Message message);
}
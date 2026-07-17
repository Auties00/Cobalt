package com.github.auties00.cobalt.wire.linked.message.interactive;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents a mapping between URLs appearing inside an {@link InteractiveMessage} and
 * their consent-aware rewritten variants.
 *
 * <p>The tracking map lets the server rewrite every URL surfaced in an interactive message
 * depending on whether the recipient has granted consent for click tracking. Each entry
 * pairs the original URL with two alternatives: one for unconsented users and one for
 * consented users. When the message contains a carousel, the optional card index identifies
 * which card the rewrite applies to.
 */
@ProtobufMessage(name = "UrlTrackingMap")
public final class UrlTrackingMap {
    /**
     * The individual URL rewrite entries that make up this tracking map.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<UrlTrackingMapElement> urlTrackingMapElements;


    /**
     * Constructs a new tracking map with the supplied entries.
     *
     * @param urlTrackingMapElements the rewrite entries, possibly {@code null}
     */
    UrlTrackingMap(List<UrlTrackingMapElement> urlTrackingMapElements) {
        this.urlTrackingMapElements = urlTrackingMapElements;
    }

    /**
     * Returns an unmodifiable view of the URL rewrite entries that make up this map.
     *
     * @return the list of entries, or an empty list if none were set
     */
    public List<UrlTrackingMapElement> urlTrackingMapElements() {
        return urlTrackingMapElements == null ? List.of() : Collections.unmodifiableList(urlTrackingMapElements);
    }

    /**
     * Updates the list of URL rewrite entries.
     *
     * @param urlTrackingMapElements the new entries, or {@code null} to clear the field
     */
    public void setUrlTrackingMapElements(List<UrlTrackingMapElement> urlTrackingMapElements) {
        this.urlTrackingMapElements = urlTrackingMapElements;
    }

    /**
     * Represents a single URL rewrite entry inside a {@link UrlTrackingMap}.
     *
     * <p>Each entry maps an original URL to two consent-gated rewrites and optionally
     * identifies the carousel card that carries the URL, allowing the client to apply the
     * right rewrite to each card independently.
     */
    @ProtobufMessage(name = "UrlTrackingMap.UrlTrackingMapElement")
    public static final class UrlTrackingMapElement {
        /**
         * The original URL as it appears in the message body.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String originalUrl;

        /**
         * The rewritten URL used when the recipient has not granted consent for click
         * tracking.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String unconsentedUsersUrl;

        /**
         * The rewritten URL used when the recipient has granted consent for click tracking.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String consentedUsersUrl;

        /**
         * The zero-based index of the carousel card that contains the URL, when applicable.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
        Integer cardIndex;


        /**
         * Constructs a new URL rewrite entry with the supplied fields.
         *
         * @param originalUrl         the original URL, possibly {@code null}
         * @param unconsentedUsersUrl the rewrite for unconsented users, possibly {@code null}
         * @param consentedUsersUrl   the rewrite for consented users, possibly {@code null}
         * @param cardIndex           the carousel card index, possibly {@code null}
         */
        UrlTrackingMapElement(String originalUrl, String unconsentedUsersUrl, String consentedUsersUrl, Integer cardIndex) {
            this.originalUrl = originalUrl;
            this.unconsentedUsersUrl = unconsentedUsersUrl;
            this.consentedUsersUrl = consentedUsersUrl;
            this.cardIndex = cardIndex;
        }

        /**
         * Returns the original URL as it appears in the message body.
         *
         * @return an {@code Optional} with the original URL, or empty if not set
         */
        public Optional<String> originalUrl() {
            return Optional.ofNullable(originalUrl);
        }

        /**
         * Returns the rewritten URL used for unconsented users.
         *
         * @return an {@code Optional} with the rewritten URL, or empty if not set
         */
        public Optional<String> unconsentedUsersUrl() {
            return Optional.ofNullable(unconsentedUsersUrl);
        }

        /**
         * Returns the rewritten URL used for consented users.
         *
         * @return an {@code Optional} with the rewritten URL, or empty if not set
         */
        public Optional<String> consentedUsersUrl() {
            return Optional.ofNullable(consentedUsersUrl);
        }

        /**
         * Returns the zero-based index of the carousel card that contains this URL.
         *
         * @return an {@code OptionalInt} with the card index, or empty if not set
         */
        public OptionalInt cardIndex() {
            return cardIndex == null ? OptionalInt.empty() : OptionalInt.of(cardIndex);
        }

        /**
         * Updates the original URL.
         *
         * @param originalUrl the new URL, or {@code null} to clear the field
         */
        public void setOriginalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
    }

        /**
         * Updates the rewritten URL used for unconsented users.
         *
         * @param unconsentedUsersUrl the new URL, or {@code null} to clear the field
         */
        public void setUnconsentedUsersUrl(String unconsentedUsersUrl) {
            this.unconsentedUsersUrl = unconsentedUsersUrl;
    }

        /**
         * Updates the rewritten URL used for consented users.
         *
         * @param consentedUsersUrl the new URL, or {@code null} to clear the field
         */
        public void setConsentedUsersUrl(String consentedUsersUrl) {
            this.consentedUsersUrl = consentedUsersUrl;
    }

        /**
         * Updates the zero-based index of the carousel card that contains this URL.
         *
         * @param cardIndex the new card index, or {@code null} to clear the field
         */
        public void setCardIndex(Integer cardIndex) {
            this.cardIndex = cardIndex;
    }
    }
}

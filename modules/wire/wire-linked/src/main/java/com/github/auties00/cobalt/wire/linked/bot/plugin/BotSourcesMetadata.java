package com.github.auties00.cobalt.wire.linked.bot.plugin;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Contains the source attributions (citations) for a Meta AI bot's rich
 * response content.
 *
 * <p>When the Meta AI bot generates a response that references external web
 * sources, this metadata carries the list of {@link BotSourceItem} entries
 * representing each cited source. Clients use these entries to render
 * footnote-style citations (e.g. [1], [2]) with clickable links to the
 * original web pages, along with provider favicons and thumbnails for visual
 * source identification.
 *
 * @see BotPluginMetadata
 */
@ProtobufMessage(name = "BotSourcesMetadata")
public final class BotSourcesMetadata {
    /**
     * The list of web sources that the bot cited when generating its response,
     * each containing a URL, title, provider, and citation number.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<BotSourceItem> sources;

    /**
     * Constructs a new {@code BotSourcesMetadata} with the specified list of
     * source citations.
     *
     * @param sources the list of cited source items, or {@code null} if no
     *                sources are cited
     */
    BotSourcesMetadata(List<BotSourceItem> sources) {
        this.sources = sources;
    }

    /**
     * Returns the list of web sources cited in the bot response.
     *
     * @return an unmodifiable list of {@link BotSourceItem} entries, or an
     *         empty list if no sources are cited
     */
    public List<BotSourceItem> sources() {
        return sources == null ? List.of() : Collections.unmodifiableList(sources);
    }

    /**
     * Sets the list of web sources cited in the bot response.
     *
     * @param sources the new list of source items, or {@code null} to clear
     *                all citations
     */
    public void setSources(List<BotSourceItem> sources) {
        this.sources = sources;
    }

    /**
     * Represents a single source citation within a Meta AI bot response,
     * identifying the web page or resource that the bot referenced when
     * generating its answer. Each item carries the source URL, title, search
     * provider, favicon, thumbnail, and a citation number for inline
     * footnote-style display.
     */
    @ProtobufMessage(name = "BotSourcesMetadata.BotSourceItem")
    public static final class BotSourceItem {
        /**
         * The search provider that returned this source result, such as Bing,
         * Google, or a support service.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        BotSourceItem.SourceProvider provider;

        /**
         * The CDN URL of a thumbnail image for this source, used for visual
         * preview in the citation display.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        URI thumbnailCdnUrl;

        /**
         * The URL of the original source page that the bot referenced, which
         * the user can visit for the full content.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        URI sourceProviderUrl;

        /**
         * The search query string that was used to discover this source.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String sourceQuery;

        /**
         * The CDN URL of the source site's favicon, displayed alongside the
         * citation for visual identification of the source domain.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        URI faviconCdnUrl;

        /**
         * The citation number displayed inline in the bot response text (for
         * example, [1] or [2]), linking the in-text reference to this source.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
        Integer citationNumber;

        /**
         * The title of the source page, displayed as the clickable label in
         * the citation list.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        String sourceTitle;

        /**
         * Constructs a new {@code BotSourceItem} with the specified values.
         *
         * @param provider          the search provider that returned this
         *                          source, or {@code null}
         * @param thumbnailCdnUrl   the CDN URL of the thumbnail image, or
         *                          {@code null}
         * @param sourceProviderUrl the URL of the original source page, or
         *                          {@code null}
         * @param sourceQuery       the search query that discovered this
         *                          source, or {@code null}
         * @param faviconCdnUrl     the CDN URL of the source site's favicon,
         *                          or {@code null}
         * @param citationNumber    the inline citation number, or {@code null}
         * @param sourceTitle       the title of the source page, or
         *                          {@code null}
         */
        BotSourceItem(SourceProvider provider, URI thumbnailCdnUrl, URI sourceProviderUrl, String sourceQuery, URI faviconCdnUrl, Integer citationNumber, String sourceTitle) {
            this.provider = provider;
            this.thumbnailCdnUrl = thumbnailCdnUrl;
            this.sourceProviderUrl = sourceProviderUrl;
            this.sourceQuery = sourceQuery;
            this.faviconCdnUrl = faviconCdnUrl;
            this.citationNumber = citationNumber;
            this.sourceTitle = sourceTitle;
        }

        /**
         * Returns the search provider that returned this source.
         *
         * @return an {@link Optional} describing the provider, or an empty
         *         {@code Optional} if not set
         */
        public Optional<SourceProvider> provider() {
            return Optional.ofNullable(provider);
        }

        /**
         * Returns the CDN URL of the thumbnail image for this source.
         *
         * @return an {@link Optional} describing the thumbnail URL, or an
         *         empty {@code Optional} if not set
         */
        public Optional<URI> thumbnailCdnUrl() {
            return Optional.ofNullable(thumbnailCdnUrl);
        }

        /**
         * Returns the URL of the original source page.
         *
         * @return an {@link Optional} describing the source URL, or an empty
         *         {@code Optional} if not set
         */
        public Optional<URI> sourceProviderUrl() {
            return Optional.ofNullable(sourceProviderUrl);
        }

        /**
         * Returns the search query string that was used to discover this
         * source.
         *
         * @return an {@link Optional} describing the query, or an empty
         *         {@code Optional} if not set
         */
        public Optional<String> sourceQuery() {
            return Optional.ofNullable(sourceQuery);
        }

        /**
         * Returns the CDN URL of the source site's favicon.
         *
         * @return an {@link Optional} describing the favicon URL, or an empty
         *         {@code Optional} if not set
         */
        public Optional<URI> faviconCdnUrl() {
            return Optional.ofNullable(faviconCdnUrl);
        }

        /**
         * Returns the inline citation number displayed in the bot response
         * text.
         *
         * @return an {@link OptionalInt} describing the citation number, or an
         *         empty {@code OptionalInt} if not set
         */
        public OptionalInt citationNumber() {
            return citationNumber == null ? OptionalInt.empty() : OptionalInt.of(citationNumber);
        }

        /**
         * Returns the title of the source page.
         *
         * @return an {@link Optional} describing the title, or an empty
         *         {@code Optional} if not set
         */
        public Optional<String> sourceTitle() {
            return Optional.ofNullable(sourceTitle);
        }

        /**
         * Sets the search provider that returned this source.
         *
         * @param provider the new provider, or {@code null} to clear
         */
        public void setProvider(SourceProvider provider) {
            this.provider = provider;
    }

        /**
         * Sets the CDN URL of the thumbnail image for this source.
         *
         * @param thumbnailCdnUrl the new thumbnail URL, or {@code null} to
         *                        clear
         */
        public void setThumbnailCdnUrl(URI thumbnailCdnUrl) {
            this.thumbnailCdnUrl = thumbnailCdnUrl;
    }

        /**
         * Sets the URL of the original source page.
         *
         * @param sourceProviderUrl the new source URL, or {@code null} to
         *                          clear
         */
        public void setSourceProviderUrl(URI sourceProviderUrl) {
            this.sourceProviderUrl = sourceProviderUrl;
    }

        /**
         * Sets the search query string that was used to discover this source.
         *
         * @param sourceQuery the new query, or {@code null} to clear
         */
        public void setSourceQuery(String sourceQuery) {
            this.sourceQuery = sourceQuery;
    }

        /**
         * Sets the CDN URL of the source site's favicon.
         *
         * @param faviconCdnUrl the new favicon URL, or {@code null} to clear
         */
        public void setFaviconCdnUrl(URI faviconCdnUrl) {
            this.faviconCdnUrl = faviconCdnUrl;
    }

        /**
         * Sets the inline citation number displayed in the bot response text.
         *
         * @param citationNumber the new citation number, or {@code null} to
         *                       clear
         */
        public void setCitationNumber(Integer citationNumber) {
            this.citationNumber = citationNumber;
    }

        /**
         * Sets the title of the source page.
         *
         * @param sourceTitle the new title, or {@code null} to clear
         */
        public void setSourceTitle(String sourceTitle) {
            this.sourceTitle = sourceTitle;
    }

        /**
         * Enumerates the search providers that can return source citations
         * within a Meta AI bot response.
         */
        @ProtobufEnum(name = "BotSourcesMetadata.BotSourceItem.SourceProvider")
        public static enum SourceProvider {
            /**
             * An unknown or unrecognized source provider.
             */
            UNKNOWN(0),

            /**
             * Microsoft Bing search.
             */
            BING(1),

            /**
             * Google search.
             */
            GOOGLE(2),

            /**
             * A support/help-center search provider.
             */
            SUPPORT(3),

            /**
             * A source provider that is not covered by the other named
             * constants.
             */
            OTHER(4);

            /**
             * Constructs a new source provider constant.
             *
             * @param index the protobuf-assigned numeric index for this
             *              constant
             */
            SourceProvider(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf-assigned numeric index for this constant.
             */
            final int index;

            /**
             * Returns the protobuf-assigned numeric index for this constant.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }
    }
}

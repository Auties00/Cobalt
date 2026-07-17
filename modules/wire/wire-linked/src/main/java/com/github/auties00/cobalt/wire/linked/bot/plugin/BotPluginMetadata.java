package com.github.auties00.cobalt.wire.linked.bot.plugin;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Describes a bot plugin that contributed to a Meta AI bot response, such as a
 * web search plugin or an Instagram Reels integration.
 *
 * <p>When the Meta AI bot invokes an external plugin to answer a user query,
 * this metadata accompanies the reply. It identifies which
 * {@link SearchProvider} fulfilled the request, the {@link PluginType} used,
 * the search query that was executed, and various URLs for thumbnails,
 * provider profile photos, favicons, and result pages. Clients use this
 * information to render attribution UI elements alongside the bot response.
 *
 * <p>Plugin responses can form chains: a follow-up or drill-down result
 * references its predecessor via {@link #parentPluginMessageKey()} and
 * {@link #parentPluginType()}.
 *
 * @see BotSourcesMetadata
 */
@ProtobufMessage(name = "BotPluginMetadata")
public final class BotPluginMetadata {
    /**
     * The external search provider that fulfilled this plugin request, such as
     * Bing, Google, or a support/help-center service.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    SearchProvider provider;

    /**
     * The type of plugin that generated this response, indicating whether it
     * was a web search, Reels integration, or another plugin category.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    PluginType pluginType;

    /**
     * The CDN URL of the thumbnail image for this plugin result, typically
     * used for Reels or image-rich search results.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    URI thumbnailCdnUrl;

    /**
     * The CDN URL of the search provider's profile photo, displayed alongside
     * the attribution to identify the source visually.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    URI profilePhotoCdnUrl;

    /**
     * The URL of the search provider's result page that the user can visit for
     * the full search results.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    URI searchProviderUrl;

    /**
     * The zero-based position of this plugin result within the ordered list of
     * results returned by the search provider.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    Integer referenceIndex;

    /**
     * The expected number of citation links embedded in this plugin result,
     * allowing the client to pre-allocate UI space for source attributions.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.UINT32)
    Integer expectedLinksCount;

    /**
     * The search query string that was sent to the search provider. For
     * example, {@code "weather in New York today"}.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String searchQuery;

    /**
     * The message key of the parent plugin message that this result is a
     * follow-up or drill-down from, enabling plugin response chaining.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    MessageKey parentPluginMessageKey;

    /**
     * A deprecated field that previously held the plugin type. Use
     * {@link #pluginType()} instead.
     *
     * @deprecated superseded by {@link #pluginType()}
     */
    @ProtobufProperty(index = 11, type = ProtobufType.ENUM)
    PluginType deprecatedField;

    /**
     * The plugin type of the parent message, when this result is a follow-up
     * to a previous plugin response.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.ENUM)
    PluginType parentPluginType;

    /**
     * The CDN URL of the search provider's favicon, displayed alongside the
     * plugin result for visual source identification.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    URI faviconCdnUrl;

    /**
     * Constructs a new {@code BotPluginMetadata} with the specified values.
     *
     * @param provider               the search provider that fulfilled the
     *                               request, or {@code null}
     * @param pluginType             the type of plugin used, or {@code null}
     * @param thumbnailCdnUrl        the thumbnail CDN URL, or {@code null}
     * @param profilePhotoCdnUrl     the provider's profile photo CDN URL, or
     *                               {@code null}
     * @param searchProviderUrl      the URL of the provider's result page, or
     *                               {@code null}
     * @param referenceIndex         the zero-based result index, or
     *                               {@code null}
     * @param expectedLinksCount     the expected number of citation links, or
     *                               {@code null}
     * @param searchQuery            the search query string, or {@code null}
     * @param parentPluginMessageKey the key of the parent plugin message, or
     *                               {@code null}
     * @param deprecatedField        the deprecated plugin type field, or
     *                               {@code null}
     * @param parentPluginType       the plugin type of the parent message, or
     *                               {@code null}
     * @param faviconCdnUrl          the provider's favicon CDN URL, or
     *                               {@code null}
     */
    BotPluginMetadata(SearchProvider provider, PluginType pluginType, URI thumbnailCdnUrl, URI profilePhotoCdnUrl, URI searchProviderUrl, Integer referenceIndex, Integer expectedLinksCount, String searchQuery, MessageKey parentPluginMessageKey, PluginType deprecatedField, PluginType parentPluginType, URI faviconCdnUrl) {
        this.provider = provider;
        this.pluginType = pluginType;
        this.thumbnailCdnUrl = thumbnailCdnUrl;
        this.profilePhotoCdnUrl = profilePhotoCdnUrl;
        this.searchProviderUrl = searchProviderUrl;
        this.referenceIndex = referenceIndex;
        this.expectedLinksCount = expectedLinksCount;
        this.searchQuery = searchQuery;
        this.parentPluginMessageKey = parentPluginMessageKey;
        this.deprecatedField = deprecatedField;
        this.parentPluginType = parentPluginType;
        this.faviconCdnUrl = faviconCdnUrl;
    }

    /**
     * Returns the external search provider that fulfilled this plugin request.
     *
     * @return an {@link Optional} describing the search provider, or an empty
     *         {@code Optional} if not set
     */
    public Optional<SearchProvider> provider() {
        return Optional.ofNullable(provider);
    }

    /**
     * Returns the type of plugin that generated this response.
     *
     * @return an {@link Optional} describing the plugin type, or an empty
     *         {@code Optional} if not set
     */
    public Optional<PluginType> pluginType() {
        return Optional.ofNullable(pluginType);
    }

    /**
     * Returns the CDN URL of the thumbnail image for this plugin result.
     *
     * @return an {@link Optional} describing the thumbnail URL, or an empty
     *         {@code Optional} if not set
     */
    public Optional<URI> thumbnailCdnUrl() {
        return Optional.ofNullable(thumbnailCdnUrl);
    }

    /**
     * Returns the CDN URL of the search provider's profile photo.
     *
     * @return an {@link Optional} describing the profile photo URL, or an
     *         empty {@code Optional} if not set
     */
    public Optional<URI> profilePhotoCdnUrl() {
        return Optional.ofNullable(profilePhotoCdnUrl);
    }

    /**
     * Returns the URL of the search provider's full result page.
     *
     * @return an {@link Optional} describing the search provider URL, or an
     *         empty {@code Optional} if not set
     */
    public Optional<URI> searchProviderUrl() {
        return Optional.ofNullable(searchProviderUrl);
    }

    /**
     * Returns the zero-based position of this plugin result within the ordered
     * list of results returned by the search provider.
     *
     * @return an {@link OptionalInt} describing the reference index, or an
     *         empty {@code OptionalInt} if not set
     */
    public OptionalInt referenceIndex() {
        return referenceIndex == null ? OptionalInt.empty() : OptionalInt.of(referenceIndex);
    }

    /**
     * Returns the expected number of citation links embedded in this plugin
     * result.
     *
     * @return an {@link OptionalInt} describing the links count, or an empty
     *         {@code OptionalInt} if not set
     */
    public OptionalInt expectedLinksCount() {
        return expectedLinksCount == null ? OptionalInt.empty() : OptionalInt.of(expectedLinksCount);
    }

    /**
     * Returns the search query string that was sent to the search provider.
     *
     * @return an {@link Optional} describing the search query, or an empty
     *         {@code Optional} if not set
     */
    public Optional<String> searchQuery() {
        return Optional.ofNullable(searchQuery);
    }

    /**
     * Returns the message key of the parent plugin message that this result is
     * a follow-up from.
     *
     * @return an {@link Optional} describing the parent message key, or an
     *         empty {@code Optional} if this is not a follow-up result
     */
    public Optional<MessageKey> parentPluginMessageKey() {
        return Optional.ofNullable(parentPluginMessageKey);
    }

    /**
     * Returns the deprecated plugin type field.
     *
     * @return an {@link Optional} describing the deprecated plugin type, or an
     *         empty {@code Optional} if not set
     * @deprecated superseded by {@link #pluginType()}
     */
    public Optional<PluginType> deprecatedField() {
        return Optional.ofNullable(deprecatedField);
    }

    /**
     * Returns the plugin type of the parent message in a follow-up chain.
     *
     * @return an {@link Optional} describing the parent plugin type, or an
     *         empty {@code Optional} if this is not a follow-up result
     */
    public Optional<PluginType> parentPluginType() {
        return Optional.ofNullable(parentPluginType);
    }

    /**
     * Returns the CDN URL of the search provider's favicon.
     *
     * @return an {@link Optional} describing the favicon URL, or an empty
     *         {@code Optional} if not set
     */
    public Optional<URI> faviconCdnUrl() {
        return Optional.ofNullable(faviconCdnUrl);
    }

    /**
     * Sets the external search provider that fulfilled this plugin request.
     *
     * @param provider the new search provider, or {@code null} to clear
     */
    public void setProvider(SearchProvider provider) {
        this.provider = provider;
    }

    /**
     * Sets the type of plugin that generated this response.
     *
     * @param pluginType the new plugin type, or {@code null} to clear
     */
    public void setPluginType(PluginType pluginType) {
        this.pluginType = pluginType;
    }

    /**
     * Sets the CDN URL of the thumbnail image for this plugin result.
     *
     * @param thumbnailCdnUrl the new thumbnail URL, or {@code null} to clear
     */
    public void setThumbnailCdnUrl(URI thumbnailCdnUrl) {
        this.thumbnailCdnUrl = thumbnailCdnUrl;
    }

    /**
     * Sets the CDN URL of the search provider's profile photo.
     *
     * @param profilePhotoCdnUrl the new profile photo URL, or {@code null} to
     *                           clear
     */
    public void setProfilePhotoCdnUrl(URI profilePhotoCdnUrl) {
        this.profilePhotoCdnUrl = profilePhotoCdnUrl;
    }

    /**
     * Sets the URL of the search provider's full result page.
     *
     * @param searchProviderUrl the new search provider URL, or {@code null} to
     *                          clear
     */
    public void setSearchProviderUrl(URI searchProviderUrl) {
        this.searchProviderUrl = searchProviderUrl;
    }

    /**
     * Sets the zero-based position of this plugin result in the ordered list.
     *
     * @param referenceIndex the new reference index, or {@code null} to clear
     */
    public void setReferenceIndex(Integer referenceIndex) {
        this.referenceIndex = referenceIndex;
    }

    /**
     * Sets the expected number of citation links in this plugin result.
     *
     * @param expectedLinksCount the new links count, or {@code null} to clear
     */
    public void setExpectedLinksCount(Integer expectedLinksCount) {
        this.expectedLinksCount = expectedLinksCount;
    }

    /**
     * Sets the search query string that was sent to the search provider.
     *
     * @param searchQuery the new search query, or {@code null} to clear
     */
    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    /**
     * Sets the message key of the parent plugin message in a follow-up chain.
     *
     * @param parentPluginMessageKey the new parent message key, or
     *                               {@code null} to clear
     */
    public void setParentPluginMessageKey(MessageKey parentPluginMessageKey) {
        this.parentPluginMessageKey = parentPluginMessageKey;
    }

    /**
     * Sets the deprecated plugin type field.
     *
     * @param deprecatedField the new deprecated plugin type, or {@code null}
     * @deprecated superseded by {@link #setPluginType(PluginType)}
     */
    public void setDeprecatedField(PluginType deprecatedField) {
        this.deprecatedField = deprecatedField;
    }

    /**
     * Sets the plugin type of the parent message in a follow-up chain.
     *
     * @param parentPluginType the new parent plugin type, or {@code null} to
     *                         clear
     */
    public void setParentPluginType(PluginType parentPluginType) {
        this.parentPluginType = parentPluginType;
    }

    /**
     * Sets the CDN URL of the search provider's favicon.
     *
     * @param faviconCdnUrl the new favicon URL, or {@code null} to clear
     */
    public void setFaviconCdnUrl(URI faviconCdnUrl) {
        this.faviconCdnUrl = faviconCdnUrl;
    }

    /**
     * Enumerates the types of bot plugins that can generate content within a
     * Meta AI bot response, such as web search or Instagram Reels integration.
     */
    @ProtobufEnum(name = "BotPluginMetadata.PluginType")
    public static enum PluginType {
        /**
         * An unknown or unrecognized plugin type.
         */
        UNKNOWN_PLUGIN(0),

        /**
         * An Instagram Reels integration plugin that surfaces short-form video
         * content in bot responses.
         */
        REELS(1),

        /**
         * A web search plugin that queries an external search provider (e.g.
         * Bing, Google) and returns results inline.
         */
        SEARCH(2);

        /**
         * Constructs a new plugin type constant.
         *
         * @param index the protobuf-assigned numeric index for this constant
         */
        PluginType(@ProtobufEnumIndex int index) {
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

    /**
     * Enumerates the external search providers that a bot plugin can use to
     * fulfill a query, such as Bing, Google, or a help-center service.
     */
    @ProtobufEnum(name = "BotPluginMetadata.SearchProvider")
    public static enum SearchProvider {
        /**
         * An unknown or unrecognized search provider.
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
        SUPPORT(3);

        /**
         * Constructs a new search provider constant.
         *
         * @param index the protobuf-assigned numeric index for this constant
         */
        SearchProvider(@ProtobufEnumIndex int index) {
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

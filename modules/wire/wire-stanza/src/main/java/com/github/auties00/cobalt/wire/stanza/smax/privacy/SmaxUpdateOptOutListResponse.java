package com.github.auties00.cobalt.wire.stanza.smax.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The sealed family of inbound replies to a {@link SmaxUpdateOptOutListRequest}, covering the marketing-messages
 * opt-out, opt-in, and signup actions.
 *
 * <p>The four variants match the four response shapes the relay can return: {@link SuccessWithMatch} confirms the
 * action against an up-to-date cache with a single echoed item, {@link SuccessWithMismatch} confirms the action and
 * returns the refreshed full list when the cache was stale, {@link ClientError} reports a malformed request, and
 * {@link ServerError} reports a transient relay failure. The {@link #of(Stanza, Stanza)} entry point dispatches an
 * inbound stanza onto the first matching variant.
 *
 * @implNote This implementation preserves WA Web's parser priority order in {@link #of(Stanza, Stanza)}: match first,
 * then mismatch, then the two error variants, so the cache-match short-circuit catches before the broader shapes.
 */
public sealed interface SmaxUpdateOptOutListResponse extends SmaxStanza.Response
        permits SmaxUpdateOptOutListResponse.SuccessWithMatch,
        SmaxUpdateOptOutListResponse.SuccessWithMismatch,
        SmaxUpdateOptOutListResponse.ClientError,
        SmaxUpdateOptOutListResponse.ServerError {

    /**
     * Dispatches the inbound stanza onto the first matching variant.
     *
     * <p>The variants are tried in priority order ({@link SuccessWithMatch}, {@link SuccessWithMismatch},
     * {@link ClientError}, {@link ServerError}); the first whose parser accepts the stanza wins. An empty result
     * signals that none of the four shapes matched the reply correlated to {@code request}.
     *
     * @param stanza    the inbound {@code <iq>} stanza; never {@code null}
     * @param request the original {@link SmaxUpdateOptOutListRequest} stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBlocklistsUpdateOptOutListRPC",
            exports = "sendUpdateOptOutListRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxUpdateOptOutListResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var successWithMatch = SuccessWithMatch.of(stanza, request);
        if (successWithMatch.isPresent()) {
            return successWithMatch;
        }
        var successWithMismatch = SuccessWithMismatch.of(stanza, request);
        if (successWithMismatch.isPresent()) {
            return successWithMismatch;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * One {@code <item>} entry in an opt-out reply, single-item for match replies and one of many for mismatch
     * replies.
     *
     * <p>Each entry carries the optional action, the optional marketing category, and the optional expiry plus the
     * mandatory {@link #bizOptOutIds()} discriminator distinguishing a brand id from a business JID. Entries are
     * surfaced from {@link SuccessWithMatch#item()} and {@link SuccessWithMismatch#listItem()}.
     *
     * @param action       the optional action marker; may be {@code null}
     * @param category     the optional marketing category; may be {@code null}
     * @param expiryAt     the optional non-negative expiry timestamp in seconds; may be {@code null}
     * @param bizOptOutIds the brand-id-versus-jid discriminator; never {@code null}
     */
    record Item(String action, String category, Long expiryAt, BizOptOutId bizOptOutIds) {
        /**
         * Validates the {@link Item} payload, rejecting a missing discriminator.
         *
         * <p>The action, category, and expiry components are optional and arrive pre-validated by
         * {@link SmaxUpdateOptOutListResponse#parseItem(Stanza)}; only the discriminator is enforced here.
         *
         * @param action       the optional action; may be {@code null}
         * @param category     the optional category; may be {@code null}
         * @param expiryAt     the optional expiry; may be {@code null}
         * @param bizOptOutIds the discriminator; never {@code null}
         * @throws NullPointerException if {@code bizOptOutIds} is {@code null}
         */
        public Item {
            Objects.requireNonNull(bizOptOutIds, "bizOptOutIds cannot be null");
        }

        /**
         * Returns the action marker when present.
         *
         * @return an {@link Optional} carrying the action, or empty when the relay omitted it
         */
        public Optional<String> actionAsOptional() {
            return Optional.ofNullable(action);
        }

        /**
         * Returns the marketing category when present.
         *
         * @return an {@link Optional} carrying the category, or empty when the relay omitted it
         */
        public Optional<String> categoryAsOptional() {
            return Optional.ofNullable(category);
        }

        /**
         * Returns the expiry timestamp when present.
         *
         * @return an {@link Optional} carrying the timestamp in seconds, or empty when the relay omitted it
         */
        public Optional<Long> expiryAtAsOptional() {
            return Optional.ofNullable(expiryAt);
        }
    }

    /**
     * Parses one {@code <item>} entry from an opt-out reply.
     *
     * <p>The action and category attributes are taken verbatim when present. The {@code expiry_at} attribute, when
     * present, must parse to a non-negative long or the parse fails. The {@link BizOptOutId} discriminator is
     * mandatory; a missing or unrecognised discriminator fails the parse. Shared by both success variants.
     *
     * @implNote This implementation mirrors {@link SmaxGetOptOutListResponse#parseItem(Stanza)} bit-for-bit so the
     * same opt-out item shape decodes identically across the get and update flows.
     *
     * @param itemStanza the source {@code <item>} stanza; never {@code null}
     * @return an {@link Optional} carrying the populated {@link Item}, or empty when the discriminator does not
     *         match or {@code expiry_at} is negative
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsBizOptOutResponseMixin",
            exports = "parseBizOptOutResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    private static Optional<Item> parseItem(Stanza itemStanza) {
        var action = itemStanza.getAttributeAsString("action").orElse(null);
        var category = itemStanza.getAttributeAsString("category").orElse(null);
        Long expiryAt = null;
        if (itemStanza.hasAttribute("expiry_at")) {
            var parsed = itemStanza.getAttributeAsLong("expiry_at");
            if (parsed.isEmpty() || parsed.getAsLong() < 0L) {
                return Optional.empty();
            }
            expiryAt = parsed.getAsLong();
        }
        var ids = BizOptOutId.parse(itemStanza).orElse(null);
        if (ids == null) {
            return Optional.empty();
        }
        return Optional.of(new Item(action, category, expiryAt, ids));
    }

    /**
     * The cache-match success reply, returned when the relay applied the action and the client's cache was already
     * up to date.
     *
     * <p>This is the success path that does not require a follow-up list refresh: the single echoed {@link #item()}
     * confirms the action's effect and {@link #listDhash()} carries the new server digest to store for the next
     * request.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsUpdateOptOutListResponseSuccessWithMatch")
    final class SuccessWithMatch implements SmaxUpdateOptOutListResponse {
        /**
         * The new server-side digest of the opt-out list.
         */
        private final String listDhash;

        /**
         * The single item descriptor echoed by the relay.
         */
        private final Item item;

        /**
         * Constructs a cache-match reply from the new digest and the echoed item.
         *
         * @param listDhash the new server digest; never {@code null}
         * @param item      the echoed item descriptor; never {@code null}
         * @throws NullPointerException if either argument is {@code null}
         */
        public SuccessWithMatch(String listDhash, Item item) {
            this.listDhash = Objects.requireNonNull(listDhash, "listDhash cannot be null");
            this.item = Objects.requireNonNull(item, "item cannot be null");
        }

        /**
         * Returns the new server digest.
         *
         * @return the digest; never {@code null}
         */
        public String listDhash() {
            return listDhash;
        }

        /**
         * Returns the single item descriptor echoed by the relay.
         *
         * @return the item; never {@code null}
         */
        public Item item() {
            return item;
        }

        /**
         * Parses a cache-match variant from the inbound stanza.
         *
         * <p>Returns empty when the result envelope does not validate, when the {@code <list/>} child is absent,
         * when {@code matched} is not {@code "true"}, when {@code dhash} is missing, when the single {@code <item>}
         * child is missing, or when that item fails {@link SmaxUpdateOptOutListResponse#parseItem(Stanza)}.
         *
         * @param stanza    the inbound stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateOptOutListResponseSuccessWithMatch",
                exports = "parseUpdateOptOutListResponseSuccessWithMatch",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessWithMatch> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var list = stanza.getChild("list").orElse(null);
            if (list == null) {
                return Optional.empty();
            }
            if (!list.hasAttribute("matched", "true")) {
                return Optional.empty();
            }
            var dhash = list.getAttributeAsString("dhash").orElse(null);
            if (dhash == null) {
                return Optional.empty();
            }
            var itemNode = list.getChild("item").orElse(null);
            if (itemNode == null) {
                return Optional.empty();
            }
            var parsed = parseItem(itemNode).orElse(null);
            if (parsed == null) {
                return Optional.empty();
            }
            return Optional.of(new SuccessWithMatch(dhash, parsed));
        }

        /**
         * Compares this reply to another object for value equality across the digest and item.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link SuccessWithMatch} with equal fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (SuccessWithMatch) obj;
            return Objects.equals(this.listDhash, that.listDhash)
                    && Objects.equals(this.item, that.item);
        }

        /**
         * Returns a hash code derived from the digest and item.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(listDhash, item);
        }

        /**
         * Returns a debug rendering of the digest and item.
         *
         * @return a diagnostic string; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxUpdateOptOutListResponse.SuccessWithMatch[listDhash=" + listDhash
                    + ", item=" + item + ']';
        }
    }

    /**
     * The full-list mismatch reply, returned when the relay applied the action and the client's cache was stale.
     *
     * <p>This is the success path that also requires a chat-list refresh: consumers persist {@link #listDhash()}
     * and replace the local opt-out list with {@link #listItem()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsUpdateOptOutListResponseSuccessWithMismatch")
    final class SuccessWithMismatch implements SmaxUpdateOptOutListResponse {
        /**
         * Whether the relay echoed the request's digest as a {@code c_dhash} attribute.
         */
        private final boolean hasListCDhash;

        /**
         * The new server-side digest.
         */
        private final String listDhash;

        /**
         * The parsed item list.
         */
        private final List<Item> listItem;

        /**
         * Constructs a mismatch reply from the echo flag, the new digest, and the parsed list.
         *
         * <p>The {@code listItem} list is defensively copied for immutability.
         *
         * @param hasListCDhash whether the {@code c_dhash} echo was present
         * @param listDhash     the new server digest; never {@code null}
         * @param listItem      the parsed list; never {@code null}
         * @throws NullPointerException if {@code listDhash} or {@code listItem} is {@code null}
         */
        public SuccessWithMismatch(boolean hasListCDhash, String listDhash, List<Item> listItem) {
            this.hasListCDhash = hasListCDhash;
            this.listDhash = Objects.requireNonNull(listDhash, "listDhash cannot be null");
            this.listItem = List.copyOf(Objects.requireNonNull(listItem, "listItem cannot be null"));
        }

        /**
         * Returns whether the relay echoed the request's digest.
         *
         * @return {@code true} when the {@code c_dhash} echo was present
         */
        public boolean hasListCDhash() {
            return hasListCDhash;
        }

        /**
         * Returns the new server digest.
         *
         * @return the digest; never {@code null}
         */
        public String listDhash() {
            return listDhash;
        }

        /**
         * Returns the parsed item list.
         *
         * @return an unmodifiable list of items; never {@code null}
         */
        public List<Item> listItem() {
            return listItem;
        }

        /**
         * Parses a mismatch variant from the inbound stanza.
         *
         * <p>Returns empty when the result envelope does not validate, when the {@code <list/>} child is absent,
         * when {@code matched} is not {@code "false"}, when {@code dhash} is missing, when the {@code c_dhash} echo
         * is present but does not match the request's {@code <item dhash/>}, when more than {@code 64000} items are
         * present, or when any item fails {@link SmaxUpdateOptOutListResponse#parseItem(Stanza)}.
         *
         * @implNote This implementation mirrors WA Web's hard cap of {@code 64000} items per list; exceeding the
         * cap rejects the parse so the priority chain can fall through to the error variants.
         *
         * @param stanza    the inbound stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateOptOutListResponseSuccessWithMismatch",
                exports = "parseUpdateOptOutListResponseSuccessWithMismatch",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessWithMismatch> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var list = stanza.getChild("list").orElse(null);
            if (list == null) {
                return Optional.empty();
            }
            if (!list.hasAttribute("matched", "false")) {
                return Optional.empty();
            }
            var dhash = list.getAttributeAsString("dhash").orElse(null);
            if (dhash == null) {
                return Optional.empty();
            }
            var replyCDhash = list.getAttributeAsString("c_dhash").orElse(null);
            var hasListCDhash = replyCDhash != null;
            if (hasListCDhash) {
                var requestItemDhash = request.getChild("item")
                        .flatMap(itemRef -> itemRef.getAttributeAsString("dhash"))
                        .orElse(null);
                if (requestItemDhash != null && !replyCDhash.equals(requestItemDhash)) {
                    return Optional.empty();
                }
            }
            var itemNodes = list.getChildren("item");
            if (itemNodes.size() > 64000) {
                return Optional.empty();
            }
            var entries = new ArrayList<Item>(itemNodes.size());
            for (var itemNode : itemNodes) {
                var parsed = parseItem(itemNode).orElse(null);
                if (parsed == null) {
                    return Optional.empty();
                }
                entries.add(parsed);
            }
            return Optional.of(new SuccessWithMismatch(hasListCDhash, dhash, entries));
        }

        /**
         * Compares this reply to another object for value equality across every field.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link SuccessWithMismatch} with equal fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (SuccessWithMismatch) obj;
            return this.hasListCDhash == that.hasListCDhash
                    && Objects.equals(this.listDhash, that.listDhash)
                    && Objects.equals(this.listItem, that.listItem);
        }

        /**
         * Returns a hash code derived from every field.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(hasListCDhash, listDhash, listItem);
        }

        /**
         * Returns a debug rendering listing every field.
         *
         * @return a diagnostic string; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxUpdateOptOutListResponse.SuccessWithMismatch[hasListCDhash=" + hasListCDhash
                    + ", listDhash=" + listDhash
                    + ", listItem=" + listItem + ']';
        }
    }

    /**
     * The malformed-request reply variant, returned when the relay rejects the request as invalid.
     *
     * <p>The {@link #errorCode()} and {@link #errorText()} pair is surfaced to the caller; WA Web maps this to the
     * {@code invalid_request} error kind. It is selected for WhatsApp Web GraphQL error codes below {@code 500}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsUpdateOptOutListResponseInvalidRequest")
    final class ClientError implements SmaxUpdateOptOutListResponse {
        /**
         * The numeric server-side error code.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the relay's error code and text.
         *
         * @param errorCode the numeric error code echoed by the relay
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text when present.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a malformed-request variant from the inbound stanza.
         *
         * <p>The envelope check is delegated to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)},
         * which accepts only error replies whose code falls below {@code 500}.
         *
         * @param stanza    the inbound stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateOptOutListResponseInvalidRequest",
                exports = "parseUpdateOptOutListResponseInvalidRequest",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for value equality across the code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ClientError} with equal fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from the code and text.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering of the code and text.
         *
         * @return a diagnostic string; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxUpdateOptOutListResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The transient server-error reply variant, returned when the relay reports a recoverable failure.
     *
     * <p>The {@link #errorCode()} and {@link #errorText()} pair is surfaced to the caller; WA Web maps this to the
     * {@code server_error} error kind and leaves the retry policy to the caller. It is selected for WhatsApp Web GraphQL error
     * codes of {@code 500} or above.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsUpdateOptOutListResponseServerError")
    final class ServerError implements SmaxUpdateOptOutListResponse {
        /**
         * The numeric server-side error code.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the relay's error code and text.
         *
         * @param errorCode the numeric error code echoed by the relay
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text when present.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a server-error variant from the inbound stanza.
         *
         * <p>The envelope check is delegated to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)},
         * which accepts only error replies whose code is {@code 500} or above.
         *
         * @param stanza    the inbound stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateOptOutListResponseServerError",
                exports = "parseUpdateOptOutListResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for value equality across the code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ServerError} with equal fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from the code and text.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering of the code and text.
         *
         * @return a diagnostic string; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxUpdateOptOutListResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

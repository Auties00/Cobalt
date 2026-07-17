package com.github.auties00.cobalt.wire.stanza.smax.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Discriminates an inbound reply to a {@link SmaxGetOptOutListRequest} into one of four shapes.
 *
 * <p>The four variants cover the cache-match short-circuit ({@link SuccessWithMatch}), the full-list mismatch
 * ({@link SuccessWithMismatch}), the malformed-request error ({@link ClientError}), and the transient server
 * error ({@link ServerError}).
 *
 * @implNote This implementation preserves the parser priority order in {@link #of(Stanza, Stanza)}: mismatch first
 * so a full-body reply cannot be mistaken for a cache-match short-circuit.
 */
public sealed interface SmaxGetOptOutListResponse extends SmaxStanza.Response
        permits SmaxGetOptOutListResponse.SuccessWithMatch,
        SmaxGetOptOutListResponse.SuccessWithMismatch,
        SmaxGetOptOutListResponse.ClientError,
        SmaxGetOptOutListResponse.ServerError {

    /**
     * Dispatches the inbound stanza onto the matching variant.
     *
     * @param stanza    the inbound {@code <iq>} stanza; never {@code null}
     * @param request the original {@link SmaxGetOptOutListRequest} stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBlocklistsGetOptOutListRPC",
            exports = "sendGetOptOutListRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGetOptOutListResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var successWithMismatch = SuccessWithMismatch.of(stanza, request);
        if (successWithMismatch.isPresent()) {
            return successWithMismatch;
        }
        var successWithMatch = SuccessWithMatch.of(stanza, request);
        if (successWithMatch.isPresent()) {
            return successWithMatch;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Describes one {@code <item>} entry in a mismatch reply's opt-out list.
     *
     * <p>Each entry's {@link #bizOptOutIds()} either renders a direct {@link BizOptOutId.UserJid} pill or
     * expands a {@link BizOptOutId.BrandId} into the full business-numbers set.
     *
     * @param action       the optional action marker (for example {@code "block"}); may be {@code null}
     * @param category     the optional marketing category; may be {@code null}
     * @param expiryAt     the optional non-negative expiry timestamp in seconds; may be {@code null}
     * @param bizOptOutIds the brand-id-versus-jid discriminator; never {@code null}
     */
    record Item(String action, String category, Long expiryAt, BizOptOutId bizOptOutIds) {
        /**
         * Validates the {@link Item} payload, rejecting a missing discriminator.
         *
         * <p>The other fields are optional and arrive pre-validated by
         * {@link SmaxGetOptOutListResponse#parseItem(Stanza)}.
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
     * <p>Shared by {@link SuccessWithMismatch#of(Stanza, Stanza)} and the single-item update reply in
     * {@link SmaxUpdateOptOutListResponse}.
     *
     * @implNote This implementation rejects a present-but-negative {@code expiry_at} so the priority chain can
     * route malformed entries to the error variants instead of accepting a nonsensical timestamp; the brand-id
     * disjunction is delegated to {@link BizOptOutId#parse(Stanza)}.
     *
     * @param itemStanza the {@code <item>} stanza; never {@code null}
     * @return an {@link Optional} carrying the populated {@link Item}, or empty when the discriminator does not
     *         match or {@code expiry_at} is negative
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsBizOptOutResponseMixin",
            exports = "parseBizOptOutResponseMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetOptOutListResponseSuccessWithMismatch",
            exports = "parseGetOptOutListResponseSuccessWithMismatchListItem",
            adaptation = WhatsAppAdaptation.ADAPTED)
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
     * Signals that the client's cached opt-out list digest matches the server's.
     *
     * <p>The caller treats this variant as a no-op: the local cache is up to date and the chat-list pills are
     * kept as-is.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsGetOptOutListResponseSuccessWithMatch")
    final class SuccessWithMatch implements SmaxGetOptOutListResponse {
        /**
         * Whether the relay echoed the request's category attribute.
         *
         * <p>Distinguishes a relay-echoed category match from a relay-omitted echo. The caller does not branch on
         * this, but it is preserved for symmetry with the WA Web payload field.
         */
        private final boolean hasCategory;

        /**
         * Constructs a cache-match reply.
         *
         * @param hasCategory whether the reply echoed a category attribute
         */
        public SuccessWithMatch(boolean hasCategory) {
            this.hasCategory = hasCategory;
        }

        /**
         * Returns whether the reply echoed a category attribute.
         *
         * @return {@code true} when the relay echoed the request's category
         */
        public boolean hasCategory() {
            return hasCategory;
        }

        /**
         * Parses a cache-match variant.
         *
         * <p>The result is empty when the envelope is wrong or when the request specified a category and the
         * reply echoes a different one; a category mismatch narrows the reply out of this variant so the
         * priority chain can fall through.
         *
         * @implNote This implementation mirrors the optional-literal contract on the category attribute: the
         * reply may omit the attribute entirely, but a present attribute must echo the request.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetOptOutListResponseSuccessWithMatch",
                exports = "parseGetOptOutListResponseSuccessWithMatch",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessWithMatch> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var requestCategory = request.getAttributeAsString("category").orElse(null);
            var replyCategory = stanza.getAttributeAsString("category").orElse(null);
            if (requestCategory != null && replyCategory != null && !replyCategory.equals(requestCategory)) {
                return Optional.empty();
            }
            return Optional.of(new SuccessWithMatch(replyCategory != null));
        }

        /**
         * Compares this reply with another for equality by category-echo flag.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link SuccessWithMatch}
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
            return this.hasCategory == that.hasCategory;
        }

        /**
         * Returns a hash code derived from the category-echo flag.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(hasCategory);
        }

        /**
         * Returns a debug representation carrying the category-echo flag.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetOptOutListResponse.SuccessWithMatch[hasCategory=" + hasCategory + ']';
        }
    }

    /**
     * Carries the full opt-out list returned when the client's cached digest is stale.
     *
     * <p>The caller refreshes the marketing-messages opt-out pills: each {@link Item#bizOptOutIds()} either
     * resolves directly to a {@code wid} (for the {@link BizOptOutId.UserJid} arm) or is batched into a
     * brand-id-to-business-numbers lookup (for the {@link BizOptOutId.BrandId} arm).
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsGetOptOutListResponseSuccessWithMismatch")
    final class SuccessWithMismatch implements SmaxGetOptOutListResponse {
        /**
         * The new server-side digest, or {@code null} when the relay omitted it.
         */
        private final String listDhash;

        /**
         * The parsed opt-out list entries.
         */
        private final List<Item> listItem;

        /**
         * Constructs a mismatch reply, defensively copying the item list.
         *
         * @param listDhash the new server digest; may be {@code null}
         * @param listItem  the parsed list; never {@code null}
         * @throws NullPointerException if {@code listItem} is {@code null}
         */
        public SuccessWithMismatch(String listDhash, List<Item> listItem) {
            this.listDhash = listDhash;
            this.listItem = List.copyOf(Objects.requireNonNull(listItem, "listItem cannot be null"));
        }

        /**
         * Returns the new server digest when present.
         *
         * <p>Persisted alongside the resulting list so the next request can use the cache-match short-circuit.
         *
         * @return an {@link Optional} carrying the digest, or empty when the relay omitted it
         */
        public Optional<String> listDhash() {
            return Optional.ofNullable(listDhash);
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
         * Parses a mismatch variant.
         *
         * <p>The result is empty when the envelope is wrong, when the {@code <list/>} child is absent, or when
         * any {@code <item>} fails the per-item parser (because the brand-id-versus-jid disjunction did not match
         * or {@code expiry_at} was negative).
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetOptOutListResponseSuccessWithMismatch",
                exports = "parseGetOptOutListResponseSuccessWithMismatch",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessWithMismatch> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var list = stanza.getChild("list").orElse(null);
            if (list == null) {
                return Optional.empty();
            }
            var dhash = list.getAttributeAsString("dhash").orElse(null);
            var items = new ArrayList<Item>();
            for (var child : list.getChildren("item")) {
                var parsed = parseItem(child).orElse(null);
                if (parsed == null) {
                    return Optional.empty();
                }
                items.add(parsed);
            }
            return Optional.of(new SuccessWithMismatch(dhash, Collections.unmodifiableList(items)));
        }

        /**
         * Compares this reply with another for equality by digest and items.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link SuccessWithMismatch}
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
            return Objects.equals(this.listDhash, that.listDhash)
                    && Objects.equals(this.listItem, that.listItem);
        }

        /**
         * Returns a hash code derived from the digest and items.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(listDhash, listItem);
        }

        /**
         * Returns a debug representation carrying the digest and items.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetOptOutListResponse.SuccessWithMismatch[listDhash=" + listDhash
                    + ", listItem=" + listItem + ']';
        }
    }

    /**
     * Carries a malformed-request error reply.
     *
     * <p>The caller folds this into the {@code (errorCode, errorText)} pair returned to it.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsGetOptOutListResponseInvalidRequest")
    final class ClientError implements SmaxGetOptOutListResponse {
        /**
         * The numeric server-side error code.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply.
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
         * Parses a malformed-request variant.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetOptOutListResponseInvalidRequest",
                exports = "parseGetOptOutListResponseInvalidRequest",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with another for equality by error code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link ClientError}
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
         * Returns a hash code derived from the error code and text.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation carrying the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetOptOutListResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a transient server-error reply.
     *
     * <p>The caller folds this into the {@code (errorCode, errorText)} pair and decides the retry policy.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsGetOptOutListResponseInternalServerError")
    final class ServerError implements SmaxGetOptOutListResponse {
        /**
         * The numeric server-side error code.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply.
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
         * Parses a server-error variant.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetOptOutListResponseInternalServerError",
                exports = "parseGetOptOutListResponseInternalServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with another for equality by error code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link ServerError}
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
         * Returns a hash code derived from the error code and text.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation carrying the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetOptOutListResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

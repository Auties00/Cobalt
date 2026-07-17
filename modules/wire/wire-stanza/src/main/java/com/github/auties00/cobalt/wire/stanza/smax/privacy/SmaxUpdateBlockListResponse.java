package com.github.auties00.cobalt.wire.stanza.smax.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
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
 * The sealed family of inbound replies to a {@link SmaxUpdateBlockListRequest}, covering the chat-info Block /
 * Unblock action's server-confirmation step.
 *
 * <p>The six variants match the six response shapes the relay can return: {@link SuccessWithMatch} confirms the
 * action against an up-to-date cache, {@link SuccessWithMismatch} returns the refreshed PN-addressed list when the
 * cache was stale, {@link MigratedSuccessWithMismatch} returns the refreshed LID-addressed list, {@link
 * CAPISuccessWithMismatch} is the Cloud-API fall-through arm of the LID-addressed chain, {@link ClientError}
 * reports a malformed request, and {@link ServerError} reports a transient relay failure. The {@link
 * #of(Stanza, Stanza)} entry point dispatches an inbound stanza onto the first matching variant.
 *
 * @implNote This implementation preserves WA Web's parser priority order in {@link #of(Stanza, Stanza)}: match before
 * the mismatch variants so the cache-match short-circuit catches first when the request and reply both reference
 * the same digest.
 */
public sealed interface SmaxUpdateBlockListResponse extends SmaxStanza.Response
        permits SmaxUpdateBlockListResponse.SuccessWithMatch,
        SmaxUpdateBlockListResponse.SuccessWithMismatch,
        SmaxUpdateBlockListResponse.MigratedSuccessWithMismatch,
        SmaxUpdateBlockListResponse.CAPISuccessWithMismatch,
        SmaxUpdateBlockListResponse.ClientError,
        SmaxUpdateBlockListResponse.ServerError {

    /**
     * Dispatches the inbound stanza onto the first matching variant.
     *
     * <p>The variants are tried in priority order ({@link SuccessWithMatch}, {@link SuccessWithMismatch}, {@link
     * MigratedSuccessWithMismatch}, {@link CAPISuccessWithMismatch}, {@link ClientError}, {@link ServerError}); the
     * first whose parser accepts the stanza wins. An empty result signals that none of the six shapes matched the
     * reply correlated to {@code request}.
     *
     * @param stanza    the inbound {@code <iq>} stanza; never {@code null}
     * @param request the original {@link SmaxUpdateBlockListRequest} stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBlocklistsUpdateBlockListRPC",
            exports = "sendUpdateBlockListRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxUpdateBlockListResponse> of(Stanza stanza, Stanza request) {
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
        var migratedSuccessWithMismatch = MigratedSuccessWithMismatch.of(stanza, request);
        if (migratedSuccessWithMismatch.isPresent()) {
            return migratedSuccessWithMismatch;
        }
        var capiSuccessWithMismatch = CAPISuccessWithMismatch.of(stanza, request);
        if (capiSuccessWithMismatch.isPresent()) {
            return capiSuccessWithMismatch;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * The descriptor for one entry in a mismatch reply's refreshed blocklist.
     *
     * <p>The {@link #jid()} is PN-addressed on the standard variant and LID-addressed otherwise; {@link #active()}
     * records whether the relay tagged the entry as currently blocked, and {@link #displayName()} carries the
     * optional display-name echo. This shape mirrors {@link SmaxGetBlockListResponse.Item} so consumers can share
     * the rendering path.
     *
     * @param jid         the blocked user JID; PN on the standard variant, LID otherwise
     * @param active      whether the relay tagged the entry as currently blocked
     * @param displayName the optional display-name echo; may be {@code null}
     */
    record Item(Jid jid, boolean active, String displayName) {
        /**
         * Returns the display name when present.
         *
         * @return an {@link Optional} carrying the display name, or empty when the relay omitted it
         */
        public Optional<String> displayNameAsOptional() {
            return Optional.ofNullable(displayName);
        }
    }

    /**
     * Parses the {@code <item/>} children of a mismatch reply into a list of {@link Item} descriptors.
     *
     * <p>Shared by every mismatch variant. When {@code requireJid} is {@code true} a per-item {@code jid} is
     * mandatory and a missing one rejects the parse; the standard and regular-migration arms pass {@code true},
     * the Cloud-API arm passes {@code false}. An {@code active} attribute, when present, must be the literal
     * {@code "true"} or the parse is rejected.
     *
     * @implNote This implementation mirrors WA Web's {@code mapChildrenWithTag} fail-the-parent semantics so the
     * priority chain in {@link SmaxUpdateBlockListResponse#of(Stanza, Stanza)} can fall through to the Cloud-API
     * variant when a LID-addressed reply carries a jid-less item. The {@code parseBlocklistIdentifierMixin}
     * disjunction is collapsed: only the {@code display_name} branch is preserved, all other branches and the
     * {@code country_code} attribute are dropped (matching {@link SmaxGetBlockListResponse#parseItems(Stanza,
     * boolean)}). The {@code active} attribute is treated as a literal-only {@code "true"} on the migrated and
     * Cloud-API arms and as a boolean on the standard arm; both reduce to the same {@code activeAttr != null}
     * branch.
     *
     * @param list       the {@code <list/>} stanza
     * @param requireJid whether the per-item {@code jid} attribute is required
     * @return an {@link Optional} carrying the parsed list, or empty when the require-jid contract is violated or
     *         when an {@code active} attribute carries a value other than {@code "true"}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseSuccessWithMismatch",
            exports = "parseUpdateBlockListResponseSuccessWithMismatchListItem",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseMigratedSuccessWithMismatch",
            exports = "parseUpdateBlockListResponseMigratedSuccessWithMismatchListItem",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseCAPISuccessWithMismatch",
            exports = "parseUpdateBlockListResponseCAPISuccessWithMismatchListItem",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsBlocklistIdentifierMixin",
            exports = "parseBlocklistIdentifierMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsBlocklistIds",
            exports = "parseBlocklistIds", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsDisplayNameMixin",
            exports = "parseDisplayNameMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    private static Optional<List<Item>> parseItems(Stanza list, boolean requireJid) {
        var items = new ArrayList<Item>();
        for (var child : list.getChildren("item")) {
            var jid = child.getAttributeAsString("jid")
                    .map(Jid::of)
                    .orElse(null);
            if (jid == null && requireJid) {
                return Optional.empty();
            }
            var activeAttr = child.getAttributeAsString("active").orElse(null);
            if (activeAttr != null && !activeAttr.equals("true")) {
                return Optional.empty();
            }
            var active = activeAttr != null;
            var displayName = child.getAttributeAsString("display_name").orElse(null);
            items.add(new Item(jid, active, displayName));
        }
        return Optional.of(Collections.unmodifiableList(items));
    }

    /**
     * The cache-match success reply, signalling the action was applied and the client's cached digest matched.
     *
     * <p>This is the success path that does not require a follow-up list refresh; the new {@link #listDhash()} is
     * still stored for the next request.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseSuccessWithMatch")
    final class SuccessWithMatch implements SmaxUpdateBlockListResponse {
        /**
         * The new server-side digest of the blocklist.
         */
        private final String listDhash;

        /**
         * Constructs a cache-match reply from the new digest.
         *
         * @param listDhash the new server digest; never {@code null}
         * @throws NullPointerException if {@code listDhash} is {@code null}
         */
        public SuccessWithMatch(String listDhash) {
            this.listDhash = Objects.requireNonNull(listDhash, "listDhash cannot be null");
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
         * Parses a cache-match variant from the inbound stanza.
         *
         * <p>Returns empty when the result envelope does not validate, when the {@code <list/>} child is missing,
         * when {@code matched} is not {@code "true"}, or when the required {@code dhash} attribute is missing.
         *
         * @param stanza    the inbound stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseSuccessWithMatch",
                exports = "parseUpdateBlockListResponseSuccessWithMatch",
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
            return Optional.of(new SuccessWithMatch(dhash));
        }

        /**
         * Compares this reply to another object for value equality on the digest.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link SuccessWithMatch} with an equal digest
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
            return Objects.equals(this.listDhash, that.listDhash);
        }

        /**
         * Returns a hash code derived from the digest.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(listDhash);
        }

        /**
         * Returns a debug rendering of the digest.
         *
         * @return a diagnostic string; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxUpdateBlockListResponse.SuccessWithMatch[listDhash=" + listDhash + ']';
        }
    }

    /**
     * The PN-addressed mismatch reply, returned when the relay applied the action and the cache was stale.
     *
     * <p>This is the success path that also requires a blocklist re-render: consumers persist {@link #listDhash()}
     * and replace the local list with {@link #listItem()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseSuccessWithMismatch")
    final class SuccessWithMismatch implements SmaxUpdateBlockListResponse {
        /**
         * Whether the relay echoed the request's digest as a {@code c_dhash} attribute.
         */
        private final boolean hasListCDhash;

        /**
         * The new server-side digest.
         */
        private final String listDhash;

        /**
         * Whether the list is PN-addressed.
         */
        private final boolean phoneNumberAddressed;

        /**
         * The parsed item list.
         */
        private final List<Item> listItem;

        /**
         * Constructs a PN-addressed mismatch reply from the echo flag, the new digest, the addressing flag, and the
         * parsed list.
         *
         * <p>The {@code listItem} list is defensively copied for immutability.
         *
         * @param hasListCDhash        whether the {@code c_dhash} echo was present
         * @param listDhash            the new server digest; never {@code null}
         * @param phoneNumberAddressed whether the list is explicitly PN-addressed
         * @param listItem             the parsed list; never {@code null}
         * @throws NullPointerException if {@code listDhash} or {@code listItem} is {@code null}
         */
        public SuccessWithMismatch(boolean hasListCDhash, String listDhash,
                                   boolean phoneNumberAddressed, List<Item> listItem) {
            this.hasListCDhash = hasListCDhash;
            this.listDhash = Objects.requireNonNull(listDhash, "listDhash cannot be null");
            this.phoneNumberAddressed = phoneNumberAddressed;
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
         * Returns whether the list is explicitly PN-addressed.
         *
         * @return {@code true} when the relay set {@code addressing_mode="pn"} explicitly
         */
        public boolean phoneNumberAddressed() {
            return phoneNumberAddressed;
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
         * Parses a PN-addressed mismatch variant from the inbound stanza.
         *
         * <p>Returns empty when the result envelope does not validate, when the {@code <list/>} child is absent,
         * when {@code matched} is not {@code "false"}, when {@code addressing_mode} is set to anything other than
         * {@code "pn"}, when the {@code c_dhash} echo (if present) does not match the request's {@code <item
         * dhash/>}, when {@code dhash} is missing, or when any per-item JID is missing.
         *
         * @param stanza    the inbound stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseSuccessWithMismatch",
                exports = "parseUpdateBlockListResponseSuccessWithMismatch",
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
            var addressingMode = list.getAttributeAsString("addressing_mode").orElse(null);
            if (addressingMode != null && !addressingMode.equals("pn")) {
                return Optional.empty();
            }
            var requestItemDhash = request.getChild("item")
                    .flatMap(item -> item.getAttributeAsString("dhash"))
                    .orElse(null);
            var replyCDhash = list.getAttributeAsString("c_dhash").orElse(null);
            if (replyCDhash != null && requestItemDhash != null && !replyCDhash.equals(requestItemDhash)) {
                return Optional.empty();
            }
            var dhash = list.getAttributeAsString("dhash").orElse(null);
            if (dhash == null) {
                return Optional.empty();
            }
            var hasListCDhash = replyCDhash != null;
            var items = parseItems(list, true).orElse(null);
            if (items == null) {
                return Optional.empty();
            }
            return Optional.of(new SuccessWithMismatch(hasListCDhash, dhash, addressingMode != null, items));
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
                    && this.phoneNumberAddressed == that.phoneNumberAddressed
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
            return Objects.hash(hasListCDhash, listDhash, phoneNumberAddressed, listItem);
        }

        /**
         * Returns a debug rendering listing every field.
         *
         * @return a diagnostic string; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxUpdateBlockListResponse.SuccessWithMismatch[hasListCDhash=" + hasListCDhash
                    + ", listDhash=" + listDhash
                    + ", phoneNumberAddressed=" + phoneNumberAddressed
                    + ", listItem=" + listItem + ']';
        }
    }

    /**
     * The regular LID-migration mismatch reply, returned for clients on the LID-addressed wire.
     *
     * <p>This carries the same caller-facing surface as {@link SuccessWithMismatch} but each {@link Item#jid()} is
     * a LID JID, translated downstream into the user-facing identity.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseMigratedSuccessWithMismatch")
    final class MigratedSuccessWithMismatch implements SmaxUpdateBlockListResponse {
        /**
         * Whether the {@code c_dhash} echo was present.
         */
        private final boolean hasListCDhash;

        /**
         * The new server-side digest.
         */
        private final String listDhash;

        /**
         * The parsed LID-addressed item list with mandatory per-item JIDs.
         */
        private final List<Item> listItem;

        /**
         * Constructs a regular-migration mismatch reply from the echo flag, the new digest, and the parsed list.
         *
         * <p>The {@code listItem} list is defensively copied for immutability.
         *
         * @param hasListCDhash whether the {@code c_dhash} echo was present
         * @param listDhash     the new server digest; never {@code null}
         * @param listItem      the parsed item list; never {@code null}
         * @throws NullPointerException if {@code listDhash} or {@code listItem} is {@code null}
         */
        public MigratedSuccessWithMismatch(boolean hasListCDhash, String listDhash, List<Item> listItem) {
            this.hasListCDhash = hasListCDhash;
            this.listDhash = Objects.requireNonNull(listDhash, "listDhash cannot be null");
            this.listItem = List.copyOf(Objects.requireNonNull(listItem, "listItem cannot be null"));
        }

        /**
         * Returns whether the {@code c_dhash} echo was present.
         *
         * @return {@code true} when present
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
         * Parses a regular-migration mismatch variant from the inbound stanza.
         *
         * <p>Returns empty when the result envelope does not validate, when {@code matched} is not {@code "false"},
         * when {@code addressing_mode} is not {@code "lid"}, when the {@code c_dhash} echo does not match the
         * request's {@code <item dhash/>}, when {@code dhash} is missing, or when any per-item JID is missing.
         *
         * @param stanza    the inbound stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseMigratedSuccessWithMismatch",
                exports = "parseUpdateBlockListResponseMigratedSuccessWithMismatch",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<MigratedSuccessWithMismatch> of(Stanza stanza, Stanza request) {
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
            if (!list.hasAttribute("addressing_mode", "lid")) {
                return Optional.empty();
            }
            var requestItemDhash = request.getChild("item")
                    .flatMap(item -> item.getAttributeAsString("dhash"))
                    .orElse(null);
            var replyCDhash = list.getAttributeAsString("c_dhash").orElse(null);
            if (replyCDhash != null && requestItemDhash != null && !replyCDhash.equals(requestItemDhash)) {
                return Optional.empty();
            }
            var dhash = list.getAttributeAsString("dhash").orElse(null);
            if (dhash == null) {
                return Optional.empty();
            }
            var hasListCDhash = replyCDhash != null;
            var items = parseItems(list, true).orElse(null);
            if (items == null) {
                return Optional.empty();
            }
            return Optional.of(new MigratedSuccessWithMismatch(hasListCDhash, dhash, items));
        }

        /**
         * Compares this reply to another object for value equality across every field.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link MigratedSuccessWithMismatch} with equal fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (MigratedSuccessWithMismatch) obj;
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
            return "SmaxUpdateBlockListResponse.MigratedSuccessWithMismatch[hasListCDhash=" + hasListCDhash
                    + ", listDhash=" + listDhash
                    + ", listItem=" + listItem + ']';
        }
    }

    /**
     * The Cloud-API mismatch reply, the fall-through arm of the LID-addressed parser priority chain.
     *
     * <p>This is wire-shape equivalent to {@link MigratedSuccessWithMismatch} but with the per-item JID optional;
     * it matches LID-addressed replies whose items lack {@code jid} attributes (the Cloud-API server flavour).
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseCAPISuccessWithMismatch")
    final class CAPISuccessWithMismatch implements SmaxUpdateBlockListResponse {
        /**
         * Whether the {@code c_dhash} echo was present.
         */
        private final boolean hasListCDhash;

        /**
         * The new server-side digest.
         */
        private final String listDhash;

        /**
         * The parsed item list; may contain entries with missing JIDs.
         */
        private final List<Item> listItem;

        /**
         * Constructs a Cloud-API mismatch reply from the echo flag, the new digest, and the parsed list.
         *
         * <p>The {@code listItem} list is defensively copied for immutability.
         *
         * @param hasListCDhash whether the {@code c_dhash} echo was present
         * @param listDhash     the new server digest; never {@code null}
         * @param listItem      the parsed item list; never {@code null}
         * @throws NullPointerException if {@code listDhash} or {@code listItem} is {@code null}
         */
        public CAPISuccessWithMismatch(boolean hasListCDhash, String listDhash, List<Item> listItem) {
            this.hasListCDhash = hasListCDhash;
            this.listDhash = Objects.requireNonNull(listDhash, "listDhash cannot be null");
            this.listItem = List.copyOf(Objects.requireNonNull(listItem, "listItem cannot be null"));
        }

        /**
         * Returns whether the {@code c_dhash} echo was present.
         *
         * @return {@code true} when present
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
         * Parses a Cloud-API mismatch variant from the inbound stanza.
         *
         * <p>Returns empty when the result envelope does not validate, when {@code matched} is not {@code "false"},
         * when {@code addressing_mode} is not {@code "lid"}, when the {@code c_dhash} echo does not match the
         * request's {@code <item dhash/>}, when {@code dhash} is missing, or when any {@code active} attribute
         * carries a non-{@code "true"} value. The per-item JID is optional, so a missing JID does not abort the
         * parse.
         *
         * @param stanza    the inbound stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseCAPISuccessWithMismatch",
                exports = "parseUpdateBlockListResponseCAPISuccessWithMismatch",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<CAPISuccessWithMismatch> of(Stanza stanza, Stanza request) {
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
            if (!list.hasAttribute("addressing_mode", "lid")) {
                return Optional.empty();
            }
            var requestItemDhash = request.getChild("item")
                    .flatMap(item -> item.getAttributeAsString("dhash"))
                    .orElse(null);
            var replyCDhash = list.getAttributeAsString("c_dhash").orElse(null);
            if (replyCDhash != null && requestItemDhash != null && !replyCDhash.equals(requestItemDhash)) {
                return Optional.empty();
            }
            var dhash = list.getAttributeAsString("dhash").orElse(null);
            if (dhash == null) {
                return Optional.empty();
            }
            var hasListCDhash = replyCDhash != null;
            var items = parseItems(list, false).orElse(null);
            if (items == null) {
                return Optional.empty();
            }
            return Optional.of(new CAPISuccessWithMismatch(hasListCDhash, dhash, items));
        }

        /**
         * Compares this reply to another object for value equality across every field.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link CAPISuccessWithMismatch} with equal fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (CAPISuccessWithMismatch) obj;
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
            return "SmaxUpdateBlockListResponse.CAPISuccessWithMismatch[hasListCDhash=" + hasListCDhash
                    + ", listDhash=" + listDhash
                    + ", listItem=" + listItem + ']';
        }
    }

    /**
     * The malformed-request reply variant, optionally carrying an addressing-mode hint.
     *
     * <p>The {@link #errorCode()} and {@link #errorText()} pair is surfaced to the caller as a block-failure log
     * line; the optional {@link #errorAddressingMode()} hint helps the caller diagnose migration-state issues by
     * indicating which wire the relay expected. It is selected for WhatsApp Web GraphQL error codes below {@code 500}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseInvalidRequest")
    final class ClientError implements SmaxUpdateBlockListResponse {
        /**
         * The numeric server-side error code.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text.
         */
        private final String errorText;

        /**
         * The optional addressing-mode hint carried on the {@code <error/>} child.
         *
         * <p>The value is validated against the {@code (lid, pn)} literal tuple by {@link #of(Stanza, Stanza)}; any
         * other value rejects this variant, so a non-{@code null} value is always one of {@code "lid"} or
         * {@code "pn"}.
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsEnums",
                exports = "ENUM_LID_PN", adaptation = WhatsAppAdaptation.ADAPTED)
        private final String errorAddressingMode;

        /**
         * Constructs a client-error reply from the relay's error code, text, and addressing-mode hint.
         *
         * @param errorCode           the numeric error code echoed by the relay
         * @param errorText           the optional human-readable text; may be {@code null}
         * @param errorAddressingMode the optional addressing-mode hint; may be {@code null}
         */
        public ClientError(int errorCode, String errorText, String errorAddressingMode) {
            this.errorCode = errorCode;
            this.errorText = errorText;
            this.errorAddressingMode = errorAddressingMode;
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
         * Returns the addressing-mode hint when present.
         *
         * <p>The hint diagnoses migration-state mismatches; a non-{@code null} value is always one of
         * {@code "lid"} or {@code "pn"} because {@link #of(Stanza, Stanza)} rejects any other value.
         *
         * @return an {@link Optional} carrying the hint, or empty when the relay omitted it
         */
        public Optional<String> errorAddressingMode() {
            return Optional.ofNullable(errorAddressingMode);
        }

        /**
         * Parses a malformed-request variant from the inbound stanza.
         *
         * <p>The envelope check is delegated to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)},
         * which accepts only error replies whose code falls below {@code 500}. The optional {@code addressing_mode}
         * attribute on the {@code <error/>} child is then validated against the {@code (lid, pn)} literal tuple;
         * any other value rejects the parse.
         *
         * @implNote This implementation inlines WA Web's {@code attrStringEnum} contract by checking explicitly
         * against the two enum keys; the WA Web {@code ENUM_LID_PN} tuple is a closed literal map, so the inlined
         * check matches behaviour.
         *
         * @param stanza    the inbound stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseInvalidRequest",
                exports = "parseUpdateBlockListResponseInvalidRequest",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            var addressingMode = stanza.getChild("error")
                    .flatMap(child -> child.getAttributeAsString("addressing_mode"))
                    .orElse(null);
            if (addressingMode != null && !addressingMode.equals("lid") && !addressingMode.equals("pn")) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text(), addressingMode));
        }

        /**
         * Compares this reply to another object for value equality across every field.
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
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText)
                    && Objects.equals(this.errorAddressingMode, that.errorAddressingMode);
        }

        /**
         * Returns a hash code derived from every field.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText, errorAddressingMode);
        }

        /**
         * Returns a debug rendering listing every field.
         *
         * @return a diagnostic string; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxUpdateBlockListResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText
                    + ", errorAddressingMode=" + errorAddressingMode + ']';
        }
    }

    /**
     * The transient server-error reply variant, returned when the relay reports a recoverable failure.
     *
     * <p>The {@link #errorCode()} and {@link #errorText()} pair is surfaced to the caller; WA Web does not retry
     * inline, leaving recovery to the caller. It is selected for WhatsApp Web GraphQL error codes of {@code 500} or above.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseServerError")
    final class ServerError implements SmaxUpdateBlockListResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsUpdateBlockListResponseServerError",
                exports = "parseUpdateBlockListResponseServerError",
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
            return "SmaxUpdateBlockListResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

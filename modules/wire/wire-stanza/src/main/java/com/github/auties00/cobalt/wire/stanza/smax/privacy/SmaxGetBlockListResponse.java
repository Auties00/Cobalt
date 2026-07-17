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
 * Discriminates an inbound reply to a {@link SmaxGetBlockListRequest} into one of seven shapes.
 *
 * <p>The seven variants cover the cache-match short-circuit ({@link SuccessWithMatch}), the PN-addressed full
 * list ({@link SuccessWithMismatch}), the regular PN-to-LID migration ({@link MigratedSuccessWithMismatch}), the
 * force migration carrying {@code dirty="true"} ({@link ForceMigratedSuccessWithMismatch}), the Cloud-API
 * fall-through ({@link CAPISuccessWithMismatch}), the malformed-request error ({@link ClientError}), and the
 * transient server error ({@link ServerError}).
 *
 * @implNote This implementation preserves the parser priority order in {@link #of(Stanza, Stanza)}; the order
 * matters because the migration variants overlap on wire shape and disambiguate only by {@code dirty} and by the
 * per-item {@code jid} requirement.
 */
public sealed interface SmaxGetBlockListResponse extends SmaxStanza.Response
        permits SmaxGetBlockListResponse.SuccessWithMatch,
        SmaxGetBlockListResponse.SuccessWithMismatch,
        SmaxGetBlockListResponse.MigratedSuccessWithMismatch,
        SmaxGetBlockListResponse.ForceMigratedSuccessWithMismatch,
        SmaxGetBlockListResponse.CAPISuccessWithMismatch,
        SmaxGetBlockListResponse.ClientError,
        SmaxGetBlockListResponse.ServerError {

    /**
     * Dispatches the inbound stanza onto the matching variant.
     *
     * <p>An empty result signals that none of the documented parser arms matched the stanza.
     *
     * @implNote This implementation tries the mismatch variants before the cache-match short-circuit so a
     * forced-migration {@code <list/>} cannot be mistaken for a bare match.
     *
     * @param stanza    the inbound {@code <iq>} stanza; never {@code null}
     * @param request the original {@link SmaxGetBlockListRequest} stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBlocklistsGetBlockListRPC",
            exports = "sendGetBlockListRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGetBlockListResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var successWithMismatch = SuccessWithMismatch.of(stanza, request);
        if (successWithMismatch.isPresent()) {
            return successWithMismatch;
        }
        var migratedSuccessWithMismatch = MigratedSuccessWithMismatch.of(stanza, request);
        if (migratedSuccessWithMismatch.isPresent()) {
            return migratedSuccessWithMismatch;
        }
        var forceMigratedSuccessWithMismatch = ForceMigratedSuccessWithMismatch.of(stanza, request);
        if (forceMigratedSuccessWithMismatch.isPresent()) {
            return forceMigratedSuccessWithMismatch;
        }
        var capiSuccessWithMismatch = CAPISuccessWithMismatch.of(stanza, request);
        if (capiSuccessWithMismatch.isPresent()) {
            return capiSuccessWithMismatch;
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
     * Describes one blocked-user entry in a mismatch-shaped reply.
     *
     * <p>The {@link #jid()} is the addressable identifier (a PN on the standard variant, a LID on the
     * migrated and Cloud-API variants). The {@link #pnJid()} is the always-PN-form companion populated when the
     * relay's identifier disjunction resolves to its phone-number arm, needed to seed the LID-to-PN cache on the
     * LID-addressed variants. The {@link #username()} carries the username arm; the {@link #displayName()}
     * carries the display-name echo; and {@link #active()} reflects whether the relay marked the entry as
     * currently blocked.
     *
     * @implNote This record drops WA Web's {@code country_code} attribute and {@code guest_name} field because no
     * Cobalt consumer reads them; the rest of the identifier disjunction is exposed verbatim so consumers can
     * route on the per-arm metadata.
     *
     * @param jid         the addressable JID; a PN on the standard variant, a LID otherwise; may be {@code null}
     *                    on the sparsely-populated force-migration and Cloud-API variants
     * @param pnJid       the always-PN-form companion JID; may be {@code null}
     * @param username    the username attribute; may be {@code null}
     * @param active      whether the relay's {@code active="true"} attribute is present
     * @param displayName the optional display-name echo; may be {@code null}
     */
    record Item(Jid jid, Jid pnJid, String username, boolean active, String displayName) {
        /**
         * Returns the always-PN-form companion JID when present.
         *
         * <p>Populated when the relay's disjunction resolves to its phone-number arm; used to learn the pairing
         * of the addressable {@link #jid()} with its PN counterpart.
         *
         * @return an {@link Optional} carrying the PN-form JID, or empty when the relay omitted it
         */
        public Optional<Jid> pnJidAsOptional() {
            return Optional.ofNullable(pnJid);
        }

        /**
         * Returns the username when present.
         *
         * <p>Populated when the relay's disjunction resolves to its username arm; keeps username-only contacts
         * addressable.
         *
         * @return an {@link Optional} carrying the username, or empty when the relay omitted it
         */
        public Optional<String> usernameAsOptional() {
            return Optional.ofNullable(username);
        }

        /**
         * Returns the display name when present.
         *
         * <p>Used when rendering the blocked-contacts list to fall back to the relay-supplied display name for
         * entries that lack a local contact record.
         *
         * @return an {@link Optional} carrying the display name, or empty when the relay omitted it
         */
        public Optional<String> displayNameAsOptional() {
            return Optional.ofNullable(displayName);
        }
    }

    /**
     * Validates the IQ-result envelope and extracts the inner {@code <list/>} child.
     *
     * <p>Shared by the mismatch-shaped variants ({@link SuccessWithMismatch}, {@link MigratedSuccessWithMismatch},
     * {@link ForceMigratedSuccessWithMismatch}, {@link CAPISuccessWithMismatch}). The cache-match variant does
     * not call this helper because it requires the {@code <list/>} child to be absent.
     *
     * @param stanza    the inbound stanza
     * @param request the original outbound request
     * @return an {@link Optional} carrying the {@code <list/>} child, or empty when the envelope or list is missing
     */
    private static Optional<Stanza> validateMismatchEnvelope(Stanza stanza, Stanza request) {
        if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
            return Optional.empty();
        }
        return stanza.getChild("list");
    }

    /**
     * Parses the {@code <item/>} children of a {@code <list/>} stanza into a list of {@link Item} descriptors.
     *
     * <p>Shared by every mismatch-shaped variant. Pass {@code requireJid=true} for the standard and
     * regular-migration arms where the per-item JID is mandatory on the wire, and {@code false} for the
     * force-migration and Cloud-API arms where the per-item JID is optional.
     *
     * @implNote This implementation mirrors the fail-the-parent semantics of the wire mapping: when
     * {@code requireJid} is set and an {@code <item/>} lacks a {@code jid} attribute, the entire parse aborts so
     * the priority chain in {@link #of(Stanza, Stanza)} can fall through to the variant whose per-item JID is
     * optional ({@link CAPISuccessWithMismatch}). The identifier disjunction is exposed as flat {@code pn_jid},
     * {@code username}, and {@code display_name} fields; the {@code guest_name} branch and {@code country_code}
     * attribute are dropped because no consumer reads them.
     *
     * @param list       the {@code <list/>} stanza
     * @param requireJid whether the per-item {@code jid} attribute is required
     * @return an {@link Optional} carrying the parsed list, or empty when {@code requireJid=true} and an item
     *         lacks a {@code jid}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetBlockListResponseSuccessWithMismatch",
            exports = "parseGetBlockListResponseSuccessWithMismatchListItem",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetBlockListResponseMigratedSuccessWithMismatch",
            exports = "parseGetBlockListResponseMigratedSuccessWithMismatchListItem",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetBlockListResponseForceMigratedSuccessWithMismatch",
            exports = "parseGetBlockListResponseForceMigratedSuccessWithMismatchListItem",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetBlockListResponseCAPISuccessWithMismatch",
            exports = "parseGetBlockListResponseCAPISuccessWithMismatchListItem",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsBlocklistIdentifierMixin",
            exports = "parseBlocklistIdentifierMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsBlocklistIds",
            exports = "parseBlocklistIds", adaptation = WhatsAppAdaptation.ADAPTED)
    private static Optional<List<Item>> parseItems(Stanza list, boolean requireJid) {
        var items = new ArrayList<Item>();
        for (var child : list.getChildren("item")) {
            var jid = child.getAttributeAsString("jid")
                    .map(Jid::of)
                    .orElse(null);
            if (jid == null && requireJid) {
                return Optional.empty();
            }
            var pnJid = child.getAttributeAsString("pn_jid")
                    .map(Jid::of)
                    .orElse(null);
            var username = child.getAttributeAsString("username").orElse(null);
            var active = child.hasAttribute("active", "true");
            var displayName = child.getAttributeAsString("display_name").orElse(null);
            items.add(new Item(jid, pnJid, username, active, displayName));
        }
        return Optional.of(Collections.unmodifiableList(items));
    }

    /**
     * Signals that the client's cached blocklist digest matches the server's.
     *
     * <p>The caller keeps the local cache as-is and skips the list re-render.
     *
     * @implNote This implementation rejects on the presence of any {@code <list/>} child to preserve the priority
     * chain in {@link SmaxGetBlockListResponse#of(Stanza, Stanza)} where the mismatch variants are tried first.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsGetBlockListResponseSuccessWithMatch")
    final class SuccessWithMatch implements SmaxGetBlockListResponse {
        /**
         * Constructs a cache-match reply.
         *
         * <p>The variant carries no payload; the constructor exists to satisfy the sealed-interface contract.
         */
        public SuccessWithMatch() {
        }

        /**
         * Parses a cache-match variant.
         *
         * <p>The result is empty when the envelope fails the standard IQ-result echo checks or when a
         * {@code <list/>} child is present, which would indicate a mismatch variant that this arm does not handle.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetBlockListResponseSuccessWithMatch",
                exports = "parseGetBlockListResponseSuccessWithMatch",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessWithMatch> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            if (stanza.getChild("list").isPresent()) {
                return Optional.empty();
            }
            return Optional.of(new SuccessWithMatch());
        }

        /**
         * Compares this reply with another for equality by runtime type.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link SuccessWithMatch}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a constant hash code shared by every instance of this stateless variant.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return SuccessWithMatch.class.hashCode();
        }

        /**
         * Returns a debug representation of this stateless variant.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetBlockListResponse.SuccessWithMatch[]";
        }
    }

    /**
     * Carries the standard PN-addressed full blocklist returned when the client's cached digest is stale.
     *
     * <p>The items are addressed by their phone-number JID; the relay either omits the {@code addressing_mode}
     * attribute or sets it to {@code "pn"}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsGetBlockListResponseSuccessWithMismatch")
    final class SuccessWithMismatch implements SmaxGetBlockListResponse {
        /**
         * The new server-side digest of the blocklist, or {@code null} when the relay omitted it.
         */
        private final String listDhash;

        /**
         * Whether the relay tagged the list with {@code addressing_mode="pn"} explicitly.
         */
        private final boolean phoneNumberAddressed;

        /**
         * The parsed PN-addressed blocklist entries.
         */
        private final List<Item> listItem;

        /**
         * Constructs a PN-addressed mismatch reply, defensively copying the item list.
         *
         * @param listDhash            the new server digest; may be {@code null}
         * @param phoneNumberAddressed whether the relay tagged the list as PN-addressed
         * @param listItem             the parsed item list; never {@code null}
         * @throws NullPointerException if {@code listItem} is {@code null}
         */
        public SuccessWithMismatch(String listDhash, boolean phoneNumberAddressed, List<Item> listItem) {
            this.listDhash = listDhash;
            this.phoneNumberAddressed = phoneNumberAddressed;
            this.listItem = List.copyOf(Objects.requireNonNull(listItem, "listItem cannot be null"));
        }

        /**
         * Returns the new server digest when present.
         *
         * <p>The caller persists this so the next {@link SmaxGetBlockListRequest} can use it for the cache-match
         * short-circuit.
         *
         * @return an {@link Optional} carrying the digest, or empty when the relay omitted it
         */
        public Optional<String> listDhash() {
            return Optional.ofNullable(listDhash);
        }

        /**
         * Returns whether the relay marked the list as PN-addressed.
         *
         * <p>Distinguishes a relay-tagged PN list from one where the {@code addressing_mode} attribute was
         * omitted entirely; both are treated as PN by the caller.
         *
         * @return {@code true} when the relay set {@code addressing_mode="pn"} explicitly
         */
        public boolean phoneNumberAddressed() {
            return phoneNumberAddressed;
        }

        /**
         * Returns the parsed blocklist entries.
         *
         * <p>Each entry's {@link Item#jid()} is a PN-addressed user JID.
         *
         * @return an unmodifiable list of items; never {@code null}
         */
        public List<Item> listItem() {
            return listItem;
        }

        /**
         * Parses a PN-addressed mismatch variant.
         *
         * <p>The result is empty when the envelope is wrong, when {@code addressing_mode} is set to anything
         * other than {@code "pn"}, or when any per-item JID is missing (the per-item JID is mandatory on this
         * variant).
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetBlockListResponseSuccessWithMismatch",
                exports = "parseGetBlockListResponseSuccessWithMismatch",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessWithMismatch> of(Stanza stanza, Stanza request) {
            var list = validateMismatchEnvelope(stanza, request).orElse(null);
            if (list == null) {
                return Optional.empty();
            }
            var addressingMode = list.getAttributeAsString("addressing_mode").orElse(null);
            if (addressingMode != null && !addressingMode.equals("pn")) {
                return Optional.empty();
            }
            var dhash = list.getAttributeAsString("dhash").orElse(null);
            var items = parseItems(list, true).orElse(null);
            if (items == null) {
                return Optional.empty();
            }
            return Optional.of(new SuccessWithMismatch(dhash, addressingMode != null, items));
        }

        /**
         * Compares this reply with another for equality by digest, addressing flag, and items.
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
            return this.phoneNumberAddressed == that.phoneNumberAddressed
                    && Objects.equals(this.listDhash, that.listDhash)
                    && Objects.equals(this.listItem, that.listItem);
        }

        /**
         * Returns a hash code derived from the digest, addressing flag, and items.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(listDhash, phoneNumberAddressed, listItem);
        }

        /**
         * Returns a debug representation carrying the digest, addressing flag, and items.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetBlockListResponse.SuccessWithMismatch[listDhash=" + listDhash
                    + ", phoneNumberAddressed=" + phoneNumberAddressed
                    + ", listItem=" + listItem + ']';
        }
    }

    /**
     * Carries the regular PN-to-LID migration blocklist returned after the relay has migrated the user to LID.
     *
     * <p>Each entry is LID-addressed and not marked dirty; the caller routes the entries through the LID-aware
     * contact-resolution path before rendering.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsGetBlockListResponseMigratedSuccessWithMismatch")
    final class MigratedSuccessWithMismatch implements SmaxGetBlockListResponse {
        /**
         * The new server-side digest, or {@code null} when the relay omitted it.
         */
        private final String listDhash;

        /**
         * The parsed LID-addressed blocklist entries.
         */
        private final List<Item> listItem;

        /**
         * Constructs a migrated mismatch reply, defensively copying the item list.
         *
         * @param listDhash the new server digest; may be {@code null}
         * @param listItem  the parsed item list; never {@code null}
         * @throws NullPointerException if {@code listItem} is {@code null}
         */
        public MigratedSuccessWithMismatch(String listDhash, List<Item> listItem) {
            this.listDhash = listDhash;
            this.listItem = List.copyOf(Objects.requireNonNull(listItem, "listItem cannot be null"));
        }

        /**
         * Returns the new server digest when present.
         *
         * <p>Stored alongside the resulting LID-addressed list for use in the next cache-match request.
         *
         * @return an {@link Optional} carrying the digest, or empty when the relay omitted it
         */
        public Optional<String> listDhash() {
            return Optional.ofNullable(listDhash);
        }

        /**
         * Returns the parsed blocklist entries.
         *
         * <p>Each {@link Item#jid()} is a LID JID.
         *
         * @return an unmodifiable list of items; never {@code null}
         */
        public List<Item> listItem() {
            return listItem;
        }

        /**
         * Parses a regular-migration variant.
         *
         * <p>The result is empty when the envelope is wrong, when {@code addressing_mode} is not {@code "lid"},
         * or when any per-item JID is missing. The {@code dirty} marker is the sole discriminator between this
         * variant and {@link ForceMigratedSuccessWithMismatch}.
         *
         * @implNote This implementation does not check for {@code dirty="true"} here so that a dirty stanza falls
         * through the priority chain to {@link ForceMigratedSuccessWithMismatch}.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetBlockListResponseMigratedSuccessWithMismatch",
                exports = "parseGetBlockListResponseMigratedSuccessWithMismatch",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<MigratedSuccessWithMismatch> of(Stanza stanza, Stanza request) {
            var list = validateMismatchEnvelope(stanza, request).orElse(null);
            if (list == null) {
                return Optional.empty();
            }
            if (!list.hasAttribute("addressing_mode", "lid")) {
                return Optional.empty();
            }
            var dhash = list.getAttributeAsString("dhash").orElse(null);
            var items = parseItems(list, true).orElse(null);
            if (items == null) {
                return Optional.empty();
            }
            return Optional.of(new MigratedSuccessWithMismatch(dhash, items));
        }

        /**
         * Compares this reply with another for equality by digest and items.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link MigratedSuccessWithMismatch}
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
            return "SmaxGetBlockListResponse.MigratedSuccessWithMismatch[listDhash=" + listDhash
                    + ", listItem=" + listItem + ']';
        }
    }

    /**
     * Carries the force-migration blocklist returned when the relay aggressively converts a stale PN list to LID.
     *
     * <p>The {@code dirty="true"} marker cues the caller to log the forced migration and to expand
     * sparsely-populated entries through LID-migration utilities before rendering them.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsGetBlockListResponseForceMigratedSuccessWithMismatch")
    final class ForceMigratedSuccessWithMismatch implements SmaxGetBlockListResponse {
        /**
         * The new server-side digest, or {@code null} when the relay omitted it.
         */
        private final String listDhash;

        /**
         * The parsed LID-addressed blocklist entries; may contain items with missing JIDs.
         */
        private final List<Item> listItem;

        /**
         * Constructs a force-migration reply, defensively copying the item list.
         *
         * @param listDhash the new server digest; may be {@code null}
         * @param listItem  the parsed item list; never {@code null}
         * @throws NullPointerException if {@code listItem} is {@code null}
         */
        public ForceMigratedSuccessWithMismatch(String listDhash, List<Item> listItem) {
            this.listDhash = listDhash;
            this.listItem = List.copyOf(Objects.requireNonNull(listItem, "listItem cannot be null"));
        }

        /**
         * Returns the new server digest when present.
         *
         * <p>Stored alongside the resulting LID list so the next request can use the cache-match short-circuit.
         *
         * @return an {@link Optional} carrying the digest, or empty when the relay omitted it
         */
        public Optional<String> listDhash() {
            return Optional.ofNullable(listDhash);
        }

        /**
         * Returns the parsed blocklist entries.
         *
         * <p>Some entries may have a {@code null} JID; the caller recovers the LID from the entry's PN
         * discriminator.
         *
         * @return an unmodifiable list of items; never {@code null}
         */
        public List<Item> listItem() {
            return listItem;
        }

        /**
         * Parses a force-migration variant.
         *
         * <p>The result is empty when the envelope is wrong, when {@code addressing_mode} is not {@code "lid"},
         * or when the {@code dirty="true"} marker is absent. The per-item JID is optional on this variant, so
         * {@link #parseItems(Stanza, boolean)} is called with {@code requireJid=false}.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetBlockListResponseForceMigratedSuccessWithMismatch",
                exports = "parseGetBlockListResponseForceMigratedSuccessWithMismatch",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ForceMigratedSuccessWithMismatch> of(Stanza stanza, Stanza request) {
            var list = validateMismatchEnvelope(stanza, request).orElse(null);
            if (list == null) {
                return Optional.empty();
            }
            if (!list.hasAttribute("addressing_mode", "lid")) {
                return Optional.empty();
            }
            if (!list.hasAttribute("dirty", "true")) {
                return Optional.empty();
            }
            var dhash = list.getAttributeAsString("dhash").orElse(null);
            var items = parseItems(list, false).orElseThrow();
            return Optional.of(new ForceMigratedSuccessWithMismatch(dhash, items));
        }

        /**
         * Compares this reply with another for equality by digest and items.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link ForceMigratedSuccessWithMismatch}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ForceMigratedSuccessWithMismatch) obj;
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
            return "SmaxGetBlockListResponse.ForceMigratedSuccessWithMismatch[listDhash=" + listDhash
                    + ", listItem=" + listItem + ']';
        }
    }

    /**
     * Carries the Cloud-API mismatch blocklist returned by the cloud-relay flavour rather than the regular relay.
     *
     * <p>Its appearance indicates the user has been routed through the Cloud API path; the caller folds it back
     * into the standard LID rendering.
     *
     * @implNote This implementation is the fall-through arm of the LID-addressed parser priority chain: a LID
     * stanza whose items all carry a {@code jid} resolves to {@link MigratedSuccessWithMismatch}, a stanza marked
     * {@code dirty="true"} resolves to {@link ForceMigratedSuccessWithMismatch}, and any other LID stanza falls
     * through to this variant whose per-item JID is optional.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsGetBlockListResponseCAPISuccessWithMismatch")
    final class CAPISuccessWithMismatch implements SmaxGetBlockListResponse {
        /**
         * The new server-side digest, or {@code null} when the relay omitted it.
         */
        private final String listDhash;

        /**
         * The parsed blocklist entries; may contain items with missing JIDs.
         */
        private final List<Item> listItem;

        /**
         * Constructs a Cloud-API mismatch reply, defensively copying the item list.
         *
         * @param listDhash the new server digest; may be {@code null}
         * @param listItem  the parsed item list; never {@code null}
         * @throws NullPointerException if {@code listItem} is {@code null}
         */
        public CAPISuccessWithMismatch(String listDhash, List<Item> listItem) {
            this.listDhash = listDhash;
            this.listItem = List.copyOf(Objects.requireNonNull(listItem, "listItem cannot be null"));
        }

        /**
         * Returns the new server digest when present.
         *
         * @return an {@link Optional} carrying the digest, or empty when the relay omitted it
         */
        public Optional<String> listDhash() {
            return Optional.ofNullable(listDhash);
        }

        /**
         * Returns the parsed blocklist entries.
         *
         * <p>The list is processed by the same sparse-entry recovery path as
         * {@link ForceMigratedSuccessWithMismatch#listItem()}.
         *
         * @return an unmodifiable list of items; never {@code null}
         */
        public List<Item> listItem() {
            return listItem;
        }

        /**
         * Parses a Cloud-API mismatch variant.
         *
         * <p>The result is empty when the envelope is wrong or when {@code addressing_mode} is not {@code "lid"}.
         * The per-item JID is optional, so the parse never fails on missing JIDs.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the variant, or empty when the envelope shape does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetBlockListResponseCAPISuccessWithMismatch",
                exports = "parseGetBlockListResponseCAPISuccessWithMismatch",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<CAPISuccessWithMismatch> of(Stanza stanza, Stanza request) {
            var list = validateMismatchEnvelope(stanza, request).orElse(null);
            if (list == null) {
                return Optional.empty();
            }
            if (!list.hasAttribute("addressing_mode", "lid")) {
                return Optional.empty();
            }
            var dhash = list.getAttributeAsString("dhash").orElse(null);
            var items = parseItems(list, false).orElseThrow();
            return Optional.of(new CAPISuccessWithMismatch(dhash, items));
        }

        /**
         * Compares this reply with another for equality by digest and items.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link CAPISuccessWithMismatch}
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
            return "SmaxGetBlockListResponse.CAPISuccessWithMismatch[listDhash=" + listDhash
                    + ", listItem=" + listItem + ']';
        }
    }

    /**
     * Carries a malformed-request error reply.
     *
     * <p>The caller surfaces the {@code (errorCode, errorText)} pair as a fetch-failure log line and applies no
     * Cobalt-side retry policy.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsGetBlockListResponseInvalidRequest")
    final class ClientError implements SmaxGetBlockListResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetBlockListResponseInvalidRequest",
                exports = "parseGetBlockListResponseInvalidRequest",
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
            return "SmaxGetBlockListResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a transient server-error reply.
     *
     * <p>The caller logs the {@code (errorCode, errorText)} pair and bubbles it back without an inline retry,
     * leaving recovery to the caller.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBlocklistsGetBlockListResponseInternalServerError")
    final class ServerError implements SmaxGetBlockListResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsGetBlockListResponseInternalServerError",
                exports = "parseGetBlockListResponseInternalServerError",
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
            return "SmaxGetBlockListResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

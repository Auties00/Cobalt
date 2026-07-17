package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Fetches profile pictures for one or more groups (parent or sub-group) in a single round trip via an
 * {@code <iq type="get" xmlns="w:g2">} stanza.
 *
 * <p>Callers build one {@link PictureRequest} per group whose picture is needed; the relay batches up to
 * {@code 1000} entries in a single envelope. Pair this request with
 * {@link SmaxGroupsGetGroupProfilePicturesResponse} to read the per-group results.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsGetGroupProfilePicturesRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsGetGroupProfilePicturesProfilePicturesRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseGetGroupOrServerMixinGroup")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseGetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseGetServerMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsParentOrSubGroupMixinGroup")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsParentGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsSubGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsProfilePictureIdMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsProfilePictureTypeMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsProfilePictureQueryMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsSubGroupHintMixin")
public final class SmaxGroupsGetGroupProfilePicturesRequest implements SmaxStanza.Request {
    /**
     * Holds the optional addressing override stamped on the IQ envelope's {@code to} attribute; {@code null}
     * routes the IQ to the implicit {@code g.us} server.
     */
    private final Jid baseGroupJid;

    /**
     * Holds the optional {@link Jid} stamped on the {@code <pictures linked_groups_membership_hint="...">}
     * attribute; carries the caller's linked-community hint.
     */
    private final Jid linkedGroupsMembershipHint;

    /**
     * Holds the per-picture sub-requests, one per group whose picture is being fetched.
     */
    private final List<PictureRequest> pictures;

    /**
     * Constructs a request batching one or more picture sub-requests.
     *
     * <p>The caller sets {@code baseGroupJid} when the IQ must be routed to a specific group (most consumers
     * route to {@code g.us} and identify the group inside each {@link PictureRequest}), and sets
     * {@code linkedGroupsMembershipHint} to forward the community-membership hint to the relay.
     *
     * @param baseGroupJid               optional IQ-{@code to} group override; {@code null} routes to
     *                                   {@code g.us}
     * @param linkedGroupsMembershipHint optional linked-community hint; may be {@code null}
     * @param pictures                   the per-picture requests; never {@code null}, never empty
     * @throws NullPointerException     if {@code pictures} is {@code null}
     * @throws IllegalArgumentException if {@code pictures} is empty
     */
    public SmaxGroupsGetGroupProfilePicturesRequest(Jid baseGroupJid, Jid linkedGroupsMembershipHint, List<PictureRequest> pictures) {
        Objects.requireNonNull(pictures, "pictures cannot be null");
        if (pictures.isEmpty()) {
            throw new IllegalArgumentException("pictures cannot be empty");
        }
        this.baseGroupJid = baseGroupJid;
        this.linkedGroupsMembershipHint = linkedGroupsMembershipHint;
        this.pictures = List.copyOf(pictures);
    }

    /**
     * Returns the optional IQ-{@code to} group override.
     *
     * @return an {@link Optional} carrying the group {@link Jid}, or empty when the IQ is routed to
     *         {@code g.us}
     */
    public Optional<Jid> baseGroupJid() {
        return Optional.ofNullable(baseGroupJid);
    }

    /**
     * Returns the optional linked-community membership hint.
     *
     * @return an {@link Optional} carrying the hint {@link Jid}, or empty when the caller did not supply one
     */
    public Optional<Jid> linkedGroupsMembershipHint() {
        return Optional.ofNullable(linkedGroupsMembershipHint);
    }

    /**
     * Returns the per-picture requests.
     *
     * @return an unmodifiable list of picture requests; never {@code null}
     */
    public List<PictureRequest> pictures() {
        return pictures;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation builds one {@code <picture/>} child per {@link PictureRequest} (each with its own
     * addressing attribute and optional hints), wraps them in a {@code <pictures/>} container, stamps the
     * optional {@code linked_groups_membership_hint} on that container, then wraps the result in the
     * {@code <iq xmlns="w:g2" type="get">} envelope. The IQ's {@code to} attribute is the
     * {@link #baseGroupJid()} when supplied, otherwise {@link JidServer#groupOrCommunity()}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsGetGroupProfilePicturesRequest",
            exports = "makeGetGroupProfilePicturesRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var pictureNodes = new ArrayList<Stanza>(pictures.size());
        for (var pictureRequest : pictures) {
            var pictureBuilder = new StanzaBuilder()
                    .description("picture");
            var parentGroupJid = pictureRequest.parentGroupJid().orElse(null);
            if (parentGroupJid != null) {
                pictureBuilder.attribute("parent_group_jid", parentGroupJid);
            }
            var subGroupJid = pictureRequest.subGroupJid().orElse(null);
            if (subGroupJid != null) {
                pictureBuilder.attribute("sub_group_jid", subGroupJid);
            }
            var pictureId = pictureRequest.pictureId().orElse(null);
            if (pictureId != null) {
                pictureBuilder.attribute("id", pictureId);
            }
            var pictureType = pictureRequest.pictureType().orElse(null);
            if (pictureType != null) {
                pictureBuilder.attribute("type", pictureType);
            }
            var pictureQuery = pictureRequest.pictureQuery().orElse(null);
            if (pictureQuery != null) {
                pictureBuilder.attribute("query", pictureQuery);
            }
            pictureNodes.add(pictureBuilder.build());
        }
        var picturesBuilder = new StanzaBuilder()
                .description("pictures")
                .content(pictureNodes);
        if (linkedGroupsMembershipHint != null) {
            picturesBuilder.attribute("linked_groups_membership_hint", linkedGroupsMembershipHint);
        }
        var iqBuilder = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("type", "get");
        if (baseGroupJid != null) {
            iqBuilder.attribute("to", baseGroupJid);
        } else {
            iqBuilder.attribute("to", JidServer.groupOrCommunity());
        }
        iqBuilder.content(picturesBuilder.build());
        return iqBuilder;
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsGetGroupProfilePicturesRequest} with
     *         identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsGetGroupProfilePicturesRequest) obj;
        return Objects.equals(this.baseGroupJid, that.baseGroupJid)
                && Objects.equals(this.linkedGroupsMembershipHint, that.linkedGroupsMembershipHint)
                && Objects.equals(this.pictures, that.pictures);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(baseGroupJid, linkedGroupsMembershipHint, pictures);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsGetGroupProfilePicturesRequest[baseGroupJid=" + baseGroupJid
                + ", linkedGroupsMembershipHint=" + linkedGroupsMembershipHint
                + ", pictures=" + pictures + ']';
    }

    /**
     * Carries a per-group picture sub-payload inside a {@link SmaxGroupsGetGroupProfilePicturesRequest}.
     *
     * <p>Each entry carries exactly one of {@link #parentGroupJid()} or {@link #subGroupJid()} as the
     * addressing attribute; the optional {@link #pictureId()} lets the relay return the {@code did_not_change}
     * marker when the caller's cache is still current. {@link #pictureType()} selects resolution ({@code "image"}
     * or {@code "preview"}); {@link #pictureQuery()} selects the projection mode ({@code "url"} for URL plus
     * {@code direct_path}, {@code "blob"} for inline bytes).
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutGroupsGetGroupProfilePicturesProfilePicturesRequestMixin")
    public static final class PictureRequest {
        /**
         * Holds the parent-group {@link Jid} stamped on the {@code <picture parent_group_jid="...">}
         * attribute; mutually exclusive with {@link #subGroupJid}.
         */
        private final Jid parentGroupJid;

        /**
         * Holds the sub-group {@link Jid} stamped on the {@code <picture sub_group_jid="...">} attribute;
         * mutually exclusive with {@link #parentGroupJid}.
         */
        private final Jid subGroupJid;

        /**
         * Holds the optional dehydration-hint id of a previously-known picture; when supplied the relay returns
         * the {@code did_not_change} marker instead of re-shipping unchanged bytes.
         */
        private final String pictureId;

        /**
         * Holds the optional picture type ({@code "image"} or {@code "preview"}) selecting the resolution
         * variant.
         */
        private final String pictureType;

        /**
         * Holds the optional projection-mode hint ({@code "url"} for a URL plus {@code direct_path},
         * {@code "blob"} for inline bytes).
         */
        private final String pictureQuery;

        /**
         * Constructs a per-group picture sub-request.
         *
         * <p>Exactly one of {@code parentGroupJid} or {@code subGroupJid} must be supplied; the constructor
         * validates the disjunction.
         *
         * @param parentGroupJid optional parent-group {@link Jid}
         * @param subGroupJid    optional sub-group {@link Jid}
         * @param pictureId      optional dehydration-hint picture id
         * @param pictureType    optional picture type
         * @param pictureQuery   optional projection mode
         * @throws IllegalArgumentException if both {@code parentGroupJid} and {@code subGroupJid} are
         *                                  supplied or both are {@code null}
         */
        public PictureRequest(Jid parentGroupJid, Jid subGroupJid,
                              String pictureId, String pictureType, String pictureQuery) {
            if (parentGroupJid == null && subGroupJid == null) {
                throw new IllegalArgumentException(
                        "either parentGroupJid or subGroupJid must be supplied");
            }
            if (parentGroupJid != null && subGroupJid != null) {
                throw new IllegalArgumentException(
                        "parentGroupJid and subGroupJid are mutually exclusive");
            }
            this.parentGroupJid = parentGroupJid;
            this.subGroupJid = subGroupJid;
            this.pictureId = pictureId;
            this.pictureType = pictureType;
            this.pictureQuery = pictureQuery;
        }

        /**
         * Returns the parent-group {@link Jid} when this request targets a parent group.
         *
         * @return an {@link Optional} carrying the parent-group JID, or empty when the entry addresses a
         *         sub-group instead
         */
        public Optional<Jid> parentGroupJid() {
            return Optional.ofNullable(parentGroupJid);
        }

        /**
         * Returns the sub-group {@link Jid} when this request targets a sub-group.
         *
         * @return an {@link Optional} carrying the sub-group JID, or empty when the entry addresses a parent
         *         group instead
         */
        public Optional<Jid> subGroupJid() {
            return Optional.ofNullable(subGroupJid);
        }

        /**
         * Returns the optional dehydration-hint id.
         *
         * <p>Empty means the relay should always ship bytes; a non-empty value lets the relay return the
         * {@code did_not_change} marker when the cached picture is still current.
         *
         * @return an {@link Optional} carrying the picture id
         */
        public Optional<String> pictureId() {
            return Optional.ofNullable(pictureId);
        }

        /**
         * Returns the optional picture type.
         *
         * <p>{@code "image"} selects the full-resolution variant; {@code "preview"} selects the low-resolution
         * variant.
         *
         * @return an {@link Optional} carrying the picture type
         */
        public Optional<String> pictureType() {
            return Optional.ofNullable(pictureType);
        }

        /**
         * Returns the optional projection mode.
         *
         * <p>{@code "url"} returns a CDN URL plus {@code direct_path}; {@code "blob"} returns the picture bytes
         * inline.
         *
         * @return an {@link Optional} carrying the projection mode
         */
        public Optional<String> pictureQuery() {
            return Optional.ofNullable(pictureQuery);
        }

        /**
         * Compares this sub-request to {@code obj} for value equality across every field.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link PictureRequest} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (PictureRequest) obj;
            return Objects.equals(this.parentGroupJid, that.parentGroupJid)
                    && Objects.equals(this.subGroupJid, that.subGroupJid)
                    && Objects.equals(this.pictureId, that.pictureId)
                    && Objects.equals(this.pictureType, that.pictureType)
                    && Objects.equals(this.pictureQuery, that.pictureQuery);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(parentGroupJid, subGroupJid, pictureId, pictureType, pictureQuery);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetGroupProfilePicturesRequest.PictureRequest[parentGroupJid="
                    + parentGroupJid + ", subGroupJid=" + subGroupJid
                    + ", pictureId=" + pictureId + ", pictureType=" + pictureType
                    + ", pictureQuery=" + pictureQuery + ']';
        }
    }
}

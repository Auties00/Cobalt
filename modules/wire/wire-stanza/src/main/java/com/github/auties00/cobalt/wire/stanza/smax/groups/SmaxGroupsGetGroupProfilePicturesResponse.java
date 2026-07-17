package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the inbound reply to a {@link SmaxGroupsGetGroupProfilePicturesRequest} as a sealed variant family.
 *
 * <p>{@link Success} carries the per-requested-group picture rows; {@link ClientError} and {@link ServerError}
 * surface caller-side and relay-side failures. Callers pattern-match the variant returned by
 * {@link #of(Stanza, Stanza)}.
 */
public sealed interface SmaxGroupsGetGroupProfilePicturesResponse extends SmaxStanza.Response
        permits SmaxGroupsGetGroupProfilePicturesResponse.Success, SmaxGroupsGetGroupProfilePicturesResponse.ClientError, SmaxGroupsGetGroupProfilePicturesResponse.ServerError {

    /**
     * Parses the inbound IQ stanza into the first matching variant.
     *
     * <p>The probes run in priority order: {@link Success}, {@link ClientError}, then {@link ServerError}. An
     * empty {@link Optional} signals a stanza shape outside the documented union.
     *
     * @implNote
     * This implementation does not throw a parsing-failure exception, leaving the recovery decision to the
     * caller.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound {@link SmaxGroupsGetGroupProfilePicturesRequest} stanza, used to
     *                validate the echoed {@code id} attribute; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented
     *         variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsGetGroupProfilePicturesRPC",
            exports = "sendGetGroupProfilePicturesRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsGetGroupProfilePicturesResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Reports that the relay echoed the {@code <pictures/>} wrapper carrying one {@link Picture} per requested
     * group.
     *
     * <p>Callers iterate {@link #pictures()} in lock-step with the request's
     * {@link SmaxGroupsGetGroupProfilePicturesRequest#pictures()} to surface per-group results; each entry
     * carries either a URL projection, an inline blob, or a partial-branch marker.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetGroupProfilePicturesResponseSuccessGroupPictures")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetGroupProfilePicturesProfilePicturesResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetGroupProfilePicturesSuccessOrGetGroupProfilePicturesPartialProfilePictureResponseMixinGroup")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsParentOrSubGroupMixinGroup")
    final class Success implements SmaxGroupsGetGroupProfilePicturesResponse {
        /**
         * Holds the per-group picture replies, one per requested group.
         */
        private final List<Picture> pictures;

        /**
         * Constructs a success variant.
         *
         * <p>Production instances are typically produced by {@link #of(Stanza, Stanza)}; direct construction seeds
         * test fixtures.
         *
         * @param pictures the per-picture replies; never {@code null}
         * @throws NullPointerException if {@code pictures} is {@code null}
         */
        public Success(List<Picture> pictures) {
            Objects.requireNonNull(pictures, "pictures cannot be null");
            this.pictures = List.copyOf(pictures);
        }

        /**
         * Returns the per-picture replies.
         *
         * @return an unmodifiable list of picture replies; never {@code null}
         */
        public List<Picture> pictures() {
            return pictures;
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant.
         *
         * <p>Runs as the first probe in the variant cascade of
         * {@link SmaxGroupsGetGroupProfilePicturesResponse#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation validates the IQ envelope via
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, extracts the {@code <pictures/>} wrapper, then
         * iterates its {@code <picture/>} children and parses each one via {@link Picture#of(Stanza)}. Any failed
         * child parse short-circuits and the whole variant is rejected.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the
         *         stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetGroupProfilePicturesResponseSuccessGroupPictures",
                exports = "parseGetGroupProfilePicturesResponseSuccessGroupPictures",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var picturesWrapper = stanza.getChild("pictures").orElse(null);
            if (picturesWrapper == null) {
                return Optional.empty();
            }
            var pictureChildren = picturesWrapper.getChildren("picture");
            if (pictureChildren.isEmpty()) {
                return Optional.empty();
            }
            var pictures = new ArrayList<Picture>(pictureChildren.size());
            for (var pictureNode : pictureChildren) {
                var picture = Picture.of(pictureNode).orElse(null);
                if (picture == null) {
                    return Optional.empty();
                }
                pictures.add(picture);
            }
            return Optional.of(new Success(pictures));
        }

        /**
         * Compares this variant to {@code obj} for value equality across the picture replies.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Success} with identical picture replies
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.pictures, that.pictures);
        }

        /**
         * Returns a hash composed of the picture replies.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(pictures);
        }

        /**
         * Returns a debug string carrying the picture replies.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetGroupProfilePicturesResponse.Success[pictures=" + pictures + ']';
        }
    }

    /**
     * Carries a per-group picture projection inside a {@link Success}.
     *
     * <p>The relay's per-picture response is a disjunction of two sub-shapes: a success projection (carrying
     * either a URL plus {@code direct_path} or inline blob bytes) and a partial projection (carrying a
     * {@code did_not_change} / {@code not_found} / error marker). This class unifies both branches;
     * {@link #url()}, {@link #directPath()}, and {@link #blob()} are populated only on the success branch, and
     * the verbatim {@code <picture/>} child is exposed via {@link #raw()} so callers can inspect any
     * partial-branch marker.
     *
     * @implNote
     * This implementation collapses the two WA Web mixin modules into one Java class because the two branches
     * are distinguishable by inspecting the absence or presence of the URL or blob payload, removing the need
     * for a sealed-interface oneof at this leaf level.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetGroupProfilePicturesSuccessProfilePictureResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetGroupProfilePicturesPartialProfilePictureResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsProfilePictureUrlResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsProfilePictureBlobResponseMixin")
    final class Picture {
        /**
         * Holds the parent-group {@link Jid} when this entry targets a parent group; mutually exclusive with
         * {@link #subGroupJid}.
         */
        private final Jid parentGroupJid;

        /**
         * Holds the sub-group {@link Jid} when this entry targets a sub-group; mutually exclusive with
         * {@link #parentGroupJid}.
         */
        private final Jid subGroupJid;

        /**
         * Holds the picture id echoed by the relay; {@code null} when omitted.
         */
        private final String pictureId;

        /**
         * Holds the picture type ({@code "image"} or {@code "preview"}) echoed by the relay; {@code null} when
         * omitted.
         */
        private final String pictureType;

        /**
         * Holds the picture URL; populated only on the URL-projection success branch.
         */
        private final String url;

        /**
         * Holds the {@code direct_path} attribute; populated only on the URL-projection success branch.
         */
        private final String directPath;

        /**
         * Holds the inline blob bytes; populated only on the blob-projection success branch.
         */
        private final byte[] blob;

        /**
         * Holds the raw {@code <picture/>} stanza carrying any partial-branch marker
         * ({@code did_not_change} / {@code not_found} / {@code bad_*}).
         */
        private final Stanza raw;

        /**
         * Constructs a picture entry.
         *
         * <p>Production instances are typically produced by {@link #of(Stanza)}; direct construction seeds test
         * fixtures.
         *
         * @param parentGroupJid optional parent-group {@link Jid}
         * @param subGroupJid    optional sub-group {@link Jid}
         * @param pictureId      optional picture id
         * @param pictureType    optional picture type
         * @param url            optional picture URL
         * @param directPath     optional direct-path attribute
         * @param blob           optional inline blob bytes
         * @param raw            the raw {@code <picture/>} {@link Stanza}; never {@code null}
         * @throws NullPointerException if {@code raw} is {@code null}
         */
        public Picture(Jid parentGroupJid, Jid subGroupJid,
                       String pictureId, String pictureType,
                       String url, String directPath, byte[] blob, Stanza raw) {
            this.parentGroupJid = parentGroupJid;
            this.subGroupJid = subGroupJid;
            this.pictureId = pictureId;
            this.pictureType = pictureType;
            this.url = url;
            this.directPath = directPath;
            this.blob = blob;
            this.raw = Objects.requireNonNull(raw, "raw cannot be null");
        }

        /**
         * Returns the parent-group {@link Jid} when set.
         *
         * @return an {@link Optional} carrying the JID
         */
        public Optional<Jid> parentGroupJid() {
            return Optional.ofNullable(parentGroupJid);
        }

        /**
         * Returns the sub-group {@link Jid} when set.
         *
         * @return an {@link Optional} carrying the JID
         */
        public Optional<Jid> subGroupJid() {
            return Optional.ofNullable(subGroupJid);
        }

        /**
         * Returns the picture id when supplied by the relay.
         *
         * @return an {@link Optional} carrying the id
         */
        public Optional<String> pictureId() {
            return Optional.ofNullable(pictureId);
        }

        /**
         * Returns the picture type when supplied by the relay.
         *
         * @return an {@link Optional} carrying the type ({@code "image"} or {@code "preview"})
         */
        public Optional<String> pictureType() {
            return Optional.ofNullable(pictureType);
        }

        /**
         * Returns the picture URL on the URL-projection branch.
         *
         * @return an {@link Optional} carrying the URL; empty on the blob-projection or partial branches
         */
        public Optional<String> url() {
            return Optional.ofNullable(url);
        }

        /**
         * Returns the {@code direct_path} attribute on the URL-projection branch.
         *
         * @return an {@link Optional} carrying the direct path; empty on the blob-projection or partial
         *         branches
         */
        public Optional<String> directPath() {
            return Optional.ofNullable(directPath);
        }

        /**
         * Returns the inline blob bytes on the blob-projection branch.
         *
         * @return an {@link Optional} carrying the blob bytes; empty on the URL-projection or partial
         *         branches
         */
        public Optional<byte[]> blob() {
            return Optional.ofNullable(blob);
        }

        /**
         * Returns the raw {@code <picture/>} stanza carrying any partial-branch sub-marker
         * ({@code did_not_change} / {@code not_found} / {@code bad_*}).
         *
         * <p>Callers inspect this stanza when {@link #url()}, {@link #directPath()}, and {@link #blob()} are all
         * empty to disambiguate the partial-branch reason.
         *
         * @return the raw {@link Stanza}; never {@code null}
         */
        public Stanza raw() {
            return raw;
        }

        /**
         * Parses a {@link Picture} from the given {@code <picture/>} child.
         *
         * <p>Called by {@link Success#of(Stanza, Stanza)} for each {@code <picture/>} child inside the
         * {@code <pictures/>} wrapper.
         *
         * @implNote
         * This implementation requires the child to carry exactly one of {@code parent_group_jid} or
         * {@code sub_group_jid}; absence of both signals a wire-format violation and the call returns
         * {@link Optional#empty()}. The {@code url}, {@code direct_path}, and inline blob bytes are read
         * unconditionally and surface as empty {@link Optional}s on the partial branch.
         *
         * @param stanza the {@code <picture/>} child stanza
         * @return an {@link Optional} carrying the parsed picture, or {@link Optional#empty()} when the
         *         child does not satisfy the addressing-disjunction schema
         */
        public static Optional<Picture> of(Stanza stanza) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            if (!stanza.hasDescription("picture")) {
                return Optional.empty();
            }
            var parentGroupJid = stanza.getAttributeAsJid("parent_group_jid").orElse(null);
            var subGroupJid = stanza.getAttributeAsJid("sub_group_jid").orElse(null);
            if (parentGroupJid == null && subGroupJid == null) {
                return Optional.empty();
            }
            var pictureId = stanza.getAttributeAsString("id").orElse(null);
            var pictureType = stanza.getAttributeAsString("type").orElse(null);
            var url = stanza.getAttributeAsString("url").orElse(null);
            var directPath = stanza.getAttributeAsString("direct_path").orElse(null);
            var blob = stanza.toContentBytes().orElse(null);
            var picture = new Picture(parentGroupJid, subGroupJid, pictureId,
                    pictureType, url, directPath, blob, stanza);
            return Optional.of(picture);
        }

        /**
         * Compares this entry to {@code obj} for value equality across every field.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Picture} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Picture) obj;
            return Objects.equals(this.parentGroupJid, that.parentGroupJid)
                    && Objects.equals(this.subGroupJid, that.subGroupJid)
                    && Objects.equals(this.pictureId, that.pictureId)
                    && Objects.equals(this.pictureType, that.pictureType)
                    && Objects.equals(this.url, that.url)
                    && Objects.equals(this.directPath, that.directPath)
                    && Arrays.equals(this.blob, that.blob)
                    && Objects.equals(this.raw, that.raw);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(parentGroupJid, subGroupJid, pictureId, pictureType,
                    url, directPath, Arrays.hashCode(blob), raw);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetGroupProfilePicturesResponse.Picture[parentGroupJid="
                    + parentGroupJid + ", subGroupJid=" + subGroupJid
                    + ", pictureId=" + pictureId + ", pictureType=" + pictureType
                    + ", url=" + url + ", directPath=" + directPath
                    + ", blob=" + Arrays.toString(blob)
                    + ", raw=" + raw + ']';
        }
    }

    /**
     * Reports that the relay rejected the request as malformed or unauthorised.
     *
     * <p>Carries the numeric {@link #errorCode()} and optional {@link #errorText()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetGroupProfilePicturesResponseClientError")
    final class ClientError implements SmaxGroupsGetGroupProfilePicturesResponse {
        /**
         * Holds the numeric server-side error code, mirroring the {@code <error code="...">} attribute on the
         * inbound stanza.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text echoed by the relay; {@code null} when the relay omitted the
         * {@code <error text="...">} attribute.
         */
        private final String errorText;

        /**
         * Constructs a client-error variant.
         *
         * <p>Production instances are typically produced by {@link #of(Stanza, Stanza)}; direct construction seeds
         * test fixtures.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric server-side error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ClientError} envelope.
         *
         * <p>Runs as the second probe in the variant cascade of
         * {@link SmaxGroupsGetGroupProfilePicturesResponse#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation delegates the error-envelope extraction to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} so every SMAX response in the family
         * shares the same client-error parsing.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the
         *         envelope does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetGroupProfilePicturesResponseClientError",
                exports = "parseGetGroupProfilePicturesResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link ClientError} with identical fields
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
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetGroupProfilePicturesResponse.ClientError[errorCode="
                    + errorCode + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Reports that the relay encountered a transient internal failure.
     *
     * <p>Callers decide whether to retry based on the surfaced {@link #errorCode()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetGroupProfilePicturesResponseServerError")
    final class ServerError implements SmaxGroupsGetGroupProfilePicturesResponse {
        /**
         * Holds the numeric server-side error code, mirroring the {@code <error code="...">} attribute on the
         * inbound stanza.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text echoed by the relay; {@code null} when the relay omitted the
         * {@code <error text="...">} attribute.
         */
        private final String errorText;

        /**
         * Constructs a server-error variant.
         *
         * <p>Production instances are typically produced by {@link #of(Stanza, Stanza)}; direct construction seeds
         * test fixtures.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric server-side error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ServerError} envelope.
         *
         * <p>Runs as the terminal probe in the variant cascade of
         * {@link SmaxGroupsGetGroupProfilePicturesResponse#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation delegates the error-envelope extraction to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} so every SMAX response in the family
         * shares the same server-error parsing.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the
         *         envelope does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetGroupProfilePicturesResponseServerError",
                exports = "parseGetGroupProfilePicturesResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link ServerError} with identical fields
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
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetGroupProfilePicturesResponse.ServerError[errorCode="
                    + errorCode + ", errorText=" + errorText + ']';
        }
    }
}

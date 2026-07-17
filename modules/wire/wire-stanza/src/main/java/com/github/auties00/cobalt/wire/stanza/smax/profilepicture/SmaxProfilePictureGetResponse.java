package com.github.auties00.cobalt.wire.stanza.smax.profilepicture;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
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
 * Models the relay's reply to a {@link SmaxProfilePictureGetRequest} as one of
 * four success arms or an error envelope.
 *
 * <p>The four success variants map onto the WA Web get-profile-picture
 * branches: {@link SuccessPictureURL} for the CDN-hosted-URL case,
 * {@link SuccessAvatarURLs} for persona avatars, {@link SuccessPictureBlob} for
 * inlined small pictures, and {@link SuccessNoData} for the no-picture case.
 * The {@link Error} arm carries one of seven documented {@code (code, text)}
 * pairs. The variant is resolved from an inbound stanza through
 * {@link #of(Stanza, Stanza)}.
 */
public sealed interface SmaxProfilePictureGetResponse extends SmaxStanza.Response
        permits SmaxProfilePictureGetResponse.SuccessPictureURL, SmaxProfilePictureGetResponse.SuccessAvatarURLs,
        SmaxProfilePictureGetResponse.SuccessPictureBlob, SmaxProfilePictureGetResponse.SuccessNoData, SmaxProfilePictureGetResponse.Error {

    /**
     * Resolves an inbound IQ reply into the first matching variant.
     *
     * <p>Variants are tried in URL, then avatar, then blob, then no-data, then
     * error priority; the first one that parses wins.
     *
     * @implNote
     * This implementation mirrors the WA Web {@code sendGetRPC} disjunction's
     * priority order.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the originating outbound IQ stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or
     *         {@link Optional#empty()} when none matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxProfilePictureGetRPC",
            exports = "sendGetRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxProfilePictureGetResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var pictureUrl = SuccessPictureURL.of(stanza, request);
        if (pictureUrl.isPresent()) {
            return pictureUrl;
        }
        var avatarUrls = SuccessAvatarURLs.of(stanza, request);
        if (avatarUrls.isPresent()) {
            return avatarUrls;
        }
        var pictureBlob = SuccessPictureBlob.of(stanza, request);
        if (pictureBlob.isPresent()) {
            return pictureBlob;
        }
        var noData = SuccessNoData.of(stanza, request);
        if (noData.isPresent()) {
            return noData;
        }
        return Error.of(stanza, request);
    }

    /**
     * Models the CDN-hosted picture-URL reply, carrying the picture id, type,
     * URL, direct-path segment, and optional integrity fields.
     *
     * <p>This arm is surfaced when the relay resolves the request to a remote
     * picture stored on the WhatsApp CDN; callers compose a full media URL from
     * {@link #pictureUrl()} (already absolute) or from
     * {@link #pictureDirectPath()} (segment only) plus the media-host pool.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureGetResponseSuccessPictureURL")
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureIQResultResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureEnums")
    final class SuccessPictureURL implements SmaxProfilePictureGetResponse {
        /**
         * The opaque picture id; usable as a cache key on subsequent fetches.
         */
        private final String pictureId;

        /**
         * The picture type literal; one of {@code "image"} or {@code "preview"}.
         */
        private final String pictureType;

        /**
         * The absolute CDN URL of the picture.
         */
        private final String pictureUrl;

        /**
         * The CDN direct-path segment, composed by the local download pipeline
         * into a full media URL against the active media-host pool.
         */
        private final String pictureDirectPath;

        /**
         * The optional content hash for integrity-checking.
         */
        private final String pictureHash;

        /**
         * The optional {@code has_staging} marker; one of {@code "false"} or
         * {@code "true"}.
         */
        private final String pictureHasStaging;

        /**
         * Constructs a picture-URL reply from the parsed attributes.
         *
         * @param pictureId         the picture id; never {@code null}
         * @param pictureType       the picture type; never {@code null}
         * @param pictureUrl        the CDN URL; never {@code null}
         * @param pictureDirectPath the direct-path segment; never {@code null}
         * @param pictureHash       the optional hash; may be {@code null}
         * @param pictureHasStaging the optional staging marker; may be
         *                          {@code null}
         * @throws NullPointerException if any required argument is {@code null}
         */
        public SuccessPictureURL(String pictureId, String pictureType, String pictureUrl,
                                 String pictureDirectPath, String pictureHash,
                                 String pictureHasStaging) {
            this.pictureId = Objects.requireNonNull(pictureId, "pictureId cannot be null");
            this.pictureType = Objects.requireNonNull(pictureType, "pictureType cannot be null");
            this.pictureUrl = Objects.requireNonNull(pictureUrl, "pictureUrl cannot be null");
            this.pictureDirectPath = Objects.requireNonNull(pictureDirectPath, "pictureDirectPath cannot be null");
            this.pictureHash = pictureHash;
            this.pictureHasStaging = pictureHasStaging;
        }

        /**
         * Returns the picture id.
         *
         * @return the id; never {@code null}
         */
        public String pictureId() {
            return pictureId;
        }

        /**
         * Returns the picture type.
         *
         * @return the type; never {@code null}
         */
        public String pictureType() {
            return pictureType;
        }

        /**
         * Returns the CDN URL.
         *
         * @return the URL; never {@code null}
         */
        public String pictureUrl() {
            return pictureUrl;
        }

        /**
         * Returns the CDN direct-path segment.
         *
         * @return the path; never {@code null}
         */
        public String pictureDirectPath() {
            return pictureDirectPath;
        }

        /**
         * Returns the optional content hash.
         *
         * @return an {@link Optional} carrying the hash, or
         *         {@link Optional#empty()} when omitted
         */
        public Optional<String> pictureHash() {
            return Optional.ofNullable(pictureHash);
        }

        /**
         * Returns the optional staging marker.
         *
         * @return an {@link Optional} carrying the marker, or
         *         {@link Optional#empty()} when omitted
         */
        public Optional<String> pictureHasStaging() {
            return Optional.ofNullable(pictureHasStaging);
        }

        /**
         * Parses a picture-URL reply from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} for any deviation from the
         * documented URL schema: missing or wrong type, missing url or
         * direct_path, or a malformed staging marker.
         *
         * @implNote
         * This implementation delegates IQ-envelope validation to
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} and accepts
         * only the {@code "image"} and {@code "preview"} type literals matching
         * WA Web's {@code parseGetResponseSuccessPictureURL} gate.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInProfilePictureGetResponseSuccessPictureURL",
                exports = "parseGetResponseSuccessPictureURL",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessPictureURL> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var picture = stanza.getChild("picture").orElse(null);
            if (picture == null) {
                return Optional.empty();
            }
            var id = picture.getAttributeAsString("id").orElse(null);
            if (id == null) {
                return Optional.empty();
            }
            var type = picture.getAttributeAsString("type").orElse(null);
            if (type == null || (!"image".equals(type) && !"preview".equals(type))) {
                return Optional.empty();
            }
            var url = picture.getAttributeAsString("url").orElse(null);
            if (url == null) {
                return Optional.empty();
            }
            var directPath = picture.getAttributeAsString("direct_path").orElse(null);
            if (directPath == null) {
                return Optional.empty();
            }
            var hash = picture.getAttributeAsString("hash").orElse(null);
            var hasStaging = picture.getAttributeAsString("has_staging").orElse(null);
            if (hasStaging != null && !"false".equals(hasStaging) && !"true".equals(hasStaging)) {
                return Optional.empty();
            }
            return Optional.of(new SuccessPictureURL(id, type, url, directPath, hash, hasStaging));
        }

        /**
         * Compares this picture-URL reply to another for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link SuccessPictureURL}
         *         with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (SuccessPictureURL) obj;
            return Objects.equals(this.pictureId, that.pictureId)
                    && Objects.equals(this.pictureType, that.pictureType)
                    && Objects.equals(this.pictureUrl, that.pictureUrl)
                    && Objects.equals(this.pictureDirectPath, that.pictureDirectPath)
                    && Objects.equals(this.pictureHash, that.pictureHash)
                    && Objects.equals(this.pictureHasStaging, that.pictureHasStaging);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(pictureId, pictureType, pictureUrl, pictureDirectPath,
                    pictureHash, pictureHasStaging);
        }

        /**
         * Returns a debug-friendly representation of this reply.
         *
         * <p>The format is intended for logging and is not part of the
         * contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxProfilePictureGetResponse.SuccessPictureURL[pictureId=" + pictureId
                    + ", pictureType=" + pictureType
                    + ", pictureUrl=" + pictureUrl
                    + ", pictureDirectPath=" + pictureDirectPath
                    + ", pictureHash=" + pictureHash
                    + ", pictureHasStaging=" + pictureHasStaging + ']';
        }
    }

    /**
     * Models the avatar-URL list reply, carrying one URL per requested pose.
     *
     * <p>This arm is surfaced when the originating
     * {@link SmaxProfilePictureGetRequest} carried an
     * {@link SmaxProfilePictureGetAvatarMixin}; it holds one {@link AvatarUrl}
     * per requested pose-id, between {@code 1} and {@code 4} entries.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureGetResponseSuccessAvatarURLs")
    final class SuccessAvatarURLs implements SmaxProfilePictureGetResponse {
        /**
         * The avatar entries; between {@code 1} and {@code 4}.
         */
        private final List<AvatarUrl> avatars;

        /**
         * Constructs an avatar-URLs reply from the parsed entries.
         *
         * @implNote
         * This implementation defensively copies the input list via
         * {@link List#copyOf(java.util.Collection)}.
         *
         * @param avatars the avatar entries; never {@code null}
         * @throws NullPointerException if {@code avatars} is {@code null}
         */
        public SuccessAvatarURLs(List<AvatarUrl> avatars) {
            Objects.requireNonNull(avatars, "avatars cannot be null");
            this.avatars = List.copyOf(avatars);
        }

        /**
         * Returns the avatar entries.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<AvatarUrl> avatars() {
            return avatars;
        }

        /**
         * Parses an avatar-URLs reply from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} for any deviation from the
         * documented avatar schema or for an empty or oversize list.
         *
         * @implNote
         * This implementation enforces the {@code 1..4} bound after parsing the
         * children, matching WA Web's {@code mapChildrenWithTag(avatar, 1, 4)}
         * gate.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInProfilePictureGetResponseSuccessAvatarURLs",
                exports = "parseGetResponseSuccessAvatarURLs",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessAvatarURLs> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var avatars = new ArrayList<AvatarUrl>();
            for (var avatarNode : stanza.getChildren("avatar")) {
                var avatar = AvatarUrl.of(avatarNode).orElse(null);
                if (avatar == null) {
                    return Optional.empty();
                }
                avatars.add(avatar);
            }
            if (avatars.isEmpty() || avatars.size() > 4) {
                return Optional.empty();
            }
            return Optional.of(new SuccessAvatarURLs(avatars));
        }

        /**
         * Compares this avatar-URLs reply to another for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link SuccessAvatarURLs}
         *         with equal entries
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (SuccessAvatarURLs) obj;
            return Objects.equals(this.avatars, that.avatars);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(avatars);
        }

        /**
         * Returns a debug-friendly representation of this reply.
         *
         * <p>The format is intended for logging and is not part of the
         * contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxProfilePictureGetResponse.SuccessAvatarURLs[avatars=" + avatars + ']';
        }

        /**
         * Models a single {@code <avatar url pose_id hash?/>} entry.
         */
        public static final class AvatarUrl {
            /**
             * The CDN URL of this avatar pose.
             */
            private final String url;

            /**
             * The pose id.
             */
            private final String poseId;

            /**
             * The optional content hash.
             */
            private final String hash;

            /**
             * Constructs an avatar-URL entry from the parsed attributes.
             *
             * @param url    the URL; never {@code null}
             * @param poseId the pose id; never {@code null}
             * @param hash   the optional hash; may be {@code null}
             * @throws NullPointerException if any required argument is
             *                              {@code null}
             */
            public AvatarUrl(String url, String poseId, String hash) {
                this.url = Objects.requireNonNull(url, "url cannot be null");
                this.poseId = Objects.requireNonNull(poseId, "poseId cannot be null");
                this.hash = hash;
            }

            /**
             * Returns the CDN URL.
             *
             * @return the URL; never {@code null}
             */
            public String url() {
                return url;
            }

            /**
             * Returns the pose id.
             *
             * @return the id; never {@code null}
             */
            public String poseId() {
                return poseId;
            }

            /**
             * Returns the optional content hash.
             *
             * @return an {@link Optional} carrying the hash, or
             *         {@link Optional#empty()} when omitted
             */
            public Optional<String> hash() {
                return Optional.ofNullable(hash);
            }

            /**
             * Parses an avatar-URL entry from the given {@code <avatar>} child.
             *
             * <p>Returns {@link Optional#empty()} for any deviation from the
             * avatar-entry schema: a non-{@code <avatar>} tag or a missing url
             * or pose_id.
             *
             * @param stanza the {@code <avatar>} child
             * @return an {@link Optional} carrying the parsed entry
             */
            @WhatsAppWebExport(moduleName = "WASmaxInProfilePictureGetResponseSuccessAvatarURLs",
                    exports = "parseGetResponseSuccessAvatarURLsAvatar",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<AvatarUrl> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("avatar")) {
                    return Optional.empty();
                }
                var url = stanza.getAttributeAsString("url").orElse(null);
                if (url == null) {
                    return Optional.empty();
                }
                var poseId = stanza.getAttributeAsString("pose_id").orElse(null);
                if (poseId == null) {
                    return Optional.empty();
                }
                var hash = stanza.getAttributeAsString("hash").orElse(null);
                return Optional.of(new AvatarUrl(url, poseId, hash));
            }

            /**
             * Compares this entry to another for value equality.
             *
             * @param obj the object to compare against
             * @return {@code true} when {@code obj} is an {@link AvatarUrl} with
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
                var that = (AvatarUrl) obj;
                return Objects.equals(this.url, that.url)
                        && Objects.equals(this.poseId, that.poseId)
                        && Objects.equals(this.hash, that.hash);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(url, poseId, hash);
            }

            /**
             * Returns a debug-friendly representation of this entry.
             *
             * <p>The format is intended for logging and is not part of the
             * contract.
             *
             * @return the string form
             */
            @Override
            public String toString() {
                return "SmaxProfilePictureGetResponse.SuccessAvatarURLs.AvatarUrl[url=" + url
                        + ", poseId=" + poseId
                        + ", hash=" + hash + ']';
            }
        }
    }

    /**
     * Models the inlined-blob reply, carrying the small picture bytes shipped as
     * the {@code <picture/>} element value.
     *
     * <p>This arm is surfaced when the relay decided the picture is small enough
     * to ship inline, letting callers skip the separate CDN fetch round-trip;
     * the bytes are already in hand.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureGetResponseSuccessPictureBlob")
    final class SuccessPictureBlob implements SmaxProfilePictureGetResponse {
        /**
         * The opaque picture id.
         */
        private final String pictureId;

        /**
         * The picture type literal; one of {@code "image"} or {@code "preview"}.
         */
        private final String pictureType;

        /**
         * The optional staging marker.
         */
        private final String pictureHasStaging;

        /**
         * The inlined raw picture bytes.
         */
        private final byte[] pictureElementValue;

        /**
         * Constructs a picture-blob reply from the parsed attributes and bytes.
         *
         * @param pictureId           the picture id; never {@code null}
         * @param pictureType         the picture type; never {@code null}
         * @param pictureHasStaging   the optional staging marker; may be
         *                            {@code null}
         * @param pictureElementValue the raw bytes; never {@code null}
         * @throws NullPointerException if any required argument is {@code null}
         */
        public SuccessPictureBlob(String pictureId, String pictureType,
                                  String pictureHasStaging, byte[] pictureElementValue) {
            this.pictureId = Objects.requireNonNull(pictureId, "pictureId cannot be null");
            this.pictureType = Objects.requireNonNull(pictureType, "pictureType cannot be null");
            this.pictureHasStaging = pictureHasStaging;
            this.pictureElementValue = Objects.requireNonNull(pictureElementValue,
                    "pictureElementValue cannot be null");
        }

        /**
         * Returns the picture id.
         *
         * @return the id; never {@code null}
         */
        public String pictureId() {
            return pictureId;
        }

        /**
         * Returns the picture type.
         *
         * @return the type; never {@code null}
         */
        public String pictureType() {
            return pictureType;
        }

        /**
         * Returns the optional staging marker.
         *
         * @return an {@link Optional} carrying the marker, or
         *         {@link Optional#empty()} when omitted
         */
        public Optional<String> pictureHasStaging() {
            return Optional.ofNullable(pictureHasStaging);
        }

        /**
         * Returns the inlined picture bytes.
         *
         * @return the bytes; never {@code null}
         */
        public byte[] pictureElementValue() {
            return pictureElementValue;
        }

        /**
         * Parses a picture-blob reply from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} for any deviation from the
         * documented blob schema: missing or wrong type, malformed staging
         * marker, or an empty element value.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInProfilePictureGetResponseSuccessPictureBlob",
                exports = "parseGetResponseSuccessPictureBlob",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessPictureBlob> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var picture = stanza.getChild("picture").orElse(null);
            if (picture == null) {
                return Optional.empty();
            }
            var id = picture.getAttributeAsString("id").orElse(null);
            if (id == null) {
                return Optional.empty();
            }
            var type = picture.getAttributeAsString("type").orElse(null);
            if (type == null || (!"image".equals(type) && !"preview".equals(type))) {
                return Optional.empty();
            }
            var hasStaging = picture.getAttributeAsString("has_staging").orElse(null);
            if (hasStaging != null && !"false".equals(hasStaging) && !"true".equals(hasStaging)) {
                return Optional.empty();
            }
            var bytes = picture.toContentBytes().orElse(null);
            if (bytes == null || bytes.length == 0) {
                return Optional.empty();
            }
            return Optional.of(new SuccessPictureBlob(id, type, hasStaging, bytes));
        }

        /**
         * Compares this blob reply to another for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link SuccessPictureBlob}
         *         with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (SuccessPictureBlob) obj;
            return Objects.equals(this.pictureId, that.pictureId)
                    && Objects.equals(this.pictureType, that.pictureType)
                    && Objects.equals(this.pictureHasStaging, that.pictureHasStaging)
                    && Arrays.equals(this.pictureElementValue, that.pictureElementValue);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @implNote
         * This implementation mixes {@link Arrays#hashCode(byte[])} of the
         * picture bytes into the hash so byte-array contents drive the result.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            var result = Objects.hash(pictureId, pictureType, pictureHasStaging);
            result = 31 * result + Arrays.hashCode(pictureElementValue);
            return result;
        }

        /**
         * Returns a debug-friendly representation of this reply.
         *
         * <p>The format is intended for logging and is not part of the
         * contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxProfilePictureGetResponse.SuccessPictureBlob[pictureId=" + pictureId
                    + ", pictureType=" + pictureType
                    + ", pictureHasStaging=" + pictureHasStaging
                    + ", pictureElementValue="
                    + Arrays.toString(pictureElementValue) + ']';
        }
    }

    /**
     * Models the no-picture reply, surfaced when the entity has set neither a
     * picture nor an avatar.
     *
     * <p>The relay returns a bare result IQ with no payload children; callers
     * branch on this arm and render the default-avatar fallback.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureGetResponseSuccessNoData")
    final class SuccessNoData implements SmaxProfilePictureGetResponse {
        /**
         * Constructs a no-data reply.
         *
         * <p>The marker variant carries no fields.
         */
        public SuccessNoData() {
        }

        /**
         * Parses a no-data reply from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the stanza carries a
         * {@code <picture>} or {@code <avatar>} child, which would indicate one
         * of the picture-bearing variants instead.
         *
         * @implNote
         * This implementation guards against false positives even when invoked
         * directly, in addition to the dispatcher-priority guarantee in
         * {@link #of(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInProfilePictureGetResponseSuccessNoData",
                exports = "parseGetResponseSuccessNoData",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessNoData> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            if (stanza.getChild("picture").isPresent()) {
                return Optional.empty();
            }
            if (stanza.getChild("avatar").isPresent()) {
                return Optional.empty();
            }
            return Optional.of(new SuccessNoData());
        }

        /**
         * Compares this reply to another for type equality.
         *
         * <p>All {@link SuccessNoData} instances compare equal; the variant
         * carries no state.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link SuccessNoData}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns the class-level constant hash code.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return SuccessNoData.class.hashCode();
        }

        /**
         * Returns a debug-friendly representation of this reply.
         *
         * <p>The format is intended for logging and is not part of the
         * contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxProfilePictureGetResponse.SuccessNoData[]";
        }
    }

    /**
     * Models the error reply, carrying the rejection code-text pair.
     *
     * <p>The pair is one of seven documented values:
     * <ul>
     *   <li>{@code (400, "bad-request")}</li>
     *   <li>{@code (401, "not-authorized")}</li>
     *   <li>{@code (404, "item-not-found")}</li>
     *   <li>{@code (429, "rate-overlimit")}</li>
     *   <li>{@code (500, "internal-server-error")}</li>
     *   <li>{@code (501, "feature-not-implemented")}</li>
     *   <li>{@code (503, "service-unavailable")}</li>
     * </ul>
     */
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureGetResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureProfilePictureGetErrors")
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureIQErrorBadRequestMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureIQErrorNotAuthorizedMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureIQErrorItemNotFoundMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureIQErrorRateOverlimitMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureIQErrorInternalServerErrorMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureIQErrorFeatureNotImplementedMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInProfilePictureIQErrorServiceUnavailableMixin")
    final class Error implements SmaxProfilePictureGetResponse {
        /**
         * The numeric error code.
         */
        private final int errorCode;

        /**
         * The optional error text.
         */
        private final String errorText;

        /**
         * Constructs an error reply from the parsed code-text pair.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public Error(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional error text.
         *
         * @return an {@link Optional} carrying the text, or
         *         {@link Optional#empty()} when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses an error reply from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when neither the client-error nor
         * the server-error envelope matched, or when the code-text pair is not
         * one of the seven documented variants.
         *
         * @implNote
         * This implementation tries the 4xx client-error envelope via
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} first,
         * then falls through to the 5xx server-error envelope via
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}; the
         * resulting code-text pair is cross-checked against the documented list,
         * and a pair outside that list collapses to {@link Optional#empty()}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInProfilePictureGetResponseError",
                exports = "parseGetResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Error> of(Stanza stanza, Stanza request) {
            var clientEnvelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            var serverEnvelope = clientEnvelope == null
                    ? SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null)
                    : null;
            var envelope = clientEnvelope != null ? clientEnvelope : serverEnvelope;
            if (envelope == null) {
                return Optional.empty();
            }
            var code = envelope.code();
            var text = envelope.text();
            if ((code == 400 && "bad-request".equals(text))
                    || (code == 401 && "not-authorized".equals(text))
                    || (code == 404 && "item-not-found".equals(text))
                    || (code == 429 && "rate-overlimit".equals(text))
                    || (code == 500 && "internal-server-error".equals(text))
                    || (code == 501 && "feature-not-implemented".equals(text))
                    || (code == 503 && "service-unavailable".equals(text))) {
                return Optional.of(new Error(code, text));
            }
            return Optional.empty();
        }

        /**
         * Compares this error reply to another for value equality on the
         * code-text pair.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an {@link Error} with
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
            var that = (Error) obj;
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug-friendly representation of this reply.
         *
         * <p>The format is intended for logging and is not part of the
         * contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxProfilePictureGetResponse.Error[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

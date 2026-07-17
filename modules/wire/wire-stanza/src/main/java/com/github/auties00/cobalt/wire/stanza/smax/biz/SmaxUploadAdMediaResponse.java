package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxDeprecatedIqErrorResponseOptionalFromMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqErrorResponseMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The sealed family of inbound reply variants produced by the relay in response to a
 * {@link SmaxUploadAdMediaRequest}.
 * Backs the CTWA native-ad media linking flow that registers an already-uploaded WA media handle against the
 * connected Facebook ad account. The three variants split the wire outcome into {@link Success} (relay echoed
 * the linked {@code (id, type)} entries via a {@code <media>} and/or {@code <media_list>} tree),
 * {@link ClientError} (relay rejected the link with one of two documented {@code 4xx} arms: {@code (bad-request,
 * 400)} or {@code (forbidden, 403)}) and {@link ServerError} (one of two documented {@code 5xx} arms:
 * {@code (internal-server-error, 500)} or {@code (service-unavailable, 503)}).
 *
 * @implSpec
 * Permitted variants are exactly {@link Success}, {@link ClientError}, and {@link ServerError}; new wire
 * outcomes must be added here and to {@link #of(Stanza, Stanza)} in priority order.
 *
 * @implNote
 * This implementation tries each variant in priority order via {@link #of(Stanza, Stanza)} and returns the first
 * successful parse.
 */
public sealed interface SmaxUploadAdMediaResponse extends SmaxStanza.Response
        permits SmaxUploadAdMediaResponse.Success, SmaxUploadAdMediaResponse.ClientError, SmaxUploadAdMediaResponse.ServerError {

    /**
     * Tries each {@link SmaxUploadAdMediaResponse} variant in priority order and returns the first that parses
     * cleanly.
     * Invoked by the smax reply pump after dispatching a {@link SmaxUploadAdMediaRequest}. The priority order
     * ({@link Success} then {@link ClientError} then {@link ServerError}) ensures that a malformed
     * {@code Success} stanza falls through to {@link ClientError} rather than masking an error. An unrecognised
     * stanza shape yields {@link Optional#empty()}.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza, used to validate echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented
     *         variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBizCtwaNativeAdUploadAdMediaRPC",
            exports = "sendUploadAdMediaRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxUploadAdMediaResponse> of(Stanza stanza, Stanza request) {
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
     * The {@code Success} reply variant carrying the relay's echoed media registrations.
     * Projected by {@link SmaxUploadAdMediaResponse#of(Stanza, Stanza)} when the relay returns the documented
     * {@code <iq>} envelope with an optional {@code <media>} child and a {@code 0..10} sequence of
     * {@code <media_list>} siblings.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdUploadAdMediaResponseSuccess")
    final class Success implements SmaxUploadAdMediaResponse {
        /**
         * The optional primary {@code <media/>} echo carrying the registered {@code (id, type)} pair, or
         * {@code null} when the relay omitted the child.
         */
        private final SmaxUploadAdMediaMediaEntry media;

        /**
         * The list of {@code <media_list/>} sibling echoes, each carrying a registered {@code (id, type)} pair;
         * admits {@code 0..10} entries.
         */
        private final List<SmaxUploadAdMediaMediaEntry> mediaList;

        /**
         * Constructs a new successful reply.
         *
         * @param media     the optional primary media echo; may be {@code null}
         * @param mediaList the media-list echoes; never {@code null}
         * @throws NullPointerException if {@code mediaList} is {@code null}
         */
        public Success(SmaxUploadAdMediaMediaEntry media, List<SmaxUploadAdMediaMediaEntry> mediaList) {
            Objects.requireNonNull(mediaList, "mediaList cannot be null");
            this.media = media;
            this.mediaList = List.copyOf(mediaList);
        }

        /**
         * Returns the optional primary {@code <media/>} echo.
         *
         * @return an {@link Optional} carrying the entry, or empty when the relay omitted the {@code <media/>}
         *         child
         */
        public Optional<SmaxUploadAdMediaMediaEntry> media() {
            return Optional.ofNullable(media);
        }

        /**
         * Returns the {@code <media_list/>} sibling echoes.
         *
         * @return an unmodifiable list of {@code 0..10} entries; never {@code null}
         */
        public List<SmaxUploadAdMediaMediaEntry> mediaList() {
            return mediaList;
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         * Validates the IQ result envelope, then walks the optional {@code <media>} child followed by the
         * {@code <media_list>} sequence, rejecting the parse when an echo stanza is malformed or when more than
         * ten {@code <media_list>} entries are present.
         *
         * @implNote
         * This implementation enforces the {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} envelope
         * check up front, then drives both echo trees through the same {@link #parseEntry(Stanza)} helper since
         * their per-child shapes are identical aside from the tag name. The hard cap of ten {@code <media_list>}
         * entries reflects the WA Web {@code mapChildrenWithTag(..., 0, 10, e)} contract. Cobalt's
         * {@link Stanza#getChild(String)} silently picks the first matching child when more than one
         * {@code <media>} is present, whereas WA Web's {@code optionalChildWithTag} rejects; the relay never
         * emits multiple {@code <media>} children so this divergence is unreachable in practice.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the
         *         success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdUploadAdMediaResponseSuccess",
                exports = "parseUploadAdMediaResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            SmaxUploadAdMediaMediaEntry media = null;
            var mediaNode = stanza.getChild("media").orElse(null);
            if (mediaNode != null) {
                var parsed = parseEntry(mediaNode);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                media = parsed.get();
            }
            var entries = new ArrayList<SmaxUploadAdMediaMediaEntry>();
            var iter = stanza.streamChildren("media_list").iterator();
            while (iter.hasNext()) {
                var listNode = iter.next();
                var parsed = parseEntry(listNode);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                entries.add(parsed.get());
            }
            if (entries.size() > 10) {
                return Optional.empty();
            }
            return Optional.of(new Success(media, entries));
        }

        /**
         * Parses a single {@code (id, type)} entry from a {@code <media/>} or {@code <media_list/>} echo stanza.
         * Drives both echo trees through a single parser, returning {@link Optional#empty()} when either
         * attribute is missing or malformed.
         *
         * @implNote
         * This implementation consolidates WA Web's two parsers that differ only in the asserted tag name; the
         * tag check is already enforced by the call sites ({@link Stanza#getChild(String)} for {@code <media>}
         * and {@link Stanza#streamChildren(String)} for {@code <media_list>}), so a redundant tag assertion here
         * would be a no-op. The {@code id} attribute is required as a non-empty string; the {@code type}
         * attribute is required to match the lowercase {@code {"image", "video"}} dictionary via
         * {@link SmaxUploadAdMediaMediaType#of(String)}.
         *
         * @param stanza the {@code <media/>} or {@code <media_list/>} child stanza
         * @return an {@link Optional} carrying the parsed entry, or empty when either attribute is missing or
         *         malformed
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdUploadAdMediaResponseSuccess",
                exports = "parseUploadAdMediaResponseSuccessMedia",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdUploadAdMediaResponseSuccess",
                exports = "parseUploadAdMediaResponseSuccessMediaList",
                adaptation = WhatsAppAdaptation.ADAPTED)
        private static Optional<SmaxUploadAdMediaMediaEntry> parseEntry(Stanza stanza) {
            var id = stanza.getAttributeAsString("id").orElse(null);
            if (id == null) {
                return Optional.empty();
            }
            var typeStr = stanza.getAttributeAsString("type").orElse(null);
            if (typeStr == null) {
                return Optional.empty();
            }
            var type = SmaxUploadAdMediaMediaType.of(typeStr).orElse(null);
            if (type == null) {
                return Optional.empty();
            }
            return Optional.of(new SmaxUploadAdMediaMediaEntry(id, type));
        }

        /**
         * Compares this reply with another object for equality.
         * Two replies are equal when both the primary media echo and the media list match.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Success} with the same {@code media} and
         *         {@code mediaList}
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
            return Objects.equals(this.media, that.media)
                    && Objects.equals(this.mediaList, that.mediaList);
        }

        /**
         * Returns a hash code derived from the primary media echo and the media list.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(media, mediaList);
        }

        /**
         * Returns a debug representation listing the primary media echo and the media list.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxUploadAdMediaResponse.Success[media=" + media
                    + ", mediaList=" + mediaList + ']';
        }
    }

    /**
     * The {@code ClientError} reply variant carrying one of two documented {@code 4xx} native-ad rejection
     * pairs.
     * Surfaced when the relay rejected the link via either {@code (text="bad-request", code=400)} or
     * {@code (text="forbidden", code=403)}; any other {@code (code, text)} pair falls through the disjunction
     * and is rejected by {@link #of(Stanza, Stanza)}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdUploadAdMediaResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdNativeAdErrors")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdIQErrorBadRequestMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdIQErrorForbiddenMixin")
    final class ClientError implements SmaxUploadAdMediaResponse {
        /**
         * The numeric server-side error code; one of {@code 400} or {@code 403}.
         */
        private final int errorCode;

        /**
         * The human-readable error text; one of {@code "bad-request"} or {@code "forbidden"} when the pair
         * matches a documented arm.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply.
         *
         * @param errorCode the numeric error code
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
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         * Validates the error envelope and admits only the two documented client-error arms; non-matching pairs
         * fall through the disjunction so the smax dispatch can drop to {@link ServerError}.
         *
         * @implNote
         * This implementation runs the {@link SmaxDeprecatedIqErrorResponseOptionalFromMixin#validate(Stanza,
         * Stanza)} envelope check first, then flattens the {@code <error/>} child via
         * {@link SmaxIqErrorResponseMixin#parseError(Stanza)}, and finally requires the {@code (code, text)} pair
         * to match one of the two documented client-error arms via {@link #matchClientErrorPair(int, String)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the
         *         client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdUploadAdMediaResponseError",
                exports = "parseUploadAdMediaResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdNativeAdErrors",
                exports = "parseNativeAdErrors",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdDeprecatedIQErrorResponseOptionalFromMixin",
                exports = "parseDeprecatedIQErrorResponseOptionalFromMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdIQErrorBadRequestMixin",
                exports = "parseIQErrorBadRequestMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdIQErrorForbiddenMixin",
                exports = "parseIQErrorForbiddenMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            if (!SmaxDeprecatedIqErrorResponseOptionalFromMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var envelope = SmaxIqErrorResponseMixin.parseError(stanza).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return matchClientErrorPair(envelope.code(), envelope.text())
                    ? Optional.of(new ClientError(envelope.code(), envelope.text()))
                    : Optional.empty();
        }

        /**
         * Reports whether the supplied {@code (code, text)} pair matches one of the two documented
         * {@code ClientError} arms.
         * The two admitted pairs are {@code ("bad-request", 400)} and {@code ("forbidden", 403)}.
         *
         * @implNote
         * This implementation hard-codes the literal pairs that the WA Web mixins enforce so a malformed pair
         * fails the parse the same way.
         *
         * @param code the parsed error code
         * @param text the parsed error text; may be {@code null}
         * @return {@code true} when the pair matches one of the enumerated client-error arms; {@code false}
         *         otherwise
         */
        private static boolean matchClientErrorPair(int code, String text) {
            return ("bad-request".equals(text) && code == 400)
                    || ("forbidden".equals(text) && code == 403);
        }

        /**
         * Compares this reply with another object for equality.
         * Two replies are equal when both the {@code errorCode} and the {@code errorText} match.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ClientError} with the same code and text
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
         * Returns a hash code derived from the {@code errorCode} and the {@code errorText}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation listing the {@code errorCode} and the {@code errorText}.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxUploadAdMediaResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The {@code ServerError} reply variant carrying one of two documented {@code 5xx} native-ad transient
     * failure pairs.
     * Surfaced when the relay returned either {@code (text="internal-server-error", code=500)} or
     * {@code (text="service-unavailable", code=503)}; the caller can re-issue the request with backoff.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdUploadAdMediaResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdNativeAdErrors")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdIQErrorInternalServerErrorMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdIQErrorServiceUnavailableMixin")
    final class ServerError implements SmaxUploadAdMediaResponse {
        /**
         * The numeric server-side error code; one of {@code 500} or {@code 503}.
         */
        private final int errorCode;

        /**
         * The human-readable error text; one of {@code "internal-server-error"} or {@code "service-unavailable"}
         * when the pair matches a documented arm.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply.
         *
         * @param errorCode the numeric error code
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
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         * Validates the error envelope and admits only the two documented server-error arms; non-matching pairs
         * fall through the disjunction and yield {@link Optional#empty()}.
         *
         * @implNote
         * This implementation runs the {@link SmaxDeprecatedIqErrorResponseOptionalFromMixin#validate(Stanza,
         * Stanza)} envelope check first, then flattens the {@code <error/>} child via
         * {@link SmaxIqErrorResponseMixin#parseError(Stanza)}, and finally requires the {@code (code, text)} pair
         * to match one of the two documented server-error arms via {@link #matchServerErrorPair(int, String)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the
         *         server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdUploadAdMediaResponseError",
                exports = "parseUploadAdMediaResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdNativeAdErrors",
                exports = "parseNativeAdErrors",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdDeprecatedIQErrorResponseOptionalFromMixin",
                exports = "parseDeprecatedIQErrorResponseOptionalFromMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdIQErrorInternalServerErrorMixin",
                exports = "parseIQErrorInternalServerErrorMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaNativeAdIQErrorServiceUnavailableMixin",
                exports = "parseIQErrorServiceUnavailableMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            if (!SmaxDeprecatedIqErrorResponseOptionalFromMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var envelope = SmaxIqErrorResponseMixin.parseError(stanza).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return matchServerErrorPair(envelope.code(), envelope.text())
                    ? Optional.of(new ServerError(envelope.code(), envelope.text()))
                    : Optional.empty();
        }

        /**
         * Reports whether the supplied {@code (code, text)} pair matches one of the two documented
         * {@code ServerError} arms.
         * The two admitted pairs are {@code ("internal-server-error", 500)} and
         * {@code ("service-unavailable", 503)}.
         *
         * @implNote
         * This implementation hard-codes the literal pairs that the WA Web mixins enforce; any other {@code 5xx}
         * pair falls through the disjunction the same way.
         *
         * @param code the parsed error code
         * @param text the parsed error text; may be {@code null}
         * @return {@code true} when the pair matches one of the enumerated server-error arms; {@code false}
         *         otherwise
         */
        private static boolean matchServerErrorPair(int code, String text) {
            return ("internal-server-error".equals(text) && code == 500)
                    || ("service-unavailable".equals(text) && code == 503);
        }

        /**
         * Compares this reply with another object for equality.
         * Two replies are equal when both the {@code errorCode} and the {@code errorText} match.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ServerError} with the same code and text
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
         * Returns a hash code derived from the {@code errorCode} and the {@code errorText}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation listing the {@code errorCode} and the {@code errorText}.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxUploadAdMediaResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

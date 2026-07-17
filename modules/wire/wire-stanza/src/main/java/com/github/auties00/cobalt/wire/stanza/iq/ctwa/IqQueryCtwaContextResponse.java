package com.github.auties00.cobalt.wire.stanza.iq.ctwa;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ctwa.BusinessCtwaMediaType;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the inbound reply variants the relay produces in response to an
 * {@link IqQueryCtwaContextRequest}.
 *
 * <p>This sealed family splits the reply into a {@link Success} carrying the resolved CTWA
 * context, a {@link ClientError} for permanent rejections, and a {@link ServerError} for
 * transient failures, so the chat composer can decide whether to render the ad context, hide
 * the CTWA banner entirely (permanent), or fall back to a degraded preview and retry
 * (transient).
 *
 * @implNote
 * WA Web's {@code ctwaContext} parser throws a generic {@code "invalid response"} error on any
 * failure path; this implementation instead routes failures to {@link ClientError} and
 * {@link ServerError} so callers can distinguish permanent from transient outcomes.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryCtwaContextJob")
public sealed interface IqQueryCtwaContextResponse extends IqStanza.Response
        permits IqQueryCtwaContextResponse.Success, IqQueryCtwaContextResponse.ClientError, IqQueryCtwaContextResponse.ServerError {

    /**
     * Tries each {@link IqQueryCtwaContextResponse} variant in priority order and returns the
     * first that parses cleanly.
     *
     * <p>The priority order is {@link Success}, then {@link ClientError}, then
     * {@link ServerError}: a {@code type="result"} envelope carrying a {@code <context>} child
     * wins, otherwise the {@code <error code/>} branch is split by code range.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound stanza
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when
     *         no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryCtwaContextJob",
            exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqQueryCtwaContextResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the projected {@code <context>} grandchild for a successful CTWA-context lookup.
     *
     * <p>The mandatory fields ({@code sourceUrl}, {@code sourceId}, {@code sourceType}) always
     * populate. The optional fields populate when the ad creative included them, and the WAMO
     * automated-greeting-message fields ({@code greetingMessageBody},
     * {@code automatedGreetingMessageShown}, {@code ctaPayload}, {@code originalImageUrl})
     * populate only when the relay reports the WAMO-AGM integration as enabled for the source
     * app.
     */
    @WhatsAppWebModule(moduleName = "WAWebQueryCtwaContextJob")
    final class Success implements IqQueryCtwaContextResponse {
        /**
         * Holds the mandatory {@code <source>}/{@code <url>} content carrying the ad's landing
         * URL.
         */
        private final String sourceUrl;

        /**
         * Holds the mandatory {@code <source>}/{@code <id>} content carrying the ad creative
         * id.
         */
        private final String sourceId;

        /**
         * Holds the mandatory {@code <source>}/{@code <type>} content tagging the ad source
         * surface.
         */
        private final String sourceType;

        /**
         * Holds the optional {@code <headline>} content rendered as the chat preview title.
         */
        private final String title;

        /**
         * Holds the optional {@code <body>} content rendered as the chat preview description.
         */
        private final String description;

        /**
         * Holds the optional {@code <thumbnail>}/{@code <url>} content for an
         * externally-hosted thumbnail.
         */
        private final String thumbnailUrl;

        /**
         * Holds the optional inline {@code <thumbnail>}/{@code <bytes>} content.
         *
         * @implNote
         * WA Web base64-encodes these bytes for the JS bridge; this implementation surfaces
         * the raw decoded bytes instead.
         */
        private final byte[] thumbnailBytes;

        /**
         * Holds the optional {@code <video>}/{@code <url>} content when the ad creative is a
         * video.
         */
        private final String mediaUrl;

        /**
         * Holds the media classifier derived from the response shape.
         *
         * <p>This is {@link BusinessCtwaMediaType#VIDEO} when the response carried a
         * {@code <video>} grandchild, {@link BusinessCtwaMediaType#IMAGE} when it carried only
         * a thumbnail, and {@code null} when the response had no thumbnail at all.
         */
        private final BusinessCtwaMediaType mediaType;

        /**
         * Holds the optional {@code <sourceApp>} content tagging the host platform of the ad.
         *
         * <p>The value is typically {@code "instagram"} or {@code "facebook"} and also gates
         * the WAMO-AGM fields server-side.
         */
        private final String sourceApp;

        /**
         * Holds the optional WAMO-AGM {@code <greetingMessageBody>} content carrying the
         * prefilled greeting the relay wants the composer to render.
         */
        private final String greetingMessageBody;

        /**
         * Holds the optional WAMO-AGM {@code <automatedGreetingMessageShown>} flag.
         *
         * <p>The relay encodes it as the literal string {@code "true"} or {@code "false"},
         * which {@link #of(Stanza, Stanza)} parses into a tri-state {@link Boolean}
         * ({@code null} when the field is absent).
         */
        private final Boolean automatedGreetingMessageShown;

        /**
         * Holds the optional WAMO-AGM {@code <ctaPayload>} content carrying the
         * call-to-action payload the composer should attach to the first outbound message.
         */
        private final String ctaPayload;

        /**
         * Holds the optional WAMO-AGM {@code <originalImageUrl>} content carrying the
         * un-cropped source image URL.
         */
        private final String originalImageUrl;

        /**
         * Constructs a successful reply from the parsed {@code <context>} fields.
         *
         * @implNote
         * This implementation defensively clones {@code thumbnailBytes}; the remaining fields
         * are immutable strings, enums, or boxed primitives and are stored directly.
         *
         * @param sourceUrl                     the source URL
         * @param sourceId                      the source id
         * @param sourceType                    the source type
         * @param title                         the optional title, or {@code null}
         * @param description                   the optional description, or {@code null}
         * @param thumbnailUrl                  the optional thumbnail URL, or {@code null}
         * @param thumbnailBytes                the optional inline thumbnail bytes, or
         *                                      {@code null}
         * @param mediaUrl                      the optional video URL, or {@code null}
         * @param mediaType                     the media classifier, or {@code null} when no
         *                                      thumbnail was shipped
         * @param sourceApp                     the optional source app, or {@code null}
         * @param greetingMessageBody           the optional WAMO-AGM greeting, or {@code null}
         * @param automatedGreetingMessageShown the optional WAMO-AGM shown flag, or
         *                                      {@code null}
         * @param ctaPayload                    the optional WAMO-AGM CTA payload, or
         *                                      {@code null}
         * @param originalImageUrl              the optional WAMO-AGM original image URL, or
         *                                      {@code null}
         * @throws NullPointerException if {@code sourceUrl}, {@code sourceId}, or
         *                              {@code sourceType} is {@code null}
         */
        public Success(String sourceUrl, String sourceId, String sourceType, String title,
                       String description, String thumbnailUrl, byte[] thumbnailBytes,
                       String mediaUrl, BusinessCtwaMediaType mediaType, String sourceApp,
                       String greetingMessageBody, Boolean automatedGreetingMessageShown,
                       String ctaPayload, String originalImageUrl) {
            this.sourceUrl = Objects.requireNonNull(sourceUrl, "sourceUrl cannot be null");
            this.sourceId = Objects.requireNonNull(sourceId, "sourceId cannot be null");
            this.sourceType = Objects.requireNonNull(sourceType, "sourceType cannot be null");
            this.title = title;
            this.description = description;
            this.thumbnailUrl = thumbnailUrl;
            this.thumbnailBytes = thumbnailBytes == null ? null : thumbnailBytes.clone();
            this.mediaUrl = mediaUrl;
            this.mediaType = mediaType;
            this.sourceApp = sourceApp;
            this.greetingMessageBody = greetingMessageBody;
            this.automatedGreetingMessageShown = automatedGreetingMessageShown;
            this.ctaPayload = ctaPayload;
            this.originalImageUrl = originalImageUrl;
        }

        /**
         * Returns the source URL.
         *
         * @return the URL, never {@code null}
         */
        public String sourceUrl() {
            return sourceUrl;
        }

        /**
         * Returns the source identifier.
         *
         * @return the id, never {@code null}
         */
        public String sourceId() {
            return sourceId;
        }

        /**
         * Returns the source type.
         *
         * @return the type, never {@code null}
         */
        public String sourceType() {
            return sourceType;
        }

        /**
         * Returns the optional title.
         *
         * @return an {@link Optional} carrying the title, or {@link Optional#empty()}
         */
        public Optional<String> title() {
            return Optional.ofNullable(title);
        }

        /**
         * Returns the optional description.
         *
         * @return an {@link Optional} carrying the description, or {@link Optional#empty()}
         */
        public Optional<String> description() {
            return Optional.ofNullable(description);
        }

        /**
         * Returns the optional thumbnail URL.
         *
         * @return an {@link Optional} carrying the URL, or {@link Optional#empty()}
         */
        public Optional<String> thumbnailUrl() {
            return Optional.ofNullable(thumbnailUrl);
        }

        /**
         * Returns the optional inline thumbnail bytes.
         *
         * <p>Returns a defensive copy; callers may mutate the array without affecting
         * subsequent reads.
         *
         * @return an {@link Optional} carrying a clone of the bytes, or {@link Optional#empty()}
         */
        public Optional<byte[]> thumbnailBytes() {
            return Optional.ofNullable(thumbnailBytes).map(byte[]::clone);
        }

        /**
         * Returns the optional video URL.
         *
         * @return an {@link Optional} carrying the URL, or {@link Optional#empty()}
         */
        public Optional<String> mediaUrl() {
            return Optional.ofNullable(mediaUrl);
        }

        /**
         * Returns the optional media classifier.
         *
         * @return an {@link Optional} carrying the type, or {@link Optional#empty()} when no
         *         thumbnail was shipped
         */
        public Optional<BusinessCtwaMediaType> mediaType() {
            return Optional.ofNullable(mediaType);
        }

        /**
         * Returns the optional source-app identifier.
         *
         * @return an {@link Optional} carrying the identifier, or {@link Optional#empty()}
         */
        public Optional<String> sourceApp() {
            return Optional.ofNullable(sourceApp);
        }

        /**
         * Returns the optional WAMO-AGM greeting body.
         *
         * @return an {@link Optional} carrying the greeting, or {@link Optional#empty()}
         */
        public Optional<String> greetingMessageBody() {
            return Optional.ofNullable(greetingMessageBody);
        }

        /**
         * Returns the optional WAMO-AGM shown flag.
         *
         * @return an {@link Optional} carrying the flag, or {@link Optional#empty()}
         */
        public Optional<Boolean> automatedGreetingMessageShown() {
            return Optional.ofNullable(automatedGreetingMessageShown);
        }

        /**
         * Returns the optional WAMO-AGM CTA payload.
         *
         * @return an {@link Optional} carrying the payload, or {@link Optional#empty()}
         */
        public Optional<String> ctaPayload() {
            return Optional.ofNullable(ctaPayload);
        }

        /**
         * Returns the optional WAMO-AGM original image URL.
         *
         * @return an {@link Optional} carrying the URL, or {@link Optional#empty()}
         */
        public Optional<String> originalImageUrl() {
            return Optional.ofNullable(originalImageUrl);
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         *
         * <p>The parse only succeeds when {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}
         * confirms the envelope echoes the {@code request} id, the envelope carries a
         * {@code <context>} child with a {@code <source>} grandchild, and that {@code <source>}
         * grandchild populates {@code <url>}, {@code <id>}, and {@code <type>}; anything else
         * returns {@link Optional#empty()} so the caller can fall through to the error variants.
         *
         * @implNote
         * This implementation deviates from WA Web's parser, which throws
         * {@code "invalid response"} as soon as it sees a {@code <context>} child carrying an
         * {@code <error>} grandchild. Cobalt does not throw on that shape; it instead lets
         * {@link ClientError#of(Stanza, Stanza)} pick the envelope up via the standard
         * {@code <error/>} child mechanism.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebQueryCtwaContextJob",
                exports = "ctwaContext",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var context = stanza.getChild("context").orElse(null);
            if (context == null) {
                return Optional.empty();
            }
            var source = context.getChild("source").orElse(null);
            if (source == null) {
                return Optional.empty();
            }
            var sourceUrl = source.getChild("url")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            var sourceId = source.getChild("id")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            var sourceType = source.getChild("type")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            if (sourceUrl == null || sourceId == null || sourceType == null) {
                return Optional.empty();
            }
            var title = context.getChild("headline")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            var description = context.getChild("body")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            String thumbnailUrl = null;
            byte[] thumbnailBytes = null;
            BusinessCtwaMediaType mediaType = null;
            String mediaUrl = null;
            var thumbnail = context.getChild("thumbnail").orElse(null);
            if (thumbnail != null) {
                thumbnailUrl = thumbnail.getChild("url")
                        .flatMap(Stanza::toContentString)
                        .orElse(null);
                thumbnailBytes = thumbnail.getChild("bytes")
                        .flatMap(Stanza::toContentBytes)
                        .orElse(null);
                var video = context.getChild("video").orElse(null);
                if (video != null) {
                    mediaUrl = video.getChild("url")
                            .flatMap(Stanza::toContentString)
                            .orElse(null);
                    mediaType = BusinessCtwaMediaType.VIDEO;
                } else {
                    mediaType = BusinessCtwaMediaType.IMAGE;
                }
            }
            var sourceApp = context.getChild("sourceApp")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            var greetingMessageBody = context.getChild("greetingMessageBody")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            Boolean automatedGreetingMessageShown = null;
            var automatedShown = context.getChild("automatedGreetingMessageShown")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            if (automatedShown != null) {
                automatedGreetingMessageShown = "true".equals(automatedShown);
            }
            var ctaPayload = context.getChild("ctaPayload")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            var originalImageUrl = context.getChild("originalImageUrl")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            return Optional.of(new Success(sourceUrl, sourceId, sourceType, title, description,
                    thumbnailUrl, thumbnailBytes, mediaUrl, mediaType, sourceApp,
                    greetingMessageBody, automatedGreetingMessageShown, ctaPayload,
                    originalImageUrl));
        }

        /**
         * Compares this reply to another object for value equality across every field.
         *
         * @implNote
         * This implementation compares {@code thumbnailBytes} with
         * {@link Arrays#equals(byte[], byte[])} rather than {@link Objects#equals(Object, Object)}
         * so the array is compared by content.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link Success} whose every field is equal
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
            return Objects.equals(this.sourceUrl, that.sourceUrl)
                    && Objects.equals(this.sourceId, that.sourceId)
                    && Objects.equals(this.sourceType, that.sourceType)
                    && Objects.equals(this.title, that.title)
                    && Objects.equals(this.description, that.description)
                    && Objects.equals(this.thumbnailUrl, that.thumbnailUrl)
                    && Arrays.equals(this.thumbnailBytes, that.thumbnailBytes)
                    && Objects.equals(this.mediaUrl, that.mediaUrl)
                    && this.mediaType == that.mediaType
                    && Objects.equals(this.sourceApp, that.sourceApp)
                    && Objects.equals(this.greetingMessageBody, that.greetingMessageBody)
                    && Objects.equals(this.automatedGreetingMessageShown, that.automatedGreetingMessageShown)
                    && Objects.equals(this.ctaPayload, that.ctaPayload)
                    && Objects.equals(this.originalImageUrl, that.originalImageUrl);
        }

        /**
         * Returns a hash code derived from every field.
         *
         * @implNote
         * This implementation folds {@code thumbnailBytes} in via
         * {@link Arrays#hashCode(byte[])} so the contribution is content-based, consistent with
         * {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(sourceUrl, sourceId, sourceType, title, description,
                    thumbnailUrl, Arrays.hashCode(thumbnailBytes), mediaUrl, mediaType,
                    sourceApp, greetingMessageBody, automatedGreetingMessageShown,
                    ctaPayload, originalImageUrl);
        }

        /**
         * Returns a debug string listing every field.
         *
         * @implNote
         * This implementation renders {@code thumbnailBytes} as a length ({@code -1} when
         * absent) rather than dumping the raw array.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            var thumbnailBytesLength = thumbnailBytes == null ? -1 : thumbnailBytes.length;
            return "IqQueryCtwaContextResponse.Success[sourceUrl=" + sourceUrl
                    + ", sourceId=" + sourceId
                    + ", sourceType=" + sourceType
                    + ", title=" + title
                    + ", description=" + description
                    + ", thumbnailUrl=" + thumbnailUrl
                    + ", thumbnailBytesLength=" + thumbnailBytesLength
                    + ", mediaUrl=" + mediaUrl
                    + ", mediaType=" + mediaType
                    + ", sourceApp=" + sourceApp
                    + ", greetingMessageBody=" + greetingMessageBody
                    + ", automatedGreetingMessageShown=" + automatedGreetingMessageShown
                    + ", ctaPayload=" + ctaPayload
                    + ", originalImageUrl=" + originalImageUrl + ']';
        }
    }

    /**
     * Signals that the relay permanently rejected the CTWA-context lookup.
     *
     * <p>This variant covers both the {@code 4xx} branch of WA Web's reply pipeline and the
     * inline {@code <error/>} child that WA Web's {@code ctwaContext} parser flags as
     * {@code "invalid response"}; in either case the ad context cannot be rendered and the
     * composer should hide the CTWA banner.
     */
    @WhatsAppWebModule(moduleName = "WAWebQueryCtwaContextJob")
    final class ClientError implements IqQueryCtwaContextResponse {
        /**
         * Holds the numeric server-side error code from the {@code <error code/>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the {@code <error text/>}
         * attribute.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the parsed error code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional text, or {@code null} when omitted
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
         * Returns the optional error text.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when
         *         omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>Returns a populated {@link Optional} only when the stanza is a {@code type="error"}
         * envelope echoing the {@code request} id and carrying an {@code <error/>} child whose
         * {@code code} attribute falls in the {@code 4xx} range, per the parsing contract of
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebQueryCtwaContextJob",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for value equality across both fields.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link ClientError} with equal
         *         {@code errorCode} and {@code errorText}
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
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string listing both fields.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqQueryCtwaContextResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Signals a transient server-side failure on a CTWA-context lookup.
     *
     * <p>This variant maps to the {@code 5xx} branch of WA Web's reply pipeline; callers may
     * retry the same lookup after a backoff or fall back to a degraded composer state.
     */
    @WhatsAppWebModule(moduleName = "WAWebQueryCtwaContextJob")
    final class ServerError implements IqQueryCtwaContextResponse {
        /**
         * Holds the numeric server-side error code from the {@code <error code/>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the {@code <error text/>}
         * attribute.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the parsed error code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional text, or {@code null} when omitted
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
         * Returns the optional error text.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when
         *         omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>Returns a populated {@link Optional} only when the stanza is a {@code type="error"}
         * envelope echoing the {@code request} id and carrying an {@code <error/>} child whose
         * {@code code} attribute falls outside the {@code 4xx} range, per the parsing contract
         * of {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebQueryCtwaContextJob",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for value equality across both fields.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link ServerError} with equal
         *         {@code errorCode} and {@code errorText}
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
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string listing both fields.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqQueryCtwaContextResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

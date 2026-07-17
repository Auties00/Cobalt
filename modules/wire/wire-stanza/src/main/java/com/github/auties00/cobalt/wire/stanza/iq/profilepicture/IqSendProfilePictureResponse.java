package com.github.auties00.cobalt.wire.stanza.iq.profilepicture;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Objects;
import java.util.Optional;

/**
 * Roots the sealed family of inbound reply variants produced by the relay in response to an
 * {@link IqSendProfilePictureRequest}.
 *
 * <p>The three permitted variants partition every documented reply: {@link Success} carries the
 * relay-assigned picture id on an accepted set or clear, {@link ClientError} reports a caller-side
 * rejection, and {@link ServerError} reports a transient relay-side failure. Splitting failures
 * into two variants lets the caller distinguish a permanent rejection (for example, an optimistic
 * picture update that must be reverted) from a transient one that may be retried.
 */
public sealed interface IqSendProfilePictureResponse extends IqStanza.Response
        permits IqSendProfilePictureResponse.Success, IqSendProfilePictureResponse.ClientError, IqSendProfilePictureResponse.ServerError {

    /**
     * Tries each {@link IqSendProfilePictureResponse} variant in priority order and returns the
     * first that parses cleanly.
     *
     * <p>Variants are attempted in the order {@link Success}, {@link ClientError},
     * {@link ServerError}; the first non-empty parse wins, and an empty result means the stanza
     * matched no documented variant.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound stanza
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when
     *         no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendProfilePictureJob",
            exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqSendProfilePictureResponse> of(Stanza stanza, Stanza request) {
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
     * Models the reply variant signalling that the relay accepted the picture set or clear.
     *
     * <p>Carries the relay-assigned picture id when a picture was uploaded; the id is absent when
     * the request cleared the existing picture, since the reply then carries no {@code <picture/>}
     * grandchild.
     */
    @WhatsAppWebModule(moduleName = "WAWebSendProfilePictureJob")
    final class Success implements IqSendProfilePictureResponse {
        /**
         * Holds the relay-assigned picture identifier, or {@code null} when the picture was
         * cleared.
         */
        private final Long pictureId;

        /**
         * Constructs a successful reply from a nullable picture identifier.
         *
         * @param pictureId the relay-assigned picture identifier, or {@code null} when the
         *                  picture was cleared
         */
        public Success(Long pictureId) {
            this.pictureId = pictureId;
        }

        /**
         * Returns the relay-assigned picture identifier.
         *
         * @return an {@link Optional} carrying the picture id, or {@link Optional#empty()} when
         *         the picture was cleared
         */
        public Optional<Long> pictureId() {
            return Optional.ofNullable(pictureId);
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         *
         * <p>Yields a populated {@link Optional} only when the envelope is a {@code type="result"}
         * echoing the {@code request} id, as enforced by
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}. The {@code <picture id/>}
         * grandchild is optional, and its absence signals a clear-picture confirmation.
         *
         * @implNote
         * This implementation folds three input shapes to a {@code null} picture id: a missing
         * {@code <picture/>} child, a present {@code <picture/>} child with no {@code id}
         * attribute, and an explicit clear. Only a {@code <picture/>} child carrying a numeric
         * {@code id} yields a non-{@code null} id; a malformed or absent {@code id} never fails
         * the parse.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSendProfilePictureJob",
                exports = "photoResponseParser",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var pictureChild = stanza.getChild("picture").orElse(null);
            if (pictureChild == null) {
                return Optional.of(new Success(null));
            }
            var idAttr = pictureChild.getAttributeAsLong("id");
            if (idAttr.isEmpty()) {
                return Optional.of(new Success(null));
            }
            return Optional.of(new Success(idAttr.getAsLong()));
        }

        /**
         * Compares this reply with another object for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is a {@link Success} carrying an equal picture id,
         *         otherwise {@code false}
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
            return Objects.equals(this.pictureId, that.pictureId);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash of the picture id
         */
        @Override
        public int hashCode() {
            return Objects.hash(pictureId);
        }

        /**
         * Returns a debug representation of this reply.
         *
         * @return a string carrying the picture id
         */
        @Override
        public String toString() {
            return "IqSendProfilePictureResponse.Success[pictureId=" + pictureId + ']';
        }
    }

    /**
     * Models the reply variant signalling that the relay rejected the picture set or clear.
     *
     * <p>Corresponds to the caller-side ({@code 4xx}) error branch; typical codes are {@code 403}
     * when the caller is not an admin of the target group, and {@code 406} when the JPEG bytes
     * fail relay-side format validation.
     */
    @WhatsAppWebModule(moduleName = "WAWebSendProfilePictureJob")
    final class ClientError implements IqSendProfilePictureResponse {
        /**
         * Holds the numeric server-side error code from the {@code <error code/>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the {@code <error text/>} attribute,
         * or {@code null} when omitted.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from an error code and optional text.
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
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>Yields a populated {@link Optional} only when the stanza is a {@code type="error"}
         * envelope echoing the {@code request} id and carrying an {@code <error/>} child whose
         * {@code code} attribute falls in the caller-side ({@code 4xx}) range, per the parsing
         * contract of {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSendProfilePictureJob",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with another object for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is a {@link ClientError} carrying an equal code and
         *         text, otherwise {@code false}
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the combined hash of the error code and text
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation of this reply.
         *
         * @return a string carrying the error code and text
         */
        @Override
        public String toString() {
            return "IqSendProfilePictureResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the reply variant signalling a transient server-side failure on a picture set or
     * clear.
     *
     * <p>Corresponds to the relay-side ({@code 5xx}) error branch; the same request may be
     * retried after a short backoff once the socket has settled.
     */
    @WhatsAppWebModule(moduleName = "WAWebSendProfilePictureJob")
    final class ServerError implements IqSendProfilePictureResponse {
        /**
         * Holds the numeric server-side error code from the {@code <error code/>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the {@code <error text/>} attribute,
         * or {@code null} when omitted.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from an error code and optional text.
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
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>Yields a populated {@link Optional} only when the stanza is a {@code type="error"}
         * envelope echoing the {@code request} id and carrying an {@code <error/>} child whose
         * {@code code} attribute falls outside the caller-side ({@code 4xx}) range, per the
         * parsing contract of {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSendProfilePictureJob",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with another object for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is a {@link ServerError} carrying an equal code and
         *         text, otherwise {@code false}
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the combined hash of the error code and text
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation of this reply.
         *
         * @return a string carrying the error code and text
         */
        @Override
        public String toString() {
            return "IqSendProfilePictureResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

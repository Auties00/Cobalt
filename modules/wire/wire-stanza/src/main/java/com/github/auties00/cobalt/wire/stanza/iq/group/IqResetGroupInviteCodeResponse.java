package com.github.auties00.cobalt.wire.stanza.iq.group;

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
 * Models the sealed family of inbound reply variants the relay produces in response to an {@link IqResetGroupInviteCodeRequest}.
 *
 * <p>After dispatching the rotate request, callers pattern-match the returned variant.
 * {@link Success} carries the freshly-issued invite code, {@link ClientError} surfaces caller-side
 * rejections (typically {@code 403} when the caller is not a group admin or {@code 404} when the
 * group no longer exists), and {@link ServerError} surfaces transient {@code 5xx} relay failures
 * that may be retried.
 *
 * @implNote
 * This implementation routes both the resolved success object and the rejected error envelope into
 * one typed sealed family so callers can match against a closed set.
 */
@WhatsAppWebModule(moduleName = "WAWebGroupInviteJob")
public sealed interface IqResetGroupInviteCodeResponse extends IqStanza.Response
        permits IqResetGroupInviteCodeResponse.Success, IqResetGroupInviteCodeResponse.ClientError, IqResetGroupInviteCodeResponse.ServerError {

    /**
     * Tries each {@link IqResetGroupInviteCodeResponse} variant in priority order and returns the first that parses cleanly.
     *
     * <p>The dispatcher hands the inbound {@link Stanza} together with the original outbound request
     * so that each variant can correlate echoed identifiers before claiming a match. Returns
     * {@link Optional#empty()} when none of the documented shapes match, which the caller should
     * treat as an unknown server reply and surface up.
     *
     * @implNote
     * This implementation tries {@link Success} first, then {@link ClientError}, then
     * {@link ServerError}, asserting the success path before any error envelope is inspected.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza used to validate echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupInviteJob",
            exports = "resetGroupInviteCode", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqResetGroupInviteCodeResponse> of(Stanza stanza, Stanza request) {
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
     * Models the success reply variant carrying the freshly-rotated invite code.
     *
     * <p>Carries the freshly-rotated invite code echoed by the relay inside
     * {@code <invite code="..."/>}; callers should overwrite any cached
     * {@code chat.whatsapp.com/<code>} URL with this new value before surfacing the result to the
     * user.
     *
     * @implNote
     * This implementation reads the {@code code} attribute on the {@code <invite>} child of the
     * result envelope into the single {@link #code()} accessor.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupInviteJob")
    final class Success implements IqResetGroupInviteCodeResponse {
        /**
         * Holds the freshly-rotated invite code.
         */
        private final String code;

        /**
         * Constructs a success reply carrying the given invite code.
         *
         * @param code the new invite code; never {@code null}
         * @throws NullPointerException if {@code code} is {@code null}
         */
        public Success(String code) {
            this.code = Objects.requireNonNull(code, "code cannot be null");
        }

        /**
         * Returns the freshly-rotated invite code.
         *
         * @return the new invite code; never {@code null}
         */
        public String code() {
            return code;
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         *
         * <p>Callers normally reach this through
         * {@link IqResetGroupInviteCodeResponse#of(Stanza, Stanza)}; this factory is exposed so callers
         * can short-circuit when they already know the wire shape is a success.
         *
         * @implNote
         * This implementation first runs {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} to
         * confirm the envelope is an IQ {@code result} matching the request id, then reads the
         * {@code <invite>} child and extracts its {@code code} attribute; missing either yields
         * {@link Optional#empty()} so the caller can fall through to an error envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGroupInviteJob",
                exports = "resetGroupInviteCode",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var inviteChild = stanza.getChild("invite").orElse(null);
            if (inviteChild == null) {
                return Optional.empty();
            }
            var code = inviteChild.getAttributeAsString("code").orElse(null);
            if (code == null) {
                return Optional.empty();
            }
            return Optional.of(new Success(code));
        }

        /**
         * Compares this reply with another object for equality.
         *
         * <p>Two replies are equal when they carry the same {@link #code()}.
         *
         * @param obj the object to compare with; may be {@code null}
         * @return {@code true} when {@code obj} is an equal reply, {@code false} otherwise
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
            return Objects.equals(this.code, that.code);
        }

        /**
         * Returns a hash code derived from the invite code.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(code);
        }

        /**
         * Returns a debug string describing the invite code.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqResetGroupInviteCodeResponse.Success[code=" + code + ']';
        }
    }

    /**
     * Models the client-error reply variant for envelope-level caller-side rejections.
     *
     * <p>Surfaces caller-side rejections of the rotate request: typically {@code 403} when the
     * caller is not an admin of the target group, {@code 404} when the group does not exist, or
     * {@code 400} on a malformed stanza. Retrying the same request is pointless; the caller must
     * surface the failure to the user.
     *
     * @implNote
     * This implementation reads the {@code <error>} envelope's {@code code} and {@code text}
     * attributes into {@link #errorCode()} and {@link #errorText()}.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupInviteJob")
    final class ClientError implements IqResetGroupInviteCodeResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text when the relay supplied one, otherwise {@code null}.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply.
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
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>Callers normally reach this through
         * {@link IqResetGroupInviteCodeResponse#of(Stanza, Stanza)}; this factory is exposed so callers
         * can short-circuit when they already know the wire shape is a client error.
         *
         * @implNote
         * This implementation delegates to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, which validates the IQ
         * envelope's {@code type="error"} attribute and the {@code <error>} child's {@code code} in
         * the {@code 4xx} range before extracting code/text.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGroupInviteJob",
                exports = "resetGroupInviteCode", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with another object for equality.
         *
         * <p>Two replies are equal when they carry the same {@link #errorCode()} and the same
         * {@link #errorText()}.
         *
         * @param obj the object to compare with; may be {@code null}
         * @return {@code true} when {@code obj} is an equal reply, {@code false} otherwise
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
         * Returns a hash code derived from the error code and text.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string describing the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqResetGroupInviteCodeResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the server-error reply variant for transient relay failures.
     *
     * <p>Surfaces transient {@code 5xx} relay failures while rotating the invite code; the request
     * may be retried after a backoff.
     *
     * @implNote
     * This implementation reads the {@code <error>} envelope's {@code code} and {@code text}
     * attributes into {@link #errorCode()} and {@link #errorText()}.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupInviteJob")
    final class ServerError implements IqResetGroupInviteCodeResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text when the relay supplied one, otherwise {@code null}.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply.
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
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>Callers normally reach this through
         * {@link IqResetGroupInviteCodeResponse#of(Stanza, Stanza)}; this factory is exposed so callers
         * can short-circuit when they already know the wire shape is a server error.
         *
         * @implNote
         * This implementation delegates to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, which validates the IQ
         * envelope's {@code type="error"} attribute and the {@code <error>} child's {@code code} in
         * the {@code 5xx} range before extracting code/text.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGroupInviteJob",
                exports = "resetGroupInviteCode", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with another object for equality.
         *
         * <p>Two replies are equal when they carry the same {@link #errorCode()} and the same
         * {@link #errorText()}.
         *
         * @param obj the object to compare with; may be {@code null}
         * @return {@code true} when {@code obj} is an equal reply, {@code false} otherwise
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
         * Returns a hash code derived from the error code and text.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string describing the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqResetGroupInviteCodeResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

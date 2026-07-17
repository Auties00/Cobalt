package com.github.auties00.cobalt.wire.stanza.smax.pushconfig;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the sealed family of inbound reply variants for a {@link SmaxPushConfigSetRequest}.
 *
 * <p>A reply resolves to exactly one of three shapes: {@link Success} when the relay accepted the
 * change, {@link InternalServerError} for a transient {@code 500}, and {@link Conflict} for a
 * {@code 409} raised when another registration already holds the same push channel. The two error
 * variants expose their {@code (code, text)} pair so callers can log the failure and decide whether
 * to retry; recovery is left to the caller rather than performed inline.
 */
public sealed interface SmaxPushConfigSetResponse extends SmaxStanza.Response
        permits SmaxPushConfigSetResponse.Success, SmaxPushConfigSetResponse.InternalServerError, SmaxPushConfigSetResponse.Conflict {

    /**
     * Lifts an inbound IQ stanza into the sealed disjunction by trying each variant in priority
     * order.
     *
     * <p>This is the dispatcher entry point used by the SMAX layer to parse a push-config reply.
     *
     * @implNote This implementation tries {@link Success}, then {@link InternalServerError}, then
     * {@link Conflict}, mirroring the WA Web call order in {@code WASmaxPushConfigSetRPC.sendSetRPC}.
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound stanza, used to validate echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no
     *         variant matches
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxPushConfigSetRPC",
            exports = "sendSetRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxPushConfigSetResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var internalServerError = InternalServerError.of(stanza, request);
        if (internalServerError.isPresent()) {
            return internalServerError;
        }
        return Conflict.of(stanza, request);
    }

    /**
     * Represents the {@code Success} reply variant.
     *
     * <p>Signals that the relay accepted the push-config change; the variant carries no payload
     * beyond the envelope echo and so holds no per-instance state.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPushConfigSetResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInPushConfigIQResultResponseMixin")
    final class Success implements SmaxPushConfigSetResponse {
        /**
         * Constructs a successful reply.
         *
         * <p>{@link #of(Stanza, Stanza)} builds the instance after envelope validation; the type
         * carries no per-instance state.
         */
        public Success() {
        }

        /**
         * Tries to parse a {@link Success} variant from the given reply.
         *
         * <p>Returns {@link Optional#empty()} when the envelope is not a well-formed
         * {@code <iq type="result">} echoing the request identifiers.
         *
         * @implNote This implementation delegates the envelope check to
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} because the success shape carries
         * no further state to extract.
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPushConfigSetResponseSuccess",
                exports = "parseSetResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * Compares this reply to another object for equality.
         *
         * @implNote This implementation treats every instance as equal to every other because the
         * type carries no state.
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link Success}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a class-level hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Success.class.hashCode();
        }

        /**
         * Returns a debug rendering of this reply.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxPushConfigSetResponse.Success[]";
        }
    }

    /**
     * Represents the {@code InternalServerError} reply variant carrying
     * {@code (500, "internal-server-error")}.
     *
     * <p>Signals a transient relay-side failure. The variant carries no payload beyond the fixed
     * {@code (code, text)} pair; callers decide whether to retry with backoff, since the parse does
     * no recovery of its own.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPushConfigSetResponseInternalServerError")
    @WhatsAppWebModule(moduleName = "WASmaxInPushConfigIQErrorInternalServerErrorMixin")
    final class InternalServerError implements SmaxPushConfigSetResponse {
        /**
         * Constructs an internal-server-error reply.
         *
         * <p>The shape carries no payload beyond the asserted {@code (500, "internal-server-error")}
         * pair, so callers normally let {@link #of(Stanza, Stanza)} build the instance.
         */
        public InternalServerError() {
        }

        /**
         * Returns the numeric error code.
         *
         * <p>Always {@code 500}; surfaced for symmetry with the {@code (code, text)} pair exposed
         * by other SMAX error variants.
         *
         * @return the code
         */
        public int errorCode() {
            return 500;
        }

        /**
         * Returns the error text.
         *
         * <p>Always {@code "internal-server-error"}; surfaced for symmetry with the
         * {@code (code, text)} pair exposed by other SMAX error variants.
         *
         * @return the text
         */
        public String errorText() {
            return "internal-server-error";
        }

        /**
         * Tries to parse an {@link InternalServerError} variant from the given reply.
         *
         * <p>Returns {@link Optional#empty()} for any reply whose error envelope does not carry
         * exactly {@code (500, "internal-server-error")}.
         *
         * @implNote This implementation re-uses
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} for envelope validation and
         * then asserts the literal {@code (500, "internal-server-error")} pair.
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPushConfigSetResponseInternalServerError",
                exports = "parseSetResponseInternalServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<InternalServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            if (envelope.code() != 500 || !"internal-server-error".equals(envelope.text())) {
                return Optional.empty();
            }
            return Optional.of(new InternalServerError());
        }

        /**
         * Compares this reply to another object for equality.
         *
         * @implNote This implementation treats every instance as equal to every other because the
         * type carries no per-instance state.
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an {@link InternalServerError}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a class-level hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return InternalServerError.class.hashCode();
        }

        /**
         * Returns a debug rendering of this reply.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxPushConfigSetResponse.InternalServerError[]";
        }
    }

    /**
     * Represents the {@code Conflict} reply variant carrying {@code (409, "conflict")}.
     *
     * <p>The requested push registration collides with an existing one (typically two devices
     * vying for the same push token); the relay keeps the existing registration. Callers surfacing
     * push-registration UIs should prompt the user to drop the conflicting device or send a
     * {@link SmaxPushConfigSetSetVariant.Clear} request before retrying.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPushConfigSetResponseConflict")
    @WhatsAppWebModule(moduleName = "WASmaxInPushConfigIQErrorConflictMixin")
    final class Conflict implements SmaxPushConfigSetResponse {
        /**
         * Constructs a conflict reply.
         *
         * <p>The shape carries no payload beyond the asserted {@code (409, "conflict")} pair, so
         * callers normally let {@link #of(Stanza, Stanza)} build the instance.
         */
        public Conflict() {
        }

        /**
         * Returns the numeric error code.
         *
         * <p>Always {@code 409}; surfaced for symmetry with the {@code (code, text)} pair exposed
         * by other SMAX error variants.
         *
         * @return the code
         */
        public int errorCode() {
            return 409;
        }

        /**
         * Returns the error text.
         *
         * <p>Always {@code "conflict"}; surfaced for symmetry with the {@code (code, text)} pair
         * exposed by other SMAX error variants.
         *
         * @return the text
         */
        public String errorText() {
            return "conflict";
        }

        /**
         * Tries to parse a {@link Conflict} variant from the given reply.
         *
         * <p>Returns {@link Optional#empty()} for any reply whose error envelope does not carry
         * exactly {@code (409, "conflict")}.
         *
         * @implNote This implementation re-uses
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} for envelope validation and
         * then asserts the literal {@code (409, "conflict")} pair.
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPushConfigSetResponseConflict",
                exports = "parseSetResponseConflict",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Conflict> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            if (envelope.code() != 409 || !"conflict".equals(envelope.text())) {
                return Optional.empty();
            }
            return Optional.of(new Conflict());
        }

        /**
         * Compares this reply to another object for equality.
         *
         * @implNote This implementation treats every instance as equal to every other because the
         * type carries no per-instance state.
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link Conflict}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a class-level hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Conflict.class.hashCode();
        }

        /**
         * Returns a debug rendering of this reply.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxPushConfigSetResponse.Conflict[]";
        }
    }
}

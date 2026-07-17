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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed reply family for a {@link SmaxGroupsRevokeRequestCodeRequest}.
 *
 * <p>The three permitted variants are {@link Success}, {@link ClientError}, and {@link ServerError}.
 * {@link Success} always wraps the per-participant outcome list returned by the relay; the envelope succeeds even
 * when individual candidates carry the literal {@code error="404"} marker (signalling no outstanding code), so
 * callers must walk {@link Success#participants()} to detect partial failures.
 */
public sealed interface SmaxGroupsRevokeRequestCodeResponse extends SmaxStanza.Response
        permits SmaxGroupsRevokeRequestCodeResponse.Success, SmaxGroupsRevokeRequestCodeResponse.ClientError, SmaxGroupsRevokeRequestCodeResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsRevokeRequestCodeResponse} variant in priority order
     * and returns the first that parses cleanly.
     *
     * <p>The variants are tried in the order {@link Success}, {@link ClientError}, {@link ServerError}.
     *
     * @implNote This implementation returns an empty {@link Optional} when the stanza shape matches none of the
     * variants; WA Web throws a parsing failure on the same path, but Cobalt defers the decision to the caller so
     * it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsRevokeRequestCodeRequest} stanza, used to validate
     *                echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsRevokeRequestCodeRPC",
            exports = "sendRevokeRequestCodeRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsRevokeRequestCodeResponse> of(Stanza stanza, Stanza request) {
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
     * Represents the reply variant carrying the per-participant outcome list when the relay accepted the request
     * envelope.
     *
     * <p>The IQ envelope succeeds even when every candidate carries the literal {@code error="404"} marker
     * (signalling that the candidate had no outstanding code to revoke), so callers must walk
     * {@link #participants()} to detect such partial failures.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsRevokeRequestCodeResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupAddressingModeMixin")
    final class Success implements SmaxGroupsRevokeRequestCodeResponse {
        /**
         * The optional {@code addressing_mode} attribute echoed on the IQ envelope.
         */
        private final String addressingMode;

        /**
         * The per-participant outcome rows projected from the {@code <revoke>} child.
         */
        private final List<RevokeParticipantResult> participants;

        /**
         * Constructs a {@link Success}.
         *
         * <p>The participant list is copied so post-construction mutation of the caller's list has no effect on
         * the variant.
         *
         * @param addressingMode the optional addressing-mode echo ({@code "lid"} or {@code "pn"}); may be
         *                       {@code null}
         * @param participants   the per-participant outcomes
         * @throws NullPointerException if {@code participants} is {@code null}
         */
        public Success(String addressingMode, List<RevokeParticipantResult> participants) {
            this.addressingMode = addressingMode;
            Objects.requireNonNull(participants, "participants cannot be null");
            this.participants = List.copyOf(participants);
        }

        /**
         * Returns the optional {@code addressing_mode} echo.
         *
         * <p>The relay flips between {@code "lid"} and {@code "pn"} according to the group's addressing mode; the
         * attribute is omitted on legacy groups.
         *
         * @return an {@link Optional} carrying the mode, or empty when the relay omitted it
         */
        public Optional<String> addressingMode() {
            return Optional.ofNullable(addressingMode);
        }

        /**
         * Returns the per-participant outcome rows.
         *
         * @return an unmodifiable list of outcome rows; never {@code null}
         */
        public List<RevokeParticipantResult> participants() {
            return participants;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         *
         * <p>The IQ must be a valid {@code type="result"} echo of {@code request}, validated through
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, must carry a {@code <revoke>} child, and every
         * {@code <participant>} grand-child must satisfy {@link RevokeParticipantResult#of(Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsRevokeRequestCodeResponseSuccess",
                exports = "parseRevokeRequestCodeResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var revoke = stanza.getChild("revoke").orElse(null);
            if (revoke == null) {
                return Optional.empty();
            }
            var addressingMode = stanza.getAttributeAsString("addressing_mode").orElse(null);
            var participants = new ArrayList<RevokeParticipantResult>();
            for (var participantNode : revoke.getChildren("participant")) {
                var participant = RevokeParticipantResult.of(participantNode).orElse(null);
                if (participant == null) {
                    return Optional.empty();
                }
                participants.add(participant);
            }
            return Optional.of(new Success(addressingMode, participants));
        }

        /**
         * Compares this success to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Success} with identical fields
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
            return Objects.equals(this.addressingMode, that.addressingMode)
                    && Objects.equals(this.participants, that.participants);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(addressingMode, participants);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsRevokeRequestCodeResponse.Success[addressingMode=" + addressingMode
                    + ", participants=" + participants + ']';
        }

        /**
         * Represents the per-participant outcome row produced by the relay for a single revocation candidate.
         *
         * <p>Each row exposes {@link #jid()} (always present), {@link #error()} (the literal {@code "404"} marker
         * when present, signalling the candidate had no outstanding code to revoke), and the optional
         * {@link #phoneNumber()} and {@link #username()} echoes.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsRevokeRequestCodeResponseSuccess")
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsPhoneNumberMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsUsernameAttMixin")
        public static final class RevokeParticipantResult {
            /**
             * The participant {@link Jid}, always present on every outcome row.
             */
            private final Jid jid;

            /**
             * The optional literal {@code error="404"} marker echoed by the relay.
             */
            private final String error;

            /**
             * The optional {@code phone_number} attribute echoed by the relay.
             */
            private final String phoneNumber;

            /**
             * The optional {@code username} attribute echoed by the relay.
             */
            private final String username;

            /**
             * Constructs a {@link RevokeParticipantResult} row.
             *
             * @param jid         the participant {@link Jid}
             * @param error       the optional literal {@code error="404"} marker; may be {@code null}
             * @param phoneNumber the optional {@code phone_number} echo; may be {@code null}
             * @param username    the optional {@code username} echo; may be {@code null}
             * @throws NullPointerException if {@code jid} is {@code null}
             */
            public RevokeParticipantResult(Jid jid, String error, String phoneNumber, String username) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.error = error;
                this.phoneNumber = phoneNumber;
                this.username = username;
            }

            /**
             * Returns the participant {@link Jid}.
             *
             * @return the {@link Jid}; never {@code null}
             */
            public Jid jid() {
                return jid;
            }

            /**
             * Returns the optional literal {@code error="404"} marker.
             *
             * <p>The relay surfaces this marker when the candidate had no outstanding code to revoke; the result
             * is empty when the revocation succeeded.
             *
             * @return an {@link Optional} carrying the marker, or empty
             */
            public Optional<String> error() {
                return Optional.ofNullable(error);
            }

            /**
             * Returns the optional {@code phone_number} echo.
             *
             * @return an {@link Optional} carrying the phone number, or empty when the relay omitted it
             */
            public Optional<String> phoneNumber() {
                return Optional.ofNullable(phoneNumber);
            }

            /**
             * Returns the optional {@code username} echo.
             *
             * @return an {@link Optional} carrying the username, or empty when the relay omitted it
             */
            public Optional<String> username() {
                return Optional.ofNullable(username);
            }

            /**
             * Tries to parse an outcome row from a single {@code <participant>} child of the {@code <revoke>}
             * payload.
             *
             * <p>The stanza must be a {@code <participant>} carrying a {@code jid} attribute, with an optional
             * literal {@code error="404"} attribute.
             *
             * @param stanza the {@code <participant>} child
             * @return an {@link Optional} carrying the parsed row, or empty when the stanza does not match
             * @throws NullPointerException if {@code stanza} is {@code null}
             */
            @WhatsAppWebExport(moduleName = "WASmaxInGroupsRevokeRequestCodeResponseSuccess",
                    exports = "parseRevokeRequestCodeResponseSuccessRevokeParticipant",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<RevokeParticipantResult> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("participant")) {
                    return Optional.empty();
                }
                var jid = stanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var error = stanza.getAttributeAsString("error").orElse(null);
                var phoneNumber = stanza.getAttributeAsString("phone_number").orElse(null);
                var username = stanza.getAttributeAsString("username").orElse(null);
                return Optional.of(new RevokeParticipantResult(jid, error, phoneNumber, username));
            }

            /**
             * Compares this row to {@code obj} for value equality across every field.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is a {@link RevokeParticipantResult} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (RevokeParticipantResult) obj;
                return Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.error, that.error)
                        && Objects.equals(this.phoneNumber, that.phoneNumber)
                        && Objects.equals(this.username, that.username);
            }

            /**
             * Returns a hash composed of every field.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, error, phoneNumber, username);
            }

            /**
             * Returns a debug string carrying every field.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsRevokeRequestCodeResponse.Success.RevokeParticipantResult[jid=" + jid
                        + ", error=" + error
                        + ", phoneNumber=" + phoneNumber
                        + ", username=" + username + ']';
            }
        }
    }

    /**
     * Represents the reply variant emitted when the relay rejected the request envelope as malformed,
     * unauthorised, or referencing a non-existent group.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsRevokeRequestCodeResponseClientError")
    final class ClientError implements SmaxGroupsRevokeRequestCodeResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed by the relay.
         */
        private final String errorText;

        /**
         * Constructs a {@link ClientError} from raw error attributes.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code echoed by the relay.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text echoed by the relay.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from {@code stanza}.
         *
         * <p>The shared {@code <iq type="error"><error code="..." text="..."/></iq>} envelope is validated through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, and its code and text populate the
         * returned variant.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsRevokeRequestCodeResponseClientError",
                exports = "parseRevokeRequestCodeResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this error to {@code obj} for value equality across both fields.
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
            return "SmaxGroupsRevokeRequestCodeResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Represents the reply variant emitted on transient relay-side failure.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsRevokeRequestCodeResponseServerError")
    final class ServerError implements SmaxGroupsRevokeRequestCodeResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed by the relay.
         */
        private final String errorText;

        /**
         * Constructs a {@link ServerError} from raw error attributes.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code echoed by the relay.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text echoed by the relay.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from {@code stanza}.
         *
         * <p>The shared {@code <iq type="error"><error code="..." text="..."/></iq>} envelope is validated through
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, and its code and text populate the
         * returned variant.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsRevokeRequestCodeResponseServerError",
                exports = "parseRevokeRequestCodeResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this error to {@code obj} for value equality across both fields.
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
            return "SmaxGroupsRevokeRequestCodeResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

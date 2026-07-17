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
 * Sealed reply family for a {@link SmaxGroupsAddParticipantsRequest}.
 *
 * The three variants partition every reply the relay can return: {@link Success}, {@link ClientError} and
 * {@link ServerError}. {@link Success} always wraps the per-participant outcome list; a successful envelope still
 * carries individual rejections, so callers must walk {@link Success#participants()} to detect partial failures.
 */
public sealed interface SmaxGroupsAddParticipantsResponse extends SmaxStanza.Response
        permits SmaxGroupsAddParticipantsResponse.Success, SmaxGroupsAddParticipantsResponse.ClientError, SmaxGroupsAddParticipantsResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsAddParticipantsResponse} variant in priority order
     * and returns the first that parses cleanly.
     *
     * {@link Success} is probed first, then {@link ClientError}, then {@link ServerError}.
     *
     * @implNote This implementation returns an empty {@link Optional} when the stanza shape matches none of the
     * documented variants; WA Web throws a parsing failure on the same path, but Cobalt defers the decision to the
     * caller so it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsAddParticipantsRequest} stanza, used to validate
     *                echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsAddParticipantsRPC",
            exports = "sendAddParticipantsRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsAddParticipantsResponse> of(Stanza stanza, Stanza request) {
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
     * Reply variant carrying the per-participant outcome list when the relay accepted the request envelope.
     *
     * The IQ envelope succeeds even when every candidate is rejected at the participant-policy level (private
     * accounts, blocked contacts, non-WA users); callers must walk {@link #participants()} to detect partial or
     * total rejection.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsAddParticipantsResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsAddParticipantsParticipantAddedOrNonRegisteredWaUserParticipantErrorLidResponseMixinGroup")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupAddressingModeMixin")
    final class Success implements SmaxGroupsAddParticipantsResponse {
        /**
         * Holds the optional {@code addressing_mode} attribute echoed on the IQ envelope.
         */
        private final String addressingMode;

        /**
         * Holds the per-participant outcome rows projected from the {@code <add>} child.
         */
        private final List<AddParticipantResult> participants;

        /**
         * Constructs a {@link Success}.
         *
         * The supplied participant list is defensively copied and a {@code null} value is treated as empty.
         *
         * @param addressingMode the optional addressing-mode echo ({@code "lid"} or {@code "pn"}); may be
         *                       {@code null}
         * @param participants   the per-participant outcomes; may be {@code null}
         */
        public Success(String addressingMode, List<AddParticipantResult> participants) {
            this.addressingMode = addressingMode;
            this.participants = List.copyOf(Objects.requireNonNullElse(participants, List.of()));
        }

        /**
         * Returns the optional {@code addressing_mode} echo.
         *
         * The relay flips between {@code "lid"} and {@code "pn"} according to the group's addressing mode; the
         * field is omitted on legacy groups.
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
        public List<AddParticipantResult> participants() {
            return participants;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         *
         * The IQ must be a valid {@code type="result"} echo of the request, must carry an {@code <add>} child, and
         * every {@code <participant>} grand-child must satisfy {@link AddParticipantResult#of(Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsAddParticipantsResponseSuccess",
                exports = "parseAddParticipantsResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var add = stanza.getChild("add").orElse(null);
            if (add == null) {
                return Optional.empty();
            }
            var addressingMode = stanza.getAttributeAsString("addressing_mode").orElse(null);
            var participants = new ArrayList<AddParticipantResult>();
            for (var participantNode : add.getChildren("participant")) {
                var participant = AddParticipantResult.of(participantNode).orElse(null);
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
            return "SmaxGroupsAddParticipantsResponse.Success[addressingMode=" + addressingMode
                    + ", participants=" + participants + ']';
        }

        /**
         * Per-participant outcome row produced by the relay for a single candidate JID.
         *
         * The always-present {@link #jid()} identifies the candidate; the optional {@link #nonRegisteredUser()}
         * payload discriminates the accepted arm (empty) from the rejected arm (present).
         *
         * @implNote This implementation fuses the WA Web wire-level disjunction (accepted-candidate arm vs.
         * non-registered-WA-user arm) into a single class so callers do not need to pattern-match on the
         * disjunction at every call site; the {@code nonRegisteredUser} {@link Optional} is the discriminator.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsAddParticipantsParticipantAddedResponseMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsNonRegisteredWaUserParticipantErrorLidResponseMixin")
        public static final class AddParticipantResult {
            /**
             * Holds the participant {@link Jid}, always present on both disjunction arms.
             */
            private final Jid jid;

            /**
             * Holds the optional {@code phone_number} attribute echoed by the relay.
             */
            private final String phoneNumber;

            /**
             * Holds the optional {@code username} attribute echoed by the relay.
             */
            private final String username;

            /**
             * Holds the optional non-registered-user payload, populated only on the rejected arm.
             */
            private final NonRegisteredWaUser nonRegisteredUser;

            /**
             * Constructs an {@link AddParticipantResult} row.
             *
             * @param jid               the participant {@link Jid}
             * @param phoneNumber       the optional {@code phone_number} echo; may be {@code null}
             * @param username          the optional {@code username} echo; may be {@code null}
             * @param nonRegisteredUser the optional rejection payload; may be {@code null}
             * @throws NullPointerException if {@code jid} is {@code null}
             */
            public AddParticipantResult(Jid jid, String phoneNumber, String username,
                                        NonRegisteredWaUser nonRegisteredUser) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.phoneNumber = phoneNumber;
                this.username = username;
                this.nonRegisteredUser = nonRegisteredUser;
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
             * Returns the optional non-registered-user payload.
             *
             * An empty value identifies this row as the accepted-candidate arm; a present value identifies it as
             * the rejected arm and carries the reason code via {@link NonRegisteredWaUser#errorCode()}.
             *
             * @return an {@link Optional} carrying the rejection payload
             */
            public Optional<NonRegisteredWaUser> nonRegisteredUser() {
                return Optional.ofNullable(nonRegisteredUser);
            }

            /**
             * Tries to parse an outcome row from a single {@code <participant>} child of the {@code <add>}
             * payload.
             *
             * The stanza must be a {@code <participant>} carrying a {@code jid} attribute, with an optional
             * {@code error} attribute discriminating the rejected arm.
             *
             * @param stanza the {@code <participant>} child
             * @return an {@link Optional} carrying the parsed row, or empty when the stanza does not match
             */
            @WhatsAppWebExport(moduleName = "WASmaxInGroupsAddParticipantsResponseSuccess",
                    exports = "parseAddParticipantsResponseSuccessAddParticipant",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<AddParticipantResult> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("participant")) {
                    return Optional.empty();
                }
                var jid = stanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var phoneNumber = stanza.getAttributeAsString("phone_number").orElse(null);
                var username = stanza.getAttributeAsString("username").orElse(null);
                var nonRegisteredUser = NonRegisteredWaUser.of(stanza).orElse(null);
                return Optional.of(new AddParticipantResult(jid, phoneNumber, username,
                        nonRegisteredUser));
            }

            /**
             * Compares this row to {@code obj} for value equality across every field.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is an {@link AddParticipantResult} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (AddParticipantResult) obj;
                return Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.phoneNumber, that.phoneNumber)
                        && Objects.equals(this.username, that.username)
                        && Objects.equals(this.nonRegisteredUser, that.nonRegisteredUser);
            }

            /**
             * Returns a hash composed of every field.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, phoneNumber, username, nonRegisteredUser);
            }

            /**
             * Returns a debug string carrying every field.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsAddParticipantsResponse.Success.AddParticipantResult[jid=" + jid
                        + ", phoneNumber=" + phoneNumber
                        + ", username=" + username
                        + ", nonRegisteredUser=" + nonRegisteredUser + ']';
            }

            /**
             * Rejection payload for a candidate whose supplied phone number or LID did not map to a registered
             * WhatsApp user.
             *
             * The {@link #errorCode()} identifies which rejection arm the relay took (request-code-can-be-sent
             * vs. cannot-be-created-for-legal-concerns vs. has-invalid-PN).
             */
            @WhatsAppWebModule(moduleName = "WASmaxInGroupsParticipantRequestCodeCanBeSentOrRequestCodeCannotBeCreatedForLegalConcernsOrHasInvalidPNMixinGroup")
            public static final class NonRegisteredWaUser {
                /**
                 * Holds the numeric reason code identifying why the candidate was rejected.
                 */
                private final int errorCode;

                /**
                 * Constructs a {@link NonRegisteredWaUser} payload.
                 *
                 * @param errorCode the numeric reason code
                 */
                public NonRegisteredWaUser(int errorCode) {
                    this.errorCode = errorCode;
                }

                /**
                 * Returns the numeric reason code.
                 *
                 * @return the error code
                 */
                public int errorCode() {
                    return errorCode;
                }

                /**
                 * Tries to parse a {@link NonRegisteredWaUser} payload from a {@code <participant>} child.
                 *
                 * The presence of a non-negative {@code error} attribute discriminates the rejected arm from the
                 * accepted-candidate arm; an empty value signals the accepted arm.
                 *
                 * @implNote This implementation reads the {@code error} attribute defaulting to {@code -1}; any
                 * negative value (including the missing-attribute default) signals the accepted arm and yields an
                 * empty {@link Optional}.
                 *
                 * @param stanza the {@code <participant>} child
                 * @return an {@link Optional} carrying the parsed payload, or empty when the stanza is on the
                 *         accepted arm
                 */
                public static Optional<NonRegisteredWaUser> of(Stanza stanza) {
                    Objects.requireNonNull(stanza, "stanza cannot be null");
                    var error = stanza.getAttributeAsInt("error").orElse(-1);
                    if (error < 0) {
                        return Optional.empty();
                    }
                    return Optional.of(new NonRegisteredWaUser(error));
                }

                /**
                 * Compares this payload to {@code obj} for value equality on {@link #errorCode()}.
                 *
                 * @param obj the other object
                 * @return {@code true} when {@code obj} is a {@link NonRegisteredWaUser} with the same code
                 */
                @Override
                public boolean equals(Object obj) {
                    if (obj == this) {
                        return true;
                    }
                    if (obj == null || obj.getClass() != this.getClass()) {
                        return false;
                    }
                    var that = (NonRegisteredWaUser) obj;
                    return this.errorCode == that.errorCode;
                }

                /**
                 * Returns a hash derived from {@link #errorCode()}.
                 *
                 * @return the hash code
                 */
                @Override
                public int hashCode() {
                    return Objects.hash(errorCode);
                }

                /**
                 * Returns a debug string carrying {@link #errorCode()}.
                 *
                 * @return the debug representation
                 */
                @Override
                public String toString() {
                    return "SmaxGroupsAddParticipantsResponse.Success.AddParticipantResult.NonRegisteredWaUser[errorCode="
                            + errorCode + ']';
                }
            }
        }
    }

    /**
     * Reply variant emitted when the relay rejected the request envelope as malformed, unauthorised, or
     * referencing a non-existent group.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsAddParticipantsResponseClientError")
    final class ClientError implements SmaxGroupsAddParticipantsResponse {
        /**
         * Holds the numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the relay.
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
         * Delegates the envelope validation to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)},
         * which checks the shared {@code <iq type="error"><error code="..." text="..."/></iq>} shape.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsAddParticipantsResponseClientError",
                exports = "parseAddParticipantsResponseClientError",
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
            return "SmaxGroupsAddParticipantsResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Reply variant emitted on transient relay-side failure.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsAddParticipantsResponseServerError")
    final class ServerError implements SmaxGroupsAddParticipantsResponse {
        /**
         * Holds the numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the relay.
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
         * Delegates the envelope validation to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)},
         * which checks the shared {@code <iq type="error"><error code="..." text="..."/></iq>} shape.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsAddParticipantsResponseServerError",
                exports = "parseAddParticipantsResponseServerError",
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
            return "SmaxGroupsAddParticipantsResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

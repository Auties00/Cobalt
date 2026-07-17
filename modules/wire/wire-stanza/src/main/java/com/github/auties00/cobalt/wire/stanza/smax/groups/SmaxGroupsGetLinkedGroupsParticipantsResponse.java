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
 * The sealed reply family for a {@link SmaxGroupsGetLinkedGroupsParticipantsRequest}.
 * <p>
 * Exactly one of three variants matches a given inbound stanza: {@link Success} carries the deduplicated participant
 * union across the community's sub-groups, {@link ClientError} carries a caller-side rejection code, and
 * {@link ServerError} carries a transient relay-side failure code.
 */
public sealed interface SmaxGroupsGetLinkedGroupsParticipantsResponse extends SmaxStanza.Response
        permits SmaxGroupsGetLinkedGroupsParticipantsResponse.Success, SmaxGroupsGetLinkedGroupsParticipantsResponse.ClientError, SmaxGroupsGetLinkedGroupsParticipantsResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsGetLinkedGroupsParticipantsResponse} variant and returns
     * the first that parses cleanly.
     * <p>
     * Variants are tried in priority order: {@link Success} first, then {@link ClientError}, then {@link ServerError}.
     * The result is empty when the stanza matches none of the three variants.
     *
     * @implNote This implementation defers the no-match decision to the caller by returning an empty {@link Optional}
     * rather than throwing, so the caller can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound request
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsGetLinkedGroupsParticipantsRPC",
            exports = "sendGetLinkedGroupsParticipantsRPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsGetLinkedGroupsParticipantsResponse> of(Stanza stanza, Stanza request) {
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
     * The reply variant emitted when the relay returned the participant union for the addressed community.
     * <p>
     * {@link #participants()} holds one {@link Participant} per distinct member across the community's sub-groups.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetLinkedGroupsParticipantsResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsParticipantWithJidMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsPhoneNumberMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupAddressingModeMixin")
    final class Success implements SmaxGroupsGetLinkedGroupsParticipantsResponse {
        /**
         * The deduplicated participant list across the addressed community's sub-groups.
         */
        private final List<Participant> participants;

        /**
         * Constructs a {@link Success} reply.
         *
         * @param participants the participant list; never {@code null}
         * @throws NullPointerException if {@code participants} is {@code null}
         */
        public Success(List<Participant> participants) {
            Objects.requireNonNull(participants, "participants cannot be null");
            this.participants = List.copyOf(participants);
        }

        /**
         * Returns the participant list.
         *
         * @return an unmodifiable list of {@link Participant}; never {@code null}
         */
        public List<Participant> participants() {
            return participants;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         * <p>
         * The envelope is validated through {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} and the
         * {@code <linked_groups_participants>} wrapper must be present; each {@code <participant/>} child is parsed via
         * {@link Participant#of(Stanza)} and a single failed child fails the whole parse.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetLinkedGroupsParticipantsResponseSuccess",
                exports = "parseGetLinkedGroupsParticipantsResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var wrapper = stanza.getChild("linked_groups_participants").orElse(null);
            if (wrapper == null) {
                return Optional.empty();
            }
            var participantNodes = wrapper.getChildren("participant");
            var participants = new ArrayList<Participant>(participantNodes.size());
            for (var participantNode : participantNodes) {
                var participant = Participant.of(participantNode).orElse(null);
                if (participant == null) {
                    return Optional.empty();
                }
                participants.add(participant);
            }
            return Optional.of(new Success(participants));
        }

        /**
         * Compares this success to {@code obj} for value equality across every field.
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
            return Objects.equals(this.participants, that.participants);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(participants);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetLinkedGroupsParticipantsResponse.Success[participants="
                    + participants + ']';
        }
    }

    /**
     * Per-participant projection carrying the addressing JID plus an optional resolved phone-number JID.
     * <p>
     * The phone-number JID is non-null only when the relay can map a LID participant to its PN counterpart.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsParticipantWithJidMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsPhoneNumberMixin")
    final class Participant {
        /**
         * The participant's primary addressing {@link Jid}.
         */
        private final Jid jid;

        /**
         * The optional resolved phone-number {@link Jid}; {@code null} when the relay omitted the PN-LID mapping.
         */
        private final Jid phoneNumber;

        /**
         * Constructs a {@link Participant} projection.
         *
         * @param jid         the addressing {@link Jid}; never {@code null}
         * @param phoneNumber the optional phone-number {@link Jid}; may be {@code null}
         * @throws NullPointerException if {@code jid} is {@code null}
         */
        public Participant(Jid jid, Jid phoneNumber) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
            this.phoneNumber = phoneNumber;
        }

        /**
         * Returns the participant's primary addressing {@link Jid}.
         *
         * @return the addressing {@link Jid}; never {@code null}
         */
        public Jid jid() {
            return jid;
        }

        /**
         * Returns the participant's resolved phone-number {@link Jid} when supplied by the relay.
         *
         * @return an {@link Optional} carrying the phone-number {@link Jid}, or empty when the relay omitted it
         */
        public Optional<Jid> phoneNumber() {
            return Optional.ofNullable(phoneNumber);
        }

        /**
         * Tries to parse a {@link Participant} from the given {@code <participant/>} child.
         * <p>
         * Parsing succeeds when the child carries the {@code jid} attribute; the {@code phone_number} attribute is
         * optional.
         *
         * @param stanza the {@code <participant/>} child stanza
         * @return an {@link Optional} carrying the parsed participant, or empty when the child does not match
         */
        public static Optional<Participant> of(Stanza stanza) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            if (!stanza.hasDescription("participant")) {
                return Optional.empty();
            }
            var jid = stanza.getAttributeAsJid("jid").orElse(null);
            if (jid == null) {
                return Optional.empty();
            }
            var phoneNumber = stanza.getAttributeAsJid("phone_number").orElse(null);
            return Optional.of(new Participant(jid, phoneNumber));
        }

        /**
         * Compares this participant to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Participant} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Participant) obj;
            return Objects.equals(this.jid, that.jid)
                    && Objects.equals(this.phoneNumber, that.phoneNumber);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(jid, phoneNumber);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetLinkedGroupsParticipantsResponse.Participant[jid=" + jid
                    + ", phoneNumber=" + phoneNumber + ']';
        }
    }

    /**
     * The reply variant emitted when the relay rejected the request as malformed, unauthorised, or referencing a
     * non-community group.
     * <p>
     * The {@link #errorCode()} carries the HTTP-style status assigned by the relay and {@link #errorText()} carries
     * the optional human-readable reason.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetLinkedGroupsParticipantsResponseClientError")
    final class ClientError implements SmaxGroupsGetLinkedGroupsParticipantsResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed by the relay; {@code null} when omitted.
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
         * <p>
         * The shared {@code <iq type="error"><error code="..." text="..."/></iq>} envelope is validated through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, which matches only client-range codes.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetLinkedGroupsParticipantsResponseClientError",
                exports = "parseGetLinkedGroupsParticipantsResponseClientError",
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
            return "SmaxGroupsGetLinkedGroupsParticipantsResponse.ClientError[errorCode="
                    + errorCode + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The reply variant emitted on transient relay-side failure.
     * <p>
     * Unlike {@link ClientError} this code typically signals a retry-eligible relay outage rather than a malformed or
     * unauthorised request; {@link #errorCode()} carries the server-range status and {@link #errorText()} the optional
     * reason.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetLinkedGroupsParticipantsResponseServerError")
    final class ServerError implements SmaxGroupsGetLinkedGroupsParticipantsResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed by the relay; {@code null} when omitted.
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
         * <p>
         * The shared {@code <iq type="error"><error code="..." text="..."/></iq>} envelope is validated through
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, which matches only server-range codes.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetLinkedGroupsParticipantsResponseServerError",
                exports = "parseGetLinkedGroupsParticipantsResponseServerError",
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
            return "SmaxGroupsGetLinkedGroupsParticipantsResponse.ServerError[errorCode="
                    + errorCode + ", errorText=" + errorText + ']';
        }
    }
}

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
 * The sealed reply family for a {@link SmaxGroupsPromoteDemoteAdminRequest}.
 *
 * <p>Unlike the non-admin promote/demote reply, both promote and demote arms share a single
 * {@link SuccessMultiAdmin} envelope: the relay returns one {@code <admin>} child that intermixes promotion and
 * demotion outcomes, and callers walk {@link SuccessMultiAdmin#participants()} to inspect each row's {@code type}
 * marker and optional rejection code. {@link ClientError} and {@link ServerError} surface the relay's reason codes.
 *
 * @deprecated superseded by {@code SmaxGroupsPromoteDemote}.
 */
@Deprecated
public sealed interface SmaxGroupsPromoteDemoteAdminResponse extends SmaxStanza.Response
        permits SmaxGroupsPromoteDemoteAdminResponse.SuccessMultiAdmin, SmaxGroupsPromoteDemoteAdminResponse.ClientError, SmaxGroupsPromoteDemoteAdminResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsPromoteDemoteAdminResponse} variant in priority order
     * and returns the first that parses cleanly.
     *
     * <p>Tries {@link SuccessMultiAdmin} first, then {@link ClientError}, then {@link ServerError}.
     *
     * @implNote The empty {@link Optional} surfaces when the stanza shape matches none of the documented variants;
     * WA Web throws {@code SmaxParsingFailure} on the same path, but Cobalt defers the decision to the caller so
     * it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsPromoteDemoteAdminRequest} stanza, used to validate
     *                echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsPromoteDemoteAdminRPC",
            exports = "sendPromoteDemoteAdminRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsPromoteDemoteAdminResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = SuccessMultiAdmin.of(stanza, request);
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
     * The reply variant carrying the per-participant outcome list when the relay accepted the admin-roster
     * mutation envelope.
     *
     * <p>The {@code <admin>} child returned by the relay merges promotion and demotion results into one flat list:
     * each {@link AdminParticipantResult} carries an optional {@code type="admin"} marker (set on confirmed
     * promotions, omitted on demotions) and an optional rejection code. The IQ envelope succeeds even when every
     * candidate is rejected at the participant-policy level, so callers must inspect each row.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsPromoteDemoteAdminResponseSuccessMultiAdmin")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupAddressingModeMixin")
    final class SuccessMultiAdmin implements SmaxGroupsPromoteDemoteAdminResponse {
        /**
         * The optional {@code addressing_mode} attribute echoed on the IQ envelope.
         */
        private final String addressingMode;

        /**
         * The per-participant outcome rows projected from the {@code <admin>} child.
         */
        private final List<AdminParticipantResult> participants;

        /**
         * Constructs a {@link SuccessMultiAdmin}.
         *
         * @param addressingMode the optional addressing-mode echo ({@code "lid"} or {@code "pn"}); may be
         *                       {@code null}
         * @param participants   the per-participant outcomes; defensively copied, {@code null} treated as empty
         */
        public SuccessMultiAdmin(String addressingMode, List<AdminParticipantResult> participants) {
            this.addressingMode = addressingMode;
            this.participants = List.copyOf(Objects.requireNonNullElse(participants, List.of()));
        }

        /**
         * Returns the optional {@code addressing_mode} echo.
         *
         * <p>The relay flips between {@code "lid"} and {@code "pn"} according to the group's addressing mode; the
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
        public List<AdminParticipantResult> participants() {
            return participants;
        }

        /**
         * Tries to parse a {@link SuccessMultiAdmin} variant from {@code stanza}.
         *
         * <p>The IQ must be a valid {@code type="result"} echo of the request, must carry an {@code <admin>}
         * child, and every {@code <participant>} grand-child must satisfy {@link AdminParticipantResult#of(Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsPromoteDemoteAdminResponseSuccessMultiAdmin",
                exports = "parsePromoteDemoteAdminResponseSuccessMultiAdmin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<SuccessMultiAdmin> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var admin = stanza.getChild("admin").orElse(null);
            if (admin == null) {
                return Optional.empty();
            }
            var addressingMode = stanza.getAttributeAsString("addressing_mode").orElse(null);
            var participants = new ArrayList<AdminParticipantResult>();
            for (var participantNode : admin.getChildren("participant")) {
                var participant = AdminParticipantResult.of(participantNode).orElse(null);
                if (participant == null) {
                    return Optional.empty();
                }
                participants.add(participant);
            }
            return Optional.of(new SuccessMultiAdmin(addressingMode, participants));
        }

        /**
         * Compares this success to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link SuccessMultiAdmin} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (SuccessMultiAdmin) obj;
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
            return "SmaxGroupsPromoteDemoteAdminResponse.SuccessMultiAdmin[addressingMode=" + addressingMode
                    + ", participants=" + participants + ']';
        }

        /**
         * The per-participant outcome row produced by the relay for a single admin-roster mutation candidate.
         *
         * <p>Each row exposes {@link #jid()} (always present), {@link #type()} (set to {@code "admin"} on confirmed
         * promotions, empty on demotions and on rejections), {@link #errorCode()} (the rejection code on rejected
         * entries), and the optional {@link #phoneNumber()} / {@link #username()} echoes. Callers distinguish
         * promotion from demotion by matching the request's promote list against the row's JID; the relay does not
         * duplicate the promote/demote split in the response.
         */
        public static final class AdminParticipantResult {
            /**
             * The participant {@link Jid}, always present on every outcome row.
             */
            private final Jid jid;

            /**
             * The optional {@code type} marker echoed by the relay.
             */
            private final String type;

            /**
             * The optional rejection-error code lifted from the {@code error} attribute.
             */
            private final Integer errorCode;

            /**
             * The optional {@code phone_number} attribute echoed by the relay.
             */
            private final String phoneNumber;

            /**
             * The optional {@code username} attribute echoed by the relay.
             */
            private final String username;

            /**
             * Constructs an {@link AdminParticipantResult} row.
             *
             * @param jid         the participant {@link Jid}
             * @param type        the optional {@code type} marker; may be {@code null}
             * @param errorCode   the optional rejection-error code; may be {@code null}
             * @param phoneNumber the optional {@code phone_number} echo; may be {@code null}
             * @param username    the optional {@code username} echo; may be {@code null}
             * @throws NullPointerException if {@code jid} is {@code null}
             */
            public AdminParticipantResult(Jid jid, String type, Integer errorCode,
                                          String phoneNumber, String username) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.type = type;
                this.errorCode = errorCode;
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
             * Returns the optional {@code type} marker.
             *
             * <p>Present and set to {@code "admin"} on confirmed promotions; empty on demotions and on rejected
             * entries.
             *
             * @return an {@link Optional} carrying the marker, or empty
             */
            public Optional<String> type() {
                return Optional.ofNullable(type);
            }

            /**
             * Returns the optional rejection-error code.
             *
             * <p>The code is one of {@code 403}, {@code 404}, {@code 406}, or {@code 419}; empty when the row
             * succeeded.
             *
             * @return an {@link Optional} carrying the error code, or empty
             */
            public Optional<Integer> errorCode() {
                return Optional.ofNullable(errorCode);
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
             * Tries to parse an outcome row from a single {@code <participant>} child of the {@code <admin>}
             * payload.
             *
             * <p>The stanza must be a {@code <participant>} carrying a {@code jid} attribute, with optional
             * {@code type="admin"} and {@code error} attributes; the {@code error} value is one of {@code 403},
             * {@code 404}, {@code 406}, or {@code 419}.
             *
             * @param stanza the {@code <participant>} child
             * @return an {@link Optional} carrying the parsed row, or empty when the stanza does not match
             */
            @WhatsAppWebExport(moduleName = "WASmaxInGroupsPromoteDemoteAdminResponseSuccessMultiAdmin",
                    exports = "parsePromoteDemoteAdminResponseSuccessMultiAdminAdminParticipant",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<AdminParticipantResult> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("participant")) {
                    return Optional.empty();
                }
                var jid = stanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var type = stanza.getAttributeAsString("type").orElse(null);
                var error = stanza.getAttributeAsInt("error").orElse(-1);
                var errorCode = error < 0 ? null : Integer.valueOf(error);
                var phoneNumber = stanza.getAttributeAsString("phone_number").orElse(null);
                var username = stanza.getAttributeAsString("username").orElse(null);
                return Optional.of(new AdminParticipantResult(jid, type, errorCode,
                        phoneNumber, username));
            }

            /**
             * Compares this row to {@code obj} for value equality across every field.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is an {@link AdminParticipantResult} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (AdminParticipantResult) obj;
                return Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.type, that.type)
                        && Objects.equals(this.errorCode, that.errorCode)
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
                return Objects.hash(jid, type, errorCode, phoneNumber, username);
            }

            /**
             * Returns a debug string carrying every field.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsPromoteDemoteAdminResponse.SuccessMultiAdmin.AdminParticipantResult[jid=" + jid
                        + ", type=" + type
                        + ", errorCode=" + errorCode
                        + ", phoneNumber=" + phoneNumber
                        + ", username=" + username + ']';
            }
        }
    }

    /**
     * The reply variant emitted when the relay rejected the request envelope as malformed, unauthorised, or
     * referencing a non-existent community parent group.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsPromoteDemoteAdminResponseClientError")
    final class ClientError implements SmaxGroupsPromoteDemoteAdminResponse {
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
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} which validates the shared
         * {@code <iq type="error"><error code="..." text="..."/></iq>} envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsPromoteDemoteAdminResponseClientError",
                exports = "parsePromoteDemoteAdminResponseClientError",
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
            return "SmaxGroupsPromoteDemoteAdminResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The reply variant emitted on transient relay-side failure.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsPromoteDemoteAdminResponseServerError")
    final class ServerError implements SmaxGroupsPromoteDemoteAdminResponse {
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
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} which validates the shared
         * {@code <iq type="error"><error code="..." text="..."/></iq>} envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsPromoteDemoteAdminResponseServerError",
                exports = "parsePromoteDemoteAdminResponseServerError",
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
            return "SmaxGroupsPromoteDemoteAdminResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

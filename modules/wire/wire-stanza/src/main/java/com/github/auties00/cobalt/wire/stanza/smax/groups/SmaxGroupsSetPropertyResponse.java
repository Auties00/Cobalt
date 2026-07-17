package com.github.auties00.cobalt.wire.stanza.smax.groups;

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
 * Models the sealed reply family for a {@link SmaxGroupsSetPropertyRequest}.
 *
 * <p>The three permitted variants are {@link Success}, {@link ClientError}, and {@link ServerError}.
 * {@link Success} echoes back the set of toggles the relay actually applied, decomposed into typed accessors that
 * mirror the request's flag set; callers compare the echoed flags against the requested flags to detect partially
 * honoured mutations.
 */
public sealed interface SmaxGroupsSetPropertyResponse extends SmaxStanza.Response
        permits SmaxGroupsSetPropertyResponse.Success, SmaxGroupsSetPropertyResponse.ClientError, SmaxGroupsSetPropertyResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsSetPropertyResponse} variant in priority order and
     * returns the first that parses cleanly.
     *
     * <p>The variants are tried in the order {@link Success}, {@link ClientError}, {@link ServerError}.
     *
     * @implNote This implementation returns an empty {@link Optional} when the stanza shape matches none of the
     * variants; WA Web throws a parsing failure on the same path, but Cobalt defers the decision to the caller so
     * it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsSetPropertyRequest} stanza, used to validate echoed
     *                identifiers
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsSetPropertyRPC",
            exports = "sendSetPropertyRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsSetPropertyResponse> of(Stanza stanza, Stanza request) {
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
     * Represents the reply variant emitted when the relay accepted the property mutation.
     *
     * <p>Each requested toggle is echoed back as a sibling child under the IQ envelope; the typed accessors below
     * report which children were echoed. The {@code <ephemeral/>} echo is decomposed into its {@code expiration}
     * and {@code trigger} attributes, and the {@code <membership_approval_mode/>} echo is exposed as the raw
     * {@code group_join_mode} string. Callers compare each echoed flag against the requested flag to detect
     * partially honoured mutations.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsSetPropertyResponseSuccess")
    final class Success implements SmaxGroupsSetPropertyResponse {
        /**
         * Whether the relay echoed a {@code <locked/>} child.
         */
        private final boolean locked;

        /**
         * Whether the relay echoed an {@code <announcement/>} child.
         */
        private final boolean announcement;

        /**
         * Whether the relay echoed a {@code <no_frequently_forwarded/>} child.
         */
        private final boolean noFrequentlyForwarded;

        /**
         * The optional ephemeral-message expiration echoed by the relay.
         */
        private final Integer ephemeralExpiration;

        /**
         * The optional ephemeral-message trigger echoed by the relay.
         */
        private final Integer ephemeralTrigger;

        /**
         * Whether the relay echoed an {@code <unlocked/>} child.
         */
        private final boolean unlocked;

        /**
         * Whether the relay echoed a {@code <not_announcement/>} child.
         */
        private final boolean notAnnouncement;

        /**
         * Whether the relay echoed a {@code <frequently_forwarded_ok/>} child.
         */
        private final boolean frequentlyForwardedOk;

        /**
         * Whether the relay echoed a {@code <not_ephemeral/>} child.
         */
        private final boolean notEphemeral;

        /**
         * The optional membership-approval mode echoed by the relay.
         */
        private final String membershipApprovalGroupJoinMode;

        /**
         * Whether the relay echoed an {@code <allow_admin_reports/>} child.
         */
        private final boolean allowAdminReports;

        /**
         * Whether the relay echoed a {@code <not_allow_admin_reports/>} child.
         */
        private final boolean notAllowAdminReports;

        /**
         * Whether the relay echoed an {@code <allow_non_admin_sub_group_creation/>} child.
         */
        private final boolean allowNonAdminSubGroupCreation;

        /**
         * Whether the relay echoed a {@code <not_allow_non_admin_sub_group_creation/>} child.
         */
        private final boolean notAllowNonAdminSubGroupCreation;

        /**
         * Whether the relay echoed a {@code <group_history/>} child.
         */
        private final boolean groupHistory;

        /**
         * Whether the relay echoed a {@code <no_group_history/>} child.
         */
        private final boolean noGroupHistory;

        /**
         * Constructs a {@link Success} from the per-toggle echo flags and optional attribute echoes.
         *
         * @param locked                            see {@link #locked()}
         * @param announcement                      see {@link #announcement()}
         * @param noFrequentlyForwarded             see {@link #noFrequentlyForwarded()}
         * @param ephemeralExpiration               optional ephemeral expiration echo; may be {@code null}
         * @param ephemeralTrigger                  optional ephemeral trigger echo; may be {@code null}
         * @param unlocked                          see {@link #unlocked()}
         * @param notAnnouncement                   see {@link #notAnnouncement()}
         * @param frequentlyForwardedOk             see {@link #frequentlyForwardedOk()}
         * @param notEphemeral                      see {@link #notEphemeral()}
         * @param membershipApprovalGroupJoinMode   optional membership-approval mode echo; may be {@code null}
         * @param allowAdminReports                 see {@link #allowAdminReports()}
         * @param notAllowAdminReports              see {@link #notAllowAdminReports()}
         * @param allowNonAdminSubGroupCreation     see {@link #allowNonAdminSubGroupCreation()}
         * @param notAllowNonAdminSubGroupCreation  see {@link #notAllowNonAdminSubGroupCreation()}
         * @param groupHistory                      see {@link #groupHistory()}
         * @param noGroupHistory                    see {@link #noGroupHistory()}
         */
        public Success(boolean locked,
                       boolean announcement,
                       boolean noFrequentlyForwarded,
                       Integer ephemeralExpiration,
                       Integer ephemeralTrigger,
                       boolean unlocked,
                       boolean notAnnouncement,
                       boolean frequentlyForwardedOk,
                       boolean notEphemeral,
                       String membershipApprovalGroupJoinMode,
                       boolean allowAdminReports,
                       boolean notAllowAdminReports,
                       boolean allowNonAdminSubGroupCreation,
                       boolean notAllowNonAdminSubGroupCreation,
                       boolean groupHistory,
                       boolean noGroupHistory) {
            this.locked = locked;
            this.announcement = announcement;
            this.noFrequentlyForwarded = noFrequentlyForwarded;
            this.ephemeralExpiration = ephemeralExpiration;
            this.ephemeralTrigger = ephemeralTrigger;
            this.unlocked = unlocked;
            this.notAnnouncement = notAnnouncement;
            this.frequentlyForwardedOk = frequentlyForwardedOk;
            this.notEphemeral = notEphemeral;
            this.membershipApprovalGroupJoinMode = membershipApprovalGroupJoinMode;
            this.allowAdminReports = allowAdminReports;
            this.notAllowAdminReports = notAllowAdminReports;
            this.allowNonAdminSubGroupCreation = allowNonAdminSubGroupCreation;
            this.notAllowNonAdminSubGroupCreation = notAllowNonAdminSubGroupCreation;
            this.groupHistory = groupHistory;
            this.noGroupHistory = noGroupHistory;
        }

        /**
         * Returns whether the relay echoed a {@code <locked/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean locked() {
            return locked;
        }

        /**
         * Returns whether the relay echoed an {@code <announcement/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean announcement() {
            return announcement;
        }

        /**
         * Returns whether the relay echoed a {@code <no_frequently_forwarded/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean noFrequentlyForwarded() {
            return noFrequentlyForwarded;
        }

        /**
         * Returns the optional ephemeral-message expiration echoed by the relay.
         *
         * @return an {@link Optional} carrying the expiration in seconds, or empty when the relay did not echo
         *         the {@code <ephemeral/>} child
         */
        public Optional<Integer> ephemeralExpiration() {
            return Optional.ofNullable(ephemeralExpiration);
        }

        /**
         * Returns the optional ephemeral-message trigger echoed by the relay.
         *
         * @return an {@link Optional} carrying the trigger, or empty when the relay omitted it
         */
        public Optional<Integer> ephemeralTrigger() {
            return Optional.ofNullable(ephemeralTrigger);
        }

        /**
         * Returns whether the relay echoed an {@code <unlocked/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean unlocked() {
            return unlocked;
        }

        /**
         * Returns whether the relay echoed a {@code <not_announcement/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean notAnnouncement() {
            return notAnnouncement;
        }

        /**
         * Returns whether the relay echoed a {@code <frequently_forwarded_ok/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean frequentlyForwardedOk() {
            return frequentlyForwardedOk;
        }

        /**
         * Returns whether the relay echoed a {@code <not_ephemeral/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean notEphemeral() {
            return notEphemeral;
        }

        /**
         * Returns the optional membership-approval mode echoed by the relay.
         *
         * @return an {@link Optional} carrying the join-mode value, or empty when the relay did not echo the
         *         {@code <membership_approval_mode/>} child
         */
        public Optional<String> membershipApprovalGroupJoinMode() {
            return Optional.ofNullable(membershipApprovalGroupJoinMode);
        }

        /**
         * Returns whether the relay echoed an {@code <allow_admin_reports/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean allowAdminReports() {
            return allowAdminReports;
        }

        /**
         * Returns whether the relay echoed a {@code <not_allow_admin_reports/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean notAllowAdminReports() {
            return notAllowAdminReports;
        }

        /**
         * Returns whether the relay echoed an {@code <allow_non_admin_sub_group_creation/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean allowNonAdminSubGroupCreation() {
            return allowNonAdminSubGroupCreation;
        }

        /**
         * Returns whether the relay echoed a {@code <not_allow_non_admin_sub_group_creation/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean notAllowNonAdminSubGroupCreation() {
            return notAllowNonAdminSubGroupCreation;
        }

        /**
         * Returns whether the relay echoed a {@code <group_history/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean groupHistory() {
            return groupHistory;
        }

        /**
         * Returns whether the relay echoed a {@code <no_group_history/>} child.
         *
         * @return {@code true} when the toggle was applied
         */
        public boolean noGroupHistory() {
            return noGroupHistory;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         *
         * <p>The IQ must be a valid {@code type="result"} echo of {@code request}, validated through
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}; each documented toggle is captured via a
         * presence check on the corresponding child, and the {@code <ephemeral/>} and
         * {@code <membership_approval_mode/>} children additionally extract their attributes.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsSetPropertyResponseSuccess",
                exports = "parseSetPropertyResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var hasLocked = stanza.getChild("locked").isPresent();
            var hasAnnouncement = stanza.getChild("announcement").isPresent();
            var hasNoFrequentlyForwarded = stanza.getChild("no_frequently_forwarded").isPresent();
            var ephemeral = stanza.getChild("ephemeral").orElse(null);
            Integer ephemeralExpiration = null;
            Integer ephemeralTrigger = null;
            if (ephemeral != null) {
                ephemeralExpiration = ephemeral.getAttributeAsInt("expiration").orElse(0);
                if (ephemeral.getAttributeAsInt("trigger").isPresent()) {
                    ephemeralTrigger = ephemeral.getAttributeAsInt("trigger").getAsInt();
                }
            }
            var hasUnlocked = stanza.getChild("unlocked").isPresent();
            var hasNotAnnouncement = stanza.getChild("not_announcement").isPresent();
            var hasFrequentlyForwardedOk = stanza.getChild("frequently_forwarded_ok").isPresent();
            var hasNotEphemeral = stanza.getChild("not_ephemeral").isPresent();
            var membershipApprovalMode = stanza.getChild("membership_approval_mode").orElse(null);
            String membershipApprovalJoinMode = null;
            if (membershipApprovalMode != null) {
                membershipApprovalJoinMode = membershipApprovalMode.getAttributeAsString("group_join_mode").orElse(null);
            }
            var hasAllowAdminReports = stanza.getChild("allow_admin_reports").isPresent();
            var hasNotAllowAdminReports = stanza.getChild("not_allow_admin_reports").isPresent();
            var hasAllowNonAdminSubGroupCreation = stanza.getChild("allow_non_admin_sub_group_creation").isPresent();
            var hasNotAllowNonAdminSubGroupCreation = stanza.getChild("not_allow_non_admin_sub_group_creation").isPresent();
            var hasGroupHistory = stanza.getChild("group_history").isPresent();
            var hasNoGroupHistory = stanza.getChild("no_group_history").isPresent();
            var success = new Success(
                    hasLocked,
                    hasAnnouncement,
                    hasNoFrequentlyForwarded,
                    ephemeralExpiration,
                    ephemeralTrigger,
                    hasUnlocked,
                    hasNotAnnouncement,
                    hasFrequentlyForwardedOk,
                    hasNotEphemeral,
                    membershipApprovalJoinMode,
                    hasAllowAdminReports,
                    hasNotAllowAdminReports,
                    hasAllowNonAdminSubGroupCreation,
                    hasNotAllowNonAdminSubGroupCreation,
                    hasGroupHistory,
                    hasNoGroupHistory);
            return Optional.of(success);
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
            return this.locked == that.locked
                    && this.announcement == that.announcement
                    && this.noFrequentlyForwarded == that.noFrequentlyForwarded
                    && this.unlocked == that.unlocked
                    && this.notAnnouncement == that.notAnnouncement
                    && this.frequentlyForwardedOk == that.frequentlyForwardedOk
                    && this.notEphemeral == that.notEphemeral
                    && this.allowAdminReports == that.allowAdminReports
                    && this.notAllowAdminReports == that.notAllowAdminReports
                    && this.allowNonAdminSubGroupCreation == that.allowNonAdminSubGroupCreation
                    && this.notAllowNonAdminSubGroupCreation == that.notAllowNonAdminSubGroupCreation
                    && this.groupHistory == that.groupHistory
                    && this.noGroupHistory == that.noGroupHistory
                    && Objects.equals(this.ephemeralExpiration, that.ephemeralExpiration)
                    && Objects.equals(this.ephemeralTrigger, that.ephemeralTrigger)
                    && Objects.equals(this.membershipApprovalGroupJoinMode, that.membershipApprovalGroupJoinMode);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(locked, announcement, noFrequentlyForwarded, ephemeralExpiration, ephemeralTrigger,
                    unlocked, notAnnouncement, frequentlyForwardedOk, notEphemeral,
                    membershipApprovalGroupJoinMode, allowAdminReports, notAllowAdminReports,
                    allowNonAdminSubGroupCreation, notAllowNonAdminSubGroupCreation, groupHistory, noGroupHistory);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsSetPropertyResponse.Success[locked=" + locked
                    + ", announcement=" + announcement
                    + ", noFrequentlyForwarded=" + noFrequentlyForwarded
                    + ", ephemeralExpiration=" + ephemeralExpiration
                    + ", ephemeralTrigger=" + ephemeralTrigger
                    + ", unlocked=" + unlocked
                    + ", notAnnouncement=" + notAnnouncement
                    + ", frequentlyForwardedOk=" + frequentlyForwardedOk
                    + ", notEphemeral=" + notEphemeral
                    + ", membershipApprovalGroupJoinMode=" + membershipApprovalGroupJoinMode
                    + ", allowAdminReports=" + allowAdminReports
                    + ", notAllowAdminReports=" + notAllowAdminReports
                    + ", allowNonAdminSubGroupCreation=" + allowNonAdminSubGroupCreation
                    + ", notAllowNonAdminSubGroupCreation=" + notAllowNonAdminSubGroupCreation
                    + ", groupHistory=" + groupHistory
                    + ", noGroupHistory=" + noGroupHistory
                    + ']';
        }
    }

    /**
     * Represents the reply variant emitted when the relay rejected the request envelope as malformed (conflicting
     * toggles, empty payload), unauthorised, or referencing a non-existent group.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsSetPropertyResponseClientError")
    final class ClientError implements SmaxGroupsSetPropertyResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsSetPropertyResponseClientError",
                exports = "parseSetPropertyResponseClientError",
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
            return "SmaxGroupsSetPropertyResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Represents the reply variant emitted on transient relay-side failure.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsSetPropertyResponseServerError")
    final class ServerError implements SmaxGroupsSetPropertyResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsSetPropertyResponseServerError",
                exports = "parseSetPropertyResponseServerError",
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
            return "SmaxGroupsSetPropertyResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

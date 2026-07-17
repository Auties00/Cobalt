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
 * The sealed reply family for a {@link SmaxGroupsLinkSubGroupsRequest}.
 *
 * <p>The three variants split the relay's response into distinct cases: {@link Success} carries per-group result
 * rows where each row records whether the link succeeded plus any participants that could not be transferred, and
 * {@link ClientError}/{@link ServerError} surface the relay's reason codes.
 */
public sealed interface SmaxGroupsLinkSubGroupsResponse extends SmaxStanza.Response
        permits SmaxGroupsLinkSubGroupsResponse.Success, SmaxGroupsLinkSubGroupsResponse.ClientError, SmaxGroupsLinkSubGroupsResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsLinkSubGroupsResponse} variant in priority order and
     * returns the first that parses cleanly.
     *
     * <p>Tries {@link Success} first, then {@link ClientError}, then {@link ServerError}.
     *
     * @implNote The empty {@link Optional} surfaces when the stanza shape matches none of the three documented
     * variants; WA Web throws {@code SmaxParsingFailure} on the same path, but Cobalt defers the decision to the
     * caller so it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsLinkSubGroupsRequest} stanza, used to validate echoed ids
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsLinkSubGroupsRPC",
            exports = "sendLinkSubGroupsRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsLinkSubGroupsResponse> of(Stanza stanza, Stanza request) {
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
     * The reply variant emitted when the relay processed every link request and returned a per-group result row.
     *
     * <p>A row with a non-empty {@link LinkedGroup#participantErrors()} list indicates participants whose privacy
     * settings forbade the implicit transfer into the sub-group.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsLinkSubGroupsResponseSuccess")
    final class Success implements SmaxGroupsLinkSubGroupsResponse {
        /**
         * The per-group result rows.
         */
        private final List<LinkedGroup> linkedGroups;

        /**
         * Constructs a {@link Success} reply.
         *
         * @param linkedGroups the per-group result rows; never {@code null}
         * @throws NullPointerException if {@code linkedGroups} is {@code null}
         */
        public Success(List<LinkedGroup> linkedGroups) {
            Objects.requireNonNull(linkedGroups, "linkedGroups cannot be null");
            this.linkedGroups = List.copyOf(linkedGroups);
        }

        /**
         * Returns the per-group result rows.
         *
         * @return an unmodifiable list of {@link LinkedGroup}; never {@code null}
         */
        public List<LinkedGroup> linkedGroups() {
            return linkedGroups;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         *
         * <p>Delegates to {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} for envelope validation, then
         * matches the {@code <links><link link_type="sub_group">...</link></links>} payload. Returns empty when any
         * {@code <participant/>} entry is missing the {@code jid} or {@code error} attribute.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsLinkSubGroupsResponseSuccess",
                exports = "parseLinkSubGroupsResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var links = stanza.getChild("links").orElse(null);
            if (links == null) {
                return Optional.empty();
            }
            var link = links.getChild("link").orElse(null);
            if (link == null) {
                return Optional.empty();
            }
            if (!link.hasAttribute("link_type", "sub_group")) {
                return Optional.empty();
            }
            var linkedGroups = new ArrayList<LinkedGroup>();
            for (var groupNode : link.getChildren("group")) {
                var jid = groupNode.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var hiddenGroup = groupNode.getChild("hidden_group").isPresent();
                var participantErrors = new ArrayList<LinkedGroup.ParticipantError>();
                for (var participantNode : groupNode.getChildren("participant")) {
                    var participantJid = participantNode.getAttributeAsJid("jid").orElse(null);
                    if (participantJid == null) {
                        return Optional.empty();
                    }
                    var error = participantNode.getAttributeAsString("error").orElse(null);
                    if (error == null) {
                        return Optional.empty();
                    }
                    participantErrors.add(new LinkedGroup.ParticipantError(participantJid, error));
                }
                linkedGroups.add(new LinkedGroup(jid, hiddenGroup, participantErrors));
            }
            if (linkedGroups.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Success(linkedGroups));
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
            return Objects.equals(this.linkedGroups, that.linkedGroups);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(linkedGroups);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsLinkSubGroupsResponse.Success[linkedGroups=" + linkedGroups + ']';
        }

        /**
         * Per-group result row inside a {@link Success}.
         *
         * <p>The {@link #participantErrors()} list is non-empty only when one or more participants could not be
         * transferred into the sub-group.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsLinkSubGroupsResponseSuccess")
        public static final class LinkedGroup {
            /**
             * The sub-group {@link Jid} echoed by the relay.
             */
            private final Jid jid;

            /**
             * Whether the relay echoed a {@code <hidden_group/>} marker for this sub-group.
             */
            private final boolean hiddenGroup;

            /**
             * Per-participant errors encountered while transferring the sub-group's roster.
             */
            private final List<ParticipantError> participantErrors;

            /**
             * Constructs a {@link LinkedGroup} result row.
             *
             * @param jid               the sub-group {@link Jid}; never {@code null}
             * @param hiddenGroup       whether the relay echoed the hidden marker
             * @param participantErrors the per-participant errors; never {@code null}
             * @throws NullPointerException if {@code jid} or {@code participantErrors} is {@code null}
             */
            public LinkedGroup(Jid jid, boolean hiddenGroup, List<ParticipantError> participantErrors) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.hiddenGroup = hiddenGroup;
                Objects.requireNonNull(participantErrors, "participantErrors cannot be null");
                this.participantErrors = List.copyOf(participantErrors);
            }

            /**
             * Returns the sub-group {@link Jid}.
             *
             * @return the sub-group {@link Jid}; never {@code null}
             */
            public Jid jid() {
                return jid;
            }

            /**
             * Returns whether the relay echoed the hidden-group marker.
             *
             * @return {@code true} when the {@code <hidden_group/>} marker is present
             */
            public boolean hiddenGroup() {
                return hiddenGroup;
            }

            /**
             * Returns the per-participant error rows.
             *
             * @return an unmodifiable list of {@link ParticipantError}; never {@code null}
             */
            public List<ParticipantError> participantErrors() {
                return participantErrors;
            }

            /**
             * Compares this row to {@code obj} for value equality across every field.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is a {@link LinkedGroup} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (LinkedGroup) obj;
                return this.hiddenGroup == that.hiddenGroup
                        && Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.participantErrors, that.participantErrors);
            }

            /**
             * Returns a hash composed of every field.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, hiddenGroup, participantErrors);
            }

            /**
             * Returns a debug string carrying every field.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsLinkSubGroupsResponse.Success.LinkedGroup[jid=" + jid
                        + ", hiddenGroup=" + hiddenGroup
                        + ", participantErrors=" + participantErrors + ']';
            }

            /**
             * Per-participant error row inside a {@link LinkedGroup}.
             *
             * <p>The {@link #error()} attribute is always {@code "403"} in current relay schemas: it indicates the
             * participant could not be transferred to the sub-group because their privacy settings forbid the
             * implicit community-driven group add.
             */
            @WhatsAppWebModule(moduleName = "WASmaxInGroupsLinkSubGroupsResponseSuccess")
            public static final class ParticipantError {
                /**
                 * The participant {@link Jid} that could not be transferred.
                 */
                private final Jid participantJid;

                /**
                 * The error code reported by the relay (always {@code "403"} in current schemas).
                 */
                private final String error;

                /**
                 * Constructs a {@link ParticipantError} entry.
                 *
                 * @param participantJid the participant {@link Jid}; never {@code null}
                 * @param error          the error code; never {@code null}
                 * @throws NullPointerException if either argument is {@code null}
                 */
                public ParticipantError(Jid participantJid, String error) {
                    this.participantJid = Objects.requireNonNull(participantJid, "participantJid cannot be null");
                    this.error = Objects.requireNonNull(error, "error cannot be null");
                }

                /**
                 * Returns the participant {@link Jid}.
                 *
                 * @return the participant {@link Jid}; never {@code null}
                 */
                public Jid participantJid() {
                    return participantJid;
                }

                /**
                 * Returns the relay-reported error code.
                 *
                 * @return the error code; never {@code null}
                 */
                public String error() {
                    return error;
                }

                /**
                 * Compares this row to {@code obj} for value equality across both fields.
                 *
                 * @param obj the other object
                 * @return {@code true} when {@code obj} is a {@link ParticipantError} with identical fields
                 */
                @Override
                public boolean equals(Object obj) {
                    if (obj == this) {
                        return true;
                    }
                    if (obj == null || obj.getClass() != this.getClass()) {
                        return false;
                    }
                    var that = (ParticipantError) obj;
                    return Objects.equals(this.participantJid, that.participantJid)
                            && Objects.equals(this.error, that.error);
                }

                /**
                 * Returns a hash composed of both fields.
                 *
                 * @return the hash code
                 */
                @Override
                public int hashCode() {
                    return Objects.hash(participantJid, error);
                }

                /**
                 * Returns a debug string carrying both fields.
                 *
                 * @return the debug representation
                 */
                @Override
                public String toString() {
                    return "SmaxGroupsLinkSubGroupsResponse.Success.LinkedGroup.ParticipantError[participantJid=" + participantJid
                            + ", error=" + error + ']';
                }
            }
        }
    }

    /**
     * The reply variant emitted when the relay rejected the link batch as malformed, unauthorised, or referencing a
     * non-existent parent or sub-group pair.
     *
     * <p>WA Web logs the {@link #errorCode()} as the HTTP-style status passed back to the community admin "Manage
     * groups" UI.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsLinkSubGroupsResponseClientError")
    final class ClientError implements SmaxGroupsLinkSubGroupsResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsLinkSubGroupsResponseClientError",
                exports = "parseLinkSubGroupsResponseClientError",
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
            return "SmaxGroupsLinkSubGroupsResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The reply variant emitted on transient relay-side failure.
     *
     * <p>Logged at the same severity as {@link ClientError} but typically signals retry-eligible relay outages
     * rather than caller error.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsLinkSubGroupsResponseServerError")
    final class ServerError implements SmaxGroupsLinkSubGroupsResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsLinkSubGroupsResponseServerError",
                exports = "parseLinkSubGroupsResponseServerError",
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
            return "SmaxGroupsLinkSubGroupsResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

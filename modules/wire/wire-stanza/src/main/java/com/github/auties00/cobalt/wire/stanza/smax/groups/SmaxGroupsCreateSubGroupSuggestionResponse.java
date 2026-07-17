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
 * Models the inbound reply to a {@link SmaxGroupsCreateSubGroupSuggestionRequest} as a sealed variant family.
 *
 * <p>{@link NewGroupSuggestionSuccess} carries the provisional sub-group metadata when the new-group branch was
 * used, {@link ExistingGroupsSuggestionSuccess} carries the per-candidate verdict rows for the existing-groups
 * branch, and {@link ClientError} and {@link ServerError} surface caller-side and relay-side failures. Callers
 * pattern-match the variant returned by {@link #of(Stanza, Stanza)}.
 */
public sealed interface SmaxGroupsCreateSubGroupSuggestionResponse extends SmaxStanza.Response
        permits SmaxGroupsCreateSubGroupSuggestionResponse.NewGroupSuggestionSuccess,
                SmaxGroupsCreateSubGroupSuggestionResponse.ExistingGroupsSuggestionSuccess,
                SmaxGroupsCreateSubGroupSuggestionResponse.ClientError,
                SmaxGroupsCreateSubGroupSuggestionResponse.ServerError {

    /**
     * Parses the inbound IQ stanza into the first matching variant.
     *
     * <p>The probes run in priority order: {@link NewGroupSuggestionSuccess},
     * {@link ExistingGroupsSuggestionSuccess}, {@link ClientError}, then {@link ServerError}. An empty
     * {@link Optional} signals a stanza shape outside the documented union.
     *
     * @implNote
     * This implementation does not throw a parsing-failure exception, leaving the recovery decision to the
     * caller.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound {@link SmaxGroupsCreateSubGroupSuggestionRequest} stanza, used to
     *                validate the echoed {@code id} attribute; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented
     *         variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsCreateSubGroupSuggestionRPC",
            exports = "sendCreateSubGroupSuggestionRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsCreateSubGroupSuggestionResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var newGroupSuccess = NewGroupSuggestionSuccess.of(stanza, request);
        if (newGroupSuccess.isPresent()) {
            return newGroupSuccess;
        }
        var existingGroupsSuccess = ExistingGroupsSuggestionSuccess.of(stanza, request);
        if (existingGroupsSuccess.isPresent()) {
            return existingGroupsSuccess;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Reports that the relay accepted a {@link SmaxGroupsCreateSubGroupSuggestionSuggestion.NewGroup}
     * suggestion and provisioned the sub-group.
     *
     * <p>Carries the provisional identity triple ({@link #subGroupSuggestionJid()},
     * {@link #subGroupSuggestionCreator()}, {@link #subGroupSuggestionCreation()}), the optional creator
     * phone-number JID for LID-addressed creators, and an optional description-error marker for callers that
     * need to surface partial-acceptance UI when the description body was rejected even though the sub-group
     * was created.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsCreateSubGroupSuggestionResponseNewGroupSuggestionSuccess")
    final class NewGroupSuggestionSuccess implements SmaxGroupsCreateSubGroupSuggestionResponse {
        /**
         * Holds the provisional sub-group {@link Jid} echoed by the relay.
         */
        private final Jid subGroupSuggestionJid;

        /**
         * Holds the creator {@link Jid} of the sub-group suggestion.
         */
        private final Jid subGroupSuggestionCreator;

        /**
         * Holds the creation timestamp in seconds since epoch.
         */
        private final long subGroupSuggestionCreation;

        /**
         * Holds the optional creator phone-number {@link Jid} echoed by the relay for LID-addressed creators;
         * {@code null} when omitted.
         */
        private final Jid subGroupSuggestionCreatorPn;

        /**
         * Holds the optional {@code error} attribute on the inner {@code <description/>} child; surfaces parse
         * failures (for example {@code "406"}) when the relay could not accept the description verbatim;
         * {@code null} when the description committed cleanly.
         */
        private final String subGroupSuggestionDescriptionError;

        /**
         * Constructs a new-group success variant.
         *
         * <p>Production instances are typically produced by {@link #of(Stanza, Stanza)}; direct construction seeds
         * test fixtures.
         *
         * @param subGroupSuggestionJid              the sub-group JID; never {@code null}
         * @param subGroupSuggestionCreator          the creator JID; never {@code null}
         * @param subGroupSuggestionCreation         the creation timestamp
         * @param subGroupSuggestionCreatorPn        the optional creator phone JID; may be {@code null}
         * @param subGroupSuggestionDescriptionError the optional description-error string; may be {@code null}
         * @throws NullPointerException if {@code subGroupSuggestionJid} or {@code subGroupSuggestionCreator}
         *                              is {@code null}
         */
        public NewGroupSuggestionSuccess(Jid subGroupSuggestionJid,
                                         Jid subGroupSuggestionCreator,
                                         long subGroupSuggestionCreation,
                                         Jid subGroupSuggestionCreatorPn,
                                         String subGroupSuggestionDescriptionError) {
            this.subGroupSuggestionJid = Objects.requireNonNull(subGroupSuggestionJid, "subGroupSuggestionJid cannot be null");
            this.subGroupSuggestionCreator = Objects.requireNonNull(subGroupSuggestionCreator, "subGroupSuggestionCreator cannot be null");
            this.subGroupSuggestionCreation = subGroupSuggestionCreation;
            this.subGroupSuggestionCreatorPn = subGroupSuggestionCreatorPn;
            this.subGroupSuggestionDescriptionError = subGroupSuggestionDescriptionError;
        }

        /**
         * Returns the provisional sub-group {@link Jid}.
         *
         * @return the sub-group JID; never {@code null}
         */
        public Jid subGroupSuggestionJid() {
            return subGroupSuggestionJid;
        }

        /**
         * Returns the creator {@link Jid}.
         *
         * @return the creator JID; never {@code null}
         */
        public Jid subGroupSuggestionCreator() {
            return subGroupSuggestionCreator;
        }

        /**
         * Returns the creation timestamp.
         *
         * @return the creation timestamp in seconds since epoch
         */
        public long subGroupSuggestionCreation() {
            return subGroupSuggestionCreation;
        }

        /**
         * Returns the optional creator phone-number {@link Jid}.
         *
         * @return an {@link Optional} carrying the creator phone JID, or empty when the relay omitted it
         */
        public Optional<Jid> subGroupSuggestionCreatorPn() {
            return Optional.ofNullable(subGroupSuggestionCreatorPn);
        }

        /**
         * Returns the optional description-error string.
         *
         * <p>A non-empty value means the relay accepted the sub-group but rejected the description body, so
         * callers should surface a partial-acceptance UI rather than treating the whole operation as failed.
         *
         * @return an {@link Optional} carrying the error string, or empty when the relay accepted the
         *         description
         */
        public Optional<String> subGroupSuggestionDescriptionError() {
            return Optional.ofNullable(subGroupSuggestionDescriptionError);
        }

        /**
         * Parses the inbound stanza into a {@link NewGroupSuggestionSuccess} variant.
         *
         * <p>Runs as the first probe in the variant cascade of
         * {@link SmaxGroupsCreateSubGroupSuggestionResponse#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation validates the IQ envelope via
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, then extracts the
         * {@code <sub_group_suggestion/>} child and reads the {@code jid}, {@code creator}, {@code creation},
         * and optional {@code creator_pn} attributes plus the optional inner {@code <description error="...">}
         * marker.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the
         *         stanza does not match the new-group-success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsCreateSubGroupSuggestionResponseNewGroupSuggestionSuccess",
                exports = "parseCreateSubGroupSuggestionResponseNewGroupSuggestionSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<NewGroupSuggestionSuccess> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var suggestion = stanza.getChild("sub_group_suggestion").orElse(null);
            if (suggestion == null) {
                return Optional.empty();
            }
            var jid = suggestion.getAttributeAsJid("jid").orElse(null);
            if (jid == null) {
                return Optional.empty();
            }
            var creator = suggestion.getAttributeAsJid("creator").orElse(null);
            if (creator == null) {
                return Optional.empty();
            }
            if (suggestion.getAttributeAsLong("creation").isEmpty()) {
                return Optional.empty();
            }
            var creation = suggestion.getAttributeAsLong("creation").getAsLong();
            var creatorPn = suggestion.getAttributeAsJid("creator_pn").orElse(null);
            String descriptionError = null;
            var description = suggestion.getChild("description").orElse(null);
            if (description != null) {
                descriptionError = description.getAttributeAsString("error").orElse(null);
            }
            return Optional.of(new NewGroupSuggestionSuccess(jid, creator, creation, creatorPn, descriptionError));
        }

        /**
         * Compares this variant to {@code obj} for value equality across every field.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link NewGroupSuggestionSuccess} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (NewGroupSuggestionSuccess) obj;
            return this.subGroupSuggestionCreation == that.subGroupSuggestionCreation
                    && Objects.equals(this.subGroupSuggestionJid, that.subGroupSuggestionJid)
                    && Objects.equals(this.subGroupSuggestionCreator, that.subGroupSuggestionCreator)
                    && Objects.equals(this.subGroupSuggestionCreatorPn, that.subGroupSuggestionCreatorPn)
                    && Objects.equals(this.subGroupSuggestionDescriptionError, that.subGroupSuggestionDescriptionError);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(subGroupSuggestionJid, subGroupSuggestionCreator, subGroupSuggestionCreation,
                    subGroupSuggestionCreatorPn, subGroupSuggestionDescriptionError);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsCreateSubGroupSuggestionResponse.NewGroupSuggestionSuccess[subGroupSuggestionJid=" + subGroupSuggestionJid
                    + ", subGroupSuggestionCreator=" + subGroupSuggestionCreator
                    + ", subGroupSuggestionCreation=" + subGroupSuggestionCreation
                    + ", subGroupSuggestionCreatorPn=" + subGroupSuggestionCreatorPn
                    + ", subGroupSuggestionDescriptionError=" + subGroupSuggestionDescriptionError + ']';
        }
    }

    /**
     * Reports that the relay accepted, or partially accepted, a
     * {@link SmaxGroupsCreateSubGroupSuggestionSuggestion.ExistingGroups} suggestion.
     *
     * <p>Carries one {@link Candidate} row per requested group; each row's {@link Candidate#errorTag()}
     * signals whether that specific candidate was admitted or rejected and, when rejected, why.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsCreateSubGroupSuggestionResponseExistingGroupsSuggestionSuccess")
    final class ExistingGroupsSuggestionSuccess implements SmaxGroupsCreateSubGroupSuggestionResponse {
        /**
         * Holds the per-candidate result rows, one per requested group.
         */
        private final List<Candidate> candidates;

        /**
         * Constructs an existing-groups success variant.
         *
         * <p>Production instances are typically produced by {@link #of(Stanza, Stanza)}; direct construction seeds
         * test fixtures.
         *
         * @param candidates the per-group result rows; never {@code null}
         * @throws NullPointerException if {@code candidates} is {@code null}
         */
        public ExistingGroupsSuggestionSuccess(List<Candidate> candidates) {
            Objects.requireNonNull(candidates, "candidates cannot be null");
            this.candidates = List.copyOf(candidates);
        }

        /**
         * Returns the per-candidate result rows.
         *
         * @return an unmodifiable list of result rows
         */
        public List<Candidate> candidates() {
            return candidates;
        }

        /**
         * Parses the inbound stanza into an {@link ExistingGroupsSuggestionSuccess} variant.
         *
         * <p>Runs as the second probe in the variant cascade of
         * {@link SmaxGroupsCreateSubGroupSuggestionResponse#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation validates the IQ envelope via
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, iterates the {@code <group/>} children of the
         * inner {@code <sub_group_suggestion/>} stanza, and walks each child's child list looking for the first
         * recognised error-discriminator tag through {@link Candidate#isErrorTag(String)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the
         *         stanza does not match the existing-groups-success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsCreateSubGroupSuggestionResponseExistingGroupsSuggestionSuccess",
                exports = "parseCreateSubGroupSuggestionResponseExistingGroupsSuggestionSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ExistingGroupsSuggestionSuccess> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var suggestion = stanza.getChild("sub_group_suggestion").orElse(null);
            if (suggestion == null) {
                return Optional.empty();
            }
            var groups = suggestion.getChildren("group");
            if (groups.isEmpty()) {
                return Optional.empty();
            }
            var candidates = new ArrayList<Candidate>();
            for (var groupNode : groups) {
                var jid = groupNode.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                String errorTag = null;
                for (var child : groupNode.children()) {
                    var description = child.description();
                    if (description == null) {
                        continue;
                    }
                    if (Candidate.isErrorTag(description)) {
                        errorTag = description;
                        break;
                    }
                }
                candidates.add(new Candidate(jid, errorTag));
            }
            return Optional.of(new ExistingGroupsSuggestionSuccess(candidates));
        }

        /**
         * Compares this variant to {@code obj} for value equality across every field.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is an {@link ExistingGroupsSuggestionSuccess} with identical
         *         candidate rows
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ExistingGroupsSuggestionSuccess) obj;
            return Objects.equals(this.candidates, that.candidates);
        }

        /**
         * Returns a hash composed of the candidate rows.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(candidates);
        }

        /**
         * Returns a debug string carrying the candidate rows.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsCreateSubGroupSuggestionResponse.ExistingGroupsSuggestionSuccess[candidates=" + candidates + ']';
        }

        /**
         * Records the relay's verdict for a single candidate group inside an
         * {@link ExistingGroupsSuggestionSuccess}.
         *
         * <p>The optional {@link #errorTag()} captures the discriminator tag emitted by the relay when a single
         * candidate is rejected; absence signals the candidate was admitted cleanly. The recognised tags are
         * {@code "not_authorized"}, {@code "not_exist"}, {@code "conflict"}, {@code "suggestion_not_allowed"},
         * {@code "resource_limit"}, {@code "bad_request"}, {@code "not_acceptable"}, and {@code "server_error"}.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsCreateSubGroupSuggestionResponseExistingGroupsSuggestionSuccess")
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsSubGroupNotAuthorizedOrNotExistOrConflictOrSuggestionNotAllowedOrResourceLimitOrBadRequestOrNotAcceptableOrServerErrorMixinGroup")
        public static final class Candidate {
            /**
             * Returns whether {@code description} is one of the documented sub-group error discriminator tags.
             *
             * <p>Called by {@link ExistingGroupsSuggestionSuccess#of(Stanza, Stanza)} to single out the first
             * recognised error marker child while ignoring other metadata children.
             *
             * @param description the child tag to test
             * @return {@code true} when the tag is a recognised error tag
             */
            private static boolean isErrorTag(String description) {
                return "not_authorized".equals(description)
                        || "not_exist".equals(description)
                        || "conflict".equals(description)
                        || "suggestion_not_allowed".equals(description)
                        || "resource_limit".equals(description)
                        || "bad_request".equals(description)
                        || "not_acceptable".equals(description)
                        || "server_error".equals(description);
            }

            /**
             * Holds the candidate sub-group {@link Jid} echoed by the relay.
             */
            private final Jid jid;

            /**
             * Holds the optional error-discriminator tag emitted as a child of the {@code <group/>} entry;
             * {@code null} when the candidate was admitted cleanly.
             */
            private final String errorTag;

            /**
             * Constructs a candidate result row.
             *
             * <p>Production instances are typically produced by
             * {@link ExistingGroupsSuggestionSuccess#of(Stanza, Stanza)}; direct construction seeds test fixtures.
             *
             * @param jid      the candidate JID; never {@code null}
             * @param errorTag the optional error-discriminator tag; may be {@code null}
             * @throws NullPointerException if {@code jid} is {@code null}
             */
            public Candidate(Jid jid, String errorTag) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.errorTag = errorTag;
            }

            /**
             * Returns the candidate {@link Jid}.
             *
             * @return the candidate JID; never {@code null}
             */
            public Jid jid() {
                return jid;
            }

            /**
             * Returns the optional error-discriminator tag.
             *
             * <p>Empty means the candidate was admitted; otherwise the value is one of the eight tags listed on
             * the enclosing class.
             *
             * @return an {@link Optional} carrying the tag, or empty when the candidate was admitted cleanly
             */
            public Optional<String> errorTag() {
                return Optional.ofNullable(errorTag);
            }

            /**
             * Compares this row to {@code obj} for value equality across both fields.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is a {@link Candidate} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Candidate) obj;
                return Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.errorTag, that.errorTag);
            }

            /**
             * Returns a hash composed of both fields.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, errorTag);
            }

            /**
             * Returns a debug string carrying both fields.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsCreateSubGroupSuggestionResponse.ExistingGroupsSuggestionSuccess.Candidate[jid=" + jid
                        + ", errorTag=" + errorTag + ']';
            }
        }
    }

    /**
     * Reports that the relay rejected the request as malformed, unauthorised, or referencing an inadmissible
     * community.
     *
     * <p>Carries the envelope-level {@link #errorCode()} and {@link #errorText()}; per-candidate rejection
     * markers live on {@link ExistingGroupsSuggestionSuccess.Candidate#errorTag()}, not on this error.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsCreateSubGroupSuggestionResponseClientError")
    final class ClientError implements SmaxGroupsCreateSubGroupSuggestionResponse {
        /**
         * Holds the numeric server-side error code, mirroring the {@code <error code="...">} attribute on the
         * inbound stanza.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text echoed by the relay; {@code null} when the relay omitted the
         * {@code <error text="...">} attribute.
         */
        private final String errorText;

        /**
         * Constructs a client-error variant.
         *
         * <p>Production instances are typically produced by {@link #of(Stanza, Stanza)}; direct construction seeds
         * test fixtures.
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
         * Parses the inbound stanza into a {@link ClientError} envelope.
         *
         * <p>Runs as the third probe in the variant cascade of
         * {@link SmaxGroupsCreateSubGroupSuggestionResponse#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation delegates the error-envelope extraction to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} so every SMAX response in the family
         * shares the same client-error parsing.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the
         *         envelope does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsCreateSubGroupSuggestionResponseClientError",
                exports = "parseCreateSubGroupSuggestionResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to {@code obj} for value equality across both fields.
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
            return "SmaxGroupsCreateSubGroupSuggestionResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Reports that the relay encountered a transient internal failure.
     *
     * <p>Callers decide whether to retry based on the surfaced {@link #errorCode()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsCreateSubGroupSuggestionResponseServerError")
    final class ServerError implements SmaxGroupsCreateSubGroupSuggestionResponse {
        /**
         * Holds the numeric server-side error code, mirroring the {@code <error code="...">} attribute on the
         * inbound stanza.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text echoed by the relay; {@code null} when the relay omitted the
         * {@code <error text="...">} attribute.
         */
        private final String errorText;

        /**
         * Constructs a server-error variant.
         *
         * <p>Production instances are typically produced by {@link #of(Stanza, Stanza)}; direct construction seeds
         * test fixtures.
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
         * Parses the inbound stanza into a {@link ServerError} envelope.
         *
         * <p>Runs as the terminal probe in the variant cascade of
         * {@link SmaxGroupsCreateSubGroupSuggestionResponse#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation delegates the error-envelope extraction to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} so every SMAX response in the family
         * shares the same server-error parsing.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the
         *         envelope does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsCreateSubGroupSuggestionResponseServerError",
                exports = "parseCreateSubGroupSuggestionResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to {@code obj} for value equality across both fields.
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
            return "SmaxGroupsCreateSubGroupSuggestionResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

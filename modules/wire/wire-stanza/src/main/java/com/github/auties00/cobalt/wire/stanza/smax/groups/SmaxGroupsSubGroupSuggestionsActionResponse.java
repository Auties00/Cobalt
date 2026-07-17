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
 * Models the sealed reply family for a {@link SmaxGroupsSubGroupSuggestionsActionRequest}.
 *
 * <p>The three permitted variants are {@link Success}, {@link ClientError}, and {@link ServerError}.
 * {@link Success} aggregates per-suggestion echo rows for each sub-action, so callers can detect partial failures
 * (for example a suggestion the relay refused to approve) even when the envelope is successful.
 */
public sealed interface SmaxGroupsSubGroupSuggestionsActionResponse extends SmaxStanza.Response
        permits SmaxGroupsSubGroupSuggestionsActionResponse.Success, SmaxGroupsSubGroupSuggestionsActionResponse.ClientError, SmaxGroupsSubGroupSuggestionsActionResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsSubGroupSuggestionsActionResponse} variant in
     * priority order and returns the first that parses cleanly.
     *
     * <p>The variants are tried in the order {@link Success}, {@link ClientError}, {@link ServerError}.
     *
     * @implNote This implementation returns an empty {@link Optional} when the stanza shape matches none of the
     * variants; WA Web throws a parsing failure on the same path, but Cobalt defers the decision to the caller so
     * it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsSubGroupSuggestionsActionRequest} stanza, used to
     *                validate echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsSubGroupSuggestionsActionRPC",
            exports = "sendSubGroupSuggestionsActionRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsSubGroupSuggestionsActionResponse> of(Stanza stanza, Stanza request) {
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
     * Represents the reply variant carrying per-suggestion echo rows for each sub-action.
     *
     * <p>Approve rows expose the {@code (creator, jid, creator_pn?)} triple plus an optional approval-error
     * discriminator; reject and cancel rows additionally carry an optional identity tag and a not-found marker.
     * The envelope succeeds even when individual rows carry rejection or not-found markers, so callers must walk
     * each list to detect partial failures.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsSubGroupSuggestionsActionResponseSuccess")
    final class Success implements SmaxGroupsSubGroupSuggestionsActionResponse {
        /**
         * The approve sub-action echo rows.
         */
        private final List<ApprovedSuggestion> approve;

        /**
         * The reject sub-action echo rows.
         */
        private final List<RejectedSuggestion> reject;

        /**
         * The cancel sub-action echo rows.
         */
        private final List<CancelledSuggestion> cancel;

        /**
         * Constructs a {@link Success}.
         *
         * <p>The three lists are copied so post-construction mutation of the caller's lists has no effect on the
         * variant.
         *
         * @param approve the approve echo rows
         * @param reject  the reject echo rows
         * @param cancel  the cancel echo rows
         * @throws NullPointerException if any argument is {@code null}
         */
        public Success(List<ApprovedSuggestion> approve,
                       List<RejectedSuggestion> reject,
                       List<CancelledSuggestion> cancel) {
            Objects.requireNonNull(approve, "approve cannot be null");
            Objects.requireNonNull(reject, "reject cannot be null");
            Objects.requireNonNull(cancel, "cancel cannot be null");
            this.approve = List.copyOf(approve);
            this.reject = List.copyOf(reject);
            this.cancel = List.copyOf(cancel);
        }

        /**
         * Returns the approve echo rows.
         *
         * @return an unmodifiable list of {@link ApprovedSuggestion} entries; never {@code null}
         */
        public List<ApprovedSuggestion> approve() {
            return approve;
        }

        /**
         * Returns the reject echo rows.
         *
         * @return an unmodifiable list of {@link RejectedSuggestion} entries; never {@code null}
         */
        public List<RejectedSuggestion> reject() {
            return reject;
        }

        /**
         * Returns the cancel echo rows.
         *
         * @return an unmodifiable list of {@link CancelledSuggestion} entries; never {@code null}
         */
        public List<CancelledSuggestion> cancel() {
            return cancel;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         *
         * <p>The IQ must be a valid {@code type="result"} echo of {@code request}, validated through
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, must carry a
         * {@code <sub_group_suggestions_action>} child, and each optional {@code <approve>}, {@code <reject>},
         * and {@code <cancel>} grand-child's entries must satisfy the matching {@link ApprovedSuggestion#of(Stanza)},
         * {@link RejectedSuggestion#of(Stanza)}, and {@link CancelledSuggestion#of(Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsSubGroupSuggestionsActionResponseSuccess",
                exports = "parseSubGroupSuggestionsActionResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var actionNode = stanza.getChild("sub_group_suggestions_action").orElse(null);
            if (actionNode == null) {
                return Optional.empty();
            }
            var approveList = new ArrayList<ApprovedSuggestion>();
            var approveSection = actionNode.getChild("approve").orElse(null);
            if (approveSection != null) {
                for (var suggestion : approveSection.getChildren("sub_group_suggestion")) {
                    var parsed = ApprovedSuggestion.of(suggestion).orElse(null);
                    if (parsed == null) {
                        return Optional.empty();
                    }
                    approveList.add(parsed);
                }
            }
            var rejectList = new ArrayList<RejectedSuggestion>();
            var rejectSection = actionNode.getChild("reject").orElse(null);
            if (rejectSection != null) {
                for (var suggestion : rejectSection.getChildren("sub_group_suggestion")) {
                    var parsed = RejectedSuggestion.of(suggestion).orElse(null);
                    if (parsed == null) {
                        return Optional.empty();
                    }
                    rejectList.add(parsed);
                }
            }
            var cancelList = new ArrayList<CancelledSuggestion>();
            var cancelSection = actionNode.getChild("cancel").orElse(null);
            if (cancelSection != null) {
                for (var suggestion : cancelSection.getChildren("sub_group_suggestion")) {
                    var parsed = CancelledSuggestion.of(suggestion).orElse(null);
                    if (parsed == null) {
                        return Optional.empty();
                    }
                    cancelList.add(parsed);
                }
            }
            return Optional.of(new Success(approveList, rejectList, cancelList));
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
            return Objects.equals(this.approve, that.approve)
                    && Objects.equals(this.reject, that.reject)
                    && Objects.equals(this.cancel, that.cancel);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(approve, reject, cancel);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsSubGroupSuggestionsActionResponse.Success[approve=" + approve
                    + ", reject=" + reject
                    + ", cancel=" + cancel + ']';
        }

        /**
         * Represents an approve-list echo row carrying the {@code (creator, jid, creator_pn?)} triple plus an
         * optional approval-error discriminator tag.
         *
         * <p>{@link #approvalErrorTag()} is one of {@code "sub_group_creation_internal_server_error"},
         * {@code "pending_group_adds_error"}, {@code "resource_constraint"}, {@code "suggestion_conflict"}, or
         * {@code "suggestion_not_found"}, and is empty when the approval committed cleanly.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsSubGroupSuggestionMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsSubGroupSuggestionsApprovalErrors")
        public static final class ApprovedSuggestion {
            /**
             * Returns whether {@code description} matches one of the documented approval-error discriminator
             * tags.
             *
             * @param description the child tag to test
             * @return {@code true} when the tag is recognised as an approval-error discriminator
             */
            private static boolean isApprovalErrorTag(String description) {
                return "sub_group_creation_internal_server_error".equals(description)
                        || "pending_group_adds_error".equals(description)
                        || "resource_constraint".equals(description)
                        || "suggestion_conflict".equals(description)
                        || "suggestion_not_found".equals(description);
            }

            /**
             * The creator {@link Jid} echoed by the relay.
             */
            private final Jid creator;

            /**
             * The sub-group {@link Jid} echoed by the relay.
             */
            private final Jid jid;

            /**
             * The optional creator phone-number {@link Jid} echoed by the relay.
             */
            private final Jid creatorPn;

            /**
             * The optional approval-error discriminator tag.
             */
            private final String approvalErrorTag;

            /**
             * Constructs an {@link ApprovedSuggestion} echo row.
             *
             * @param creator          the creator {@link Jid}
             * @param jid              the sub-group {@link Jid}
             * @param creatorPn        the optional creator phone-number {@link Jid}; may be {@code null}
             * @param approvalErrorTag the optional approval-error tag; may be {@code null}
             * @throws NullPointerException if {@code creator} or {@code jid} is {@code null}
             */
            public ApprovedSuggestion(Jid creator, Jid jid, Jid creatorPn, String approvalErrorTag) {
                this.creator = Objects.requireNonNull(creator, "creator cannot be null");
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.creatorPn = creatorPn;
                this.approvalErrorTag = approvalErrorTag;
            }

            /**
             * Returns the creator {@link Jid}.
             *
             * @return the creator {@link Jid}; never {@code null}
             */
            public Jid creator() {
                return creator;
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
             * Returns the optional creator phone-number {@link Jid}.
             *
             * @return an {@link Optional} carrying the phone-number {@link Jid}, or empty when omitted
             */
            public Optional<Jid> creatorPn() {
                return Optional.ofNullable(creatorPn);
            }

            /**
             * Returns the optional approval-error tag.
             *
             * @return an {@link Optional} carrying the tag, or empty when the approval committed cleanly
             */
            public Optional<String> approvalErrorTag() {
                return Optional.ofNullable(approvalErrorTag);
            }

            /**
             * Tries to parse an {@link ApprovedSuggestion} echo row from a {@code <sub_group_suggestion/>} stanza.
             *
             * <p>The {@code creator} and {@code jid} attributes are mandatory; an empty {@link Optional} is
             * returned when either is absent. The first child whose tag matches an approval-error discriminator
             * becomes the {@link #approvalErrorTag()}.
             *
             * @param suggestion the {@code <sub_group_suggestion/>} stanza
             * @return an {@link Optional} carrying the parsed row, or empty when the stanza is malformed
             */
            static Optional<ApprovedSuggestion> of(Stanza suggestion) {
                var creator = suggestion.getAttributeAsJid("creator").orElse(null);
                if (creator == null) {
                    return Optional.empty();
                }
                var jid = suggestion.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var creatorPn = suggestion.getAttributeAsJid("creator_pn").orElse(null);
                String approvalErrorTag = null;
                for (var child : suggestion.children()) {
                    var description = child.description();
                    if (description == null) {
                        continue;
                    }
                    if (isApprovalErrorTag(description)) {
                        approvalErrorTag = description;
                        break;
                    }
                }
                return Optional.of(new ApprovedSuggestion(creator, jid, creatorPn, approvalErrorTag));
            }

            /**
             * Compares this row to {@code obj} for value equality across every field.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is an {@link ApprovedSuggestion} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (ApprovedSuggestion) obj;
                return Objects.equals(this.creator, that.creator)
                        && Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.creatorPn, that.creatorPn)
                        && Objects.equals(this.approvalErrorTag, that.approvalErrorTag);
            }

            /**
             * Returns a hash composed of every field.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(creator, jid, creatorPn, approvalErrorTag);
            }

            /**
             * Returns a debug string carrying every field.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsSubGroupSuggestionsActionResponse.Success.ApprovedSuggestion[creator=" + creator
                        + ", jid=" + jid
                        + ", creatorPn=" + creatorPn
                        + ", approvalErrorTag=" + approvalErrorTag + ']';
            }
        }

        /**
         * Represents a reject-list echo row carrying the {@code (creator, jid, creator_pn?)} triple plus an
         * optional identity tag and an optional not-found marker.
         *
         * <p>{@link #identityTag()} is the raw discriminator tag the WA Web parser routes through its identity
         * types; {@link #notFound()} mirrors the presence of an inner {@code <suggestion_not_found/>} child.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsSubGroupSuggestionMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsIdentityMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsSubGroupSuggestionsActionSubGroupSuggestionNotFoundMixin")
        public static final class RejectedSuggestion {
            /**
             * The creator {@link Jid} echoed by the relay.
             */
            private final Jid creator;

            /**
             * The sub-group {@link Jid} echoed by the relay.
             */
            private final Jid jid;

            /**
             * The optional creator phone-number {@link Jid} echoed by the relay.
             */
            private final Jid creatorPn;

            /**
             * The optional identity-mixin discriminator tag.
             */
            private final String identityTag;

            /**
             * Whether the relay marked the suggestion as not-found.
             */
            private final boolean notFound;

            /**
             * Constructs a {@link RejectedSuggestion} echo row.
             *
             * @param creator     the creator {@link Jid}
             * @param jid         the sub-group {@link Jid}
             * @param creatorPn   the optional creator phone-number {@link Jid}; may be {@code null}
             * @param identityTag the optional identity tag; may be {@code null}
             * @param notFound    whether the not-found marker is present
             * @throws NullPointerException if {@code creator} or {@code jid} is {@code null}
             */
            public RejectedSuggestion(Jid creator, Jid jid, Jid creatorPn, String identityTag, boolean notFound) {
                this.creator = Objects.requireNonNull(creator, "creator cannot be null");
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.creatorPn = creatorPn;
                this.identityTag = identityTag;
                this.notFound = notFound;
            }

            /**
             * Returns the creator {@link Jid}.
             *
             * @return the creator {@link Jid}; never {@code null}
             */
            public Jid creator() {
                return creator;
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
             * Returns the optional creator phone-number {@link Jid}.
             *
             * @return an {@link Optional} carrying the phone-number {@link Jid}, or empty when omitted
             */
            public Optional<Jid> creatorPn() {
                return Optional.ofNullable(creatorPn);
            }

            /**
             * Returns the optional identity-mixin discriminator tag.
             *
             * @return an {@link Optional} carrying the tag, or empty when omitted
             */
            public Optional<String> identityTag() {
                return Optional.ofNullable(identityTag);
            }

            /**
             * Returns whether the not-found marker is present.
             *
             * @return {@code true} when the relay surfaced the not-found marker
             */
            public boolean notFound() {
                return notFound;
            }

            /**
             * Tries to parse a {@link RejectedSuggestion} echo row from a {@code <sub_group_suggestion/>} stanza.
             *
             * <p>The {@code creator} and {@code jid} attributes are mandatory; an empty {@link Optional} is
             * returned when either is absent. The first non-{@code suggestion_not_found} child contributes the
             * {@link #identityTag()}.
             *
             * @param suggestion the {@code <sub_group_suggestion/>} stanza
             * @return an {@link Optional} carrying the parsed row, or empty when the stanza is malformed
             */
            static Optional<RejectedSuggestion> of(Stanza suggestion) {
                var creator = suggestion.getAttributeAsJid("creator").orElse(null);
                if (creator == null) {
                    return Optional.empty();
                }
                var jid = suggestion.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var creatorPn = suggestion.getAttributeAsJid("creator_pn").orElse(null);
                var notFound = suggestion.getChild("suggestion_not_found").isPresent();
                String identityTag = null;
                for (var child : suggestion.children()) {
                    var description = child.description();
                    if (description == null) {
                        continue;
                    }
                    if ("suggestion_not_found".equals(description)) {
                        continue;
                    }
                    identityTag = description;
                    break;
                }
                return Optional.of(new RejectedSuggestion(creator, jid, creatorPn, identityTag, notFound));
            }

            /**
             * Compares this row to {@code obj} for value equality across every field.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is a {@link RejectedSuggestion} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (RejectedSuggestion) obj;
                return this.notFound == that.notFound
                        && Objects.equals(this.creator, that.creator)
                        && Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.creatorPn, that.creatorPn)
                        && Objects.equals(this.identityTag, that.identityTag);
            }

            /**
             * Returns a hash composed of every field.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(creator, jid, creatorPn, identityTag, notFound);
            }

            /**
             * Returns a debug string carrying every field.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsSubGroupSuggestionsActionResponse.Success.RejectedSuggestion[creator=" + creator
                        + ", jid=" + jid
                        + ", creatorPn=" + creatorPn
                        + ", identityTag=" + identityTag
                        + ", notFound=" + notFound + ']';
            }
        }

        /**
         * Represents a cancel-list echo row carrying only the {@code jid} plus an optional identity tag and a
         * not-found marker.
         *
         * <p>Cancel rows omit the {@code creator} attribute by parity with the request side; the cancelling
         * caller is implicit and the relay enforces ownership server-side.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsSubGroupSuggestionWithoutCreatorMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsIdentityMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsSubGroupSuggestionsActionSubGroupSuggestionNotFoundMixin")
        public static final class CancelledSuggestion {
            /**
             * The sub-group {@link Jid} echoed by the relay.
             */
            private final Jid jid;

            /**
             * The optional identity-mixin discriminator tag.
             */
            private final String identityTag;

            /**
             * Whether the relay marked the suggestion as not-found.
             */
            private final boolean notFound;

            /**
             * Constructs a {@link CancelledSuggestion} echo row.
             *
             * @param jid         the sub-group {@link Jid}
             * @param identityTag the optional identity tag; may be {@code null}
             * @param notFound    whether the not-found marker is present
             * @throws NullPointerException if {@code jid} is {@code null}
             */
            public CancelledSuggestion(Jid jid, String identityTag, boolean notFound) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.identityTag = identityTag;
                this.notFound = notFound;
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
             * Returns the optional identity-mixin discriminator tag.
             *
             * @return an {@link Optional} carrying the tag, or empty when omitted
             */
            public Optional<String> identityTag() {
                return Optional.ofNullable(identityTag);
            }

            /**
             * Returns whether the not-found marker is present.
             *
             * @return {@code true} when the relay surfaced the not-found marker
             */
            public boolean notFound() {
                return notFound;
            }

            /**
             * Tries to parse a {@link CancelledSuggestion} echo row from a {@code <sub_group_suggestion/>} stanza.
             *
             * <p>The {@code jid} attribute is mandatory; an empty {@link Optional} is returned when absent. The
             * first non-{@code suggestion_not_found} child contributes the {@link #identityTag()}.
             *
             * @param suggestion the {@code <sub_group_suggestion/>} stanza
             * @return an {@link Optional} carrying the parsed row, or empty when the stanza is malformed
             */
            static Optional<CancelledSuggestion> of(Stanza suggestion) {
                var jid = suggestion.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var notFound = suggestion.getChild("suggestion_not_found").isPresent();
                String identityTag = null;
                for (var child : suggestion.children()) {
                    var description = child.description();
                    if (description == null) {
                        continue;
                    }
                    if ("suggestion_not_found".equals(description)) {
                        continue;
                    }
                    identityTag = description;
                    break;
                }
                return Optional.of(new CancelledSuggestion(jid, identityTag, notFound));
            }

            /**
             * Compares this row to {@code obj} for value equality across every field.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is a {@link CancelledSuggestion} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (CancelledSuggestion) obj;
                return this.notFound == that.notFound
                        && Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.identityTag, that.identityTag);
            }

            /**
             * Returns a hash composed of every field.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, identityTag, notFound);
            }

            /**
             * Returns a debug string carrying every field.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsSubGroupSuggestionsActionResponse.Success.CancelledSuggestion[jid=" + jid
                        + ", identityTag=" + identityTag
                        + ", notFound=" + notFound + ']';
            }
        }
    }

    /**
     * Represents the reply variant emitted when the relay rejected the request envelope as malformed,
     * unauthorised, or referencing non-existent suggestions.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsSubGroupSuggestionsActionResponseClientError")
    final class ClientError implements SmaxGroupsSubGroupSuggestionsActionResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsSubGroupSuggestionsActionResponseClientError",
                exports = "parseSubGroupSuggestionsActionResponseClientError",
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
            return "SmaxGroupsSubGroupSuggestionsActionResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Represents the reply variant emitted on transient relay-side failure.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsSubGroupSuggestionsActionResponseServerError")
    final class ServerError implements SmaxGroupsSubGroupSuggestionsActionResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsSubGroupSuggestionsActionResponseServerError",
                exports = "parseSubGroupSuggestionsActionResponseServerError",
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
            return "SmaxGroupsSubGroupSuggestionsActionResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

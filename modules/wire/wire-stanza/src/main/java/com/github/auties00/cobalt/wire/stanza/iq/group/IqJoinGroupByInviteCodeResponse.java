package com.github.auties00.cobalt.wire.stanza.iq.group;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed family of inbound reply variants the relay produces in response to an {@link IqJoinGroupByInviteCodeRequest}.
 *
 * <p>After dispatching the join request, callers pattern-match the returned variant.
 * {@link Success} carries the group JID and a flag indicating whether the caller was admitted
 * immediately or queued for moderator approval; {@link UnexpectedJoinShape} surfaces a mismatch
 * between the caller's expected gating mode and the relay's actual reply shape; and
 * {@link ClientError} / {@link ServerError} surface envelope-level rejections, typically
 * {@code 401} for an expired or revoked code, {@code 403} for a banned caller, or {@code 404} for
 * a deleted group.
 *
 * @implNote
 * This implementation collapses the join parser's return shape plus the unexpected-shape error it
 * would otherwise throw into a single sealed sum, so callers pattern-match a closed set of variants
 * instead of catching a sibling exception.
 */
@WhatsAppWebModule(moduleName = "WAWebGroupInviteJob")
public sealed interface IqJoinGroupByInviteCodeResponse extends IqStanza.Response
        permits IqJoinGroupByInviteCodeResponse.Success, IqJoinGroupByInviteCodeResponse.UnexpectedJoinShape,
                IqJoinGroupByInviteCodeResponse.ClientError, IqJoinGroupByInviteCodeResponse.ServerError {

    /**
     * Tries each {@link IqJoinGroupByInviteCodeResponse} variant in priority order and returns the first that parses cleanly.
     *
     * <p>The dispatcher hands the inbound {@link Stanza} together with the original outbound request
     * so that the parser can correlate echoed identifiers and approval-mode expectations. Returns
     * {@link Optional#empty()} when none of the documented shapes match, which the caller should
     * treat as an unknown server reply and surface up.
     *
     * @implNote
     * This implementation tries {@link Success} first, then {@link UnexpectedJoinShape}, then
     * {@link ClientError}, then {@link ServerError}, asserting the result branch before any error
     * envelope is inspected.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza used to validate echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupInviteJob",
            exports = "joinGroupViaInvite",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqJoinGroupByInviteCodeResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var unexpected = UnexpectedJoinShape.of(stanza, request);
        if (unexpected.isPresent()) {
            return unexpected;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Models the success reply variant carrying the joined group and its gating outcome.
     *
     * <p>Carries the group {@link Jid} the caller joined (or whose approval was queued) and the
     * {@link #isMembershipApprovalPending()} flag that discriminates the two paths: {@code true}
     * means the request is queued for moderator approval and the caller is not yet a member,
     * {@code false} means the caller is already in. Callers typically follow up by locating or
     * creating the chat for the returned JID so the new chat surfaces in the user's chat list.
     *
     * @implNote
     * This implementation projects the join parser's success shape plus the explicit
     * {@code membership_approval_request} versus {@code group} child discriminator into a typed
     * pair of accessors.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupInviteJob")
    final class Success implements IqJoinGroupByInviteCodeResponse {
        /**
         * Holds the group JID the caller joined or whose membership approval was queued.
         */
        private final Jid groupJid;

        /**
         * Holds {@code true} when the entry is queued for moderator approval, {@code false} when
         * the caller is already admitted.
         */
        private final boolean membershipApprovalPending;

        /**
         * Constructs a success reply.
         *
         * @param groupJid                  the group {@link Jid}; never {@code null}
         * @param membershipApprovalPending {@code true} when the entry is queued for approval
         * @throws NullPointerException if {@code groupJid} is {@code null}
         */
        public Success(Jid groupJid, boolean membershipApprovalPending) {
            this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
            this.membershipApprovalPending = membershipApprovalPending;
        }

        /**
         * Returns the group JID.
         *
         * @return the group {@link Jid}; never {@code null}
         */
        public Jid groupJid() {
            return groupJid;
        }

        /**
         * Returns whether the entry is queued for moderator approval.
         *
         * @return {@code true} when the request is queued for moderator approval, {@code false} when the caller has already been admitted
         */
        public boolean isMembershipApprovalPending() {
            return membershipApprovalPending;
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         *
         * <p>Callers normally reach this through
         * {@link IqJoinGroupByInviteCodeResponse#of(Stanza, Stanza)}; this factory is exposed so
         * callers can short-circuit when they already know the wire shape is a success.
         *
         * @implNote
         * This implementation requires the IQ to be a {@code result} sent {@code from="g.us"}, then
         * tries the {@code <group>} child first (open-join shape) and falls back to the
         * {@code <membership_approval_request>} child (approval-gated shape); either branch reads
         * the JID from the child's {@code jid} attribute. The branch order is independent of the
         * caller's expected mode, leaving {@link UnexpectedJoinShape} to surface mismatches.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGroupInviteJob",
                exports = "joinGroupViaInvite",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var fromAttr = stanza.getAttributeAsJid("from").orElse(null);
            if (fromAttr == null || !"g.us".equals(fromAttr.toString())) {
                return Optional.empty();
            }
            var groupChild = stanza.getChild("group").orElse(null);
            if (groupChild != null) {
                var gid = groupChild.getAttributeAsJid("jid").orElse(null);
                if (gid == null) {
                    return Optional.empty();
                }
                return Optional.of(new Success(gid, false));
            }
            var approvalChild = stanza.getChild("membership_approval_request").orElse(null);
            if (approvalChild != null) {
                var gid = approvalChild.getAttributeAsJid("jid").orElse(null);
                if (gid == null) {
                    return Optional.empty();
                }
                return Optional.of(new Success(gid, true));
            }
            return Optional.empty();
        }

        /**
         * Compares this reply with another object for equality.
         *
         * <p>Two replies are equal when they carry the same {@link #groupJid()} and the same
         * {@link #isMembershipApprovalPending()} flag.
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
            return this.membershipApprovalPending == that.membershipApprovalPending
                    && Objects.equals(this.groupJid, that.groupJid);
        }

        /**
         * Returns a hash code derived from the group JID and approval flag.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(groupJid, membershipApprovalPending);
        }

        /**
         * Returns a debug string describing the group JID and approval flag.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqJoinGroupByInviteCodeResponse.Success[groupJid=" + groupJid
                    + ", membershipApprovalPending=" + membershipApprovalPending + ']';
        }
    }

    /**
     * Models the reply variant for a gating mode that differs from the caller's expectation.
     *
     * <p>Fires when the relay admits the caller through a different gating mode than the request
     * signalled via {@link IqJoinGroupByInviteCodeRequest#expectsMembershipApproval()}. Carries
     * both the actual group {@link Jid} the relay returned and the actual gating mode the relay
     * used, so callers can correct their cached view of the group without issuing a second round
     * trip.
     *
     * @implNote
     * This implementation surfaces as a sealed variant the case that would otherwise be raised as a
     * parser exception when the present child differs from the caller's expected child.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupInviteJob")
    final class UnexpectedJoinShape implements IqJoinGroupByInviteCodeResponse {
        /**
         * Holds the group JID parsed from the alternate child.
         */
        private final Jid groupJid;

        /**
         * Holds {@code true} when the relay actually returned a
         * {@code <membership_approval_request>}, {@code false} when it returned a {@code <group>}.
         */
        private final boolean actualMembershipApprovalPending;

        /**
         * Constructs an unexpected-shape reply.
         *
         * @param groupJid                        the group {@link Jid}; never {@code null}
         * @param actualMembershipApprovalPending the actual gating mode used by the relay
         * @throws NullPointerException if {@code groupJid} is {@code null}
         */
        public UnexpectedJoinShape(Jid groupJid, boolean actualMembershipApprovalPending) {
            this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
            this.actualMembershipApprovalPending = actualMembershipApprovalPending;
        }

        /**
         * Returns the group JID parsed from the alternate child.
         *
         * @return the group {@link Jid}; never {@code null}
         */
        public Jid groupJid() {
            return groupJid;
        }

        /**
         * Returns the actual gating mode used by the relay.
         *
         * @return {@code true} when the relay queued for approval, {@code false} when it admitted directly
         */
        public boolean actualMembershipApprovalPending() {
            return actualMembershipApprovalPending;
        }

        /**
         * Returns {@link Optional#empty()} unconditionally.
         *
         * <p>Detecting the unexpected-shape case requires comparing the inbound reply against the
         * caller's expected gating mode, which is not available from the wire {@link Stanza} alone.
         * The dispatcher therefore constructs this variant directly by comparing
         * {@link Success#isMembershipApprovalPending()} against
         * {@link IqJoinGroupByInviteCodeRequest#expectsMembershipApproval()}; this static factory
         * exists so the parser fall-through chain stays symmetric with the other sibling variants.
         *
         * @implNote
         * This implementation deliberately never returns a present value because the
         * {@code stanza}/{@code request} pair does not carry the caller's expectation flag; the
         * equivalent detection is routed through the dispatcher instead.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return always {@link Optional#empty()}
         */
        @WhatsAppWebExport(moduleName = "WAWebGroupInviteJob",
                exports = "joinGroupViaInvite",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<UnexpectedJoinShape> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            return Optional.empty();
        }

        /**
         * Compares this reply with another object for equality.
         *
         * <p>Two replies are equal when they carry the same {@link #groupJid()} and the same
         * {@link #actualMembershipApprovalPending()} flag.
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
            var that = (UnexpectedJoinShape) obj;
            return this.actualMembershipApprovalPending == that.actualMembershipApprovalPending
                    && Objects.equals(this.groupJid, that.groupJid);
        }

        /**
         * Returns a hash code derived from the group JID and actual gating mode.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(groupJid, actualMembershipApprovalPending);
        }

        /**
         * Returns a debug string describing the group JID and actual gating mode.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqJoinGroupByInviteCodeResponse.UnexpectedJoinShape[groupJid=" + groupJid
                    + ", actualMembershipApprovalPending=" + actualMembershipApprovalPending + ']';
        }
    }

    /**
     * Models the client-error reply variant for envelope-level caller-side rejections.
     *
     * <p>Surfaces caller-side rejections of the invite redemption: typically {@code 401} when the
     * invite code is expired or revoked, {@code 403} when the caller is banned from the group, or
     * {@code 404} when the group no longer exists. Retries are not meaningful without first
     * fetching a fresh invite link.
     *
     * @implNote
     * This implementation reads the {@code <error>} envelope's {@code code} and {@code text}
     * attributes into {@link #errorCode()} and {@link #errorText()}.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupInviteJob")
    final class ClientError implements IqJoinGroupByInviteCodeResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional text; may be {@code null}
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
         * Returns the optional error text.
         *
         * @return an {@link Optional} carrying the text, or empty when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>Callers normally reach this through
         * {@link IqJoinGroupByInviteCodeResponse#of(Stanza, Stanza)}; this factory is exposed so
         * callers can short-circuit when they already know the wire shape is a client error.
         *
         * @implNote
         * This implementation delegates to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} to validate the
         * {@code type="error"} envelope and the {@code <error>} child's {@code 4xx} {@code code}
         * before extracting code/text.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGroupInviteJob",
                exports = "joinGroupViaInvite",
                adaptation = WhatsAppAdaptation.ADAPTED)
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
            return "IqJoinGroupByInviteCodeResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the server-error reply variant for transient relay failures.
     *
     * <p>Surfaces transient {@code 5xx} relay failures while redeeming the invite code; the request
     * may be retried after a backoff.
     *
     * @implNote
     * This implementation reads the {@code <error>} envelope's {@code code} and {@code text}
     * attributes into {@link #errorCode()} and {@link #errorText()}.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupInviteJob")
    final class ServerError implements IqJoinGroupByInviteCodeResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional text; may be {@code null}
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
         * Returns the optional error text.
         *
         * @return an {@link Optional} carrying the text, or empty when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>Callers normally reach this through
         * {@link IqJoinGroupByInviteCodeResponse#of(Stanza, Stanza)}; this factory is exposed so
         * callers can short-circuit when they already know the wire shape is a server error.
         *
         * @implNote
         * This implementation delegates to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} to validate the
         * {@code type="error"} envelope and the {@code <error>} child's {@code 5xx} {@code code}
         * before extracting code/text.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGroupInviteJob",
                exports = "joinGroupViaInvite",
                adaptation = WhatsAppAdaptation.ADAPTED)
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
            return "IqJoinGroupByInviteCodeResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}

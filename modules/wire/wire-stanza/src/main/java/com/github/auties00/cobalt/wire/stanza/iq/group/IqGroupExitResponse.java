package com.github.auties00.cobalt.wire.stanza.iq.group;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed family of inbound reply variants the relay produces in response to an {@link IqGroupExitRequest}.
 *
 * <p>After dispatching the leave request, callers pattern-match the returned variant.
 * {@link Success} carries one {@link Success.LeaveResult} per requested target, with a per-target
 * outcome code so partial failures inside a batch are visible; {@link ClientError} and
 * {@link ServerError} surface envelope-level rejections that abort the entire batch.
 *
 * @implNote
 * This implementation collapses the separate group-leave and community-leave parsers into a single
 * sealed sum; the grandchild shape (whether to read {@code id} on {@code <group>} or
 * {@code parent_group_jid} on {@code <linked_groups>}) is recovered from the outbound request
 * rather than from the reply.
 */
@WhatsAppWebModule(moduleName = "WAWebGroupExitJob")
public sealed interface IqGroupExitResponse extends IqStanza.Response
        permits IqGroupExitResponse.Success, IqGroupExitResponse.ClientError, IqGroupExitResponse.ServerError {

    /**
     * Tries each {@link IqGroupExitResponse} variant in priority order and returns the first that parses cleanly.
     *
     * <p>The dispatcher hands the inbound {@link Stanza} together with the original outbound request
     * so that the parser can recover the grandchild shape from the request rather than from the
     * reply, since the relay echoes whichever shape the caller used. Returns
     * {@link Optional#empty()} when no documented variant matched the stanza shape.
     *
     * @implNote
     * This implementation tries {@link Success} first, then {@link ClientError}, then
     * {@link ServerError}, asserting the result branch before any error envelope is inspected.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza used to validate echoed identifiers and to discover the grandchild-shape mode; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
            exports = "leaveGroup", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
            exports = "leaveCommunity", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
            exports = "leaveCommunities", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqGroupExitResponse> of(Stanza stanza, Stanza request) {
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
     * Models the success reply variant carrying one per-target leave outcome.
     *
     * <p>Carries one {@link LeaveResult} per requested target. Each {@link LeaveResult} carries an
     * {@code error} code mirroring the relay's per-target status, defaulting to {@code 200} when
     * the relay omits it, which signals a per-target success. A batch that nominally succeeded at
     * the envelope level can still hold individual non-{@code 200} entries when one or more targets
     * failed, so callers iterating {@link #results()} should treat any code other than {@code 200}
     * as a per-target leave failure.
     *
     * @implNote
     * This implementation maps each {@code <group>} or {@code <linked_groups>} grandchild of the
     * {@code <leave>} child to a {@code (jid, code)} pair.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupExitJob")
    final class Success implements IqGroupExitResponse {
        /**
         * Models the per-target reply projection pairing the echoed target JID with its leave outcome code.
         *
         * <p>The {@link #code()} accessor returns {@code 200} on a clean leave; any other value is
         * the relay's per-target error code, for example {@code 403} when the caller is not
         * actually a member of the target group.
         */
        public static final class LeaveResult {
            /**
             * Holds the echoed target JID.
             */
            private final Jid jid;

            /**
             * Holds the per-target outcome code.
             */
            private final int code;

            /**
             * Constructs a per-target leave result.
             *
             * @param jid  the echoed target {@link Jid}; never {@code null}
             * @param code the per-target outcome code; {@code 200} signals a clean per-target leave
             * @throws NullPointerException if {@code jid} is {@code null}
             */
            public LeaveResult(Jid jid, int code) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.code = code;
            }

            /**
             * Returns the echoed target JID.
             *
             * @return the target {@link Jid}; never {@code null}
             */
            public Jid jid() {
                return jid;
            }

            /**
             * Returns the per-target outcome code.
             *
             * @return the outcome code; {@code 200} on success
             */
            public int code() {
                return code;
            }

            /**
             * Compares this result with another object for equality.
             *
             * <p>Two results are equal when they carry the same {@link #jid()} and the same
             * {@link #code()}.
             *
             * @param obj the object to compare with; may be {@code null}
             * @return {@code true} when {@code obj} is an equal result, {@code false} otherwise
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (LeaveResult) obj;
                return this.code == that.code
                        && Objects.equals(this.jid, that.jid);
            }

            /**
             * Returns a hash code derived from the JID and code.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, code);
            }

            /**
             * Returns a debug string describing the JID and code.
             *
             * @return the string representation
             */
            @Override
            public String toString() {
                return "IqGroupExitResponse.Success.LeaveResult[jid=" + jid
                        + ", code=" + code + ']';
            }
        }

        /**
         * Holds the list of per-target outcome projections.
         */
        private final List<LeaveResult> results;

        /**
         * Constructs a success reply.
         *
         * @param results the per-target outcome list; never {@code null}
         * @throws NullPointerException if {@code results} is {@code null}
         */
        public Success(List<LeaveResult> results) {
            Objects.requireNonNull(results, "results cannot be null");
            this.results = List.copyOf(results);
        }

        /**
         * Returns the list of per-target outcome projections.
         *
         * @return an unmodifiable {@link List} of {@link LeaveResult} entries; never {@code null}
         */
        public List<LeaveResult> results() {
            return results;
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         *
         * <p>Callers normally reach this through {@link IqGroupExitResponse#of(Stanza, Stanza)}; this
         * factory is exposed so callers can short-circuit when they already know the wire shape is
         * a success.
         *
         * @implNote
         * This implementation recovers the grandchild shape from the outbound request: if the
         * outbound {@code <leave>} carries any {@code <linked_groups>} child the parser reads
         * {@code parent_group_jid} on each grandchild, otherwise it reads {@code id} on
         * {@code <group>} children. The {@code error} attribute defaults to {@code 200} when
         * absent.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
                exports = "leaveGroup",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
                exports = "leaveCommunity",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
                exports = "leaveCommunities",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var leaveChild = stanza.getChild("leave").orElse(null);
            if (leaveChild == null) {
                return Optional.empty();
            }
            var requestLeaveChild = request.getChild("leave").orElse(null);
            if (requestLeaveChild == null) {
                return Optional.empty();
            }
            String jidAttribute;
            String grandchildTag;
            if (requestLeaveChild.hasChild("linked_groups")) {
                grandchildTag = "linked_groups";
                jidAttribute = "parent_group_jid";
            } else {
                grandchildTag = "group";
                jidAttribute = "id";
            }
            var results = new ArrayList<LeaveResult>();
            var children = leaveChild.streamChildren(grandchildTag).toList();
            for (var child : children) {
                var jid = child.getAttributeAsJid(jidAttribute).orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var code = child.getAttributeAsInt("error", 200);
                results.add(new LeaveResult(jid, code));
            }
            return Optional.of(new Success(results));
        }

        /**
         * Compares this reply with another object for equality.
         *
         * <p>Two replies are equal when they carry the same {@link #results()} list.
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
            return Objects.equals(this.results, that.results);
        }

        /**
         * Returns a hash code derived from the results.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(results);
        }

        /**
         * Returns a debug string describing the results.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqGroupExitResponse.Success[results=" + results + ']';
        }
    }

    /**
     * Models the client-error reply variant for envelope-level caller-side rejections.
     *
     * <p>Surfaces caller-side rejections of the whole leave batch: typically {@code 400} on a
     * malformed stanza or {@code 401} when the caller's session no longer authorises group
     * operations. Per-target failures are surfaced as non-{@code 200} entries inside a
     * {@link Success} instead; this variant only fires when the relay rejects the envelope itself.
     *
     * @implNote
     * This implementation reads the {@code <error>} envelope's {@code code} and {@code text}
     * attributes into {@link #errorCode()} and {@link #errorText()}.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupExitJob")
    final class ClientError implements IqGroupExitResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text when the relay supplied one, otherwise {@code null}.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply.
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
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>Callers normally reach this through {@link IqGroupExitResponse#of(Stanza, Stanza)}; this
         * factory is exposed so callers can short-circuit when they already know the wire shape is
         * a client error.
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
        @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
                exports = "leaveGroup",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
                exports = "leaveCommunity",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
                exports = "leaveCommunities",
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
            return "IqGroupExitResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the server-error reply variant for transient relay failures.
     *
     * <p>Surfaces transient {@code 5xx} relay failures while processing the leave batch; the
     * request may be retried after a backoff.
     *
     * @implNote
     * This implementation reads the {@code <error>} envelope's {@code code} and {@code text}
     * attributes into {@link #errorCode()} and {@link #errorText()}.
     */
    @WhatsAppWebModule(moduleName = "WAWebGroupExitJob")
    final class ServerError implements IqGroupExitResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text when the relay supplied one, otherwise {@code null}.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply.
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
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>Callers normally reach this through {@link IqGroupExitResponse#of(Stanza, Stanza)}; this
         * factory is exposed so callers can short-circuit when they already know the wire shape is
         * a server error.
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
        @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
                exports = "leaveGroup",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
                exports = "leaveCommunity",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
                exports = "leaveCommunities",
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
            return "IqGroupExitResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
